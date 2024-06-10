package org.edu_sharing.repository.client.tools.metadata;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations =  1)
@Measurement(iterations = 1)
@Fork(value = 1)
@State(Scope.Benchmark)
public class ValueToolPerformanceTest {
     public static void main(String[] args) throws Exception {
            org.openjdk.jmh.Main.main(args);
    }
    @Benchmark
    public void getMultivalue() {
        ValueTool.getMultivalue("a");
        ValueTool.getMultivalue("a[#]b");
    }
}