package main.circuit;

import main.sat.FormulaFactoryWrapped;
import main.utilities.GlobalCounter;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Gate{
	private GateType type;
	private List<String> inputs;
	private final String output;
	private final boolean neg;
	
	public Gate(GateType type, String output, String ... inputs) throws Exception{
		this.type = type;
		this.inputs = Arrays.asList(inputs);
		this.output = output;
		this.neg = this.type.isNeg();
		if(this.type.equals(GateType.NOT) && inputs.length != 1){
			throw new Exception("malformed NOT gate (multiple inputs to NOT gate)");
		}
		if(this.type.equals(GateType.BUF) && inputs.length != 1){
			throw new Exception("malformed BUF gate (multiple inputs to NOT gate)");
		}
		if( ( !this.type.equals(GateType.NOT) && !this.type.equals(GateType.BUF) ) && inputs.length < 2){
			throw new Exception("malformed "+this.type+" gate (not enough inputs to "+this.type+" gate)");
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

	public List<Gate> simplifyGate() throws Exception {
		ArrayList<Gate> decompositedGates = new ArrayList<>();

		if(this.getType().equals(GateType.BUF) || this.getType().equals(GateType.NOT)){
			decompositedGates.add(this);
			return decompositedGates;
		}
		
		if(this.getInputs().size() == 2){
			decompositedGates.add(this);
			return decompositedGates;
		}
		
		if(this.getInputs().size() > 2){
			GateType generatedGatesType = null;
			if (this.getType() == GateType.NAND)
				generatedGatesType = GateType.AND;
			else
				generatedGatesType = this.getType();

			String name = "G_" + GlobalCounter.getCounter();

			decompositedGates.add(new Gate(generatedGatesType, name, this.getInputs().get(0), this.getInputs().get(1)));
			for(int i = 2; i < this.getInputs().size(); i++){
				String name2 = "G_" + GlobalCounter.getCounter();
				if(i == (this.getInputs().size()-1)){
					decompositedGates.add(new Gate(generatedGatesType, this.getOutput(), this.getInputs().get(i), name));
				}else{
					decompositedGates.add(new Gate(generatedGatesType, name2, this.getInputs().get(i), name));
				}
				name = name2;
			}

			if (this.getType() == GateType.NAND)
				decompositedGates.get(decompositedGates.size()-1).setType(GateType.NAND);

			return decompositedGates;
		}
		return null;
	}

	public Formula toFormula() throws Exception{
		FormulaFactory f = FormulaFactoryWrapped.getFormulaFactory();
		List<Formula> operands = new ArrayList<>();

		//preparation for gates (N-AND, N-OR, X-N-OR)
		Variable inputA = f.variable(this.inputs.get(0));
		Variable inputB = f.variable("");
		Variable output = f.variable(this.output);

		if (!(this.inputs.size() == 2 || this.inputs.size() == 1))
			throw new Exception("unknown gate when creating formula");

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
				throw new Exception("unable to get formula from gate");
				

		}
	}
}
