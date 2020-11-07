package main.circuit;

public enum GateType {
	AND,
	OR,
	NAND,
	NOR,
	XOR,
	XNOR,
	NOT,
	BUF;

	public boolean isNeg() {
		switch(this){
			case NAND:
			case NOR:
			case XNOR:
			case NOT:
				return true;
			default:
				return false;			
		}
	}
}
