package mas.bs;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import mas.bs.onto.*;
import mas.bs.onto.BookInfo;
import mas.bs.onto.BookOntology;

import java.util.*;

/**
 * Created by Martin Pilat on 11.2.14.
 *
 * Implementace prostredi, ktere se stara o obchodovani mezi agenty, posilani knih a spravu informaci o agentech.
 */
public class Environment extends Agent {

    ArrayList<String> agents = new ArrayList<String>();
    HashMap<String, AgentInfo> agentBooks = new HashMap<String, AgentInfo>();
    HashMap<String, TransactionInfo> unfinishedTransaction = new HashMap<String, TransactionInfo>();
    Codec codec = new SLCodec();
    Ontology onto = BookOntology.getInstance();
    Random rnd = new Random();


    @Override
    protected void setup() {
        super.setup();

        //napred je potreba rict agentovi, jakym zpusobem jsou zpravy kodovany, a jakou pouzivame ontologii
        this.getContentManager().registerLanguage(codec);
        this.getContentManager().registerOntology(onto);

        //popis sluzby environment
        ServiceDescription sd = new ServiceDescription();
        sd.setType("environment");
        sd.setName("env");

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

        //chovani, ktere vsem posle info o zacatku obchodovani
        addBehaviour(new StartTradingBehavior());
        //chovani, ktere periodicky vypisuje aktualni zisky agentu
        addBehaviour(new PrintAgentUtilityBehaviour(this));
        //chovani, ktere se stara o vsechny requesty
        addBehaviour(new MessageDispatcherBehavior());
        //chovani, ktere periodicky odstranuje transakce, ktere neprobehly vcas
        addBehaviour(new UnfinishedTransactionsRemoverBehavior(this));

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

    //posleme vsem info o zacatku obchodovani, vygenerujeme prirazeni knih a cile
    private class StartTradingBehavior extends OneShotBehaviour {

        @Override
        public void action() {

            //najdeme vsechny obchodniky
            ServiceDescription sd = new ServiceDescription();
            sd.setType("book-trader");
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            try {
                DFAgentDescription[] traders = DFService.search(myAgent, dfd);

                ACLMessage startMsg = new ACLMessage(ACLMessage.REQUEST);
                startMsg.setOntology(onto.getName());
                startMsg.setLanguage(codec.getName());

                ArrayList<String> booksNames = new ArrayList<String>();
                booksNames.addAll(Constants.getBooknames());
                int bID = 0;

                //vygenerujeme cile a knihy pro kazdeho agenta
                for (DFAgentDescription tr : traders) {

                    Collections.shuffle(booksNames,rnd);
                    AgentInfo ai = new AgentInfo();
                    ArrayList<BookInfo> books = new ArrayList<BookInfo>();
                    ArrayList<Goal> goal = new ArrayList<Goal>();


                    for (int i = 0; i < 4; i++) {
                        BookInfo bi = new BookInfo();
                        bi.setBookID(bID++);
                        bi.setBookName(booksNames.get(i));
                        books.add(bi);
                    }

                    for (int i = 4; i < booksNames.size(); i++) {
                        BookInfo bi = new BookInfo();
                        bi.setBookID(bID++);
                        bi.setBookName(booksNames.get(i));
                        Goal g = new Goal();
                        g.setBook(bi);
                        g.setValue(Constants.getPrice(booksNames.get(i))+rnd.nextInt(40)-20);
                        goal.add(g);
                    }

                    ai.setBooks(books);
                    ai.setGoals(goal);
                    ai.setMoney(400);

                    System.out.println("Created goals for: " + tr.getName().getName());
                    System.out.println("\t" + ai.toString());
                    agentBooks.put(tr.getName().getName(), ai);
                    startMsg.addReceiver(tr.getName());
                }


                getContentManager().fillContent(startMsg, new Action(myAgent.getAID(), new StartTrading()));

                addBehaviour(new AchieveREInitiator(myAgent, startMsg));

            } catch (FIPAException e) {
                e.printStackTrace();
            } catch (Codec.CodecException e) {
                e.printStackTrace();
            } catch (OntologyException e) {
                e.printStackTrace();
            }

        }
    }

    //kazdych 15 vterin vypiseme, kdo ma jaky uzitek
    private class PrintAgentUtilityBehaviour extends TickerBehaviour {

        public PrintAgentUtilityBehaviour(Agent myAgent) {
            super(myAgent, 1000);
        }

        private class AgentUtil implements Comparable<AgentUtil> {

            public String agent;
            public double util;
            public boolean goalMet;

            public AgentUtil(String agent, double util, boolean goalMet) {
                this.agent = agent;
                this.util = util;
                this.goalMet = goalMet;
            }

            @Override
            public int compareTo(AgentUtil o) {
                return Double.compare(this.util, o.util);
            }
        }

        @Override
        protected void onTick() {

            ArrayList<AgentUtil> utils = new ArrayList<AgentUtil>();

            for (String aid : agentBooks.keySet()) {
                AgentInfo ai = agentBooks.get(aid);
                double util = Utils.computeUtility(ai);
                boolean allGoals = Utils.hasAllBooks(ai);
                utils.add(new AgentUtil(aid, util, allGoals));
            }

            Collections.sort(utils);

            System.out.println();
            System.out.println();
            for (int i = utils.size() - 1; i >=0; i--) {
                AgentUtil au = utils.get(i);
                System.out.printf("%50s  %13f %5s \n", au.agent, au.util, au.goalMet ? "YES" : "NO");
                //System.out.println(agentBooks.get(au.agent));
            }
            System.out.println();

        }
    }

    //kazdych 5 vterin smazeme transakce, ke kterym behem 5 vterin nedorazila druha polovina
    private class UnfinishedTransactionsRemoverBehavior extends TickerBehaviour {

        public UnfinishedTransactionsRemoverBehavior(Agent myAgent) {
            super(myAgent, 5000);
        }

        @Override
        protected void onTick() {

            ArrayList<String> remove = new ArrayList<String>();

            long time = System.currentTimeMillis();
            for (TransactionInfo ti : unfinishedTransaction.values()) {
                if (ti.getTimeReceived()-time > 5000)
                    remove.add(ti.getSendOrder().getTradeConversationID());
            }

            for (String s : remove) {

                ACLMessage fail = unfinishedTransaction.get(s).getSenderMessage().createReply();
                fail.setPerformative(ACLMessage.FAILURE);
                fail.setContent("transaction not matched in time");
                send(fail);
                unfinishedTransaction.remove(s);
            }

        }
    }

    //rozdeleni requestu mezi dve chovani, ktera se o ne postaraji
    private class MessageDispatcherBehavior extends CyclicBehaviour {

        @Override
        public void action() {

            ACLMessage received = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            if (received == null) {
                block();
                return;
            }

            ContentElement ce = null;
            try {
                ce = myAgent.getContentManager().extractContent(received);
            } catch (Codec.CodecException e) {
                e.printStackTrace();
            } catch (OntologyException e) {
                e.printStackTrace();
            }

            if (!(ce instanceof Action)) {
                System.err.println("Unexpected message: " + received.getContent());
            }

            Action aa = (Action)ce;

            //pridani chovani pro MakeTransaction
            if (aa.getAction() instanceof MakeTransaction) {
                myAgent.addBehaviour(new HandleSendBehaviour(myAgent, (MakeTransaction)aa.getAction(), received));
            }

            //pridani chovani pro GetMyInfo
            if (aa.getAction() instanceof GetMyInfo) {
                myAgent.addBehaviour(new HandleInfoBehaviour(myAgent, (GetMyInfo)aa.getAction(), received));
            }

        }
    }

    //chovani, ktere se stara o zaslani informaci o agentovi, ktery o to projevi zajem
    private class HandleInfoBehaviour extends OneShotBehaviour {

        Agent myAgent;
        GetMyInfo gmi;
        ACLMessage request;

        public HandleInfoBehaviour(Agent myAgent, GetMyInfo gmi, ACLMessage request) {
            this.myAgent = myAgent;
            this.gmi = gmi;
            this.request = request;
        }

        @Override
        public void action() {

            //System.out.println("Got info request from " + request.getSender().getName());

            ACLMessage reply = request.createReply();

            String agentName = request.getSender().getName();

            //zjistime informace
            AgentInfo ai = agentBooks.get(agentName);

            if (ai == null) {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("agent not found");
                send(reply);
                return;
            }

            //posleme je agentovi
            reply.setPerformative(ACLMessage.INFORM);
            try {
                getContentManager().fillContent(reply, new Result(gmi, ai));
            } catch (Codec.CodecException e) {
                e.printStackTrace();
            } catch (OntologyException e) {
                e.printStackTrace();
            }

            myAgent.send(reply);
        }
    }


    //zpracovani transakce mezi dvemi agenty
    private class HandleSendBehaviour extends OneShotBehaviour {

        MakeTransaction sendMsgContent;
        ACLMessage sendMsg;

        private HandleSendBehaviour(Agent a, MakeTransaction sendMsgContent, ACLMessage sendMsg ) {
            super(a);
            this.sendMsgContent = sendMsgContent;
            this.sendMsg = sendMsg;
        }

        @Override
        public void action() {

            String transactionID = sendMsgContent.getTradeConversationID();


            //prisla info od jednoho agenta, zapamatujeme si transakci
            if (!unfinishedTransaction.containsKey(transactionID)) { //this is the first time we know about transaction
                unfinishedTransaction.put(transactionID, new TransactionInfo(sendMsgContent, sendMsg, System.currentTimeMillis()));
                return;
            }

            //prisla info od druheho ucastnika transakce
            ACLMessage sendMsg1 = unfinishedTransaction.get(transactionID).getSenderMessage();
            ACLMessage sendMsg2 = sendMsg;

            MakeTransaction sendOrder1 = unfinishedTransaction.get(transactionID).getSendOrder();
            MakeTransaction sendOrder2 = sendMsgContent;


            //kontrola, ze sedi odesilatele a prijemci v obou zpravach
            if (!sendOrder1.getReceiverName().equals(sendOrder2.getSenderName()) ||
                    !sendOrder1.getSenderName().equals(sendOrder2.getReceiverName())) {

                sendFailure(sendMsg1, sendMsg2, "sender and receiver do not match");
            }

            String agentName1 = sendOrder1.getSenderName();
            String agentName2 = sendOrder2.getSenderName();

            AgentInfo agentInfo1 = agentBooks.get(agentName1);
            AgentInfo agentInfo2 = agentBooks.get(agentName2);

            //kontrola, ze agenti maji vsechny knihy, ktere chteji odeslat
            ArrayList<BookInfo> ag1MissingBooks = getMissingBooks(agentInfo1, sendOrder1.getSendingBooks());
            if (ag1MissingBooks.size() > 0) {
                sendFailure(sendMsg1, sendMsg2, agentName1 + " does not have " + ag1MissingBooks);
                return;
            }

            ArrayList<BookInfo> ag2MissingBooks = getMissingBooks(agentInfo2, sendOrder2.getSendingBooks());
            if (ag2MissingBooks.size() > 0) {
                sendFailure(sendMsg1, sendMsg2, agentName2 + " does not have " + ag2MissingBooks);
                return;
            }

            //kontrola, ze agenti maji dost penez
            if (agentInfo1.getMoney() < sendOrder1.getSendingMoney()) {
                sendFailure(sendMsg1, sendMsg2, agentName1 + " does not have enough money");
                return;
            }

            if (agentInfo2.getMoney() < sendOrder2.getSendingMoney()) {
                sendFailure(sendMsg1, sendMsg2, agentName2 + " does not have enough money");
                return;
            }

            //kontrola, ze sedi knihy, ktere agent odesila a druhy agent ocekava
            if (sendOrder1.getSendingBooks() != null && sendOrder2.getReceivingBooks() != null)
                if (sendOrder1.getSendingBooks().size() != sendOrder2.getReceivingBooks().size()) {
                    sendFailure(sendMsg1, sendMsg2, "orders do not match");
                    return;
                }

            if (sendOrder2.getSendingBooks() != null && sendOrder1.getReceivingBooks() != null)
                if (sendOrder1.getReceivingBooks().size() != sendOrder2.getSendingBooks().size()) {
                    sendFailure(sendMsg1, sendMsg2, "orders do not match");
                    return;
                }

            for (int i = 0; i < sendOrder1.getSendingBooks().size(); i++) {
                if (sendOrder1.getSendingBooks().get(i).getBookID() != sendOrder2.getReceivingBooks().get(i).getBookID()) {
                    sendFailure(sendMsg1, sendMsg2, "orders do not match");
                    return;
                }
            }

            for (int i = 0; i < sendOrder2.getSendingBooks().size(); i++) {
                if (sendOrder2.getSendingBooks().get(i).getBookID() != sendOrder1.getReceivingBooks().get(i).getBookID()) {
                    sendFailure(sendMsg1, sendMsg2, "orders do not match");
                    return;
                }
            }

            //kontrola, ze sedi mnozstvi penez, ktere agenti posilaji a ocekavaji
            if (sendOrder1.getSendingMoney() != sendOrder2.getReceivingMoney()) {
                sendFailure(sendMsg1, sendMsg2, "orders do not match");
                return;
            }

            if (sendOrder1.getReceivingMoney() != sendOrder2.getSendingMoney()) {
                sendFailure(sendMsg1, sendMsg2, "orders do not match");
                return;
            }

            //odstranime knihy, ktere agent odesila
            ArrayList<BookInfo> books = agentInfo1.getBooks();
            ArrayList<BookInfo> removeBooks = sendOrder1.getSendingBooks();
            for (int i = 0; i < removeBooks.size(); i++) {
                for (int j = 0; j < books.size(); j++) {
                    if (books.get(j).getBookID() == removeBooks.get(i).getBookID()) {
                        books.remove(j);
                        break;
                    }
                }
            }

            //to same pro druheho agenta
            books = agentInfo2.getBooks();
            removeBooks = sendOrder2.getSendingBooks();
            for (int i = 0; i < removeBooks.size(); i++) {
                for (int j = 0; j < books.size(); j++) {
                    if (books.get(j).getBookID() == removeBooks.get(i).getBookID()) {
                        books.remove(j);
                        break;
                    }
                }
            }

            //pridame knihy, ktere agent dostava
            books = agentInfo1.getBooks();
            ArrayList<BookInfo> addBooks = sendOrder2.getSendingBooks();
            for (int i = 0; i < addBooks.size(); i++) {
                books.add(addBooks.get(i));
            }

            //a zase to same pro druheho
            books = agentInfo2.getBooks();
            addBooks = sendOrder1.getSendingBooks();
            for (int i = 0; i < addBooks.size(); i++) {
                books.add(addBooks.get(i));
            }

            //System.out.println("Transaction: " + sendOrder1);


            //prevedeme penize mezi agenty
            agentInfo1.setMoney(agentInfo1.getMoney()-sendOrder1.getSendingMoney());
            agentInfo1.setMoney(agentInfo1.getMoney()+sendOrder2.getSendingMoney());

            agentInfo2.setMoney(agentInfo2.getMoney()-sendOrder2.getSendingMoney());
            agentInfo2.setMoney(agentInfo2.getMoney()+sendOrder1.getSendingMoney());

            //posleme obema info, ze se prevod povedl
            ACLMessage reply1 = sendMsg1.createReply();
            ACLMessage reply2 = sendMsg2.createReply();
            reply1.setPerformative(ACLMessage.INFORM);
            reply2.setPerformative(ACLMessage.INFORM);
            reply1.setContent("done");
            reply2.setContent("done");
            send(reply1);
            send(reply2);

            //System.out.println(agentName1 + " " + agentInfo1.toString());
            //System.out.println(agentName2 + " " + agentInfo2.toString());

        }

        //poslani chyby obema agentum, chyba je jako text, dulezity je jen performative
        void sendFailure(ACLMessage msg1, ACLMessage msg2, String text) {
            ACLMessage reply1 = msg1.createReply();
            ACLMessage reply2 = msg2.createReply();
            reply1.setPerformative(ACLMessage.FAILURE);
            reply2.setPerformative(ACLMessage.FAILURE);
            reply1.setContent(text);
            reply2.setContent(text);
            send(reply1);
            send(reply2);
        }


        //najde knihy, ktere agent chce poslat, ale nema je
        ArrayList<BookInfo> getMissingBooks(AgentInfo agent, ArrayList<BookInfo> books) {
            ArrayList<BookInfo> missing = new ArrayList<BookInfo>();
            for (BookInfo bi : books) {
                boolean found = false;
                for (int i = 0; i < agent.getBooks().size(); i++) {
                    if (agent.getBooks().get(i).getBookID() == bi.getBookID()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missing.add(bi);
                }
            }
            return missing;
        }
    }

}
