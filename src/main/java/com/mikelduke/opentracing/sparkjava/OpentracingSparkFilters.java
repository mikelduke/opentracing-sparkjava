package com.mikelduke.opentracing.sparkjava;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.util.GlobalTracer;
import spark.ExceptionHandler;
import spark.Filter;
import spark.Request;

public class OpenTracingSparkFilters {

	public static final String SERVER_SPAN = OpenTracingSparkFilters.class.getName() + ".activeSpanContext";

	private final Tracer tracer;
	private final List<OpenTracingTagDecorator> decorators;

	public OpenTracingSparkFilters() {
		this(GlobalTracer.get());
	}

	public OpenTracingSparkFilters(Tracer tracer) {
		this(tracer, Collections.singletonList(new DefaultTagDecorator()));
	}

	public OpenTracingSparkFilters(Tracer tracer, List<OpenTracingTagDecorator> decorators) {
		this.tracer = tracer;
		this.decorators = Collections.unmodifiableList(new ArrayList<>(decorators));
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
					.start();
			
			for(OpenTracingTagDecorator decorator : decorators) {
				decorator.before(request, response, span);
			}

			request.attribute(SERVER_SPAN, span);
		};
	}

	public Filter afterAfter() {
		return (req, res) -> {
			Span span = req.attribute(SERVER_SPAN);
			if (span == null) return;

			for (OpenTracingTagDecorator decorator : decorators) {
				decorator.after(req, res, span);
			}

			span.finish();
		};
	}

	public <T extends Exception> ExceptionHandler<T> exception() {
		return exception(null);
	}

	public <T extends Exception> ExceptionHandler<T> exception(ExceptionHandler<T> delegate) {
		return (exception, request, response) -> {
			Span span = request.attribute(SERVER_SPAN);
			if (span == null) return;

			for (OpenTracingTagDecorator decorator : decorators) {
				decorator.exception(request, response, span, exception);
			}

			if (delegate != null) {
				delegate.handle(exception, request, response);
			}
		};
	}

	public static SpanContext serverSpanContext(Request request) {
		return request.attribute(SERVER_SPAN);
	}
}
