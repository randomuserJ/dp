package main.sat;

import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.solvers.sat.MiniSatConfig;

import java.util.Collection;


/*
 * 	Not to be used as singleton
 */
public class SatSolverWrapped {
	
	private SATSolver satSolver;
	private FormulaFactory ff;
	private Assignment model;
		
	public SatSolverWrapped(){
		this.ff = FormulaFactoryWrapped.getFormulaFactory();
		
//		this.satSolver = MiniSat.miniSat(ff, MiniSatConfig.builder().incremental(false).removeSatisfied(false).build());
		this.satSolver = MiniSat.miniSat(ff);
		this.model = null;
	}
	
	public SATSolver getSatSolver() {
		return satSolver;
	}
	
	public void addFormula(Formula f){
		satSolver.add(f);
	}
	
	public void addFormulas(Collection<Formula> formulas){
		satSolver.add(formulas);
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
	
	//testing
	// public static void main(String[] args) throws ParserException {


		/*
		Variable a = f.variable("A");
		Variable b = f.variable("B");
		Formula formula = f.or(a, b);
		System.out.println("A: "+a.phase());
		System.out.println("B: "+b.phase());

		ss.addFormula(formula);
		ss.solve();
		
		System.out.println("Formula: "+formula);
		System.out.println("Formula: Literals: "+formula.literals());
		System.out.println(ss.getModel());
		System.out.println("A: "+a.phase());
		System.out.println("A: Literals: "+a.literals());
		System.out.println("B: "+b.phase());
		System.out.println("B: Literals: "+b.literals());
		*/


				
		/*
		Variable a = f.variable("A");
		Variable b = f.variable("B");
		Formula formula = f.or(a, b);
		
		Assignment as = new Assignment();
		as.addLiteral(f.literal("A", false));
		as.addLiteral(f.literal("B", true));

		System.out.println(formula.evaluate(as));
		*/
		
		
		/*
		LogicCircuit ls = LogicCircuit.getLogicCircuitInstance(new File("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\resources\\Janikovsky CD\\solver\\CODE\\LOCKED\\rand\\1_c17.bench"));
		ls.simplifyAllGates();
		ls.createCNF();
		ss.addFormula(ls.getCNF());
		ss.satSolver.sat();
		System.out.println(ss.satSolver.model());
		System.out.println(ss.satSolver.enumerateAllModels());
		*/
		
		/*
		Formula formula = f.or(f.and(f.variable("A").negate(), f.variable("B"), f.variable("B")));
		Iterator<Formula> i = formula.iterator();
		while(i.hasNext()){
			System.out.println(i.next());
		}
		*/

		/*
		Formula formula_A = f.and(f.variable("A"), f.variable("B"));
		Formula formula_B = f.or(f.variable("C"), f.variable("D"));
		
		ss.addFormula(formula_A);
		ss.solve();
		System.out.println(ss.getModel());
		
		ss.addFormula(formula_B);
		ss.solve();
		System.out.println(ss.getModel());
		
		Collection<Variable> col = new ArrayList<Variable>();
		col.add(f.variable("C"));
		
		Assignment a = ss.getModel(col);
		System.out.println(a.literals());
		*/
		
		
		/*
		Formula formulaA = f.and(f.variable("A").negate(),  f.variable("B"));
		Formula formulaB = null;
		Substitution sub = new Substitution();
		
		sub.addMapping(f.variable("A"), f.variable("A_new"));
		sub.addMapping(f.variable("B"), f.variable("B_new"));
		formulaB = formulaA.substitute(sub);
		
		System.out.println(formulaA);
		System.out.println(formulaB);
		*/
		

		
	//}
	
}
