package org.jgrapht.alg.my_util;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.*;
import org.jgrapht.util.SupplierUtil;

import java.io.*;
import java.util.*;

public class Util {
    public static final String BASE_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp";
    public static final String MATRIX_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\matrix.txt";
    public static final String EDGE_LIST_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\edge_list.txt";
    public static final String CODE_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\code.txt";
    public static final String JAR_PATH = "C:\\Java_Projects\\delaunay-triangulator\\library\\build\\libs\\Triangulation-1.jar";
    public static final String ARR_PATH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\arr.txt";
    public static final String DOT_GRAPH = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\graphs\\graph.dot";
    static final String planarGraphPath = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\planar\\graph.txt";

    public static Graph<Integer, DefaultEdge> readPlanarGraph(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            Graph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            String s;
            reader.readLine();
            while ((s = reader.readLine()) != null) {
                String[] tokens = s.split("\\s+");
                Graphs.addEdgeWithVertices(graph, Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
            }
            return graph;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Graph<Integer, DefaultEdge> readPlanarGraph(){
        return readPlanarGraph(planarGraphPath);
    }

    public static void checkIsMetric(Graph<Integer, DefaultWeightedEdge> graph) {
        int n = graph.vertexSet().size();
        for (int i = 2; i < n; i++) {
            for (int j = 1; j < i; j++) {
                for (int k = 0; k < j; k++) {
                    checkCondition(graph, graph.getEdge(i, j), graph.getEdge(j, k), graph.getEdge(i, k));
                }
            }
        }
    }

    public static void checkCondition(Graph<Integer, DefaultWeightedEdge> graph, DefaultWeightedEdge edge1, DefaultWeightedEdge edge2, DefaultWeightedEdge edge3) {
        if (graph.getEdgeWeight(edge1) + graph.getEdgeWeight(edge2) <= graph.getEdgeWeight(edge3)) {
            throw new RuntimeException();
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

    public static <V, E> void convertIntoArrInt(Graph<V, E> graph) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(ARR_PATH)))) {
            int m = graph.edgeSet().size();
            writer.write("int[][] edges = new int[][]{");
            int i = 0;
            for (E edge : graph.edgeSet()) {
                V a = graph.getEdgeSource(edge);
                V b = graph.getEdgeTarget(edge);
                int weight = (int) graph.getEdgeWeight(edge);
                writer.write("{" + a + ", " + b + ", " + weight + "}");
                if (i != m - 1) {
                    writer.write(",");
                }
                if (i % 7 == 0 && i != 0) {
                    writer.write("\n");
                }
                i++;
            }
            writer.write("};");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportMatrix(Graph<Integer, DefaultWeightedEdge> graph) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(BASE_PATH + "\\matrix.txt")))) {
            writer.write(graph.vertexSet().size() + " " + graph.edgeSet().size() + "\n");
            int n = graph.vertexSet().size();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (graph.containsEdge(i, j)) {
                        //noinspection UnnecessaryBoxing
                        writer.write(String.valueOf((int) graph.getEdgeWeight(graph.getEdge(i, j))));
                    } else {
                        writer.write("0");
                    }
                    writer.write(" ");
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkTour(GraphPath<Integer, DefaultWeightedEdge> path) {
        Set<Integer> visited = new HashSet<>();
        List<Integer> tourVertices = path.getVertexList();
        for (int i = 0; i < tourVertices.size() - 1; i++) {
            if (!visited.add(tourVertices.get(i))) {
                throw new RuntimeException();
            }
        }
        if (!tourVertices.get(0).equals(tourVertices.get(tourVertices.size() - 1))) {
            throw new RuntimeException();
        }
    }

    public static Graph<Integer, DefaultWeightedEdge> generateTriangulation(int pointNum, int x, int y, boolean fractional, boolean exportPoints) {
        try {
            Runtime runtime = Runtime.getRuntime();
            String command = String.format("\"C:\\C++_Projects\\delaunay\\release\\delaunay.exe\" " +
                            "-e \"C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\edge_list.txt\" " +
                            " %s %s -n %d -x %d -y %d", exportPoints ? "-p \"C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\point_set.txt\" " : "",
                    fractional ? "-f" : "", pointNum, x, y);

            Process process = runtime.exec(command);
            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                throw new RuntimeException();
            }
            return readEdgeList();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Graph<Integer, DefaultWeightedEdge> generateComplete(int size, int upperBound) {
        Random random = new Random(System.currentTimeMillis());
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (int i = 0; i < size; i++) {
            graph.addVertex(i);
        }
        for (int i = 1; i < size; i++) {
            for (int j = 0; j < i; j++) {
                int weight = random.nextInt(upperBound) + 1;
                Graphs.addEdgeWithVertices(graph, i, j, weight);
            }
        }
        return graph;
    }

    public static Graph<Integer, DefaultWeightedEdge> generateCompleteMetric(int size, int x, int y) {
        List<Pair<Integer, Integer>> points = generatePoints(size, x, y);
        Graph<Integer, DefaultWeightedEdge> graph =
                new DefaultUndirectedWeightedGraph<>(SupplierUtil.createIntegerSupplier(), SupplierUtil.createSupplier(DefaultWeightedEdge.class));
        graph.addVertex();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < i; j++) {
                Graphs.addEdgeWithVertices(graph, i, j, dist(points.get(i), points.get(j)));
            }
        }
        return graph;
    }

    public static Graph<Integer, DefaultWeightedEdge> importMatrix() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(MATRIX_PATH)))) {
            Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            int n = Integer.parseInt(reader.readLine());
            for (int i = 0; i < n; i++) {
                String[] tokens = reader.readLine().split("\\s+");
                int j = 0;
                for (String token : tokens) {
                    if (j > i) {
                        int w = Integer.parseInt(token);
                        Graphs.addEdgeWithVertices(graph, i, j, w);
                    }
                    j++;
                }
            }
            return graph;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double dist(Pair<Integer, Integer> pointA, Pair<Integer, Integer> pointB) {
        return Math.sqrt(Math.pow(pointA.getFirst() - pointB.getSecond(), 2) + Math.pow(pointA.getSecond() - pointB.getSecond(), 2));
    }

    public static List<Pair<Integer, Integer>> generatePoints(int num, int x, int y) {
        Set<Pair<Integer, Integer>> points = new HashSet<>();
        Random random = new Random();
        int i = 0;
        while (points.size() < num) {
            int a = random.nextInt(x);
            int b = random.nextInt(y);
            Pair<Integer, Integer> pair = new Pair<>(a, b);
            points.add(pair);
        }
        return new ArrayList<>(points);
    }

    public static Graph<Integer, DefaultWeightedEdge> readEdgeList() {
        return readEdgeList(false);
    }

    public static Graph<Integer, DefaultWeightedEdge> readEdgeList(boolean directed) {
        Graph<Integer, DefaultWeightedEdge> graph;
        if (directed) {
            graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        } else {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(EDGE_LIST_PATH)))) {
            String[] tokens = reader.readLine().split("\\s+");
            int m = Integer.valueOf(tokens[1]);
            for (int i = 0; i < m; i++) {
                tokens = reader.readLine().split("\\s+");
                int a = Integer.valueOf(tokens[0]);
                int b = Integer.valueOf(tokens[1]);
                double w = Integer.valueOf(tokens[2]);
                Graphs.addEdgeWithVertices(graph, a, b, w);
            }
            return graph;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
