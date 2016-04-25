package mas.bs.onto;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;

/**
 * Created by marti_000 on 3.2.14.
 */
public class BookOntology extends BeanOntology {

    static BookOntology theInstance = null;

    private BookOntology() {
        super("book-ontology");

        try {
            add("mas.cv4.onto");
        }
        catch (BeanOntologyException be) {
            be.printStackTrace();
        }
    }

    public static BookOntology getInstance() {
        if (theInstance == null)
            theInstance = new BookOntology();

        return theInstance;
    }

}
