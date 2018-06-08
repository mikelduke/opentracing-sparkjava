package com.mikelduke.opentracing.sparkjava;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import spark.Request;
import spark.Response;

public class DefaultTagDecorator implements OpenTracingTagDecorator {

    @Override
    public void before(Request req, Response res, Span span) {
        Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
        Tags.COMPONENT.set(span, "sparkjava");
        Tags.HTTP_METHOD.set(span, req.requestMethod());
        Tags.HTTP_URL.set(span, req.uri());
    }

    @Override
    public void after(Request req, Response res, Span span) {
        Tags.HTTP_STATUS.set(span, res.status());
    }

    @Override
    public void exception(Request req, Response res, Span span, Exception e) {
        Tags.ERROR.set(span, Boolean.TRUE);
		span.log(logsForException(e));
    }

    private Map<String, String> logsForException(Throwable throwable) {
		Map<String, String> errorLog = new HashMap<>(3);
		errorLog.put("event", Tags.ERROR.getKey());

		String message = throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
		if (message != null) {
			errorLog.put("message", message);
		}
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		errorLog.put("stack", sw.toString());

		return errorLog;
	}
}
