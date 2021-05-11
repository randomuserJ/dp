package main.circuit.utilities;

import main.circuit.LogicCircuit;
import main.circuit.utilities.custom_comparators.CustomLiteralComparator;
import main.helpers.FormulaFactoryWrapper;
import main.helpers.utilities.Protocol;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.Collectors;

public class CircuitUtilities {

    private final static String AntiSatGatePrefix = "ASgat";
    private static int AntiSatGateId = 0;

    /**
     * Checks whether each two literals in the same position have the same value for both assignments.
     * @param as1 first assignment
     * @param as2 second assignment
     * @param debugMode true for detail information (intended for development purposes)
     * @return true if assignments are logically equal
     */
    public static boolean compareAssignments(Assignment as1, Assignment as2, boolean debugMode) {
        return compareLiteralCollection(as1.literals(), as2.literals(), debugMode);
    }

    /**
     * Checks whether each two literals in the same position have the same value for both collections.
     * @param arr1 first collection of Literals
     * @param arr2 second collection of Literals
     * @return true if collections are logically equal
     */
    public static boolean compareOutputs(Collection<Literal> arr1, Collection<Literal> arr2) {
        CircuitUtilities.replaceAntiSatGate(arr1, arr2);
        return compareLiteralCollection(arr1, arr2, false);
    }

    /**
     * Checks whether each two literals in the same position have the same value for both collections.
     */
    private static boolean compareLiteralCollection(Collection<Literal> as1, Collection<Literal> as2, boolean debugMode) {
        if (as1.size() != as2.size())
            return false;

        List<Literal> firstList = new ArrayList<>(as1);
        List<Literal> secondList = new ArrayList<>(as2);

        firstList.sort(new CustomLiteralComparator());
        secondList.sort(new CustomLiteralComparator());

        Iterator<Literal> firstIt = firstList.iterator();
        Iterator<Literal> secondIt = secondList.iterator();
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

    /**
     * Counts the number of differences between two values of literal at the same position.
     */
    public static int arrayDifference(Collection<Literal> arr1, Collection<Literal> arr2) {
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

    /**
     * Counts the Hamming weight of the collection. Hamming weight of vector
     * equals the number of literals whose boolean value is 1.
     */
    public static int hammingWeightOfVector(Collection<Literal> literals) {
        int count = 0;

        for (Literal literal : literals) {
            if (literal.phase())
                count++;
        }

        return count;
    }

    /**
     * Creates two copies of the specific circuit. Each output and key variable 'var'
     * will be duplicated and renamed to 'var_A' and 'var_B'. Input variables will remain unchanged.
     * The mathematical form of the formula is C(X, K_A, Y_A) & C(X, K_B, Y_B).
     * @return boolean CNF formula of logic circuit that is duplicated and connected
     * by shared inputs
     */
    public static Formula distinctCircuitsWithSameInput(LogicCircuit circuit) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
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

    /**
     * Creates a formula which force the SAT solver to find
     * two different assignments of circuit outputs.
     */
    public static Formula createDifferentOutputs(LogicCircuit circuit) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();

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
     * Returns a CNF formed Formula of 2 same-sized vectors whose Hamming weight is one.
     * Hamming weight of 1 means, that their variables have the same logical value,
     * except for one bit at specific index.
     */
    public static Formula differenceAtIndex(int index, List<Variable> first, List<Variable> second) {

        if (first.size() != second.size()) {
            Protocol.printErrorMessage("Error while computing Hamming weights - vectors must have same sizes.");
            return null;
        }

        if (index >= first.size()) {
            Protocol.printErrorMessage("Error while computing Hamming weights - index " + index + " overflowing vector's " +
                    "size (" + first.size() + ").");
            return null;
        }

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        Formula hamming = ff.and();

        for (int i = 0; i < first.size(); i++) {
            if (i == index)
                hamming = ff.and(hamming, Operators.xor(first.get(i), second.get(i)));
            else
                hamming = ff.and(hamming, Operators.xnor(first.get(i), second.get(i)));
        }

        return hamming;
    }

    /**
     * Finds one literal whose name is different in each collections and changes its name by the name
     * of literal from the second collection.
     * @param expectedLiterals collection of literals where one literal name was substituted for other
     * @param realLiterals collection of literals with original names
     */
    public static void replaceAntiSatGate(Collection<Literal> expectedLiterals, Collection<Literal> realLiterals) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        Set<Literal> expectedCopy = new HashSet<>(expectedLiterals);
        Collection<String> realNames = realLiterals.stream().map(Literal::name).collect(Collectors.toList());

        for (Literal outputLiteral : expectedLiterals) {
            if (realNames.contains(removeSuffix(outputLiteral).name())) {
                expectedCopy.remove(outputLiteral);
                realNames.remove(removeSuffix(outputLiteral).name());
            }
        }

        if (expectedCopy.size() != 1 || realNames.size() != 1)
            return;

        Literal oldLiteral = expectedCopy.stream().findFirst().get();
        expectedLiterals.add(ff.literal(realNames.stream().findFirst().get(), oldLiteral.phase()));
        expectedLiterals.remove(oldLiteral);
    }

    /**
     * Removes the suffix '_A' or '_B' from the literal l. If the name of literal doesn't end with
     * this suffix, it will be retained.
     */
    public static Literal removeSuffix(Literal l) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();

        if (l.name().endsWith("_A") || l.name().endsWith("_B"))
            return ff.literal(l.name().substring(0, l.name().length()-2), l.phase());

        return l;
    }

    public static String getNewGateName() {
        return AntiSatGatePrefix + (AntiSatGateId++);
    }
}
