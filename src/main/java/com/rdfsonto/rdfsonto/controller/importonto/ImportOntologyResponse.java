package com.rdfsonto.rdfsonto.controller.importonto;

import java.util.Map;

import lombok.Builder;
import lombok.RequiredArgsConstructor;


@Builder(setterPrefix = "with")
@RequiredArgsConstructor
public class ImportOntologyResponse
{
    private final Long triplesLoaded;
    private final Map<String, String> namespaces;
}