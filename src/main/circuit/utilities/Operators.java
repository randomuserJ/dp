package main.circuit.utilities;

import main.helpers.FormulaFactoryWrapper;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public class Operators {

    /**
     * Returns an exclusive or of two operands. Operand can be any type of Formula.
     * This method is expansion of standard FormulaFactory methods.
     * E.g. xor(a, NOT(b)) => ( (A OR NOT(b)) AND (NOT(a) OR b) )
     */
    public static Formula xor(Formula leftOperand, Formula rightOperand) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        return ff.and(ff.or(leftOperand, rightOperand), ff.or(leftOperand.negate(), rightOperand.negate()));
    }

    /**
     * Returns an exclusive nor of two formulas. Operand can be any type of Formula.
     * This method is expansion of standard FormulaFactory methods.
     * E.g. xnor(a, NOT(b)) => ( (A OR b) AND (NOT(a) OR NOT(b)) )
     */
    public static Formula xnor(Formula leftOperand, Formula rightOperand) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        return ff.and(ff.or(leftOperand, rightOperand.negate()), ff.or(leftOperand.negate(), rightOperand));
    }
}
