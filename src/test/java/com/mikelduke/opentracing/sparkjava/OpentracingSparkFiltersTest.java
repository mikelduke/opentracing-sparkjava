package com.mikelduke.opentracing.sparkjava;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.jaegertracing.Tracer;
import io.jaegertracing.samplers.ConstSampler;
import io.jaegertracing.samplers.Sampler;
import io.opentracing.tag.Tags;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import spark.Spark;

public class OpentracingSparkFiltersTest {

    private static DummyJaegerReporter reporter = new DummyJaegerReporter();

    private static Sampler sampler = new ConstSampler(true);

    private static Tracer tracer = new Tracer.Builder("sparkjava-test")
            .withReporter(reporter)
            .withSampler(sampler)
            .build();

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private static String host = "";

    @BeforeClass
    public static void beforeClass() {
        OpenTracingSparkFilters sparkTracingFilters = new OpenTracingSparkFilters(tracer);
        Spark.before(sparkTracingFilters.before());
		Spark.afterAfter(sparkTracingFilters.afterAfter());
        Spark.exception(Exception.class, sparkTracingFilters.exception());

        Spark.get("/", (req, res) -> "hello");
        Spark.get("/error", (req, res) -> {
			throw new RuntimeException("test exceotion");
		});

        Spark.awaitInitialization();

        host = "http://localhost:" + Spark.port() + "/";
    }

    @AfterClass
    public static void afterClass() {
        Spark.stop();
    }

    @Before
    public void beforeEach() {
        reporter.setSpan(null);
    }
    
    @Test
    public void spanIsNotNull() throws IOException {
        Request request = new Request.Builder()
                .url(host + "/hello")
                .build();

        client.newCall(request).execute();

        Assert.assertNotNull(reporter.getSpan());
    }

    @Test
    public void spanHasHttpMethod() throws IOException {
        Request request = new Request.Builder()
                .url(host + "/hello")
                .build();

        client.newCall(request).execute();

        Assert.assertTrue("GET".equalsIgnoreCase((String) reporter.getSpan().getTags().get(Tags.HTTP_METHOD.getKey())));
    }

    @Test
    public void spanShowsError() throws IOException {
        Request request = new Request.Builder()
                .url(host + "/error")
                .build();

        client.newCall(request).execute();

        Assert.assertTrue(Boolean.TRUE.equals((Boolean) reporter.getSpan().getTags().get(Tags.ERROR.getKey())));
    }

    @Test
    public void spanHasUrl() throws IOException {
        Request request = new Request.Builder()
                .url(host + "/hello")
                .build();

        client.newCall(request).execute();

        String url = (String) reporter.getSpan().getTags().get(Tags.HTTP_URL.getKey());

        Assert.assertTrue(url.contains("hello"));
    }
}
