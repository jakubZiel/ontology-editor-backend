package com.rdfsonto.rdfsonto.classnode.database;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


public interface ClassNodeRepository extends Neo4jRepository<ClassNodeVo, Long>
{
    @Query("""
        MATCH (n:Resource)-[relation]->(neighbour:Resource)
        WHERE id(n) = $nodeId
        RETURN neighbour, type(relation) as relation
        """)
    List<ClassNodeVo> findAllOutgoingNeighbours(@Param("nodeId") long id);

    @Query("""
        MATCH (n:Resource)<-[relation]-(neighbour:Resource)
        WHERE id(n) = $nodeId
        RETURN neighbour, type(relation) as relation
        """)
    List<ClassNodeVo> findAllIncomingNeighbours(@Param("nodeId") long id);

    @Query("""
        MATCH (n:Resource)<-[rel]-(neighbour:Resource)
        WHERE id(n) IN $nodeIds
        RETURN neighbour, type(rel) as relation, id(n) as source
        """)
    List<ClassNodeVo> findAllIncomingNeighbours(@Param("nodeIds") List<Long> ids);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        CALL apoc.neighbors.tohop(n, "<|>", $maxDistance)
        YIELD node
        RETURN node
        UNION
        MATCH (n:Resource) WHERE id(n) = $nodeId
        RETURN n as node
        """)
    List<ClassNodeVo> findAllNeighbours(@Param("maxDistance") final int maxDistance, @Param("nodeId") final long sourceNodeId);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        CALL apoc.neighbors.tohop.count(n, "<|>", $maxDistance)
        YIELD value
        RETURN value
        """)
    int countAllNeighbours(@Param("maxDistance") final int maxDistance, @Param("nodeId") final long sourceNodeId);

    @Query("""
        MATCH (n:Resource)
        WHERE NOT (n)-[:`http://www.w3.org/2000/01/rdf-schema#subClassOf`]->()
        AND ()-[:`http://www.w3.org/2000/01/rdf-schema#subClassOf`]->(n)
        RETURN n
        """)
    List<ClassNodeVo> findAllHierarchyRoots(final List<String> relationships);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        UNWIND keys(n) AS prop
        RETURN prop
        """)
    List<String> getAllNodeProperties(@Param("nodeId") long id);

    @Query("""
        MATCH (n:Resource) WHERE id(n) = $nodeId
        UNWIND keys(n) AS prop
        RETURN val: n[prop], key: prop
        """)
    List<Object> getAllNodeValues(@Param("nodeId") long id);

    @Query("""
        CALL db.relationshipTypes()
        YIELD relationshipType
        RETURN relationshipType
        """)
    List<String> findAllRelationshipTypes(final String projectTag);

    @Query("""
        CALL db.labels()
        YIELD label
        RETURN label
         """)
    List<String> findAllLabels(final String projectTag);

    @Query("""
        CALL db.propertyKeys()
        YIELD propertyKey
        RETURN propertyKey
        """)
    List<String> findAllPropertyKeys(final String projectTag);

    @Query("""
        MATCH (n:Resource)
        WHERE n.`$key` = "$value"
        RETURN n
        """)
    List<ClassNodeVo> findAllClassNodesVoByPropertyValue(@Param("key") String key, @Param("value") String value, @Param("tag") String tag);

    Long countAllByClassLabelsContaining(String projectTag);
}