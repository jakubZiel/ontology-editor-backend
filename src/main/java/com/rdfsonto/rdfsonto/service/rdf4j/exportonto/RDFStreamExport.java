package com.rdfsonto.rdfsonto.service.rdf4j.exportonto;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import com.rdfsonto.rdfsonto.service.rdf4j.RDFInputOutput;


public class RDFStreamExport extends RDFInputOutput implements RDFHandler
{
    @Override
    protected IRI handlePredicate(final IRI predicate, final String tag)
    {
        return null;
    }

    @Override
    protected IRI handleSubject(final IRI subject, final String tag)
    {
        return null;
    }

    @Override
    protected Value handleObject(final Value object, final String tag)
    {
        return null;
    }

    @Override
    public void startRDF() throws RDFHandlerException
    {

    }

    @Override
    public void endRDF() throws RDFHandlerException
    {

    }

    @Override
    public void handleNamespace(final String prefix, final String uri) throws RDFHandlerException
    {

    }

    @Override
    public void handleStatement(final Statement st) throws RDFHandlerException
    {

    }

    @Override
    public void handleComment(final String comment) throws RDFHandlerException
    {

    }
}
