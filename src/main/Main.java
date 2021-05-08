package main;

import main.attacker.CircuitAttacker;
import main.circuit.CircuitValidator;
import main.circuit.LogicCircuit;
import main.circuit.AbstractLogicCircuit;
import main.circuit.utilities.CircuitLoader;
import main.global_utilities.FormulaFactoryWrapper;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String ANTISAT = ROOT + "antisatLocked" + File.separator;

    public static void main(String[] args) {
        boolean validation = false;
        int logicCircuitIndex = 15;

        /* LOAD FILES CONTAINING LOGIC CIRCUIT */
        File lockedFile = CircuitLoader.loadLockedCircuit(logicCircuitIndex);
        File plainFile = CircuitLoader.loadValidationCircuit(logicCircuitIndex);

        /* CHECK IF FILES ARE IN GOOD FORMAT */
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

//        locked.printCNF();
//        plain.printCNF();

        CircuitAttacker.performSATAttack(locked, true, false);

//        locked.insertAntiSAT(0, locked.getInputNames().size());
//        locked.writeToFile(ANTISAT, "as_" + lockedFile.getName(), "");
//        System.out.println("AntiSat key: " + Arrays.toString(locked.getAntisatKey()));
//
//        CircuitAttacker.performSPSAttack(locked, 100, false);
//        CircuitAttacker.performSPSAttackWithSAS(locked, 1000, false);
//
//

//        File sigFile = CircuitLoader.loadSigCircuit(Integer.parseInt("10"));
//        LogicCircuit sig = AbstractLogicCircuit.getCircuitInstance(sigFile);

//        /* SigAttack */
//        sig.insertAntiSAT(0, sig.getInputNames().size());

//        sig.createEvaluationCircuit(sigFile);
//        sig.writeToFile(ANTISAT, "as_" + sigFile.getName(), "");
//        System.out.println("AntiSat key: " + Arrays.toString(plain.getAntisatKey()));
//        CircuitAttacker.performSigAttack(sig, true, false);

    }
}
