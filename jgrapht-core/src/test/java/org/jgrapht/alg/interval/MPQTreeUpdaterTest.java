package org.jgrapht.alg.interval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.jgrapht.Graph;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.Leaf;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.MPQTreeNode;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.PNode;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.QNode;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.QSectionNode;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.builder.GraphBuilder;
import org.junit.Test;
import org.junit.experimental.theories.internal.Assignments;

public class MPQTreeUpdaterTest<Vertex> {

    public KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.PNode createPNode(HashSet<Integer> elements) {
        Graph newGraph = createGraph1();

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(newGraph);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.PNode pNode = test.new PNode(elements);
        return pNode;

    }

    public KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QSectionNode createQSectionNode(HashSet<Integer> elements) {
        Graph newGraph = createGraph1();

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(newGraph);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QSectionNode qSectionNode = test.new QSectionNode(elements);
        return qSectionNode;

    }

    public KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.Leaf createLeaf(HashSet<Integer> elements) {
        Graph newGraph = createGraph1();

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(newGraph);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.Leaf leaf = test.new Leaf(elements);
        return leaf;

    }

    public KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QNode createQNode(QSectionNode qSectionNode) {
        Graph newGraph = createGraph1();
        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(newGraph);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QNode qNode = test.new QNode(qSectionNode);
        return qNode;
        

    }

    public Graph<Integer, DefaultEdge> createGraph1() {

        GraphBuilder<Integer, DefaultEdge, ? extends SimpleGraph<Integer, DefaultEdge>> builder 
                = SimpleGraph.createBuilder(DefaultEdge.class);

        builder.addEdge(1, 4);
        builder.addEdge(4, 3);
        builder.addEdge(3, 2);
        return builder.build();

    }


    public Graph<Integer, DefaultEdge> createGraph2() {

        Graph<Integer, DefaultEdge> newGraph = new SimpleGraph<>(DefaultEdge.class);

        newGraph.addVertex(1);
        newGraph.addVertex(2);
        newGraph.addVertex(3);
        newGraph.addEdge(2, 3);

        HashSet<Integer> elements = new HashSet<>();
        elements.add(2);
        elements.add(3);

        return newGraph;

    }



    @Test
    public void testPartitionVertexSet() {

        HashSet<Integer> elements = new HashSet<>();
        elements.add(1);
        elements.add(2);
        elements.add(3);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(createGraph1());

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.PNode node = test.new PNode(elements);

        HashMap<Integer, HashSet<Integer>> resultMap = MPQTreeUpdater.partitionVertexSet(4, createGraph1(), node);
        assertEquals(1, resultMap.get(1).size());
        assertEquals(2, resultMap.get(0).size());


    }


    @Test
    public void testPartitionVertexSet2() {

        HashSet<Integer> elements = new HashSet<>();
        elements.add(1);
        elements.add(4);
        elements.add(3);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(createGraph1());

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.PNode node = test.new PNode(elements);

        HashMap<Integer, HashSet<Integer>> resultMap = MPQTreeUpdater.partitionVertexSet(2, createGraph1(), node);
        assertEquals(2, resultMap.get(1).size());
        assertEquals(1, resultMap.get(0).size());


    }



    @Test
    public void testPartitionVertexSet3() {

        Graph newGraph = createGraph1();
        HashSet<Integer> elements = new HashSet<>();
        elements.add(2);
        elements.add(3);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(newGraph);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.PNode node = test.new PNode(elements);

        HashMap<Integer, HashSet<Integer>> resultMap = MPQTreeUpdater.partitionVertexSet(1, newGraph, node);
        assertEquals(2, resultMap.get(1).size());
        assertEquals(0, resultMap.get(0).size());


    }

    @Test
    public void testPartitionVertexSet4() {
        Graph<Integer, DefaultEdge> newGraph = new SimpleGraph<>(DefaultEdge.class);

        newGraph.addVertex(1);
        newGraph.addVertex(2);
        newGraph.addVertex(3);
        newGraph.addEdge(2, 3);

        HashSet<Integer> elements = new HashSet<>();
        elements.add(6);
        elements.add(8);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(newGraph);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.PNode node = test.new PNode(elements);

        HashMap<Integer, HashSet<Integer>> resultMap = MPQTreeUpdater.partitionVertexSet(2, newGraph, node);
        assertEquals(2, resultMap.get(1).size());
        assertEquals(0, resultMap.get(0).size());


    }
  /*  @Test
   public void allSectionContainsElementsTest() {
       
        HashSet<Integer> elements2 = new HashSet<>();
        elements2.add(6);
        elements2.add(2);

        QSectionNode middleQSectionNode = createQSectionNode(elements2);


        HashSet<Integer> elements3 = new HashSet<>();
         elements3.add(1);
        elements3.add(8);

        QSectionNode rightQSectionNode = createQSectionNode(elements3);
        QSectionNode leftQSectionNode = createQSectionNode(elements3);

        QNode qNode = createQNode(leftQSectionNode);
        qNode.leftmostSection=leftQSectionNode;
        qNode.rightmostSection=rightQSectionNode;
        leftQSectionNode.rightSibling=middleQSectionNode;
        middleQSectionNode.leftSibling=leftQSectionNode;
        rightQSectionNode.leftSibling=middleQSectionNode;
        middleQSectionNode.rightSibling=rightQSectionNode;



        HashSet<Integer> elementsToBeSearched = new HashSet<>();
        elementsToBeSearched.add(2);


        ;
        assertEquals(false, MPQTreeUpdater.allSectionsContainsElementSet(qNode, elementsToBeSearched));

*/





  //  }

    /*    @Test
    public void extractQNodeElementsTest() {

        Graph<Integer, DefaultEdge> newGraph = new SimpleGraph<>(DefaultEdge.class);

        newGraph.addVertex(1);
        newGraph.addVertex(2);
        newGraph.addVertex(3);
        newGraph.addEdge(2, 3);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge> test= new KorteMoehringIntervalGraphRecognizer<>(newGraph);
        HashSet<Integer> elements1 = new HashSet<>();
        elements1.add(2);
        elements1.add(8);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QSectionNode leftQSectionNode = test.new QSectionNode(elements1);

        HashSet<Integer> elements2 = new HashSet<>();
        elements2.add(6);
        elements2.add(2);
        elements2.add(3);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QSectionNode middleQSectionNode = test.new QSectionNode(elements2);


        HashSet<Integer> elements3 = new HashSet<>();
        elements3.add(1);
        elements3.add(8);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QSectionNode rightQSectionNode = test.new QSectionNode(elements3);

        KorteMoehringIntervalGraphRecognizer<Integer, DefaultEdge>.QNode qNode = test.new QNode(leftQSectionNode);
        qNode.leftmostSection=leftQSectionNode;
        qNode.rightmostSection=rightQSectionNode;
        leftQSectionNode.rightSibling=middleQSectionNode;
        middleQSectionNode.leftSibling=leftQSectionNode;
        rightQSectionNode.leftSibling=middleQSectionNode;
        middleQSectionNode.rightSibling=rightQSectionNode;
        leftQSectionNode.leftSibling=null;
        rightQSectionNode.rightSibling=null;





        HashSet<Integer> elementsToBeRemoved = new HashSet<>();
        elementsToBeRemoved.add(2);

        QNode newQNode =MPQTreeUpdater.extractQNodeElements(qNode, elementsToBeRemoved);


        // assertEquals(false ,newQNode.leftmostSection.elements.contains(2));
        //  assertEquals(false ,newQNode.rightmostSection.elements.contains(2));
        //  assertEquals(false ,newQNode.rightmostSection.leftSibling.elements.contains(2));


    }
     */
    //node is a leaf and B= null
    
    @Test
    public void addVertexToLeaf1() {
        
        HashSet<Integer> leafElement = new HashSet<>();
        leafElement.add(1); leafElement.add(3); 
        
        

        Leaf leaf = createLeaf(leafElement);
        ArrayList<MPQTreeNode> path = new ArrayList<MPQTreeNode>();
        path.add(leaf);
        Graph graph = createGraph1();
        
       
        MPQTreeUpdater.addVertexToLeaf(4, path, graph);
       
        assertEquals(1, path.size());
        HashSet<Integer> expectedElements = new HashSet<>();
        expectedElements.add(1);expectedElements.add(3);expectedElements.add(4);
        assertEquals(expectedElements, path.get(0).elements);


    }
    
//node is a leaf and B!= null
    
    @Test
    public void addVertexToLeaf2() {
        
        HashSet<Integer> leafElement = new HashSet<>();
        leafElement.add(1); leafElement.add(2); 
        
        

        Leaf leaf = createLeaf(leafElement);
        ArrayList<MPQTreeNode> path = new ArrayList<MPQTreeNode>();
        path.add(leaf);
        Graph graph = createGraph1();
        
       
        MPQTreeUpdater.addVertexToLeaf(4, path, graph);
       
    //    System.out.println(path.get(0).elements.size());


    }

//node is a PNodez and B= null
    
    @Test
    public void addVertexToLeaf3() {
        
        HashSet<Integer> pNodeElement = new HashSet<>();
        pNodeElement.add(1); pNodeElement.add(3); 
        

        PNode pNode = createPNode(pNodeElement);
        ArrayList<MPQTreeNode> path = new ArrayList<MPQTreeNode>();
        path.add(pNode);
        Graph graph = createGraph1();
        
       
        MPQTreeUpdater.addVertexToLeaf(4, path, graph);
       
 //      System.out.println(path.get(0).elements);
       
       PNode pNode1 = (PNode) path.get(0);
       
    //   System.out.println(pNode1.currentChild.elements);
       


    }
    
    
//node is a PNodez and B!= null
    
    @Test
    public void addVertexToLeaf4() {
        
        HashSet<Integer> pNodeElement = new HashSet<>();
        pNodeElement.add(1); pNodeElement.add(2); 
        

        PNode pNode = createPNode(pNodeElement);
        ArrayList<MPQTreeNode> path = new ArrayList<MPQTreeNode>();
        path.add(pNode);
        Graph graph = createGraph1();
        
       
        MPQTreeUpdater.addVertexToLeaf(4, path, graph);
       
       //need to include stuff herre


    }
    
    
//node is a QNode and A is present in every section
    
    @Test
    public void addVertexToLeaf5() {
        
        HashSet<Integer> qSec1 = new HashSet<>();
        qSec1.add(1); qSec1.add(2);  qSec1.add(3);
        
        HashSet<Integer> qSec2 = new HashSet<>();
        qSec1.add(1); qSec1.add(2);  qSec1.add(4);
        
        HashSet<Integer> qSec3 = new HashSet<>();
        qSec1.add(1); qSec1.add(2);  qSec1.add(5);
        
        
        

        QSectionNode qSec11 = createQSectionNode(qSec1);QSectionNode qSec22 = createQSectionNode(qSec2);QSectionNode qSec33 = createQSectionNode(qSec3);
        ArrayList<MPQTreeNode> path = new ArrayList<MPQTreeNode>();
        
        QNode newQNode = createQNode(qSec11);
        newQNode.leftmostSection=qSec11;
        newQNode.rightmostSection=qSec22;
        qSec11.rightSibling=qSec22; qSec22.leftSibling=qSec11;
        
        path.add(newQNode);
        Graph graph = createGraph1();
        
       
        MPQTreeUpdater.addVertexToLeaf(4, path, graph);
       
        

    }
    
    
    
    

    
    
    
    
    
}


