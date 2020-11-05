package main.sat;

import org.logicng.formulas.FormulaFactory;


/*
 * 	Singleton class representing formula factory
 * 	
 */
public class FormulaFactoryWrapped {
	private static FormulaFactoryWrapped instance = null;
	private FormulaFactory ff = null;
	
	private FormulaFactoryWrapped(){
		ff = new FormulaFactory();
	}
	
	public static FormulaFactory getFormulaFactory(){
		if(instance == null){
			instance = new FormulaFactoryWrapped();
			return instance.ff;
		}
		return instance.ff;
	}
	
}
