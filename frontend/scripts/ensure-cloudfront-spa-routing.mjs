import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const FUNCTION_RUNTIME = 'cloudfront-js-2.0'
const FUNCTION_COMMENT = 'Rewrite non-API SPA routes to /index.html'
const WAIT_INTERVAL_MS = 15_000
const WAIT_TIMEOUT_MS = 20 * 60_000

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')

const distributionId = process.env.CLOUDFRONT_DISTRIBUTION_ID?.trim()
const functionName = process.env.CLOUDFRONT_SPA_FUNCTION_NAME?.trim() || 'taskflow-prd-spa-router'
const functionCodePath = path.join(projectRoot, 'cloudfront', 'spa-viewer-request.js')

if (!distributionId) {
  console.error('CLOUDFRONT_DISTRIBUTION_ID is required.')
  process.exit(1)
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function runAws(args) {
  const result = spawnSync('aws', args, {
    cwd: projectRoot,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  })

  if (result.status !== 0) {
    const command = ['aws', ...args].join(' ')
    const stderr = result.stderr?.trim() || '(no stderr)'
    throw new Error(`${command} failed: ${stderr}`)
  }

  return result.stdout
}

function runAwsJson(args) {
  const output = runAws([...args, '--output', 'json'])
  return JSON.parse(output)
}

function normalizeFunctionAssociations(functionAssociations) {
  const items = functionAssociations?.Items ?? []
  return items
    .map((item) => ({
      EventType: item.EventType,
      FunctionARN: item.FunctionARN,
    }))
    .sort((left, right) => left.EventType.localeCompare(right.EventType))
}

function normalizeCustomErrorResponses(customErrorResponses) {
  const items = customErrorResponses?.Items ?? []
  return items
    .map((item) => ({
      ErrorCode: item.ErrorCode,
      ResponseCode: item.ResponseCode,
      ResponsePagePath: item.ResponsePagePath,
      ErrorCachingMinTTL: item.ErrorCachingMinTTL,
    }))
    .sort((left, right) => left.ErrorCode - right.ErrorCode)
}

function configNeedsUpdate(currentConfig, nextConfig) {
  const currentAssociations = normalizeFunctionAssociations(
    currentConfig.DefaultCacheBehavior?.FunctionAssociations,
  )
  const nextAssociations = normalizeFunctionAssociations(
    nextConfig.DefaultCacheBehavior?.FunctionAssociations,
  )
  const currentErrors = normalizeCustomErrorResponses(currentConfig.CustomErrorResponses)
  const nextErrors = normalizeCustomErrorResponses(nextConfig.CustomErrorResponses)

  return (
    JSON.stringify(currentAssociations) !== JSON.stringify(nextAssociations)
    || JSON.stringify(currentErrors) !== JSON.stringify(nextErrors)
  )
}

function buildUpdatedDistributionConfig(distributionConfig, functionArn) {
  const nextConfig = JSON.parse(JSON.stringify(distributionConfig))
  const existingAssociations = nextConfig.DefaultCacheBehavior?.FunctionAssociations?.Items ?? []
  const nextAssociations = existingAssociations
    .filter((item) => item.EventType !== 'viewer-request')
    .concat([{ EventType: 'viewer-request', FunctionARN: functionArn }])

  nextConfig.DefaultCacheBehavior.FunctionAssociations = {
    Quantity: nextAssociations.length,
    Items: nextAssociations,
  }

  const retainedErrors = (nextConfig.CustomErrorResponses?.Items ?? []).filter(
    (item) => item.ErrorCode !== 403 && item.ErrorCode !== 404,
  )

  nextConfig.CustomErrorResponses = retainedErrors.length > 0
    ? {
        Quantity: retainedErrors.length,
        Items: retainedErrors,
      }
    : {
        Quantity: 0,
      }

  return nextConfig
}

function createFunctionConfigFile(tempDir) {
  const filePath = path.join(tempDir, 'function-config.json')
  writeFileSync(
    filePath,
    JSON.stringify(
      {
        Comment: FUNCTION_COMMENT,
        Runtime: FUNCTION_RUNTIME,
      },
      null,
      2,
    ),
  )
  return filePath
}

function createDistributionConfigFile(tempDir, distributionConfig) {
  const filePath = path.join(tempDir, 'distribution-config.json')
  writeFileSync(filePath, JSON.stringify(distributionConfig, null, 2))
  return filePath
}

function describeFunction(stage) {
  return runAwsJson(['cloudfront', 'describe-function', '--name', functionName, '--stage', stage])
}

function ensureCloudFrontFunction(tempDir) {
  const functionConfigPath = createFunctionConfigFile(tempDir)
  const codeArg = `fileb://${functionCodePath}`
  const configArg = `file://${functionConfigPath}`

  let developmentEtag

  try {
    const development = describeFunction('DEVELOPMENT')
    developmentEtag = development.ETag
  } catch (error) {
    if (!String(error.message).includes('NoSuchFunctionExists')) {
      throw error
    }

    const created = runAwsJson([
      'cloudfront',
      'create-function',
      '--name',
      functionName,
      '--function-config',
      configArg,
      '--function-code',
      codeArg,
    ])
    developmentEtag = created.ETag
  }

  const updated = runAwsJson([
    'cloudfront',
    'update-function',
    '--name',
    functionName,
    '--if-match',
    developmentEtag,
    '--function-config',
    configArg,
    '--function-code',
    codeArg,
  ])

  const published = runAwsJson([
    'cloudfront',
    'publish-function',
    '--name',
    functionName,
    '--if-match',
    updated.ETag,
  ])

  return published.FunctionSummary.FunctionMetadata.FunctionARN
}

async function waitForDistributionDeployment(id) {
  const startedAt = Date.now()

  while (true) {
    const distribution = runAwsJson(['cloudfront', 'get-distribution', '--id', id])
    const status = distribution.Distribution?.Status

    if (status === 'Deployed') {
      return
    }

    if (Date.now() - startedAt > WAIT_TIMEOUT_MS) {
      throw new Error(`Timed out waiting for CloudFront distribution ${id} to become Deployed.`)
    }

    await sleep(WAIT_INTERVAL_MS)
  }
}

async function main() {
  const tempDir = mkdtempSync(path.join(tmpdir(), 'cloudfront-spa-routing-'))

  try {
    const functionArn = ensureCloudFrontFunction(tempDir)
    const current = runAwsJson(['cloudfront', 'get-distribution-config', '--id', distributionId])
    const nextDistributionConfig = buildUpdatedDistributionConfig(current.DistributionConfig, functionArn)

    if (!configNeedsUpdate(current.DistributionConfig, nextDistributionConfig)) {
      console.log(
        JSON.stringify(
          {
            distributionId,
            functionName,
            functionArn,
            updated: false,
            message: 'CloudFront SPA routing is already up to date.',
          },
          null,
          2,
        ),
      )
      return
    }

    const distributionConfigPath = createDistributionConfigFile(tempDir, nextDistributionConfig)

    runAws([
      'cloudfront',
      'update-distribution',
      '--id',
      distributionId,
      '--if-match',
      current.ETag,
      '--distribution-config',
      `file://${distributionConfigPath}`,
    ])

    await waitForDistributionDeployment(distributionId)

    console.log(
      JSON.stringify(
        {
          distributionId,
          functionName,
          functionArn,
          updated: true,
          removedCustomErrorResponses: [403, 404],
          viewerRequestRewriteEnabled: true,
        },
        null,
        2,
      ),
    )
  } finally {
    rmSync(tempDir, { force: true, recursive: true })
  }
}

await main()
