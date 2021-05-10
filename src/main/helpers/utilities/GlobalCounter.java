package main.helpers.utilities;

public class GlobalCounter {
    private static int counter = 0;

    public static int getCounter() {
        return counter++;
    }
}
