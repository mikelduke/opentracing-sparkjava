package com.mikelduke.opentracing.sparkjava.testapp;

import io.jaegertracing.Span;
import io.jaegertracing.Tracer;
import io.jaegertracing.reporters.InMemoryReporter;
import io.jaegertracing.reporters.Reporter;
import io.jaegertracing.samplers.ConstSampler;
import io.jaegertracing.samplers.Sampler;
import io.opentracing.util.GlobalTracer;

public class JaegerOpenTracingTestApp {

	public static void main(String[] args) {

        Reporter reporter = new InMemoryReporter() {
            @Override
            public void report(Span span) {
                super.report(span);
                System.out.println("Span Reported: " + span + " Tags: " + span.getTags());
            }
        };
        
        Sampler sampler = new ConstSampler(true);
        Tracer tracer = new Tracer.Builder("sparkjava-test")
                .withReporter(reporter)
                .withSampler(sampler)
                .build();

        GlobalTracer.register(tracer);

        new OpenTracingSparkTestApplication().start();
	}
}
