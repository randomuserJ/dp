package main.helpers.utilities;

public class GlobalCounter {
    private static int counter = 0;

    /**
     * Returns an incremented integer value of global counter.
     */
    public static int getCounter() {
        return counter++;
    }

    /**
     * Resets the counter.
     */
    public static void reset() {
        counter = 0;
    }
}
