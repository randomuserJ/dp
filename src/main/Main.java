package main;

import main.attacker.CircuitAttacker;
import main.circuit.CircuitValidator;
import main.circuit.LogicCircuit;
import main.circuit.AbstractLogicCircuit;
import main.attacker.sat.FormulaFactoryWrapped;
import main.attacker.sat.SatSolverWrapped;
import org.logicng.formulas.FormulaFactory;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

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
        LogicCircuit locked = AbstractLogicCircuit.getCircuitInstance(lockedFile);
        System.out.println(locked.getCNF());
        locked.writeToFile(LOCKED, "new_simplified_" + lockedFile.getName(), "");
    }

    public static void main(String[] args) {

        FormulaFactory f = FormulaFactoryWrapped.getFormulaFactory();
//        testing();
//        if (true)
//            return;


//        File lockedFile = new File(LOCKED + "1_c17.bench");
//        File plainFile = new File(CIRCUITS + "c17.bench");
        File drawnFile = new File(ANTISAT + "drawn_as_c17.bench");

        File lockedFile = new File(LOCKED + "8_c432.bench");
        File plainFile = new File(CIRCUITS + "c432.bench");

//        File lockedFile = new File(LOCKED + "10_c499.bench");
//        File plainFile = new File(CIRCUITS + "c499.bench");

//        File lockedFile = new File(LOCKED + "16_c432.bench");
//        File plainFile = new File(CIRCUITS + "c432.bench");

//        File lockedFile = new File(LOCKED + "19_c880.bench");
//        File plainFile = new File(CIRCUITS + "c880.bench");

//        File lockedFile = new File(LOCKED + "20_c499.bench");
//        File plainFile = new File(CIRCUITS + "c499.bench");

//        File lockedFile = new File(LOCKED + "24_c432.bench");
//        File plainFile = new File(CIRCUITS + "c432.bench");

//        File lockedFile = new File(LOCKED + "27_c1355.bench");
//        File plainFile = new File(CIRCUITS + "c1355.bench");

//        File lockedFile = new File(LOCKED + "30_c499.bench");
//        File plainFile = new File(CIRCUITS + "c499.bench");


        /* CHECK IF FILE EXISTS */
        LogicCircuit locked = AbstractLogicCircuit.getCircuitInstance(lockedFile);
        if (locked == null) {
            System.err.println("Incorrect input file " + lockedFile.getAbsolutePath());
            return;
        }

        /* LOCKING VALIDATION */
        if (!CircuitValidator.validateCircuitLock(lockedFile, plainFile, 10, false)) {
            System.out.println("Incorrect lock");
        }

        LogicCircuit plain = AbstractLogicCircuit.getCircuitInstance(plainFile);

        locked.printCNF();
        plain.printCNF();

//        CircuitAttacker.performSATAttack(locked, false, true);

//        locked.insertAntiSAT(0, locked.getInputNames().size(), 1);
//        locked.writeToFile(ANTISAT, "as_" + lockedFile.getName(), "");
//        System.out.println("AntiSat key: " + Arrays.toString(locked.getAntisatKey()));

        plain.insertAntiSATWithCopy(0, plain.getInputNames().size(), 1, plainFile);
        plain.writeToFile(ANTISAT, "as_" + plainFile.getName(), "");
        System.out.println("AntiSat key: " + Arrays.toString(plain.getAntisatKey()));

//        CircuitAttacker.performSPSAttack(locked, 1);

        LogicCircuit drawnCircuit = LogicCircuit.getCircuitInstance(drawnFile);
        editInputs(drawnCircuit, new File(CIRCUITS + "c17.bench"));

//        CircuitAttacker.performSPSAttack(drawnCircuit, 1000);
//        CircuitAttacker.performSigAttack(plain);
        CircuitAttacker.performSigAttack(drawnCircuit);
    }

    private static void editInputs(LogicCircuit drawnCircuit, File plain) {
        List<String> inputsToRemove = new ArrayList<>();
        for (String inputName : drawnCircuit.getInputNames()) {
            if (inputName.startsWith("ASk"))
                inputsToRemove.add(inputName);
        }
        for (String s : inputsToRemove) {
            drawnCircuit.getInputNames().remove(s);
            drawnCircuit.getKeyInputNames().add(s);
        }

        drawnCircuit.evaluationCircuit = AbstractLogicCircuit.getCircuitInstance(plain);
    }
}
