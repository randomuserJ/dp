package main.attacker;

import main.attacker.sat.FormulaFactoryWrapped;
import main.circuit.components.Gate;
import main.circuit.LogicCircuit;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;


public class SPSConfig {

    public enum KeySet {
        REAL,
        RANDOM
    }

    private int rounds;
    private KeySet keySet;
    private boolean debugMode;
    private boolean printResult;

    private SPSConfig() {
        this.rounds = 100;
        this.keySet = KeySet.RANDOM;
        this.debugMode = false;
        this.printResult = true;
    }

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

    public SPSConfig printResult(boolean printResult) {
        this.printResult = printResult;
        return this;
    }

    public SPSConfig setKeySet(KeySet keySet) {
        this.keySet = keySet;
        return this;
    }

    public void performSPSAttack(LogicCircuit locked) {
        SecureRandom sr = new SecureRandom();
        FormulaFactory f = FormulaFactoryWrapped.getFormulaFactory();
        Map<Gate, BigDecimal> stats = new HashMap<>();
        Map<Gate, BigDecimal> adsStats = new HashMap<>();
        Collection<Variable> filter = new ArrayList<>();
        BigDecimal averageADS = new BigDecimal((this.rounds / 2));
        BigDecimal bigRounds = new BigDecimal(this.rounds);

        for (Gate gate : locked.getGates()) {
            filter.add(f.variable(gate.getOutput()));
            stats.put(gate, BigDecimal.ZERO);
        }

        if (locked.getAntisatKey().length == 0)
            throw new IllegalStateException("Cannot perform SPS attack on logic circuit without AntiSAT lock.");

        System.err.println("Performing SPS attack with " + (this.keySet == KeySet.RANDOM ? "random" : "real") + " keys.");

        for (int round = 0; round < this.rounds; round++) {
            int[] rndInputs = new int[locked.getInputNames().size()];
            for (int i = 0; i < locked.getInputNames().size(); i++)
                rndInputs[i] = sr.nextInt() % 2;
            Collection<Literal> testInputs = locked.getInputLiterals(f, rndInputs);

            int[] combinedKey = locked.getCombinedKey();
            int[] rndKeys = new int[locked.getKeyInputNames().size()];
            for (int i = 0; i < locked.getKeyInputNames().size(); i++)
                rndKeys[i] = sr.nextInt() % 2;


            // testing with real key inputs .. expecting 0 - 100 relation between the inputs to final AND gate
            Collection<Literal> realKeys = new ArrayList<>(locked.getKeyLiterals(f, combinedKey));

            // testing with random key inputs .. expecting something close to 0 - 100 relation
            // between the inputs to final AND gate (that is XORed with circuit output)
            Collection<Literal> randomKeys = new ArrayList<>(locked.getKeyLiterals(f, rndKeys));

            Collection<Literal> testKeys = (this.keySet == KeySet.RANDOM) ? randomKeys : realKeys;

            Assignment output = locked.evaluate(testInputs, testKeys, filter);

//            for (Variable v : output.positiveVariables()) {
//                if (stats.containsKey(v.name()))
//                    stats.put(v.name(), stats.get(v.name()).add(BigDecimal.ONE));
//            }

            for (Variable v : output.positiveVariables()) {
                Gate g = locked.getSingleGate(v.name());
                if (g == null) System.err.println(v.name() + " is not a Gate");
                if (stats.containsKey(g))
                    stats.put(g, stats.get(g).add(BigDecimal.ONE));
            }

            if (debugMode)
                System.out.println(output);
        }
        if (debugMode)
            System.out.println("\n" + stats);

        for (Gate gate : locked.getGates()) {
            List<String> inputs = gate.getInputs();
            if (inputs.size() != 2)
                continue;

            Gate firstInput = locked.getSingleGate(inputs.get(0));
            Gate secondInput = locked.getSingleGate(inputs.get(1));
            BigDecimal firstSPS = (firstInput == null) ? averageADS : stats.get(firstInput);
            BigDecimal secondSPS = (secondInput == null) ? averageADS : stats.get(secondInput);

            adsStats.put(gate, firstSPS.subtract(secondSPS).abs().divide(bigRounds).setScale(5, RoundingMode.DOWN));
        }

        if (printResult) {
            System.out.println("Candidates for Y:");
            adsStats.entrySet().stream()
                    .sorted(Map.Entry.<Gate, BigDecimal>comparingByValue().reversed())
                    .limit(3)
                    .forEach((entry) -> System.out.println("\t" + entry.getKey().getOutput() + " : " + entry.getValue()));
        }
//        stats.entrySet().stream()
//                .sorted(Map.Entry.<Gate, BigDecimal>comparingByValue().reversed())
//                .forEach((entry) -> System.out.println(entry.getKey().getOutput() + " : " + entry.getValue()));
    }
}