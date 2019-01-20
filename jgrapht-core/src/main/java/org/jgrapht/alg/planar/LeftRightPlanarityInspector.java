package org.jgrapht.alg.planar;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.PlanarityTestingAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

public class LeftRightPlanarityInspector implements PlanarityTestingAlgorithm {

    private static final boolean DEBUG = true;
    private Graph<Integer, DefaultEdge> graph;
    private List<Node> nodes;
    private List<Node> dfsRoots;
    private Deque<ConflictPair> constraintStack;
    private boolean tested = false;
    private Embedding<Integer, DefaultEdge> embedding;
    private Graph<Integer, DefaultEdge> subdivision;
    private Map<Integer, List<DefaultEdge>> debugGraph;
    private boolean planar;

    public LeftRightPlanarityInspector(Graph<Integer, DefaultEdge> graph) {
        this.graph = Objects.requireNonNull(graph);
        this.nodes = new ArrayList<>();
        this.dfsRoots = new ArrayList<>();
        this.constraintStack = new ArrayDeque<>();
    }

    public static <V, E> boolean isKuratowskiSubdivision(Graph<V, E> graph) {
        return isK33Subdivision(graph) || isK5Subdivision(graph);
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
            int degree = graph.degreeOf(vertex);
            if (degree == 4) {
                degree5.add(vertex);
            } else if (degree != 2) {
                return false;
            }
        }
        if (degree5.size() != 5) {
            return false;
        }
        for (V vertex : degree5) {
            Set<V> reachable = reachableWithDegree(graph, vertex, 4);
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
    public Embedding<Integer, DefaultEdge> getEmbedding() {
        return lazyComputeEmbedding();
    }

    @Override
    public Graph<Integer, DefaultEdge> getKuratowskiSubdivision() {
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

    private Embedding<Integer, DefaultEdge> lazyComputeEmbedding() {
        if (embedding == null) {
            lazyTestPlanarity();
            if (!planar) {
                throw new IllegalArgumentException("Graph is not planar");
            }
            embed();
            if (DEBUG) {
                printState();
            }
            Map<Integer, List<DefaultEdge>> embeddingMap = new HashMap<>(graph.vertexSet().size());
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
        /*Map<Integer, Node> nodeMapping = initDebug();
        Node node1 = nodeMapping.get(0);
        dfsRoots.add(node1);
        orientDfs(nodeMapping, node1);*/
        Map<Integer, Node> nodeMapping = new HashMap<>(graph.vertexSet().size());
        for (Integer vertex : graph.vertexSet()) {
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

    private Map<Integer, Node> initDebug() {
        Node node0 = new Node(0, 0);
        Node node1 = new Node(1, 0);
        Node node2 = new Node(2, 0);
        Node node3 = new Node(3, 0);
        Node node4 = new Node(4, 0);
        Node node5 = new Node(5, 0);
        Node node6 = new Node(6, 0);

        nodes = new ArrayList<>(Arrays.asList(node0, node1, node2, node3, node4, node5, node6));
        Map<Integer, Node> nodeMapping = new HashMap<>();
        nodeMapping.put(0, node0);
        nodeMapping.put(1, node1);
        nodeMapping.put(2, node2);
        nodeMapping.put(3, node3);
        nodeMapping.put(4, node4);
        nodeMapping.put(5, node5);
        nodeMapping.put(6, node6);

        DefaultEdge e01 = graph.getEdge(0, 1);
        DefaultEdge e12 = graph.getEdge(1, 2);
        DefaultEdge e23 = graph.getEdge(2, 3);
        DefaultEdge e34 = graph.getEdge(3, 4);
        DefaultEdge e45 = graph.getEdge(4, 5);
        DefaultEdge e56 = graph.getEdge(5, 6);
        DefaultEdge e64 = graph.getEdge(6, 4);
        DefaultEdge e61 = graph.getEdge(6, 1);
        DefaultEdge e50 = graph.getEdge(5, 0);
        DefaultEdge e52 = graph.getEdge(5, 2);
        DefaultEdge e30 = graph.getEdge(3, 0);

        debugGraph = new HashMap<>();
        debugGraph.put(0, Arrays.asList(e50, e30, e01));
        debugGraph.put(1, Arrays.asList(e61, e01, e12));
        debugGraph.put(2, Arrays.asList(e12, e52, e23));
        debugGraph.put(3, Arrays.asList(e23, e30, e34));
        debugGraph.put(4, Arrays.asList(e34, e64, e45));
        debugGraph.put(5, Arrays.asList(e45, e50, e52, e56));
        debugGraph.put(6, Arrays.asList(e56, e64, e61));
        return nodeMapping;
    }

    private void orientDfs(Map<Integer, Node> nodeMapping, Node start) {
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
                // TODO fix
                for (DefaultEdge e : graph.edgesOf(current.graphVertex)) {
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
        for (Node node : nodes) {
            for (Arc arc : node.arcs) {
                assert !arc.visited;
            }
        }
        return new Pair<>(leftFork, rightFork);
    }

    private Node forkNode(Arc first, Arc second) {
        return forkArc(first, second).getFirst().source;
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

    private void addFundamentalCycle(Set<Arc> edges, Arc... backArcs) {
        for (Arc backArc : backArcs) {
            Node stop = backArc.target;
            Arc current = backArc;
            while (current.source != stop) {
                edges.add(current);
                current = current.source.parentArc;
            }
            edges.add(current);
        }
    }

    private void addNodesFromFunamentalCycle(Set<Node> nodes, Arc backArc) {
        nodes.add(backArc.target);
        Node currentNode = backArc.source;
        while (currentNode != backArc.target) {
            nodes.add(currentNode);
            currentNode = currentNode.parentArc.source;
        }
    }

    private void removeFundamentalCycle(Set<Arc> edges, Arc backArc) {
        Node stop = backArc.target;
        Arc current = backArc;
        while (current.source != stop) {
            edges.remove(current);
            current = current.source.parentArc;
        }
        edges.add(current);
    }

    private void removePath(Set<Arc> edges, Node high, Node low) {
        for (Arc i = high.parentArc; i != low.parentArc; i = i.source.parentArc) {
            edges.remove(i);
        }
    }

    private Arc searchPath(Set<Node> forbiddenNodes, Node from, Node to, int low, int high) {
        for (Node current = from; current != to; current = current.parentArc.source) {
            Arc res = searchPath(forbiddenNodes, current, low, high);
            if (res != null) {
                return res;
            }
        }
        return null;
    }


    private Arc searchPath(Set<Node> forbiddenNodes, Node current, int low, int high) {
        for (Arc arc : current.arcs) {
            if (arc.target.height < high && arc.target.height > low) {
                return arc;
            } else if (!forbiddenNodes.contains(arc.target) && arc.isTreeArc()) {
                Arc res = searchPath(forbiddenNodes, arc.target, low, high);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    private Node lowest(Node... searchNodes) {
        Node res = null;
        int minHeight = Integer.MAX_VALUE;
        for (Node node : searchNodes) {
            if (minHeight > node.height) {
                res = node;
                minHeight = node.height;
            }
        }
        return res;
    }

    private Node highest(Node... searchNodes) {
        Node res = null;
        int minHeight = Integer.MIN_VALUE;
        for (Node node : searchNodes) {
            if (minHeight < node.height) {
                res = node;
                minHeight = node.height;
            }
        }
        return res;
    }

    private Set<Node> getForbidden(Arc... arcs) {
        Set<Node> res = new HashSet<>();
        for (Arc arc : arcs) {
            addNodesFromFunamentalCycle(res, arc);
        }
        return res;
    }

    private void extractKuratowskiSubdivision(ConflictPair conflictPair, Arc failedArcSource, boolean previousConstraints) {
        if (DEBUG) {
            System.out.println("Extracting Kuratowski subdivision");
        }
        Set<Arc> edges = new HashSet<>();
        addFundamentalCycle(edges, failedArcSource);
        Arc leftLow = conflictPair.left.low;
        Arc rightLow = conflictPair.right.low;
        if (previousConstraints) {
            if (DEBUG)
                System.out.println("Previous constraints, return");
            return;
        } else {
            Arc failedArc = failedArcSource.lowPointArc;
            Node failedNode = failedArcSource.source;
            Pair<Arc, Arc> fork = forkArc(leftLow, rightLow);
            Arc leftFork = fork.getFirst();
            Arc rightFork = fork.getSecond();
            Node forkNode = leftFork.source;
            if (leftFork.lowPointArc.target.isHigherThan(rightFork.lowPointArc.target) || !leftFork.isTreeArc()) {
                if (DEBUG)
                    System.out.println("Swapping the sides");
                Arc t = leftFork;
                leftFork = rightFork;
                rightFork = t;
                t = leftLow;
                leftLow = rightLow;
                rightLow = t;
                conflictPair.swap();
            }
            assert leftFork.isTreeArc();
            if (DEBUG) {
                System.out.printf("Failed arc source = %s, failed arc low point arc = %s\n", failedArcSource.toString(), failedArc.toString());
                System.out.printf("Left low = %s, right low = %s\n", leftLow.toString(), rightLow.toString());
                System.out.printf("Left fork = %s, right fork = %s\n", leftFork.toString(), rightFork.toString());
                System.out.printf("Left fork low point arc = %s\n", leftFork.lowPointArc.toString());
            }

            Set<Node> forbiddenNodes = getForbidden(leftLow, rightLow, leftFork.lowPointArc, failedArc);
            Arc leftHigh = searchPath(forbiddenNodes, leftLow.source, forkNode, rightLow.lowPoint, forkNode.height);
            if (leftHigh == null) {
                leftHigh = searchPath(forbiddenNodes, leftFork.lowPointArc.source, forkNode, rightLow.lowPoint, forkNode.height);
            }
            if (leftHigh != null) {
                addNodesFromFunamentalCycle(forbiddenNodes, leftHigh);
            }

            Arc rightHigh = searchPath(forbiddenNodes, rightLow.source, forkNode, rightLow.lowPoint, forkNode.height);
            if (rightHigh != null) {
                addNodesFromFunamentalCycle(forbiddenNodes, rightHigh);
            }

            Arc neededArc = searchPath(forbiddenNodes, forkNode, -1, leftLow.lowPoint);
            // TODO fix needed arc ending at leftLow.lowPoint
            if (DEBUG) {
                System.out.printf("Left high arc = %s\nNeeded arc: %s\nRight high arc = %s\n", String.valueOf(leftHigh), String.valueOf(neededArc), String.valueOf(rightHigh));
            }

            if (leftLow.endsHigherThan(rightLow)) {
                if (DEBUG) {
                    System.out.println("Simple case 1");
                }
                addFundamentalCycle(edges, leftLow, rightLow, leftFork.lowPointArc);
            } else if (leftFork.lowPointArc != leftLow && rightFork.lowPointArc != rightLow) {
                if (DEBUG) {
                    System.out.println("Simple case 2");
                }
                assert leftFork.lowPoint == rightFork.lowPoint;
                addFundamentalCycle(edges, leftLow, rightLow, leftFork.lowPointArc, rightFork.lowPointArc);
                removePath(edges, leftLow.target, leftFork.lowPointArc.target);
            } else if (neededArc != null && rightHigh != null) {
                assert leftHigh != null;
                addFundamentalCycle(edges, leftLow, rightHigh, leftHigh, rightHigh, neededArc);
                removePath(edges, forkNode, highest(failedNode, leftHigh.target, rightHigh.target));
                removePath(edges, lowest(failedNode, leftHigh.target, rightHigh.target), rightLow.target);
            } else {
                assert leftHigh != null;
                assert leftHigh.lowPoint > rightLow.lowPoint && leftHigh.lowPoint < forkNode.height;
                if (leftHigh.lowPoint < failedArcSource.source.height) {
                    // case 7.1
                    if (DEBUG)
                        System.out.println("Case 7.1");
                    addFundamentalCycle(edges, leftFork.lowPointArc);
                    addFundamentalCycle(edges, leftHigh);
                    addFundamentalCycle(edges, rightLow);
                } else if (neededArc == null) {
                    assert leftFork.lowPointArc != leftLow;
                    // case 7.2
                    addFundamentalCycle(edges, leftLow);
                    addFundamentalCycle(edges, rightLow);
                    addFundamentalCycle(edges, leftHigh);

                    Node lowHighFork = forkNode(leftLow, leftHigh);
                    Set<Node> forbidden = getForbidden(leftLow, leftHigh, rightLow, failedArc); // note: no leftLow.lowPointArc
                    Arc lowPointArc = searchPath(forbiddenNodes, lowHighFork.parentArc.source, forkNode, leftLow.lowPoint - 1, leftLow.lowPoint + 1);
                    Node lowPointLowFork = forkNode(leftLow, lowPointArc);
                    Node lowPointHighFork = forkNode(leftHigh, lowPointArc);
                    if (!lowHighFork.isHigherThan(lowPointHighFork) && !lowHighFork.isHigherThan(lowPointLowFork)) {
                        if (DEBUG)
                            System.out.println("Extended 7.2 case");
                        boolean fromLow = true;
                        Arc forceArc = searchPath(forbidden, leftLow.source, lowHighFork.parentArc.source, leftHigh.lowPoint, lowHighFork.height);
                        if (forceArc == null) {
                            fromLow = false;
                            forceArc = searchPath(forbiddenNodes, rightLow.source, lowHighFork, leftHigh.lowPoint, lowHighFork.height);
                        }
                        assert forceArc != null;
                        // TODO fix immediate force
                        addNodesFromFunamentalCycle(forbidden, forceArc);
                        lowPointArc = searchPath(forbidden, forceArc.source, lowHighFork, -1, leftFork.lowPoint + 1);
                        assert lowPointArc != null;
                        assert lowPointArc.lowPoint == leftFork.lowPoint;
                        addFundamentalCycle(edges, lowPointArc);
                        addFundamentalCycle(edges, forceArc);
                        if (leftLow.endsLowerThan(rightLow)) {
                            Node forceLowPointFork = forkNode(lowPointArc, forceArc);
                            if (fromLow) {
                                // when from left low arc
                                Node lowFork = forkNode(leftLow, lowPointArc);
                                removePath(edges, forceLowPointFork, lowFork);
                            } else {
                                // when from left high arc
                                Node highFork = forkNode(leftHigh, lowPointArc);
                                removePath(edges, forceLowPointFork, highFork);
                            }
                            if (forceArc.endsHigherThan(forkNode)) {
                                System.out.println("Case 7.2.1");
                                removePath(edges, lowest(forkNode, leftHigh.target), rightLow.target);
                            } else {
                                System.out.println("Case 7.2.2");
                                removePath(edges, forceArc.target, leftHigh.target);
                            }
                        } else {
                            System.out.println("Case 7.2.3");
                            removePath(edges, lowHighFork, forkNode);
                            removePath(edges, lowest(failedNode, leftHigh.target), leftLow.target);
                        }
                    } else {
                        if (DEBUG)
                            System.out.println("Case 7.2");
                        addFundamentalCycle(edges, leftFork.lowPointArc);
                        removePath(edges, failedArcSource.source, rightLow.target);
                    }
                } else if (leftLow.lowPoint == rightLow.lowPoint) {
                    if (leftHigh.lowPoint > failedArcSource.source.height) {
                        int[] heights = {neededArc.lowPoint, leftFork.lowPoint, failedArc.lowPoint};
                        Arrays.sort(heights);
                        if (heights[1] == heights[2]) {
                            // case 1.1
                            if (DEBUG)
                                System.out.println("Case 1.1");
                            addFundamentalCycle(edges, leftHigh);
                            addFundamentalCycle(edges, leftFork.lowPointArc);
                            addFundamentalCycle(edges, rightLow);
                            addFundamentalCycle(edges, leftLow);
                            addFundamentalCycle(edges, neededArc);
                            Node fork1 = forkNode(leftFork.lowPointArc, leftLow);
                            Node fork2 = forkNode(leftFork.lowPointArc, leftHigh);
                            Node high = fork1.height > fork2.height ? fork2 : fork1;
                            removePath(edges, high, forkNode);
                            removePath(edges, leftLow.target, leftFork.lowPointArc.target);
                        } else if (neededArc.lowPoint > leftFork.lowPoint && neededArc.lowPoint > failedArc.lowPoint) {
                            // case 1.2
                            if (DEBUG)
                                System.out.println("Case 1.2");
                            addFundamentalCycle(edges, leftHigh);
                            addFundamentalCycle(edges, leftFork.lowPointArc);
                            addFundamentalCycle(edges, leftLow);
                            addFundamentalCycle(edges, neededArc);
                            Node fork1 = forkNode(leftFork.lowPointArc, leftLow);
                            Node fork2 = forkNode(leftFork.lowPointArc, leftHigh);
                            Node high = fork1.height > fork2.height ? fork2 : fork1;
                            removePath(edges, high, forkNode);
                            removePath(edges, failedArcSource.source, leftLow.target);
                        } else {
                            // case 1.3
                            if (DEBUG)
                                System.out.println("Case 1.3");
                            addFundamentalCycle(edges, leftHigh);
                            addFundamentalCycle(edges, leftFork.lowPointArc);
                            addFundamentalCycle(edges, rightLow);
                            addFundamentalCycle(edges, neededArc);
                            Node fork1 = forkNode(leftFork.lowPointArc, leftLow);
                            Node fork2 = forkNode(leftFork.lowPointArc, leftHigh);
                            Node high = fork1.height > fork2.height ? fork2 : fork1;
                            removePath(edges, high, forkNode);
                        }
                    } else if (leftHigh.lowPoint == failedArcSource.source.height) {
                        int[] heights = {neededArc.lowPoint, leftFork.lowPoint, failedArc.lowPoint};
                        Arrays.sort(heights);
                        if (heights[1] == heights[2]) {
                            // case 2.1
                            Node lowHighFork = forkNode(leftLow, leftHigh);
                            Node lowPointLowFork = forkNode(leftLow, leftFork.lowPointArc);
                            Node lowPointHighFork = forkNode(leftHigh, leftFork.lowPointArc);
                            if (lowHighFork.height == lowPointLowFork.height && lowPointLowFork.height == lowPointHighFork.height) {
                                // case 2.1.1
                                // extracting K_5
                                if (DEBUG)
                                    System.out.println("Case 2.1.1");
                                addFundamentalCycle(edges, leftHigh);
                                addFundamentalCycle(edges, leftFork.lowPointArc);
                                addFundamentalCycle(edges, rightLow);
                                addFundamentalCycle(edges, leftLow);
                                addFundamentalCycle(edges, neededArc);
                            } else if (lowPointLowFork.height > lowPointHighFork.height) {
                                // case 2.1.2
                                if (DEBUG)
                                    System.out.println("Case 2.1.2");
                                assert lowPointLowFork.height > lowHighFork.height;
                                addFundamentalCycle(edges, leftHigh);
                                addFundamentalCycle(edges, leftFork.lowPointArc);
                                addFundamentalCycle(edges, rightLow);
                                addFundamentalCycle(edges, leftLow);
                                addFundamentalCycle(edges, neededArc);
                                removePath(edges, leftLow.target, leftFork.lowPointArc.target);
                                removePath(edges, forkNode, leftHigh.target);
                            } else if (lowHighFork.height > lowPointHighFork.height) {
                                // case 2.1.3
                                if (DEBUG)
                                    System.out.println("Case 2.1.3");
                                assert lowHighFork.height > lowPointLowFork.height;
                                addFundamentalCycle(edges, leftHigh);
                                addFundamentalCycle(edges, leftFork.lowPointArc);
                                addFundamentalCycle(edges, rightLow);
                                addFundamentalCycle(edges, leftLow);
                                removePath(edges, leftHigh.target, leftLow.target);
                            } else {
                                // case 2.1.4
                                if (DEBUG)
                                    System.out.println("Case 2.1.4");
                                assert lowPointLowFork.height > lowHighFork.height;
                                assert lowPointLowFork.height > lowPointLowFork.height;
                                addFundamentalCycle(edges, leftHigh);
                                addFundamentalCycle(edges, leftFork.lowPointArc);
                                addFundamentalCycle(edges, leftLow);
                                addFundamentalCycle(edges, neededArc);
                                removeFundamentalCycle(edges, failedArc);
                            }
                        } else if (neededArc.lowPoint > leftFork.lowPoint && neededArc.lowPoint > failedArc.lowPoint) {
                            // case 2.2
                            if (DEBUG)
                                System.out.println("Case 2.2");
                            addFundamentalCycle(edges, leftLow);
                            addFundamentalCycle(edges, leftFork.lowPointArc);
                            addFundamentalCycle(edges, neededArc);
                        } else {
                            // case 2.3
                            if (DEBUG)
                                System.out.println("Case 2.3");
                            addFundamentalCycle(edges, leftHigh);
                            addFundamentalCycle(edges, leftFork.lowPointArc);
                            addFundamentalCycle(edges, neededArc);
                            addFundamentalCycle(edges, rightLow);
                            removePath(edges, forkNode, leftLow.target);
                        }
                    } else {
                        // leftHigh.lowPoint < failedArcSource.source.height
                        addFundamentalCycle(edges, leftHigh);
                        addFundamentalCycle(edges, rightLow);
                        if (leftFork.lowPointArc == leftLow) {
                            // case 3.1
                            if (DEBUG)
                                System.out.println("Case 3.1");
                            addFundamentalCycle(edges, neededArc);
                        } else {
                            // case 3.2
                            if (DEBUG)
                                System.out.println("Case 3.2");
                            addFundamentalCycle(edges, leftFork.lowPointArc);
                        }
                    }
                } else {
                    if (leftHigh.lowPoint < failedArcSource.source.height) {
                        // case 4.1
                        if (DEBUG)
                            System.out.println("Case 4.1");
                        addFundamentalCycle(edges, leftLow);
                        addFundamentalCycle(edges, leftHigh);
                        addFundamentalCycle(edges, rightLow);
                    } else if (leftHigh.lowPoint == failedArcSource.source.height) {
                        // case 5.2
                        if (DEBUG)
                            System.out.println("Case 5.2");
                        addFundamentalCycle(edges, leftLow);
                        addFundamentalCycle(edges, leftHigh);
                        addFundamentalCycle(edges, rightLow);
                        addFundamentalCycle(edges, neededArc);
                        removePath(edges, forkNode, leftHigh.target);
                    } else {
                        // case 6.2
                        if (DEBUG)
                            System.out.println("Case 6.2");
                        addFundamentalCycle(edges, leftLow);
                        addFundamentalCycle(edges, leftHigh);
                        addFundamentalCycle(edges, rightLow);
                        addFundamentalCycle(edges, neededArc);
                        Node lowHighForkNode = forkNode(leftLow, leftHigh);
                        removePath(edges, lowHighForkNode, forkNode);
                    }
                }
            }


        }
        Set<Integer> vertexSubset = new HashSet<>();
        Set<DefaultEdge> edgeSubset = new HashSet<>();
        edges.forEach(a -> {
            edgeSubset.add(a.graphEdge);
            vertexSubset.add(a.source.graphVertex);
            vertexSubset.add(a.target.graphVertex);
        });
        subdivision = new AsSubgraph<>(graph, vertexSubset, edgeSubset);
        assert isKuratowskiSubdivision(subdivision);
        if (DEBUG)
            System.out.println("Is Kuratowski subdivision: true");
    }

    private Arc findArc(int low, int high, Node current) {
        for (Arc arc : current.arcs) {
            if (arc.isTreeArc()) {
                Arc res = findArc(low, high, arc.target);
                if (res != null) {
                    return res;
                }
            } else if (arc.target.height > low && arc.target.height < high) {
                return arc;
            }
        }
        return null;
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
                if (DEBUG) {
                    System.out.println("Top conflict pair:");
                    System.out.println(currentPair);
                }
                extractKuratowskiSubdivision(currentPair, currentArc.source.arcs.get(0), false);
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
                if (DEBUG) {
                    System.out.println("Top conflict pair:");
                    System.out.println(currentPair);
                }
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
        Integer graphVertex;
        List<Arc> arcs;
        List<DefaultEdge> embedded;
        int height;
        Arc leftRef;
        Arc rightRef;

        Node(Integer graphVertex, int degree) {
            this.graphVertex = graphVertex;
            this.arcs = new ArrayList<>();
            this.embedded = new ArrayList<>(degree);
            this.height = -1;
        }

        boolean isHigherThan(Node node) {
            return height > node.height;
        }

        boolean isLowerThan(Node node) {
            return height < node.height;
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
        DefaultEdge graphEdge;
        Node source;
        Node target;
        int lowPoint;
        int lowPoint2;
        int nestingDepth;
        boolean visited;
        Arc ref;
        Arc lowPointArc;
        ConflictPair stackBottom;

        Arc(DefaultEdge graphEdge, Node source, Node target) {
            this.graphEdge = graphEdge;
            this.source = source;
            this.target = target;
            this.side = 1;
            lowPoint = lowPoint2 = -1;
        }

        public Arc(DefaultEdge graphEdge, Node source, Node target, int lowPoint, int lowPoint2, int nestingDepth) {
            this.graphEdge = graphEdge;
            this.source = source;
            this.target = target;
            this.lowPoint = lowPoint;
            this.lowPoint2 = lowPoint2;
            this.nestingDepth = nestingDepth;
        }

        boolean endsHigherThan(Arc arc) {
            return lowPointArc.target.isHigherThan(arc.lowPointArc.target);
        }

        boolean endsLowerThan(Arc arc) {
            return lowPointArc.target.isLowerThan(arc.lowPointArc.target);
        }

        boolean endsHigherThan(Node node) {
            return lowPointArc.target.isHigherThan(node);
        }

        boolean endsLowerThan(Node node) {
            return lowPointArc.target.isLowerThan(node);
        }

        public String toString(boolean full) {
            String res = toString();
            if (full) {
                res += String.format(": lowpoint = %d, lowpoint2 = %d, nesting_depth = %d, side = %d, ref = %s, low pt. arc = %s", lowPoint, lowPoint2, nestingDepth, side, String.valueOf(ref), String.valueOf(lowPointArc));
            }
            return res;
        }

        public boolean isTreeArc() {
            return this == target.parentArc;
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
