package com.mikelduke.opentracing.sparkjava;

import io.jaegertracing.Span;
import io.jaegertracing.reporters.InMemoryReporter;

public class DummyJaegerReporter extends InMemoryReporter {

    private Span span = null;

    @Override
    public void report(Span span) {
        super.report(span);
        this.span = span;
    }

    public Span getSpan() {
        return this.span;
    }

	public void setSpan(Span span) {
        this.span = span;
	}
}
