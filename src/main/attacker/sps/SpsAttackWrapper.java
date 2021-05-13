package main.attacker.sps;

import main.helpers.FormulaFactoryWrapper;
import main.circuit.LogicCircuit;
import main.circuit.components.Gate;
import main.helpers.utilities.ProgressBar;
import main.helpers.utilities.Protocol;
import main.helpers.utilities.Randomizer;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

public class SpsAttackWrapper {

    private final SPSConfig SPSConfiguration;
    private LogicCircuit lockedCircuit;
    private boolean circuitLockedWithSAS = false;

    /**
     * Default constructor sets SPS Attack properties to a default values:
     * 1000 attack iterations with random key bits. Only the result will be printed.
     */
    public SpsAttackWrapper() {
        this.SPSConfiguration = SPSConfig.createSPSConfig();
    }

    /**
     * Constructor for user, who wants to modify Attack properties.
     * @param spsRounds number of attack iteration(s)
     * @param keySet use either RANDOM or REAL keys
     * @param printKey true if 10 best candidates for Y gate shall be printed
     */
    public SpsAttackWrapper(int spsRounds, KeySetType keySet, boolean printKey) {
        this.SPSConfiguration = SPSConfig.createSPSConfig()
                .setRounds(spsRounds)
                .setKeySet(keySet == null ? KeySetType.RANDOM : keySet)
                .shouldPrintResult(printKey)
                .setDebugMode(false);      // default
    }

    /**
     * Performs Signal Probability Skew Attack with settings chosen in constructor.
     */
    public void performSPSAttack() {

        if (!validateCircuitForSPSAttack())
            return;

        String usedKeys = this.SPSConfiguration.keySet == KeySetType.RANDOM ? "random" : "real";
        Protocol.printInfoMessage(String.format(
                "Performing SPS attack on circuit %s with %s keys in %d iteration(s).",
                this.lockedCircuit.getName(), usedKeys, this.SPSConfiguration.rounds));
        if (this.circuitLockedWithSAS)
            Protocol.printInfoMessage("Simulating SAS protection.");

        Protocol.printSection("SPS Attack");
        if (this.SPSConfiguration.rounds == 0) {
            Protocol.printSection("");
            Protocol.printWarningMessage("No results.");
            return;
        }


        Map<Gate, BigDecimal> stats = computeSkews();
        Map<Gate, BigDecimal> adsStats = computeAbsoluteDifferences(stats);

        if (this.SPSConfiguration.debugMode)
            System.out.println("\n" + stats);

        Optional<Map.Entry<Gate, BigDecimal>> candidate = adsStats.entrySet().stream().max(Map.Entry.comparingByValue());
        if (candidate.isPresent()) {
            System.out.printf("SPS Attack result: %s is the Y gate with the probability of %.03f %%.",
                    candidate.get().getKey().getOutput(), candidate.get().getValue().doubleValue() * 100);
            System.out.println(candidate.get().getKey().getOutput().equals(this.lockedCircuit.getAntisatGate()) ?
                    " [CORRECT GUESS]\n" : " [INCORRECT GUESS]\n");
        }

        if (this.SPSConfiguration.printDetailResult) {
            System.out.println("Candidates for Y:");
            adsStats.entrySet().stream()
                    .sorted(Map.Entry.<Gate, BigDecimal>comparingByValue().reversed())
                    .limit(10)
                    .forEach((entry) -> System.out.println(
                            "\t" + entry.getKey().getOutput() + " : " + entry.getValue() +
                                    (entry.getKey().getOutput().equals(this.lockedCircuit.getAntisatGate()) ?
                                            " <--- CORRECT" : ""))
                    );
        }
    }


    /**
     * Computes Absolute Skew Difference between both inputs of each gate. Gate with
     * the highest ADS is the best candidate for Y gate.
     * @param stats map of probability skews of every gate.
     * @return map of Gate objects and theirs ADS values
     */
    private Map<Gate, BigDecimal> computeAbsoluteDifferences(Map<Gate, BigDecimal> stats) {

        BigDecimal averageADS = new BigDecimal((this.SPSConfiguration.rounds / 2));
        BigDecimal bigRounds = new BigDecimal(this.SPSConfiguration.rounds);
        Map<Gate, BigDecimal> absoluteDifferences = new HashMap<>();

        for (Gate gate : this.lockedCircuit.getGates()) {
            List<String> inputs = gate.getInputs();
            if (inputs.size() != 2)
                continue;

            Gate firstInput = this.lockedCircuit.getSingleGate(inputs.get(0));
            Gate secondInput = this.lockedCircuit.getSingleGate(inputs.get(1));
            BigDecimal firstSPS = (firstInput == null) ? averageADS : stats.get(firstInput);
            BigDecimal secondSPS = (secondInput == null) ? averageADS : stats.get(secondInput);

            absoluteDifferences.put(gate, firstSPS.subtract(secondSPS).abs().
                    divide(bigRounds, 5, RoundingMode.CEILING));
        }
        return absoluteDifferences;
    }

    /**
     * Computes Signal Probability Skew of every gate in circuit. Probability skew is a
     * decimal representation of gate's usability in running attack.
     * @return map of Gate objects and theirs SPS values
     */
    private Map<Gate, BigDecimal> computeSkews() {

        FormulaFactory f = FormulaFactoryWrapper.getFormulaFactory();
        Collection<Variable> outputFilter = new ArrayList<>();
        Map<Gate, BigDecimal> stats = new HashMap<>();

        for (Gate gate : this.lockedCircuit.getGates()) {
            outputFilter.add(f.variable(gate.getOutput()));
            stats.put(gate, BigDecimal.ZERO);
        }

        ProgressBar bar = new ProgressBar(this.SPSConfiguration.rounds, "SPS Attack", true);

        for (int round = 0; round < this.SPSConfiguration.rounds; round++) {

            Collection<Literal> testInputs = createInputSetForAttack();
            Collection<Literal> testKeys = createKeySetForAttack();

            if (this.circuitLockedWithSAS)
                testInputs = this.lockedCircuit.changeInputBySAS(testInputs, testKeys);

            Assignment output = this.lockedCircuit.evaluate(testInputs, testKeys, outputFilter);

            for (Variable v : output.positiveVariables()) {
                Gate g = this.lockedCircuit.getSingleGate(v.name());
                if (stats.containsKey(g))
                    stats.put(g, stats.get(g).add(BigDecimal.ONE));
            }

            if (this.SPSConfiguration.debugMode)
                System.out.println(output);

            bar.updateBar(round);
        }

        return stats;
    }

    /**
     * Creates a collection of random input bits. Size of collection is depending on the number
     * of inputs in logic circuit.
     * @return collection of random input literals.
     */
    private Collection<Literal> createInputSetForAttack() {

        FormulaFactory f = FormulaFactoryWrapper.getFormulaFactory();
        SecureRandom sr = Randomizer.getSecureRandom();

        int[] rndInputs = new int[this.lockedCircuit.getInputNames().size()];
        for (int i = 0; i < this.lockedCircuit.getInputNames().size(); i++)
            rndInputs[i] = sr.nextInt(2);

        return this.lockedCircuit.getInputLiterals(f, rndInputs);
    }

    /**
     * Creates a collection of key bits. Size of collection is depending on the number
     * of keys in logic circuit and locking schema.
     * @return collection of either random key literals or correct key. This depends on
     * the value of keySet property.
     */
    private Collection<Literal> createKeySetForAttack() {

        FormulaFactory f = FormulaFactoryWrapper.getFormulaFactory();
        SecureRandom sr = Randomizer.getSecureRandom();

        int[] combinedKey = this.lockedCircuit.getCombinedKey();
        int[] rndKeys = new int[this.lockedCircuit.getKeyInputNames().size()];
        for (int i = 0; i < this.lockedCircuit.getKeyInputNames().size(); i++)
            rndKeys[i] = sr.nextInt(2);

        // testing with real key inputs .. expecting 0 - 100 relation between the inputs to final AND gate
        Collection<Literal> realKeys = new ArrayList<>(this.lockedCircuit.getKeyLiterals(f, combinedKey));

        // testing with random key inputs .. expecting something close to 0 - 100 relation
        // between the inputs to final AND gate (that is XORed with circuit output)
        Collection<Literal> randomKeys = new ArrayList<>(this.lockedCircuit.getKeyLiterals(f, rndKeys));

        return (this.SPSConfiguration.keySet == KeySetType.RANDOM) ? randomKeys : realKeys;
    }

    /**
     * Checks if the specific logic circuit is available for SPS Attack. Circuit must be defined
     * and locked with Anti-SAT. Otherwise returns false.
     */
    private boolean validateCircuitForSPSAttack() {
        try {
            if (this.lockedCircuit == null)
                throw new IllegalStateException("Logic circuit is not set.");

            if (this.lockedCircuit.getAntisatKey().length == 0)
                throw new IllegalStateException("Logic circuit must be locked with AntiSAT.");

        } catch (IllegalStateException e) {
            Protocol.printErrorMessage("Unable to perform SPS Attack: " + e.getMessage());
            return false;
        }

        return true;
    }

    /* Setters */

    public void setLockedCircuit(LogicCircuit lockedCircuit) {
        this.lockedCircuit = lockedCircuit;
    }

    /* Utilities */

    public void simulateSASLock() {
        this.circuitLockedWithSAS = true;
    }
}
