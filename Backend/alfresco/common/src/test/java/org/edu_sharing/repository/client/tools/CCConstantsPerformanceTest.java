package org.edu_sharing.repository.client.tools;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations =  1)
@Measurement(iterations = 1)
@Fork(value = 1)
@State(Scope.Benchmark)
public class CCConstantsPerformanceTest {
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
    @Benchmark
    public void getValidGlobalName() {
        CCConstants.getValidGlobalName("cm:name");
    }
    @Benchmark
    public void getValidLocalName() {
        CCConstants.getValidLocalName(CCConstants.CM_NAME);
    }
}