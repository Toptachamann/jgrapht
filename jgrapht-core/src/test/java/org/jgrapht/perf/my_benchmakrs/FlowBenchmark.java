/*
package org.jgrapht.perf.my_benchmakrs;

import org.jgrapht.alg.my_util.FlowMain;
import org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow;
import org.jgrapht.alg.flow.mincost.MinimumCostFlowProblem;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(value = 5, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 10)
public class FlowBenchmark {


    @Benchmark
    public double test(Data data) {
        CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> flow = new CapacityScalingMinimumCostFlow<>(8);
        return flow.getMinimumCostFlow(data.problem).getCost();
    }

    @State(Scope.Benchmark)
    public static class Data {
        @Param({"100", "300", "500", "1000", "1500", "2000", "3000", "5000"})
        public int vertexNum;
        MinimumCostFlowProblem<Integer, DefaultWeightedEdge> problem;

        @Setup
        public void init() {
            Random random = new Random();
            FlowMain.generateNetwork(vertexNum, 5 * vertexNum, vertexNum / 20,
                    vertexNum / 20, 100 * vertexNum, 1, 100 * vertexNum,
                    1, 10 * vertexNum, random.nextInt(100000), 100, false, false);
            problem = FlowMain.convert(FlowMain.readProblem());
        }
    }
}
*/
