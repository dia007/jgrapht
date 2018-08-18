package org.jgrapht.alg.interval;

import com.sun.istack.internal.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.cycle.ChordalityInspector;
import org.jgrapht.graph.interval.Interval;
import org.jgrapht.graph.interval.IntervalVertexPair;

import java.util.*;

/**
 * TODO: better Javadoc
 * @author Ira Justus Fesefeldt (PhoenixIra)
 * @author Jiong Fu
 * @author Suchanda Bhattacharyya (dia007)
 * @author Timofey Chudakov
 *
 * @param <V> the vertex type of the graph
 * @param <E> the edge type of the graph
 */
public class KorteMoehringIntervalGraphRecognizer<V, E> implements IntervalGraphRecognizerInterface<V>
{

    /**
     * The graph to be recognized
     */
    private Graph<V, E> graph;

    /**
     * The chordal graph inspector
     */
    private ChordalityInspector<V, E> chordalInspector;

    /**
     * The root of the MPQ tree
     */
    private MPQTreeNode treeRoot;

    /**
     * The mapping from a vertex in the graph to a set of nodes in the MPQ tree, in order to reach the associated node quickly
     */
    private HashMap<V, Set<MPQTreeNode>> vertexNodeMap;

    /**
     * The boolean flag indicating if the input graph is an interval graph
     */
    private boolean isIntervalGraph;

    /**
     * Constructor for the algorithm
     *
     * @param graph the graph to be recognized
     */
    public KorteMoehringIntervalGraphRecognizer(Graph<V, E> graph) {
        this.graph = graph;
        chordalInspector = new ChordalityInspector<>(graph);
        treeRoot = new PNode(null);
    }

    /**
     * TODO: better Javadoc
     * 
     * the Korte-Moehring Algorithm, which tests the graphs with an MPQ tree for an interval representation.
     * If the algorithm returns true, we can computed an interval representation of the MPQ Tree
     * If the algorithm returns false, we can computed an counter example of the MPQ Tree
     */
    private void testIntervalGraph()
    {
        // if the graph is not chordal, then it is not interval
        if (!chordalInspector.isChordal()) {
            isIntervalGraph = false;
            return;
        }

        // get the perfect elimination order for the vertices in the graph
        // TODO: reverse the perfect elimination order retrieved from the chordal inspector
        List<V> perfectEliminationOrder = chordalInspector.getPerfectEliminationOrder();
        Map<V, Integer> vertexIndexMap = getVertexIndexMap(perfectEliminationOrder);

        // iterate over the perfect elimination order
        for (V u : perfectEliminationOrder) {

            // get the predecessors of the vertex u
            Set<V> predecessors = getPredecessors(vertexIndexMap, u);

            // special case for predecessors is empty
            if (predecessors.isEmpty()) {
                addEmptyPredecessors(u);
                continue;
            }

            // labeling
            Queue<MPQTreeNode> treeNodeQueue = new LinkedList<>();

            // phase A
            for (V vertex : predecessors) {
                MPQTreeNode associatedNode = getAssociatedTreeNode(vertex);
                if (associatedNode == null) {
                    throw new IllegalStateException("The vertex is not found in the MPQ tree.");
                }

                if (associatedNode.getClass() == QSectionNode.class) {
                    QSectionNode qSectionNode = (QSectionNode) associatedNode;
                    if (!qSectionNode.isLeftmostSection() && !qSectionNode.isRightmostSection()) {
                        return; // TODO: then the input graph is not an interval graph, return false here in the future
                    }
                }

                removeVertexFromAssociatedTreeNode(vertex, associatedNode);

                // put the node or the outer section on a queue
                treeNodeQueue.add(associatedNode);
            }

            // phase B
            List<MPQTreeNode> markedTreeNodes = new LinkedList<>();

            while (!treeNodeQueue.isEmpty()) {

                // delete the tree node from the front of the queue
                MPQTreeNode currentTreeNode = treeNodeQueue.remove();

                // if the tree node is unmarked, mark it and add its father at the rear of the queue
                if (!markedTreeNodes.contains(currentTreeNode)) {
                    markedTreeNodes.add(currentTreeNode);
                    if (currentTreeNode.parent != null) {
                        treeNodeQueue.add(currentTreeNode.parent);
                    }
                }
            }

            Map<MPQTreeNode,Integer> positiveLabels = getPositiveLabels(predecessors);

            // test phase:
            // check for path of positive labels
            if(!testPath(positiveLabels.keySet()) 
                    //check if outer sections of Q nodes N contain predecessors intersection V(N)
                    | !testOuterSectionsOfQNodes(positiveLabels.keySet(), predecessors))
            {
                //then this is not an interval graph
                isIntervalGraph = false;
                return;
            }

            // update phase:
            // generate the path
            List<MPQTreeNode> path = getPath(positiveLabels.keySet());

            //get lowest positive node in path
            MPQTreeNode smallestNode = getSmallN(path, positiveLabels);

            //get highest non-inf node in path
            MPQTreeNode biggestNode = getBigN(path, positiveLabels);

            //update MPQ Tree
            if(smallestNode.equals(biggestNode))
                addVertexToLeaf(u,path);
            else
                changedPathToTemplates(u,path,smallestNode,biggestNode);

        }
    }

    /**
     * Returns the vertices on the one hand adjacent to the given vertex,
     * on the other hand with indices smaller than the index of the given vertex
     *
     * @param vertexIndexMap the mapping of the vertices in the graph to their indices in an ordering
     * @param vertex         the vertex in the graph to be tested
     * @return the predecessors of {@code vertex} in order defines by {@code map}.
     */
    private Set<V> getPredecessors(Map<V, Integer> vertexIndexMap, V vertex) {
        Set<V> result = new HashSet<>();
        Integer vertexIndex = vertexIndexMap.get(vertex);

        for (E edge : graph.edgesOf(vertex)) {
            V oppositeVertex = Graphs.getOppositeVertex(graph, edge, vertex);
            Integer oppositeIndex = vertexIndexMap.get(oppositeVertex);
            if (oppositeIndex < vertexIndex) {
                result.add(oppositeVertex);
            }
        }

        return result;
    }

    /**
     * Returns a map containing vertices from the {@code vertexOrder} mapped to their indices in
     * {@code vertexOrder}.
     *
     * @param vertexOrder the list of vertices
     * @return a mapping of vertices from {@code vertexOrder} to their indices in {@code vertexOrder}.
     */
    private Map<V, Integer> getVertexIndexMap(List<V> vertexOrder) {
        Map<V, Integer> vertexIndexMap = new HashMap<>(vertexOrder.size());
        for (int i = 0; i < vertexOrder.size(); i++) {
            vertexIndexMap.put(vertexOrder.get(i), i);
        }
        return vertexIndexMap;
    }

    /**
     * Changed the MPQ Tree if u has no predecessors.
     * Adds a new leaf node with the bag of this vertex to the root.
     * 
     * @param u the vertex to be added to the MPQ Tree
     */
    private void addEmptyPredecessors(V u) {
        HashSet<V> elements = new HashSet<>();  // to be implemented by the doubly linked circular list
        elements.add(u);
        MPQTreeNode leaf = new PNode(elements);
        // add leaf to the tree root
        leaf.parent = treeRoot;
    }

    /**
     * Get positive labels of the modified PQ-tree
     *
     * @return the map of the tree node and the positive label
     */
    private Map<MPQTreeNode, Integer> getPositiveLabels(Set<V> predecessors) {
        labelTree(predecessors);
        return null;
    }

    /**
     * Label every node N and every section S of a Q-node
     * according to the relation between the vertex u and the vertices of N or S
     * <p>
     * the label is:
     * - 2, if u is adjacent to all vertices from N or S
     * - 1, if u is adjacent to some vertices from N or S
     * - 0, if u is adjacent to no vertices from N or S
     *
     * @param predecessors the predecessors which are used to label the vertices in the tree
     */
    private void labelTree(Set<V> predecessors) {

    }

    /**
     * Get the node in the MPQ tree associated with the given vertex in the graph from the map
     *
     * @param vertex the vertex in the graph to be tested
     * @return the associated node in the MPQ tree
     */
    private MPQTreeNode getAssociatedTreeNode(V vertex) {
        Set<MPQTreeNode> associatedPositions = vertexNodeMap.get(vertex);

        // if the associated position set is empty, then the associated tree node is not found
        if (associatedPositions.isEmpty()) {
            return null;
        }

        // if the associated position set contains only one element, then the associated tree node is unique
        if (associatedPositions.size() == 1) {
            return associatedPositions.iterator().next();
        }

        // if the associated position set contains more than one element
        throw new IllegalStateException("The vertex is associated with more than one node in the MPQ tree.");
    }

    /**
     * Remove the vertex from the associated node in the MPQ tree
     *
     * @param vertex   the vertex to be removed
     * @param treeNode the associated node of the vertex
     * @return true if the vertex is successfully removed from the associated node, false otherwise
     */
    private boolean removeVertexFromAssociatedTreeNode(V vertex, MPQTreeNode treeNode) {
        if (treeNode.elements == null) {
            throw new IllegalStateException("The element set in the associated node is null.");
        }
        return treeNode.elements.remove(vertex);
    }

    /**
     * TODO: Better Javadoc
     * tests if positiveLabels form a path
     *
     * @param positiveLabels the vertices which should form a path
     * @return true iff it forms a path
     */
    private boolean testPath(Set<MPQTreeNode> positiveLabels) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * TODO: Better Javadoc
     * tests if an outer section of every Q nodes N in positive labels contains predecessors intersection V(N)
     *
     * @param positiveLabels the positive vertices
     * @param predecessors   the predecessors of u
     * @return true iff it fulfills the condition
     */
    private boolean testOuterSectionsOfQNodes(Set<MPQTreeNode> positiveLabels, Set<V> predecessors) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * TODO: better Javadoc
     * computes a path from the root to a leaf, containing all positive vertices
     *
     * @param positiveLabels the vertices which forms a path
     * @return the path from root to a leaf
     */
    private List<MPQTreeNode> getPath(Set<MPQTreeNode> positiveLabels) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * TODO: better Javadoc
     * computes the smallest vertex N of the Tree which has a positive label
     *
     * @param path the path from root to leaf
     * @param positiveLabels the map from nodes to positive labels
     * @return smalles vertex N with positive label
     */
    private MPQTreeNode getSmallN(List<MPQTreeNode> path, Map<MPQTreeNode, Integer> positiveLabels) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * TODO: better Javadoc
     * computes the highest vertex N of the tree which is non-empty and non-inf
     *
     * @param path           the path from root to leaf
     * @param positiveLabels the map from nodes to positive labels
     * @return highest non-empty, non-inf vertex N
     */
    private MPQTreeNode getBigN(List<MPQTreeNode> path, Map<MPQTreeNode, Integer> positiveLabels) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * TODO: better Javadoc
     * Adds the vertex u to the leaf of the path
     * 
     * @param u the vertex to be added
     * @param path the path of the leaf
     */
    //here the path is modified, not the tree 
    private void addVertexToLeaf(V u, List<MPQTreeNode> path)
    {
        //TODO: better naming for the varibles in terms of the paper
        int lastIndexofPath= path.size()-1;
        MPQTreeNode lastNodeInPath = path.get(lastIndexofPath);
        HashMap<Integer, HashSet<V>> partitionedVertexSet = partitionVertexSet(u,graph, lastNodeInPath);

        //check if lastnodeInPath is P or Q or leaf

        //Addition if the node is a leaf

        if(lastNodeInPath.getClass()== Leaf.class) {
            //check if B is empty

            if(partitionedVertexSet.get(1).isEmpty()) {

                path.get(lastIndexofPath).elements.add(u);

            }

            else {
                //transform the leaf containing A+B into a PNode containing A
                PNode newPNode = new PNode( partitionedVertexSet.get(0));
                //Create two leaves for PNode children, add the children to the PNode

                Leaf leaf1 = new Leaf(u);
                Leaf leaf2 = new Leaf( partitionedVertexSet.get(1));
                leaf1.parent=newPNode;
                leaf2.parent=newPNode;
                //add children to the PNode
                //need to implement the add child option



                newPNode.addChild(leaf1);
                newPNode.addChild(leaf2);
                path.remove(lastIndexofPath);

                path.add(newPNode);


            }

        } 


        //Addition if the last node in path is the PNode
        else if (lastNodeInPath.getClass()== PNode.class) {

            PNode tempPNode = (KorteMoehringIntervalGraphRecognizer<V, E>.PNode) lastNodeInPath;
            if(partitionedVertexSet.get(1).isEmpty()) {
                //swap P node containing A+B with P Node containing just A,since B is empty makes no change
                //keep it the same, just add the new leaves there
                Leaf newLeaf= new Leaf(u);
                newLeaf.parent=tempPNode;
                tempPNode.addChild(newLeaf);
                tempPNode.currentChild=newLeaf;

                path.remove(lastIndexofPath);
                path.add(tempPNode);


            }else {
                //update the previous PNode elements with elements in A

                PNode newPNodeA= new PNode(partitionedVertexSet.get(0));
                //create a new PNode and add the B set

                PNode newPNodeB = tempPNode;
                newPNodeB.elements.removeAll(partitionedVertexSet.get(0));
                newPNodeA.addChild(newPNodeB);
                newPNodeB.parent=newPNodeA;

                Leaf newChildLeaf = new Leaf(u);
                newPNodeA.addChild(newChildLeaf);
                newChildLeaf.parent=newPNodeA;

                newPNodeA.currentChild=newChildLeaf;
                path.remove(lastIndexofPath);
                path.add(newPNodeA);




            }


        } 


        //last node is a Qnode

        else if(lastNodeInPath.getClass()== QNode.class) {



            //test if all sections contains A or not

            // remove the lastNodeinPath and need to perform some addition
            QNode currentQNode = (KorteMoehringIntervalGraphRecognizer<V, E>.QNode) lastNodeInPath ;
            boolean containsA = allSectionsContainsElementSet(currentQNode, partitionedVertexSet.get(0));
            //if containsA == true Q1, else Q2 (both the cases of Q2)


            if(containsA) {
                //Q1

                //create a new PNode A 
                PNode newElementPNode = new PNode(partitionedVertexSet.get(0));

                Leaf newLeaf = new Leaf(u);
                newElementPNode.addChild(newLeaf);
                newLeaf.parent=newElementPNode;
                newElementPNode.addChild(newLeaf);
                newLeaf.parent=newElementPNode;



                QNode newChildQNode = extractQNodeElements(currentQNode,partitionedVertexSet.get(0) );
                newElementPNode.addChild(newChildQNode); 
                newChildQNode.parent=newElementPNode;


                path.remove(currentQNode);
                path.add(newElementPNode);


            }else {

                //TODO: implement the helper functions

                //check if B is null

                if(partitionedVertexSet.get(1).isEmpty()) {

                    PNode newChildPNode = new PNode (null);

                    //take the existing childsubtree of the QNode and add it to the PNode
                    newChildPNode.addChild(currentQNode.leftmostSection.child);
                    currentQNode.leftmostSection.child.parent=newChildPNode;


                    Leaf newChildLeaf = new Leaf (u);
                    newChildPNode.addChild(newChildLeaf);
                    newChildLeaf.parent=newChildPNode;
                    newChildPNode.currentChild=  newChildLeaf;


                    currentQNode.leftmostSection.child=newChildPNode;
                    newChildPNode.parent=currentQNode.leftmostSection;



                }else {

                    //create a new QSection

                    QSectionNode newQSectionNode = new QSectionNode(partitionedVertexSet.get(0));
                    newQSectionNode.rightSibling=currentQNode.leftmostSection;
                    currentQNode.leftmostSection.leftSibling=newQSectionNode;
                    currentQNode.leftmostSection=newQSectionNode;

                    Leaf newChildLeaf = new Leaf (u);
                    currentQNode.leftmostSection.child=newChildLeaf;
                    newChildLeaf.parent=currentQNode.leftmostSection;

                }

            }




        }
    }
    //make this lessless  ugly and javadoc

    private QNode extractQNodeElements(QNode qNode, HashSet<V> elementSet) {

        for(QSectionNode curerntQSectionNode = qNode.leftmostSection; curerntQSectionNode.rightSibling!=null; curerntQSectionNode= curerntQSectionNode.rightSibling ) {
            curerntQSectionNode.elements.removeAll(elementSet);
        }



        return qNode;
    }



    private boolean allSectionsContainsElementSet(QNode newQNode, HashSet<V> elements) {
        //traverse all sections of the QNode and check if every section contains an A
        //should there be a section identifier

        QSectionNode leftSection = newQNode.leftmostSection;
        QSectionNode rightSection = newQNode.rightmostSection;
        boolean isContained= false;


        while	(leftSection != rightSection) {

            if (leftSection.elements.contains(elements) && rightSection.elements.contains(elements)) {

                leftSection=leftSection.rightSibling;
                rightSection = rightSection.leftSibling;
                isContained= true;


            }

            else {
                isContained = false;
            }


        }

        return isContained;
    }



    /**
     * @param Vertex u
     * @param Interval Graph graph
     * @param MPQTree Node node
     * @return HashMap containing the partitioned vertex sets
     */
    HashMap<Integer, HashSet<V>> partitionVertexSet(V u, Graph graph, MPQTreeNode node){

        //find all the vertices associated with the node
        HashSet<V> elementsInNode = node.elements;


        //find vertices adjacent to u in graph G
        @SuppressWarnings("unchecked")
        HashSet<V> neighbourVerticesofV = new HashSet<V>(Graphs.neighborListOf(graph, u));

        HashSet<V> vertexPartitionSetA = new HashSet<>();
        HashSet<V> vertexPartitionSetB = new HashSet<>();
        Map<Integer, HashSet<V>> vertexPartitionMap= new HashMap<Integer, HashSet<V>>();


        if(neighbourVerticesofV.equals(elementsInNode)) {

            vertexPartitionSetA =  neighbourVerticesofV; 
            vertexPartitionMap.put(0,vertexPartitionSetA);
            vertexPartitionMap.put(1, vertexPartitionSetB);

        }
        else {
            for(V vertex:elementsInNode ) {
                if(neighbourVerticesofV.contains(vertex)) {

                    vertexPartitionSetA.add(vertex);
                }else {
                    vertexPartitionSetB.add(vertex);
                }
            }

            vertexPartitionMap.put(0,vertexPartitionSetA);
            vertexPartitionMap.put(1,vertexPartitionSetB);
        }



        return (HashMap<Integer, HashSet<V>>) vertexPartitionMap;

    }



    /**
     * TODO: better Javadoc
     * Checks the path for specific patterns and changes every node accordingly
     * 
     * @param u the vertex to add to the tree
     * @param path the path of vertices to be changed
     * @param nSmall the smalles positive node in path
     * @param nBig the highest non-empty, non-inf node in path
     */
    private void changedPathToTemplates(V u, List<MPQTreeNode> path, MPQTreeNode nSmall, MPQTreeNode nBig)
    {
        //traverse from nSmall to nBig
        int minIndex = path.indexOf(nSmall);
        int currentIndex = path.indexOf(nSmall);
        int maxIndex = path.indexOf(nBig);
        //   while(currentIndex != maxIndex )  

        for(int i = currentIndex; i<=maxIndex; i++){

            MPQTreeNode currentNode = path.get(currentIndex);
            //split the vertex initially
            HashMap<Integer, HashSet<V>> partitionedVertexSet  = partitionVertexSet(u, graph, currentNode);


            //CHECK the type of node
            if(currentNode.getClass()==Leaf.class) {

                //just make a check about the current index being equal to nSmall, remove later -> TODO

                if(currentIndex==minIndex) {

                    //create a Qnode with 2 section nodes 

                    QSectionNode newQSection1 = new QSectionNode(partitionedVertexSet.get(0));                  
                    QSectionNode newQSection2 = new QSectionNode(partitionedVertexSet.get(0));

                    QNode newElementQNode = new QNode(newQSection1);
                    newElementQNode.addChild(newQSection2);
                    newElementQNode.leftmostSection=newQSection1;
                    newElementQNode.rightmostSection=newQSection2;

                    newQSection1.rightSibling=newQSection2;
                    newQSection1.parent=newElementQNode;

                    newQSection2.leftSibling=newQSection1;
                    newQSection2.parent=newElementQNode;


                    Leaf leftSectionChildLeaf = new Leaf(u);
                    Leaf righSectiontChildLeaf = new Leaf(partitionedVertexSet.get(1));

                    newQSection1.child=leftSectionChildLeaf;
                    leftSectionChildLeaf.parent=newQSection1;

                    newQSection2.child=righSectiontChildLeaf;
                    righSectiontChildLeaf.parent=newQSection2;



                    path.remove(currentIndex);
                    path.add(newElementQNode);




                }


            }

            else if (path.get(currentIndex).getClass()==PNode.class) {
                PNode tempPNode=(KorteMoehringIntervalGraphRecognizer<V, E>.PNode) path.get(currentIndex);


                if(currentIndex==minIndex) {

                    QSectionNode newQSectionNode1 = new QSectionNode(partitionedVertexSet.get(0));                  
                    QSectionNode newQSectionNode2 = new QSectionNode(partitionedVertexSet.get(0));

                    QNode newQNode = new QNode(newQSectionNode1);
                    newQNode.addChild(newQSectionNode2);

                    newQNode.leftmostSection=newQSectionNode1;
                    newQSectionNode1.parent=newQNode;

                    newQNode.rightmostSection=newQSectionNode2;
                    newQSectionNode2.parent=newQNode;

                    newQSectionNode1.rightSibling=newQSectionNode2;
                    newQSectionNode2.leftSibling=newQSectionNode1;

                    Leaf leftChildLeaf = new Leaf(u);

                    PNode rightChildPNode = tempPNode;
                    rightChildPNode.elements.remove(partitionedVertexSet.get(0));

                    newQSectionNode1.addChild(leftChildLeaf);
                    leftChildLeaf.parent=newQSectionNode1;

                    newQSectionNode2.addChild(rightChildPNode);
                    rightChildPNode.parent=newQSectionNode2;


                    path.remove(currentIndex);
                    path.add(newQNode);






                }
                else {
                    //TODO: implement helper classes here
                    QSectionNode rightMostQSection = new QSectionNode(tempPNode.elements);
                    PNode rightmostQSectionChildPNode = tempPNode;
                    rightmostQSectionChildPNode.elements.removeAll(tempPNode.elements);
                    rightMostQSection.addChild(rightmostQSectionChildPNode);
                    rightmostQSectionChildPNode.parent=rightMostQSection;

                    QNode tempQNode = (KorteMoehringIntervalGraphRecognizer<V, E>.QNode) tempPNode.currentChild;
                    QNode newElementQNode = tempQNode;


                    ;

                    for(QSectionNode currentSection = newElementQNode.leftmostSection; currentSection.rightSibling!=null; currentSection= currentSection.rightSibling) {

                        if(currentSection == newElementQNode.leftmostSection ) {


                            currentSection.elements.addAll(partitionedVertexSet.get(0));


                        }

                        else {

                            currentSection.elements.addAll(tempPNode.elements);

                        }
                    }


                    //need to add addSection for QNode here

                    newElementQNode.rightmostSection.rightSibling=rightMostQSection;
                    rightMostQSection.leftSibling=newElementQNode.rightmostSection;
                    newElementQNode.rightmostSection=rightMostQSection;


                    path.remove(tempPNode);
                    path.add(newElementQNode);


                }







            }

            else if (path.get(currentIndex).getClass()==QNode.class) {

                QNode tempQNode = (KorteMoehringIntervalGraphRecognizer<V, E>.QNode) path.get(currentIndex);

                //first check:current node = NSmall, second check A is present in everything


                if(currentIndex==minIndex) {

                    //second check A is present in everything

                    if(allSectionsContainsElementSet(tempQNode, partitionedVertexSet.get(0))) {
                        //create Qnode1

                        QSectionNode rightMostSection = new QSectionNode(partitionedVertexSet.get(0));
                        QSectionNode leftMostSection = new QSectionNode(partitionedVertexSet.get(0));

                        QNode newQNodeA = new QNode(leftMostSection);
                        //need to add rightmost section in newQNodeA
                        newQNodeA.leftmostSection=leftMostSection;
                        newQNodeA.rightmostSection=rightMostSection;

                        leftMostSection.rightSibling=rightMostSection;
                        rightMostSection.leftSibling=leftMostSection;

                        Leaf newChild = new Leaf(u);

                        leftMostSection.addChild(newChild);
                        leftMostSection.child=newChild;
                        newChild.parent=leftMostSection;


                        //create QNode2



                        QNode newQNodeB = tempQNode;

                        //TODO: check this part,

                        for(QSectionNode currentSection = newQNodeB.leftmostSection; currentSection.rightSibling != null; currentSection = currentSection.rightSibling ) {


                            currentSection.elements.removeAll(partitionedVertexSet.get(0));

                        }

                        rightMostSection.addChild(newQNodeB);
                        rightMostSection.child=newQNodeB;
                        newQNodeB.parent=rightMostSection;


                        path.remove(tempQNode);
                        path.add(newQNodeA);


                    }else {


                        QSectionNode qSectionNode= new QSectionNode(partitionedVertexSet.get(0));
                        tempQNode.leftmostSection.leftSibling=qSectionNode;
                        qSectionNode.rightSibling=tempQNode.leftmostSection;
                        tempQNode.leftmostSection=qSectionNode;

                        Leaf newChild = new Leaf(u); 
                        tempQNode.addChild(newChild);
                        newChild.parent=tempQNode;


                    }


                }
                else {

                    QNode tempChildQNode=(KorteMoehringIntervalGraphRecognizer<V, E>.QNode) tempQNode.leftmostSection.child;

                    for(QSectionNode currentSection = tempChildQNode.leftmostSection; currentSection.rightSibling != null; currentSection = currentSection.rightSibling ) {
                        if(currentSection==tempChildQNode.leftmostSection) {

                            currentSection.elements.addAll(partitionedVertexSet.get(0));
                        }

                        else {
                            currentSection.elements.addAll(tempQNode.leftmostSection.elements);
                        }

                        tempChildQNode.rightmostSection.rightSibling=tempQNode.leftmostSection.rightSibling;
                        tempQNode.leftmostSection= tempChildQNode.leftmostSection;


                    }


                }



            }











        }




    }

    @Override
    public boolean isIntervalGraph()
    {
        return isIntervalGraph;
    }

    @Override
    public List<Interval<Integer>> getIntervalsSortedByStartingPoint()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<V, IntervalVertexPair<V, Integer>> getVertexToIntervalMap()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Interval<Integer>, V> getIntervalToVertexMap()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * TODO: better javadoc
     * 
     * the hole in the graph as an counter example for chordality
     * @return a hole if the graph is not chordal, or null if the graph is chordal.
     */
    public GraphPath<V,E> getHole()
    {
        return chordalInspector.getHole();
    }

    /**
     * TODO: better javadoc
     * 
     * the Umbrella sub graph in the graph iff the graph is chordal but not an interval graph
     * @return an umbrella if the graph is not an intervalgraph, or null if the graph is an intervalgraph.
     */
    public Graph<V,E> getUmbrellaSubGraph()
    {
        // TODO implement
        return null;
    }

    /**
     * Modified PQ-tree data structure
     */

    /**
     * A node of a modified PQ-tree
     */
    private abstract class MPQTreeNode {

        /**
         * The parent of the current node
         */
        MPQTreeNode parent;

        /**
         * The graph vertices associated with the current tree node
         *
         * The associated set of vertices is given by a doubly linked circular list
         */
        HashSet<V> elements = null;

        /**
         * Instantiate a tree node associating with no graph vertex
         */
        MPQTreeNode() { }

        /**
         * Instantiate a tree node associating with a set of graph vertices
         * TODO: replace this with an addElement method later
         *
         * @param elements a set of graph vertices associated with this tree node
         */
        MPQTreeNode(HashSet<V> elements) {
            this.elements = elements;
        }


        void addChild(MPQTreeNode child) {
            // TODO: add child according to the template operations
        }



        // abstract boolean containsAtMostOneSon();

    }

    /**
     * A P-node of a modified PQ-tree
     */
    private class PNode extends MPQTreeNode {

        /**
         * The children of a P-node are stored with a doubly linked circular list
         * <p>
         * P-node has a pointer of the current child as the entrance to this list
         */
        MPQTreeNode currentChild;

        /**
         * Instantiate a P node associating with a set of graph vertices
         *
         * @param elements a set of graph vertices associated with this P node
         */
        PNode(HashSet<V> elements) {
            super(elements);
        }

        /**
         * add child for the current P-node
         *
         * @param child the child node to be added
         */
        void addChild(MPQTreeNode child) {
            
            



        }




    }

    /**
     * A Q-node of a modified PQ-tree
     */
    private class QNode extends MPQTreeNode {

        /**
         * The children of a Q-node are stored with a doubly linked list
         * <p>
         * Q-node has two pointers of the outermost sections as the entrances to this list
         */
        @NotNull QSectionNode leftmostSection;
        @NotNull QSectionNode rightmostSection;

        /**
         * Instantiate a Q node associating with a set of graph vertices
         */
        QNode(QSectionNode section) {
            super(null); // elements of Q-node are currently stored in the corresponding section nodes, make this null here

            // TODO: check nullability of the input section and raise NullPointerException accordingly, no more nullability check after this point
            this.leftmostSection = section;
            this.rightmostSection = section;
        }

    }

    /**
     * A section node of a Q-node
     */
    private class QSectionNode extends MPQTreeNode {

        /**
         * The child of the current Q section node
         * <p>
         * Each section has a pointer to its son
         */
        MPQTreeNode child;

        /**
         * The sections have a pointer to their neighbor sections
         * <p>
         * For the left most section, the left sibling is null
         * For the right most section, the right sibling is null
         */
        QSectionNode leftSibling;
        QSectionNode rightSibling;

        QSectionNode(HashSet<V> elements) {
            super(elements);
        }

        /**
         * Test if the current Q section node is the leftmost section in the associated Q node by checking if the left sibling is null
         *
         * @return true if the current Q section node is the leftmost section, false otherwise
         */  
        private boolean isLeftmostSection() {
            return this.leftSibling == null;
        }

        /**
         * Test if the current Q section node is the rightmost section in the associated Q node by checking if the right sibling is null
         *
         * @return true if the current Q section node is the rightmost section, false otherwise
         */
        private boolean isRightmostSection() {
            return this.rightSibling == null;
        }

    }

    /**
     * A leaf node of a modified PQ-tree
     */
    /*  private class Leaf extends MPQTreeNode {

        Leaf(HashSet<V> elements) {
            this.elements = elements;
        }

    }*/

    private class Leaf extends MPQTreeNode {

        Leaf(V v) {
            this.elements.add(v);
        }

        Leaf(HashSet<V> elements) {
            this.elements = elements;
        }


    } 


    /**
     * the label of a node N or a section S of a Q-node
     */
    private enum Label {

        ALL(2), SOME(1), NONE(0);

        private int value;

        Label(int value) {
            this.value = value;
        }
    }

}