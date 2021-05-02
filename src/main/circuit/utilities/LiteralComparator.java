package main.circuit.utilities;

import main.circuit.utilities.CircuitUtilities;
import org.logicng.formulas.Literal;

import java.util.Comparator;

public class LiteralComparator implements Comparator<Literal> {

    @Override
    public int compare(Literal l1, Literal l2) {

        String first = CircuitUtilities.removeSuffix(l1).name();
        String second = CircuitUtilities.removeSuffix(l2).name();

        return first.compareTo(second);
    }
}
