package main.circuit;

import main.utilities.FormulaFactoryWrapper;
import main.circuit.components.Gate;
import main.circuit.components.GateType;
import main.utilities.KeyMapper;
import main.utilities.CircuitUtilities;
import main.utilities.Randomizer;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;


/**
 * This class is implementation of AbstractLogicCircuit class, just for some code cleanness.
 */
public class LogicCircuit extends AbstractLogicCircuit {
    private int[] correctKey;
    private int[] antisatKey;
    private Map<String, KeyMapper> inputKeyMapping;
    private LogicCircuit evaluationCircuit;

    public LogicCircuit() {
        this.correctKey = new int[0];
        this.antisatKey = new int[0];
        this.inputKeyMapping = new HashMap<>();
    }

    /**
     * Writes a LogicCircuit instance into .bench formatted file. First line is reserved for comments.
     * In second line will be written a correct circuit key (if the file is locked).
     * In third line will be written a correct AntiSAT key (if the file is locked with AntiSAT).
     * Other lines are intended for circuit components, such as input / output variables and gates.
     * @param path Relative path of file.
     * @param outputFileName It is what it is.
     * @param comment Optional parameter for user's comment.
     */
    public void writeToFile(String path, String outputFileName, String comment) {
        BufferedWriter bw = null;
        File file;

        try {
            file = new File(path + File.separator + outputFileName);

            bw = new BufferedWriter(new FileWriter(file));
            bw.write("#" + (comment != null ? comment : "without comments"));
            bw.newLine();

            addKeysToBuffer(bw);
            addInputsToBuffer(bw);
            addOutputsToBuffer(bw);
            addGatesToBuffer(bw);

        } catch (IOException ioe) {
            System.err.println("ERR: writing logic circuit to file: " + ioe.getMessage());
            ioe.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    System.err.println("ERR: closing writer: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void addKeysToBuffer(BufferedWriter bw) throws IOException{
        bw.write("#");
        for (int i : this.correctKey) {
            bw.write(String.valueOf(i));
        }
        bw.newLine();
        bw.write("#ASk: ");
        for (int i : this.antisatKey) {
            bw.write(String.valueOf(i));
        }
        bw.newLine();
    }

    private void addInputsToBuffer(BufferedWriter bw) throws IOException {
        for (String inputRegular : this.getInputNames()) {
            bw.write("INPUT(" + inputRegular + ")");
            bw.newLine();
        }
        bw.flush();
        for (String inputKey : this.getKeyInputNames()) {
            bw.write("INPUT(" + inputKey + ")");
            bw.newLine();
        }
        bw.flush();
    }

    private void addOutputsToBuffer(BufferedWriter bw) throws IOException {
        for (String output : this.getOutputNames()) {
            bw.write("OUTPUT(" + output + ")");
            bw.newLine();
        }
        bw.newLine();
        bw.flush();
    }

    private void addGatesToBuffer(BufferedWriter bw) throws IOException {
        for (Gate gate : this.getGates()) {
            bw.write(gate.getOutput());
            bw.write(" = ");
            bw.write(gate.getType().toString().toLowerCase());
            bw.write("(");
            bw.write(gate.getInputs().toString().substring(1, gate.getInputs().toString().indexOf("]")));
            bw.write(")");
            bw.newLine();
        }
        bw.flush();
    }

    /**
     * Inserts AntiSAT lock into the logic circuit.
     * @param type Describes AntiSAT type. Use type 0 for activating inverting signal of locking mechanism by
     *             one specific combination, or type 1 for activating signal by any but 1 specific combination
     * @param n The number of circuit inputs that will be in relation with AntiSAT key.
     */
    public void insertAntiSAT(int type, int n) {
        if (!checkParamsForAntiSat(type, n))
            return;

        Map<String, Boolean> newKeys = new HashMap<>();
        generateAntiSatKey(n * 2, newKeys);

        ArrayList<String> regularInputList = new ArrayList<>(this.getInputNames());
        ArrayList<String> regularOutputList = new ArrayList<>(this.getOutputNames());
        ArrayList<Gate> newGates = new ArrayList<>();
        ArrayList<String> AS_inputs_A = new ArrayList<>();
        ArrayList<String> AS_inputs_B = new ArrayList<>();

        for (int i = 0; i < n * 2; i++) {
            Boolean keyBit = newKeys.get("ASk" + i);
            if (keyBit == null) {
                System.err.println("Trying to get invalid key when creating Anti-SAT");
                break;
            }

            String outputGate = CircuitUtilities.getNewGateName();
            if (keyBit)
                newGates.add(new Gate(GateType.XNOR, outputGate, regularInputList.get(i % n), "ASk" + i));
            else
                newGates.add(new Gate(GateType.XOR, outputGate, regularInputList.get(i % n), "ASk" + i));

            if (i < n) {
                this.inputKeyMapping.put(regularInputList.get(i % n),
                        new KeyMapper("ASk" + i, keyBit ? GateType.XNOR :GateType.XOR));
                AS_inputs_A.add(outputGate);
            }
            else
                AS_inputs_B.add(outputGate);
        }

        String outputA = CircuitUtilities.getNewGateName();
        String outputB = CircuitUtilities.getNewGateName();
        String antiSATOutput = CircuitUtilities.getNewGateName();
        String newRegularOutput = CircuitUtilities.getNewGateName();
        String replacedOutput = regularOutputList.get(0);

        newGates.add(new Gate(GateType.AND, outputA, AS_inputs_A.toArray(new String[0])));      // g
        newGates.add(new Gate(GateType.NAND, outputB, AS_inputs_B.toArray(new String[0])));     // g'

        if (type == 0) {
            newGates.add(new Gate(GateType.AND, antiSATOutput, outputA, outputB));
            newGates.add(new Gate(GateType.XOR, newRegularOutput, antiSATOutput, replacedOutput));
        } else {
            newGates.add(new Gate(GateType.OR, antiSATOutput, outputA, outputB));
            newGates.add(new Gate(GateType.XNOR, newRegularOutput, antiSATOutput, replacedOutput));
        }

        if (this.getOutputNames().contains(replacedOutput))
            this.getOutputNames().remove(replacedOutput);
        else
            System.err.println("Trying to remove invalid output from regular outputs");

        this.getOutputNames().add(newRegularOutput);
        this.getGates().addAll(newGates);

        createCNF();
    }

    private void generateAntiSatKey(int size, Map<String, Boolean> newKeys) {
        SecureRandom sr = Randomizer.getSecureRandom();

        this.antisatKey = new int[size];
        for (int i = 0; i < size; i++) {
            Boolean value = sr.nextBoolean();
            newKeys.put("ASk" + i, value);
            this.antisatKey[i] = value ? 1 : 0;
        }

        this.getKeyInputNames().addAll(newKeys.keySet());
    }

    public boolean evaluateAndCheck(Collection<Literal> input, Assignment expectedOutput, boolean debugMode) {
        if (this.evaluationCircuit == null) {
            System.err.println("Unable to evaluate expected output - Evaluating circuit missing." +
                    "Try to insert AntiSAT lock to LogicCircuit.");
            return false;
        }

        return evaluateAndCompare(input, null, expectedOutput, this.evaluationCircuit, debugMode);
    }

    public boolean evaluateAndCompare(Collection<Literal> input, Collection<Literal> key,
                                    Assignment expectedOutput, boolean debugMode) {
        return evaluateAndCompare(input, key, expectedOutput, null, debugMode);
    }

    private boolean evaluateAndCompare(Collection<Literal> input, Collection<Literal> key,
                                       Assignment expectedOutput, LogicCircuit circuit, boolean debugMode) {

        LogicCircuit evalCircuit = (circuit == null) ? this : circuit;

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();

        if (key == null)
            key = evalCircuit.getKeyLiterals(ff, null);

        Collection<Variable> outputVariables = evalCircuit.getOutputVariables(ff);
        Assignment realOutput = evalCircuit.evaluate(input, key, outputVariables);

        return CircuitUtilities.arrayComparator(expectedOutput.literals(), realOutput.literals());
    }

    private Boolean checkParamsForAntiSat(int type, int n) {
        if (n < 2 || n > this.getInputNames().size()) {
            if (n < 2)
                System.err.println("Not enough inputs to AntiSAT (n = " + n + ").");
            else
                System.err.println("Too much inputs to Anti-SAT defined (n = " + n + ", inputs = " + this.getInputNames().size() + ").");
            return false;
        }

        if (type != 0 && type != 1) {
            System.err.println("Invalid AntiSAT type. Choose either AntiSAT type 0 or type 1.");
            return false;
        }

        if (this.antisatKey.length != 0) {
            System.err.println("AntiSAT lock has already been inserted into circuit.");
            return false;
        }

        return true;
    }

    public Collection<Literal> changeInputBySAS(Collection<Literal> input, Collection<Literal> keys) {

        if (CircuitUtilities.hammingWeightOfVector(input) % 2 == 0)
            return input;

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        Collection<Literal> newInputs = new ArrayList<>();

        // if input has even Hamming weight
        // than change input to negated first part of key
        for (Literal l : input) {
            KeyMapper mapper = this.inputKeyMapping.get(l.name());
            Literal relatedKey = getLiteral(keys, mapper.getKey());
            if (relatedKey == null) {
                System.err.println("Error in SAS: Unable to find variable.");
                continue;
            }

            newInputs.add(ff.literal(l.name(), (mapper.getGate() == GateType.XOR) != relatedKey.phase()));
        }

        return newInputs;
    }

    private Literal getLiteral(Collection<Literal> keys, String name) {
        for (Literal key : keys) {
            if (key.name().equals(name))
                return key;
        }
        return null;
    }

    /* Getters */

    public int[] getCorrectKey() {
        return correctKey;
    }

    public int[] getAntisatKey() {
        return antisatKey;
    }

    public int[] getCombinedKey() {
        int[] combinedKey = new int[correctKey.length + antisatKey.length];
        int index = 0;
        for (int i : correctKey) {
            combinedKey[index++] = i;
        }
        for (int i : antisatKey) {
            combinedKey[index++] = i;
        }
        return combinedKey;
    }

    public Map<String, KeyMapper> getInputKeyMapping() {
        return inputKeyMapping;
    }

    public LogicCircuit getEvaluationCircuit() {
        return evaluationCircuit;
    }

    /* Setters */

    public void setCorrectKey(int[] correctKey) {
        this.correctKey = correctKey;
    }

    public void setAntisatKey(int[] antisatKey) {
        this.antisatKey = antisatKey;
    }

    public void createEvaluationCircuit(File plainFile) {
        LogicCircuit eval = LogicCircuit.getCircuitInstance(plainFile);
        if (eval != null)
            this.evaluationCircuit = eval;
        else
            throw new IllegalArgumentException("Evaluation circuit could not be set.");
    }
}
