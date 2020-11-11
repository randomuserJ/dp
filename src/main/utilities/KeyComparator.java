package main.utilities;

import java.util.Comparator;

public class KeyComparator implements Comparator<String> {

    @Override
    public int compare(String s, String t1) {
        if (s.startsWith("k") && t1.startsWith("A"))
            return -1;

        if (s.startsWith("A") && t1.startsWith("k"))
            return 1;

        String first = s+"a";
        String second = t1+"a";

        return first.compareTo(second);
    }
}
