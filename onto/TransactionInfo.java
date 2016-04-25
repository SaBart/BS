package mas.bs.onto;

import jade.content.Concept;
import jade.lang.acl.ACLMessage;

/**
 * Created by Martin Pilat on 13.2.14.
 *
 * Informace o transakci, nikdy se nikam neposila.
 */
public class TransactionInfo implements Concept {

    MakeTransaction sendOrder;
    long timeReceived;
    ACLMessage senderMessage;

    public TransactionInfo(MakeTransaction sendOrder, ACLMessage senderMessage, long timeReceived) {
        this.sendOrder = sendOrder;
        this.timeReceived = timeReceived;
        this.senderMessage = senderMessage;
    }

    public TransactionInfo() {
    }

    public MakeTransaction getSendOrder() {
        return sendOrder;
    }

    public void setSendOrder(MakeTransaction sendOrder) {
        this.sendOrder = sendOrder;
    }

    public long getTimeReceived() {
        return timeReceived;
    }

    public void setTimeReceived(long timeReceived) {
        this.timeReceived = timeReceived;
    }

    public ACLMessage getSenderMessage() {
        return senderMessage;
    }

    public void setSenderMessage(ACLMessage senderMessage) {
        this.senderMessage = senderMessage;
    }
}
