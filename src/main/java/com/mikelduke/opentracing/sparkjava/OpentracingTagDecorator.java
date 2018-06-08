package com.mikelduke.opentracing.sparkjava;

import io.opentracing.Span;
import spark.Request;
import spark.Response;

public interface OpenTracingTagDecorator {
    public void before(Request req, Response res, Span span);
    public void after(Request req, Response res, Span span);
    public void exception(Request req, Response res, Span span, Exception e);
}
