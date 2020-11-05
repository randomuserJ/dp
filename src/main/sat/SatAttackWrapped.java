package main.sat;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SatAttackWrapped {
    private LogicCircuit lockedLC;
    private FormulaFactory ff;
    private Assignment realKey;

    public SatAttackWrapped(LogicCircuit locked, Assignment realKey) {
        this.lockedLC = locked;
        this.realKey = realKey;
        ff = FormulaFactoryWrapped.getFormulaFactory();
    }

    public boolean isInputVariable(Variable var) {
        return this.lockedLC.getInputNames().contains(var.name());
    }
    public boolean isKeyVariable(Variable var) {
        return this.lockedLC.getKeyInputNames().contains(var.name());
    }

    public Assignment performAttack() throws Exception {

        SatSolverWrapped satSolver = new SatSolverWrapped();
        SatSolverWrapped keySolver = new SatSolverWrapped();
        Formula CNF = this.lockedLC.getCNF();

        //variables used as key input variables in first half of F1 (CNF: Variable a -> a+"_A")
        Collection<Variable> keyInputVariable_A = new ArrayList<>();

        //preparation for substitution of first half of CNF in sat attack
        Substitution substitution_A = new Substitution();


        // vsetky premenne, ktore niesu vstupne sa nahradia za *_A (napr. G10=G10_A, O22=O22_A)
        // kluc sa takisto nahradi za k*_A a prida sa do pola
        // so vstupnymi premennymi sa nerobi nic
        for (Variable v : CNF.variables()) {
            if (!isInputVariable(v)) {
                substitution_A.addMapping(v, ff.variable(v.name() + "_A"));
                if (isKeyVariable(v)) {
                    keyInputVariable_A.add(ff.variable(v.name() + "_A"));
                }
            }
        }

        //preparation for substitution of second half of CNF in sat attack
        Substitution substitution_B = new Substitution();

        // opat sa vsetky nevstupne premenne nahradia za *_B
        // tentokrat sa kluc nikde neulozi
        // vstupne premenne ostavaju s rovnakym nazvom
        for (Variable v : CNF.variables()) {
            if (!isInputVariable(v)) {
                substitution_B.addMapping(v, ff.variable(v.name() + "_B"));
            }
        }

        // vytvoria sa 2 nove formuly, rovnake ako povodna, ale s nahradenymi premennymi (_A, _B)
        // F pre dane kolo bude kombinaciou CNF_A & CNF_B
        Formula CNF_A = CNF.substitute(substitution_A);
        Formula CNF_B = CNF.substitute(substitution_B);

        //adding Y_1 != Y_2
        // spravi sa xor z vystupnych premennych (napr. O22_A XOR O22_B)
        Collection<Formula> outputElements = new ArrayList<>();
        for (String output : this.lockedLC.getOutputNames()) {
            outputElements.add(this.xor(ff.variable(output + "_A"), ff.variable(output + "_B")));
        }

        // zrejme, ze aspon jedna vystupna rovnica musi platit (cize O_A != O_B)
        Formula notEqualoutputs = ff.or(outputElements).cnf();

        //formula F_1 being constructed
        // spojenie oboch polovicnych formul
        Formula F_i = ff.and(CNF_A, CNF_B);

        //Collection of primary input variables
        // vytvoria sa mnoziny vstupov a vystupov (bez dodatkov _A, _B)
        Collection<Variable> inputVariablesFilter = new ArrayList<Variable>();
        for (String inputName : this.lockedLC.getInputNames()) {
            inputVariablesFilter.add(ff.variable(inputName));
        }

        //Collection of primary output variables
        Collection<Variable> outputVariablesFilter = new ArrayList<Variable>();
        for (String outputName : this.lockedLC.getOutputNames()) {
            outputVariablesFilter.add(ff.variable(outputName));
        }

        //formulas for concating CNF_i_A, CNF_i_B to F_(i+1)
        Formula F_i_A;
        Formula F_i_B;
        Formula assumptions;

        //asignment X_i^d
        Assignment distinguishingInput;
        //asignment Y_i^d
        Assignment distinguishingOutput;

        Substitution substitution_F_i;
        Substitution substitution_K_a;
        Substitution substitution_K_b;

        Set<Formula> assumptionsSet;

        int iteration = 1;
        Tristate result = null;

        // spoja sa rovnice celkovej CNF (obe polovice) a poziadavka na aspon jeden rozlicnny vystup (O_A != O_B)
        // nasladne sa bude volat solver, pokym bude existovat riesenie (SATisfiable)
        satSolver.addFormula(ff.and(F_i, notEqualoutputs));
        while ((result = satSolver.solve()) == Tristate.TRUE) {
            System.err.println("Starting round \t" + iteration);

            //reseting variables
            assumptionsSet = new HashSet<>();
            substitution_F_i = new Substitution();
            substitution_K_a = new Substitution();
            substitution_K_b = new Substitution();

            //get distinguishing input X_i^d
            distinguishingInput = satSolver.getModel(inputVariablesFilter);

            System.out.println("X_d_" + iteration + ": " + distinguishingInput);


            //get distinguishing output Y_i^d from circuit unlocked by real key
            // metoda evaluate vrati hodnoty na vystupe pre distinguishing vstupu,
            //  pokial existuje riesenie CNF obvodu na zaklade realneho kluca
            distinguishingOutput = this.lockedLC.evaluate(distinguishingInput.literals(), realKey.literals(), outputVariablesFilter);
            System.out.println("Y_d_" + iteration + ": " + distinguishingOutput);

            // vytvoria sa nove klucove premenne k_A, k_B a ulozia sa do prislusnych substitucii
            // vsetky ostatne premenne sa zmenia na *_i (I2_1, O22_1, G10_1, ..) a ulozia sa do F_i substitucie
            for (Variable v : CNF.variables()) {
                if (this.lockedLC.getKeyInputNames().contains(v.name())) {
                    substitution_K_a.addMapping(v, ff.variable(v.name() + "_A"));
                    substitution_K_b.addMapping(v, ff.variable(v.name() + "_B"));
                } else {
                    substitution_F_i.addMapping(v, ff.variable(v.name() + "_" + iteration));
                }
            }


            // vytvoria sa nove formuly v ktorych budu vystupovat klucove premenne _A resp. _B
            // a ostatne (unikatne) premenne s poradovym cislom iteracie 
            F_i_A = CNF.substitute(substitution_K_a).substitute(substitution_F_i);
            F_i_B = CNF.substitute(substitution_K_b).substitute(substitution_F_i);


            // prida hodnoty DIO paru do prazdnej mnoziny assumptions (normalne alebo neg.)
            for (Literal x : distinguishingInput.literals()) {
                if (x.phase()) {
                    assumptionsSet.add(ff.variable(x.name() + "_" + iteration));
                } else {
                    assumptionsSet.add(ff.variable(x.name() + "_" + iteration).negate());
                }
            }

            for (Literal y : distinguishingOutput.literals()) {
                if (y.phase()) {
                    assumptionsSet.add(ff.variable(y.name() + "_" + iteration));
                } else {
                    assumptionsSet.add(ff.variable(y.name() + "_" + iteration).negate());
                }
            }

            // vytvorenie formule zo skutocnych hodnot DIO paru
            // tieto hodnoty budu predstavovat samostatne jednotkove klauzuly
            // cize automaticke priradenie hodnoty T / F v SATe
            assumptions = ff.and(assumptionsSet);

            Formula contradictionCheck1 = ff.and(F_i, assumptions);
            Formula contradictionCheck2 = ff.and(F_i, assumptions);

            //construction of F_i
            F_i = ff.and(F_i, F_i_A, F_i_B, assumptions);

//            System.out.println("Adding formula F_" + iteration + " and notEqualOutput: \n" + ff.and(F_i, notEqualoutputs));

//            contradictionCheck1 = ff.and(contradictionCheck1, F_i);
//            contradictionCheck2 = ff.and(contradictionCheck2, F_i);
//
//
//            satSolver.reset();
//            satSolver.addFormula(ff.and(F_i_A, assumptions));
//            System.out.println("F_A: " + satSolver.solve());
//            System.out.println("K1: " + satSolver.getModel());
//
//            satSolver.reset();
//            satSolver.addFormula(ff.and(F_i_B, assumptions));
//            System.out.println("F_B: " + satSolver.solve());
//            System.out.println("K2: " + satSolver.getModel());
//
//            satSolver.reset();
//            satSolver.addFormula(ff.and(F_i_A, F_i_B, assumptions));
//            System.out.println("F_A and F_B: " + satSolver.solve());
//            System.out.println("K: " + satSolver.getModel());
//
//            satSolver.reset();
//            satSolver.addFormula(contradictionCheck1);
//            System.out.println("Contradiction check1: " + satSolver.solve());
//            System.out.println("K: " + satSolver.getModel());
//
//            satSolver.reset();
//            satSolver.addFormula(contradictionCheck2);
//            System.out.println("Contradiction check2: " + satSolver.solve());
//            System.out.println("K: " + satSolver.getModel());

            iteration++;
            satSolver.reset();
            satSolver.addFormula(ff.and(F_i, notEqualoutputs));

            System.err.println("Ending round \t" + (iteration - 1));
        }


        System.err.println("Atack ended after " + (iteration - 1) + " round(s) due to result = " + result);
        keySolver.addFormula(F_i);
        keySolver.solve();
        Assignment keyAssignment = keySolver.getModel(keyInputVariable_A);

        System.out.println("\nKey solved:");

        /** ZMENA OPROTI STARSEJ KNIZNICI **/
//        for (Literal l : keyAssignment.positiveLiterals()) {
//            System.out.println(l.name() + " = " + l.phase());
//        }
//        for (Literal l : keyAssignment.negativeLiterals()) {
//            System.out.println(l.name() + " = " + l.phase());
//        }

        for (Literal l : keyAssignment.literals()) {
            System.out.println(l.name() + " = " + l.phase());
        }

        return keyAssignment;
    }

    private Formula xor(Formula leftOperand, Formula rightOperand) {
        return ff.and(ff.or(leftOperand, rightOperand), ff.or(leftOperand.negate(), rightOperand.negate()));
    }

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

        LogicCircuit lockedCircuit = LogicCircuit.getLogicCircuitInstance(circuitPath);

        System.out.println(lockedCircuit.getCNF());

        SatAttackWrapped saw = new SatAttackWrapped(lockedCircuit, realKey);

        System.out.println("=========== DECRYPTING ===========");

        Assignment decryptedKey = saw.performAttack();


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
		
		/*
		FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
		Collection<Literal> in = new HashSet<Literal>();
		in.add(ff.literal("A", false));
		in.add(ff.literal("B", false));
		in.add(ff.literal("C", false));

		Collection<Literal> key = new HashSet<Literal>();
		key.add(ff.literal("k1", true));
		key.add(ff.literal("k2", true));
			
		System.out.println(lockLC.evaluate(in, key, null));
		key.clear();
		
		key.add(ff.literal("k1", true));
		key.add(ff.literal("k2", false));
		System.out.println(lockLC.evaluate(in, key, null));
		key.clear();

		key.add(ff.literal("k1", false));
		key.add(ff.literal("k2", true));
		System.out.println(lockLC.evaluate(in, key, null));
		key.clear();

		key.add(ff.literal("k1", false));
		key.add(ff.literal("k2", false));
		System.out.println(lockLC.evaluate(in, key, null));

		*/

        //reggex to match locked and unlocked circuit
        //(I|O){1}([0-9]*)(gat)
        //G$2gat
    }
}
