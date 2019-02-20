/*
package org.jgrapht.alg.flow.mincost;

import org.jgrapht.alg.my_util.FlowMain;
import org.jgrapht.alg.interfaces.MinimumCostFlowAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(value = 15, warmups = 0)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4)
@Measurement(iterations = 15)
public class MinCostBenchmark {

    @Benchmark
    public MinimumCostFlowAlgorithm.MinimumCostFlow<DefaultWeightedEdge> TestCapacityScaling(Data data) {
        CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> flow = new CapacityScalingMinimumCostFlow<>(data.problem, data.scaling);
        return flow.getMinimumCostFlow();
    }


    @State(Scope.Benchmark)
    public static class Data {
        MinimumCostFlowProblem<Integer, DefaultWeightedEdge> problem;
        @Param({"0", "2", "4", "8", "16", "32", "64", "128"})
        int scaling;
        @Param({"500", "1200"})
        int n;
        @Param({"4000", "8000"})
        int m;

        @Setup
        public void init() {
            FlowMain.generateNetwork(n, m, 10, 10, 100000, 0, 10000, 0, 10000, true, true);
            this.problem = FlowMain.convert(FlowMain.readProblem());
        }
    }
}
*/
