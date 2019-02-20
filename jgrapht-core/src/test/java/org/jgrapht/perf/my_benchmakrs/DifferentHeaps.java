/*
package org.jgrapht.alg.matching.blossom.v5;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jheaps.MergeableAddressableHeap;
import org.jheaps.tree.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.State;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Fork(value = 8, warmups = 1)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 8)
public class DifferentHeaps {

    @Benchmark
    public MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> test(Data data) {
        KolmogorovWeightedPerfectMatching.edgeHeapSupplier = data.edgeHeapSuppliers.get(data.supplierNum);
        KolmogorovWeightedPerfectMatching.nodeHeapSupplier = data.nodeHeapSuppliers.get(data.supplierNum);
        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> matching = new KolmogorovWeightedPerfectMatching<>(data.graph, data.allOptions[data.optionsNum]);
        return matching.getMatching();
    }

    @State(Scope.Benchmark)
    public static class Data {
        //@Param({"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"})
        public int optionsNum = 9;
        public int vertexNum = 1000;
        @Param({"3000", "10000", "50000", "100000", "200000", "400000"})
        //@Param({"3000", "10000", "50000", "100000"})
        //@Param({"200000", "400000"})
        public int edgeNum;
        //@Param({"0", "1", "2", "3", "4", "5"})
        @Param({"1", "4"})
        public int supplierNum;
        public int weigthUpperBound = 1000000;
        public List<Supplier<MergeableAddressableHeap<Double, BlossomVEdge>>> edgeHeapSuppliers = Arrays.asList(FibonacciHeap::new, PairingHeap::new, LeftistHeap::new, SimpleFibonacciHeap::new, CostlessMeldPairingHeap::new, SkewHeap::new);
        public List<Supplier<MergeableAddressableHeap<Double, BlossomVNode>>> nodeHeapSuppliers = Arrays.asList(FibonacciHeap::new, PairingHeap::new, LeftistHeap::new, SimpleFibonacciHeap::new, CostlessMeldPairingHeap::new, SkewHeap::new);
        public BlossomVOptions[] allOptions = BlossomVOptions.ALL_OPTIONS;
        private Graph<Integer, DefaultWeightedEdge> graph;

        @Setup(Level.Iteration)
        public void setup() {
            System.out.println("Generated");
            graph = MatchingMain.generateWithPerfectMatching(vertexNum, edgeNum, weigthUpperBound, true);
        }
    }
}
*/
