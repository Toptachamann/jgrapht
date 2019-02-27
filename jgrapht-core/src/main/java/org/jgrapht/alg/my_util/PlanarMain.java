package org.jgrapht.alg.my_util;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.PlanarityTestingAlgorithm;
import org.jgrapht.alg.planar.LeftRightPlanarityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;



public class PlanarMain {
    public static void main(String[] args) {
//        testKuratowski(8, 10000);
        custom(Util.readGraph(Util.EDGE_LIST_PATH));
//        debug();
//        testSubdivision();
    }

    public static void testSubdivision() {
        Graph<Integer, DefaultEdge> graph = Util.readGraph(Util.PLANAR_GRAPH_PATH);
        System.out.println("Is K_33 subdivision: " + LeftRightPlanarityInspector.isK33Subdivision(graph));
    }

    public static void custom(Graph<Integer, DefaultEdge> graph) {
        LeftRightPlanarityInspector inspector = new LeftRightPlanarityInspector(graph);
        boolean planar = inspector.isPlanar();
        System.out.println("Graph is planar = " + planar);
        if (planar) {
            System.out.println(inspector.getEmbedding());
        }
    }


    public static void debug() {
        Graph<Integer, DefaultEdge> graph = Util.readGraph(Util.EDGE_LIST_PATH);
        testOnGraph(graph, 1);
    }

    public static void test(int vertexNum, int testcaseNum) {
        for (int i = 0; i < testcaseNum; i++) {
            Graph<Integer, DefaultEdge> graph = generateGraph1(vertexNum);
            Util.exportSimple(graph, false);
            Graph<Integer, DefaultEdge> testGraph = Util.readGraph(Util.EDGE_LIST_PATH);
            testOnGraph(testGraph, i + 1);
        }
    }

    public static void testKuratowski(int vertexNum, int testcaseNum) {
        for (int i = 0; i < testcaseNum; ) {
            Graph<Integer, DefaultEdge> graph = generateGraph1(vertexNum);
            Util.exportSimple(graph, false);
            Graph<Integer, DefaultEdge> testGraph = Util.readGraph(Util.EDGE_LIST_PATH);
            if (testOnGraphKuratowski(testGraph, i + 1)) {
                ++i;
            }
        }
    }

    public static boolean checkIsPlanar() {
        try {
            String command = "C:\\C++_Projects\\lemon_benchmark\\cmake-build-release\\src\\planarity_tester.exe";
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(command);
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
//            System.out.println(line);
            if (line.equals("Graph is planar")) {
                return true;
            } else if (line.equals("Graph is not planar")) {
                return false;
            } else {
                throw new RuntimeException("Solver is working incorrectly");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static boolean testOnGraphKuratowski(Graph<Integer, DefaultEdge> graph, int testCase) {
        LeftRightPlanarityInspector inspector = new LeftRightPlanarityInspector(graph);
        boolean planar = inspector.isPlanar();
        boolean actPlanar = checkIsPlanar();
        assert planar == actPlanar;
        if (!planar) {
            System.out.printf("%d. Graph is not planar\n", testCase);
            return true;
        }
        return false;
    }

    public static void testOnGraph(Graph<Integer, DefaultEdge> graph, int testCase) {
        LeftRightPlanarityInspector inspector = new LeftRightPlanarityInspector(graph);
        boolean planar = inspector.isPlanar();
        boolean actPlanar = checkIsPlanar();
        if (planar) {
            System.out.printf("%d. Graph is planar\n", testCase);
            PlanarityTestingAlgorithm.Embedding<Integer, DefaultEdge> embedding = inspector.getEmbedding();
//            System.out.println(embedding);
            int sum = 0;
            for (Integer vertex : graph.vertexSet()) {
                sum += embedding.getEdgesAround(vertex).size();
            }
            assert sum == 2 * graph.edgeSet().size();
        } else {
            System.out.printf("%d. Graph is not planar\n", testCase);
        }
        assert planar == actPlanar;
    }

    public static Graph<Integer, DefaultEdge> generateGraph1(int n) {
        Graph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int i = 0; i < n; i++) {
            graph.addVertex(i);
        }
        Random random = new Random();
        int edgeNum = 0, toAdd = (int) (1.8 * n);
        for (int i = 0; i < 2; i++) {
            for (int from = 0; from < n && edgeNum < toAdd; from++) {
                int added = 0;
                int attempts = 0;
                while (added < 1 && attempts < n) {
                    int to = random.nextInt(n);
                    if (from != to && !graph.containsEdge(from, to)) {
                        ++added;
                        ++edgeNum;
                        graph.addEdge(from, to);
                    }
                    ++attempts;
                }
            }
        }
        return graph;
    }
}
