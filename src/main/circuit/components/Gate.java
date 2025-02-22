package main.circuit.components;

import main.helpers.FormulaFactoryWrapper;
import main.helpers.utilities.GlobalCounter;
import main.helpers.utilities.Protocol;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Gate{
	private GateType type;
	private final List<String> inputs;
	private final String output;
	private final boolean neg;

	/**
	 * Default constructor for Gate object. User can specify one or inputs.
	 * @param type type of gate
	 * @param output gate's name
	 * @param inputs arbitrary count of gate inputs
	 */
	public Gate(GateType type, String output, String ... inputs) {
		this.type = type;
		this.inputs = Arrays.asList(inputs);
		this.output = output;
		this.neg = this.type.isNeg();

		if(this.type.equals(GateType.NOT) && inputs.length != 1){
			Protocol.printWarningMessage("Malformed NOT gate (multiple inputs to NOT gate).");
		}
		if(this.type.equals(GateType.BUF) && inputs.length != 1){
			Protocol.printWarningMessage("Malformed BUF gate (multiple inputs to BUF gate).");
		}
		if( ( !this.type.equals(GateType.NOT) && !this.type.equals(GateType.BUF) ) && inputs.length < 2){
			Protocol.printWarningMessage("Malformed " + this.type + " gate (not enough inputs).");
		}
	}

	/**
	 * Decomposes all multi-input gates and create multiple gates with two inputs from it.
	 * Logic of each gate has to be retained.
	 * @return list of gates containing only of NOT and BUF gates or other gates with only two inputs
	 */
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

	/**
	 * Converts gate to a CNF formula following the standard Tseytin transformation.
	 * @return CNF representation of logic gate
	 */
	public Formula toFormula() throws IllegalStateException{
		FormulaFactory f = FormulaFactoryWrapper.getFormulaFactory();
		List<Formula> operands = new ArrayList<>();

		//preparation for gates (NAND, NOR, XNOR)
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
				throw new IllegalStateException("Unable to get formula from gate");
		}
	}

	/* Getters */

	public GateType getType() {
		return type;
	}

	public List<String> getInputs() {
		return inputs;
	}

	public String getOutput() {
		return output;
	}

	public boolean isNeg(){
		return this.neg;
	}

	/* Setters */

	public void setType(GateType type) {
		this.type = type;
	}

	/* Utilities */
	@Override
	public String toString(){
		return "Inputs: " + this.inputs.toString() + "\n" +
				"Output: " + this.output + "\n" +
				"Type:   " + this.type + "\n" +
				"Neg:    " + this.neg;
	}
}
