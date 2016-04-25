package mas.bs.onto;

import jade.content.AgentAction;
import jade.content.onto.annotations.Result;
import jade.content.onto.annotations.Slot;

/**
 * Created by marti_000 on 8.4.14.
 */
@Result(type = BookInfo.class)
public class SellBook implements AgentAction {

    BookInfo bi;

    @Slot(mandatory = true)
    public BookInfo getBi() {
        return bi;
    }

    public void setBi(BookInfo bi) {
        this.bi = bi;
    }
}
