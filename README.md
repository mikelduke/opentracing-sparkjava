# opentracing-sparkjava
OpenTracing Instrumentation for SparkJava

This repo contains tracing filters and an exception handler for [SparkJava](http://sparkjava.com/).
The filters extract tracing headers from incoming requests to create
[OpenTracing](http://opentracing.io) spans. These spans can be exported to 
a variety of backends including Jaeger, Hawkular, and others.

To enable tracing you need to add `before`, `exception` and `afterAfter`
hooks:
```java
OpentracingSparkFilters sparkTracingFilters = new OpentracingSparkFilters(tracer);
Spark.before(sparkTracingFilters.before());
Spark.exception(sparkTracingFilters.exception());
Spark.afterAfter(sparkTracingFilters.afterAfter());

// tracing is added for all routes
Spark.get("/hello", (req, res) -> "hello world");
```
