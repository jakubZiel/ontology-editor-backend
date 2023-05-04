package com.rdfsonto.importonto.service;

import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.FAILED_ONTOLOGY_IMPORT;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_ONTOLOGY_URL;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_PROJECT_ID;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_RDF_FORMAT;
import static com.rdfsonto.importonto.service.ImportOntologyErrorCode.INVALID_USER_ID;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.classnode.service.UniqueUriIdHandler;
import com.rdfsonto.importonto.database.ImportOntologyRepository;
import com.rdfsonto.importonto.database.ImportOntologyResult;
import com.rdfsonto.prefix.service.PrefixNodeService;
import com.rdfsonto.project.database.ProjectNode;
import com.rdfsonto.project.service.ProjectService;
import com.rdfsonto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
class ImportOntologyServiceImpl implements ImportOntologyService
{
    private static final String WORKSPACE_DIR = System.getProperty("user.dir") + "/workspace/";

    private final UserService userService;
    private final ImportOntologyRepository importOntologyRepository;
    private final ProjectService projectService;
    private final PrefixNodeService prefixNodeService;
    private final UniqueUriIdHandler uniqueUriIdHandler;

    @Override
    public ImportOntologyResult importOntology(final URL source, final RDFFormat rdfFormat, final Long userId, final Long projectId)
    {
        userService.findById(userId)
            .orElseThrow(() -> new ImportOntologyException("User with ID: %s does not exist.".formatted(userId), INVALID_USER_ID));

        final var validRdfFormat = Optional.ofNullable(rdfFormat)
            .orElseThrow(() -> new ImportOntologyException("Invalid RDF format", INVALID_RDF_FORMAT));

        final var project = projectService.findById(projectId)
            .orElseThrow(() -> new ImportOntologyException("Project with ID: %s does not exist.".formatted(projectId), INVALID_PROJECT_ID));

        final var downloadedOntology = downloadOntology(source, project, validRdfFormat);

        if (downloadedOntology.ioException() != null)
        {
            throw new ImportOntologyException("Failed to download ontology form URL: %s.".formatted(source), INVALID_ONTOLOGY_URL);
        }

        final var importResult = importOntology(downloadedOntology);

        if (!importResult.getTerminationStatus().equals("OK") || importResult.getTriplesLoaded() <= 0)
        {
            throw new ImportOntologyException("Failed to import ontology.", FAILED_ONTOLOGY_IMPORT);
        }

        return importResult;
    }

    public DownloadedOntology downloadOntology(final URL source,
                                               final ProjectNode project,
                                               final RDFFormat rdfFormat)
    {
        final var rdf4jDownloader = new RDFImporter();

        final var ontologyTag = projectService.getProjectTag(project);
        final var outputFile = Path.of(WORKSPACE_DIR + ontologyTag + ".input");

        try
        {
            rdf4jDownloader.prepareRDFFileToMergeIntoNeo4j(source, outputFile, ontologyTag, rdfFormat);
            importPrefixes(rdf4jDownloader, project.getId());

            return DownloadedOntology.builder()
                .withPath(outputFile)
                .withRdfFormat(rdfFormat)
                .build();
        }
        catch (final IOException ioException)
        {
            return DownloadedOntology.builder()
                .withIoException(ioException)
                .build();
        }
    }

    private ImportOntologyResult importOntology(final DownloadedOntology downloadedOntology)
    {
        final var path = "file://" + getWorkspaceDirAbsolutePath(downloadedOntology.path().toString());
        final var rdfFormat = downloadedOntology.rdfFormat().getName();

        return importOntologyRepository.importOntology(path, rdfFormat);
    }

    private void importPrefixes(final RDFImporter rdf4jDownloader, final long projectId)
    {
        final var importNamespaces = rdf4jDownloader.getLoadedNamespaces().stream()
            .map(x -> Map.entry(x.getPrefix(), uniqueUriIdHandler.removeUniqueness(x.getName())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final var newNamespaces = prefixNodeService.findAll(projectId)
            .map(namespace -> updateNamespace(namespace, importNamespaces))
            .orElse(importNamespaces);

        prefixNodeService.save(projectId, newNamespaces);
    }

    private Map<String, String> updateNamespace(final Map<String, String> currentNamespaces, final Map<String, String> updateNamespace)
    {
        final var modifiedDuplicates = updateNamespace.entrySet().stream()
            .filter(namespace -> isDuplicateClashingNamespace(namespace, currentNamespaces))
            .map(namespace -> Map.entry(namespace.getKey().concat("_dup"), namespace.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Stream.of(currentNamespaces, modifiedDuplicates, updateNamespace)
            .flatMap(namespace -> namespace.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (original, update) -> original));
    }

    private String getWorkspaceDirAbsolutePath(final String localWorkspaceDir)
    {
        return localWorkspaceDir.substring(localWorkspaceDir.indexOf("/workspace"));
    }

    private boolean isDuplicateClashingNamespace(final Map.Entry<String, String> namespace, final Map<String, String> currentNamespaces)
    {
        final var isDuplicate = currentNamespaces.containsKey(namespace.getKey());
        final var isClashing = !namespace.getValue().equals(currentNamespaces.get(namespace.getKey()));

        return isDuplicate && isClashing;
    }
}