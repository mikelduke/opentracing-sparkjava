package com.mikelduke.opentracing.sparkjava;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.util.GlobalTracer;
import spark.ExceptionHandler;
import spark.Filter;

public class OpentracingSparkFilters {

	private final Tracer tracer;

	public OpentracingSparkFilters() {
		this(GlobalTracer.get());
	}

	public OpentracingSparkFilters(Tracer tracer) {
		this.tracer = tracer;
	}
	
	public Filter before() {
		return (request, response) -> {
			Map<String, String> headerMap = new HashMap<>();
			request.headers().forEach(h -> headerMap.put(h, request.headers(h)));

			SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS,
					new TextMapExtractAdapter(headerMap));

			Span span;
			if(parentSpan == null){
				span = tracer.buildSpan(request.requestMethod()).start();
			} else {
				span = tracer.buildSpan(request.requestMethod()).asChildOf(parentSpan).start();
			}

			request.attribute(Span.class.getName(), span);
		};
	}

	public Filter afterAfter() {
		return (req, res) -> {
			Span span = req.attribute(Span.class.getName());

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
			Span span = request.attribute(Span.class.getName());

			if (span == null) return;
			span.setTag("error", true);
			
			if (delegate != null) {
				delegate.handle(exception, request, response);
			}
		};
	}
}
