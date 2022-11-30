package com.rdfsonto.rdfsonto.controller.exportonto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.rdfsonto.service.project.ProjectService;
import com.rdfsonto.rdfsonto.service.rdf4j.RdfFormatParser;
import com.rdfsonto.rdfsonto.service.rdf4j.exportonto.ExportOntologyService;
import com.rdfsonto.rdfsonto.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/export")
public class ExportOntologyController
{
    private final ExportOntologyService exportOntologyService;
    private final ProjectService projectService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> exportRDFResource(final ExportOntologyRequest exportOntologyRequest, final HttpServletResponse response)
    {
        if (isInvalid(exportOntologyRequest))
        {
            log.warn("Invalid export ontology request: {}", exportOntologyRequest);
            return ResponseEntity.badRequest().body("invalid_request");
        }

        final var project = projectService.findProjectByNameAndUserId(exportOntologyRequest.projectName(), exportOntologyRequest.userId());
        if (project.isEmpty())
        {
            log.warn("Invalid export ontology name: {}, ontology does not exist", exportOntologyRequest.projectName());
            return ResponseEntity.badRequest().body("invalid_project_name");
        }

        if (userService.findById(exportOntologyRequest.userId()).isEmpty())
        {
            log.warn("Attempted ontology export for non-existing user id: {}", exportOntologyRequest.userId());
            return ResponseEntity.notFound().build();
        }

        final var rdfFormat = RdfFormatParser.parse(exportOntologyRequest.rdfFormat());

        if (rdfFormat == null)
        {
            log.warn("Invalid rdf format: {}", exportOntologyRequest.rdfFormat());
            return ResponseEntity.badRequest().body("invalid_rdf_format");
        }

        final var extractedOntology = exportOntologyService.extractOntology(
            exportOntologyRequest.userId(),
            project.get().getId(),
            rdfFormat
        );

        final var ontologyExportResult = exportOntologyService.exportOntology(
            exportOntologyRequest.userId(),
            project.get().getId(),
            extractedOntology
        );

        final var ontologyFile = ontologyExportResult.ontologyFile();
        final var ontologyFileInputStream = ontologyExportResult.inputStream();

        final var fileAttached = attachFileToHttpResponse(response, ontologyFile, ontologyFileInputStream);

        if (!fileAttached)
        {
            log.error("Error attaching a file: {} to http response", ontologyFile.getName());
            return ResponseEntity.internalServerError().body("ontology-export-error");
        }

        exportOntologyService.clearWorkspace(extractedOntology.path());

        return ResponseEntity.ok(exportOntologyRequest.fileName());
    }

    private boolean attachFileToHttpResponse(final HttpServletResponse response, final File ontologyFile, final InputStream responseInputStream)
    {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"%s\"".formatted(ontologyFile.getName()));
        response.setContentLength((int) ontologyFile.length());

        try
        {
            FileCopyUtils.copy(responseInputStream, response.getOutputStream());
            return true;
        }
        catch (final IOException ioException)
        {
            return false;
        }
    }

    private boolean isInvalid(final ExportOntologyRequest request)
    {
        return (request.projectName() == null ||
            request.userId() == null ||
            request.fileName() == null ||
            request.rdfFormat() == null);
    }
}