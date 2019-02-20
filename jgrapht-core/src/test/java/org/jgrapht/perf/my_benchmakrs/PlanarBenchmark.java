/*
package org.jgrapht.perf.my_benchmakrs;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.PlanarityTestingAlgorithm;
import org.jgrapht.alg.my_util.Util;
import org.jgrapht.alg.planar.LeftRightPlanarityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(value = 5, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
public class PlanarBenchmark {

    @Benchmark
    public boolean benchmarkTesting(Data data){
        LeftRightPlanarityInspector<Integer, DefaultEdge> inspector = new LeftRightPlanarityInspector<>(data.graph);
        return inspector.isPlanar();
    }

    @Benchmark
    public PlanarityTestingAlgorithm.Embedding<Integer, DefaultEdge> benchmarkEmbedding(Data data) {
        LeftRightPlanarityInspector<Integer, DefaultEdge> inspector = new LeftRightPlanarityInspector<>(data.graph);
        return inspector.getEmbedding();
    }

    @State(Scope.Benchmark)
    public static class Data {
        Graph<Integer, DefaultEdge> graph;
        //        @Param({"10", "50", "100", "400", "700", "1000", "3000", "10000"})
        @Param({"10", "2000", "4000", "6000", "8000", "10000"})
        int graphSize;

        @Setup(Level.Iteration)
        public void init(){
            Util.generateTriangulation(graphSize, 200, 200, false, false);
            graph = Util.readGraph(Util.EDGE_LIST_PATH);
        }
    }
}
*/
