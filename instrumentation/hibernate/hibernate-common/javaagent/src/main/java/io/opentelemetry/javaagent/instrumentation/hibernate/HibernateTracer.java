/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.hibernate;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.BaseInstrumenter;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HibernateTracer extends BaseInstrumenter {
  private static final HibernateTracer TRACER = new HibernateTracer();

  public static HibernateTracer tracer() {
    return TRACER;
  }

  public Span startSpan(Context context, String operationName, Object entity) {
    return startSpan(context, spanNameForOperation(operationName, entity));
  }

  public Span startSpan(Context context, String spanName) {
    return tracer.spanBuilder(spanName).setParent(context).startSpan();
  }

  private String spanNameForOperation(String operationName, Object entity) {
    if (entity != null) {
      String entityName = entityName(entity);
      if (entityName != null) {
        return operationName + " " + entityName;
      }
    }
    return operationName;
  }

  String entityName(Object entity) {
    if (entity == null) {
      return null;
    }
    String name = null;
    Set<String> annotations = new HashSet<>();
    for (Annotation annotation : entity.getClass().getDeclaredAnnotations()) {
      annotations.add(annotation.annotationType().getName());
    }

    if (entity instanceof String) {
      // We were given an entity name, not the entity itself.
      name = (String) entity;
    } else if (annotations.contains("javax.persistence.Entity")) {
      // We were given an instance of an entity.
      name = entity.getClass().getName();
    } else if (entity instanceof List && !((List) entity).isEmpty()) {
      // We have a list of entities.
      name = entityName(((List) entity).get(0));
    }

    return name;
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.javaagent.hibernate";
  }
}