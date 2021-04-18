package main;

import main.attacker.CircuitAttacker;
import main.circuit.CircuitValidator;
import main.circuit.LogicCircuit;
import main.circuit.AbstractLogicCircuit;

import java.io.File;
import java.util.*;

public class Main {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String ANTISAT = ROOT + "antisatLocked" + File.separator;
    private static final String LOCKED = ROOT + "locked" + File.separator;
    private static final String NOCHAIN = ROOT + LOCKED + "nochain" + File.separator;
    private static final String CIRCUITS = ROOT + "circuits" + File.separator;


    public static void main(String[] args) {
        boolean validation = true;

        File drawnFile = new File(ANTISAT + "drawn_as_c17.bench");
//        File lockedFile = new File(LOCKED + "1_c17.bench");
//        File plainFile = new File(CIRCUITS + "c17.bench");

//        File lockedFile = new File(LOCKED + "8_c432.bench");
//        File plainFile = new File(CIRCUITS + "c432.bench");

//        File lockedFile = new File(LOCKED + "10_c499.bench");
//        File plainFile = new File(CIRCUITS + "c499.bench");

//        File lockedFile = new File(LOCKED + "16_c432.bench");
//        File plainFile = new File(CIRCUITS + "c432.bench");

//        File lockedFile = new File(LOCKED + "19_c880.bench");
//        File plainFile = new File(CIRCUITS + "c880.bench");

        File lockedFile = new File(LOCKED + "20_c499.bench");
        File plainFile = new File(CIRCUITS + "c499.bench");

//        File lockedFile = new File(LOCKED + "24_c432.bench");
//        File plainFile = new File(CIRCUITS + "c432.bench");

//        File lockedFile = new File(LOCKED + "27_c1355.bench");
//        File plainFile = new File(CIRCUITS + "c1355.bench");

//        File lockedFile = new File(LOCKED + "30_c499.bench");
//        File plainFile = new File(CIRCUITS + "c499.bench");


        /* CHECK IF FILES EXISTS */
        LogicCircuit locked = AbstractLogicCircuit.getCircuitInstance(lockedFile);
        LogicCircuit plain = AbstractLogicCircuit.getCircuitInstance(plainFile);

        if (locked == null || plain == null) {
            System.err.println("Incorrect input file " +
                    (locked == null ? lockedFile.getAbsolutePath() : plainFile.getAbsolutePath()));
            return;
        }

        /* LOCKING VALIDATION */
        if (validation) {
            if (!CircuitValidator.validateCircuitLock(lockedFile, plainFile, 10, false)) {
                System.out.println("Incorrect lock");
            }
        }

        LogicCircuit drawnCircuit = AbstractLogicCircuit.getCircuitInstance(drawnFile);
        editInputs(drawnCircuit, new File(CIRCUITS + "c17.bench"));

//        locked.printCNF();
//        plain.printCNF();

//        CircuitAttacker.performSATAttack(locked, true, false);

        locked.insertAntiSAT(0, locked.getInputNames().size());
        locked.writeToFile(ANTISAT, "as_" + lockedFile.getName(), "");
//        System.out.println("AntiSat key: " + Arrays.toString(locked.getAntisatKey()));
//
        CircuitAttacker.performSPSAttack(locked, 1000, false);
//        CircuitAttacker.performSPSAttackWithSAS(locked, 1000, false);


        /* SigAttack */
//        plain.insertAntiSAT(0, plain.getInputNames().size(), 1);
//        plain.createEvaluationCircuit(plainFile);
//        plain.writeToFile(ANTISAT, "as_" + plainFile.getName(), "");
//        System.out.println("AntiSat key: " + Arrays.toString(plain.getAntisatKey()));


//        CircuitAttacker.performSigAttack(plain, false);
//        CircuitAttacker.performSigAttack(drawnCircuit, false);

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

        drawnCircuit.createEvaluationCircuit(plain);
    }
}
