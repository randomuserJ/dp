package main.circuit;

import main.helpers.FormulaFactoryWrapper;
import main.circuit.components.Gate;
import main.circuit.components.GateType;
import main.circuit.utilities.CircuitUtilities;
import main.helpers.utilities.Protocol;
import main.helpers.utilities.Randomizer;
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
 * This class is concrete implementation of AbstractLogicCircuit class, just for some code cleanness.
 */
public class LogicCircuit extends AbstractLogicCircuit {
    private int[] correctKey;
    private int[] antisatKey;
    private String antisatGate;
    private LogicCircuit evaluationCircuit;
    private final Map<String, KeyMapper> inputKeyMapping;

    protected LogicCircuit() {
        this.correctKey = new int[0];
        this.antisatKey = new int[0];
        this.antisatGate = "";
        this.inputKeyMapping = new HashMap<>();
    }

    /**
     * Writes a LogicCircuit instance into .bench formatted file. First line is reserved for comments.
     * In the second line will be written a correct circuit key (if the file is locked).
     * In the third line will be written a correct AntiSAT key (if the file is locked with AntiSAT).
     * Other lines are intended for circuit components, such as input / output variables and gates.
     * @param path relative path of file
     * @param outputFileName it is what it is :)
     * @param comment optional parameter for user's comment
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

            Protocol.printInfoMessage("Logic circuit was written into " + file.getName() + " file.");

        } catch (IOException ioe) {
            Protocol.printErrorMessage("Writing logic circuit to file: " + ioe.getMessage());
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    Protocol.printErrorMessage("Closing writer: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Writes the correct value of the key and the antisat key into the buffer.
     */
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

    /**
     * Writes every input variable of this logic circuit into the buffer.
     */
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

    /**
     * Writes every ouput variable of this logic circuit into the buffer.
     */
    private void addOutputsToBuffer(BufferedWriter bw) throws IOException {
        for (String output : this.getOutputNames()) {
            bw.write("OUTPUT(" + output + ")");
            bw.newLine();
        }
        bw.newLine();
        bw.flush();
    }

    /**
     * Writes every gate of this logic circuit into the buffer.
     * The string representation of gate follows the .bench format.
     * E.g. G1 = and(I1, I2)
     */
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
                Protocol.printWarningMessage("Trying to get invalid key when creating Anti-SAT.");
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
        this.antisatGate = antiSATOutput;

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
            Protocol.printWarningMessage("Trying to remove invalid output from regular outputs.");

        this.getOutputNames().add(newRegularOutput);
        this.getGates().addAll(newGates);

        createCNF();
    }

    /**
     * Checks the correctness of necessary parameters.
     * @param type type of AntiSAT lock
     * @param n number of input that should be bounded by the AntiSAT gates
     * @return true if all parameters are valid, false otherwise
     */
    private boolean checkParamsForAntiSat(int type, int n) {
        if (n < 2 || n > this.getInputNames().size()) {
            if (n < 2)
                Protocol.printErrorMessage("Not enough inputs to AntiSAT (n = " + n + ").");
            else
                Protocol.printErrorMessage("Too much inputs to Anti-SAT defined " +
                        "(n = " + n + ", inputs = " + this.getInputNames().size() + ").");
            return false;
        }

        if (type != 0 && type != 1) {
            Protocol.printErrorMessage("Invalid AntiSAT type. Choose either AntiSAT type 0 or type 1.");
            return false;
        }

        if (this.antisatKey.length != 0) {
            Protocol.printErrorMessage("AntiSAT lock has already been inserted into circuit.");
            return false;
        }

        return true;
    }

    /**
     * Fills the newKeys object with randomly generated antisat key.
     * @param size the length of antisat key
     * @param newKeys object where the new antisat key should be stored
     */
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

    /**
     * Loads the input vector into the validation circuit and checks if the correct output matches the expected one.
     * @param input collection of input literals
     * @param expectedOutput output which should be validated
     * @param debugMode true for detail information (intended for development purposes)
     * @return true if the output of validation circuit for specific input is the same as expected output
     */
    public boolean evaluateAndCheck(Collection<Literal> input, Assignment expectedOutput, boolean debugMode) {
        if (this.evaluationCircuit == null) {
            Protocol.printErrorMessage("Unable to evaluate expected output. Evaluating circuit is missing.");
            return false;
        }

        return evaluateAndCompare(input, null, expectedOutput, this.evaluationCircuit, debugMode);
    }

    /**
     * Loads the input vector and the specific key into the validation circuit and
     * checks if the correct output matches the expected one.
     * @param input collection of input literals
     * @param key collection of key literals
     * @param expectedOutput output which should be validated
     * @param debugMode true for detail information (intended for development purposes)
     * @return true if the output of validation circuit for specific input is the same as expected output
     */
    public boolean evaluateAndCompare(Collection<Literal> input, Collection<Literal> key,
                                    Assignment expectedOutput, boolean debugMode) {
        return evaluateAndCompare(input, key, expectedOutput, null, debugMode);
    }

    /**
     * Loads the input vector and the specific key into the validation circuit and
     * checks if the correct output matches the expected one. If the key wasn't defined, loads the
     * correct key from validation circuit.
     * @param input collection of input literals
     * @param key collection of key literals
     * @param expectedOutput output which should be validated
     * @param debugMode true for detail information (intended for development purposes)
     * @return true if the output of validation circuit for specific input is the same as expected output
     */
    private boolean evaluateAndCompare(Collection<Literal> input, Collection<Literal> key,
                                       Assignment expectedOutput, LogicCircuit circuit, boolean debugMode) {

        LogicCircuit evalCircuit = (circuit == null) ? this : circuit;

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();

        if (key == null)
            key = evalCircuit.getKeyLiterals(ff, null);

        Collection<Variable> outputVariables = evalCircuit.getOutputVariables(ff);
        Assignment realOutput = evalCircuit.evaluate(input, key, outputVariables);

        return CircuitUtilities.compareOutputs(expectedOutput.literals(), realOutput.literals());
    }

    /**
     * Simulates the Strong-AntiSAT protection. Changes the input values so the complementary
     * antisat block 'g' will be activated.
     * @return the collection of literals with adjusted values
     */
    public Collection<Literal> changeInputBySAS(Collection<Literal> input, Collection<Literal> keys) {

        if (CircuitUtilities.hammingWeightOfVector(input) % 2 == 0)
            return input;

        FormulaFactory ff = FormulaFactoryWrapper.getFormulaFactory();
        Collection<Literal> newInputs = new ArrayList<>();

        // if input has even Hamming weight
        // than change input to negated first part of key
        for (Literal l : input) {
            KeyMapper mapper = this.inputKeyMapping.get(l.name());
            Literal relatedKey = findLiteralWithName(keys, mapper.getKey());
            if (relatedKey == null) {
                Protocol.printWarningMessage("SAS: Unable to find variable " + l.name() + ".");
                continue;
            }

            newInputs.add(ff.literal(l.name(), (mapper.getGate() == GateType.XOR) != relatedKey.phase()));
        }

        return newInputs;
    }

    /**
     * Searches through the keys collection and checks if there is a literal with specific name.
     * @return found instance of Literal or null if the literal is not in keys collection.
     */
    private Literal findLiteralWithName(Collection<Literal> keys, String name) {
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

    public String getAntisatGate() {
        return antisatGate;
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
