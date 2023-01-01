package com.rdfsonto.rdfsonto.service.project;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import com.rdfsonto.rdfsonto.repository.project.ProjectNode;
import com.rdfsonto.rdfsonto.repository.project.ProjectRepository;
import com.rdfsonto.rdfsonto.repository.user.UserNode;
import com.rdfsonto.rdfsonto.service.security.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService
{
    private final AuthService authService;
    private final ProjectRepository projectRepository;

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
            log.warn("Attempted to deleted non-existing project id: {}", project.getId());
            return;
        }

        projectRepository.delete(project);
    }

    @Override
    public String getProjectTag(final long projectId)
    {
        throw new NotImplementedException();
    }
}
