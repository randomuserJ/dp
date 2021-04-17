package main.attacker.sat;

import org.logicng.formulas.FormulaFactory;


public class FormulaFactoryWrapper {
	private static FormulaFactoryWrapper instance = null;
	private final FormulaFactory ff;
	
	private FormulaFactoryWrapper(){
		ff = new FormulaFactory();
	}

	/**
	 * Singleton method for getting global object of FormulaFactory
	 * @return a static object of FormulaFactory
	 */
	public static FormulaFactory getFormulaFactory() {
		if (instance == null) {
			instance = new FormulaFactoryWrapper();
			return instance.ff;
		}
		return instance.ff;
	}
}
