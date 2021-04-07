package main.utilities;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Literal;

import java.util.Collection;
import java.util.Iterator;

public class LogicUtilities {

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

    public static int hammingWeight(Collection<Literal> literals) {
        int count = 0;

        for (Literal literal : literals) {
            if (literal.phase())
                count++;
        }

        return count;
    }
}
