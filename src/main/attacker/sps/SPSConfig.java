package main.attacker.sps;

public class SPSConfig {

    protected int rounds;
    protected KeySetType keySet;
    protected boolean debugMode;
    protected boolean printDetailResult;

    private SPSConfig() {
        this.rounds = 1000;
        this.keySet = KeySetType.RANDOM;
        this.debugMode = false;
        this.printDetailResult = true;
    }

    /**
     * Creates an empty instance of SPS Attack configuration. This and all setter methods are
     * made by Builder design pattern.
     * @return an empty instance of SPS Attack configuration
     */
    public static SPSConfig createSPSConfig() {
        return new SPSConfig();
    }

    public SPSConfig setRounds(int rounds) {
        this.rounds = rounds;
        return this;
    }

    public SPSConfig setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    public SPSConfig shouldPrintResult(boolean printResult) {
        this.printDetailResult = printResult;
        return this;
    }

    public SPSConfig setKeySet(KeySetType keySet) {
        this.keySet = keySet;
        return this;
    }
}