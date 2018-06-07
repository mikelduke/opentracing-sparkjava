package com.mikelduke.opentracing.sparkjava;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import spark.Spark;

public class OpentracingSparkTestApplication {
    private static final String CLAZZ = OpentracingSparkTestApplication.class.getName();
	private static final Logger LOGGER = Logger.getLogger(CLAZZ);

	private final Tracer tracer;

	public static void main(String[] args) {
		new OpentracingSparkTestApplication().start();
	}

	public OpentracingSparkTestApplication() {
		this(GlobalTracer.get());
	}

	public OpentracingSparkTestApplication(Tracer tracer) {
		this.tracer = tracer;
	}

	public void start() {
		OpentracingSparkFilters sparkTracingFilters = new OpentracingSparkFilters();
		Spark.before(sparkTracingFilters.before());
		Spark.afterAfter(sparkTracingFilters.afterAfter());
        Spark.exception(Exception.class, sparkTracingFilters.exception());

		Spark.get("/", (req, res) -> "hello");

		Spark.get("/bad", (req, res) -> {
			res.status(400);
			res.body("bad request");
			return res;
		});

		Spark.get("/child", (req, res) -> {
			Span span = req.attribute(OpentracingSparkFilters.SERVER_SPAN);
			tracer.buildSpan("child").asChildOf(span).withTag("test", "value").start().finish();

			return "done";
		  });

		Spark.get("/error", (req, res) -> {
			throw new RuntimeException("test exceotion");
		});

		Spark.awaitInitialization();
		LOGGER.logp(Level.INFO, CLAZZ, "main", "Server started on port " + Spark.port());
	}
}
