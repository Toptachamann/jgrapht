/*
package org.jgrapht.perf.matching.blossom.v5;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.blossom.v5.BlossomVOptions;
import org.jgrapht.alg.matching.blossom.v5.KolmogorovWeightedPerfectMatching;
import org.jgrapht.alg.matching.blossom.v5.MatchingMain;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(value = 15, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 25)
public class PerfectMatchingPerformanceTest {

    @Benchmark
    public MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> testEdmonds(Data data){
        EdmondsMaximumCardinalityMatching<Integer, DefaultWeightedEdge> matching = new EdmondsMaximumCardinalityMatching<>(data.graph);
        return matching.getMatching();
    }

    @Benchmark
    public MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> testBlossomV(Data data){
        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> matching = new KolmogorovWeightedPerfectMatching<>(data.graph, BlossomVOptions.ALL_OPTIONS[21]);
        return matching.getMatching();
    }

    @State(Scope.Benchmark)
    public static class Data {
        //@Param({"1000", "3000", "5000", "10000"})
        @Param({"100", "300", "500"})
        public int graphSize;
        Graph<Integer, DefaultWeightedEdge> graph;

        @Setup(Level.Iteration)
        public void init() {
            this.graph = MatchingMain.generateComplete(graphSize, 10000);
        }
    }
}
*/
