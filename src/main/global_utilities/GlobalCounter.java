package main.global_utilities;

public class GlobalCounter {
    private static int counter = 0;

    public static int getCounter() {
        return counter++;
    }
}
