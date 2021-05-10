package main.attacker.sat;

import main.helpers.FormulaFactoryWrapper;
import main.circuit.LogicCircuit;
import main.circuit.utilities.CircuitUtilities;
import main.helpers.utilities.Protocol;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;

public class SatAttackWrapper {
    private final LogicCircuit lockedLC;
    private final FormulaFactory ff;
    private final Assignment realKey;
    private Assignment estimatedKey;

    /**
     * Constructor with arguments. Correct key should be specified by user.
     * @param lockedCircuit Instance of locked logic circuit.
     * @param realKey Assignment of correct key.
     */
    public SatAttackWrapper(LogicCircuit lockedCircuit, Assignment realKey) {
        this.ff = FormulaFactoryWrapper.getFormulaFactory();
        this.lockedLC = lockedCircuit;
        this.realKey = realKey;
        this.estimatedKey = new Assignment();
    }

    /**
     * Standard constructor with single argument. Correct key will be parsed
     * from the property of LogicCircuit.
     * @param lockedCircuit Instance of locked logic circuit.
     */
    public SatAttackWrapper(LogicCircuit lockedCircuit) {
        this.ff = FormulaFactoryWrapper.getFormulaFactory();
        this.lockedLC = lockedCircuit;

        Assignment correctKey = new Assignment();
        for (int i = 0; i < lockedCircuit.getCorrectKey().length; i++)
            correctKey.addLiteral(this.ff.literal("k" + i, lockedCircuit.getCorrectKey()[i] == 1));

        this.realKey = correctKey;
        this.estimatedKey = new Assignment();
    }

    /**
     * Performs SAT attack and prints only estimated key.
     * @param debugMode True for detail information. Intended for development purpose.
     */
    public void performSATAttack(boolean debugMode) throws IllegalStateException, IllegalArgumentException {

        if (this.lockedLC.getAntisatKey().length != 0)
            throw new IllegalStateException("Attacking file locked with AntiSAT is not possible (not implemented).");

        if (this.lockedLC.getCorrectKey().length == 0)
            throw new IllegalStateException("No key for attack. Logic circuit seems to be unlocked.");

        Protocol.printInfoMessage("Performing SAT attack on circuit " + this.lockedLC.getName() + ".");
        Protocol.printSection("SAT Attack");

        if (debugMode) {
            performSATAttackWithDetails();
            return;
        }

        SatSolverWrapper satSolver = new SatSolverWrapper();
        SatSolverWrapper keySolver = new SatSolverWrapper();

        Formula CNF = this.lockedLC.getCNF();

        int iteration = 1;

        Collection<Variable> keyInputVariable_A = new ArrayList<>();

        for (String keyInputName : this.lockedLC.getKeyInputNames()) {
            keyInputVariable_A.add(ff.variable(keyInputName + "_A"));
        }

        Formula F_i = CircuitUtilities.distinctCircuitsWithSameInput(this.lockedLC);
        Formula distinctOutputs = CircuitUtilities.createDifferentOutputs(this.lockedLC);

        satSolver.addFormula(ff.and(F_i, distinctOutputs));

        while (satSolver.solve() == Tristate.TRUE) {

            F_i = createCurrentMainFormula(CNF, satSolver, F_i, iteration);
            ff.clear();

            iteration++;
            satSolver.reset();
            satSolver.addFormula(ff.and(F_i, distinctOutputs));
            ff.clear();
        }

        keySolver.addFormula(F_i);
        keySolver.solve();
        Assignment keyAssignment = keySolver.getModel(keyInputVariable_A);

        System.out.println("\nKey solved:");

        for (Literal l : keyAssignment.literals())
            System.out.println(l.name() + " = " + l.phase());

        this.estimatedKey = keyAssignment;
    }

    /**
     * Creates a main formula F_i for current SAT attack iteration i. Formula is created as a conjunction
     * of previous formula F_{i-1} and two separate version of attacked logic circuit.
     * @param CNF CNF formula of logic circuit
     * @param satSolver Instance of a SAT solver, which is currently used in SAT attack
     * @param previousF_i Previous form of the main formula
     * @param iteration Number of current SAT attack iteration
     * @return Main formula for current SAT attack iteration
     */
    private Formula createCurrentMainFormula(Formula CNF, SatSolverWrapper satSolver,
                                             Formula previousF_i, int iteration) {

        Formula DIO = computeDIOFormula(satSolver);
        ff.clear();

        ArrayList<Substitution> substitutions = createSubstitution(CNF, iteration);
        Substitution circuitSubstitution_A = substitutions.get(0);
        Substitution circuitSubstitution_B = substitutions.get(1);

        Substitution DIOSub_A = new Substitution();
        Substitution DIOSub_B = new Substitution();
        for (Literal l : DIO.literals()) {
            DIOSub_A.addMapping(l.variable(), ff.variable(l.name() + "_fA" + iteration));
            DIOSub_B.addMapping(l.variable(), ff.variable(l.name() + "_fB" + iteration));
        }

        Formula K1 = ff.and(CNF.substitute(circuitSubstitution_A), DIO.substitute(DIOSub_A));
        Formula K2 = ff.and(CNF.substitute(circuitSubstitution_B), DIO.substitute(DIOSub_B));
        ff.clear();

        return ff.and(previousF_i, K1, K2);
    }

    /**
     * Computes the Distinguishing Input-Output formula for attack iteration.
     * @param satSolver Instance of a SAT solver, which is currently used in SAT attack
     * @return DIO formula for current SAT attack iteration
     */
    private Formula computeDIOFormula(SatSolverWrapper satSolver) {
        Collection<Variable> inputVariablesFilter = this.lockedLC.getInputVariables(ff);
        Collection<Variable> outputVariablesFilter = this.lockedLC.getOutputVariables(ff);

        Assignment distinguishingInput = satSolver.getModel(inputVariablesFilter);
        Assignment distinguishingOutput = this.lockedLC.evaluate(distinguishingInput.literals(),
                realKey.literals(), outputVariablesFilter);

        return ff.and(distinguishingInput.formula(ff), distinguishingOutput.formula(ff));
    }

    /**
     * Creates two substitutions for logic circuit variables. Each non-key variable name is extended
     * by "_A" for the first and "_B" for the second substitution. Key variables are extended by "_fA"
     * and "_fB". Combining this two substitution will give us the two independent logic circuits.
     * @param CNF CNF formula of logic circuit
     * @param iteration Number of current SAT attack iteration
     * @return Two substitutions wrapped in ArrayList
     */
    private ArrayList<Substitution> createSubstitution(Formula CNF, int iteration) {
        Substitution filterSubstitution_A = new Substitution();
        Substitution filterSubstitution_B = new Substitution();
        ArrayList<Substitution> combinedFilter = new ArrayList<>();

        for (Variable var : CNF.variables()) {
            if (!this.lockedLC.isKeyVariable(var)) {
                filterSubstitution_A.addMapping(var, ff.variable(var.name() + "_fA" + iteration));
                filterSubstitution_B.addMapping(var, ff.variable(var.name() + "_fB" + iteration));
            }
            else {
                filterSubstitution_A.addMapping(var, ff.variable(var.name() + "_A"));
                filterSubstitution_B.addMapping(var, ff.variable(var.name() + "_B"));
            }
        }
        combinedFilter.add(filterSubstitution_A);
        combinedFilter.add(filterSubstitution_B);

        return combinedFilter;
    }

    /**
     * Prints the comparison between the estimated and the correct key along with success rate.
     */
    public void printKeyStats() {
        List<Literal> parsedEstimatedKey = new ArrayList<>();
        for (Literal literal : this.estimatedKey.literals()) {
            parsedEstimatedKey.add(ff.literal(literal.name().split("_")[0], literal.phase()));
        }
        Collections.sort(parsedEstimatedKey);
        System.out.println("Estimated key: \t\t" + parsedEstimatedKey);
        System.out.println("Real key inserted: \t" + this.realKey.literals());

        int keyLength = parsedEstimatedKey.size();
        int differences = CircuitUtilities.arrayDifference(parsedEstimatedKey, this.realKey.literals());
        System.out.printf("Success rate %d / %d = [%.03f %%]%n",
                keyLength - differences, keyLength, ((double)(keyLength - differences)*100) / keyLength);
    }

    /**
     * Performs SAT attack and prints all the information about the process in each step. The code in
     * this method is explained in comments and can be used for learning and understanding purposes
     * for Slovak readers.
     */
    private void performSATAttackWithDetails() {
        SatSolverWrapper satSolver = new SatSolverWrapper();
        SatSolverWrapper keySolver = new SatSolverWrapper();

        Formula CNF = this.lockedLC.getCNF();
        Collection<Variable> inputVariablesFilter = this.lockedLC.getInputVariables(ff);
        Collection<Variable> outputVariablesFilter = this.lockedLC.getOutputVariables(ff);

        Assignment distinguishingInput;
        Assignment distinguishingOutput;

        int iteration = 1;

        // variables used as key input variables in first half of F1 (CNF: Variable a -> a+"_A")
        Collection<Variable> keyInputVariable_A = new ArrayList<>();

        // filter for displaying keys after SAT solver produces solution
        for (String keyInputName : this.lockedLC.getKeyInputNames()) {
            keyInputVariable_A.add(ff.variable(keyInputName + "_A"));
        }

        // vytvorenie rovnice F_i ako spojenie CNF_A & CNF_B & Y_A != Y_B
        // a prida sa do solvera pripravena na vyhodnotenie
        Formula F_i = CircuitUtilities.distinctCircuitsWithSameInput(this.lockedLC);
        Formula distinctOutputs = CircuitUtilities.createDifferentOutputs(this.lockedLC);

        satSolver.addFormula(ff.and(F_i, distinctOutputs));

        // nasladne sa bude volat solver, pokym bude existovat riesenie (SATisfiable)
        while (satSolver.solve() == Tristate.TRUE) {
            Protocol.printSection("Starting round " + iteration);

            // distinguishing input ziskame vdaka najdenemu rieseniu sat solvera
            // metoda evaluate vrati hodnoty na vystupe pre distinguishing vstup,
            // pokial existuje riesenie CNF obvodu na zaklade realneho kluca
            distinguishingInput = satSolver.getModel(inputVariablesFilter);
            distinguishingOutput = this.lockedLC.evaluate(distinguishingInput.literals(),
                    realKey.literals(), outputVariablesFilter);

            System.out.println("X_d_" + iteration + ": " + distinguishingInput);
            System.out.println("Y_d_" + iteration + ": " + distinguishingOutput);

            // DIO reprezentuje konkretne priradenie hodnot pre jednotlive literali DIO paru
            Formula DIO = ff.and(distinguishingInput.formula(ff), distinguishingOutput.formula(ff));

            // aby sme mohli vytvorit filter, cez ktory prejdu iba kluce vyhovujuce DIO predpokladu
            // potrebujeme vytvorit "samostatne" obvody v ktorych budu vystupovat unikatne premenne
            // *_fAi, *_fBi a pomedzi ne bude zapleteny skutocny kluc podla F_i
            // (teda K_A = [k0_A, .., kn_A], K_B = [k0_B, .., kn_B]
            ArrayList<Substitution> substitutions = createSubstitution(CNF, iteration);
            Substitution circuitSubstitution_A = substitutions.get(0);
            Substitution circuitSubstitution_B = substitutions.get(1);

            // vstupne a vystupne premenne noveho filtra budu fixovane podla aktualneho DIO paru
            // cize kluce, ktore bude adeptom pre F_i, musia prejst cez vsetky doterajsie filtre
            Substitution DIOSub_A = new Substitution();
            Substitution DIOSub_B = new Substitution();
            for (Literal l : DIO.literals()) {
                DIOSub_A.addMapping(l.variable(), ff.variable(l.name() + "_fA" + iteration));
                DIOSub_B.addMapping(l.variable(), ff.variable(l.name() + "_fB" + iteration));
            }

            // formuly K1, K2 predstavuju uz spominane filtre pre potencialne hodnoty klucov
            // K_A = [k0_A, .., kn_A], K_B = [k0_B, .., kn_B]
            // kazde nove riesnie, ktore solver najde musi obsahovat klucove bity, ktore presli cez filtre
            // vdaka tomu eliminujeme vsetky zle kluce a ponechame iba tie spravne
            Formula K1 = ff.and(CNF.substitute(circuitSubstitution_A), DIO.substitute(DIOSub_A));
            Formula K2 = ff.and(CNF.substitute(circuitSubstitution_B), DIO.substitute(DIOSub_B));

            F_i = ff.and(F_i, K1, K2);

            iteration++;
            satSolver.reset();
            satSolver.addFormula(ff.and(F_i, distinctOutputs));

            Protocol.printSection("Ending round \t" + (iteration - 1));
        }

        Protocol.printInfoMessage("Attack ended after " + (iteration - 1) + " round(s).");

        keySolver.addFormula(F_i);
        keySolver.solve();
        Assignment keyAssignment = keySolver.getModel(keyInputVariable_A);

        System.out.println("\nKey solved:");

        for (Literal l : keyAssignment.literals())
            System.out.println(l.name() + " = " + l.phase());

        this.estimatedKey = keyAssignment;
    }
}
