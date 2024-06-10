package org.edu_sharing.repository.server.tools;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations =  1)
@Measurement(iterations = 1)
@Fork(value = 1)
@State(Scope.Benchmark)
public class NameSpaceToolPerformanceTest {
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
    @Benchmark
    public void transformToShortQName() {
        NameSpaceTool.transformToShortQName(CCConstants.CM_NAME);
        NameSpaceTool.transformToShortQName("cm:name");
    }
}