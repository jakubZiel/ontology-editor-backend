package com.rdfsonto.rdfsonto.classnode.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.rdfsonto.classnode.database.ClassNodeVo;
import com.rdfsonto.rdfsonto.classnode.database.ClassNodeVoMapper;
import com.rdfsonto.rdfsonto.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class ClassNodeServiceImpl implements ClassNodeService
{
    private final static long MAX_NUMBER_OF_NEIGHBOURS = 1000;
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ClassNodeMapper classNodeMapper;
    private final ClassNodeVoMapper classNodeVoMapper;
    private final ProjectService projectService;

    @Override
    public List<ClassNode> findByIds(final List<Long> ids)
    {
        final var notHydratedNodes = classNodeRepository.findAllById(ids);

        if (notHydratedNodes.size() != ids.size())
        {
            throw new IllegalStateException("Not all nodes exist");
        }

        final var incoming = classNodeRepository.findAllIncomingNeighbours(ids);
        final var outgoing = classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(ids);

        final var groupedIncoming = incoming.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));
        final var groupedOutgoing = outgoing.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));

        return notHydratedNodes.stream()
            .map(node ->
                classNodeMapper.mapToDomain(node,
                    groupedIncoming.get(node.getId()),
                    groupedOutgoing.get(node.getId())))
            .collect(Collectors.toList());
    }

    @Override
    public List<ClassNode> findByPropertyValue(final long projectId, final String propertyKey, final String value)
    {
        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new IllegalStateException("Project id: %s does not exist".formatted(projectId)));

        final var projectTag = projectService.getProjectTag(project);

        final var nodeIds = classNodeRepository.findAllClassNodesVoByPropertyValue(propertyKey, value, projectTag).stream()
            .map(ClassNodeVo::getId)
            .toList();

        return findByIds(nodeIds);
    }

    @Override
    public Optional<ClassNode> findById(final Long id)
    {
        final var notHydratedNode = classNodeRepository.findById(id);

        if (notHydratedNode.isEmpty())
        {
            return Optional.empty();
        }

        final var incoming = classNodeRepository.findAllIncomingNeighbours(id);
        final var outgoing = classNodeRepository.findAllOutgoingNeighbours(id);

        return Optional.of(classNodeMapper.mapToDomain(notHydratedNode.get(), incoming, outgoing));
    }

    @Override
    public List<ClassNode> findNeighbours(final long id, final int maxDistance, final List<String> allowedRelationships)
    {
        final var numberOfNeighbours = classNodeRepository.countAllNeighbours(maxDistance, id);

        if (numberOfNeighbours > MAX_NUMBER_OF_NEIGHBOURS)
        {
            log.warn("Handling more than {} number of neighbours", numberOfNeighbours);
            throw new NotImplementedException();
        }

        final var neighbourIds = classNodeRepository.findAllNeighbours(maxDistance, id).stream()
            .map(ClassNodeVo::getId)
            .toList();

        return findByIds(neighbourIds);
    }

    // TODO take care of more types of relationship coming from the same node
    @Override
    public Optional<ClassNode> save(final ClassNode node)
    {
        final var nodeVo = classNodeVoMapper.mapToVo(node);

        final var savedVo = classNodeRepository.save(nodeVo);

        final var outgoing = classNodeRepository.findAllById(node.outgoingNeighbours().keySet());
        final var incoming = classNodeRepository.findAllById(node.incomingNeighbours().keySet());

        savedVo.setNeighbours(new HashMap<>());

        node.incomingNeighbours().keySet().forEach(neighbour -> {
            final var relationship = node.incomingNeighbours().get(neighbour);

            final var neighboursByRelationship = savedVo.getNeighbours().get(relationship);

            if (neighboursByRelationship != null)
            {
                neighboursByRelationship.add(
                    incoming.stream()
                        .filter(n -> n.getId().equals(neighbour))
                        .findFirst()
                        .orElseThrow());
            }
            else
            {
                savedVo.getNeighbours().put(relationship,
                    new ArrayList<>(List.of(incoming.stream()
                        .filter(n -> n.getId().equals(neighbour))
                        .findFirst()
                        .orElseThrow())));
            }
        });

        node.outgoingNeighbours().keySet().forEach(neighbour -> {
            final var relationship = node.outgoingNeighbours().get(neighbour);

            final var destinationNode = outgoing.stream()
                .filter(n -> n.getId().equals(neighbour))
                .findFirst()
                .orElseThrow();

            connectOutgoing(savedVo, destinationNode, relationship);
        });

        classNodeRepository.saveAll(outgoing);
        classNodeRepository.save(savedVo);

        return findById(savedVo.getId());
    }

    @Override
    public Optional<ClassNode> update(final ClassNode node)
    {
        final var original = classNodeRepository.findById(node.id());

        if (original.isEmpty())
        {
            return Optional.empty();
        }

        final var outgoing = classNodeRepository.findAllById(node.outgoingNeighbours().keySet());
        final var incoming = classNodeRepository.findAllById(node.incomingNeighbours().keySet());

        final var originalNode = original.get();

        if (originalNode.getNeighbours() == null)
        {
            originalNode.setNeighbours(new HashMap<>());
        }

        final var incomingNeighbours = originalNode.getNeighbours();

        incoming.forEach(neighbour -> {
            final var relationship = node.incomingNeighbours().get(neighbour.getId());

            if (incomingNeighbours.containsKey(relationship))
            {
                final var neighbourByRelationship = incomingNeighbours.get(relationship);

                final var isNewRelation = neighbourByRelationship.stream()
                    .noneMatch(n -> n.getId().equals(neighbour.getId()));

                if (isNewRelation)
                {
                    neighbourByRelationship.add(neighbour);
                }
            }
            else
            {
                incomingNeighbours.put(relationship, new ArrayList<>(List.of(neighbour)));
            }
        });

        outgoing.forEach(neighbour -> {
            final var relationship = node.outgoingNeighbours().get(neighbour.getId());

            connectOutgoing(originalNode, neighbour, relationship);
        });

        classNodeRepository.saveAll(outgoing);
        classNodeRepository.save(originalNode);

        return findById(node.id());
    }

    @Override
    public boolean deleteById(final long id)
    {
        classNodeRepository.deleteById(id);
        return !classNodeRepository.existsById(id);
    }

    @Override
    public ProjectNodeMetadata findProjectNodeMetaData(final String projectTag)
    {
        final var propertyKeys = classNodeRepository.findAllPropertyKeys(projectTag);
        final var relationshipTypes = classNodeRepository.findAllRelationshipTypes(projectTag);
        final var labels = classNodeRepository.findAllLabels(projectTag);

        return ProjectNodeMetadata.builder()
            .withPropertyKeys(propertyKeys)
            .withRelationshipTypes(relationshipTypes)
            .withNodeLabels(labels)
            .build();
    }

    private void connectOutgoing(final ClassNodeVo originalNode, final ClassNodeVo neighbour, final String relationship)
    {
        if (neighbour.getNeighbours() == null)
        {
            neighbour.setNeighbours(new HashMap<>());
        }

        final var destinationNodeNeighboursByRelationship = neighbour.getNeighbours().get(relationship);

        if (destinationNodeNeighboursByRelationship != null)
        {
            destinationNodeNeighboursByRelationship.add(originalNode);
        }
        else
        {
            neighbour.getNeighbours().put(relationship, new ArrayList<>(List.of((originalNode))));
        }
    }
}