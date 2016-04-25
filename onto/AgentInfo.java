package mas.bs.onto;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

/**
 * Created by Martin Pilat on 12.2.14.
 *
 * Trida obsahujici informace o agentovi -- cile, senznam knih a penize, ktere agent ma.
 */
public class AgentInfo implements Concept {

    ArrayList<BookInfo> books;
    ArrayList<Goal> goals;
    double money;

    @Slot(mandatory = true)
    public ArrayList<BookInfo> getBooks() {
        return books;
    }

    public void setBooks(ArrayList<BookInfo> books) {
        this.books = books;
    }

    @Slot(mandatory = true)
    public ArrayList<Goal> getGoals() {
        return goals;
    }

    public void setGoals(ArrayList<Goal> goals) {
        this.goals = goals;
    }

    @Slot(mandatory = true)
    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public String toString() {

        String ret = "books: ";

        for (BookInfo bi : books) {
            ret += bi.toString();
        }

        ret += "\ngoals: ";

        for (Goal g : goals) {
            ret += g.toString();
        }

        ret += "\nmoney: " + money;

        return ret;
    }
}
