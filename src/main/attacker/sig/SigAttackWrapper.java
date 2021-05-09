package main.attacker.sig;

import main.global_utilities.FormulaFactoryWrapper;
import main.attacker.sat.SatSolverWrapper;
import main.circuit.LogicCircuit;
import main.circuit.utilities.CircuitUtilities;
import main.global_utilities.ProgressBar;
import main.global_utilities.Protocol;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SigAttackWrapper {

    private final LogicCircuit lockedCircuit;

    private final List<Variable> inputVariables;
    private final List<Variable> keyInputVariables_A;
    private final List<Variable> keyInputVariables_B;
    private final List<Variable> outputVariables_A;
    private final List<Variable> outputVariables_B;
    private final Map<String, String> relatedInputs;

    public SigAttackWrapper(LogicCircuit lockedCircuit) {
        if (lockedCircuit.getCorrectKey().length != 0)
            throw new IllegalStateException("Attacking file locked in basic way is not possible at this time (not implemented).");

        this.lockedCircuit = lockedCircuit;
        this.inputVariables = new ArrayList<>();
        this.keyInputVariables_A = new ArrayList<>();
        this.keyInputVariables_B = new ArrayList<>();
        this.outputVariables_A = new ArrayList<>();
        this.outputVariables_B = new ArrayList<>();
        this.relatedInputs = new TreeMap<>();
    }

    /**
     * Performs Sig attack and prints all estimated pairs of AntiSAT key bit with corresponding input bit.
     * @param debugMode True for detail information. Intended for development purpose.
     */
    public void performSigAttack(boolean printStatistics, boolean debugMode) {

        if (this.lockedCircuit.getEvaluationCircuit() == null) {
            Protocol.printWarningMessage("Evaluating circuit is missing. " +
                "Maybe try to insert circuit by createEvaluationCircuit() method.");
            throw new IllegalStateException("Evaluation circuit is required for Sig attack.");
        }

        Protocol.printInfoMessage("Performing SigAttack on circuit " + this.lockedCircuit.getName() + ".");
        Protocol.printSection("SigAttack");

        if (debugMode) {
            performSigAttackWithDetails();
            return;
        }

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        SatSolverWrapper satSolver = new SatSolverWrapper();

        createFilters();
        ProgressBar bar = new ProgressBar(this.keyInputVariables_A.size(), "SigAttack", true);

        Formula distinctCircuits = CircuitUtilities.distinctCircuitsWithSameInput(this.lockedCircuit);
        Formula notEqualOutputs = CircuitUtilities.createDifferentOutputs(this.lockedCircuit);

        for (int k = 0; k < this.keyInputVariables_A.size(); k++) {
            bar.updateBar(k);

            Formula hammingKeys = CircuitUtilities.differenceAtIndex(k,
                    this.keyInputVariables_A, this.keyInputVariables_B);

            Formula F = ff.and(distinctCircuits, notEqualOutputs, hammingKeys);
            ff.clear();
            satSolver.reset();
            satSolver.addFormula(F);

            if (satSolver.solve() != Tristate.TRUE)
                throw new IllegalStateException("Formula is not satisfiable.");

            String actualASKey = CircuitUtilities.removeSuffix(this.keyInputVariables_A.get(k)).name();
            performSigAttackIteration(satSolver, actualASKey);
        }

        evaluateSuccess(printStatistics);
    }

    /**
     * Tries to find corresponding input bit to specific key bit.
     * @param satSolver Instance of a SAT solver, which is currently used in Sig attack
     * @param currentASKey String representation of antisat key in specific iteration
     */
    private void performSigAttackIteration(SatSolverWrapper satSolver, String currentASKey) {

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();

        Assignment input = satSolver.getModel(this.inputVariables);
        Assignment key_A = satSolver.getModel(this.keyInputVariables_A);
        Assignment key_B = satSolver.getModel(this.keyInputVariables_B);
        Assignment output_A = satSolver.getModel(outputVariables_A);
        Assignment output_B = satSolver.getModel(outputVariables_B);

        boolean unflippedA = lockedCircuit.evaluateAndCheck(input.literals(), output_A, false);
        boolean unflippedB = lockedCircuit.evaluateAndCheck(input.literals(), output_B, false);

        if (unflippedA == unflippedB)
            return;

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

            Assignment out_A = this.lockedCircuit.evaluate(flippedInput, K1, this.lockedCircuit.getOutputVariables(ff));
            Assignment out_B = this.lockedCircuit.evaluate(flippedInput, K2, this.lockedCircuit.getOutputVariables(ff));

            if (!CircuitUtilities.compareAssignments(out_A, out_B, false))
                this.relatedInputs.put(currentASKey, l.name());
        }
    }


    private void evaluateSuccess(boolean printStats) {
        AtomicInteger successCount = new AtomicInteger();
        this.relatedInputs.forEach(
                (key, value) -> {
                    boolean correctEstimation = this.lockedCircuit.getInputKeyMapping().get(value).getKey().equals(key);
                    if (printStats)
                        System.out.printf("%s - %s - %b\n", key, value, correctEstimation);
                    if (correctEstimation)
                        successCount.getAndIncrement();
                });

        System.out.printf("Success rate %d / %d = [%.03f %%]%n", successCount.get(), this.keyInputVariables_A.size() / 2,
                ((double) successCount.get() / (this.keyInputVariables_A.size() / 2)) * 100);
    }

    /**
     * Fills the private properties of class.
     */
    private void createFilters() {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();

        for (Variable v : this.lockedCircuit.getCNF().variables()) {
            if (this.lockedCircuit.isInputVariable(v)) {
                if (this.lockedCircuit.isOutputVariable(v))
                    Protocol.printWarningMessage("Variable " + v.name() +
                            " is defined as both Input and Output. The SigAttack might fail.");
                this.inputVariables.add(ff.variable(v.name()));
            }

            else if (this.lockedCircuit.isKeyVariable(v)) {
                this.keyInputVariables_A.add(ff.variable(v.name() + "_A"));
                this.keyInputVariables_B.add(ff.variable(v.name() + "_B"));
            }

            else if (this.lockedCircuit.isOutputVariable(v)) {
                this.outputVariables_A.add(ff.variable(v.name() + "_A"));
                this.outputVariables_B.add(ff.variable(v.name() + "_B"));
            }
        }
    }

    /**
     * Performs Sig attack and prints all the information about the process in each step. The code in this method
     * is explained in comments and can be used for learning and understanding purposes for readers.
     */
    public void performSigAttackWithDetails() {

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        SatSolverWrapper satSolver = new SatSolverWrapper();

        createFilters();

         // C(X, K_B, Y_B) && C(X, K_A, Y_A)
        Formula distinctCircuits = CircuitUtilities.distinctCircuitsWithSameInput(lockedCircuit);
        Formula notEqualOutputs = CircuitUtilities.createDifferentOutputs(lockedCircuit);

        /////     sigAttack Iterations     ////

        for (int k = 0; k < keyInputVariables_A.size(); k++) {
            String actualASKey = CircuitUtilities.removeSuffix(keyInputVariables_A.get(k)).name();
            Formula hammingKeys = CircuitUtilities.differenceAtIndex(k, keyInputVariables_A, keyInputVariables_B);

            // C(X, K_1, Y_1) && C(X, K_2, Y_2) && (Y_1 != Y_2) && (W_H(K_1, K_2) = 1)
            Formula F = ff.and(distinctCircuits, notEqualOutputs, hammingKeys);
            satSolver.reset();
            satSolver.addFormula(F);

            if (satSolver.solve() != Tristate.TRUE)
                throw new IllegalStateException("Formula is not satisfiable.");

            Assignment output_A = satSolver.getModel(outputVariables_A);
            Assignment output_B = satSolver.getModel(outputVariables_B);
            Assignment key_A = satSolver.getModel(keyInputVariables_A);
            Assignment key_B = satSolver.getModel(keyInputVariables_B);
            Assignment input = satSolver.getModel(inputVariables);

            // Y1 is different from Y2, because key bit k_i is flipped, which results to H(V') = 1 and H(V) = 0
            // Now we have to find corresponding x_i bit that is in relation with k_i
            // We subsequently flip every bit of X and see, if H(V) = 1 again

            System.out.println("X: " + input);
            System.out.println("K1: " + key_A);
            System.out.println("K2: " + key_B);
            System.out.println("Y1: " + output_A);
            System.out.println("Y2: " + output_B);
            System.out.println("Y*: " + lockedCircuit.getEvaluationCircuit().evaluate(input.literals(),
                    null, lockedCircuit.getEvaluationCircuit().getOutputVariables(ff)));

            boolean unflippedA = lockedCircuit.evaluateAndCheck(input.literals(), output_A, false);
            boolean unflippedB = lockedCircuit.evaluateAndCheck(input.literals(), output_B, false);
            if (unflippedA == unflippedB)
                continue;

            System.out.println("A: " + (unflippedA ? "not flipped" : "flipped"));
            System.out.println("B: " + (unflippedB ? "not flipped" : "flipped"));

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
                Assignment out_A = lockedCircuit.evaluate(flippedInput, K1, lockedCircuit.getOutputVariables(ff));
                Assignment out_B = lockedCircuit.evaluate(flippedInput, K2, lockedCircuit.getOutputVariables(ff));
                System.out.println("K1: " + out_A);
                System.out.println("K2: " + out_B);

                if (!CircuitUtilities.compareAssignments(out_A, out_B, false))
                    System.out.printf("%s - %s - %b\n", actualASKey, l.name(),
                            lockedCircuit.getInputKeyMapping().get(l.name()).getKey().equals(actualASKey));
            }

            Protocol.printSection("");
        }
    }
}
