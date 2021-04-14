package main.attacker.sps;

import main.attacker.sat.FormulaFactoryWrapper;
import main.circuit.LogicCircuit;
import main.circuit.components.Gate;
import main.utilities.Randomizer;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

public class SpsAttackWrapper {

    private SPSConfig SPSConfiguration;
    private LogicCircuit lockedCircuit;
    private boolean circuitLockedWithSAS = false;

    public SpsAttackWrapper() {
        this.SPSConfiguration = SPSConfig.createSPSConfig();
    }

    public SpsAttackWrapper(int spsRounds, KeySetForSPS keySet, boolean printKey) {
        this.SPSConfiguration = SPSConfig.createSPSConfig()
                .setRounds(spsRounds)
                .setKeySet(keySet == null ? KeySetForSPS.RANDOM : keySet)
                .printResult(printKey)
                .setDebugMode(false);      // default
    }

    public void setLockedCircuit(LogicCircuit lockedCircuit) {
        this.lockedCircuit = lockedCircuit;
    }

    public void simulateSASLock() {
        this.circuitLockedWithSAS = true;
    }

    public void performSPSAttack() {

        if (!validateCircuitForSPSAttack())
            return;

        Map<Gate, BigDecimal> stats = computeStatics();
        Map<Gate, BigDecimal> adsStats = computeAbsoluteDifferences(stats);

        if (this.SPSConfiguration.debugMode)
            System.out.println("\n" + stats);

        Optional<Map.Entry<Gate, BigDecimal>> candidate = adsStats.entrySet().stream().max(Map.Entry.comparingByValue());
        if (candidate.isPresent())
            System.out.println("SPS Attack result: " + candidate.get().getKey().getOutput() + " is Y gate with " + candidate.get().getValue().doubleValue() * 100 + "%." );
        else
            System.out.println("Attack unsuccessful.");

        if (this.SPSConfiguration.printDetailResult) {
            System.out.println("Candidates for Y:");
            adsStats.entrySet().stream()
                    .sorted(Map.Entry.<Gate, BigDecimal>comparingByValue().reversed())
                    .limit(10)
                    .forEach((entry) -> System.out.println("\t" + entry.getKey().getOutput() + " : " + entry.getValue()));
        }
    }
    //        stats.entrySet().stream()
    //                .sorted(Map.Entry.<Gate, BigDecimal>comparingByValue().reversed())
    //                .forEach((entry) -> System.out.println(entry.getKey().getOutput() + " : " + entry.getValue()));

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

            absoluteDifferences.put(gate, firstSPS.subtract(secondSPS).abs().divide(bigRounds).setScale(5, RoundingMode.DOWN));
        }
        return absoluteDifferences;
    }


    private Map<Gate, BigDecimal> computeStatics() {

        FormulaFactory f = FormulaFactoryWrapper.getFormulaFactory();
        Collection<Variable> outputFilter = new ArrayList<>();
        Map<Gate, BigDecimal> stats = new HashMap<>();

        for (Gate gate : this.lockedCircuit.getGates()) {
            outputFilter.add(f.variable(gate.getOutput()));
            stats.put(gate, BigDecimal.ZERO);
        }

        String usedKeys = this.SPSConfiguration.keySet == KeySetForSPS.RANDOM ? "random" : "real";
        System.err.printf("Performing SPS attack with %s keys in %d iteration(s).\n", usedKeys, this.SPSConfiguration.rounds);

        for (int round = 0; round < this.SPSConfiguration.rounds; round++) {

            Collection<Literal> testInputs = createInputSetForAttack();
            Collection<Literal> testKeys = createKeySetForAttack();

            if (this.circuitLockedWithSAS)
                testInputs = this.lockedCircuit.changeInputBySAS(testInputs, testKeys);

            Assignment output = this.lockedCircuit.evaluate(testInputs, testKeys, outputFilter);

            for (Variable v : output.positiveVariables()) {
                Gate g = this.lockedCircuit.getSingleGate(v.name());
                if (g == null) System.err.println(v.name() + " is not a Gate");
                if (stats.containsKey(g))
                    stats.put(g, stats.get(g).add(BigDecimal.ONE));
            }

            if (this.SPSConfiguration.debugMode)
                System.out.println(output);
        }

        return stats;
    }

    private Collection<Literal> createInputSetForAttack() {

        FormulaFactory f = FormulaFactoryWrapper.getFormulaFactory();
        SecureRandom sr = Randomizer.getSecureRandom();

        int[] rndInputs = new int[this.lockedCircuit.getInputNames().size()];
        for (int i = 0; i < this.lockedCircuit.getInputNames().size(); i++)
            rndInputs[i] = sr.nextInt(2);

        return this.lockedCircuit.getInputLiterals(f, rndInputs);
    }

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

        return (this.SPSConfiguration.keySet == KeySetForSPS.RANDOM) ? randomKeys : realKeys;
    }

    private boolean validateCircuitForSPSAttack() {
        try {
            if (this.lockedCircuit == null)
                throw new IllegalStateException("Logic circuit is not set.");

            if (this.lockedCircuit.getAntisatKey().length == 0)
                throw new IllegalStateException("Logic circuit must be locked by AntiSAT lock.");

        } catch (IllegalStateException e) {
            System.err.println("Unable to perform SPS Attack: " + e.getMessage());
            return false;
        }

        return true;
    }
}
