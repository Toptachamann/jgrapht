package org.jgrapht.perf.my_benchmakrs;

import org.jgrapht.Graph;
import org.jgrapht.alg.my_util.Util;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.openjdk.jmh.annotations.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Fork(value = 3, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
public class ClosestFirstIteratorPerformanceTest {

    public Map<Integer, Double> testDense(DenseGraphData data) {
        BetweennessCentrality<Integer, DefaultWeightedEdge> centrality = new BetweennessCentrality<>(data.graph);
        return centrality.getScores();
    }

    @Benchmark
    public Map<Integer, Double> testSparse(SparseGraphData data) {
        BetweennessCentrality<Integer, DefaultWeightedEdge> centrality = new BetweennessCentrality<>(data.graph);
        return centrality.getScores();
    }

    @State(Scope.Benchmark)
    public static class DenseGraphData {
        //        @Param({"400"})
        @Param({"30", "50", "100", "200", "400"})
        int graphSize;
        Graph<Integer, DefaultWeightedEdge> graph;

        @Setup(Level.Iteration)
        public void init() {
            graph = Util.generateComplete(graphSize, 100000);
        }
    }

    @State(Scope.Benchmark)
    public static class SparseGraphData {
        //        @Param({"100"})
        @Param({"50", "100", "300", "500", "1000", "1500", "2000"})
        int graphSize;
        Graph<Integer, DefaultWeightedEdge> graph;

        @Setup
        public void init() {
            graph = Util.generateTriangulation(graphSize, 100000, 100000, false, false);
        }
    }
}
