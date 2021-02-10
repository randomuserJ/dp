package main.attacker.sat;

import main.attacker.CircuitAttacker;
import main.circuit.AbstractLogicCircuit;
import main.circuit.LogicCircuit;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.File;
import java.security.SecureRandom;
import java.util.*;

public class SatAttackWrapped {
    private final LogicCircuit lockedLC;
    private final FormulaFactory ff;
    private Assignment realKey;
    private Assignment estimatedKey;

    /**
     * Constructor with 2 arguments. Correct key should be specified by user.
     * @param lockedCircuit Instance of locked logic circuit.
     * @param realKey Assignment of correct key.
     */
    public SatAttackWrapped(LogicCircuit lockedCircuit, Assignment realKey) {
        this.ff = FormulaFactoryWrapped.getFormulaFactory();
        this.lockedLC = lockedCircuit;
        this.realKey = realKey;
        this.estimatedKey = new Assignment();
    }

    /**
     * One argument constructor. Correct key will be parsed from integer array of real key from circuit.
     * @param lockedCircuit Instance of locked logic circuit.
     */
    public SatAttackWrapped(LogicCircuit lockedCircuit) {
        this.ff = FormulaFactoryWrapped.getFormulaFactory();
        this.lockedLC = lockedCircuit;

        Assignment correctKey = new Assignment();
        for (int i = 0; i < lockedCircuit.getCorrectKey().length; i++)
            correctKey.addLiteral(this.ff.literal("k" + i, lockedCircuit.getCorrectKey()[i] == 1));

        this.realKey = correctKey;
        this.estimatedKey = new Assignment();
    }

    public void performSATAttack(boolean debugMode, boolean printKeyAssignment) throws IllegalStateException {

        SatSolverWrapped satSolver = new SatSolverWrapped();
        SatSolverWrapped keySolver = new SatSolverWrapped();

        if (this.lockedLC.getAntisatKey().length != 0)
            throw new IllegalStateException("SAT: Attacking file locked with AntiSAT is not possible at this time (not implemented).");

        Formula CNF = this.lockedLC.getCNF();
        // vytvoria sa mnoziny (filtre) vstupov a vystupov (bez dodatkov _A, _B)
        Collection<Variable> inputVariablesFilter = this.lockedLC.getInputVariables(ff);
        Collection<Variable> outputVariablesFilter = this.lockedLC.getOutputVariables(ff);

        Assignment distinguishingInput;
        Assignment distinguishingOutput;

        int iteration = 1;
        Tristate result;

        // variables used as key input variables in first half of F1 (CNF: Variable a -> a+"_A")
        Collection<Variable> keyInputVariable_A = new ArrayList<>();

        // filter for displaying keys after SAT solver produces solution
        for (String keyInputName : this.lockedLC.getKeyInputNames()) {
            keyInputVariable_A.add(ff.variable(keyInputName + "_A"));
        }

        // vytvorenie rovnice F_i ako spojenie CNF_A & CNF_B & Y_A != Y_B
        // a prida sa do solvera pripravena na vyhodnotenie
        Formula F_i = CircuitAttacker.duplicateCircuitWithSameInput(this.lockedLC);
        Formula notEqualoutputs = CircuitAttacker.differentOutputs(this.lockedLC);
        satSolver.addFormula(ff.and(F_i, notEqualoutputs));

        // nasladne sa bude volat solver, pokym bude existovat riesenie (SATisfiable)
        while ((result = satSolver.solve()) == Tristate.TRUE) {
            if (debugMode)
                System.err.println("Starting round \t" + iteration);

            // distinguishing input ziskame vdaka najdenemu rieseniu sat solvera
            // metoda evaluate vrati hodnoty na vystupe pre distinguishing vstup,
            //  pokial existuje riesenie CNF obvodu na zaklade realneho kluca
            distinguishingInput = satSolver.getModel(inputVariablesFilter);
            try {
                distinguishingOutput = this.lockedLC.evaluate(distinguishingInput.literals(), realKey.literals(), outputVariablesFilter);
            } catch (IllegalStateException | IllegalArgumentException e) {
                System.err.println("SAT attack exception - " + e.getMessage());
                return;
            }

            if (debugMode) {
                System.out.println("X_d_" + iteration + ": " + distinguishingInput);
                System.out.println("Y_d_" + iteration + ": " + distinguishingOutput);
            }

            // DIO reprezentuje konkretne priradenie hodnot pre jednotlive literali DIO paru
            Formula DIO = ff.and(distinguishingInput.formula(ff), distinguishingOutput.formula(ff));

            // aby sme mohli vytvorit filter, cez ktory prejdu iba kluce vyhovujuce DIO predpokladu
            // potrebujeme vytvorit "samostatne" obvody v ktorych budu vystupovat unikatne premenne
            // *_fAi, *_fBi a pomedzi ne bude zapleteny skutocny kluc podla F_i
            // (teda K_A = [k0_A, .., kn_A], K_B = [k0_B, .., kn_B]
            Substitution filterSubstitution_A = new Substitution();
            Substitution filterSubstitution_B = new Substitution();
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
            Formula K1 = ff.and(CNF.substitute(filterSubstitution_A), DIO.substitute(DIOSub_A));
            Formula K2 = ff.and(CNF.substitute(filterSubstitution_B), DIO.substitute(DIOSub_B));

            F_i = ff.and(F_i, K1, K2);

            iteration++;
            satSolver.reset();
            satSolver.addFormula(ff.and(F_i, notEqualoutputs));

            if (debugMode)
                System.err.println("Ending round \t" + (iteration - 1));
        }

        System.err.println("Attack ended after " + (iteration - 1) + " round(s) due to result = " + result);

        keySolver.addFormula(F_i);
        keySolver.solve();
        Assignment keyAssignment = keySolver.getModel(keyInputVariable_A);

        if (printKeyAssignment) {
            System.out.println("\nKey solved:");

            for (Literal l : keyAssignment.literals())
                System.out.println(l.name() + " = " + l.phase());
        }

        this.estimatedKey = keyAssignment;
    }

    public void printKeyStats() {
        List<Literal> parsedEstimatedKey = new ArrayList<>();
        for (Literal literal : this.estimatedKey.literals()) {
            parsedEstimatedKey.add(ff.literal(literal.name().split("_")[0], literal.phase()));
        }
        Collections.sort(parsedEstimatedKey);
        System.out.println("Estimated key: \t\t" + parsedEstimatedKey);
        System.out.println("Inserted real key: \t" + this.realKey.literals());
    }

    public Assignment getEstimatedKey() {
        return estimatedKey;
    }

/*
    public static void main(String[] args) throws Exception {

        //uzamknuty obvod so spravnym klucom nema rovnake vystupy ako obvod bez klucov
        //z janikovskeho DP

        //3.4 ma celu nahovno kapitolu

        String rootPath = System.getProperty("user.dir") + File.separator;
        String lockedPath = "locked" + File.separator + "nochain" + File.separator;

//		File lockedLC = new File("..\\locked\\rand\\8_c432.bench");
        File circuitPath = new File(rootPath + lockedPath + "1_c17.bench");

        Assignment realKey = new Assignment();
        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();

        String key = "0";
        int iab = 0;
        for (char c : key.toCharArray()) {
            realKey.addLiteral(ff.literal("k" + (iab++), "1".equals(Character.toString(c))));
        }
        System.out.println("Inserted real key: " + realKey.literals() + " for eval(X) purpose");

        LogicCircuit lockedCircuit = AbstractLogicCircuit.getCircuitInstance(circuitPath);

        System.out.println(lockedCircuit.getCNF());

        SatAttackWrapped saw = new SatAttackWrapped(lockedCircuit, realKey);

        System.out.println("=========== DECRYPTING ===========");

        saw.performSATAttack(true, true);
        saw.printKeyStats();
        Assignment decryptedKey = saw.getEstimatedKey();

        Assignment parsedDecryptedKey = new Assignment();

        for (Literal l : decryptedKey.literals()) {
            parsedDecryptedKey.addLiteral(ff.literal(l.name().split("_")[0], l.phase()));
        }


        int MAX_TEST = 10;
        for (int u = 0; u < 10; u++) {
            System.out.print("=========== TESTING ===========");
        }
        System.out.println("\nTesting on " + MAX_TEST + " random tests");

        Collection<Variable> outputs = new HashSet<Variable>();
        for (String s : lockedCircuit.getOutputNames()) {
            outputs.add(ff.variable(s));
        }

        Collection<Literal> inputLiterals = new HashSet<Literal>();

        SecureRandom sr = new SecureRandom();
        int notC = 0;
        int C = 0;
        for (int i = 0; i < MAX_TEST; i++) {
            for (String s : lockedCircuit.getInputNames()) {
                inputLiterals.add(ff.literal(s, sr.nextInt(2) == 0));
            }
            if (!lockedCircuit.evaluate(inputLiterals, realKey.literals(), outputs).equals(lockedCircuit.evaluate(inputLiterals, parsedDecryptedKey.literals(), outputs))) {
                System.out.println("\n\nNot correct key");
                notC++;
                System.out.println("For inputs: " + inputLiterals);
                System.out.println("\t(real key): " + realKey.literals());
                System.out.println("\t(dec. key): " + parsedDecryptedKey.literals());
                System.out.println("Y: (dec. key): " + lockedCircuit.evaluate(inputLiterals, decryptedKey.literals(), outputs));
                System.out.println("Y: (real key): " + lockedCircuit.evaluate(inputLiterals, realKey.literals(), outputs));
            } else {
                C++;
            }
            inputLiterals.clear();
        }
        System.out.println("Correct key for: " + C + ", not correct key for: " + notC + " of " + MAX_TEST + " tests");
        if (notC == 0) {
            System.out.println("Key has passed all tests.");
        } else {
            System.out.println("Incorrect key was decrypted.");
        }
    }
*/

}
