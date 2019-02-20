/*
package org.jgrapht.alg.my_util;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow;
import org.jgrapht.alg.flow.mincost.MinimumCostFlowProblem;
import org.jgrapht.alg.interfaces.MinimumCostFlowAlgorithm;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow.CAP_INF;

public class FlowMain {
    private static final String NETGEN_PROBLEM = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\min_cost\\problem_netgen.txt";
    private static final String NETGEN_EXE = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\netgen\\cmake-build-release\\netgen.exe";
    private static final String LEMON_SOLVER = "C:\\C++_Projects\\Trial\\release\\LEMON_SOLVER.exe";
    private static final String LEMON_SOLUTION = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\min_cost\\solution_lemon.txt";
    private static final String MY_CODE_SOLUTION = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\min_cost\\my_code_solution.txt";
    private static final String TEST_CASE = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\min_cost\\arr.txt";


    public static void main(String[] args) throws IOException, InterruptedException {
        MinimumCostFlowProblem<Integer, DefaultWeightedEdge> problem = convert(readProblem());
        CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> flow = new CapacityScalingMinimumCostFlow<>(4);
        System.out.println(flow.getMinimumCostFlow(problem).getCost());
    }


    public static long measureTime(MinimumCostFlowProblem<Integer, DefaultWeightedEdge> problem, int warmup, int measurement) {
        long sum = 0;
        for (int i = 0; i < warmup; i++) {
            measureTime(problem);
        }
        for (int i = 0; i < measurement; i++) {
            sum += measureTime(problem);
        }
        System.out.println("" +
                "Average time = " + getTime(sum / measurement));
        return sum / measurement;
    }

    public static long measureTime(MinimumCostFlowProblem<Integer, DefaultWeightedEdge> problem) {
        long start = System.nanoTime();
        CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> flow = new CapacityScalingMinimumCostFlow<>(1);
        flow.getFlowCost(problem);
        long end = System.nanoTime();
        System.out.println("Time = " + getTime(end - start));
        return start - end;
    }

    public static void testMinCostFlow(int testCaseNum, boolean withLowerBounds, boolean withNegativeCosts) {
        for (int i = 0; i < testCaseNum; i++) {
            System.out.print("Test case " + (i + 1) + ", result = ");
            generateNetwork(1000, 20000, 100, 100, 10000, 1, 100000, 1, 10, withLowerBounds, withNegativeCosts);
            double res = solveProblem();
            if (res == -1) {
                System.out.println("INFEASIBLE");
                try {
                    testOnProblem(convert(readProblem()));
                    throw new RuntimeException();
                } catch (IllegalArgumentException e) {

                }
            } else {
                double myRes = testOnProblem(convert(readProblem()));
                if (Math.abs(res - myRes) > 10e-9) {
                    throw new RuntimeException();
                }
            }
        }
    }

    public static <V, E> MinimumCostFlowProblem<V, E> convert(Problem<V, E> problem) {
        return new MinimumCostFlowProblem.MinimumCostFlowProblemImpl<>(problem.graph, v -> problem.supplyMap.getOrDefault(v, 0),
                e -> problem.upperBoundCapacityMap.getOrDefault(e, 0), e -> problem.lowerBoundCapacityMap.getOrDefault(e, 0));
    }

    public static void generateNetwork(int n, int m, int sources, int sinks, int supply, int lowerCap,
                                       int upperCap, int lowerCost, int upperCost, int seed, int capacitated, boolean withLowerBounds, boolean withNegativeCosts) {
        try {
            String exec = String.format("\"%s\" -f \"%s\" -n %d -m %d -s %d -i %d -t %d -r %d -b %d -e %d -l %d -u %d -c %d",
                    NETGEN_EXE, NETGEN_PROBLEM, n, m, sources, sinks, supply, Math.abs(seed), lowerCap, upperCap, lowerCost, upperCost, capacitated);
//            System.out.println(exec);
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(exec);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//            System.out.println(reader.readLine());
            process.waitFor();
            if (withLowerBounds) {
                addLbAndNC(withLowerBounds, withNegativeCosts);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void generateNetwork(int n, int m, int sources, int sinks, int supply, int lowerCap,
                                       int upperCap, int lowerCost, int upperCost, boolean withLowerBounds, boolean withNegativeCosts) {
        generateNetwork(n, m, sources, sinks, supply, lowerCap, upperCap, lowerCost, upperCost, (int) System.nanoTime(), 100, withLowerBounds, withNegativeCosts);
    }

    public static Problem<Integer, DefaultWeightedEdge> readProblem() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(NETGEN_PROBLEM)))) {
            Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
            Map<Integer, Integer> supplyMap = new HashMap<>();
            Map<DefaultWeightedEdge, Integer> lowerMap = new HashMap<>();
            Map<DefaultWeightedEdge, Integer> upperMap = new HashMap<>();
            String line;
            String[] token = reader.readLine().split("\\s+");
            int n = Integer.parseInt(token[0]);
            int m = Integer.parseInt(token[1]);
            while ((line = reader.readLine()) != null) {
                token = line.split("\\s+");
                if (token[0].equals("n")) {
                    int a = Integer.parseInt(token[1]);
                    int supply = Integer.parseInt(token[2]);
                    supplyMap.put(a, supply);
                } else {
                    int a = Integer.parseInt(token[1]);
                    int b = Integer.parseInt(token[2]);
                    int lower = Integer.parseInt(token[3]);
                    int upper = Integer.parseInt(token[4]);
                    double cost = Double.parseDouble(token[5]);
                    DefaultWeightedEdge edge = Graphs.addEdgeWithVertices(graph, a, b, cost);
                    lowerMap.put(edge, lower);
                    upperMap.put(edge, upper);
                }
            }
            return new Problem<>(graph, supplyMap, lowerMap, upperMap);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double testOnProblem(MinimumCostFlowProblem<Integer, DefaultWeightedEdge> problem) {
        CapacityScalingMinimumCostFlow<Integer, DefaultWeightedEdge> flow = new CapacityScalingMinimumCostFlow<>(3);
        System.out.printf("My code : %.3f\n", flow.getFlowCost(problem));
//        exportSolution(flow.getMinimumCostFlow(problem), problem.getGraph());
        if (!flow.testOptimality(1e-9)) {
            throw new RuntimeException();
        }
        return flow.getMinimumCostFlow(problem).getCost();
    }

    public static void exportSolution(MinimumCostFlowAlgorithm.MinimumCostFlow<DefaultWeightedEdge> flow, Graph<Integer, DefaultWeightedEdge> graph) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MY_CODE_SOLUTION)))) {
            writer.write(String.format("%d %d\n", graph.vertexSet().size(), flow.getFlowMap().entrySet().stream().filter(entry -> entry.getValue() != 0).count()));
            for (Map.Entry<DefaultWeightedEdge, Double> entry : flow.getFlowMap().entrySet()) {
                if (entry.getValue() > 0) {
                    writer.write(String.format("%d %d %d\n", graph.getEdgeSource(entry.getKey()), graph.getEdgeTarget(entry.getKey()), (int) (double) entry.getValue()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getTime(long time) {
        return String.format("%.3f sec", (double) time / 1e9);
    }

    public static double solveProblem() {
        try {
            String exec = String.format("\"%s\" -i \"%s\" -o \"%s\"", LEMON_SOLVER, NETGEN_PROBLEM, LEMON_SOLUTION);
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(exec);
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String result = reader.readLine();
            System.out.println(result);
            if ("INFEASIBLE".equalsIgnoreCase(result)) {
                return -1;
            } else {
                return Double.parseDouble(result);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void addLbAndNC(boolean lowerBounds, boolean negativeCosts) {
        Random random = new Random(System.nanoTime());
        Problem<Integer, DefaultWeightedEdge> problem = readProblem();
        Map<DefaultWeightedEdge, Integer> lowerMap = problem.getLowerBoundCapacityMap();
        Map<Integer, Integer> supplyMap = problem.getSupplyMap();
        Map<DefaultWeightedEdge, Integer> upperMap = problem.getUpperBoundCapacityMap();
        Graph<Integer, DefaultWeightedEdge> graph = problem.getGraph();
        int lowerBound;
        if (lowerBounds) {
            for (Map.Entry<DefaultWeightedEdge, Integer> entry : problem.getUpperBoundCapacityMap().entrySet()) {
                if (entry.getValue() < CAP_INF) {
                    lowerBound = random.nextInt(entry.getValue() / 100 + 1);
                } else {
                    lowerBound = random.nextInt(1000);
                }
                lowerBound += 1;
                lowerMap.put(entry.getKey(), lowerBound);
                supplyMap.put(graph.getEdgeSource(entry.getKey()), supplyMap.getOrDefault(graph.getEdgeSource(entry.getKey()), 0) + lowerBound);
                supplyMap.put(graph.getEdgeTarget(entry.getKey()), supplyMap.getOrDefault(graph.getEdgeTarget(entry.getKey()), 0) - lowerBound);
            }
        }
        if (negativeCosts) {
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                double r = random.nextDouble();
                if (r < 0.1 && upperMap.get(edge) < CAP_INF) {
                    graph.setEdgeWeight(edge, -graph.getEdgeWeight(edge));
                    upperMap.put(edge, upperMap.getOrDefault(edge, 0) / 10 + 1);
                    lowerMap.put(edge, lowerMap.getOrDefault(edge, 0 / 10));
                }
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(NETGEN_PROBLEM)))) {
            writer.write(String.format("%d %d\n", graph.vertexSet().size(), graph.edgeSet().size()));
            for (Map.Entry<Integer, Integer> entry : supplyMap.entrySet()) {
                if (entry.getValue() != 0) {
                    writer.write(String.format("n %d %d\n", entry.getKey(), entry.getValue()));
                }
            }
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                writer.write(String.format("a %d %d %d %d %.0f\n", graph.getEdgeSource(edge), graph.getEdgeTarget(edge), lowerMap.get(edge), upperMap.get(edge), graph.getEdgeWeight(edge)));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkCosts() {
        Problem<Integer, DefaultWeightedEdge> problem = readProblem();
        System.out.print("Lemon solution ");
        checkSolution(problem, readSolution(LEMON_SOLUTION, problem.getGraph()));
        System.out.print("My solution ");
        checkSolution(problem, readSolution(MY_CODE_SOLUTION, problem.getGraph()));
    }

    public static void checkSolution(Problem<Integer, DefaultWeightedEdge> problem, Map<DefaultWeightedEdge, Integer> solution) {
        Graph<Integer, DefaultWeightedEdge> graph = problem.getGraph();
        Map<Integer, Integer> supplyMap = problem.getSupplyMap();
        Map<DefaultWeightedEdge, Integer> lowerMap = problem.getLowerBoundCapacityMap();
        Map<DefaultWeightedEdge, Integer> upperMap = problem.getUpperBoundCapacityMap();
        for (Map.Entry<DefaultWeightedEdge, Integer> entry : solution.entrySet()) {
            if (entry.getValue() < lowerMap.getOrDefault(entry.getKey(), 0)) {
                throw new RuntimeException("Lower capacity violation on edge " + entry.getKey());
            }
        }
        for (Map.Entry<DefaultWeightedEdge, Integer> entry : solution.entrySet()) {
            if (entry.getValue() > upperMap.get(entry.getKey())) {
                throw new RuntimeException("Upper capacity violation on edge " + entry.getKey());
            }
        }
        Map<Integer, Integer> actualSupply = new HashMap<>();
        for (Map.Entry<DefaultWeightedEdge, Integer> entry : solution.entrySet()) {
            int a = graph.getEdgeSource(entry.getKey());
            int b = graph.getEdgeTarget(entry.getKey());
            actualSupply.put(a, actualSupply.getOrDefault(a, 0) + entry.getValue());
            actualSupply.put(b, actualSupply.getOrDefault(b, 0) - entry.getValue());
        }
        for (Integer vertex : graph.vertexSet()) {
            if (!actualSupply.getOrDefault(vertex, 0).equals(supplyMap.getOrDefault(vertex, 0))) {
                throw new RuntimeException("Supply violation on vertex " + vertex);
            }
        }
        System.out.println("passed");
    }

    public static Map<DefaultWeightedEdge, Integer> readSolution(String path, Graph<Integer, DefaultWeightedEdge> graph) {
        Map<DefaultWeightedEdge, Integer> solution = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(path)))) {
            String[] tokens = reader.readLine().split("\\s+");
            int m = Integer.parseInt(tokens[1]);
            int a, b, flow;
            for (int i = 0; i < m; i++) {
                tokens = reader.readLine().split("\\s+");
                a = Integer.parseInt(tokens[0]);
                b = Integer.parseInt(tokens[1]);
                flow = Integer.parseInt(tokens[2]);
                solution.put(graph.getEdge(a, b), flow);
            }
            return solution;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static void generateTestCase(int n, int m, int sources, int sinks, int supply, int minCap, int maxCap,
                                        int minCost, int maxCost, int seed, int testCaseNum, boolean withLowerBounds,
                                        boolean withnegaticeEdgeCosts, int capacitated) {
        //generateNetwork(n, m, sources, sinks, supply, minCap, maxCap, minCost, maxCost, seed, capacitated, withLowerBounds, withnegaticeEdgeCosts);
        Problem<Integer, DefaultWeightedEdge> problem = readProblem();
        Graph<Integer, DefaultWeightedEdge> graph = problem.getGraph();
        Map<DefaultWeightedEdge, Integer> lowerMap = problem.getLowerBoundCapacityMap();
        Map<DefaultWeightedEdge, Integer> upperMap = problem.getUpperBoundCapacityMap();
        double cost = solveProblem();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(TEST_CASE)))) {
            writer.write(String.format("  /**\n  * Test case generated with NETGEN generator\n  * params: vertices = %d, " +
                    "edges = %d,  sources = %d, sinks = %d, supply = %d, min. capacity = %d,\n  * max. capacity = %d, min. cost = %d," +
                    " max. cost = %d, capacitated = %d%%, seed = %d\n  * with lower bounds = %b, with negative edge costs = %b\n  \n  @Test\n  public void testGetMinimumCostFlow%d() {\n" +
                    "    int testCase[][] = new int[][]{", graph.vertexSet().size(), graph.edgeSet().size(), sources, sinks, supply, minCap, maxCap, minCost, maxCost, capacitated, seed, withLowerBounds, withnegaticeEdgeCosts, testCaseNum));
            String formatted;
            int size = 0;
            for (Map.Entry<Integer, Integer> entry : problem.getSupplyMap().entrySet()) {
                formatted = String.format("{%d, %d},", entry.getKey(), entry.getValue());
                size += formatted.length();
                writer.write(formatted);
                if (size > 100) {
                    writer.newLine();
                    size = 0;
                }
            }
            for (DefaultWeightedEdge edge : problem.getGraph().edgeSet()) {
                formatted = String.format("{%d, %d, %d, %d, %.0f},", graph.getEdgeSource(edge), graph.getEdgeTarget(edge),
                        lowerMap.get(edge), upperMap.get(edge), graph.getEdgeWeight(edge));
                writer.write(formatted);
                size += formatted.length();
                if (size > 100) {
                    writer.newLine();
                    size = 0;
                }
            }
            writer.write(String.format("\n    };\n    test(testCase, %.0f);\n  }", cost));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void convertIntoTestCase(MinimumCostFlowProblem<Integer, DefaultWeightedEdge> problem) {

    }

    private static class Problem<V, E> {
        Graph<V, E> graph;
        Map<V, Integer> supplyMap;
        Map<E, Integer> lowerBoundCapacityMap;
        Map<E, Integer> upperBoundCapacityMap;


        public Problem(Graph<V, E> graph, Map<V, Integer> supplyMap, Map<E, Integer> lowerBoundCapacityMap, Map<E, Integer> upperMap) {
            this.graph = graph;
            this.supplyMap = supplyMap;
            this.lowerBoundCapacityMap = lowerBoundCapacityMap;
            this.upperBoundCapacityMap = upperMap;
        }

        public Graph<V, E> getGraph() {
            return graph;
        }

        public Map<V, Integer> getSupplyMap() {
            return supplyMap;
        }

        public Map<E, Integer> getLowerBoundCapacityMap() {
            return lowerBoundCapacityMap;
        }

        public Map<E, Integer> getUpperBoundCapacityMap() {
            return upperBoundCapacityMap;
        }
    }
}
*/
