package com.rdfsonto.exportonto.rest;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.INVALID_REQUEST;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode;
import com.rdfsonto.exportonto.service.ExportOntologyService;
import com.rdfsonto.infrastructure.workspacemanagement.WorkspaceManagementService;
import com.rdfsonto.rdf4j.RdfFormatParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo4j/export")
public class ExportOntologyController
{
    private final ExportOntologyService exportOntologyService;
    private final WorkspaceManagementService workspaceManagementService;

    @PostMapping
    public ResponseEntity<?> exportRdfResource(@RequestBody final ExportOntologyRequest exportOntologyRequest, final HttpServletResponse response)
    {
        if (isInvalid(exportOntologyRequest))
        {
            log.warn("Invalid export ontology request: {}", exportOntologyRequest);
            return ResponseEntity.badRequest().body(INVALID_REQUEST);
        }

        final var rdfFormat = RdfFormatParser.parse(exportOntologyRequest.rdfFormat());

        final var ontologyExport = exportOntologyService.exportOntology(exportOntologyRequest.userId(), exportOntologyRequest.projectId(), rdfFormat);

        try
        {
            if (false)
            {
                throw new IOException();
            }
            /*attachFileToHttpResponse(response,
                rdfFormat,
                ontologyExport.exportedOntologyFile(),
                exportOntologyRequest.fileName(),
                ontologyExport.inputStream());
            workspaceManagementService.clearWorkspace(ontologyExport.exportId());*/
            return ResponseEntity.ok(exportOntologyRequest.fileName());

        }
        catch (final IOException ioException)
        {
            log.error("Error attaching a file to export ID: {} to http response", ontologyExport.exportId());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/get-file/{file}")
    public void getFile(@PathVariable final String file, final HttpServletResponse response) throws IOException
    {

        final var result = new BufferedInputStream(new FileInputStream(
            "/home/jzielins/Projects/ontology-editor-backend/workspace/" + file));

        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment; filename=" + file);

        final var out = response.getOutputStream();

        IOUtils.copy(result, out);
        out.close();
        result.close();

        /*final var responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(file));
        return new ResponseEntity<>(IOUtils.toByteArray(result), responseHeaders, HttpStatus.OK);*/
    }

    @ExceptionHandler(ClassNodeException.class)
    public ResponseEntity<?> handle(final ClassNodeException classNodeException)
    {

        if (classNodeException.getErrorCode() == ClassNodeExceptionErrorCode.INTERNAL_ERROR)
        {
            classNodeException.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        log.warn(classNodeException.getMessage());
        return ResponseEntity.badRequest().body(classNodeException.getErrorCode());
    }
// TODO
/*    private void attachFileToHttpResponse(final HttpServletResponse response,
                                          final RDFFormat rdfFormat,
                                          final File exportedOntologyFile,
                                          final String requestedFileName,
                                          final InputStream responseInputStream)
        throws IOException
    {
        final var fileNameWithExtension = "%s.%s".formatted(requestedFileName, rdfFormat.getDefaultFileExtension());

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"%s\"".formatted(fileNameWithExtension));
        response.setContentLength((int) exportedOntologyFile.length());

        FileCopyUtils.copy(responseInputStream, response.getOutputStream());
    }*/

    private boolean isInvalid(final ExportOntologyRequest request)
    {
        return (request.projectId() == null ||
            request.userId() == null ||
            request.fileName() == null ||
            request.rdfFormat() == null);
    }
}