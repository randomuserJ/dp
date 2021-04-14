package main.circuit.components;

import main.attacker.sat.FormulaFactoryWrapper;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public class Operators {

    public static Formula xor(Formula leftOperand, Formula rightOperand) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        return ff.and(ff.or(leftOperand, rightOperand), ff.or(leftOperand.negate(), rightOperand.negate()));
    }

    public static Formula xnor(Formula leftOperand, Formula rightOperand) {
        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        return ff.and(ff.or(leftOperand, rightOperand.negate()), ff.or(leftOperand.negate(), rightOperand));
    }
}
