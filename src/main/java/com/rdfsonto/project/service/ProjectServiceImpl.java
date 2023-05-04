package com.rdfsonto.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.service.ClassNodeService;
import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.infrastructure.security.service.AuthService;
import com.rdfsonto.prefix.service.PrefixNodeService;
import com.rdfsonto.project.database.ProjectNode;
import com.rdfsonto.project.database.ProjectRepository;
import com.rdfsonto.user.database.UserNode;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService
{
    private final AuthService authService;
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final UniqueUriIdHandler uniqueUriIdHandler;

    @Override
    public Optional<ProjectNode> findById(final long projectId)
    {
        return projectRepository.findById(projectId);
    }

    @Override
    public Optional<ProjectNode> findProjectByNameAndUserId(final String projectName, final long userId)
    {
        return projectRepository.findProjectByNameAndUserId(projectName, userId);
    }

    @Override
    public List<ProjectNode> findProjectNodesByUsername(final String username)
    {
        return projectRepository.findProjectNodesByUser(username);
    }

    @Override
    public List<ProjectNode> findAll()
    {
        return projectRepository.findAll();
    }

    @Override
    public ProjectNode save(final String projectName, final UserNode user)
    {
        final var project = ProjectNode.builder()
            .withProjectName(projectName)
            .withOwner(user)
            .build();

        final var saved = projectRepository.save(project);

        return projectRepository.findById(saved.getId())
            .orElseThrow(() -> new IllegalStateException("Could not save a project."));
    }

    @Override
    public ProjectNode update(final ProjectNode update)
    {
        return projectRepository.save(update);
    }

    @Override
    public void delete(final ProjectNode project)
    {
        if (projectRepository.findById(project.getId()).isEmpty())
        {
            log.warn("Attempted to delete non-existing project id: {}", project.getId());
            return;
        }

        projectRepository.delete(project);
    }

    @Override
    public String getProjectTag(final ProjectNode project)
    {
        final var ownerId = project.getOwnerId();
        final var userNode = userService.findById(ownerId);

        final var user = userNode.orElseThrow(() -> new IllegalStateException("Can't get a tag for a non-existing user, id: %s".formatted(ownerId)));

        return uniqueUriIdHandler.uniqueUri(user.getId(), project.getId());
    }
}