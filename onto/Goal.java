package mas.bs.onto;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

/**
 * Created by Martin Pilat on 12.2.14.
 *
 * Trida reprezentujici jeden cil agenta -- cenu a jeji hodnotu pro agenta.
 */
public class Goal implements Concept {

    BookInfo book;
    double value;

    @Slot(mandatory = true)
    public BookInfo getBook() {
        return book;
    }

    public void setBook(BookInfo book) {
        this.book = book;
    }

    @Slot(mandatory = true)
    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String toString() {
        return "[" + book.getBookName() + "," + value + "]";
    }
}
