package com.rdfsonto.rdfsonto.repository.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import lombok.NonNull;


public interface ProjectRepository extends Neo4jRepository<ProjectNode, Long>
{
    @Query("""
        MATCH (u:User{name: $user})-[:OWNER]->(p:Project)
        RETURN p, id(u) as ownerId
        """)
    List<ProjectNode> findProjectNodesByUser(@Param("user") String user);

    @Query("""
        MATCH (u:User) WHERE id(u) = $userId
        MATCH (p:Project {name: $projectName})<-[:OWNS]-(u)
        RETURN p, id(u) as ownerId
        """)
    Optional<ProjectNode> findProjectByNameAndUserId(@Param("projectName") String projectName, @Param("userId") long userId);

    @NonNull
    @Query("""
        MATCH (n:Project)<-[:OWNER]-(o:User)
        RETURN n, id(o) as ownerId
        """)
    List<ProjectNode> findAll();

    @NonNull
    @Query("""
        MATCH (p:Project)<-[:OWNS]-(u)
        WHERE id(p) = $projectId
        RETURN p, id(u) as ownerId
        """)
    Optional<ProjectNode> findById(@NonNull Long projectId);
}
