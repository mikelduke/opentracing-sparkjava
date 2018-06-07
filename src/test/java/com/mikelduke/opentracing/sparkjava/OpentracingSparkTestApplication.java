package com.mikelduke.opentracing.sparkjava;

import java.util.logging.Level;
import java.util.logging.Logger;

import spark.Spark;

public class OpentracingSparkTestApplication {
    private static final String CLAZZ = OpentracingSparkTestApplication.class.getName();
	private static final Logger LOGGER = Logger.getLogger(CLAZZ);

    public static void main(String[] args) {
        OpentracingSparkFilters sparkTracingFilters = new OpentracingSparkFilters();
		Spark.before(sparkTracingFilters.before());
		Spark.afterAfter(sparkTracingFilters.afterAfter());

        Spark.exception(Exception.class, sparkTracingFilters.exception());

		Spark.get("/", (req, res) -> "hello");

		Spark.get("/error", (req, res) -> {
			throw new RuntimeException("test exceotion");
		});

		Spark.awaitInitialization();
		LOGGER.logp(Level.INFO, CLAZZ, "main", "Server started on port " + Spark.port());
	}
}
