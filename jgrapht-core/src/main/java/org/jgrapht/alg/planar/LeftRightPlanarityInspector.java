package org.jgrapht.alg.planar;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.PlanarityTestingAlgorithm;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class LeftRightPlanarityInspector<V, E> implements PlanarityTestingAlgorithm<V, E> {

    private static final boolean DEBUG = false;
    private Graph<V, E> graph;
    private List<Node> nodes;
    private List<Node> dfsRoots;
    private Deque<ConflictPair> stack;

    public LeftRightPlanarityInspector(Graph<V, E> graph) {
        this.graph = Objects.requireNonNull(graph);
        this.nodes = new ArrayList<>();
        this.dfsRoots = new ArrayList<>();
        this.stack = new ArrayDeque<>();
    }

    public boolean isPlanar() {
        orient();
        for (Node node : nodes) {
            node.sortAdjacencyList();
        }
        if (DEBUG) {
            printState();
            System.out.println("Start testing phase\n");
        }
        for (Node root : dfsRoots) {
            if (!testDfs(root)) {
                return false;
            }
        }
        //embed();
        if (DEBUG) {
            printState();
        }
        return true;
    }

    private void embed() {
        for (Node node : nodes) {
            for (Arc arc : node.arcs) {
                arc.nestingDepth *= sign(arc);
            }
            node.sortAdjacencyList();
        }
        /*for (Node root : dfsRoots) {
            embedDfs(root);
        }*/
    }

    private int sign(Arc arc) {
        if (arc.ref != null) {
            arc.side = arc.side * sign(arc.ref);
            arc.ref = null;
        }
        return arc.side;
    }

    private void embedDfs(Node current) {
        for (Arc arc : current.arcs) {
            Node target = arc.target;
            if (arc == target.parentArc) {
                target.embedded.set(0, arc.graphEdge);
            } else {

            }
        }
    }

    @Override
    public Embedding<V, E> getEmbedding() {
        return null;
    }

    @Override
    public Graph<V, E> getKuratowskiSubdivision() {
        return null;
    }

    private void orient() {
        Map<V, Node> nodeMapping = new HashMap<>(graph.vertexSet().size());
        for (V vertex : graph.vertexSet()) {
            Node node = new Node(vertex, graph.degreeOf(vertex));
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

    private void orientDfs(Map<V, Node> nodeMapping, Set<E> visitedEdges, Node start) {
        List<Pair<Node, E>> stack = new ArrayList<>();
        for (E e : graph.edgesOf(start.graphVertex)) {
            stack.add(new Pair<>(start, e));
        }

        while (!stack.isEmpty()) {
            E edge = stack.get(stack.size() - 1).getSecond();
            if (visitedEdges.contains(edge)) {
                continue;
            }
            visitedEdges.add(edge);
            Node node = stack.get(stack.size() - 1).getFirst();
            Node opposite = nodeMapping.get(Graphs.getOppositeVertex(graph, edge, node.graphVertex));
            Arc arc = new Arc(edge, node, opposite);

            for (E e : graph.edgesOf(current.graphVertex)) {
                Node opposite = nodeMapping.get(Graphs.getOppositeVertex(graph, e, current.graphVertex));

                visitedEdges.add(e);
                Arc currentArc = new Arc(e, current, opposite);
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

    }

    private boolean testDfs(Node currentNode) {
        Arc parentArc = currentNode.parentArc;
        for (Arc arc : currentNode.arcs) {
            if (DEBUG) {
                System.out.printf("\nCurrent node: %s, traversing arc: %s\n\n", currentNode.graphVertex.toString(), arc);
            }

            arc.stackBottom = stack.peek();
            if (arc == arc.target.parentArc) {
                if (!testDfs(arc.target)) {
                    return false;
                }
            } else {
                arc.lowPointArc = arc;
                ConflictPair toPush = new ConflictPair(new Interval(), new Interval(arc, arc));
                stack.push(toPush);
                if (DEBUG) {
                    System.out.printf("Encountered back edge, pushing conflict pair:\n%s\n", toPush.toString());
                    printStack();
                }
            }
            if (arc.lowPoint < currentNode.height) {
                if (arc == currentNode.arcs.get(0)) {
                    parentArc.lowPointArc = arc.lowPointArc;
                } else {
                    if (!addConstraintsForArc(arc, parentArc)) {
                        // failed to add constraints, graph is not planar
                        return false;
                    }
                }
            }
        }
        if (parentArc != null) {
            Node source = parentArc.source;
            trimBackEdges(source);
            if (parentArc.lowPoint < source.height) {
                ConflictPair conflictPair = stack.peek();
                Arc leftHigh = conflictPair.left.high;
                Arc rightHigh = conflictPair.right.high;
                if (leftHigh != null && (rightHigh == null || leftHigh.lowPoint > rightHigh.lowPoint)) {
                    parentArc.ref = leftHigh;
                } else {
                    parentArc.ref = rightHigh;
                }
            }
        }
        return true;
    }

    private boolean addConstraintsForArc(Arc currentArc, Arc parent) {
        if (DEBUG) {
            System.out.printf("Adding constraints for arc: %s\n\n", currentArc.toString());
            System.out.println("Stack bottom: " + currentArc.stackBottom);
            printStack();

        }
        ConflictPair merged = new ConflictPair();

        // merging return edges of the current arc into the right interval
        do {
            ConflictPair currentPair = stack.pop();
            if (!currentPair.left.isEmpty()) {
                currentPair.swap();
            }
            if (!currentPair.left.isEmpty()) {
                return false; // graph is not planar
            }
            if (currentPair.right.low.lowPoint > parent.lowPoint) {
                if (merged.right.isEmpty()) {
                    merged.right.high = currentPair.right.high;
                } else {
                    merged.right.low.ref = currentPair.right.high;
                }
                merged.right.low = currentPair.right.low;
            } else {
                currentPair.right.low.ref = parent.lowPointArc;
            }
        } while (stack.peek() != currentArc.stackBottom);

        if (DEBUG) {
            System.out.printf("Merged into right interval:\n%s\n", merged.toString());
        }

        // merging conflicting return edges into the left interval
        assert !stack.isEmpty();
        if (DEBUG) {
            if (conflicting(stack.peek().left, currentArc) || conflicting(stack.peek().right, currentArc)) {
                System.out.println("There are conflicting constraints\n");
            } else {
                System.out.println("There are no conflicting constraints\n");
            }
        }
        while (conflicting(stack.peek().left, currentArc) || conflicting(stack.peek().right, currentArc)) {
            ConflictPair currentPair = stack.pop();
            if (conflicting(currentPair.right, currentArc)) {
                currentPair.swap();
                if (DEBUG) {
                    System.out.println("Swapped conflict pair:\n" + currentPair.toString());
                }
            }
            if (conflicting(currentPair.right, currentArc)) {
                return false; // graph is not planar
            }
            merged.right.low.ref = currentPair.right.high;
            if (!currentPair.right.isEmpty()) {
                merged.right.low = currentPair.right.low;
            }
            if (merged.left.isEmpty()) {
                merged.left.high = currentPair.left.high;
            } else {
                merged.left.low.ref = currentPair.left.high;
            }
            merged.left.low = currentPair.left.low;
        }
        if (!merged.isEmpty()) {
            stack.push(merged);
            if (DEBUG) {
                System.out.printf("Merged pair:\n%s\n", merged.toString());
                printStack();
            }
        }
        return true;
    }

    private void trimBackEdges(Node node) {
        if (DEBUG) {
            System.out.printf("Trimming edges for node: %s\n\n", node.toString());
            printStack();
        }
        while (!stack.isEmpty() && lowest(stack.peek()) == node.height) {
            ConflictPair pair = stack.pop();
            if (!pair.left.isEmpty()) {
                pair.left.low.side = -1;
            }
            if (DEBUG) {
                System.out.printf("Popped pair: %s\n\n", pair.toString());
            }
        }
        if (!stack.isEmpty()) {
            ConflictPair pair = stack.pop();
            if (DEBUG) {
                System.out.printf("Trimming conflict pair:\n%s\n\n", pair.toString());
            }
            while (pair.left.high != null && pair.left.high.target == node) {
                pair.left.high = pair.left.high.ref;
            }
            if (pair.left.high == null && pair.left.low != null) {
                if (DEBUG) {
                    System.out.println("Conflict pair removed\n");
                }
                pair.left.low.ref = pair.right.low;
                pair.left.low.side = -1;
                pair.left.low = null;
            }
            // trim right interval
            while (pair.right.high != null && pair.right.high.target == node) {
                pair.right.high = pair.right.high.ref;
            }
            if (pair.right.high == null && pair.right.low != null) {
                if (DEBUG) {
                    System.out.println("Conflict pair removed\n");
                }
                pair.right.low.ref = pair.left.low;
                pair.right.low.side = -1;
                pair.right.low = null;
            }
            stack.push(pair);
        }
        if (DEBUG) {
            System.out.println("Trimmed back edges");
            printStack();
        }
    }

    private int lowest(ConflictPair pair) {
        if (pair.left.isEmpty()) {
            return pair.right.low.lowPoint;
        } else if (pair.right.isEmpty()) {
            return pair.left.low.lowPoint;
        } else {
            return Math.min(pair.left.low.lowPoint, pair.right.low.lowPoint);
        }
    }

    private boolean conflicting(Interval interval, Arc arc) {
        if (DEBUG && !interval.isEmpty()) {
            System.out.println("Interval: " + interval);
            System.out.printf("Arc: %s, lowpoint: %d\n\n", arc.toString(), arc.lowPoint);
        }
        return !interval.isEmpty() && interval.high.lowPoint > arc.lowPoint;
    }


    private void printState() {
        System.out.println("Printing state");
        System.out.println("Dfs roots: " + dfsRoots.stream().map(r -> r.graphVertex.toString()).collect(Collectors.joining(", ", "[", "]")));
        System.out.println("Nodes:");
        for (Node node : nodes) {
            System.out.println(node.toString(true));
        }
        System.out.println("\n");
    }

    private void printStack() {
        List<ConflictPair> list = new ArrayList<>();
        for (ConflictPair pair : stack) {
            list.add(pair);
        }
        System.out.println("Printing stack:");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
        System.out.println("Stack end\n");
    }

    private class Node {
        Arc parentArc;
        V graphVertex;
        List<Arc> arcs;
        int height;
        int leftRef;
        int rightRef;
        boolean visited;
        List<E> embedded;

        Node(V graphVertex, int degree) {
            this.graphVertex = graphVertex;
            this.embedded = new ArrayList<>(Collections.nCopies(degree, null));
            this.rightRef = degree - 1;
            this.arcs = new ArrayList<>();
        }

        @Override
        public String toString() {
            return String.format("{%s}: height = %d, parentArc = %s\n",
                    graphVertex.toString(), height, String.valueOf(parentArc));

        }

        public String toString(boolean withArcs) {
            StringBuilder builder = new StringBuilder(toString());
            if (withArcs) {
                for (Arc arc : arcs) {
                    builder.append(arc.toString(true)).append('\n');
                }
            }
            return builder.toString();
        }

        void sortAdjacencyList() {
            if (arcs.isEmpty()) {
                return;
            }
            /*int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (Arc arc : arcs) {
                min = Math.min(min, arc.nestingDepth);
                max = Math.max(max, arc.nestingDepth);
            }
            int size = max - min + 1;*/
            int size = 2 * graph.vertexSet().size();
            List<List<Arc>> countList = new ArrayList<>(Collections.nCopies(size, null));
            /*for (int i = 0; i < size; i++) {
                countList.add(new ArrayList<>());
            }*/
            for (Arc arc : arcs) {
//                countList.get(arc.nestingDepth - min).add(arc);
                if (countList.get(arc.nestingDepth) == null) {
                    countList.set(arc.nestingDepth, new ArrayList<>());
                }
                countList.get(arc.nestingDepth).add(arc);
            }
            List<Arc> sorted = new ArrayList<>(arcs.size());
            for (List<Arc> arcList : countList) {
                if (arcList != null) {
                    sorted.addAll(arcList);
                }
            }
            arcs = sorted;
        }
    }

    private class Arc {
        int side;
        E graphEdge;
        Node source;
        Node target;
        int lowPoint;
        int lowPoint2;
        int nestingDepth;
        Arc ref;
        Arc lowPointArc;
        ConflictPair stackBottom;

        Arc(E graphEdge, Node source, Node target) {
            this.graphEdge = graphEdge;
            this.source = source;
            this.target = target;
            this.side = 1;
            lowPoint = lowPoint2 = -1;
        }

        public String toString(boolean full) {
            String res = toString();
            if (full) {
                res += String.format(": lowpoint = %d, lowpoint2 = %d, nesting_depth = %d, side = %d", lowPoint, lowPoint2, nestingDepth, side);
            }
            return res;
        }

        @Override
        public String toString() {
            return String.format("(%s -> %s)",
                    source.graphVertex.toString(), target.graphVertex.toString());
        }
    }

    private class Interval {
        Arc high;
        Arc low;

        Interval(Arc high, Arc low) {
            this.high = high;
            this.low = low;
        }

        Interval() {
        }

        boolean isEmpty() {
            return high == low && low == null;
        }

        @Override
        public String toString() {
            if (isEmpty()) {
                return "empty interval";
            }
            StringBuilder builder = new StringBuilder();
            for (Arc current = high; current != low; current = current.ref) {
                builder.append(current.toString()).append(", ");
            }
            builder.append(low.toString());
            return builder.toString();
        }
    }

    private class ConflictPair {
        Interval left;
        Interval right;

        ConflictPair(Interval left, Interval right) {
            this.left = left;
            this.right = right;
        }

        ConflictPair() {
            this.left = new Interval();
            this.right = new Interval();
        }

        void swap() {
            Interval t = left;
            left = right;
            right = t;
        }

        boolean isEmpty() {
            return left.isEmpty() && right.isEmpty();
        }

        @Override
        public String toString() {
            if (isEmpty()) {
                return "Empty conflict pair";
            }
            return String.format("Left interval: %s\nRight interval: %s\n", left.toString(), right.toString());
        }
    }
}
