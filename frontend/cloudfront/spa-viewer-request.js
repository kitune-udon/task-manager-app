function isApiPath(uri) {
  return uri.startsWith('/api/')
}

function looksLikeStaticAsset(uri) {
  return uri.includes('.')
}

function handler(event) {
  var request = event.request
  var uri = request.uri || '/'

  if (isApiPath(uri) || looksLikeStaticAsset(uri)) {
    return request
  }

  request.uri = '/index.html'
  return request
}
