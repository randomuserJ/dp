package main.circuit.utilities;

import main.circuit.LogicCircuit;
import main.helpers.utilities.Protocol;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

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

    private static boolean checkLockedFileIntegrity(LogicCircuit lockedCircuit, LogicCircuit plainCircuit, boolean debugMode) {

        FormulaFactory ffLocked = new FormulaFactory();
        FormulaFactory ffPlain = new FormulaFactory();

        if (lockedCircuit.getInputNames().size() != plainCircuit.getInputNames().size()) {
            Protocol.printErrorMessage("Circuits have a different number od inputs.");
            return false;
        }

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
     * @param locked Instance of locked LogicCircuit.
     * @param plain Instance of plain (activated) LogicCircuit.
     * @param rounds The number of test rounds.
     * @param debugMode Able or disable logs.
     * @return true, if all rounds of test have passed, false otherwise.
     */
    public static boolean validateCircuitLock(LogicCircuit locked, LogicCircuit plain, int rounds, boolean debugMode) {

        if (locked == null || plain == null) {
            Protocol.printWarningMessage("Missing instance(s) of LogicCircuit. " +
                    "File integrity validation will be skipped.\n");
            return true;
        }


        System.out.print("INFO: Testing file lock integrity in " + rounds + " rounds ..." + (debugMode ? "\n" : " "));
        for (int i = 0; i < rounds; i++) {
            if (debugMode)
                Protocol.printSection("Iteration " + (i + 1));
            if (!checkLockedFileIntegrity(locked, plain, debugMode)) {
                System.out.println("FAILED");
                Protocol.printSection("");
                Protocol.printErrorMessage("Validation failed: Incorrect lock.");
                return false;
            }
            if (debugMode)
                Protocol.printSection("");
        }
        System.out.println((debugMode ? "Circuits have same functionality - OK\n" : "OK\n"));
        return true;
    }
}
