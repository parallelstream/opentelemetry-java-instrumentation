/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.httpclients;

import static io.opentelemetry.instrumentation.spring.httpclients.RestTemplateTracer.TRACER;

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Wraps RestTemplate requests in a span. Adds the current span context to request headers. */
public final class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

  private final Tracer tracer;

  public RestTemplateInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    Span span = TRACER.startSpan(request);
    try (Scope scope = TRACER.startScope(span, request.getHeaders())) {
      ClientHttpResponse response = execution.execute(request, body);
      TRACER.end(span, response);
      return response;
    }
  }
}
