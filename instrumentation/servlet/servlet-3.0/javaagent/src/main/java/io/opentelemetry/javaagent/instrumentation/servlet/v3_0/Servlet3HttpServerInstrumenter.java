/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.servlet.ServletHttpServerInstrumenter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlet3HttpServerInstrumenter
    extends ServletHttpServerInstrumenter<HttpServletResponse> {

  private static final Servlet3HttpServerInstrumenter TRACER = new Servlet3HttpServerInstrumenter();

  public static Servlet3HttpServerInstrumenter tracer() {
    return TRACER;
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.javaagent.servlet";
  }

  @Override
  protected Integer peerPort(HttpServletRequest connection) {
    return connection.getRemotePort();
  }

  @Override
  protected int responseStatus(HttpServletResponse httpServletResponse) {
    return httpServletResponse.getStatus();
  }

  @Override
  public void endExceptionally(
      Context context, Throwable throwable, HttpServletResponse response, long timestamp) {
    if (response.isCommitted()) {
      super.endExceptionally(context, throwable, response, timestamp);
    } else {
      // passing null response to super, in order to capture as 500 / INTERNAL, due to servlet spec
      // https://javaee.github.io/servlet-spec/downloads/servlet-4.0/servlet-4_0_FINAL.pdf:
      // "If a servlet generates an error that is not handled by the error page mechanism as
      // described above, the container must ensure to send a response with status 500."
      super.endExceptionally(context, throwable, null, timestamp);
    }
  }

  @Override
  public void end(Context context, HttpServletResponse response, long timestamp) {
    super.end(context, response, timestamp);
  }

  public void onTimeout(Context context, long timeout) {
    Span span =
        io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.spanFromContext(context);
    span.setStatus(StatusCode.ERROR);
    span.setAttribute("servlet.timeout", timeout);
    span.end();
  }

  /*
  Given request already has a context associated with it.
  As there should not be nested spans of kind SERVER, we should NOT create a new span here.

  But it may happen that there is no span in current Context or it is from a different trace.
  E.g. in case of async servlet request processing we create span for incoming request in one thread,
  but actual request continues processing happens in another thread.
  Depending on servlet container implementation, this processing may again arrive into this method.
  E.g. Jetty handles async requests in a way that calls HttpServlet.service method twice.

  In this case we have to put the span from the request into current context before continuing.
  */
  public static boolean needsRescoping(Context attachedContext) {
    return !sameTrace(
        io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.spanFromContext(
            Context.current()),
        io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge.spanFromContext(
            attachedContext));
  }

  private static boolean sameTrace(Span oneSpan, Span otherSpan) {
    return oneSpan
        .getSpanContext()
        .getTraceIdAsHexString()
        .equals(otherSpan.getSpanContext().getTraceIdAsHexString());
  }
}