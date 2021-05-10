package main.circuit.utilities;

import main.circuit.AbstractLogicCircuit;
import main.circuit.LogicCircuit;
import main.helpers.utilities.Protocol;

import java.io.File;

public class CircuitLoader {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String LOCKED = ROOT + "locked" + File.separator;
    private static final String CIRCUITS = ROOT + "circuits" + File.separator;

    public static File loadLockedCircuitFile(int index) {
        switch (index) {
            case 1:
                return new File(LOCKED + "1_c17.bench");
            case 2:
                return new File(LOCKED + "8_c432.bench");
            case 3:
                return new File(LOCKED + "10_c499.bench");
            case 4:
                return new File(LOCKED + "16_c432.bench");
            case 5:
                return new File(LOCKED + "19_c880.bench");
            case 6:
                return new File(LOCKED + "20_c499.bench");
            case 7:
                return new File(LOCKED + "24_c432.bench");
            case 8:
                return new File(LOCKED + "27_c1355.bench");
            case 9:
                return new File(LOCKED + "30_c499.bench");
            case 10:
                return new File(LOCKED + "38_c880.bench");
            case 11:
                return new File(LOCKED + "82_c1355.bench");
            case 12:
                return new File(LOCKED + "83_c3540.bench");
            case 13:
                return new File(LOCKED + "88_c1908.bench");
            case 14:
                return new File(LOCKED + "121_c6288.bench");
            case 15:
                return new File(LOCKED + "127_c2670.bench");
            default:
                Protocol.printErrorMessage("Unable to load locked circuit with index (" + index + ")." +
                        " Please choose an index between 1 - 15.");
                return null;
        }
    }

    public static File loadValidationCircuitFile(int index) {
        switch (index) {
            case 1:
                return new File(CIRCUITS + "c17.bench");
            case 2:
            case 4:
            case 7:
                return new File(CIRCUITS + "c432.bench");
            case 3:
            case 6:
            case 9:
                return new File(CIRCUITS + "c499.bench");
            case 5:
            case 10:
                return new File(CIRCUITS + "c880.bench");
            case 8:
            case 11:
                return new File(CIRCUITS + "c1355.bench");
            case 12:
                return new File(CIRCUITS + "c3540.bench");
            case 13:
                return new File(CIRCUITS + "c1908.bench");
            case 14:
                return new File(CIRCUITS + "c6288.bench");
            case 15:
                return new File(CIRCUITS + "c2670.bench");
            default:
                Protocol.printErrorMessage("Unable to load validation circuit with index (" + index + ")." +
                        " Please choose an index between 1 - 15.");
                return null;
        }
    }

    public static LogicCircuit loadLockedCircuit(int index) {
        File lockedCircuitFile = loadLockedCircuitFile(index);
        return (lockedCircuitFile == null) ?
                null : AbstractLogicCircuit.getCircuitInstance(lockedCircuitFile);
    }

    public static LogicCircuit loadValidationCircuit(int index) {
        File validationCircuitFile = loadValidationCircuitFile(index);
        return (validationCircuitFile == null) ?
                null : AbstractLogicCircuit.getCircuitInstance(validationCircuitFile);
    }
}
