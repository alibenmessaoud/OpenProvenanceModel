

package org.openprovenance.reproduce;
import javax.xml.bind.JAXBException;

import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.vocabulary.RDFS;

import org.openprovenance.model.OPMFactory;
import org.openprovenance.model.OPMDeserialiser;
import org.openprovenance.model.OPMGraph;
import org.openprovenance.model.IndexedOPMGraph;
import org.openprovenance.model.Artifact;
import org.openprovenance.model.Account;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Reproducibility of PC1
 */
public class PC1ReproduceTest extends TestCase {
    public static String PC1_NS="http://www.ipaw.info/pc1/";

    public OPMFactory oFactory=new OPMFactory();

    public PrimitiveEnvironment primEnv=new OpenProvenanceEnvironment();

    public ArtifactFactory aFactory= new ArtifactGenerator (oFactory);


    Utilities u=new Utilities(oFactory);

    public PC1ReproduceTest (String testName) {
        super(testName);
    }

    public void testWithModel1() {
        loadModel();
    }
    
    static Model theModel;
    static IndexedOPMGraph graph;

    public void loadModel() {
        
        // ontologies that will be used
        String ont1 = "file:src/test/resources/opm.owl";
        String ont2 = "file:src/test/resources/primitives3.owl";

        String ns1 = "http://openprovenance.org/ontology#";
        String ns2 = "http://openprovenance.org/primitives#";
        
  	    // create Pellet reasoner
        Reasoner reasoner = PelletReasonerFactory.theInstance().create();
        
        // create an empty model
        Model emptyModel = ModelFactory.createDefaultModel( );
        
        // create an inferencing model using Pellet reasoner
        InfModel model = ModelFactory.createInfModel( reasoner, emptyModel );
            
        // read the files
        model.read( "file:src/test/resources/pc1-full.n3", "N3" );
        model.read( ont1 );
        model.read( ont2 );
        
        // print validation report
        ValidityReport report = model.validate();
        printIterator( report.getReports(), "Validation Results" );
        
        // print superclasses
        Resource c = model.getResource( ns1 + "Artifact" );         
        printIterator(model.listObjectsOfProperty(c, RDFS.subClassOf), "All super classes of " + c.getLocalName());

        c = model.getResource( ns2 + "Integer" );         
        printIterator(model.listObjectsOfProperty(c, RDFS.subClassOf), "All super classes of " + c.getLocalName());
        
        theModel=model;
        try {
            loadOPMGraph();
        } catch (JAXBException je) {
        }
    }

    public void loadOPMGraph() throws JAXBException    {
        OPMDeserialiser deserial=OPMDeserialiser.getThreadOPMDeserialiser();
        OPMGraph graph1=deserial.deserialiseOPMGraph(new File("src/test/resources/pc1-full.xml"));
        graph=new IndexedOPMGraph(oFactory,graph1);
    }




    
    public static void printIterator(Iterator<?> i, String header) {
        System.out.println(header);
        for(int c = 0; c < header.length(); c++)
            System.out.print("=");
        System.out.println();
        
        if(i.hasNext()) {
	        while (i.hasNext()) 
	            System.out.println( i.next() );
        }       
        else
            System.out.println("<EMPTY>");
        
        System.out.println();
    }

    public void testQuery2() throws java.io.FileNotFoundException, java.io.IOException {
        
        // Create a new query
        String queryString = 
            "PREFIX opm: <http://openprovenance.org/ontology#> " +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "SELECT ?proc " +
            "WHERE {" +
            "      ?proc rdf:type opm:Process " +
            "      }";

        runQuery(queryString);
    }


    public void testQuery3() throws java.io.FileNotFoundException, java.io.IOException {
        
        // Create a new query
        String queryString = 
            "PREFIX opm: <http://openprovenance.org/ontology#> " +
            "PREFIX pc1: <http://www.ipaw.info/pc1/>  " +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "SELECT ?a1 ?a2 " +
            "WHERE {" +
            "      ?a1 opm:_wasDerivedFrom_star ?a2 " +
            "      }";

        runQuery(queryString);
    }

    public void runQuery (String queryString) {
        Query query = QueryFactory.create(queryString);

        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, theModel);
        ResultSet results = qe.execSelect();

        // Output query results	
        ResultSetFormatter.out(System.out, results, query);

        // Important - free up resources used running the query
        qe.close();
    }


    public void testQuery4() {

        Queries q=new Queries(theModel);
        q.addPrefixes("pc1",PC1_NS);

        List ll=q.getUsedArtifactsAsResources("pc1:p1");
        System.out.println(" found " + ll);
        q.close();
        assertTrue(ll.size()==4);


        ll=q.getGeneratedArtifactsAsResources("pc1:p2");
        System.out.println(" found " + ll);
        q.close();
        assertTrue(ll.size()==1);
        
    }


    public void testOrderedProcesses() {
        Queries q=new Queries(theModel);
        String pc1NS="http://www.ipaw.info/pc1/";
        q.addPrefixes("pc1",pc1NS);

        List<Resource> results = q.getProcessesAsResources(pc1NS);

        System.out.println("Found Processes " + results);

        q.close();
    }


    public void testInputArtifacts() {
        Queries q=new Queries(theModel);
        String pc1NS="http://www.ipaw.info/pc1/";
        q.addPrefixes("pc1",pc1NS);

        List<Resource> results = q.getInputArtifactsAsResources(pc1NS);

        System.out.println("==> Found Input Artifacts " + results);

        q.close();
    }


    public void testQuery5() {
        Queries q=new Queries(theModel);
        String pc1NS="http://www.ipaw.info/pc1/";
        q.addPrefixes("pc1",pc1NS);

        List<Resource> results = q.getProcessesWithPropAsResources("http://openprovenance.org/primitives#primitive",
                                                                   "http://openprovenance.org/primitives#align_warp");

        System.out.println("==> Found Processes for Primitive " + results);

        q.close();
    }


    public void testQuery6() {
        Queries q=new Queries(theModel);
        String pc1NS="http://www.ipaw.info/pc1/";
        q.addPrefixes("pc1",pc1NS);

        List<Literal> results = q.getResourcePropertyAsLiterals("http://www.ipaw.info/pc1/p1",
                                                                  "http://openprovenance.org/primitives#primitive");

        System.out.println("==> Found Value for property " + results);

        q.close();
    }


    public void testQuery7() {
        Queries q=new Queries(theModel);
        String pc1NS="http://www.ipaw.info/pc1/";
        q.addPrefixes("pc1",pc1NS);

        ResultSet results = q.getUsedArtifactsAndRoles("pc1:p1");
        ResultSetFormatter.out(System.out, results);

        q.close();
    }


    static String PATH_PROPERTY="http://openprovenance.org/primitives#path";
    static String FILE_LOCATION="//home/lavm/papers/papers/opmowl/OpenProvenanceModel/reproduce/src/test/resources/pc1/";
    //static String FILE_LOCATION="../src/test/resources/pc1/";


    static List<Account> black=new LinkedList();

    public void testReproduceP1() throws java.io.IOException, org.jaxen.JaxenException, org.xml.sax.SAXException {

        /*

~/Download/AIR5.2.6/bin/reslice params1.warp resliced1
~/Download/AIR5.2.6/bin/softmean atlas.hdr y null resliced1.img 
~/Download/fsl/bin/slicer ./atlas.hdr -x .5 ./sliced.pgm

        */


        invokeProcess("http://www.ipaw.info/pc1/p1");

        invokeProcess("http://www.ipaw.info/pc1/p5");

        invokeProcess("http://www.ipaw.info/pc1/p9");

        invokeProcess("http://www.ipaw.info/pc1/p10");

        invokeProcess("http://www.ipaw.info/pc1/p13");
        
    }

    public String localName(String uri, String ns) {
        return uri.substring(ns.length());
    }
            

    public void invokeProcess(String process) throws java.io.IOException, org.jaxen.JaxenException, org.xml.sax.SAXException {
        
        String name=localName(process,PC1_NS) + "-swift";
        HashMap<String,Artifact> args=new HashMap();
        Queries q=new Queries(theModel); 
        List<Literal> results = q.getResourcePropertyAsLiterals(process,
                                                                "http://openprovenance.org/primitives#primitive");
        String primitiveName=results.get(0).getString();
        q.close();
        
        q=new Queries(theModel); 
        q.addPrefixes("pc1",PC1_NS);
        ResultSet results2 = q.getUsedArtifactsAndRoles("<" + process + ">");
        while (results2.hasNext()) {
            QuerySolution qs=results2.next();
            String artifactUri=qs.getResource("?a").getURI();
            String role=qs.getLiteral("?r").getString();
            Artifact a=graph.getArtifact(localName(artifactUri,PC1_NS));
            //System.out.println("Found role " + role);
            //System.out.println("Found artifact " + a);

            // should invoke factory here
            args.put(role,a);
        }
        q.close();

        q=new Queries(theModel); 
        q.addPrefixes("pc1",PC1_NS);
        ResultSet results3 = q.getGeneratedArtifactsAndRoles("<" + process + ">");
        while (results3.hasNext()) {
            QuerySolution qs=results3.next();
            String artifactUri=qs.getResource("?a").getURI();
            String role=qs.getLiteral("?r").getString();
            Artifact a=graph.getArtifact(localName(artifactUri,PC1_NS));
            //System.out.println("Found role " + role);
            //System.out.println("Found artifact " + a);

            // should invoke factory here
            args.put(role,a);
        }
        q.close();
        
        String primitive=primEnv.get(primitiveName);
        invokePrimitive(primitive,
                        args,
                        name);
    }
        



    public void invokePrimitive(String primitive,
                                HashMap<String,Artifact> args,
                                String name)
        throws java.io.IOException, org.jaxen.JaxenException, org.xml.sax.SAXException {
        
        Execute exec=new Execute(oFactory,aFactory);
        Document doc=exec.createInvocationDocument(primitive,args);
        u.serializeToStandardOut(doc.getDocumentElement(), doc);
        u.serialize(doc.getDocumentElement(),
                    doc,
                    new FileOutputStream("target/" + name + ".xml"));
        exec.invokeSwift(name + ".xml", name + ".kml");
    }

        
    

}