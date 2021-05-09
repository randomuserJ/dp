package main;

import main.circuit.utilities.CircuitValidator;
import main.circuit.LogicCircuit;
import main.circuit.AbstractLogicCircuit;
import main.circuit.utilities.CircuitLoader;
import main.global_utilities.ArgumentProcessor;
import main.global_utilities.Protocol;

import java.io.File;
import java.util.Arrays;

public class Main {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String ANTISAT = ROOT + "antisatLocked" + File.separator;

    public static void main(String[] args) {
        boolean validation = true;
        int logicCircuitIndex = 2;

        ArgumentProcessor processor = new ArgumentProcessor(args);
        processor.processArguments();




        /* LOAD FILES CONTAINING LOGIC CIRCUIT */
//        File lockedFile = CircuitLoader.loadLockedCircuit(logicCircuitIndex);
//        File plainFile = CircuitLoader.loadValidationCircuit(logicCircuitIndex);
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
//        if (validation) {
//            if (!CircuitValidator.validateCircuitLock(lockedFile, plainFile, 10, false)) {
//                Protocol.printWarningMessage("Circuit validation: Incorrect lock");
//            }
//        }

//        locked.printCNF();
//        plain.printCNF();

//        CircuitAttacker.performSATAttack(locked, true, false);

//        locked.insertAntiSAT(0, locked.getInputNames().size());
//        locked.writeToFile(ANTISAT, "as_" + lockedFile.getName(), "");
//        System.out.println("AntiSat key: " + Arrays.toString(locked.getAntisatKey()));
//
//        CircuitAttacker.performSPSAttack(locked, 1000, false);
//        CircuitAttacker.performSPSAttackWithSAS(locked, 1000, false);


//        /* SigAttack */
//        plain.insertAntiSAT(0, plain.getInputNames().size());
//
//        plain.createEvaluationCircuit(plainFile);
//        plain.writeToFile(ANTISAT, "as_" + plainFile.getName(), "");
////        System.out.println("AntiSat key: " + Arrays.toString(plain.getAntisatKey()));
//        CircuitAttacker.performSigAttack(plain, true, false);

    }
}
