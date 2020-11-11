package main;

import main.circuit.CircuitValidator;
import main.circuit.Gate;
import main.circuit.LogicCircuit;
import main.sat.FormulaFactoryWrapped;
import main.sat.SatAttackWrapped;
import main.sat.SatSolverWrapped;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.File;
import java.security.SecureRandom;
import java.util.*;

public class Main {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String ANTISAT = ROOT + "antisatLocked" + File.separator;
    private static final String LOCKED = ROOT + "locked" + File.separator;
    private static final String NOCHAIN = ROOT + LOCKED + "nochain" + File.separator;
    private static final String CIRCUITS = ROOT + "circuits" + File.separator;


    public static void testing() {
        SatSolverWrapped ss = new SatSolverWrapped();
        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();

        File lockedFile = new File(LOCKED + "nand.bench");
        LogicCircuit locked = LogicCircuit.getLogicCircuitInstance(lockedFile);
        System.out.println(locked.getCNF());
        locked.writeToFile(LOCKED, "new_simplified_" + lockedFile.getName(), "");
    }

    public static void main(String[] args) throws Exception {

        FormulaFactory f = FormulaFactoryWrapped.getFormulaFactory();
//        testing();
//        if (true)
//            return;


//        File lockedFile = new File(LOCKED + "16_c432.bench");
//        File plainFile = new File(CIRCUITS + "c432.bench");
        File lockedFile = new File(LOCKED + "1_c17.bench");
        File plainFile = new File(CIRCUITS + "c17.bench");

        if (!CircuitValidator.validateCircuitLock(lockedFile, plainFile, 10, false)) {
            System.out.println("Incorrect lock");
        }

        LogicCircuit locked = LogicCircuit.getLogicCircuitInstance(lockedFile);
        if (locked == null) {
            System.err.println("Incorrect input file " + lockedFile.getAbsolutePath());
            return;
        }

        locked.insertAntiSAT(0, locked.getInputNames().size(), 1);
        locked.writeToFile(ANTISAT, "anti_" + lockedFile.getName(), "");
        System.out.println("AntiSat key: " + Arrays.toString(locked.getAntisatKey()));

        SecureRandom sr = new SecureRandom();
        Map<String, Integer> stats = new HashMap<>();
        Collection<Variable> filter = new ArrayList<>();
        for (Gate gate : locked.getGates()) {
            filter.add(f.variable(gate.getOutput()));
            stats.put(gate.getOutput(), 0);
        }

        for (int round = 0; round < 100; round++) {
            int[] rndInputs = new int[locked.getInputNames().size()];
            for(int i = 0; i < locked.getInputNames().size(); i++)
                rndInputs[i] = sr.nextInt() % 2;
            Collection<Literal> testInputs = locked.getInputLiterals(f, rndInputs);

            int[] combinedKey = locked.getCombinedKey();
            int[] rndKeys = new int[locked.getKeyInputNames().size()];
            for(int i = 0; i < locked.getKeyInputNames().size(); i++)
                rndKeys[i] = sr.nextInt() % 2;

            Collection<Literal> realKeys = new ArrayList<>(locked.getKeyLiterals(f, combinedKey));
            Collection<Literal> testKeys = new ArrayList<>(locked.getKeyLiterals(f, rndKeys));

            Assignment output = locked.evaluate(testInputs, testKeys, filter);
            for (Variable v : output.positiveVariables()) {
                if (stats.containsKey(v.name()))
                    stats.put(v.name(), stats.get(v.name()) + 1);
            }
            System.out.println(output);
        }

        System.out.println("\n" + stats);

//        Assignment correctKey = new Assignment();
//        for (int i = 0; i < locked.getCorrectKey().length; i++)
//            correctKey.addLiteral(f.literal("k" + i, locked.getCorrectKey()[i] == 1));
//
//        System.out.println("CNF: " + locked.getCNF());
//
//        SatAttackWrapped attacker = new SatAttackWrapped(locked,correctKey);
//        attacker.performAttack();
//        Assignment estimatedKey = attacker.getEstimatedKey();
//
//        List<Literal> parsedEstimatedKey = new ArrayList<>();
//        for (Literal literal : estimatedKey.literals()) {
//            parsedEstimatedKey.add(f.literal(literal.name().split("_")[0], literal.phase()));
//        }
//        Collections.sort(parsedEstimatedKey);
//        System.out.println("Estimated key: \t\t" + parsedEstimatedKey);
//        System.out.println("Inserted real key: \t" + correctKey.literals());


    }
}
