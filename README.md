# opentracing-sparkjava
OpenTracing Instrumentation for SparkJava

This repo contains tracing filters and an exception handler for [SparkJava](http://sparkjava.com/).
The filters extract tracing headers from incoming requests to create
[OpenTracing](http://opentracing.io) spans. These spans can be exported to 
a variety of backends including Jaeger, Hawkular, and others.

To enable tracing you need to add `before`, `exception` and `afterAfter`
hooks:
```java
OpenTracingSparkFilters sparkTracingFilters = new OpenTracingSparkFilters(tracer);
Spark.before(sparkTracingFilters.before());
Spark.afterAfter(sparkTracingFilters.afterAfter());
Spark.exception(sparkTracingFilters.exception());

// tracing is added for all routes
Spark.get("/hello", (req, res) -> "hello world");
```

To access the current span in a resource retrieve it from the request attributes
using `OpenTracingSparkFilters.SERVER_SPAN`:
```java
Spark.get("/path", (req, res) -> {
    Span span = req.attribute(OpenTracingSparkFilters.SERVER_SPAN);
    tracer.buildSpan("child").asChildOf(span).withTag("test", "value").start().finish();

    //do stuff
    return "hello world";
});
```
