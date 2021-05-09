package main.circuit.utilities;

import main.circuit.AbstractLogicCircuit;
import main.circuit.LogicCircuit;
import main.global_utilities.Protocol;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.File;
import java.util.Collection;
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

        Collection<Literal> lockedInputLiterals = lockedCircuit.getInputLiterals(ffLocked, inputValues);
        Collection<Literal> lockedKeyLiterals = lockedCircuit.getKeyLiterals(ffLocked, keyValues);
        Collection<Variable> lockedOutputFilter = lockedCircuit.getOutputVariables(ffLocked);

        Collection<Literal> plainInputLiterals = plainCircuit.getInputLiterals(ffPlain, inputValues);
        Collection<Literal> plainKeyLiterals = plainCircuit.getKeyLiterals(ffPlain, null);
        Collection<Variable> plainOutputFilter = plainCircuit.getOutputVariables(ffPlain);

        Assignment lockedOutput;
        Assignment plainOutput;

        try {
            lockedOutput = lockedCircuit.evaluate(lockedInputLiterals, lockedKeyLiterals, lockedOutputFilter);
            plainOutput = plainCircuit.evaluate(plainInputLiterals, plainKeyLiterals, plainOutputFilter);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Protocol.printErrorMessage("Evaluating circuit: " + e.getMessage());
            return false;
        }

        return CircuitUtilities.compareAssignments(lockedOutput, plainOutput, debugMode);
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
        System.out.print("INFO: Testing file lock integrity in " + rounds + " rounds ..." + (debugMode ? "\n" : " "));
        for (int i = 0; i < rounds; i++) {
            if (debugMode)
                Protocol.printSection("Iteration " + i);
            if (!checkLockedFileIntegrity(lockedFile,plainFile,debugMode)) {
                System.out.println("FAILED");
                return false;
            }
            if (debugMode)
                Protocol.printSection("");
        }
        System.out.println((debugMode ? "Circuits are equal - OK" : "OK"));
        return true;
    }
}
