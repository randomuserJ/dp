package main.attacker;

import main.attacker.sat.FormulaFactoryWrapped;
import main.circuit.components.Gate;
import main.circuit.LogicCircuit;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


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
        Map<String, Integer> stats = new HashMap<>();
        Collection<Variable> filter = new ArrayList<>();

        for (Gate gate : locked.getGates()) {
            filter.add(f.variable(gate.getOutput()));
            stats.put(gate.getOutput(), 0);
        }

        System.err.println("Performing SPS attack with " + (this.keySet == KeySet.RANDOM ? "random" : "real") + " keys.");

        for (int round = 0; round < this.rounds; round++) {
            int[] rndInputs = new int[locked.getInputNames().size()];
            for(int i = 0; i < locked.getInputNames().size(); i++)
                rndInputs[i] = sr.nextInt() % 2;
            Collection<Literal> testInputs = locked.getInputLiterals(f, rndInputs);

            int[] combinedKey = locked.getCombinedKey();
            int[] rndKeys = new int[locked.getKeyInputNames().size()];
            for(int i = 0; i < locked.getKeyInputNames().size(); i++)
                rndKeys[i] = sr.nextInt() % 2;


            // testing with real key inputs .. expecting 0 - 100 relation between the inputs to final AND gate
            Collection<Literal> realKeys = new ArrayList<>(locked.getKeyLiterals(f, combinedKey));

            // testing with random key inputs .. expecting something close to 0 - 100 relation
            // between the inputs to final AND gate (that is XORed with circuit output)
            Collection<Literal> randomKeys = new ArrayList<>(locked.getKeyLiterals(f, rndKeys));

            Collection<Literal> testKeys = (this.keySet == KeySet.RANDOM) ? randomKeys : realKeys;

            Assignment output = locked.evaluate(testInputs, testKeys, filter);

            for (Variable v : output.positiveVariables()) {
                if (stats.containsKey(v.name()))
                    stats.put(v.name(), stats.get(v.name()) + 1);
            }

            if (debugMode)
                System.out.println(output);
        }
        if (debugMode)
            System.out.println("\n" + stats);

        if (printResult)
            stats.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach((entry) -> System.out.println(entry.getKey() + " : " + entry.getValue()));
    }
}
