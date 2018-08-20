package org.jgrapht.alg.interval;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.Leaf;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.MPQTreeNode;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.PNode;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.QNode;
import org.jgrapht.alg.interval.KorteMoehringIntervalGraphRecognizer.QSectionNode;



/**
 * Updates the MPQ Trees according to templates 
 * @author Suchanda Bhattacharyya (dia007)
 * 
 * 
 *
 */
public  class MPQTreeUpdater {
    /**
     * Adds the vertex u to the leaf of the path
     * @param <V>
     * @param <V>
     * @param <E>
     * @param <E>
     * 
     * @param u the vertex to be added
     * @param path the path of the leaf
     * @param graph the interval graph 
     * @return adds the vertex u at the end of the path after making changes according to the template
     */
    public static <V, E> void addVertexToLeaf(V u, List<KorteMoehringIntervalGraphRecognizer<V, E>.MPQTreeNode> path, Graph graph)
    {
        int lastIndexofPath= path.size()-1;
        MPQTreeNode lastNodeInPath = path.get(lastIndexofPath);
        
        
        KorteMoehringIntervalGraphRecognizer<V, E> test = new KorteMoehringIntervalGraphRecognizer<>(graph);


        //check if lastnodeInPath is P or Q or leaf

        
        if(lastNodeInPath.getClass()== Leaf.class) {
            //check if set B is empty
            HashMap<Integer, HashSet<V>> partitionedVertexSet = partitionVertexSet(u,graph, lastNodeInPath);

            if(partitionedVertexSet.get(1).isEmpty()) {

                path.get(lastIndexofPath).elements.add(u);

            }

            else {
                //transform the leaf containing A+B into a PNode containing A
                
                
                PNode newPNode = test.new PNode( partitionedVertexSet.get(0));


                //Create two leaves for PNode children, add the children to the PNode
                
                HashSet leafElements = new HashSet<>();
                leafElements.add(u);

                Leaf leaf1 = test.new Leaf(leafElements);
                Leaf leaf2 = test.new Leaf( partitionedVertexSet.get(1));
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
            HashMap<Integer, HashSet<V>> partitionedVertexSet = partitionVertexSet(u,graph, lastNodeInPath);

            if(partitionedVertexSet.get(1).isEmpty()) {
                //swap P node containing A+B with P Node containing just A,since B is empty makes no change
                //keep it the same, just add the new leaves there
                HashSet leafElements = new HashSet<>();
                leafElements.add(u);
                
                Leaf newLeaf= test.new Leaf(leafElements);
                tempPNode.addCurrentChild(newLeaf);
                tempPNode.currentChild=newLeaf;
                newLeaf.parent=tempPNode;

                path.remove(lastIndexofPath);
                path.add(tempPNode);


            }else {
                //update the previous PNode elements with elements in A

                PNode newPNodeA= test.new PNode(partitionedVertexSet.get(0));
                //create a new PNode and add the B set

                PNode newPNodeB = tempPNode;
                newPNodeB.elements.removeAll(partitionedVertexSet.get(0));
                newPNodeA.addChild(newPNodeB);
                HashSet leafElements = new HashSet<>();
                leafElements.add(u);
                
                Leaf newChildLeaf = test.new Leaf(leafElements);
                newPNodeA.addCurrentChild(newChildLeaf);

                path.remove(lastIndexofPath);
                path.add(newPNodeA);




            }


        } 


        //last node is a Qnode

        else if(lastNodeInPath.getClass()== QNode.class) {

            //find what is the common set 
            
            

            //test if all sections contains A or not

            QNode currentQNode = (KorteMoehringIntervalGraphRecognizer<V, E>.QNode) lastNodeInPath ;
            HashSet commonElement = (HashSet) Graphs.neighborSetOf(graph, u);
            boolean containsCommonElement = allSectionsContainsElementSet(currentQNode,commonElement);
            
            //if containsA == true Q1, else Q2 (both the cases of Q2)


            if(containsCommonElement) {
                //Q1

                //create a new PNode A 
                PNode newElementPNode = test.new PNode(commonElement);
                HashSet leafElements = new HashSet<>();
                leafElements.add(u);

                Leaf newLeaf = test.new Leaf(leafElements);
                newElementPNode.addChild(newLeaf);
                newElementPNode.addChild(newLeaf);


                QNode newChildQNode = extractQNodeElements(currentQNode,commonElement );
                newElementPNode.addChild(newChildQNode); 


                path.remove(currentQNode);
                path.add(newElementPNode);


            }else {

                //TODO: implement the helper functions

                //check if B is null
                
                
                if(currentQNode.leftmostSection.elements==commonElement)
                {

                    PNode newChildPNode = test.new PNode (null);

                    //take the existing childsubtree of the QNode and add it to the PNode
                    newChildPNode.addChild(currentQNode.leftmostSection.child);

                    HashSet leafElements = new HashSet<>();
                    leafElements.add(u);
                    Leaf newChildLeaf = test.new Leaf (leafElements);
                    newChildPNode.addCurrentChild(newChildLeaf);



                    currentQNode.leftmostSection.addChild(newChildPNode);
                    
                //    modifyAccordingToHelperTemplate(newChildPNode);



                }else {

                    //create a new QSection

                    QSectionNode newQSectionNode = test.new QSectionNode(commonElement);
                    newQSectionNode.rightSibling=currentQNode.leftmostSection;
                    currentQNode.leftmostSection.leftSibling=newQSectionNode;
                    currentQNode.leftmostSection=newQSectionNode;
                    HashSet leafElements = new HashSet<>();
                    leafElements.add(u);
                    Leaf newChildLeaf = test.new Leaf (leafElements);
                    currentQNode.leftmostSection.addChild(newChildLeaf);

                }

            }




        }
        
    }
    
    /*private static PNode modifyAccordingToHelperTemplate(PNode pNode) {
       
        if(pNode.elements==null) {
            for(MPQTreeNode currentNode =pNode.currentChild; currentNode. ;)
            
        }
        else {
            return pNode;
        }
        return pNode;
        
    }
*/
    /**
     * Removes the given elementset from each section of the given qNode
     * 
     * @param qNode the given qNode
     * @param elementSet the elements to be removed from all sections of the qNode
     * @return the qNode after removing the elementset from each section's elements
     */

    public static  <V> QNode extractQNodeElements(QNode qNode, HashSet<V> elementSet) {

        for(QSectionNode curerntQSectionNode = qNode.leftmostSection; curerntQSectionNode.rightSibling!=null; curerntQSectionNode= curerntQSectionNode.rightSibling ) {
            curerntQSectionNode.elements.removeAll(elementSet);
        }



        return qNode;
    }


    /**
     * Checks if all sections of the qNode contains the element Set
     * 
     * @param qNode the given qNode
     * @param elementSet the elements to be contained all sections of the qNode
     * @return true if all sections contain the elementSet, else false
     */

    public static  <V> boolean allSectionsContainsElementSet(QNode newQNode, HashSet element) {
        //traverse all sections of the QNode and check if every section contains an A
        //should there be a section identifier

        QSectionNode leftSection = newQNode.leftmostSection;
        QSectionNode rightSection = newQNode.rightmostSection;
        boolean isContained = false;
        
        
        
        
        while   (leftSection != rightSection) {
            
            
            if(leftSection.elements.contains(element) && rightSection.elements.contains(element))
            {
                leftSection=leftSection.rightSibling;
                rightSection=rightSection.leftSibling;
                isContained = true;
            }
            else {
                isContained= false;
                break;
                
            }
            


        }

        return isContained;
    }



    /**
     * Partitions the vertex set in the given node into two sets- one which are the neighbours of u in the graph and the other which are not
     * @param <V>
     * @param u the vertex u in the interval graph
     * @param graph the given interval Graph
     * @param node the MPQTree Node
     * @return HashMap containing the partitioned vertex sets, vertexPartitionSetA contains the vertices in the node which are 
     * Neighbors of u in the graph, vertexPartitionSetA contains the vertices which are not neighbors of u in the graph
     *
     */
    public static <V> HashMap<Integer, HashSet<V>> partitionVertexSet(V u, Graph graph, MPQTreeNode node){

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
     * Checks the path for specific patterns and changes every node accordingly
     * @param <V>
     * @param <E>
     * 
     * @param u the vertex to add to the tree
     * @param path the path of vertices to be changed
     * @param nSmall the smalles positive node in path
     * @param nBig the highest non-empty, non-inf node in path
     */
    public static <V, E> void changedPathToTemplates(V u, List<KorteMoehringIntervalGraphRecognizer<V, E>.MPQTreeNode> path, MPQTreeNode nSmall, MPQTreeNode nBig, Graph graph)
    {
        //traverse from nSmall to nBig
        int minIndex = path.indexOf(nSmall);
        int currentIndex = path.indexOf(nSmall);
        int maxIndex = path.indexOf(nBig);
        KorteMoehringIntervalGraphRecognizer<V, E> test = new KorteMoehringIntervalGraphRecognizer<>(graph);


        for(int i = currentIndex; i<=maxIndex; i++){

            MPQTreeNode currentNode = path.get(currentIndex);
            //split the vertex initially


            //CHECK the type of node
            if(currentNode.getClass()==Leaf.class) {
                HashMap<Integer, HashSet<V>> partitionedVertexSet  = partitionVertexSet(u, graph, currentNode);


                if(currentIndex==minIndex) {

                    //create a Qnode with 2 section nodes 

                    QSectionNode newQSection2 = test.new QSectionNode(partitionedVertexSet.get(0));
                    QSectionNode newQSection1 = test.new QSectionNode(partitionedVertexSet.get(0));   


                    QNode newElementQNode = test.new QNode(newQSection1);
                    //     newElementQNode.addSection(newQSection2);
                    newElementQNode.leftmostSection=newQSection1;
                    newElementQNode.rightmostSection=newQSection2;

                    newQSection1.rightSibling=newQSection2;
                    newQSection1.parent=newElementQNode;

                    newQSection2.leftSibling=newQSection1;

                    HashSet leafElements = new HashSet<>();
                    leafElements.add(u);
                    Leaf leftSectionChildLeaf = test.new Leaf(leafElements);
                    Leaf righSectiontChildLeaf = test.new Leaf(partitionedVertexSet.get(1));

                    newQSection1.addChild(leftSectionChildLeaf);

                    newQSection2.addChild(righSectiontChildLeaf);



                    path.remove(currentIndex);
                    path.add(newElementQNode);




                }


            }

            else if (path.get(currentIndex).getClass()==PNode.class) {
                PNode tempPNode=(KorteMoehringIntervalGraphRecognizer<V, E>.PNode) path.get(currentIndex);
                HashMap<Integer, HashSet<V>> partitionedVertexSet  = partitionVertexSet(u, graph, currentNode);


                if(currentIndex==minIndex) {

                    QSectionNode newQSectionNode1 = test.new QSectionNode(partitionedVertexSet.get(0));                  
                    QSectionNode newQSectionNode2 = test.new QSectionNode(partitionedVertexSet.get(0));

                    QNode newQNode = test.new QNode(newQSectionNode1);
                    //   newQNode.addSection(newQSectionNode2);

                    newQNode.leftmostSection=newQSectionNode1;

                    newQNode.rightmostSection=newQSectionNode2;

                    newQSectionNode1.rightSibling=newQSectionNode2;
                    newQSectionNode2.leftSibling=newQSectionNode1;
                    HashSet leafElements = new HashSet<>();
                    leafElements.add(u);
                    Leaf leftChildLeaf = test.new Leaf(leafElements);

                    PNode rightChildPNode = tempPNode;
                    rightChildPNode.elements.remove(partitionedVertexSet.get(0));

                    newQSectionNode1.addChild(leftChildLeaf);

                    newQSectionNode2.addChild(rightChildPNode);


                    path.remove(currentIndex);
                    path.add(newQNode);






                }
                else {
                    //TODO: implement helper classes here
                    QSectionNode rightMostQSection = test.new QSectionNode(tempPNode.elements);
                    PNode rightmostQSectionChildPNode = tempPNode;
                    rightmostQSectionChildPNode.elements.removeAll(tempPNode.elements);
                    rightMostQSection.addChild(rightmostQSectionChildPNode);

                    QNode tempQNode = (KorteMoehringIntervalGraphRecognizer<V, E>.QNode) tempPNode.currentChild;
                    QNode newElementQNode = tempQNode;



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
                HashSet commonElements = (HashSet) Graphs.neighborSetOf(graph, u);

                //first check:current node = NSmall, second check A is present in everything


                if(currentIndex==minIndex) {

                    //second check A is present in everything

                    if(allSectionsContainsElementSet(tempQNode, commonElements)) {
                        //create Qnode1

                        QSectionNode rightMostSection = test.new QSectionNode(commonElements);
                        QSectionNode leftMostSection = test.new QSectionNode(commonElements);

                        QNode newQNodeA = test.new QNode(leftMostSection);
                        //need to add rightmost section in newQNodeA
                        newQNodeA.leftmostSection=leftMostSection;
                        newQNodeA.rightmostSection=rightMostSection;

                        leftMostSection.rightSibling=rightMostSection;
                        rightMostSection.leftSibling=leftMostSection;
                        HashSet leafElements = new HashSet<>();
                        leafElements.add(u);
                        Leaf newChild = test.new Leaf(leafElements);

                        leftMostSection.addChild(newChild);


                        //create QNode2



                        QNode newQNodeB = tempQNode;


                        for(QSectionNode currentSection = newQNodeB.leftmostSection; currentSection.rightSibling != null; currentSection = currentSection.rightSibling ) {


                            currentSection.elements.removeAll(commonElements);

                        }

                        rightMostSection.addChild(newQNodeB);


                        path.remove(tempQNode);
                        path.add(newQNodeA);


                    }else {


                        QSectionNode qSectionNode= test.new QSectionNode(commonElements);
                        tempQNode.leftmostSection.leftSibling=qSectionNode;
                        qSectionNode.rightSibling=tempQNode.leftmostSection;
                        tempQNode.leftmostSection=qSectionNode;
                        HashSet leafElements = new HashSet<>();
                        leafElements.add(u);
                        Leaf newChild = test.new Leaf(leafElements); 
                        tempQNode.addChild(newChild);


                    }


                }
                else {

                    QNode tempChildQNode=(KorteMoehringIntervalGraphRecognizer<V, E>.QNode) tempQNode.leftmostSection.child;

                    for(QSectionNode currentSection = tempChildQNode.leftmostSection; currentSection.rightSibling != null; currentSection = currentSection.rightSibling ) {
                        if(currentSection==tempChildQNode.leftmostSection) {

                            currentSection.elements.addAll(commonElements);
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

}
