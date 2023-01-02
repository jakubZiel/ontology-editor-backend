package com.rdfsonto.rdfsonto.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rdfsonto.rdfsonto.project.database.ProjectNode;
import com.rdfsonto.rdfsonto.project.database.ProjectRepository;
import com.rdfsonto.rdfsonto.user.database.UserNode;
import com.rdfsonto.rdfsonto.infrastructure.security.service.AuthService;
import com.rdfsonto.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService
{
    private final AuthService authService;
    private final ProjectRepository projectRepository;
    private final UserService userService;

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
            .build();

        final var saved = projectRepository.save(project);

        projectRepository.addProjectToUser(saved.getId(), user.getId());

        return projectRepository.findProjectByNameAndUserId(projectName, user.getId())
            .orElseThrow();
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
        final var id = project.getId();
        final var userNode = userService.findById(id);

        final var user = userNode.orElseThrow(() -> new IllegalStateException("Can not get a tag for a non-existing user, id: %s".formatted(id)));

        return "%s@%s".formatted(id, user.getId());
    }
}