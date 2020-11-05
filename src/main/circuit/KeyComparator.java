package main.circuit;

import java.util.Comparator;

public class KeyComparator implements Comparator<String> {

    @Override
    public int compare(String s, String t1) {
        String first = s+"a";
        String second = t1+"a";

        return first.compareTo(second);
    }
}
