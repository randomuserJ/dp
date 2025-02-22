package main.circuit;

import main.helpers.FormulaFactoryWrapper;
import main.attacker.sat.SatSolverWrapper;
import main.circuit.components.Gate;
import main.circuit.components.GateType;
import main.circuit.utilities.custom_comparators.CustomKeyComparator;
import main.helpers.utilities.Protocol;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.*;
import java.util.*;

/**
 * This class contains creating and includes some annoying getters / setters methods,
 * so they don't have to be in concrete LogicCircuit class.
 */
public abstract class AbstractLogicCircuit {
    private String name;
    private final Set<String> inputNames;
    private final Set<String> keyInputNames;
    private final Set<String> outputNames;
    private List<Gate> gates;
    private Formula CNF;

    public AbstractLogicCircuit() {
        this.inputNames = new HashSet<>();
        this.keyInputNames = new HashSet<>();
        this.outputNames = new HashSet<>();
        this.gates = new ArrayList<>();
        this.name = "";
    }

    /**
     * Creates a circuit instance from .bench formatted file and computes its CNF form. Reads the
     * correct circuit key and AntiSAT key as well, if possible (if the file is locked).
     * @param benchFile logic circuit in .bench formatted file
     * @return an instance of LogicCircuit
     */
    public static LogicCircuit getCircuitInstance(File benchFile) {
        LogicCircuit ls = new LogicCircuit();
        ls.setName(benchFile.getName());

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
                        keyValues = new int[keyBits.length];
                        for (int i = 0; i < keyBits.length; i++)
                            keyValues[i] = Character.getNumericValue(keyBits[i]);

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
            Protocol.printErrorMessage("Unable to read bench file: " + ioe.getMessage());
            return null;
        } catch (Exception e) {
            Protocol.printErrorMessage(e.getMessage());
            return null;
        }

        ls.createCNF();

        return ls;
    }

    /**
     * Creates a formula in Conjunctive Normal Form and saves it as a property of logic circuit.
     */
    protected void createCNF() {
        simplifyAllGates();
        List<Formula> CNFClauses = new ArrayList<>();
        for (Gate g : this.gates) {
            try {
                CNFClauses.add(g.toFormula());
            } catch (Exception e) {
                Protocol.printErrorMessage("Unable to create CNF: " + e.getMessage());
                Protocol.printErrorMessage("Gate: " + g.toString() + " seems malformed.");
                return;
            }
        }
        this.CNF = FormulaFactoryWrapper.getFormulaFactory().and(CNFClauses);
    }

    /**
     * Decomposes every gate with more than 2 inputs. The gate's functionality have to remain.
     */
    private void simplifyAllGates() {
        List<Gate> decomposedGates = new ArrayList<>();
        for (Gate g : this.gates) {
            try {
                decomposedGates.addAll(g.simplifyGate());
            } catch (Exception e) {
                Protocol.printErrorMessage("Unable to simplify gate: " + e.getMessage());
                Protocol.printErrorMessage("Gate: " + g.toString() + " seems malformed.");
                return;
            }
        }
        this.gates = decomposedGates;
    }

    /**
     * Produces an assignment (boolean value) of either each variable (input, key, output) or just output variable.
     * Throws an exception if the CNF form of circuit is unsatisfiable (assignment does not exist).
     * @param inputLiterals input variables with defined boolean values
     * @param keyLiterals key variables with defined boolean values
     * @param outputVariables filter of output variables. If null, method returns an assignment of each variable.
     * @return an assignment (boolean value) of either each variable (input, key, output) or just output variable
     * (depending on presence of outputVariables argument)
     */
    public Assignment evaluate(Collection<Literal> inputLiterals, Collection<Literal> keyLiterals, Collection<Variable> outputVariables)
            throws IllegalArgumentException, IllegalStateException {
        if (keyLiterals != null) {
            if (this.keyInputNames.size() != keyLiterals.size()) {
                throw new IllegalArgumentException("Invalid amount of key inputs defined to evaluate.");
            }
        }

        if (this.inputNames.size() != inputLiterals.size()) {
            throw new IllegalArgumentException("Invalid amount of regular inputs defined to evaluate: " +
                    this.inputNames.size() + " vs. " + inputLiterals.size() + " (parameter of method).");
        }

        SatSolverWrapper solver = new SatSolverWrapper();
        solver.addFormula(this.CNF);

        // Assumption is a combination of input and key variables.
        Collection<Literal> assumptions = new HashSet<>(inputLiterals);
        if (keyLiterals != null) {
            assumptions.addAll(keyLiterals);
        }

        if (solver.solve(assumptions).equals(Tristate.TRUE))
            if (outputVariables == null)
                return solver.getModel();
            else
                return solver.getModel(outputVariables);


        throw new IllegalStateException("Unable to evaluate circuit.");
    }

    /* Getters */

    public String getName() {
        return name;
    }

    public Set<String> getInputNames() {
        return inputNames;
    }

    public Collection<Literal> getInputLiterals(FormulaFactory ff, int[] initValues) {
        if (initValues != null) {
            if (this.inputNames.size() != initValues.length) {
                Protocol.printErrorMessage("Incorrect size of init values for inputs. " +
                        "Got " + initValues.length + ", required " + this.inputNames.size());
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
                Protocol.printErrorMessage("Incorrect size of init values for key. " +
                        "Got " + initValues.length + ", required " + this.keyInputNames.size());
                return null;
            }
        }

        List<String> sortedKeyNames = new ArrayList<>(keyInputNames);
        sortedKeyNames.sort(new CustomKeyComparator());
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
        Collection<Variable> outputVariables = new HashSet<>();
        for (String s : outputNames) {
            outputVariables.add(ff.variable(s));
        }

        return outputVariables;
    }

    public Formula getCNF() {
        return this.CNF;
    }

    public List<Gate> getGates() { return this.gates; }

    /* Setters */

    public void setName(String name) {
        this.name = name;
    }

    /* Utilities */

    public void printCNF() {
        System.out.println("CNF: " + this.CNF.toString());
    }

    public boolean isInputVariable(Variable var) {
        return this.inputNames.contains(var.name());
    }

    public boolean isKeyVariable(Variable var) {
        return this.keyInputNames.contains(var.name());
    }

    public boolean isOutputVariable(Variable var) {
        return this.outputNames.contains(var.name());
    }

    /**
     * Finds a gate with specific name.
     * @return found gate or null if the gate with that name doesn't exists.
     */
    public Gate getSingleGate(String name) {
        for (Gate gate : this.gates) {
            if (gate.getOutput().equals(name))
                return gate;
        }
        return null;
    }
}
