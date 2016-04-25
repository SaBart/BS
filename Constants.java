package mas.bs;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by Martin Pilat on 3.2.14.
 */
public class Constants {

    static HashMap<String, Double> bookPrices;


    //seznam knih, se kterymi je mozne obchodovat a jejich defaultni ceny
    private static void fillPrices() {
        bookPrices = new HashMap<String, Double>();
        bookPrices.put("The Goldfinch", 50.0);
        bookPrices.put("The Rosie Project", 80.0);
        bookPrices.put("Sycamore Row", 120.0);
        bookPrices.put("The Invention of Wings", 100.0);
        bookPrices.put("The Husband's Secretes", 90.0);
        bookPrices.put("Grain Brain", 150.0);
        bookPrices.put("Shadow Spell", 40.0);
    }

    public static double getPrice(String bookName) {

        if (bookPrices == null) {
            fillPrices();
        }

        if (!bookPrices.containsKey(bookName))
            return Double.NaN;
        return bookPrices.get(bookName);

    }

    public static Set<String> getBooknames() {

        if (bookPrices == null) {
            fillPrices();
        }

        return bookPrices.keySet();

    }
}
