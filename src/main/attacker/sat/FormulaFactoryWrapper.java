package main.attacker.sat;

import org.logicng.formulas.FormulaFactory;


/*
 * 	Singleton class representing formula factory
 * 	
 */
public class FormulaFactoryWrapper {
	private static FormulaFactoryWrapper instance = null;
	private FormulaFactory ff;
	
	private FormulaFactoryWrapper(){
		ff = new FormulaFactory();
	}

	/**
	 * Singleton method for getting global object of FormulaFactory
	 * @return a static object of FormulaFactory
	 */
	public static FormulaFactory getFormulaFactory(){
		if(instance == null){
			instance = new FormulaFactoryWrapper();
			return instance.ff;
		}
		return instance.ff;
	}
	
}
