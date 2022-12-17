package com.rdfsonto.rdfsonto.controller.classnode;

import com.rdfsonto.rdfsonto.service.classnode.ClassNode;

import lombok.Builder;


public record NodeChangeEvent(long nodeId, ClassNode body, ChangeEventType type)
{
    public enum ChangeEventType
    {
        DELETE, UPDATE, CREATE
    }
}