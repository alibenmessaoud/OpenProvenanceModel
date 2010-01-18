package org.openprovenance.elmo;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Hashtable;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.openrdf.elmo.ElmoManager;

import org.openprovenance.model.Account;
import org.openprovenance.model.AccountRef;
import org.openprovenance.model.Agent;
import org.openprovenance.model.Annotable;
import org.openprovenance.model.Annotation;
import org.openprovenance.model.Artifact;
import org.openprovenance.model.EmbeddedAnnotation;
import org.openprovenance.model.HasAccounts;
import org.openprovenance.model.Identifiable;
import org.openprovenance.model.Label;
import org.openprovenance.model.OPMGraph;
import org.openprovenance.model.Process;
import org.openprovenance.model.Property;
import org.openprovenance.model.Type;
import org.openprovenance.model.Used;
import org.openprovenance.model.Value;
import org.openprovenance.model.WasControlledBy;
import org.openprovenance.model.WasDerivedFrom;
import org.openprovenance.model.WasGeneratedBy;
import org.openprovenance.model.WasTriggeredBy;

import org.openprovenance.rdf.AnnotationOrEdgeOrNode;
import org.openrdf.elmo.Entity;

/** The factory class to create OPM entities that can be serialised to rdf.
    This package relies on Sesame's elmo, which uses a facade object to access the content of the triple
    store and update it.  Each OPM class generated by JAXB is subclassed to get access to the facade
    for each instance.

    The factory class offers several kinds of factory methods.

    - factory methods that are inherited from OPMFactory, created instance of objects with a facade to the triple store.

    - factory methods that clone opm entities and create new instances with a facade to a triple store

    - factroy methods that create facade-with instances of OPM classes from the content of a triple store. 

    Note that to ensure that graph structure is preserved between the Java representation and the RDF triple store,
    an instance of this factory class is stateful!  (The original OPMFactory was purely stateless. 

    Note that JAXB beans that have a field that contains a list only
    offer a getter method for these fields. They can be updated by
    getting the list and updating it.  If bean content are update this
    way, the RDF facade may not be aware of the update, and may not
    propagate it to the triple store. Therefore the factory classes
    (OPMFactory) offers a number of update methods (typically to add
    new elements to lists).  These methods are overridden by
    RdfOPMFactory to update the triple store.

*/


public class RdfOPMFactory extends org.openprovenance.model.OPMFactory {

    final ElmoManager manager;

    public RdfOPMFactory(org.openprovenance.model.ObjectFactory o) {
        super(o);
        this.manager=null;
    }
    public RdfOPMFactory(org.openprovenance.model.ObjectFactory o, ElmoManager manager) {
        super(o);
        this.manager=manager;
    }

    static int count=0;

    public String autoGenerateId(String prefix) {
        String id=prefix+count++;
        return id;
    }

    public String autoGenerateId(String prefix, String id) {
        if (id!=null) return id;
        id=prefix+count++;
        return id;
    }

    
    public void addProperty(EmbeddedAnnotation ann, Property p) {
        super.addProperty(ann,p);
        Object o=((HasFacade) ann).findMyFacade();
        org.openprovenance.rdf.Annotation ann2=(org.openprovenance.rdf.Annotation) o;
        org.openprovenance.rdf.Property p2=(org.openprovenance.rdf.Property) ((HasFacade)p).findMyFacade();
        ann2.getProperties().add(p2);
    }

    public void addProperty(Annotation ann, Property p) {
        super.addProperty(ann,p);
        Object o=((HasFacade) ann).findMyFacade();
        org.openprovenance.rdf.Annotation ann2=(org.openprovenance.rdf.Annotation) o;
        org.openprovenance.rdf.Property p2=(org.openprovenance.rdf.Property) ((HasFacade)p).findMyFacade();
        ann2.getProperties().add(p2);
    }

    public void addAccounts(HasAccounts element, Collection<AccountRef> accounts) {
        super.addAccounts(element,accounts);
        if (element instanceof HasAccounts) { //Annotable
            HasFacade facade=(HasFacade) element;
            Object o=facade.findMyFacade();
            AnnotationOrEdgeOrNode el=(AnnotationOrEdgeOrNode) o;

            Set set=new HashSet();
            for (AccountRef accr: accounts) {
                set.add(((HasFacade)accr.getRef()).findMyFacade());
            }
            el.getAccounts().addAll(set);
            //el.setAccounts(set);
        }
    }

    public void addAnnotation(Annotable annotable,
                               JAXBElement<? extends org.openprovenance.model.EmbeddedAnnotation> ann) {

        EmbeddedAnnotation ea=ann.getValue();
        addAnnotation(annotable,ea);
    }

    public void addAnnotation(Annotable annotable, Label ann) {
        if (ann!=null) {
            super.addAnnotation(annotable,ann);
            try {
                ((RdfLabel)ann).toRdf(annotable);
            } catch (org.openrdf.repository.RepositoryException e) {
                e.printStackTrace();
            }
        }
    }

    public void addAnnotation(Annotable annotable, Type ann) {
        if (ann!=null) {
            super.addAnnotation(annotable,ann);
            try {
                ((RdfType)ann).toRdf(annotable);
            } catch (org.openrdf.repository.RepositoryException e) {
                e.printStackTrace();
            }
        }
    }

    public void addAnnotation(Annotable annotable, Value ann) {
        if (ann!=null) {
            super.addAnnotation(annotable,ann);
            try {
                ((RdfValue)ann).toRdf(annotable);
            } catch (org.openrdf.repository.RepositoryException e) {
                e.printStackTrace();
            }
        }
    }


    public void addAnnotation(org.openprovenance.model.Annotable annotable,
                              org.openprovenance.model.Annotation ann) {
        System.out.println("*********** adding a annotations ");
        annotable.getAnnotation().add(of.createAnnotation(ann));
    }

    /** Add annotation to an annotable JAXB Bean and its corresponding RDF facade. */

    public void addAnnotation(org.openprovenance.model.Annotable annotable,
                              org.openprovenance.model.EmbeddedAnnotation ann) {

        if (ann instanceof Label) {
            addAnnotation(annotable,(Label) ann);
        } else if (ann instanceof Type) {
            addAnnotation(annotable,(Type) ann);
        } else {
            super.addAnnotation(annotable,ann);


            HasFacade facade=(HasFacade) annotable;
            Object o=facade.findMyFacade();
            org.openprovenance.rdf.Annotable annotable2=(org.openprovenance.rdf.Annotable) o;
            org.openprovenance.rdf.Annotation ann2=(org.openprovenance.rdf.Annotation) ((HasFacade)ann).findMyFacade();
            annotable2.getAnnotations().add(ann2);
        }
    }

    public OPMGraph newOPMGraph(String id,
                                Collection<org.openprovenance.model.Account> accs,
                                Collection<org.openprovenance.model.Overlaps> ops,
                                Collection<org.openprovenance.model.Process> ps,
                                Collection<org.openprovenance.model.Artifact> as,
                                Collection<org.openprovenance.model.Agent> ags,
                                Collection<Object> lks,
                                Collection<org.openprovenance.model.Annotation> anns)
    {
        OPMGraph graph=super.newOPMGraph(id,accs,ops,ps,as,ags,lks,anns);
        HasFacade facade=(HasFacade) graph;
        org.openprovenance.rdf.OPMGraph rdfGraph=(org.openprovenance.rdf.OPMGraph) facade.findMyFacade();

        Set<org.openprovenance.rdf.Account> accounts=new HashSet();
        for (org.openprovenance.model.Account p: accs) {
            org.openprovenance.rdf.Account pp=(org.openprovenance.rdf.Account) ((HasFacade)p).findMyFacade();
            accounts.add(pp);
        }
        rdfGraph.setHasAccount(accounts);


        Set<org.openprovenance.rdf.Process> processes=new HashSet();
        for (org.openprovenance.model.Process p: ps) {
            org.openprovenance.rdf.Process pp=(org.openprovenance.rdf.Process) ((HasFacade)p).findMyFacade();
            processes.add(pp);
        }
        rdfGraph.setHasProcess(processes);


        Set<org.openprovenance.rdf.Artifact> artifacts=new HashSet();
        for (org.openprovenance.model.Artifact p: as) {
            org.openprovenance.rdf.Artifact pp=(org.openprovenance.rdf.Artifact) ((HasFacade)p).findMyFacade();
            artifacts.add(pp);
        }
        rdfGraph.setHasArtifact(artifacts);


        Set<org.openprovenance.rdf.Agent> agents=new HashSet();
        for (org.openprovenance.model.Agent p: ags) {
            org.openprovenance.rdf.Agent pp=(org.openprovenance.rdf.Agent) ((HasFacade)p).findMyFacade();
            agents.add(pp);
        }
        rdfGraph.setHasAgent(agents);

        Set<org.openprovenance.rdf.Edge> dependency=new HashSet();
        for (Object o: lks) {
            org.openprovenance.rdf.Edge pp=(org.openprovenance.rdf.Edge) ((HasFacade)o).findMyFacade();
            dependency.add(pp);
        }
        rdfGraph.setHasDependency(dependency);



        return graph;
        
    }


    public RdfArtifact newArtifact(org.openprovenance.rdf.Artifact a) {
        QName qname=((Entity)a).getQName();
        RdfArtifact res=register(qname,new RdfArtifact(manager,qname));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,res.getAccount());
        processAnnotations(a,res);
        return res;
    }

    public RdfArtifact newArtifact(QName qname) {
        return register(qname,new RdfArtifact(manager,qname));
    }


    public RdfProcess newProcess(org.openprovenance.rdf.Process a) {
        QName qname=((Entity)a).getQName();
        RdfProcess res=register(qname, new RdfProcess(manager,qname));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,res.getAccount());
        processAnnotations(a,res);
        return res;
    }

    public RdfProcess newProcess(QName qname) {
        return register(qname,new RdfProcess(manager,qname));
    }

    public RdfAgent newAgent(org.openprovenance.rdf.Agent a) {
        QName qname=((Entity)a).getQName();
        RdfAgent res=register(qname,new RdfAgent(manager,qname));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,res.getAccount());
        processAnnotations(a,res);
        return res;
    }

    public RdfAgent newAgent(QName qname) {
        RdfAgent res=register(qname,new RdfAgent(manager,qname));
        return res;
    }

    public RdfAccount newAccount(QName qname) {
        return register(qname,new RdfAccount(manager,qname));
    }

    public RdfAccount newAccount(org.openprovenance.rdf.Account a) {
        QName qname=((Entity)a).getQName();
        return register(qname, new RdfAccount(manager,qname));
    }

    public RdfRole newRole(QName qname) {
        return new RdfRole(manager,qname);
    }

    public RdfRole newRole(org.openprovenance.rdf.Role r) {
        QName qname=((Entity)r).getQName();
        String value=(String)r.getValue();
        return new RdfRole(manager,qname,value);
    }

    public RdfProperty newProperty(org.openprovenance.rdf.Property a) {
        QName qname=((Entity)a).getQName();
        RdfProperty res=new RdfProperty(manager,qname);
        res.setFields(a.getUri(), a.getValue());
        return res;
    }

    public RdfAnnotation newAnnotation(org.openprovenance.rdf.Annotation a) {
        QName qname=((Entity)a).getQName();

        RdfAnnotation res=new RdfAnnotation(manager,qname);
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,res.getAccount());
        for (org.openprovenance.rdf.Property prop: a.getProperties()) {
            res.getProperty().add(newProperty(prop));
        }
        return res;
    }

    public RdfEmbeddedAnnotation newEmbeddedAnnotation(org.openprovenance.rdf.Annotation a) {
        QName qname=((Entity)a).getQName();
        RdfEmbeddedAnnotation res=new RdfEmbeddedAnnotation(manager,qname);
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,res.getAccount());
        for (org.openprovenance.rdf.Property prop: a.getProperties()) {
            res.getProperty().add(newProperty(prop));
        }
        return res;
    }




    private Hashtable<QName, RdfAgent>    agentRegister    =new Hashtable();
    private Hashtable<QName, RdfProcess>  processRegister  =new Hashtable();
    private Hashtable<QName, RdfArtifact> artifactRegister =new Hashtable();
    private Hashtable<QName, RdfAccount>  accountRegister  =new Hashtable();

    public RdfAgent register(QName qname, RdfAgent agent) {
        agentRegister.put(qname,agent);
        return agent;
    }
    public RdfProcess register(QName qname, RdfProcess process) {
        processRegister.put(qname,process);
        return process;
    }
    public RdfArtifact register(QName qname, RdfArtifact artifact) {
        artifactRegister.put(qname,artifact);
        return artifact;
    }
    public RdfAccount register(QName qname, RdfAccount account) {
        accountRegister.put(qname,account);
        return account;
    }


    private Hashtable<String, Agent>    agentRegister2    =new Hashtable();
    private Hashtable<String, Process>  processRegister2  =new Hashtable();
    private Hashtable<String, Artifact> artifactRegister2 =new Hashtable();
    private Hashtable<String, Account>  accountRegister2  =new Hashtable();

    public Agent register(String id, Agent agent) {
        agentRegister2.put(id,agent);
        return agent;
    }
    public Process register(String id, Process process) {
        processRegister2.put(id,process);
        return process;
    }
    public Artifact register(String id, Artifact artifact) {
        artifactRegister2.put(id,artifact);
        return artifact;
    }
    public Account register(String id, Account account) {
        accountRegister2.put(id,account);
        return account;
    }
    

    public RdfOTime newOTime(org.openprovenance.rdf.OTime a) {
        QName qname=((Entity)a).getQName();
        RdfOTime res=new RdfOTime(manager,qname);
        res.setFields(a.getNoEarlierThan(), a.getNoEarlierThan(), a.getExactlyAt());
        return res;
    }

    public void addAccounts(org.openprovenance.rdf.AnnotationOrEdgeOrNode hasAccounts,
                            List<AccountRef> accounts) {
        for (org.openprovenance.rdf.Account acc: hasAccounts.getAccounts()) {
            accounts.add(newAccountRef(newAccount(acc)));
        }
    }

    public List<AccountRef> newAccounts(List<AccountRef> accounts) {
        List<AccountRef> res=new LinkedList();
        for (AccountRef acc: accounts) {
            res.add(newAccountRef(accountRegister2.get(((Account)acc.getRef()).getId())));
        }
        return res;
    }

    public RdfWasDerivedFrom newWasDerivedFrom(org.openprovenance.rdf.WasDerivedFrom a) {
        QName qname=((Entity)a).getQName();
        RdfWasDerivedFrom wdf=new RdfWasDerivedFrom(manager,qname);
        org.openprovenance.rdf.Node cause=a.getCause();
        org.openprovenance.rdf.Node effect=a.getEffect();
        wdf.setNodes(newArtifactRef(artifactRegister.get(((Entity)cause).getQName())),
                     newArtifactRef(artifactRegister.get(((Entity)effect).getQName())));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,wdf.getAccount());
        processAnnotations(a,wdf);
        return wdf;
    }

    public RdfWasGeneratedBy newWasGeneratedBy(org.openprovenance.rdf.WasGeneratedBy a) {
        QName qname=((Entity)a).getQName();
        RdfWasGeneratedBy wgb=new RdfWasGeneratedBy(manager,qname);
        org.openprovenance.rdf.Node cause=a.getCause();
        org.openprovenance.rdf.Node effect=a.getEffect();
        org.openprovenance.rdf.Role role=a.getRole();
        wgb.setFields(newProcessRef(processRegister.get(((Entity)cause).getQName())),
                      newArtifactRef(artifactRegister.get(((Entity)effect).getQName())),
                      newRole(role));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,wgb.getAccount());
        processAnnotations(a,wgb);
        wgb.setTime(newOTime(a.getTime()));
        return wgb;
    }

    public RdfUsed newUsed(org.openprovenance.rdf.Used a) {
        QName qname=((Entity)a).getQName();
        RdfUsed u=new RdfUsed(manager,qname);
        org.openprovenance.rdf.Node cause=a.getCause();
        org.openprovenance.rdf.Node effect=a.getEffect();
        org.openprovenance.rdf.Role role=a.getRole();
        u.setFields(newArtifactRef(artifactRegister.get(((Entity)cause).getQName())),
                    newProcessRef(processRegister.get(((Entity)effect).getQName())),
                    newRole(role));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,u.getAccount());
        processAnnotations(a,u);
        u.setTime(newOTime(a.getTime()));
        return u;
    }

    public RdfWasControlledBy newWasControlledBy(org.openprovenance.rdf.WasControlledBy a) {
        QName qname=((Entity)a).getQName();
        RdfWasControlledBy wcb=new RdfWasControlledBy(manager,qname);
        org.openprovenance.rdf.Node cause=a.getCause();
        org.openprovenance.rdf.Node effect=a.getEffect();
        org.openprovenance.rdf.Role role=a.getRole();
        wcb.setFields(newAgentRef(agentRegister.get(((Entity)cause).getQName())),
                      newProcessRef(processRegister.get(((Entity)effect).getQName())),
                      newRole(role));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,wcb.getAccount());
        processAnnotations(a,wcb);
        wcb.setStartTime(newOTime(a.getStartTime()));
        wcb.setEndTime(newOTime(a.getEndTime()));
        return wcb;
    }


    public RdfWasTriggeredBy newWasTriggeredBy(org.openprovenance.rdf.WasTriggeredBy a) {
        QName qname=((Entity)a).getQName();
        RdfWasTriggeredBy wtb=new RdfWasTriggeredBy(manager,qname);
        org.openprovenance.rdf.Node cause=a.getCause();
        org.openprovenance.rdf.Node effect=a.getEffect();
        wtb.setNodes(newProcessRef(processRegister.get(((Entity)cause).getQName())),
                     newProcessRef(processRegister.get(((Entity)effect).getQName())));
        addAccounts((org.openprovenance.rdf.AnnotationOrEdgeOrNode)a,wtb.getAccount());
        processAnnotations(a,wtb);
        return wtb;
    }

    public void processAnnotations(org.openprovenance.rdf.Annotable a, Annotable res) {
        for (org.openprovenance.rdf.Annotation ann: a.getAnnotations()) {
            addAnnotation(res,newEmbeddedAnnotation(ann));
        }
        for (String label: a.getLabels()) {
            super.addAnnotation(res,newLabel(label));
        }
        for (URI pname: a.getPnames()) {
            super.addAnnotation(res,newPName(pname.toString()));
        }
        for (String profile: a.getProfiles()) {
            super.addAnnotation(res,newProfile(profile));
        }
        for (URI type: a.getTypes()) {
            super.addAnnotation(res,newType(type.toString()));
        }
    }


    public void addNewAnnotations(Annotable res,
                                  List<JAXBElement<? extends org.openprovenance.model.EmbeddedAnnotation>> anns) {
        for (JAXBElement<? extends org.openprovenance.model.EmbeddedAnnotation> ann: anns) {
            EmbeddedAnnotation ea=ann.getValue();

            String label=getLabel(ea);
            if (label!=null) addAnnotation(res,newLabel(label));

            String type=getType(ea);
            if (type!=null) addAnnotation(res,newType(type));

            String profile=getProfile(ea);
            if (profile!=null) addAnnotation(res,newProfile(profile));

            String pname=getPname(ea);
            if (pname!=null) addAnnotation(res,newPName(pname));

            if (ea.getId()!=null) 
            addAnnotation(res,newEmbeddedAnnotation(ea.getId(),
                                                    ea.getProperty(),
                                                    newAccounts(ea.getAccount()),
                                                    null));
        }
    }


    public OPMGraph newOPMGraph(org.openprovenance.rdf.OPMGraph gr) {

        QName qname=((Entity)gr).getQName();
        
        List<Account> accs=new LinkedList();
        for (org.openprovenance.rdf.Account acc: gr.getHasAccount()) {
            accs.add(newAccount(acc));
        }


        List<Artifact> as=new LinkedList();
        for (org.openprovenance.rdf.Artifact a: gr.getHasArtifact()) {
            as.add(newArtifact(a));
        }

        List<Process> ps=new LinkedList();
        for (org.openprovenance.rdf.Process p: gr.getHasProcess()) {
            ps.add(newProcess(p));
        }

        List<Agent> ags=new LinkedList();
        for (org.openprovenance.rdf.Agent ag: gr.getHasAgent()) {
            ags.add(newAgent(ag));
        }

        List<Object> lks=new LinkedList();
        for (org.openprovenance.rdf.Edge edge: gr.getHasDependency()) {
             if (edge instanceof org.openprovenance.rdf.Used) {
                lks.add(newUsed((org.openprovenance.rdf.Used) edge));
            } else if (edge instanceof org.openprovenance.rdf.WasGeneratedBy) {
                lks.add(newWasGeneratedBy((org.openprovenance.rdf.WasGeneratedBy) edge));
            } else if (edge instanceof org.openprovenance.rdf.WasDerivedFrom) {
                lks.add(newWasDerivedFrom((org.openprovenance.rdf.WasDerivedFrom) edge));
            } else if (edge instanceof org.openprovenance.rdf.WasControlledBy) {
                lks.add(newWasControlledBy((org.openprovenance.rdf.WasControlledBy) edge));
            } else if (edge instanceof org.openprovenance.rdf.WasTriggeredBy) {
                lks.add(newWasTriggeredBy((org.openprovenance.rdf.WasTriggeredBy) edge));
            }
        }


        List<Annotation> anns=null;

        OPMGraph res=super.newOPMGraph(qname.getLocalPart(),
                                       accs,
                                       new LinkedList(),
                                       ps,
                                       as,
                                       ags,
                                       lks,
                                       anns);

        for (org.openprovenance.rdf.Annotation ann: gr.getAnnotations()) {
            //RdfAnnotation ann2=newAnnotation(ann);
            RdfEmbeddedAnnotation ann2=newEmbeddedAnnotation(ann);
            super.addAnnotation(res,ann2);
        }

        return res;
    }

    public WasDerivedFrom newWasDerivedFrom(WasDerivedFrom d) {
        WasDerivedFrom wdf=newWasDerivedFrom(d.getId(),
                                             newArtifactRef(artifactRegister2.get(((Artifact)d.getEffect().getRef()).getId())),
                                             newArtifactRef(artifactRegister2.get(((Artifact)d.getCause().getRef()).getId())),
                                             newAccounts(d.getAccount()));
        wdf.getAnnotation().addAll(d.getAnnotation());
        return wdf;
    }

    public WasTriggeredBy newWasTriggeredBy(WasTriggeredBy d) {
        WasTriggeredBy wtb=newWasTriggeredBy(d.getId(),
                                             newProcessRef(processRegister2.get(((Process)d.getEffect().getRef()).getId())),
                                             newProcessRef(processRegister2.get(((Process)d.getCause().getRef()).getId())),
                                             newAccounts(d.getAccount()));
        wtb.getAnnotation().addAll(d.getAnnotation());
        return wtb;
    }

    public Used newUsed(Used d) {
        Used u=newUsed(d.getId(),
                       newProcessRef(processRegister2.get(((Process)d.getEffect().getRef()).getId())),
                       newRole(d.getRole()),
                       newArtifactRef(artifactRegister2.get(((Artifact)d.getCause().getRef()).getId())),
                       newAccounts(d.getAccount()));
        u.getAnnotation().addAll(d.getAnnotation());
        u.setTime(newOTime(d.getTime()));
        return u;
    }
    public WasGeneratedBy newWasGeneratedBy(WasGeneratedBy d) {
        WasGeneratedBy wgb=newWasGeneratedBy(d.getId(),
                                             newArtifactRef(artifactRegister2.get(((Artifact)d.getEffect().getRef()).getId())),

                                             newRole(d.getRole()),
                                             newProcessRef(processRegister2.get(((Process)d.getCause().getRef()).getId())),
                                             newAccounts(d.getAccount()));
        wgb.getAnnotation().addAll(d.getAnnotation());
        wgb.setTime(newOTime(d.getTime()));
        return wgb;
    }

    public WasControlledBy newWasControlledBy(WasControlledBy d) {
        WasControlledBy wcb=newWasControlledBy(d.getId(),
                                               newProcessRef(processRegister2.get(((Process)d.getEffect().getRef()).getId())),
                                               newRole(d.getRole()),
                                               newAgentRef(agentRegister2.get(((Agent)d.getCause().getRef()).getId())),
                                               newAccounts(d.getAccount()));
        wcb.getAnnotation().addAll(d.getAnnotation());
        wcb.setStartTime(newOTime(d.getStartTime()));
        wcb.setEndTime(newOTime(d.getEndTime()));
        return wcb;
    }

    public List<Property> newProperties(List<Property> props) {
        List<Property> res=new LinkedList();
        for (Property prop: props) {
            res.add(newProperty(prop));
        }
        return res;
    }
    
    public Annotation newAnnotation(Annotation ann) {
        Annotation res=newAnnotation(ann.getId(),
                                     getFromRegisters(((Identifiable)ann.getLocalSubject()).getId()),
                                     newProperties(ann.getProperty()),
                                     newAccounts(ann.getAccount()));
        return res;
    }

    public Object getFromRegisters(String id) {
        Object res=null;
        res=artifactRegister2.get(id);
        if (res!=null) return res;

        res=accountRegister2.get(id);
        if (res!=null) return res;

        res=processRegister2.get(id);
        if (res!=null) return res;

        res=agentRegister2.get(id);
        if (res!=null) return res;

        return res;
    }


    /** Clones an OPMGraph to an OPMGraph with facade, and creates
     * corresponding rdf representation in triple store. This is
     * definitely a stateful operation. */

    public RdfOPMGraph newOPMGraph(OPMGraph gr) {

        List<Account> accs=new LinkedList();
        for (Account acc: gr.getAccounts().getAccount()) {
            accs.add(register(acc.getId(),newAccount(acc)));
        }



        List<Artifact> as=new LinkedList();
        for (Artifact a: gr.getArtifacts().getArtifact()) {
            as.add(register(a.getId(),newArtifact(a)));
        }

        List<Process> ps=new LinkedList();
        for (Process p: gr.getProcesses().getProcess()) {
            ps.add(register(p.getId(), newProcess(p)));
        }


        List<Agent> ags=new LinkedList();
        for (Agent ag: gr.getAgents().getAgent()) {
            ags.add(register(ag.getId(), newAgent(ag)));
        }

        List<Object> lks=new LinkedList();

        for (Object edge: gr.getCausalDependencies().getUsedOrWasGeneratedByOrWasTriggeredBy()) {
             if (edge instanceof org.openprovenance.model.Used) {
                lks.add(newUsed((org.openprovenance.model.Used) edge));
            } else if (edge instanceof org.openprovenance.model.WasGeneratedBy) {
                lks.add(newWasGeneratedBy((org.openprovenance.model.WasGeneratedBy) edge));
            } else if (edge instanceof org.openprovenance.model.WasDerivedFrom) {
                lks.add(newWasDerivedFrom((org.openprovenance.model.WasDerivedFrom) edge));
             } else if (edge instanceof org.openprovenance.model.WasControlledBy) {
                 lks.add(newWasControlledBy((org.openprovenance.model.WasControlledBy) edge));
             } else if (edge instanceof org.openprovenance.model.WasTriggeredBy) {
                 lks.add(newWasTriggeredBy((org.openprovenance.model.WasTriggeredBy) edge));
            }
        }



        List<Annotation> anns=new LinkedList();
        for (Annotation ann: gr.getAnnotations().getAnnotation()) {
            Annotation ann2=newAnnotation(ann);
            anns.add(ann2);
        }

        OPMGraph res=super.newOPMGraph(gr.getId(),
                                       accs,
                                       new LinkedList(),
                                       ps,
                                       as,
                                       ags,
                                       lks,
                                       anns);

        addNewAnnotations(res,gr.getAnnotation());

        return (RdfOPMGraph) res;
    }


}
