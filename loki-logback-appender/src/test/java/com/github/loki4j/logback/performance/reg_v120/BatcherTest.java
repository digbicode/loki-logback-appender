package com.github.loki4j.logback.performance.reg_v120;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;

import com.github.loki4j.common.batch.Batcher;
import com.github.loki4j.common.batch.LogRecord;
import com.github.loki4j.common.batch.LogRecordBatch;
import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;

public class BatcherTest {

    private static AbstractLoki4jEncoder initEnc(AbstractLoki4jEncoder e) {
        e.setContext(new LoggerContext());
        e.start();
        return e;
    }

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var batchSize = 1000;
        var batch = new LogRecordBatch(batchSize);

        var stats = Benchmarker.run(new Benchmarker.Config<LogRecord>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> {
                var jsonEncSta = initEnc(jsonEncoder(true, "testLabel"));
                return Arrays
                    .stream(generateEvents(100_000, 10))
                    .map(e -> eventToRecord(e, jsonEncSta))
                    .iterator();
            };
            this.benchmarks = Arrays.asList(
                Benchmark.of("v110 add",
                    () -> new BatcherV110(batchSize, 60 * 1000),
                    (b, r) -> b.add(r, batch)),
                Benchmark.of("v120a add",
                    () -> new BatcherV120a(batchSize, 10000, 60 * 1000),
                    (b, r) -> b.add(r, batch)),
                Benchmark.of("new add",
                    () -> new Batcher(batchSize, 10000, 60 * 1000),
                    (b, r) -> b.add(r, batch)),
                Benchmark.of("v120a sizeCheck & add",
                    () -> new BatcherV120a(batchSize, 10000, 60 * 1000),
                    (b, r) ->{
                        b.checkSizeBeforeAdd(r, batch);
                        b.add(r, batch);
                    }),
                Benchmark.of("new sizeCheck & add",
                    () -> new Batcher(batchSize, 10000, 60 * 1000),
                    (b, r) -> {
                        b.checkSizeBeforeAdd(r, batch);
                        b.add(r, batch);
                    })
            );
        }});

        stats.forEach(System.out::println);
    }
}
