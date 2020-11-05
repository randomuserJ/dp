package main.circuit;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

        LogicCircuit lockedCircuit = LogicCircuit.getLogicCircuitInstance(lockedFile);
        LogicCircuit plainCircuit = LogicCircuit.getLogicCircuitInstance(plainFile);

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

        Iterator<Literal> lockedIt = lockedOutput.literals().iterator();
        Iterator<Literal> plainIt = plainOutput.literals().iterator();
        while (lockedIt.hasNext()) {
            Literal ll = lockedIt.next();
            Literal pl = plainIt.next();

//            System.out.println("L: " + ll + " : " + ll.phase());
//            System.out.println("P: " + pl + " : " + pl.phase());
            if (ll.phase() != pl.phase()) {
                System.err.println("--- ERR ---");
                if (debugMode) {
                    try {
                        lockedOutput = lockedCircuit.evaluate(lockedInputLiterals, lockedKeyLiterals, null);
                        plainOutput = plainCircuit.evaluate(plainInputLiterals, plainKeyLiterals, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    System.out.println("I: " + Arrays.toString(inputValues));
                    System.out.println("K: " + Arrays.toString(keyValues));
                    for (Literal literal : lockedOutput.literals()) {
                        System.out.println("L: " + literal);
                    }
                    for (Literal literal : plainOutput.literals()) {
                        System.out.println("P: " + literal);
                    }
                }

                return false;
            }
        }

        return true;
    }

    public static boolean validateCircuitLock(File lockedFile, File plainFile, int rounds, boolean debugMode) {
        System.out.print("Testing file lock integrity in " + rounds + " rounds ... ");
        for (int i = 0; i < rounds; i++) {
            if (!checkLockedFileIntegrity(lockedFile,plainFile,debugMode)) {
                System.out.println("FAILED");
                return false;
            }
        }
        System.out.println("OK");
        return true;
    }
}
