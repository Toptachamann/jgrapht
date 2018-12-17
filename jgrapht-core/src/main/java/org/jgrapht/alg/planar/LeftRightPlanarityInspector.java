package org.jgrapht.alg.planar;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;

import java.util.*;
import java.util.stream.Collectors;

public class LeftRightPlanarityInspector<V, E> {

    private Graph<V, E> graph;
    private List<Node> nodes;
    private List<Node> dfsRoots;
    private List<Arc> arcs;
    private Deque<ConflictPair> stack;

    public LeftRightPlanarityInspector(Graph<V, E> graph) {
        this.graph = Objects.requireNonNull(graph);
        this.nodes = new ArrayList<>();
        this.dfsRoots = new ArrayList<>();
        this.arcs = new ArrayList<>();
        this.stack = new ArrayDeque<>();
    }

    public boolean isPlanar() {
        orient();
        for (Node node : nodes) {
            node.sortAdjacencyList();
        }
        printState();
        return false;
    }

    private void test() {
        for (Node root : dfsRoots) {
            testDfs(root);
        }
    }

    private void testDfs(Node current) {
        Arc parentArc = current.parentArc;
        for (Arc arc : current.arcs) {
            arc.stackBottom = stack.peek();
            if (arc == arc.to.parentArc) {
                testDfs(arc.to);
            } else {
                arc.ref = arc;
                stack.push(new ConflictPair(new Interval(), new Interval(arc, arc)));
            }
            if (arc.lowPoint < current.height) {
                if (arc == current.arcs.get(0)) {
                    parentArc.ref = arc.ref;
                } else {
                    // Alg 4
                }
            }
        }
        if (parentArc != null) {
            Node source = parentArc.from;
            // Alg 5
            if (parentArc.lowPoint < source.height) {
                ConflictPair conflictPair = stack.peek();
                Arc leftLow = conflictPair.left.low;
                Arc rightHigh = conflictPair.right.high;
                if (leftLow != null && (rightHigh == null || leftLow.lowPoint > rightHigh.lowPoint)) {
                    parentArc.nextArc = leftLow;
                } else {
                    parentArc.nextArc = rightHigh;
                }
            }
        }
    }

    private void addConstraintsForArc(Arc arc, Arc parent) {
        ConflictPair merged = new ConflictPair();
        do {
            ConflictPair currentPair = stack.pop();
            if (!currentPair.left.isEmpty()) {
                currentPair.swap();
            }
            if (!currentPair.left.isEmpty()) {
                throw new IllegalArgumentException("Not planar");
            }
            if (currentPair.right.low.lowPoint > parent.lowPoint) {
                if (!merged.right.isEmpty()) {
                    merged.right.high = currentPair.right.high;
                }else{
                    merged.right.low.nextArc = currentPair.right.high;
                }
                merged.right.low = currentPair.right.low;
            }else{
                currentPair.right.low = parent.ref;
            }
        } while (stack.peek() != arc.stackBottom);
        while (conflicting(stack.peek().left, arc) || conflicting(stack.peek().right, arc)) {
            ConflictPair currentPair = stack.peek();
            if (conflicting(currentPair.right, arc)) {
                currentPair.swap();
            }
            if (conflicting(currentPair.right, arc)) {
                throw new IllegalArgumentException("Not planar");
            }
            merged.right.low.ref = currentPair.right.high;
            if (!currentPair.right.isEmpty()) {
                merged.right.low = currentPair.right.low;
            }
            if (merged.left.isEmpty()) {
                merged.left.high = currentPair.left.high;
            }else{
                merged.left.low.ref = currentPair.left.high;
            }
            merged.left.low = currentPair.left.low;
        }
        if (!merged.isEmpty()) {
            stack.push(merged);
        }
    }

    private boolean conflicting(Interval interval, Arc arc) {
        return interval.isEmpty() && interval.high.lowPoint > arc.lowPoint;
    }

    private void orient() {
        Map<V, Node> nodeMapping = new HashMap<>(graph.vertexSet().size());
        for (V vertex : graph.vertexSet()) {
            Node node = new Node(vertex);
            nodes.add(node);
            nodeMapping.put(vertex, node);
        }
        Set<E> visitedEdges = new HashSet<>();
        for (Node node : nodes) {
            if (!node.visited) {
                node.height = 0; // let's do it explicitly
                dfsRoots.add(node);
                orientDfs(nodeMapping, visitedEdges, node);
            }
        }
        nodes.forEach(n -> n.visited = false);
    }

    void printState() {
        System.out.println("Dfs roots: " + dfsRoots.stream().map(r -> r.graphVertex.toString()).collect(Collectors.joining(", ", "[", "]")));
        System.out.println("Nodes:");
        for (Node node : nodes) {
            System.out.println(node);
        }
    }


    private void orientDfs(Map<V, Node> nodeMapping, Set<E> visitedEdges, Node current) {
        current.visited = true;
        for (E e : graph.edgesOf(current.graphVertex)) {
            Node opposite = nodeMapping.get(Graphs.getOppositeVertex(graph, e, current.graphVertex));
            if (current.parentArc != null && opposite == current.parentArc.from || visitedEdges.contains(e)) {
                continue;
            }
            visitedEdges.add(e);
            Arc currentArc = new Arc(e, current, opposite);
            arcs.add(currentArc);
            current.arcs.add(currentArc);
            currentArc.lowPoint = currentArc.lowPoint2 = current.height;
            if (!opposite.visited) {
                opposite.parentArc = currentArc;
                opposite.height = current.height + 1;
                orientDfs(nodeMapping, visitedEdges, opposite);
            } else {
                currentArc.lowPoint = opposite.height;
            }

            currentArc.nestingDepth = 2 * currentArc.lowPoint;
            if (currentArc.lowPoint2 < current.height) {
                ++currentArc.nestingDepth;
            }

            Arc currentParent = current.parentArc;
            if (currentParent != null) {
                if (currentParent.lowPoint > currentArc.lowPoint) {
                    currentParent.lowPoint2 = Math.min(currentParent.lowPoint, currentArc.lowPoint2);
                    currentParent.lowPoint = currentArc.lowPoint;
                } else if (currentParent.lowPoint < currentArc.lowPoint) {
                    currentParent.lowPoint2 = Math.min(currentArc.lowPoint, currentParent.lowPoint2);
                } else {
                    currentParent.lowPoint2 = Math.min(currentArc.lowPoint2, currentParent.lowPoint2);
                }
            }
        }
    }

    private class Node {
        Arc parentArc;
        V graphVertex;
        List<Arc> arcs;
        int height;
        boolean visited;

        public Node(V graphVertex) {
            this.graphVertex = graphVertex;
            arcs = new ArrayList<>();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(String.format("{%s}: height = %d, parentArc = %s\n",
                    graphVertex.toString(), height, String.valueOf(parentArc)));
            for (Arc arc : arcs) {
                builder.append(arc.toString()).append('\n');
            }
            return builder.toString();
        }

        public void sortAdjacencyList() {
            if (arcs.isEmpty()) {
                return;
            }
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (Arc arc : arcs) {
                min = Math.min(min, arc.nestingDepth);
                max = Math.max(max, arc.nestingDepth);
            }
            int size = max - min + 1;
            List<List<Arc>> countList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                countList.add(new ArrayList<>());
            }
            for (Arc arc : arcs) {
                countList.get(arc.nestingDepth - min).add(arc);
            }
            List<Arc> sorted = new ArrayList<>(arcs.size());
            for (List<Arc> arcList : countList) {
                sorted.addAll(arcList);
            }
            arcs = sorted;
        }
    }

    private class Arc {
        E graphEdge;
        Node from;
        Node to;
        int lowPoint;
        int lowPoint2;
        int nestingDepth;
        Arc ref;
        Arc nextArc;
        ConflictPair stackBottom;

        public Arc(E graphEdge, Node from, Node to) {
            this.graphEdge = graphEdge;
            this.from = from;
            this.to = to;
            lowPoint = lowPoint2 = -1;
        }

        @Override
        public String toString() {
            return String.format("(%s -> %s): lowpoint = %d, lowpoint2 = %d, nesting_depth = %d",
                    from.graphVertex.toString(), to.graphVertex.toString(), lowPoint, lowPoint2, nestingDepth);
        }
    }

    private class Interval {
        Arc high;
        Arc low;

        public Interval(Arc high, Arc low) {
            this.high = high;
            this.low = low;
        }

        public Interval() {
        }

        public boolean isEmpty(){
            return high == low && low == null;
        }
    }

    private class ConflictPair {
        Interval left;
        Interval right;

        public ConflictPair(Interval left, Interval right) {
            this.left = left;
            this.right = right;
        }

        public ConflictPair(){
            this.left = new Interval();
            this.right = new Interval();
        }

        public void swap() {
            Interval t = left;
            left = right;
            right = t;
        }

        public boolean isEmpty(){
            return left.isEmpty() && right.isEmpty();
        }
    }
}
