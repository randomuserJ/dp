package main.circuit;

import main.sat.FormulaFactoryWrapped;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Gate{
	private final GateType type;
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
	
	public void setInputs(List<String> newInputs){
		this.inputs = new ArrayList<String>(newInputs);
	}
		
	public GateType getType() {
		return type;
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
		StringBuilder sb = new StringBuilder();
		sb.append("Inputs: "+this.inputs.toString()+"\n");
		sb.append("Output: "+this.output+"\n");
		sb.append("Type:   "+this.type+"\n");
		sb.append("Neg:    "+this.neg);
		return sb.toString();
		
	}

	public List<Gate> simplifyGate() throws Exception {
		ArrayList<Gate> decompositedGates = new ArrayList<Gate>();

		if(this.getType().equals(GateType.BUF) || this.getType().equals(GateType.NOT)){
			decompositedGates.add(this);
			return decompositedGates;
		}
		
		if(this.getInputs().size() == 2){
			decompositedGates.add(this);
			return decompositedGates;
		}
		
		if(this.getInputs().size() > 2){
			String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			
			decompositedGates.add(new Gate(this.getType(), uuid, new String[]{this.getInputs().get(0), this.getInputs().get(1)}));
			for(int i = 2; i < this.getInputs().size(); i++){
				String uuid2 = UUID.randomUUID().toString().replaceAll("-", "");
				if(i == (this.getInputs().size()-1)){
					decompositedGates.add(new Gate(this.getType(), this.getOutput(), new String[]{this.getInputs().get(i), uuid}));
				}else{
					decompositedGates.add(new Gate(this.getType(), uuid2, new String[]{this.getInputs().get(i), uuid}));
				}
				uuid = uuid2;
			}
			
			
			return decompositedGates;
		}
		return null;
	}

	public Formula toFormula() throws Exception{
		FormulaFactory f = FormulaFactoryWrapped.getFormulaFactory();
		List<Formula> operands = new ArrayList<Formula>();
		
		Variable inputA = null;
		Variable inputB = null;
		Variable output = null;
		
		if(this.inputs.size() == 2){
			//preparation for gates (N-AND, N-OR, X-N-OR)
			inputA = f.variable(this.inputs.get(0));
			inputB = f.variable(this.inputs.get(1));
			output = f.variable(this.output);
		}else if(this.inputs.size() == 1){
			//preparation for gates (BUF, INV)
			inputA = f.variable(this.inputs.get(0));
			output = f.variable(this.output);			
		}else{
			throw new Exception("unknown gate when creating formula");
		}
		
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
