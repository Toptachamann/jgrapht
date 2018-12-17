package org.jgrapht.alg.my_util;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Util {
    static final String planarGraphPath = "C:\\Users\\timof\\Documents\\stuff\\GSoC_2018\\Tmp\\planar\\graph.txt";

    public static Graph<Integer, DefaultEdge> readGraph() {
        try (BufferedReader reader = new BufferedReader(new FileReader(planarGraphPath))) {
            Graph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            String s;
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
}
