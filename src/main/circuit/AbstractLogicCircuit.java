package main.circuit;

import main.sat.FormulaFactoryWrapped;
import main.sat.SatSolverWrapped;
import main.utilities.KeyComparator;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;

/**
 * This class contains creating and includes some annoying getters / setters methods,
 * so they don't have to be in concrete LogicCircuit class.
 */
public abstract class AbstractLogicCircuit {
    private Set<String> inputNames;
    private Set<String> keyInputNames;
    private Set<String> outputNames;
    private List<Gate> gates;
    private Formula CNF;

    public AbstractLogicCircuit() {
        this.inputNames = new HashSet<>();
        this.keyInputNames = new HashSet<>();
        this.outputNames = new HashSet<>();
        this.gates = new ArrayList<>();
    }

    /**
     * Creates a circuit instance from .bench formatted file and computes its CNF form. Reads the
     * correct circuit key and AntiSAT key as well, if possible (if the file is locked).
     * @param benchFile Logic circuit in .bench formatted file.
     * @return an instance of LogicCircuit
     */
    public static LogicCircuit getCircuitInstance(File benchFile) {
        LogicCircuit ls = new LogicCircuit();

        BufferedReader br;
        String line;
        int[] keyValues;

        try {
            br = new BufferedReader(new FileReader(benchFile));

            // parsing bench file into lines
            while ((line = br.readLine()) != null) {

                // skip empty and commented lines
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {

                    // reading the correct circuit key
                    if (line.trim().startsWith("#0") || line.trim().startsWith("#1")) {
                        char[] keyBits = line.toCharArray();
                        keyValues = new int[keyBits.length - 1];
                        for (int i = 1; i < keyBits.length; i++)
                            keyValues[i - 1] = Character.getNumericValue(keyBits[i]);

                        ls.setCorrectKey(keyValues);
                    }

                    // reading the correct antiSAT key
                    if (line.trim().startsWith("#ASk: ")) {
                        line = line.substring(6);
                        char[] keyBits = line.toCharArray();
                        keyValues = new int[keyBits.length - 1];
                        for (int i = 1; i < keyBits.length; i++)
                            keyValues[i - 1] = Character.getNumericValue(keyBits[i]);

                        ls.setAntisatKey(keyValues);
                    }

                    continue;
                }

                // reading the inputs
                if (line.startsWith("INPUT(") || line.startsWith("input(") || line.startsWith("INPUT (") || line.startsWith("input (")) {
                    String input = line.substring(line.indexOf("(") + 1, line.indexOf(")"));

                    if (input.startsWith("k") || input.startsWith("K")) {
                        ls.getKeyInputNames().add(input);
                    } else {
                        ls.getInputNames().add(input);
                    }

                    continue;
                }

                // reading the outputs
                if (line.startsWith("OUTPUT(") || line.startsWith("output(") || line.startsWith("OUTPUT (") || line.startsWith("output (")) {
                    ls.getOutputNames().add(line.substring(line.indexOf("(") + 1, line.indexOf(")")));
                    continue;
                }

                // each line, that is not empty, commented, output or input
                // must be clause line, so it has to contain assignment operator
                if (line.contains("=")) {
                    String[] assignments = line.split("=");

                    String gateOutput = assignments[0].trim();
                    GateType gateType = GateType.valueOf(assignments[1].substring(0, assignments[1].indexOf("(")).trim().toUpperCase());
                    String[] gateInputsHelper = assignments[1].substring(assignments[1].indexOf("(") + 1, assignments[1].indexOf(")")).split(",");
                    String[] gateInputs = new String[gateInputsHelper.length];

                    for (int i = 0; i < gateInputs.length; i++) {
                        gateInputs[i] = gateInputsHelper[i].trim();
                    }
                    Gate gate = new Gate(gateType, gateOutput, gateInputs);
                    ls.getGates().add(gate);
                }
            }
            br.close();
        } catch (IOException ioe) {
            System.err.println("ERR: reading bench file: " + ioe.getMessage());
            ioe.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("ERR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        ls.createCNF();

        return ls;
    }

    protected void createCNF() {
        simplifyAllGates();
        List<Formula> CNFclauses = new ArrayList<>();
        for (Gate g : this.gates) {
            try {
                CNFclauses.add(g.toFormula());
            } catch (Exception e) {
                System.err.println("ERR: creating cnf:" + e.getMessage());
                System.err.println("ERR: gate:");
                System.err.println(g.toString());
                e.printStackTrace();
                return;
            }
        }
        this.CNF = FormulaFactoryWrapped.getFormulaFactory().and(CNFclauses);
    }

    private void simplifyAllGates() {
        List<Gate> decomposedGates = new ArrayList<>();
        for (Gate g : this.gates) {
            try {
                decomposedGates.addAll(g.simplifyGate());
            } catch (Exception e) {
                System.err.println("ERR: simplifing gate: " + e.getMessage());
                System.err.println("ERR: gate:");
                System.err.println(g.toString());
                e.printStackTrace();
                return;
            }
        }
        this.gates = decomposedGates;
    }


    public Assignment evaluate(Collection<Literal> inputLiterals, Collection<Literal> keyLiterals, Collection<Variable> outputVariables) throws Exception {
        if (keyLiterals != null) {
            if (this.keyInputNames.size() != keyLiterals.size()) {
                throw new Exception("invalid amount of key inputs defined to evaluate");
            }
        }

        if (this.inputNames.size() != inputLiterals.size()) {
            throw new Exception("invalid amount of regular inputs defined to evaluate: " + this.inputNames.size() + " vs. " + inputLiterals.size() + " (parameter of method)");
        }

        SatSolverWrapped solver = new SatSolverWrapped();
        //add formula to sat solver
        solver.addFormula(this.CNF);
        //concat assumptions to solve
        Collection<Literal> assumptions = new HashSet<>(inputLiterals);
        if (keyLiterals != null) {
            assumptions.addAll(keyLiterals);
        }

        //if is satisfiable
        if (solver.solve(assumptions).equals(Tristate.TRUE))
            if (outputVariables == null) {
                return solver.getModel();
            } else {
                return solver.getModel(outputVariables);
            }
        else {
            throw new Exception("unable to eval(circuit)");
        }
    }

    // Zatial nevyuzite, mozno ani nikdy nebude.
    public String insertAntiSATtype0AIRO(boolean secureImplementation) throws Exception {
        //all primary inputs, some primary outputs
        if (!secureImplementation)
            return null;

        Collection<String> newKeys = new HashSet<>();
        Map<String, Boolean> newKeysValues = new HashMap<>();
        int maxKeyID = 0;

        // zo vstupnych klucov sa najde maximalne ID
        for (String s : this.keyInputNames) {
            if (maxKeyID < Integer.parseInt(s.substring(1))) {
                maxKeyID = Integer.parseInt(s.substring(1));
            }
        }

        int old_maxKeyID = maxKeyID;
        old_maxKeyID++;
        maxKeyID++;

        int startingpointofnewKeys = maxKeyID;
        int numOfNewKeys = this.inputNames.size() * 2;
        SecureRandom sr = new SecureRandom();

        // pocet novych klucov bude 2nasobny oproti vstupom
        // nove vstupy sa pridaju do globalneho listu a vytvori sa mapovanie s ich nahodnou hodnotou
        for (int i = 0; i < numOfNewKeys; i++) {
            newKeys.add("k" + maxKeyID);
            System.out.println("adding: " + ("k" + maxKeyID));
            newKeysValues.put("k" + maxKeyID, sr.nextInt(2) == 0);
            maxKeyID = maxKeyID + 1;
        }

        this.keyInputNames.addAll(newKeys);

        Collection<String> in_A = new HashSet<>();
        Collection<String> in_B = new HashSet<>();

        List<Gate> newGates = new ArrayList<>();
        int ASgateID = 0;

        // vytvaraju sa nove hradla
        for (String s : this.inputNames) {
            System.out.println("trying to get:" + ("k" + old_maxKeyID));

            if (newKeysValues.get("k" + old_maxKeyID)) {
                newGates.add(new Gate(GateType.XOR, "ASg" + ASgateID, s, "k" + old_maxKeyID));
                newGates.add(new Gate(GateType.NOT, "ASg" + (ASgateID + 1), "ASg" + ASgateID));
                in_A.add("ASg" + (ASgateID + 1));
                ASgateID = ASgateID + 2;
                //xor + not
            } else {
                newGates.add(new Gate(GateType.XOR, "ASg" + ASgateID, s, "k" + old_maxKeyID));
                //xor
                in_A.add("ASg" + (ASgateID));

                ASgateID++;
            }

            if (newKeysValues.get("k" + (old_maxKeyID + this.inputNames.size()))) {
                newGates.add(new Gate(GateType.XOR, "ASg" + ASgateID, s, "k" + (old_maxKeyID + this.inputNames.size())));
                newGates.add(new Gate(GateType.NOT, "ASg" + (ASgateID + 1), "ASg" + ASgateID));
                in_B.add("ASg" + (ASgateID + 1));

                ASgateID = ASgateID + 2;

            } else {
                newGates.add(new Gate(GateType.XOR, "ASg" + ASgateID, s, "k" + (old_maxKeyID + this.inputNames.size())));
                in_B.add("ASg" + (ASgateID));

                ASgateID++;
            }

            old_maxKeyID++;
        }

        ASgateID++;
        int in_A_out = ASgateID;
        newGates.add(new Gate(GateType.AND, "ASg" + ASgateID, new ArrayList<String>(in_A).toArray(new String[in_A.size()])));
        ASgateID++;
        int in_B_out = ASgateID;
        newGates.add(new Gate(GateType.NAND, "ASg" + ASgateID, new ArrayList<String>(in_B).toArray(new String[in_B.size()])));
        ASgateID++;
        newGates.add(new Gate(GateType.AND, "ASg" + ASgateID, "ASg" + in_A_out, "ASg" + in_B_out));
        String connectingPoint = "ASg" + ASgateID;

        // pripojenie na primarne vystupy
        int howManyOfOutputs = sr.nextInt() % this.outputNames.size();
        if (howManyOfOutputs == 0) {
            howManyOfOutputs++;
        }
        ArrayList<String> outputs = new ArrayList<>(this.outputNames);
        HashSet<String> hs = new HashSet<>(outputs);
        for (int i = 0; i < howManyOfOutputs; i++) {
            ASgateID++;
            String output = outputs.get(i);
            newGates.add(new Gate(GateType.XOR, "ASg" + ASgateID, connectingPoint, output));
            hs.remove(output);
            System.out.println(outputs);
            hs.add("ASg" + ASgateID);
        }
        this.outputNames = hs;

        this.gates.addAll(newGates);
        StringBuilder sb = new StringBuilder();
        for (int i = startingpointofnewKeys; i < newKeys.size() + startingpointofnewKeys; i++) {
            sb.append(newKeysValues.get("k" + i) ? "1" : "0");
        }
        return sb.toString();
    }


    /**
     * Getters, Setters
     **/

    public Set<String> getInputNames() {
        return inputNames;
    }

    public Collection<Literal> getInputLiterals(FormulaFactory ff, int[] initValues) {
        if (initValues != null) {
            if (this.inputNames.size() != initValues.length) {
                System.err.println("Incorrect size of init values for inputs. Got " + initValues.length + ", Required " + this.inputNames.size());
                return null;
            }
        }

        int i = 0;
        List<String> sortedInputNames = new ArrayList<>(inputNames);
        Collections.sort(sortedInputNames);
        Collection<Literal> inputLiterals = new HashSet<>();
        for (String s : sortedInputNames) {
            inputLiterals.add(ff.literal(s, initValues != null && initValues[i] == 1));
            i++;
        }

        return inputLiterals;
    }

    public Collection<Variable> getInputVariables(FormulaFactory ff) {
        Collection<Variable> inputVariables = new ArrayList<>();
        for (String s : this.inputNames)
            inputVariables.add(ff.variable(s));

        return inputVariables;
    }

    public Set<String> getKeyInputNames() {
        return keyInputNames;
    }

    public Collection<Literal> getKeyLiterals(FormulaFactory ff, int[] initValues) {
        if (initValues != null) {
            if (this.keyInputNames.size() != initValues.length) {
                System.err.println("Incorrect size of init values for key. Got " + initValues.length + ", Required " + this.keyInputNames.size());
                return null;
            }
        }

        List<String> sortedKeyNames = new ArrayList<>(keyInputNames);
        sortedKeyNames.sort(new KeyComparator());
        int i = 0;
        Collection<Literal> keyLiterals = new HashSet<>();
        for (String s : sortedKeyNames) {
            keyLiterals.add(ff.literal(s, initValues != null && initValues[i] == 1));
            i++;
        }

        return keyLiterals;
    }

    public Set<String> getOutputNames() {
        return outputNames;
    }

    public Collection<Variable> getOutputVariables(FormulaFactory ff) {
        Collection<Variable> outputVariables = new HashSet<Variable>();
        for (String s : outputNames) {
            outputVariables.add(ff.variable(s));
        }

        return outputVariables;
    }

    public Formula getCNF() {
        return this.CNF;
    }

    public List<Gate> getGates() { return this.gates; }


    //	public static void main(String[] args) throws Exception{
//
//		/*
//		String command1 = "./solver -in ";
//		String command2 = " -key ";
//		String command3 = " > ";
//
//		String benchFile = null;
//		String key = null;
//		String outputFile = null;
//
//		//nochain_10, _20, _30
//		for(File f:new File("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\LOCKED\\anti_sat").listFiles()){
//			//AIAO, AIRO
//			for(File g:f.listFiles()){
//				for(File h:g.listFiles()){
//					benchFile = "LOCKED/anti_sat/"+f.getName()+"/"+g.getName()+"/"+h.getName();
//					outputFile = "LOG/anti_sat/"+f.getName()+"/"+g.getName()+"/"+h.getName()+".log";
//					File z = new File("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\KEYS\\"+f.getName()+"\\"+h.getName().split("_")[1].split("\\.")[0]+"_"+f.getName()+"_key.txt");
//					if(!z.exists()){
//						break;
//					}
//					BufferedReader br = new BufferedReader(new FileReader(z));
//					key = br.readLine().trim();
//					br.close();
//					File x = new File("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\KEYS\\anti_sat\\"+f.getName()+"\\"+g.getName()+"\\"+h.getName().split("\\.")[0]+".bench.key");
//					br = new BufferedReader(new FileReader(x));
//					key = key + br.readLine().trim();
//					br.close();
//					System.out.println(command1+benchFile+command2+key+command3+outputFile);
//
//				}
//			}
//		}
//
//		*/
//
//		int n = 12;
//		int p = 1;
//		File f = new File("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\roznen_new\\c880.bench");
//		LogicCircuit lc = LogicCircuit.getLogicCircuitInstance(f);
//		lc.insertAntiSAT(-1, n, p, true);
//		lc.writeToFile("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\roznen_new", "n"+n+"_c880.bench", null);
//
//
//		/*
//		String folderWithLC = "C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\LOCKED\\";
//		String outputFolder = "C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\LOCKED\\anti_sat\\";
//
//		String keyFolder = "C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\testing\\KEYS\\anti_sat\\";
//
//		String currentLCs = "nochain_30";
//		String asmethod = "RIRO";
//
//		File folder = new File(folderWithLC+currentLCs);
//		File[] files = folder.listFiles();
//
//		for(File f:files){
//			LogicCircuit lc = LogicCircuit.getLogicCircuitInstance(f);
////			String key = lc.insertAntiSATtype0AIAO(true);
////			String key = lc.insertAntiSATtype0AIRO(true);
//			String key = lc.insertAntiSATtype0RIRO(true);
////			String key = lc.insertAntiSATtype0_RANDOM_PLACE(true);
//			lc.writeToFile(outputFolder+currentLCs+"\\"+asmethod, f.getName()+asmethod+".bench", "null");
//			File r = new File(keyFolder+currentLCs+"\\"+asmethod+"\\"+f.getName()+".key");
//			r.createNewFile();
//			FileOutputStream fos = new FileOutputStream(r, false);
//			fos.write(key.getBytes());
//			fos.flush();
//			fos.close();
//
//		}
//		*/
//
//
//		/*
//		int a = 1;
//		int b = a;
//		a++;
//		System.out.println(a);
//		System.out.println(b);
//
//		File f = new File("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\resources\\Janikovsky CD\\solver\\CODE\\LOCKED\\nochain\\44_c1908.bench");
//		LogicCircuit lc = LogicCircuit.getLogicCircuitInstance(f);
//		lc.insertAntiSATtype0_RANDOM_PLACE(true);
//		lc.writeToFile("C:\\Users\\Jan\\Desktop", "test.bench", "w/o comment");
//
//
//		File folder = new File("C:\\Users\\Jan\\Desktop\\Diplomovy projekt\\resources\\Janikovsky CD\\solver\\CODE\\IC");
//		File[] files = folder.listFiles();
//		for(File fa:files){
//			System.out.println(fa.getName());
//		}
//		*/
//
//		/*
//		File folder = new File("C:\\Users\\Jan\\Desktop\\testBench");
//		File[] files = folder.listFiles();
//		for(File f:files){
//			if(f.isFile()){
//				System.out.println("Parsing: "+f.getName());
//				LogicCircuit.getLogicCircuitInstance(f);
//
//				if(f.getName().startsWith("halfAdder")){
//					LogicCircuit ls = LogicCircuit.getLogicCircuitInstance(f);
//
//					ls.simplifyAllGates();
//					ls.createCNF();
//
//					System.out.println(ls.CNF);
//
//					ls.writeToFile("C:\\Users\\Jan\\Desktop\\testBench", "out_"+f.getName()+".bench", null);
//					System.out.println("Inputs["+ls.inputsRegular.size()+"]:  "+ls.inputsRegular.toString());
//					System.out.println("Key inputs["+ls.inputsKey.size()+"]: "+ls.inputsKey.toString());
//					System.out.println("Outputs["+ls.outputs.size()+"]: "+ls.outputs.toString());
//					System.out.println("Gates["+ls.gates.size()+"]:");
//					for(Gate g: ls.gates){
//						System.out.println();
//						System.out.println(g.toString());
//					}
//				}
//			}
//		}
//		*/
//
//		/*
//		FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
//		File f = new File("C:\\Users\\Jan\\Desktop\\testBench\\halfAdderAND.bench");
//		LogicCircuit lc = LogicCircuit.getLogicCircuitInstance(f);
//		lc.simplifyAllGates();
//		lc.createCNF();
//
//		Assignment a = lc.evaluate(Arrays.asList(new Literal[]{
//				ff.literal("A", true), ff.literal("B", true)
//		}), Arrays.asList(new Literal[]{
//
//		}));
//		*/
//
//	}

}
