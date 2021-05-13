package main;

import main.attacker.CircuitAttacker;
import main.circuit.AbstractLogicCircuit;
import main.circuit.LogicCircuit;
import main.circuit.utilities.CircuitLoader;
import main.circuit.utilities.CircuitValidator;
import main.helpers.ArgumentProcessor;
import main.helpers.utilities.Protocol;

import java.io.File;
import java.util.Arrays;

public class Main {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String ANTISAT = ROOT + "antisatLocked" + File.separator;

    public static void main(String[] args) {

        ArgumentProcessor processor = new ArgumentProcessor(args);
        processor.processArguments();


        /* LOAD FILES CONTAINING LOGIC CIRCUIT */
//        File lockedFile = CircuitLoader.loadLockedCircuitFile(2);
//        File plainFile = CircuitLoader.loadValidationCircuitFile(2);
//
//        if (lockedFile == null || plainFile == null) {
//            Protocol.printErrorMessage("Incorrect input file");
//            return;
//        }
//
//        /* CHECK IF FILES ARE IN GOOD FORMAT */
//        LogicCircuit locked = AbstractLogicCircuit.getCircuitInstance(lockedFile);
//        LogicCircuit plain = AbstractLogicCircuit.getCircuitInstance(plainFile);
//
//        if (locked == null || plain == null) {
//            Protocol.printErrorMessage("Incorrect input file " +
//                    (locked == null ? lockedFile.getAbsolutePath() : plainFile.getAbsolutePath()));
//            return;
//        }
//
//        /* LOCKING VALIDATION */
//        if (!CircuitValidator.validateCircuitLock(locked, plain, 10, false)) {
//            Protocol.printWarningMessage("Circuit validation: Incorrect lock");
//        }

//        locked.printCNF();
//        plain.printCNF();
//
//        /* SAT attack */
//        CircuitAttacker.performSATAttack(locked, true, false);


//        /* SPS Attack */
//        locked.insertAntiSAT(0, locked.getInputNames().size());
//        locked.writeToFile(ANTISAT, "as_" + lockedFile.getName(), "");
//        System.out.println("AntiSat key: " + Arrays.toString(locked.getAntisatKey()));
//
//        CircuitAttacker.performSPSAttack(locked, 1000, false);
//        CircuitAttacker.performSPSAttackWithSAS(locked, 1000, false);


//        /* SigAttack */
//        plain.insertAntiSAT(0, plain.getInputNames().size());
//        plain.createEvaluationCircuit(plainFile);
//        plain.writeToFile(ANTISAT, "as_" + plainFile.getName(), "");
////        System.out.println("AntiSat key: " + Arrays.toString(plain.getAntisatKey()));
//
//        CircuitAttacker.performSigAttack(plain, true, false);

    }
}
