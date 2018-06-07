package com.mikelduke.opentracing.sparkjava;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import spark.ExceptionHandler;
import spark.Filter;
import spark.Request;

public class OpentracingSparkFilters {

	public static final String SERVER_SPAN = OpentracingSparkFilters.class.getName() + ".activeSpanContext";

	private final Tracer tracer;

	public OpentracingSparkFilters() {
		this(GlobalTracer.get());
	}

	public OpentracingSparkFilters(Tracer tracer) {
		this.tracer = tracer;
	}
	
	public Filter before() {
		return (request, response) -> {
			if (request.attribute(SERVER_SPAN) != null) {
				return;
			}

			Map<String, String> headerMap = new HashMap<>();
			request.headers().forEach(h -> headerMap.put(h, request.headers(h)));

			SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS,
					new TextMapExtractAdapter(headerMap));

			Span span = tracer
					.buildSpan(request.requestMethod())
					.asChildOf(parentSpan)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
					.withTag(Tags.COMPONENT.getKey(), "sparkjava")
					.withTag(Tags.HTTP_METHOD.getKey(), request.requestMethod())
					.withTag(Tags.HTTP_URL.getKey(), request.url())
					.start();

			request.attribute(SERVER_SPAN, span);
		};
	}

	public Filter afterAfter() {
		return (req, res) -> {
			Span span = req.attribute(SERVER_SPAN);
			Tags.HTTP_STATUS.set(span, res.status());

			if (span == null) return;
			span.finish();
		};
	}

	public <T extends Exception> ExceptionHandler<T> exception() {
		return exception(null);
	}

	public <T extends Exception> ExceptionHandler<T> exception(ExceptionHandler<T> delegate) {
		return (exception, request, response) -> {
			// Handle the exception here
			Span span = request.attribute(SERVER_SPAN);
			if (span == null) return;

			Tags.ERROR.set(span, Boolean.TRUE);
			span.log(logsForException(exception));
			
			if (delegate != null) {
				delegate.handle(exception, request, response);
			}
		};
	}

	public static SpanContext serverSpanContext(Request request) {
		return request.attribute(SERVER_SPAN);
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
