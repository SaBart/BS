package mas.bs.onto;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/**
 * Created by Martin Pilat on 16.4.14.
 *
 * Konkretni vybrana nabidka.
 */
public class Chosen implements Predicate {


    Offer offer;

    @Slot(mandatory = true)
    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }

}
