package main.attacker;

import main.attacker.sat.FormulaFactoryWrapper;
import main.attacker.sat.SatAttackWrapper;
import main.attacker.sat.SatSolverWrapper;
import main.attacker.sps.KeySetForSPS;
import main.attacker.sps.SpsAttackWrapper;
import main.circuit.LogicCircuit;
import main.utilities.CircuitUtilities;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;

public class CircuitAttacker {

    public static void performSATAttack(LogicCircuit locked, boolean debugMode, boolean printKey) {
        SatAttackWrapper attacker = new SatAttackWrapper(locked);
        try {
            attacker.performSATAttack(debugMode, printKey);
            attacker.printKeyStats();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Wrapping method for SPS attack on circuit locked with AntiSAT.
     * @param locked Instance of LogicCircuit locked with AntiSAT.
     * @param rounds Number of rounds for SPS statistical testing.
     * @param realKeys True, if we want to use only correct key while calculating attack statistics.
     */
    public static void performSPSAttack(LogicCircuit locked, int rounds, boolean realKeys) {
        SpsAttackWrapper attacker = new SpsAttackWrapper(rounds, realKeys ? KeySetForSPS.REAL : null, true);
        attacker.setLockedCircuit(locked);
        attacker.performSPSAttack();
    }

    /**
     * Wrapping method for SPS attack on circuit locked with StrongAntiSAT.
     * @param locked Instance of LogicCircuit locked with AntiSAT. Since we don't have a possibility to create
     *               StrongAntiSAT lock, we have to simulate this attack by software.
     * @param rounds Number of rounds for SPS statistical testing.
     * @param realKeys True, if we want to use only correct key while calculating attack statistics.
     */
    public static void performSPSAttackWithSAS(LogicCircuit locked, int rounds, boolean realKeys) {
        SpsAttackWrapper attacker = new SpsAttackWrapper(rounds, realKeys ? KeySetForSPS.REAL : null, true);
        attacker.setLockedCircuit(locked);
        attacker.simulateSASLock();
        attacker.performSPSAttack();
    }

    public static void performSigAttack(LogicCircuit locked, boolean debugMode) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        SatSolverWrapper satSolver = new SatSolverWrapper();

        if (locked.getCorrectKey().length != 0)
            throw new IllegalStateException("Sig: Attacking file locked in basic way is not possible at this time (not implemented).");


        // variables used as key input variables in first half of F1 (CNF: Variable a -> a+"_A")
        List<Variable> keyInputVariables_A = new ArrayList<>();
        List<Variable> keyInputVariables_B = new ArrayList<>();
        List<Variable> outputVariables_A = new ArrayList<>();
        List<Variable> outputVariables_B = new ArrayList<>();
        List<Variable> inputVariables = new ArrayList<>();

        // duplicating circuit with same input
        Substitution substitution_A = new Substitution();
        Substitution substitution_B = new Substitution();

        for (Variable v : locked.getCNF().variables()) {
            if (!locked.isInputVariable(v)) {
                substitution_A.addMapping(v, ff.variable(v.name() + "_A"));
                substitution_B.addMapping(v, ff.variable(v.name() + "_B"));
                if (locked.isKeyVariable(v)) {
                    keyInputVariables_A.add(ff.variable(v.name() + "_A"));
                    keyInputVariables_B.add(ff.variable(v.name() + "_B"));
                }
                if (locked.isOutputVariable(v)) {
                    outputVariables_A.add(ff.variable(v.name() + "_A"));
                    outputVariables_B.add(ff.variable(v.name() + "_B"));
                }
            }
            else {
                inputVariables.add(ff.variable(v.name()));
            }
        }

        Formula CNF_A = locked.getCNF().substitute(substitution_A);         // C(X, K_A, Y_A)
        Formula CNF_B = locked.getCNF().substitute(substitution_B);         // C(X, K_B, Y_B)

        Formula notEqualOutputs = CircuitUtilities.createDifferentOutputs(locked);

        /////     sigAttack Iterations     ////


        for (int k = 0; k < keyInputVariables_A.size(); k++) {
            Formula hammingKeys = CircuitUtilities.differenceAtIndex(k, keyInputVariables_A, keyInputVariables_B);

            // C(X, K_1, Y_1) && C(X, K_2, Y_2) && (Y_1 != Y_2) && (W_H(K_1, K_2) = 1)
            Formula F = ff.and(CNF_A, CNF_B, notEqualOutputs, hammingKeys);
            satSolver.reset();
            satSolver.addFormula(F);

            if (satSolver.solve() != Tristate.TRUE) {
                System.err.println("Nejde");
                return;
            }

            Assignment output_A = satSolver.getModel(outputVariables_A);
            Assignment output_B = satSolver.getModel(outputVariables_B);
            Assignment key_A = satSolver.getModel(keyInputVariables_A);
            Assignment key_B = satSolver.getModel(keyInputVariables_B);
            Assignment input = satSolver.getModel(inputVariables);

            // Y1 is different from Y2, because key bit k_i is flipped, which results to H(V') = 1 and H(V) = 0
            // Now we have to find corresponding x_i bit that is in relation with k_i
            // We subsequently flip every bit of X and see, if H(V) = 1 again

            if (debugMode) {
                System.out.println("X: " + input);
                System.out.println("K1: " + key_A);
                System.out.println("K2: " + key_B);
                System.out.println("Y1: " + output_A);
                System.out.println("Y2: " + output_B);
                System.out.println("Y*: " + locked.evaluationCircuit.evaluate(input.literals(), null, locked.evaluationCircuit.getOutputVariables(ff)));
            }

            boolean unflippedA = locked.evaluateAndCheck(input.literals(), output_A, false);
            boolean unflippedB = locked.evaluateAndCheck(input.literals(), output_B, false);

            if (debugMode) {
                System.out.println("A: " + (unflippedA ? "not flipped" : "flipped"));
                System.out.println("B: " + (unflippedB ? "not flipped" : "flipped"));
            }
            // true - correct (evaluated) output - H(V) = 0 - output is not flipped
            // One of them (false one) is flipped, let's say Y2, so we have to find corresponding input bit.
            // If we flip it, H(V) should became 1, so output of C(X', K_2) should be the same (false) as Y_1

            Collection<Literal> K1 = new ArrayList<>();
            Collection<Literal> K2 = new ArrayList<>();
            for (Literal literal : key_A.literals()) {
                K1.add(ff.literal(literal.name().substring(0, literal.name().length()-2), literal.phase()));
            }
            for (Literal literal : key_B.literals()) {
                K2.add(ff.literal(literal.name().substring(0, literal.name().length()-2), literal.phase()));
            }

            for (Literal l : input.literals()) {
                Collection<Literal> flippedInput = input.literals();
                flippedInput.remove(l);
                flippedInput.add(l.negate());

                // if we find some input, for which K1 =! K2 while input is flipped
                // we probably found Gate corresponded to k_i
                Assignment out_A = locked.evaluate(flippedInput, K1, locked.getOutputVariables(ff));
                Assignment out_B = locked.evaluate(flippedInput, K2, locked.getOutputVariables(ff));
//                System.out.println("K1: " + out_A);
//                System.out.println("K2: " + out_B);

                if (!CircuitUtilities.assignmentComparator(out_A, out_B, false))
                    System.out.printf("AsK %d - %s - %b\n", k, l.name(),
                            locked.getInputKeyMapping().get(l.name()).getKey().equals("ASk"+k));
            }
            if (debugMode)
                System.out.println("----------------------------------\n");
        }

    }
}
