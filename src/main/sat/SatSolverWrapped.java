package main.sat;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.Collection;

public class SatSolverWrapped {
	
	private final SATSolver satSolver;
	private Assignment model;
		
	public SatSolverWrapped(){
		FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
		
//		this.satSolver = MiniSat.miniSat(ff, MiniSatConfig.builder().incremental(false).removeSatisfied(false).build());
		this.satSolver = MiniSat.miniSat(ff);
		this.model = null;
	}

	public void addFormula(Formula f){
		satSolver.add(f);
	}
	
	public Tristate solve(){
		Tristate ts = satSolver.sat();	
		this.setModel();
		return ts;
	}
	
	public Tristate solve(Collection<Literal> assumptions){
		Tristate ts = satSolver.sat(assumptions);
		this.setModel();
		return ts;
	}
	
	public Assignment getModel(){
		return this.model;
	}
	
	/*
	 * returns model restricted to variables
	 */
	public Assignment getModel(Collection<Variable> variablesFilter){
		return this.satSolver.model(variablesFilter);
	}
		
	private void setModel(){
		this.model = satSolver.model();
	}
	
	public void reset(){
		this.model = null;
		this.satSolver.reset();

	}
}
