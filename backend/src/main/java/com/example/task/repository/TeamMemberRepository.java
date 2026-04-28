package com.example.task.repository;

import com.example.task.entity.TeamMember;
import com.example.task.entity.TeamRole;
import com.example.task.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * TeamMember エンティティの保存とチーム所属検索を担当するリポジトリ。
 */
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query("""
            select case when count(tm) > 0 then true else false end
            from TeamMember tm
            where tm.team.id = :teamId
              and tm.user.id = :userId
            """)
    boolean existsByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"team", "user"})
    @Query("""
            select tm
            from TeamMember tm
            where tm.team.id = :teamId
              and tm.user.id = :userId
            """)
    Optional<TeamMember> findByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"team", "user"})
    Optional<TeamMember> findById(Long memberId);

    @EntityGraph(attributePaths = {"team", "user"})
    @Query("""
            select tm
            from TeamMember tm
            where tm.team.id = :teamId
            order by case tm.role
                       when com.example.task.entity.TeamRole.OWNER then 0
                       when com.example.task.entity.TeamRole.ADMIN then 1
                       else 2
                     end asc,
                     tm.joinedAt asc,
                     tm.user.id asc
            """)
    List<TeamMember> findByTeamId(@Param("teamId") Long teamId);

    @EntityGraph(attributePaths = {"team", "user"})
    @Query("""
            select tm
            from TeamMember tm
            where tm.user.id = :userId
            order by tm.team.name asc, tm.team.id asc
            """)
    List<TeamMember> findByUserIdOrderByTeamNameAscTeamIdAsc(@Param("userId") Long userId);

    @Query("""
            select count(tm)
            from TeamMember tm
            where tm.team.id = :teamId
            """)
    long countByTeamId(@Param("teamId") Long teamId);

    @Query("""
            select u
            from User u
            where not exists (
                select tm.id
                from TeamMember tm
                where tm.team.id = :teamId
                  and tm.user.id = u.id
            )
            order by u.name asc, u.id asc
            """)
    List<User> findAvailableUsers(@Param("teamId") Long teamId);

    @Query("""
            select tm.role
            from TeamMember tm
            where tm.team.id = :teamId
              and tm.user.id = :userId
            """)
    Optional<TeamRole> findRoleByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
