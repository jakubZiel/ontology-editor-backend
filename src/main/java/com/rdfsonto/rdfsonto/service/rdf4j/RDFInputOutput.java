package com.rdfsonto.rdfsonto.service.rdf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;


public abstract class RDFInputOutput
{
    protected final String USER_NAMESPACE = "https://www.user_neo4j.com#";
    protected final String USER_NAMESPACE_PREFIX = "un";

    protected Model model;
    protected Model outModel;
    protected final Set<String> knownNamespaces;

    public RDFInputOutput()
    {
        this.knownNamespaces = new HashSet<>();
    }

    protected abstract IRI handlePredicate(IRI predicate, String tag);

    protected abstract IRI handleSubject(IRI subject, String tag);

    protected abstract Value handleObject(Value object, String tag);

    protected BNode handleBNode(BNode bnode)
    {
        return null;
    }

    protected void loadModel(final Path outputFile, final RDFFormat rdfFormat) throws IOException
    {
        final var input = new FileInputStream(outputFile.toString());
        model = Rio.parse(input, "", rdfFormat);
    }

    protected boolean validate(Value object)
    {
        return !(object.isBNode() || object.isTriple());
    }

    protected String generateFileName(String fileName, String suffix) throws FileSystemException
    {
        final var components = fileName.split("\\.");

        if (components.length != 2)
        {
            throw new FileSystemException("Incorrect file name :" + fileName);
        }

        return components[0] + suffix + "." + components[1];
    }
}

