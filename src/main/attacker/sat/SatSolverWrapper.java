package main.attacker.sat;

import main.helpers.FormulaFactoryWrapper;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.Collection;

public class SatSolverWrapper {
	
	private final SATSolver satSolver;
	private Assignment model;
		
	public SatSolverWrapper(){
		FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
		this.satSolver = MiniSat.miniSat(ff);
		this.model = null;
	}

	public void addFormula(Formula f){
		satSolver.add(f);
	}

	/**
	 * Finds boolean assignment for each variable so that the formula is satisfied.
	 */
	public Tristate solve(){
		Tristate ts = satSolver.sat();
		this.saveModel();
		return ts;
	}

	 /**
	  * Finds boolean assignment for all values, that are not fixed in the assumption.
	  */
	public Tristate solve(Collection<Literal> assumptions){
		Tristate ts = satSolver.sat(assumptions);
		this.saveModel();
		return ts;
	}

	public void reset(){
		this.model = null;
		this.satSolver.reset();
	}

	/* Getters */

	/**
	 * Returns a model - boolean value assignment for each variable.
	 */
	public Assignment getModel(){
		return this.model;
	}

	/**
	 * Returns a model restricted to variable filter.
	 */
	public Assignment getModel(Collection<Variable> variablesFilter){
		return this.satSolver.model(variablesFilter);
	}

	/* Utilities */

	private void saveModel(){
		this.model = satSolver.model();
	}
}
