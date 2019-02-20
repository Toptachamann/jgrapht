/*
package org.jgrapht.alg.matching.blossom.v5;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.util.SupplierUtil;

import java.io.*;
import java.util.*;

import static org.jgrapht.alg.matching.blossom.v5.KolmogorovWeightedPerfectMatching.EPS;


public class MatchingMain {
    public static final String BASE_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp";
    public static final String MATRIX_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\matrix.txt";
    public static final String EDGE_LIST_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\edge_list.txt";
    public static final String CODE_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\code.txt";
    public static final String JAR_PATH = "C:\\Java_Projects\\delaunay-triangulator\\library\\build\\libs\\Triangulation-1.jar";
    public static final String ARR_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\arr.txt";
    public static final String DOT_GRAPH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\graphs\\graph.dot";
    public static BlossomVOptions[] options = BlossomVOptions.ALL_OPTIONS;

    public static void main(String[] args) throws InterruptedException, IOException {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> g = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        g.setVertexSupplier(SupplierUtil.createIntegerSupplier(0));
        for (int i = 0; i < 4; i++) {
            g.addVertex();
        }
        Graphs.addEdgeWithVertices(g, 0, 1,8);
        Graphs.addEdgeWithVertices(g, 1, 2,4);
        Graphs.addEdgeWithVertices(g, 2, 3,3);
        Graphs.addEdgeWithVertices(g, 3, 0,2);
        Graphs.addEdgeWithVertices(g, 1, 3,15);
        KolmogorovWeightedMatching<Integer, DefaultWeightedEdge> weightedMatching = new KolmogorovWeightedMatching<>(g, ObjectiveSense.MAXIMIZE);
        System.out.println(weightedMatching.getMatching());
        System.out.println(weightedMatching.getDualSolution());
    }

    public static void reformat() {
        Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
        Map<Integer, Pair<Integer, Integer>> points = importPointSet();
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            Pair<Integer, Integer> a = points.get(graph.getEdgeSource(edge));
            Pair<Integer, Integer> b = points.get(graph.getEdgeTarget(edge));
            graph.setEdgeWeight(edge, dist(a, b));
        }
        exportSimple(graph);
    }

    public static int dist(Pair<Integer, Integer> a, Pair<Integer, Integer> b) {
        return (int) (Math.sqrt(Math.pow(a.getFirst() - b.getFirst(), 2) + Math.pow(a.getSecond() - b.getSecond(), 2)) + 0.5);
    }

    public static void printTable() {
        System.out.println("\t\t\t\\hline\n" +
                "\t\t\t\t\\textbf{Options}  & \\textbf{Time, us/op} & \\textbf{Options} & \\textbf{Time, us/op} & \\textbf{Options} & \\textbf{Time, us/op} \\\\ \\hline");
        for (int i = 0; i < 8; i++) {
            System.out.print("\t\t\t\t");
            for (int j = 0; j < 3; j++) {
                System.out.print("options[" + String.valueOf(8 * j + i) + "] & " + (j == 2 ? "" : "& "));
            }
            System.out.println(" \\\\ \\hline");
        }
    }

    private static void print(int f) {
        System.out.println("\\hline\n" +
                "\t\t\t\t\t\\multirow{2}{*}{\\textbf{Options}}  &\\multicolumn{3}{c|}{Init.} & \\multicolumn{2}{c|}{Duals} & \\multicolumn{2}{c|}{Before} & \\multicolumn{2}{c|}{After} \\\\\n" +
                "\t\t\t\t\t\\cline{2-10}\n" +
                "\t\t\t\t\t& None & Greedy&Fractional & Fixed $\\delta$ & CC & true & false & true & false \\\\");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    for (int l = 0; l < 2; l++) {
                        System.out.print("\\hline\noptions[" + String.valueOf(8 * i + 4 * j + 2 * k + l) + "]");
                        if (i == 0) {
                            System.out.print(" & \\checkmark & & ");
                        } else if (i == 1) {
                            System.out.print(" & &\\checkmark & ");
                        } else {
                            System.out.print(" & & &\\checkmark ");
                        }
                        print(j);
                        print(k);
                        print(l);
                        System.out.println(" \\\\");
                    }
                }
            }
        }
        System.out.println("\\hline");

        if (f == 0) {
            System.out.print(" & \\checkmark & ");
        } else {
            System.out.print(" & &\\checkmark ");
        }
    }

    public static void printGraph(int[][] graph) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EDGE_LIST_PATH))) {
            Set<Integer> nodes = new HashSet<>();
            for (int[] edge : graph) {
                nodes.add(edge[0]);
                nodes.add(edge[1]);
            }
            writer.write(nodes.size() + " " + graph.length);
            writer.newLine();
            for (int[] edge : graph) {
                writer.write(edge[0] + " " + edge[1] + " " + edge[2]);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void drawGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
        Map<Integer, Pair<Integer, Integer>> points = importPointSet();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(DOT_GRAPH)))) {
            writer.write("graph {\ngraph[fontname=Helvetica, fontsize=32, pad=1]\n");
            for (int i = 0; i < graph.vertexSet().size(); i++) {
                writer.write(String.format("%d [pos=\"%d,%d!\" shape=point, width=0.4, height=0.4, color=\"#000000\", fontsize=36];\n", i + 1, points.get(i).getFirst(), points.get(i).getSecond(), i + 1));
            }
            Set<DefaultWeightedEdge> selectedEdges = new HashSet<>();
            for (int vertex : graph.vertexSet()) {
                List<DefaultWeightedEdge> edges = new ArrayList<>(graph.edgesOf(vertex));
                edges.sort(Comparator.comparingDouble(graph::getEdgeWeight));
                for (int i = 0; i < 4; i++) {
                    selectedEdges.add(edges.get(i));
                }
            }
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                boolean selected = selectedEdges.contains(edge);
                writer.write(String.format("%d -- %d [label =\"%s\", penwidth=%d, color=%s, fontsize=36];\n", graph.getEdgeSource(edge) + 1, graph.getEdgeTarget(edge) + 1,
                        selected ? String.format("%.0f", graph.getEdgeWeight(edge)) : "", selected ? 10 : 4, selected ? "\"#DEA75F\"" : "grey"));
            }
            writer.write("}\n");
            writer.close();
            launchDot();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void drawBig() {
        Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
        Map<Integer, Pair<Integer, Integer>> points = importPointSet();
        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> solver = new KolmogorovWeightedPerfectMatching<>(graph);
        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching = solver.getMatching();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(DOT_GRAPH)))) {
            writer.write("graph {\ngraph[dpi=80 fontname=Helvetica, fontsize=32, pad=1]\n");
            for (int i = 0; i < graph.vertexSet().size(); i++) {
                writer.write(String.format("%d [pos=\"%d,%d!\", shape=point, width=0.4, height=0.4];\n",
                        (i + 1), points.get(i).getFirst(), points.get(i).getSecond()));
            }
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                boolean matched = matching.getEdges().contains(edge);
                writer.write(String.format("%d -- %d [penwidth=%d, color =%s];\n",
                        graph.getEdgeSource(edge) + 1, graph.getEdgeTarget(edge) + 1, matched ? 20 : 15, matched ? "red" : "grey"));
            }
            writer.write("}\n");
            writer.close();
            launchDot();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void launchDot() {
        try {
            String command = "dot -Kfdp -n -Tjpg -o \"C:\\Users\\timof\\Documents\\stuff\\GSoC 2018\\graphs\\graph.jpg\" " +
                    "\"C:\\Users\\timof\\Documents\\stuff\\GSoC 2018\\graphs\\graph.dot\"";
            System.out.println(command);
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(command);
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, Pair<Integer, Integer>> importPointSet() {
        Map<Integer, Pair<Integer, Integer>> points = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(BASE_PATH + "\\point_set.txt")))) {
            int n = Integer.parseInt(reader.readLine());
            String[] tokens;
            for (int i = 0; i < n; i++) {
                tokens = reader.readLine().split("\\s+");
                int a = Integer.parseInt(tokens[0]);
                int b = Integer.parseInt(tokens[1]);
                points.put(i, new Pair<>(a, b));
            }
            return points;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void customTest() throws IOException {
        Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
        System.out.println("Imported");
        measureTime(graph, options[17], 10, 20);
    }

    public static long measureTime(Graph<Integer, DefaultWeightedEdge> graph, BlossomVOptions options, int warmUp, int measurement) {
        for (int i = 0; i < warmUp; i++) {
            measureTimeSingle(graph, options);
        }
        long total = 0;
        for (int i = 0; i < measurement; i++) {
            total += measureTimeSingle(graph, options);
        }
        total /= measurement;
        System.out.println("Average time = " + total / 1e9);
        return total;
    }

    public static long measureTimeSingle(Graph<Integer, DefaultWeightedEdge> graph, BlossomVOptions options) {
        double start = System.nanoTime();
        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> perfectMatching = new KolmogorovWeightedPerfectMatching<>(graph, options);
        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching = perfectMatching.getMatching();
        double end = System.nanoTime();
        System.out.println(String.format("Weight of the matching = %.4f", matching.getWeight()));
        System.out.println(String.format("Options = " + options + ",\nTime = %.4f", (end - start) / 1e9));
        KolmogorovWeightedPerfectMatching.Statistics statistics = perfectMatching.getStatistics();
        System.out.println(String.format("%.3f, %.3f, %.3f, %.3f, %3f", (double) statistics.augmentTime / 1e9, (double) statistics.growTime / 1e9,
                (double) statistics.shrinkTime / 1e9, (double) statistics.expandTime / 1e9, (double) statistics.dualUpdatesTime / 1e9));
        System.out.println(String.format("Grow num = %d, shrink num = %d, expand num = %d", statistics.growNum, statistics.shrinkNum, statistics.expandNum));
        System.out.println();
        return (long) (end - start);
    }

    public static void testOnComplete(int testCaseNum, int size, int upperBound) {
        for (int i = 0; i < testCaseNum; i++) {
            Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.generateComplete(size, upperBound);
            System.out.println("Generated");
            measureTimeSingle(graph, options[9]);
        }
    }

    public static void testOnTriangulation(int testCaseNum, int pointNum, int x, int y) {
        for (int i = 0; i < testCaseNum; i++) {
            MyGenerator.generateTriangulation(pointNum, x, y, false, true);
            Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
//            System.out.println("Graph's edge list has been imported");
            KolmogorovWeightedPerfectMatching.Statistics statistics = testOnGraph(graph, i + 1);

            System.out.println(statistics);
        }
    }

    public static void test() throws IOException, InterruptedException {
        String exec = "C:\\C++_Projects\\Trial\\release\\Trial.exe";
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(exec);
        System.out.println(process.waitFor());
    }

    public static <V, E> void exportMatching(Graph<V, E> graph, MatchingAlgorithm.Matching<V, E> matching) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(BASE_PATH + "\\my_code_result.txt")))) {
            for (E edge : matching.getEdges()) {
                writer.write(graph.getEdgeSource(edge) + " " + graph.getEdgeTarget(edge) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printMatchingCost(Graph<Integer, DefaultWeightedEdge> graph, String matchingPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(matchingPath)))) {
            String[] tokens;
            double weight = 0;
            boolean isPerfectMatching = true;
            Set<Integer> visited = new HashSet<>();
            for (int i = 0; i < graph.vertexSet().size() / 2; i++) {
                tokens = reader.readLine().split("\\s+");
                int a = Integer.valueOf(tokens[0]);
                int b = Integer.valueOf(tokens[1]);
                if (a == b || visited.contains(a) || visited.contains(b)) {
                    isPerfectMatching = false;
                }
                visited.add(a);
                visited.add(b);
                weight += graph.getEdgeWeight(graph.getEdge(a, b));
            }
            System.out.println(weight + (isPerfectMatching ? ", is perfect matching " : "is not a perfect matching"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void convertMatching(String inPath, String outPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(inPath)));
             BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outPath)))) {
            int i = 0;
            Map<Integer, Integer> matched = new HashMap<>();
            String token;
            while ((token = reader.readLine()) != null) {
                int to = Integer.valueOf(token);
                if (!matched.containsKey(to)) {
                    matched.put(i, to);
                }
                i++;
            }
            for (Map.Entry<Integer, Integer> entry : matched.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testError(int testCasesNum, int pointNum, int x, int y) {
        double maxError = 0;
        double avgError = 0;
        for (int i = 0; i < testCasesNum; i++) {
            MyGenerator.generateTriangulation(pointNum, x, y, false, false);
            Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
            System.out.println(graph.edgeSet().stream().map(graph::getEdgeWeight).max(Double::compare).get());
            KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> matcher = new KolmogorovWeightedPerfectMatching<>(graph);
            MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching = matcher.getMatching();
            double error = matcher.getError();
            double weight = matching.getWeight();
            maxError = Math.max(maxError, error);
            avgError += error;
            System.out.println("Test case " + (i + 1) + ", weight = " + weight + ", error = " + error);
        }
        avgError /= testCasesNum;
        System.out.println("Maximum error = " + maxError + ", avg error = " + avgError);

    }

    public static <V, E> void normalize(Graph<V, E> graph) {
        double max = graph.edgeSet().stream().max(Comparator.comparingDouble(graph::getEdgeWeight)).map(graph::getEdgeWeight).orElse(1d);
        for (E edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, graph.getEdgeWeight(edge) / max);
        }
    }

    public static void testInputOutput(String outPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(outPath)))) {
            System.out.println("Start reading graph");
            Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
            System.out.println("Graph has been imported");
            KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> matcher = new KolmogorovWeightedPerfectMatching<>(graph);
            MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching = matcher.getMatching();
            exportMatching(graph, matching);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <V, E> boolean isRegular(Graph<V, E> graph) {
        int degree = -1;
        for (V vertex : graph.vertexSet()) {
            if (degree == -1) {
                degree = graph.edgesOf(vertex).size();
            } else if (degree != graph.edgesOf(vertex).size()) {
                return false;
            }
        }
        return true;
    }

    public static void printCode() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(EDGE_LIST_PATH)));
             BufferedWriter writer = new BufferedWriter(new FileWriter(new File(CODE_PATH)))) {
            String[] tokens = reader.readLine().split("\\s+");
            int m = Integer.valueOf(tokens[1]);
            for (int i = 0; i < m; i++) {
                if (i % 4 == 0 && i != 0) {
                    writer.write('\n');
                }
                tokens = reader.readLine().split("\\s+");
                writer.write(String.format("Graphs.addEdgeWithVertices(graph, %d, %d, %d);\n", Integer.valueOf(tokens[0]), Integer.valueOf(tokens[1]), Double.valueOf(tokens[2]).intValue()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Graph<Integer, DefaultWeightedEdge> generateRegularWithoutPerfectMatching(int degree, int upperBound) {
        Random random = new Random();
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        int endVerticesStart = degree * (degree + 1);
        int theEndVertex = degree * (degree + 2);
        int weight;
        for (int i = 0; i < degree; i++) {
            int start = i * (degree + 1);
            int end = start + degree + 1;
            int cliqueVertex = endVerticesStart + i;
            for (int j = start + 1; j < end; j++) {
                for (int k = start; k < j; k++) {
                    weight = random.nextInt(upperBound);
                    Graphs.addEdgeWithVertices(graph, k, j, weight);
                }
            }
            for (int j = start; j < end - 2; j += 2) {
                graph.removeEdge(j, j + 1);
            }
            for (int j = start; j < end - 2; j++) {
                weight = random.nextInt(upperBound);
                Graphs.addEdgeWithVertices(graph, j, cliqueVertex, weight);
            }
            weight = random.nextInt(upperBound);
            Graphs.addEdgeWithVertices(graph, cliqueVertex, theEndVertex, weight);
        }
        return graph;
    }

    public static void noPerfectMatchingOnTriangulation(int pointNum, int x, int y, boolean fractional, boolean exportPoints) {
        for (int i = 0; i < 100; i++) {
            MyGenerator.generateTriangulation(pointNum, x, y, fractional, exportPoints);
            Graph<Integer, DefaultWeightedEdge> graph = MyGenerator.readEdgeList();
            graph.addVertex(pointNum);
            System.out.print("Test case " + (i + 1) + ", |V| = " + graph.vertexSet().size());
            for (BlossomVOptions options : options) {
                try {
                    KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> perfectMatching = new KolmogorovWeightedPerfectMatching<>(graph, options);
                    perfectMatching.getMatching();
                    throw new RuntimeException();
                } catch (IllegalArgumentException e) {
                    System.out.print(" thrown");
                }
            }
            System.out.println();
        }
    }

    public static void noPerfectMatchingOnRandom(int testCaseNum, int vertexNum, int edgeNum, int weightUB) {
        for (int i = 0; i < testCaseNum; i++) {
            Graph<Integer, DefaultWeightedEdge> graph = generateWithPerfectMatching(vertexNum, edgeNum, weightUB, false);
            EdmondsMaximumCardinalityMatching<Integer, DefaultWeightedEdge> matcher = new EdmondsMaximumCardinalityMatching<>(graph);
            int cardinality = matcher.getMatching().getEdges().size();
            System.out.print("Test case " + (i + 1) + ", cardinality = " + cardinality);
            for (BlossomVOptions options : options) {
                try {
                    KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> perfectMatching = new KolmogorovWeightedPerfectMatching<>(graph, options);
                    perfectMatching.getMatching();
                    throw new RuntimeException();
                } catch (IllegalArgumentException e) {
                    System.out.print(" thrown");
                }
            }
            System.out.println();

        }
    }

    public static Graph<Integer, DefaultWeightedEdge> generateWithPerfectMatching(int vertexNum, int edgeNum, int weightUpperbound, boolean hasPerfMatch) {
        Graph<Integer, DefaultWeightedEdge> graph;
        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching;
        Random random = new Random(System.nanoTime());
        do {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            for (int i = 0; i < vertexNum; i++) {
                graph.addVertex(i);
            }
            for (int i = 0; i < edgeNum; i++) {
                int a;
                int b;
                do {
                    a = random.nextInt(vertexNum);
                    b = random.nextInt(vertexNum);
                } while (graph.containsEdge(a, b) || a == b);
                int weight = random.nextInt(weightUpperbound);
                Graphs.addEdgeWithVertices(graph, a, b, weight);
            }
            EdmondsMaximumCardinalityMatching<Integer, DefaultWeightedEdge> matcher = new EdmondsMaximumCardinalityMatching<>(graph);
            matching = matcher.getMatching();
        } while ((!hasPerfMatch && matching.isPerfect()) || (hasPerfMatch && !matching.isPerfect()));
        return graph;
    }

    public static void testOnGenerated() {
        try {
            int shrinkNum = 0;
            int expandNum = 0;
            int growNum = 0;
            for (int i = 0; i < 1; i++) {
                generateMatrix(MATRIX_PATH, 3000);
                System.out.println("Generated");
                Graph<Integer, DefaultWeightedEdge> graph = readGraph(MATRIX_PATH, true);
                System.out.println("Read from file");
                exportSimple(graph);
                System.out.println("Exported");
                testOnGraph(graph, i + 1);
//KolmogorovWeightedPerfectMatching.Statistics statistics = perfectMatching.getStatistics();
//                shrinkNum += statistics.getShrinkNum();
//                expandNum += statistics.getExpandNum();
//                growNum += statistics.getGrowNum();


            }
            System.out.println("Shrink num: " + shrinkNum);
            System.out.println("Expand num: " + expandNum);
            System.out.println("Grow num: " + growNum);

        } catch (IOException e) {
            System.out.flush();
            e.printStackTrace();
        }
    }

    public static KolmogorovWeightedPerfectMatching.Statistics testOnGraph(Graph<Integer, DefaultWeightedEdge> graph, int testCase) {
        double weight;
        KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> perfectMatching = new KolmogorovWeightedPerfectMatching<>(
                graph, options[0]);
        MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching = perfectMatching.getMatching();
        weight = matching.getWeight();
        System.out.println("Test case: " + testCase + ", matching weight = " + matching.getWeight());
        for (int i = 1; i < options.length; i++) {
            BlossomVOptions opt = options[i];
//            System.out.println(i);
            KolmogorovWeightedPerfectMatching<Integer, DefaultWeightedEdge> blossomPerfectMatching = new KolmogorovWeightedPerfectMatching<>(graph, opt);
            MatchingAlgorithm.Matching<Integer, DefaultWeightedEdge> matching1 = blossomPerfectMatching.getMatching();
            double weight2 = matching1.getWeight();
            if (Math.abs(weight2 - weight) > EPS) {
                System.out.println(weight);
                System.out.println(weight2);
                throw new RuntimeException();
            }
            if (!blossomPerfectMatching.testOptimality()) {
                System.out.println(blossomPerfectMatching.getDualSolution());
                throw new RuntimeException();
            }
        }
        return perfectMatching.getStatistics();
    }

    public static Graph<Integer, DefaultWeightedEdge> readGraph(String path, boolean bipartite) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(path)))) {
            Graph<Integer, DefaultWeightedEdge> graph =
                    new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            int vertexNum = Integer.valueOf(reader.readLine());
            for (int i = 0; i < vertexNum; i++) {
                String[] tokens = reader.readLine().split("\\s+");
                if (tokens.length != vertexNum) {
                    throw new IOException("Invalid vertex num at row " + (i + 1));
                }
                for (int j = 0; j < vertexNum; j++) {
                    double weight = Double.valueOf(tokens[j]);
                    if (weight > 0) {
                        if (!bipartite) {
                            if (i < j) {
                                Graphs.addEdgeWithVertices(graph, i, j, weight);
                            }
                        } else {
                            Graphs.addEdgeWithVertices(graph, i, vertexNum + j, weight);
                        }
                    }
                }
            }
            return graph;
        }
    }

    public static void generateMatrix(String path, int vertexNum) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(path)))) {
            Random random = new Random();
            boolean odd = (vertexNum & 1) == 1;
            double upperBound = 30;
            int dummyVertex = -1;
            int dummyWeight = 0;
            writer.write((odd ? vertexNum + 1 : vertexNum) + "\n");
            if (odd) {
                dummyVertex = (int) (random.nextDouble() * vertexNum);
                dummyWeight = (int) (random.nextDouble() * upperBound + 1);
            }
            for (int i = 0; i < vertexNum; i++) {
                for (int j = 0; j < vertexNum; j++) {
                    int weight = (int) (random.nextDouble() * upperBound + 1);
                    writer.write(weight + " ");
                }
                if (odd) {
                    if (i == dummyVertex) {
                        writer.write(dummyWeight + " ");
                    } else {
                        writer.write("0 ");
                    }
                }
                writer.write("\n");
            }
            if (odd) {
                for (int i = 0; i < vertexNum + 1; i++) {
                    if (i == dummyVertex) {
                        writer.write(dummyWeight + " ");
                    } else {
                        writer.write("0 ");
                    }
                }
            }
        }
    }

    public static void exportSimple(Graph<Integer, DefaultWeightedEdge> graph) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(EDGE_LIST_PATH)))) {
            writer.write(graph.vertexSet().size() + " " + graph.edgeSet().size() + "\n");
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                int a = graph.getEdgeSource(edge);
                int b = graph.getEdgeTarget(edge);
                double weight = graph.getEdgeWeight(edge);
                writer.write(a + " " + b + " " + (int) weight + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Graph<Integer, DefaultWeightedEdge> generateCompleteBipartite(int vertexNum, int weightUpperbound) {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Random random = new Random(System.nanoTime());
        for (int i = 0; i < vertexNum; i++) {
            for (int j = vertexNum; j < 2 * vertexNum; j++) {
                int weight = random.nextInt(weightUpperbound) + 1;
                Graphs.addEdgeWithVertices(graph, i, j, weight);
            }
        }
        return graph;
    }

    public static Graph<Integer, DefaultWeightedEdge> generateBipartiteWithPerfectMatching(int vertexNum, int edgeNum, int weightUpperbound) {
        Graph<Integer, DefaultWeightedEdge> graph = null;
        int matchedNum = 0;
        Random random = new Random(System.nanoTime());
        while (matchedNum != vertexNum) {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            for (int i = 0; i < edgeNum; i++) {
                int a;
                int b;
                do {
                    a = random.nextInt(vertexNum);
                    b = random.nextInt(vertexNum) + vertexNum;
                } while (graph.containsEdge(a, b));
                int weight = random.nextInt(weightUpperbound) + 1;
                Graphs.addEdgeWithVertices(graph, a, b, weight);
            }
            EdmondsMaximumCardinalityMatching<Integer, DefaultWeightedEdge> matcher = new EdmondsMaximumCardinalityMatching<>(graph);
            matchedNum = matcher.getMatching().getEdges().size();
        }
        return graph;
    }

    public void foo() {
        Scanner scanner = new Scanner(System.in);
        for (int i = 0; i < 6; i++) {
            ArrayList<Double> results = new ArrayList<>();
            double sum = 0;
            System.out.println("Computing sum");
            System.out.println("Enter the numbers");
            for (int j = 0; j < 6; j++) {
                results.add(scanner.nextDouble());
                sum += results.get(results.size() - 1);
            }
            double avg = sum / 6;
            System.out.println("Average = " + avg);
            for (int j = 0; j < 6; j++) {
                System.out.println("For " + j + " - " + ((avg - results.get(j)) * 100 / avg));
            }
        }
    }
}
*/
