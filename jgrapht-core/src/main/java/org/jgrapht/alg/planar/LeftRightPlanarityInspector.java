package org.jgrapht.alg.planar;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.PlanarityTestingAlgorithm;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class LeftRightPlanarityInspector<V, E> implements PlanarityTestingAlgorithm<V, E> {

    private static final boolean DEBUG = true;
    private Graph<V, E> graph;
    private List<Node> nodes;
    private List<Node> dfsRoots;
    private Deque<ConflictPair> constraintStack;
    private boolean tested = false;
    private Embedding<V, E> embedding;
    private Graph<V, E> subdivision;
    private boolean planar;

    public LeftRightPlanarityInspector(Graph<V, E> graph) {
        this.graph = Objects.requireNonNull(graph);
        this.nodes = new ArrayList<>();
        this.dfsRoots = new ArrayList<>();
        this.constraintStack = new ArrayDeque<>();
    }

    public static <V, E> boolean isK33Subdivision(Graph<V, E> graph) {
        List<V> degree3 = new ArrayList<>();
        for (V vertex : graph.vertexSet()) {
            if (graph.degreeOf(vertex) == 3) {
                degree3.add(vertex);
            } else if (graph.degreeOf(vertex) != 2) {
                return false;
            }
        }
        if (degree3.size() != 6) {
            return false;
        }
        V vertex = degree3.remove(degree3.size() - 1);
        Set<V> reachable = reachableWithDegree(graph, vertex, 3);
        if (reachable.size() != 3) {
            return false;
        }
        degree3.removeAll(reachable);
        return reachable.equals(reachableWithDegree(graph, degree3.get(0), 3))
                && reachable.equals(reachableWithDegree(graph, degree3.get(1), 3));
    }

    public static <V, E> boolean isK5Subdivision(Graph<V, E> graph) {
        Set<V> degree5 = new HashSet<>();
        for (V vertex : graph.vertexSet()) {
            if (graph.degreeOf(vertex) == 5) {
                degree5.add(vertex);
            } else if (graph.degreeOf(vertex) != 2) {
                return false;
            }
        }
        if (degree5.size() != 5) {
            return false;
        }
        for (V vertex : degree5) {
            Set<V> reachable = reachableWithDegree(graph, vertex, 5);
            if (reachable.size() != 4 || !degree5.containsAll(reachable) || reachable.contains(vertex)) {
                return false;
            }
        }
        return true;
    }

    private static <V, E> Set<V> reachableWithDegree(Graph<V, E> graph, V vertex, int degree) {
        Set<V> visited = new HashSet<>();
        Set<V> reachable = new HashSet<>();
        Queue<V> queue = new ArrayDeque<>();
        queue.add(vertex);
        while (!queue.isEmpty()) {
            V current = queue.poll();
            visited.add(current);
            for (E e : graph.edgesOf(current)) {
                V opposite = Graphs.getOppositeVertex(graph, e, current);
                if (visited.contains(opposite)) {
                    continue;
                }
                if (graph.degreeOf(opposite) == degree) {
                    reachable.add(opposite);
                } else {
                    queue.add(opposite);
                }
            }
        }
        return reachable;
    }

    @Override
    public boolean isPlanar() {
        return lazyTestPlanarity();
    }

    @Override
    public Embedding<V, E> getEmbedding() {
        return lazyComputeEmbedding();
    }

    @Override
    public Graph<V, E> getKuratowskiSubdivision() {
        lazyTestPlanarity();
        if (planar) {
            throw new IllegalArgumentException("Graph is planar");
        }
        return null;
    }

    private boolean lazyTestPlanarity() {
        if (!tested) {
            tested = true;
            orient();
            sortAdjacencyLists(0, 2 * graph.vertexSet().size());
            if (DEBUG) {
                printState();
                System.out.println("Start testing phase\n");
            }
            planar = true;
            for (Node root : dfsRoots) {
                if (!testDfs(root)) {
                    planar = false;
                    break;
                }
            }
        }
        return planar;
    }

    private Embedding<V, E> lazyComputeEmbedding() {
        if (embedding == null) {
            lazyTestPlanarity();
            if (!planar) {
                throw new IllegalArgumentException("Graph is not planar");
            }
            embed();
            if (DEBUG) {
                printState();
            }
            Map<V, List<E>> embeddingMap = new HashMap<>(graph.vertexSet().size());
            for (Node node : nodes) {
                embeddingMap.put(node.graphVertex, node.embedded);
            }
            embedding = new EmbeddingImpl<>(embeddingMap);
        }
        return embedding;
    }

    private void sortAdjacencyLists(int min, int max) {
        int size = max - min + 1;
        List<List<Arc>> arcs = new ArrayList<>(Collections.nCopies(size, null));
        for (Node node : nodes) {
            for (Arc arc : node.arcs) {
                int pos = arc.nestingDepth - min;
                if (arcs.get(pos) == null) {
                    arcs.set(pos, new ArrayList<>());
                }
                arcs.get(pos).add(arc);
            }
            node.arcs.clear();
        }
        for (List<Arc> arcList : arcs) {
            if (arcList != null) {
                for (Arc arc : arcList) {
                    arc.source.arcs.add(arc);
                }
            }
        }
    }

    private void orient() {
        Map<V, Node> nodeMapping = new HashMap<>(graph.vertexSet().size());
        for (V vertex : graph.vertexSet()) {
            Node node = new Node(vertex, graph.degreeOf(vertex));
            nodes.add(node);
            nodeMapping.put(vertex, node);
        }
        for (Node node : nodes) {
            if (node.height == -1) {
                dfsRoots.add(node);
                orientDfs(nodeMapping, node);
            }
        }
    }

    private void orientDfs(Map<V, Node> nodeMapping, Node start) {
        List<Pair<Node, Boolean>> stack = new ArrayList<>();
        stack.add(new Pair<>(start, false));

        while (!stack.isEmpty()) {
            Pair<Node, Boolean> pair = stack.remove(stack.size() - 1);
            Node current = pair.getFirst();
            boolean backtrack = pair.getSecond();

            if (backtrack) {
                Arc parent = current.parentArc;
                for (Arc arc : current.arcs) {
                    arc.nestingDepth = 2 * arc.lowPoint;
                    if (arc.lowPoint2 < current.height) {
                        ++arc.nestingDepth;
                    }
                    if (parent != null) {

                        if (parent.lowPoint > arc.lowPoint) {
                            parent.lowPoint2 = Math.min(parent.lowPoint, arc.lowPoint2);
                            parent.lowPoint = arc.lowPoint;
                        } else if (parent.lowPoint < arc.lowPoint) {
                            parent.lowPoint2 = Math.min(arc.lowPoint, parent.lowPoint2);
                        } else {
                            parent.lowPoint2 = Math.min(arc.lowPoint2, parent.lowPoint2);
                        }
                    }
                }
            } else {
                if (current.height != -1) {
                    continue; // visited in the previous branch
                }
                if (current.parentArc != null) {
                    current.parentArc.source.arcs.add(current.parentArc);
                    current.height = current.parentArc.source.height + 1;
                } else {
                    current.height = 0;
                }
                stack.add(new Pair<>(current, true));
                for (E e : graph.edgesOf(current.graphVertex)) {
                    Node opposite = nodeMapping.get(Graphs.getOppositeVertex(graph, e, current.graphVertex));
                    if (opposite.height >= current.height || (current.parentArc != null && current.parentArc.source == opposite)) {
                        continue;
                    }
                    Arc currentArc = new Arc(e, current, opposite);
                    currentArc.lowPoint = currentArc.lowPoint2 = current.height;

                    if (opposite.height == -1) {
                        opposite.parentArc = currentArc;
                        stack.add(new Pair<>(opposite, false));
                    } else {
                        currentArc.lowPoint = opposite.height;
                        current.arcs.add(currentArc);
                    }

                }
            }
        }

    }

    private void embed() {
        for (Node node : nodes) {
            for (Arc arc : node.arcs) {
                arc.nestingDepth *= sign(arc);
            }
        }
        sortAdjacencyLists(-2 * graph.vertexSet().size(), 2 * graph.vertexSet().size());
        if (DEBUG) {
            printState();
        }
        for (Node node : nodes) {
            for (Arc arc : node.arcs) {
                if (arc.ref != null) {
                    throw new NullPointerException();
                }
            }
        }
        for (Node root : dfsRoots) {
            embedDfs(root);
        }
    }

    private void embedDfs(Node start) {
        List<Pair<Arc, Boolean>> stack = new ArrayList<>();
        for (int i = start.arcs.size() - 1; i >= 0; --i) {
            stack.add(Pair.of(start.arcs.get(i), false));
        }

        while (!stack.isEmpty()) {
            Arc currentArc = stack.get(stack.size() - 1).getFirst();
            boolean backtrack = stack.get(stack.size() - 1).getSecond();
            stack.remove(stack.size() - 1);

            Node target = currentArc.target;
            Node current = currentArc.source;

            if (currentArc == target.parentArc) {
                if (backtrack) {
                    for (Arc i = current.leftRef; i != null; i = i.ref) {
                        current.embedded.add(i.graphEdge);
                    }
                } else {
                    target.embedded.add(currentArc.graphEdge);
                    current.leftRef = current.rightRef = currentArc;
                    stack.add(Pair.of(currentArc, true));
                    for (int i = target.arcs.size() - 1; i >= 0; i--) {
                        stack.add(Pair.of(target.arcs.get(i), false));
                    }
                }
            } else {
                if (currentArc.side == 1) {
                    currentArc.ref = target.rightRef.ref;
                    target.rightRef.ref = currentArc;
                } else {
                    currentArc.ref = target.leftRef;
                    target.leftRef = currentArc;
                }
                current.embedded.add(currentArc.graphEdge);
            }


        }
    }

    private int sign(Arc arc) {
        if (arc.ref == null) {
            return arc.side;
        }
        Arc prev = null;
        while (arc != null) {
            Arc next = arc.ref;
            arc.ref = prev;
            prev = arc;
            arc = next;
        }
        arc = prev;
        prev = null;
        while (arc != null) {
            if (prev != null) {
                arc.side = arc.side * prev.side;
                prev.ref = null;
            }
            prev = arc;
            arc = arc.ref;
        }
        prev.ref = null;
        return prev.side;

    }

    private boolean testDfs(Node start) {
        List<Pair<Arc, Boolean>> stack = new ArrayList<>();
        for (Arc arc : start.arcs) {
            stack.add(Pair.of(arc, false));
        }

        while (!stack.isEmpty()) {
            Arc currentArc = stack.get(stack.size() - 1).getFirst();
            boolean backtrack = stack.get(stack.size() - 1).getSecond();
            stack.remove(stack.size() - 1);
            if (DEBUG) {
                System.out.println("Traversing arc: " + currentArc.toString() + ", phase: " + (backtrack ? "backtrack" : "forward"));
            }

            Arc parentArc = currentArc.source.parentArc;
            Node source = currentArc.source;

            if (!backtrack) {
                currentArc.stackBottom = constraintStack.peek();
            }
            if (currentArc != currentArc.target.parentArc) {
                // it is a back arc
                currentArc.lowPointArc = currentArc;
                ConflictPair toPush = new ConflictPair(new Interval(), new Interval(currentArc, currentArc));
                constraintStack.push(toPush);
                if (DEBUG) {
                    System.out.printf("Encountered back edge, pushing conflict pair:\n%s\n", toPush.toString());
                    printStack();
                }
            } else if (!backtrack) {
                // we are going forward
                stack.add(Pair.of(currentArc, true));
                for (int i = currentArc.target.arcs.size() - 1; i >= 0; i--) {
                    stack.add(Pair.of(currentArc.target.arcs.get(i), false));
                }
                continue;
            }

            // here backtracking begins
            if (currentArc.lowPoint < source.height) {
                // current arc has a return edge
                if (currentArc == source.arcs.get(0)) {
                    parentArc.lowPointArc = currentArc.lowPointArc;
                } else if (!addConstraintsForArc(currentArc, parentArc)) {
                    // failed to add constraints, graph is not planar
                    return false;
                }
            }
            if (currentArc == source.arcs.get(source.arcs.size() - 1)) {
                // finished all outgoing arcs of the current source node
                if (parentArc != null) {
                    Node parentSource = parentArc.source;
                    trimBackEdges(parentSource);
                    if (parentArc.lowPoint < parentSource.height) {
                        ConflictPair conflictPair = constraintStack.peek();

                        assert conflictPair != null;

                        Arc leftHigh = conflictPair.left.high;
                        Arc rightHigh = conflictPair.right.high;
                        if (leftHigh != null && (rightHigh == null || leftHigh.lowPoint > rightHigh.lowPoint)) {
                            parentArc.ref = leftHigh;
                        } else {
                            parentArc.ref = rightHigh;
                        }
                    }
                }
            }

        }
        return true;
    }

    private Pair<Arc, Arc> forkArc(Arc first, Arc second) {
        Arc current = first, next = second;
        Arc result, stop;
        while (true) {
            if (current.visited) {
                result = current;
                stop = next;
                break;
            }
            if (current.source.parentArc == null) {
                while (!next.visited) {
                    next = next.source.parentArc;
                }
                result = next;
                stop = current;
                break;
            }
            current.visited = true;
            Arc t = next;
            next = current.source.parentArc;
            current = t;

        }
        Arc leftFork = unvisit(first, result);
        Arc rightFork = unvisit(second, result);
        unvisit(result, stop);
        return new Pair<>(leftFork, rightFork);
    }

    private Arc unvisit(Arc start, Arc stop) {
        Arc i;
        for (i = start; i.source.parentArc != stop; i = i.source.parentArc) {
            i.visited = false;
        }
        i.visited = false;
        i.source.parentArc.visited = false;
        return i;
    }

    private void addFundamentalCycle(Set<E> edges, Arc backArc) {
        Node stop = backArc.target;
        Arc current = backArc;
        while (current.source != stop) {
            edges.add(current.graphEdge);
            current = current.source.parentArc;
        }
        edges.add(current.graphEdge);
    }

    private void removePath(Set<E> edges, Node high, Node low) {
        for (Arc i = high.parentArc; i != low.parentArc; i = i.source.parentArc) {
            edges.remove(i.graphEdge);
        }
    }

    private void extractKuratowskiSubdivision(Arc forkArc, Arc failedArc, boolean previousConstraints) {
        ConflictPair conflictPair = constraintStack.peek();
        Set<E> edges = new HashSet<>();
        Arc leftLow = conflictPair.left.low;
        Arc rightLow = conflictPair.right.low;
        if (previousConstraints) {

        } else {
            Pair<Arc, Arc> fork = forkArc(leftLow, rightLow);
            Arc leftFork = fork.getFirst();
            Arc rightFork = fork.getSecond();
            if (leftFork.lowPointArc.lowPoint < rightFork.lowPointArc.lowPoint) {
                assert leftFork.target.height > leftFork.source.height;
                addFundamentalCycle(edges, forkArc.lowPointArc);
                if (leftLow.lowPoint > rightLow.lowPoint) {
                    // extracting K_{3, 3}
                    addFundamentalCycle(edges, leftLow);
                    addFundamentalCycle(edges, rightLow);
                    addFundamentalCycle(edges, leftFork.lowPointArc);
                } else if (leftLow.lowPoint == rightLow.lowPoint) {
                    // extracting K_{3, 3}
                    addFundamentalCycle(edges, leftLow);
                    addFundamentalCycle(edges, rightLow);
                    addFundamentalCycle(edges, leftFork.lowPointArc);
                    addFundamentalCycle(edges, rightFork.lowPointArc);
                    removePath(edges, leftLow.target, leftFork.lowPointArc.target);
                } else {

                }
            }
        }

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
            ConflictPair currentPair = constraintStack.pop();
            if (!currentPair.left.isEmpty()) {
                currentPair.swap();
            }
            if (!currentPair.left.isEmpty()) {
                System.out.println("Top conflict pair:");
                System.out.println(currentPair);
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
        } while (constraintStack.peek() != currentArc.stackBottom);

        if (DEBUG) {
            System.out.printf("Merged into right interval:\n%s\n", merged.toString());
        }

        // merging conflicting return edges into the left interval
        assert !constraintStack.isEmpty();
        if (DEBUG) {
            if (conflicting(constraintStack.peek().left, currentArc) || conflicting(constraintStack.peek().right, currentArc)) {
                System.out.println("There are conflicting constraints\n");
            } else {
                System.out.println("There are no conflicting constraints\n");
            }
        }
        while (conflicting(constraintStack.peek().left, currentArc) || conflicting(constraintStack.peek().right, currentArc)) {
            ConflictPair currentPair = constraintStack.pop();
            if (conflicting(currentPair.right, currentArc)) {
                currentPair.swap();
                if (DEBUG) {
                    System.out.println("Swapped conflict pair:\n" + currentPair.toString());
                }
            }
            if (conflicting(currentPair.right, currentArc)) {
                System.out.println("Top conflict pair:");
                System.out.println(currentPair);
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
            constraintStack.push(merged);
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
        while (!constraintStack.isEmpty() && lowest(constraintStack.peek()) == node.height) {
            ConflictPair pair = constraintStack.pop();
            if (!pair.left.isEmpty()) {
                pair.left.low.side = -1;
            }
            if (DEBUG) {
                System.out.printf("Popped pair: %s\n\n", pair.toString());
            }
        }
        if (!constraintStack.isEmpty()) {
            ConflictPair pair = constraintStack.pop();
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
            constraintStack.push(pair);
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
        for (ConflictPair pair : constraintStack) {
            list.add(pair);
        }
        System.out.println("Printing constraintStack:");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
        System.out.println("Stack end\n");
    }

    private class Node {
        Arc parentArc;
        V graphVertex;
        List<Arc> arcs;
        List<E> embedded;
        int height;
        Arc leftRef;
        Arc rightRef;

        Node(V graphVertex, int degree) {
            this.graphVertex = graphVertex;
            this.arcs = new ArrayList<>();
            this.embedded = new ArrayList<>(degree);
            this.height = -1;
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

    }

    private class Arc {
        int side;
        E graphEdge;
        Node source;
        Node target;
        int lowPoint;
        int lowPoint2;
        int nestingDepth;
        boolean visited;
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
                res += String.format(": lowpoint = %d, lowpoint2 = %d, nesting_depth = %d, side = %d, ref = %s", lowPoint, lowPoint2, nestingDepth, side, String.valueOf(ref));
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
