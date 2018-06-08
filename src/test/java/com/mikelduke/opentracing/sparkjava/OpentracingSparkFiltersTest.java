package com.mikelduke.opentracing.sparkjava;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.jaegertracing.Tracer;
import io.jaegertracing.samplers.ConstSampler;
import io.jaegertracing.samplers.Sampler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import spark.Spark;

public class OpentracingSparkFiltersTest {

    private DummyJaegerReporter reporter = new DummyJaegerReporter();

    private Sampler sampler = new ConstSampler(true);

    private Tracer tracer = new Tracer.Builder("sparkjava-test")
            .withReporter(reporter)
            .withSampler(sampler)
            .build();

    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    @Before
    public void before() {
        reporter.setSpan(null);
        Spark.init();
        Spark.awaitInitialization();
    }

    @After
    public void after() {
        Spark.stop();
    }

    @Test
    public void spanIsNotNull() throws IOException {
        OpenTracingSparkFilters sparkTracingFilters = new OpenTracingSparkFilters(tracer);
        Spark.before(sparkTracingFilters.before());
		Spark.afterAfter(sparkTracingFilters.afterAfter());
        Spark.exception(Exception.class, sparkTracingFilters.exception());
        Spark.get("/", (req, res) -> "hello");
        
        Spark.awaitInitialization();

        int port = Spark.port();
        
        Request request = new Request.Builder()
                .url("http://localhost:" + port + "/hello")
                .build();

        client.newCall(request).execute();

        Assert.assertNotNull(reporter.getSpan());
    }

    // @Test
    // public void spanIsNullWithoutFilters() throws IOException {
    //     Spark.get("/", (req, res) -> "hello");
    //     Spark.awaitInitialization();

    //     int port = Spark.port();
        
    //     Request request = new Request.Builder()
    //             .url("http://localhost:" + port + "/hello")
    //             .build();

    //     client.newCall(request).execute();

    //     Assert.assertNull(reporter.getSpan());
    // }
}
