package main.circuit.utilities.custom_comparators;

import main.circuit.utilities.CircuitUtilities;
import org.logicng.formulas.Literal;

import java.util.Comparator;

public class CustomLiteralComparator implements Comparator<Literal> {

    @Override
    public int compare(Literal l1, Literal l2) {

        String first = CircuitUtilities.removeSuffix(l1).name();
        String second = CircuitUtilities.removeSuffix(l2).name();

        return first.compareTo(second);
    }
}
