package main.utilities;

import main.attacker.sat.FormulaFactoryWrapped;
import main.circuit.LogicCircuit;
import main.circuit.components.Operators;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CircuitUtilities {

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

            if (firstLiteral.phase() != secondLiteral.phase())
                return false;
        }
        return true;
    }

    public static int ArrayDifference(Collection<Literal> arr1, Collection<Literal> arr2) {
        int diffCount = 0;
        Iterator<Literal> firstIt = arr1.iterator();
        Iterator<Literal> secondIt = arr2.iterator();
        while (firstIt.hasNext()) {
            Literal firstLiteral = firstIt.next();
            Literal secondLiteral = secondIt.next();

            if (firstLiteral.phase() != secondLiteral.phase())
                diffCount++;
        }
        return diffCount;
    }

    public static int hammingWeightOfVector(Collection<Literal> literals) {
        int count = 0;

        for (Literal literal : literals) {
            if (literal.phase())
                count++;
        }

        return count;
    }

    public static Formula duplicateWithSameInput(LogicCircuit circuit) {
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

    public static Formula createDifferentOutputs(LogicCircuit circuit) {
        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();

        // adding Y_1 != Y_2
        // spravi sa xor z vystupnych premennych (napr. O22_A XOR O22_B)
        Collection<Formula> outputElements = new ArrayList<>();
        for (String output : circuit.getOutputNames()) {
            outputElements.add(Operators.xor(ff.variable(output + "_A"), ff.variable(output + "_B")));
        }

        // aspon jedna vystupna rovnica musi platit (cize O_A != O_B)
        return ff.or(outputElements).cnf();
    }

    /**
     * Returns a CNF formed Formula of 2 same-sized vectors whose Hamming weights is one.
     * It means that their variables have the same logical value, except *one* bit at specific index.
     */
    public static Formula differenceAtIndex(int index, List<Variable> first, List<Variable> second) {

        if (first.size() != second.size()) {
            System.err.println("Error while computing Hamming weights - vectors must have same sizes.");
            return null;
        }

        if (index >= first.size()) {
            System.err.println("Error while computing Hamming weights - index " + index + " overflowing vector's " +
                    "size (" + first.size() + ").");
            return null;
        }

        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
        Formula hamming = ff.and();

        for (int i = 0; i < first.size(); i++) {
            if (i == index)
                hamming = ff.and(hamming, Operators.xor(first.get(i), second.get(i)));
            else
                hamming = ff.and(hamming, Operators.xnor(first.get(i), second.get(i)));
        }

        return hamming;
    }
}
