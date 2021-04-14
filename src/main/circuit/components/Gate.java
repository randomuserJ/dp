package main.circuit.components;

import main.attacker.sat.FormulaFactoryWrapper;
import main.utilities.GlobalCounter;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Gate{
	private GateType type;
	private List<String> inputs;
	private final String output;
	private final boolean neg;
	
	public Gate(GateType type, String output, String ... inputs) {
		this.type = type;
		this.inputs = Arrays.asList(inputs);
		this.output = output;
		this.neg = this.type.isNeg();

		if(this.type.equals(GateType.NOT) && inputs.length != 1){
			System.err.println("WARNING: Malformed NOT gate (multiple inputs to NOT gate)");
		}
		if(this.type.equals(GateType.BUF) && inputs.length != 1){
			System.err.println("WARNING: Malformed BUF gate (multiple inputs to NOT gate)");
		}
		if( ( !this.type.equals(GateType.NOT) && !this.type.equals(GateType.BUF) ) && inputs.length < 2){
			System.err.println("WARNING: Malformed " + this.type + " gate (not enough inputs)");
		}
	}
		
	public GateType getType() {
		return type;
	}

	public void setType(GateType type) {
		this.type = type;
	}

	public List<String> getInputs() {
		return inputs;
	}
	
	public boolean isNeg(){
		return this.neg;
	}

	public String getOutput() {
		return output;
	}

	@Override
	public String toString(){
		return "Inputs: " + this.inputs.toString() + "\n" +
				"Output: " + this.output + "\n" +
				"Type:   " + this.type + "\n" +
				"Neg:    " + this.neg;
	}

	public List<Gate> simplifyGate() {
		ArrayList<Gate> decomposedGates = new ArrayList<>();

		if(this.getType().equals(GateType.BUF) || this.getType().equals(GateType.NOT)){
			decomposedGates.add(this);
			return decomposedGates;
		}
		
		if(this.getInputs().size() == 2){
			decomposedGates.add(this);
			return decomposedGates;
		}
		
		if(this.getInputs().size() > 2){
			GateType generatedGatesType;
			if (this.getType() == GateType.NAND)
				generatedGatesType = GateType.AND;
			else
				generatedGatesType = this.getType();

			String name = "G_" + GlobalCounter.getCounter();

			decomposedGates.add(new Gate(generatedGatesType, name, this.getInputs().get(0), this.getInputs().get(1)));
			for(int i = 2; i < this.getInputs().size(); i++){
				String name2 = "G_" + GlobalCounter.getCounter();
				if(i == (this.getInputs().size()-1)){
					decomposedGates.add(new Gate(generatedGatesType, this.getOutput(), this.getInputs().get(i), name));
				}else{
					decomposedGates.add(new Gate(generatedGatesType, name2, this.getInputs().get(i), name));
				}
				name = name2;
			}

			if (this.getType() == GateType.NAND)
				decomposedGates.get(decomposedGates.size()-1).setType(GateType.NAND);

			return decomposedGates;
		}
		return null;
	}

	public Formula toFormula() throws IllegalStateException{
		FormulaFactory f = FormulaFactoryWrapper.getFormulaFactory();
		List<Formula> operands = new ArrayList<>();

		//preparation for gates (N-AND, N-OR, X-N-OR)
		Variable inputA = f.variable(this.inputs.get(0));
		Variable inputB = f.variable("");
		Variable output = f.variable(this.output);

		if (!(this.inputs.size() == 2 || this.inputs.size() == 1))
			throw new IllegalStateException("unknown gate when creating formula");

		// inputB for AND, NAND, OR, NOR, XOR, XNOR
		// otherwise inputA is enough (NOT, BUF)
		if(this.inputs.size() == 2)
			inputB = f.variable(this.inputs.get(1));

		switch(this.getType()){
			case AND:
				operands.add(f.or(inputA.negate(), inputB.negate(), output));
				operands.add(f.or(inputB, output.negate()));
				operands.add(f.or(inputA, output.negate()));
				return f.and(operands);
				
			case NAND:
				operands.add(f.or(inputA.negate(), inputB.negate(), output.negate()));
				operands.add(f.or(inputB, output));
				operands.add(f.or(inputA, output));
				return f.and(operands);
				
			case OR:
				operands.add(f.or(inputA, inputB, output.negate()));
				operands.add(f.or(inputB.negate(), output));
				operands.add(f.or(inputA.negate(), output));
				return f.and(operands);
	
			case NOR:
				operands.add(f.or(inputA, inputB, output));
				operands.add(f.or(inputB.negate(), output.negate()));
				operands.add(f.or(inputA.negate(), output.negate()));
				return f.and(operands);
	
			case XOR:
				operands.add(f.or(inputA.negate(), inputB.negate(), output.negate()));
				operands.add(f.or(inputA, inputB, output.negate()));
				operands.add(f.or(inputA, inputB.negate(), output));
				operands.add(f.or(inputA.negate(), inputB, output));
				return f.and(operands);
	
			case XNOR:
				operands.add(f.or(inputA.negate(), inputB.negate(), output));
				operands.add(f.or(inputA.negate(), inputB, output.negate()));
				operands.add(f.or(inputA, inputB.negate(), output.negate()));
				operands.add(f.or(inputA, inputB, output));
				return f.and(operands);
	
			case NOT:
				operands.add(f.or(output, inputA));
				operands.add(f.or(output.negate(), inputA.negate()));
				return f.and(operands);
	
			case BUF:
				operands.add(f.or(output.negate(), inputA));
				operands.add(f.or(output, inputA.negate()));
				return f.and(operands);
				
			default:
				throw new IllegalStateException("unable to get formula from gate");
		}
	}
}
