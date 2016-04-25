package mas.bs;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPAService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.*;
import mas.cv4.onto.*;

import java.util.*;

/**
 * Created by Martin Pilat on 16.4.14.
 *
 * Jednoducha (testovaci) verze obchodujiciho agenta. Agent neobchoduje nijak rozumne, stara se pouze o to, aby
 * nenabizel knihy, ktere nema. (Ale stejne se muze stat, ze obcas nejakou knihu zkusi prodat dvakrat, kdyz o ni pozadaji
 * dva agenti rychle po sobe.)
 */
public class BookTrader extends Agent {

    Codec codec = new SLCodec();
    Ontology onto = BookOntology.getInstance();

    ArrayList<BookInfo> myBooks;
    ArrayList<Goal> myGoal;
    double myMoney;

    Random rnd = new Random();

    @Override
    protected void setup() {
        super.setup();

        //napred je potreba rict agentovi, jakym zpusobem jsou zpravy kodovany, a jakou pouzivame ontologii
        this.getContentManager().registerLanguage(codec);
        this.getContentManager().registerOntology(onto);

        //popis sluzby book-trader
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-trader");
        sd.setName("book-trader");

        //popis tohoto agenta a sluzeb, ktere nabizi
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(this.getAID());
        dfd.addServices(sd);

        //zaregistrovani s DF
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        //pridame chovani, ktere bude cekat na zpravu o zacatku obchodovani
        addBehaviour(new StartTradingBehaviour(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // ceka na zpravu o zacatku obchodovani a potom prida obchodovaci chovani
    class StartTradingBehaviour extends AchieveREResponder {


        public StartTradingBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {

            try {
                ContentElement ce = getContentManager().extractContent(request);

                if (!(ce instanceof Action)) {
                    throw new NotUnderstoodException("");
                }
                Action a = (Action)ce;


                //dostali jsme info, ze muzeme zacit obchodovat
                if (a.getAction() instanceof StartTrading) {

                    //zjistime si, co mame, a jake jsou nase cile
                    ACLMessage getMyInfo = new ACLMessage(ACLMessage.REQUEST);
                    getMyInfo.setLanguage(codec.getName());
                    getMyInfo.setOntology(onto.getName());

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    getMyInfo.addReceiver(envs[0].getName());
                    getContentManager().fillContent(getMyInfo, new Action(envs[0].getName(), new GetMyInfo()));

                    ACLMessage myInfo = FIPAService.doFipaRequestClient(myAgent, getMyInfo);

                    Result res = (Result)getContentManager().extractContent(myInfo);

                    AgentInfo ai = (AgentInfo)res.getValue();

                    myBooks = ai.getBooks();
                    myGoal = ai.getGoals();
                    myMoney = ai.getMoney();

                    //pridame chovani, ktere jednou za dve vteriny zkusi koupit vybranou knihu
                    addBehaviour(new TradingBehaviour(myAgent, 2000));

                    //pridame chovani, ktere se stara o prodej knih
                    addBehaviour(new SellBook(myAgent, MessageTemplate.MatchPerformative(ACLMessage.CFP)));

                    //odpovime, ze budeme obchodovat (ta zprava se v prostredi ignoruje, ale je slusne ji poslat)
                    ACLMessage reply = request.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    return reply;
                }

                throw new NotUnderstoodException("");

            } catch (Codec.CodecException e) {
                e.printStackTrace();
            } catch (OntologyException e) {
                e.printStackTrace();
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            return super.handleRequest(request);
        }

        class TradingBehaviour extends TickerBehaviour {


            public TradingBehaviour(Agent a, long period) {
                super(a, period);
            }

            @Override
            protected void onTick() {

                try {

                    //najdeme si ostatni prodejce a pripravime zpravu
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("book-trader");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] traders = DFService.search(myAgent, dfd);

                    ACLMessage buyBook = new ACLMessage(ACLMessage.CFP);
                    buyBook.setLanguage(codec.getName());
                    buyBook.setOntology(onto.getName());
                    buyBook.setReplyByDate(new Date(System.currentTimeMillis()+5000));

                    for (DFAgentDescription dfad : traders) {
                        if (dfad.getName().equals(myAgent.getAID()))
                            continue;
                        buyBook.addReceiver(dfad.getName());
                    }

                    ArrayList<BookInfo> bis = new ArrayList<BookInfo>();

                    //vybereme knihu k nakupu
                    BookInfo bi = new BookInfo();
                    bi.setBookName(myGoal.get(rnd.nextInt(myGoal.size())).getBook().getBookName());
                    bis.add(bi);

                    SellMeBooks smb = new SellMeBooks();
                    smb.setBooks(bis);

                    getContentManager().fillContent(buyBook, new Action(myAgent.getAID(), smb));
                    addBehaviour(new ObtainBook(myAgent, buyBook));
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        }


        //vlastni chovani, ktere se stara o opratreni knihy
        class ObtainBook extends ContractNetInitiator {

            public ObtainBook(Agent a, ACLMessage cfp) {
                super(a, cfp);
            }

            Chosen c;  //musime si pamatovat, co jsme nabidli
            ArrayList<BookInfo> shouldReceive; //pamatujeme si, i co nabidl prodavajici nam


            //prodavajici nam posila nasi objednavku, zadame vlastni pozadavek na poslani platby
            @Override
            protected void handleInform(ACLMessage inform) {
                try {


                    //vytvorime informace o transakci a posleme je prostredi
                    MakeTransaction mt = new MakeTransaction();

                    mt.setSenderName(myAgent.getName());
                    mt.setReceiverName(inform.getSender().getName());
                    mt.setTradeConversationID(inform.getConversationId());

                    if (c.getOffer().getBooks() == null)
                        c.getOffer().setBooks(new ArrayList<BookInfo>());

                    mt.setSendingBooks(c.getOffer().getBooks());
                    mt.setSendingMoney(c.getOffer().getMoney());

                    if (shouldReceive == null)
                        shouldReceive = new ArrayList<BookInfo>();

                    mt.setReceivingBooks(shouldReceive);
                    mt.setReceivingMoney(0.0);

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    ACLMessage transReq = new ACLMessage(ACLMessage.REQUEST);
                    transReq.addReceiver(envs[0].getName());
                    transReq.setLanguage(codec.getName());
                    transReq.setOntology(onto.getName());
                    transReq.setReplyByDate(new Date(System.currentTimeMillis() + 5000));

                    getContentManager().fillContent(transReq, new Action(envs[0].getName(), mt));
                    addBehaviour(new SendBook(myAgent, transReq));

                } catch (UngroundedException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }

            //zpracovani nabidek od prodavajicich
            @Override
            protected void handleAllResponses(Vector responses, Vector acceptances) {

                Iterator it = responses.iterator();

                //je potreba vybrat jen jednu nabidku (jinak vytvorime dve transakce se stejnym ID,
                //TODO 2015: upravit, aby bylo mozne prijmout i vice, jak pocitat ID?
                boolean accepted = false;
                while (it.hasNext()) {
                    ACLMessage response = (ACLMessage)it.next();

                    ContentElement ce = null;
                    try {
                        if (response.getPerformative() == ACLMessage.REFUSE) {
                            continue;
                        }

                        ce = getContentManager().extractContent(response);

                        ChooseFrom cf = (ChooseFrom)ce;

                        ArrayList<Offer> offers = cf.getOffers();

                        //zjistime, ktere nabidky muzeme splnit
                        ArrayList<Offer> canFulfill = new ArrayList<Offer>();
                        for (Offer o: offers) {
                            if (o.getMoney() > myMoney)
                                continue;

                            boolean foundAll = true;
                            if (o.getBooks() != null)
                                for (BookInfo bi : o.getBooks()) {
                                    String bn = bi.getBookName();
                                    boolean found = false;
                                    for (int j = 0; j < myBooks.size(); j++) {
                                        if (myBooks.get(j).getBookName().equals(bn)) {
                                            found = true;
                                            bi.setBookID(myBooks.get(j).getBookID());
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        foundAll = false;
                                        break;
                                    }
                                }

                            if (foundAll) {
                                canFulfill.add(o);
                            }
                        }

                        //kdyz zadnou, tak odmitneme, stejne tak, kdyz uz jsme nejakou prijali
                        if (canFulfill.size() == 0 || accepted) {
                            ACLMessage acc = response.createReply();
                            acc.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            acceptances.add(acc);
                            continue;
                        }

                        ACLMessage acc = response.createReply();
                        acc.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        accepted = true;

                        //vybereme nabidku
                        Chosen ch = new Chosen();
                        ch.setOffer(canFulfill.get(rnd.nextInt(canFulfill.size())));

                        c=ch;
                        shouldReceive = cf.getWillSell();

                        getContentManager().fillContent(acc, ch);
                        acceptances.add(acc);

                    } catch (Codec.CodecException e) {
                        e.printStackTrace();
                    } catch (OntologyException e) {
                        e.printStackTrace();
                    }

                }

            }
        }


        //chovani, ktere se stara o prodej knih
        class SellBook extends SSResponderDispatcher {

            public SellBook(Agent a, MessageTemplate tpl) {
                super(a, tpl);
            }

            @Override
            protected Behaviour createResponder(ACLMessage initiationMsg) {
                return new SellBookResponder(myAgent, initiationMsg);
            }
        }

        class SellBookResponder extends SSContractNetResponder {

            public SellBookResponder(Agent a, ACLMessage cfp) {
                super(a, cfp);
            }

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {

                try {
                    Action ac = (Action)getContentManager().extractContent(cfp);

                    SellMeBooks smb = (SellMeBooks)ac.getAction();
                    ArrayList<BookInfo> books = smb.getBooks();

                    ArrayList<BookInfo> sellBooks = new ArrayList<BookInfo>();

                    //zjistime, jestli mame knihy, ktere agent chce
                    for (int i = 0; i < books.size(); i++) {
                        boolean found = false;
                        for (int j = 0; j < myBooks.size(); j++) {
                            if (myBooks.get(j).getBookName().equals(books.get(i).getBookName())) {
                                sellBooks.add(myBooks.get(j));
                                found = true;
                                break;
                            }
                        }
                        if (!found)
                            throw new RefuseException("");
                    }

                    //vytvorime dve neodolatelne nabidky
                    Offer o1 = new Offer();
                    o1.setMoney(100);

                    ArrayList<BookInfo> bis = new ArrayList<BookInfo>();
                    bis.add(myGoal.get(rnd.nextInt(myGoal.size())).getBook());

                    Offer o2 = new Offer();
                    o2.setBooks(bis);
                    o2.setMoney(20);

                    ArrayList<Offer> offers = new ArrayList<Offer>();
                    offers.add(o1);
                    offers.add(o2);

                    ChooseFrom cf = new ChooseFrom();

                    cf.setWillSell(sellBooks);
                    cf.setOffers(offers);

                    //posleme nabidky
                    ACLMessage reply = cfp.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
                    getContentManager().fillContent(reply, cf);

                    return reply;
                } catch (UngroundedException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                }

                throw new FailureException("");
            }
            //agent se rozhodl, ze nabidku prijme
            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {

                try {
                    ChooseFrom cf = (ChooseFrom)getContentManager().extractContent(propose);

                    //pripravime info o transakci a zadame ji prostredi
                    MakeTransaction mt = new MakeTransaction();

                    mt.setSenderName(myAgent.getName());
                    mt.setReceiverName(cfp.getSender().getName());
                    mt.setTradeConversationID(cfp.getConversationId());

                    if (cf.getWillSell() == null) {
                        cf.setWillSell(new ArrayList<BookInfo>());
                    }

                    mt.setSendingBooks(cf.getWillSell());
                    mt.setSendingMoney(0.0);

                    Chosen c = (Chosen)getContentManager().extractContent(accept);

                    if (c.getOffer().getBooks() == null) {
                        c.getOffer().setBooks(new ArrayList<BookInfo>());
                    }

                    mt.setReceivingBooks(c.getOffer().getBooks());
                    mt.setReceivingMoney(c.getOffer().getMoney());

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    ACLMessage transReq = new ACLMessage(ACLMessage.REQUEST);
                    transReq.addReceiver(envs[0].getName());
                    transReq.setLanguage(codec.getName());
                    transReq.setOntology(onto.getName());
                    transReq.setReplyByDate(new Date(System.currentTimeMillis() + 5000));

                    getContentManager().fillContent(transReq, new Action(envs[0].getName(), mt));

                    addBehaviour(new SendBook(myAgent, transReq));

                    ACLMessage reply = accept.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    return reply;

                } catch (UngroundedException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

                throw new FailureException("");
            }
        }

        //po dokonceni obchodu (prostredi poslalo info) si aktualizujeme vlastni seznam knih a cile
        class SendBook extends AchieveREInitiator {

            public SendBook(Agent a, ACLMessage msg) {
                super(a, msg);
            }

            @Override
            protected void handleInform(ACLMessage inform) {

                try {
                    ACLMessage getMyInfo = new ACLMessage(ACLMessage.REQUEST);
                    getMyInfo.setLanguage(codec.getName());
                    getMyInfo.setOntology(onto.getName());

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    getMyInfo.addReceiver(envs[0].getName());
                    getContentManager().fillContent(getMyInfo, new Action(envs[0].getName(), new GetMyInfo()));

                    ACLMessage myInfo = FIPAService.doFipaRequestClient(myAgent, getMyInfo);

                    Result res = (Result)getContentManager().extractContent(myInfo);

                    AgentInfo ai = (AgentInfo)res.getValue();

                    myBooks = ai.getBooks();
                    myGoal = ai.getGoals();
                    myMoney = ai.getMoney();
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}
