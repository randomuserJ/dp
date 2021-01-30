package main.circuit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;




/**
 * This class is concrete interpretation of AbstractLogicCircuit class, just for some code cleanness.
 */
public class LogicCircuit extends AbstractLogicCircuit {
    private int[] correctKey;
    private int[] antisatKey;

    private final static String AntiSatGatePrefix = "ASgat";
    private static int AntiSatGateId = 0;


    public LogicCircuit() {
        this.correctKey = new int[0];
        this.antisatKey = new int[0];
    }


    /**
     * Writes a LogicCircuit instance into .bench formatted file. First line is reserved for comments.
     * In second line will be written a correct circuit key (if the file is locked).
     * In third line will be written a correct AntiSAT key (if the file is locked with AntiSAT).
     * @param path Relative path of file.
     * @param outputFileName It is what it is.
     * @param comment Optional parameter for user's comment.
     */
    public void writeToFile(String path, String outputFileName, String comment) {
        BufferedWriter bw = null;
        File file;
        FileWriter fw;

        try {
            file = new File(path + "/" + outputFileName);
            bw = new BufferedWriter(new FileWriter(file));
            bw.write("#" + (comment != null ? comment : "without comments"));
            bw.newLine();
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
            for (String output : this.getOutputNames()) {
                bw.write("OUTPUT(" + output + ")");
                bw.newLine();
            }
            bw.newLine();
            bw.flush();
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

    public static String getNewGateName() {
        return AntiSatGatePrefix + (AntiSatGateId++);
    }

    private Boolean checkParamsForAntiSat(int type, int n, int p) {
        if (n < 2 || n > this.getInputNames().size()) {
            if (n < 2)
                System.err.println("Not enough inputs to AntiSAT (n = " + n + ").");
            else
                System.err.println("Too much inputs to Anti-SAT defined (n = " + n + ", inputs = " + this.getInputNames().size() + ").");
            return false;
        }

        if (p != 1 && p != n) {
            System.err.println("Invalid parameter p (" + p + "). Use p = 1 or p = n (for 2^n - 1)");
            return false;
        }

        if (type != 0 && type != 1) {
            System.err.println("Invalid AntiSAT type.");
            return false;
        }

        return true;
    }

    // ked p = n, tak g(x)=1 pre 2^n - 1 pripadov
    public void insertAntiSAT(int type, int n, int p) throws Exception {
        if (!checkParamsForAntiSat(type, n, p))
            return;

        SecureRandom sr = new SecureRandom();
        Map<String, Boolean> newKeys = new HashMap<>();

        for (int i = 0; i < n * 2; i++) {
            newKeys.put("ASk" + i, sr.nextInt(2) == 0);
        }

        // pridanie klucov z Anti-SAT
        this.getKeyInputNames().addAll(newKeys.keySet());

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

            String outputGate = getNewGateName();
            if (keyBit)
                newGates.add(new Gate(GateType.XNOR, outputGate, regularInputList.get(i % n), "ASk" + i));
            else
                newGates.add(new Gate(GateType.XOR, outputGate, regularInputList.get(i % n), "ASk" + i));


            if (i < n)
                AS_inputs_A.add(outputGate);
            else
                AS_inputs_B.add(outputGate);
        }

        String outputA = getNewGateName();
        String outputB = getNewGateName();

        if (p == 1) {   // g = AND for one correct output
            newGates.add(new Gate(GateType.AND, outputA, AS_inputs_A.toArray(new String[0])));      // g
            newGates.add(new Gate(GateType.NAND, outputB, AS_inputs_B.toArray(new String[0])));     // g'
        } else {        // g = OR for 2^n - 1 correct outputs
            newGates.add(new Gate(GateType.OR, outputA, AS_inputs_A.toArray(new String[0])));       // g
            newGates.add(new Gate(GateType.NOR, outputB, AS_inputs_B.toArray(new String[0])));      // g'
        }

        String antiSATOutput = getNewGateName();
        String newRegularOutput = getNewGateName();
        String replacedOutput = regularOutputList.get(0);

        if (type == 0) {
            newGates.add(new Gate(GateType.AND, antiSATOutput, outputA, outputB));
            newGates.add(new Gate(GateType.XOR, newRegularOutput, antiSATOutput, replacedOutput));
        } else {
            newGates.add(new Gate(GateType.OR, antiSATOutput, outputA, outputB));
            newGates.add(new Gate(GateType.XNOR, newRegularOutput, antiSATOutput, replacedOutput));
        }

        if (this.getOutputNames().contains(replacedOutput)) {
            this.getOutputNames().remove(replacedOutput);
        } else {
            System.err.println("Trying to remove invalid output from regular outputs");
        }

        this.getOutputNames().add(newRegularOutput);
        this.getGates().addAll(newGates);

        this.antisatKey = new int[newKeys.size()];
        for (int i = 0; i < n * 2; i++)
            this.antisatKey[i] = newKeys.get("ASk" + i) ? 1 : 0;

        createCNF();
    }

    public void setCorrectKey(int[] correctKey) {
        this.correctKey = correctKey;
    }

    public int[] getCorrectKey() {
        return (correctKey != null) ? correctKey : new int[0];
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


    public void setAntisatKey(int[] antisatKey) {
        this.antisatKey = antisatKey;
    }
}
