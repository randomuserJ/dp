package main.attacker;

import main.attacker.sat.FormulaFactoryWrapped;
import main.attacker.sat.SatAttackWrapped;
import main.attacker.sat.SatSolverWrapped;
import main.circuit.LogicCircuit;
import main.circuit.components.Operators;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.*;

public class CircuitAttacker {

    private SPSConfig SPSConfiguration;

    /**
     * User should not create a simple instance of CircuitAttacker. This class is used as a wrapper for calling
     * static methods performing attacks. SPS attack configuration is specified in this constructor.
     */
    private CircuitAttacker(int SPSRounds) {
        this.SPSConfiguration = SPSConfig.createSPSConfig()
                .setRounds(SPSRounds)
                .setKeySet(SPSConfig.KeySet.RANDOM)   // default
                .setDebugMode(false)        // default
                .printResult(true);         // default
    }

    public static void performSATAttack(LogicCircuit locked, boolean debugMode, boolean printKey) {
        SatAttackWrapped attacker = new SatAttackWrapped(locked);
        try {
            attacker.performSATAttack(debugMode, printKey);
            attacker.printKeyStats();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Wrapper for SPS attack method called from main. At this time user cannot specify parameters, such as
     * testing with real / random keys, printed details and so on.
     * @param locked Instance of locked LogicCircuit.
     * @param rounds Number of rounds for SPS statistical testing.
     */
    public static void performSPSAttack(LogicCircuit locked, int rounds) {
        CircuitAttacker attacker = new CircuitAttacker(rounds);
        attacker.SPSConfiguration.performSPSAttack(locked);
    }

    public static void performSigAttack(LogicCircuit locked) {
        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
        SatSolverWrapped satSolver = new SatSolverWrapped();

        if (locked.getCorrectKey().length != 0)
            throw new IllegalStateException("Sig: Attacking file locked in basic way is not possible at this time (not implemented).");


        // variables used as key input variables in first half of F1 (CNF: Variable a -> a+"_A")
        List<Variable> keyInputVariable_A = new ArrayList<>();
        List<Variable> keyInputVariable_B = new ArrayList<>();
        List<Variable> outputVariable_A = new ArrayList<>();
        List<Variable> outputVariable_B = new ArrayList<>();
        List<Variable> inputVariables = new ArrayList<>();

        //preparation for substitutions of first half of CNF in sat attack
        Substitution substitution_A = new Substitution();
        Substitution substitution_B = new Substitution();

        // vsetky premenne, ktore niesu vstupne sa nahradia za *_A (napr. G10=G10_A, O22=O22_A)
        // kluc sa takisto nahradi za k*_A a prida sa do pola
        // so vstupnymi premennymi sa nerobi nic
        for (Variable v : locked.getCNF().variables()) {
            if (!locked.isInputVariable(v)) {
                substitution_A.addMapping(v, ff.variable(v.name() + "_A"));
                substitution_B.addMapping(v, ff.variable(v.name() + "_B"));
                if (locked.isKeyVariable(v)) {
                    keyInputVariable_A.add(ff.variable(v.name() + "_A"));
                    keyInputVariable_B.add(ff.variable(v.name() + "_B"));
                }
                if (locked.isOutputVariable(v)) {
                    outputVariable_A.add(ff.variable(v.name() + "_A"));
                    outputVariable_B.add(ff.variable(v.name() + "_B"));
                }
            }
            else {
                inputVariables.add(ff.variable(v.name()));
            }
        }


        // vytvoria sa 2 nove formuly, rovnake ako povodna, ale s nahradenymi premennymi (_A, _B)
        // v CNF_A su vsetky nevstupne premenne nahradene za unikatny ikvivaletna (*_A)
        // F pre dane kolo bude kombinaciou CNF_A & CNF_B, cize zdvojnasobenie povodne CNF rovnice,
        // akurat s odlisnymi nevstupnymi premennymi (vstupne sa neduplikuju)
        // spojenim oboch polovicnych formul ziskame (snad) splnitelnu rovnicu,
        // ktora pre 2 rovnake vstupy a ODLISNE kluce K_A = [k0_A, .., kn_A], K_B = [k0_B, .., kn_B]
        // vyprodukuje odlisne vystupy (O_A, O_B)
        // zatial teda mame CNF_A & CNF_B
        // este musime pridat klauzulu, ktore tieto vystupy naozaj olisi (teda O_A XOR O_B)
        Formula CNF_A = locked.getCNF().substitute(substitution_A);         // C(X, K_A, Y_A)
        Formula CNF_B = locked.getCNF().substitute(substitution_B);         // C(X, K_B, Y_B)

        // adding Y_1 != Y_2
        // spravi sa xor z vystupnych premennych (napr. O22_A XOR O22_B)
        Collection<Formula> outputElements = new ArrayList<>();
        for (String output : locked.getOutputNames()) {
            outputElements.add(Operators.xor(ff.variable(output + "_A"), ff.variable(output + "_B")));
        }

        Formula hammingKeys = ff.or();

        for (int i = 0; i < keyInputVariable_A.size(); i++) {
            Formula fi = ff.and();
            for (int j = 0; j < keyInputVariable_A.size(); j++) {
                if (i == j)
                    fi = ff.and(fi, Operators.xor(keyInputVariable_A.get(j), keyInputVariable_B.get(j)));
                else
                    fi = ff.and(fi, Operators.xnor(keyInputVariable_A.get(j), keyInputVariable_B.get(j)));
            }

            hammingKeys = ff.or(hammingKeys, fi);

        }

        // zrejme, ze aspon jedna vystupna rovnica musi platit (cize O_A != O_B)
        Formula notEqualoutputs = ff.or(outputElements).cnf();

        // vytvorenie rovnice F_i ako spojenie CNF_A & CNF_B & Y_A != Y_B
        // a prida sa do solvera pripravena na vyhodnotenie
        Formula F = ff.and(CNF_A, CNF_B, notEqualoutputs, hammingKeys);

        satSolver.addFormula(F);

        if (satSolver.solve() == Tristate.TRUE) {

            Assignment output_A = satSolver.getModel(outputVariable_B);
            Assignment output_B = satSolver.getModel(outputVariable_A);
            Assignment key_A = satSolver.getModel(keyInputVariable_A);
            Assignment key_B = satSolver.getModel(keyInputVariable_B);
            Assignment input = satSolver.getModel(inputVariables);

            System.out.println("X: " + input);
            System.out.println("K1: " + key_A);
            System.out.println("K2: " + key_B);
            System.out.println("Y1: " + output_A);
            System.out.println("Y2: " + output_B);
        } else {
            System.err.println("Nejde");
        }


    }

    public static Formula duplicateCircuitWithSameInput(LogicCircuit circuit) {
        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
        Formula CNF = circuit.getCNF();

        //preparation for substitutions of first half of CNF in sat attack
        Substitution substitution_A = new Substitution();
        Substitution substitution_B = new Substitution();

        // vsetky premenne, ktore niesu vstupne sa nahradia za *_A (napr. G10=G10_A, O22=O22_A)
        // kluc sa takisto nahradi za k*_A a prida sa do pola
        // so vstupnymi premennymi sa nerobi nic
        for (Variable v : CNF.variables()) {
            if (!circuit.isInputVariable(v)) {
                substitution_A.addMapping(v, ff.variable(v.name() + "_A"));
                substitution_B.addMapping(v, ff.variable(v.name() + "_B"));
            }
        }


        // vytvoria sa 2 nove formuly, rovnake ako povodna, ale s nahradenymi premennymi (_A, _B)
        // v CNF_A su vsetky nevstupne premenne nahradene za unikatny ikvivaletna (*_A)
        // F pre dane kolo bude kombinaciou CNF_A & CNF_B, cize zdvojnasobenie povodne CNF rovnice,
        // akurat s odlisnymi nevstupnymi premennymi (vstupne sa neduplikuju)
        // spojenim oboch polovicnych formul ziskame (snad) splnitelnu rovnicu,
        // ktora pre 2 rovnake vstupy a ODLISNE kluce K_A = [k0_A, .., kn_A], K_B = [k0_B, .., kn_B]
        // vyprodukuje odlisne vystupy (O_A, O_B)
        // zatial teda mame CNF_A & CNF_B
        // este musime pridat klauzulu, ktore tieto vystupy naozaj olisi (teda O_A XOR O_B)
        Formula CNF_A = CNF.substitute(substitution_A);         // C(X, K_A, Y_A)
        Formula CNF_B = CNF.substitute(substitution_B);         // C(X, K_B, Y_B)

        return ff.and(CNF_A, CNF_B);
    }

    public static Formula differentOutputs(LogicCircuit circuit) {
        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
        //adding Y_1 != Y_2
        // spravi sa xor z vystupnych premennych (napr. O22_A XOR O22_B)
        Collection<Formula> outputElements = new ArrayList<>();
        for (String output : circuit.getOutputNames()) {
            outputElements.add(Operators.xor(ff.variable(output + "_A"), ff.variable(output + "_B")));
        }

        // zrejme, ze aspon jedna vystupna rovnica musi platit (cize O_A != O_B)
        return ff.or(outputElements).cnf();
    }
}
