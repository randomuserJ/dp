package main.circuit;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class CircuitValidator {

    private static int[] createInputValues(int length) {
        Random rnd = new Random();
        int[] initValues = new int[length];
        for (int i = 0; i < length; i++) {
            initValues[i] = rnd.nextInt(2);
        }
        return initValues;
    }

    private static boolean checkLockedFileIntegrity(File lockedFile, File plainFile, boolean debugMode) {

        LogicCircuit lockedCircuit = AbstractLogicCircuit.getCircuitInstance(lockedFile);
        LogicCircuit plainCircuit = AbstractLogicCircuit.getCircuitInstance(plainFile);

        if (lockedCircuit == null || plainCircuit == null)
            return false;

        FormulaFactory ffLocked = new FormulaFactory();
        FormulaFactory ffPlain = new FormulaFactory();

        int[] inputValues = createInputValues(lockedCircuit.getInputNames().size());
        int[] keyValues = lockedCircuit.getCorrectKey();
//        int[] inputValues = new int[]{1, 0, 1, 1, 0};
//        int[] keyValues = new int[]{1,1,1,0,0,1,1,1,0,1,1,1,0,0,1,1};

        Collection<Literal> lockedInputLiterals = lockedCircuit.getInputLiterals(ffLocked, inputValues);
        Collection<Literal> lockedKeyLiterals = lockedCircuit.getKeyLiterals(ffLocked, keyValues);
        Collection<Variable> lockedOutputFilter = lockedCircuit.getOutputVariables(ffLocked);

        Collection<Literal> plainInputLiterals = plainCircuit.getInputLiterals(ffPlain, inputValues);
        Collection<Literal> plainKeyLiterals = plainCircuit.getKeyLiterals(ffPlain, null);
        Collection<Variable> plainOutputFilter = plainCircuit.getOutputVariables(ffPlain);

        Assignment lockedOutput = new Assignment();
        Assignment plainOutput = new Assignment();

        try {
            lockedOutput = lockedCircuit.evaluate(lockedInputLiterals, lockedKeyLiterals, lockedOutputFilter);
            plainOutput = plainCircuit.evaluate(plainInputLiterals, plainKeyLiterals, plainOutputFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return assignmentComparator(lockedOutput, plainOutput, debugMode);
    }

    public static boolean assignmentComparator(Assignment as1, Assignment as2, boolean debugMode) {
        Iterator<Literal> firstIt = as1.literals().iterator();
        Iterator<Literal> secondIt = as2.literals().iterator();
        while (firstIt.hasNext()) {
            Literal firstLiteral = firstIt.next();
            Literal secondLiteral = secondIt.next();

            if (debugMode) {
                System.out.println("A1: " + firstLiteral + " : " + firstLiteral.phase());
                System.out.println("A2: " + secondLiteral + " : " + secondLiteral.phase());
            }
            if (firstLiteral.phase() != secondLiteral.phase()) {
                if (debugMode) {
                    System.err.println("--- ERR ---");
//                    try {
//                        lockedOutput = lockedCircuit.evaluate(lockedInputLiterals, lockedKeyLiterals, null);
//                        plainOutput = plainCircuit.evaluate(plainInputLiterals, plainKeyLiterals, null);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        return false;
//                    }
//                    System.out.println("I: " + Arrays.toString(inputValues));
//                    System.out.println("K: " + Arrays.toString(keyValues));
//                    for (Literal literal : lockedOutput.literals()) {
//                        System.out.println("L: " + literal);
//                    }
//                    for (Literal literal : plainOutput.literals()) {
//                        System.out.println("P: " + literal);
//                    }
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Validates a circuit integrity. Locked circuit with correct key has to produce the same
     * results as the unlocked (plain) circuit.
     * @param lockedFile File containing locked circuit in .bench format.
     * @param plainFile File containing plain circuit in .bench format.
     * @param rounds The number of test rounds.
     * @param debugMode Able or disable logs.
     * @return true, if all rounds of test have passed, false otherwise.
     */
    public static boolean validateCircuitLock(File lockedFile, File plainFile, int rounds, boolean debugMode) {
        System.out.print("Testing file lock integrity in " + rounds + " rounds ..." + (debugMode ? "\n" : " "));
        for (int i = 0; i < rounds; i++) {
            if (!checkLockedFileIntegrity(lockedFile,plainFile,debugMode)) {
                System.out.println("FAILED");
                return false;
            }
            if (debugMode)
                System.out.println("---");
        }
        System.out.println((debugMode ? "Circuits are equal - OK" : "OK"));
        return true;
    }
}
