package mas.bs.onto;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

/**
 * Created by Martin Pilat on 12.2.14.
 *
 * Pozadavek na prostredni, aby prevedlo knihy a penize od jednoho agenta ke druhemu.
 */
public class MakeTransaction implements AgentAction {

    String senderName;
    String receiverName;
    String tradeConversationID;

    ArrayList<BookInfo> sendingBooks;
    double sendingMoney;
    ArrayList<BookInfo> receivingBooks;
    double receivingMoney;


    @Slot(mandatory = true)
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Slot(mandatory = true)
    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    @Slot(mandatory = true)
    public String getTradeConversationID() {
        return tradeConversationID;
    }

    public void setTradeConversationID(String tradeConversationID) {
        this.tradeConversationID = tradeConversationID;
    }

    @Slot(mandatory = true)
    public ArrayList<BookInfo> getSendingBooks() {
        return sendingBooks;
    }

    public void setSendingBooks(ArrayList<BookInfo> books) {
        this.sendingBooks = books;
    }

    @Slot(mandatory = true)
    public double getSendingMoney() {
        return sendingMoney;
    }

    public void setSendingMoney(double money) {
        this.sendingMoney = money;
    }

    @Slot(mandatory = true)
    public ArrayList<BookInfo> getReceivingBooks() {
        return receivingBooks;
    }

    public void setReceivingBooks(ArrayList<BookInfo> receivingBooks) {
        this.receivingBooks = receivingBooks;
    }

    @Slot(mandatory = true)
    public double getReceivingMoney() {
        return receivingMoney;
    }

    public void setReceivingMoney(double receivingMoney) {
        this.receivingMoney = receivingMoney;
    }

    public String toString() {

        String str = "";

        str += "sender: " + senderName + "\n";
        str += "receiver: " + receiverName + "\n";
        str += "sendBooks: ";

        for (int i = 0; i < sendingBooks.size(); i++) {
            str += sendingBooks.get(i);
        }

        str += "\nreceiveBooks: ";

        for (int i = 0; i < receivingBooks.size(); i++) {
            str += receivingBooks.get(i);
        }

        str += "\nsendingMoney: " + sendingMoney;
        str += "\nreceivingMoney: " + receivingMoney;

        return str;
    }
}
