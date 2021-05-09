package main.helpers;

import main.attacker.CircuitAttacker;
import main.circuit.AbstractLogicCircuit;
import main.circuit.LogicCircuit;
import main.circuit.utilities.CircuitLoader;
import main.circuit.utilities.CircuitValidator;
import main.global_utilities.Protocol;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ArgumentProcessor {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String ANTISAT = ROOT + "antisatLocked" + File.separator;

    private final List<String> argList;

    private File plainCircuitFile;
    private File lockedCircuitFile;
    private File outputFile;
    private LogicCircuit plainCircuit;
    private LogicCircuit lockedCircuit;

    private AttackType attackType;

    private int demoIndex;
    private int spsIteration;
    private int valIteration;

    private boolean saveKey;
    private boolean realKey;
    private boolean debugMode;
    private boolean validation;

    public ArgumentProcessor(String[] args) {
        this.argList = Arrays.asList(args);
        this.plainCircuitFile = null;
        this.lockedCircuitFile = null;
        this.outputFile = null;
        this.plainCircuit = null;
        this.lockedCircuit = null;
        this.attackType = AttackType.NONE;
        this.demoIndex = 0;
        this.spsIteration = 1000;
        this.valIteration = 10;
        this.saveKey = false;
        this.realKey = false;
        this.debugMode = false;
        this.validation = true;
    }

    public void processArguments() {
        loadArguments();
        loadLogicCircuits();

        if (validation) {
            if (!CircuitValidator.validateCircuitLock(
                    this.lockedCircuit, this.plainCircuit, this.valIteration, this.debugMode))
                return;
        }

        switch (this.attackType) {
            case NONE:
                return;
            case SAT:
                launchSatAttack();
                break;
            case SPS:
                launchSpsAttack();
                break;
            case SPS_WITH_SAS:
                launchSpsAttackWithSas();
                break;
            case SIG:
                launchSigAttack();
                break;
        }
    }

    private void loadArguments() {
        AtomicInteger index;
        for (index = new AtomicInteger(0); index.get() < this.argList.size(); index.incrementAndGet()) {
            String arg = this.argList.get(index.get());
            switch (arg.toLowerCase()) {
                case "-pf":
                case "-fp":
                case "-plain":
                    this.plainCircuitFile = processFileArgument(index, arg);
                    this.plainCircuit = AbstractLogicCircuit.getCircuitInstance(this.plainCircuitFile);
                    break;
                case "-lf":
                case "-fl":
                case "-locked":
                    this.lockedCircuitFile = processFileArgument(index, arg);
                    this.lockedCircuit = AbstractLogicCircuit.getCircuitInstance(this.lockedCircuitFile);
                    break;
                case "-o":
                case "-out":
                case "-output":
                    this.outputFile = processFileArgument(index, arg);
                    break;
                case "-demo":
                    this.demoIndex = processIntegerArgument(index, arg, 0);
                    break;
                case "-valit":
                    this.valIteration = processIntegerArgument(index, arg, 10);
                    break;
                case "-it":
                case "-spsit":
                    this.spsIteration = processIntegerArgument(index, arg, 1000);
                    break;
                case "-noval":
                    this.validation = false;
                    break;
                case "-debug":
                    this.debugMode = true;
                    break;
                case "-savekey":
                case "-save":
                    this.saveKey = true;
                    break;
                case "-realkey":
                case "-real":
                    this.realKey = true;
                    break;
                case "-sps":
                    this.attackType = AttackType.SPS;
                    break;
                case "-sat":
                    this.attackType = AttackType.SAT;
                    break;
                case "-sig":
                    this.attackType = AttackType.SIG;
                    break;
                case "-sas":
                    this.attackType = AttackType.SPS_WITH_SAS;
                    break;
                default:
                    Protocol.printErrorMessage("Invalid argument '" + arg + "'.");
            }
        }
    }

    private void launchSatAttack() {
        if (this.lockedCircuit == null) {
            Protocol.printErrorMessage("Locked logic circuit is required for SAT attack.");
            return;
        }
        CircuitAttacker.performSATAttack(this.lockedCircuit, true, this.debugMode);
    }

    private void launchSpsAttack() {
        if (this.lockedCircuit == null) {
            Protocol.printErrorMessage("Locked logic circuit is required for SPS attack.");
            return;
        }
        this.lockedCircuit.insertAntiSAT(0, this.lockedCircuit.getInputNames().size());
        if (this.saveKey)
            this.lockedCircuit.writeToFile(ANTISAT, "as_" + this.lockedCircuitFile.getName(), "");

        CircuitAttacker.performSPSAttack(this.lockedCircuit, this.spsIteration, this.realKey);
    }

    private void launchSpsAttackWithSas() {
        if (this.lockedCircuit == null) {
            Protocol.printErrorMessage("Locked logic circuit is required for SPS attack.");
            return;
        }
        this.lockedCircuit.insertAntiSAT(0, this.lockedCircuit.getInputNames().size());
        if (this.saveKey)
            this.lockedCircuit.writeToFile(ANTISAT, "as_" + this.lockedCircuitFile.getName(), "");

        CircuitAttacker.performSPSAttackWithSAS(this.lockedCircuit, this.spsIteration, this.realKey);
    }

    private void launchSigAttack() {
        if (this.plainCircuit == null) {
            Protocol.printErrorMessage("Plain logic circuit is required for Signature attack.");
            return;
        }
        this.plainCircuit.insertAntiSAT(0, this.plainCircuit.getInputNames().size());
        this.plainCircuit.createEvaluationCircuit(this.plainCircuitFile);
        if (this.saveKey)
            this.plainCircuit.writeToFile(ANTISAT, "as_" + this.plainCircuitFile.getName(), "");

        CircuitAttacker.performSigAttack(this.plainCircuit, true, this.debugMode);
    }

    private void loadLogicCircuits() {
        if (demoIndex != 0) {
            this.lockedCircuit = CircuitLoader.loadLockedCircuit(demoIndex);
            this.plainCircuit = CircuitLoader.loadValidationCircuit(demoIndex);
        }

        if (this.lockedCircuit != null)
            Protocol.printInfoMessage("LogicCircuit " + this.lockedCircuitFile.getName() + " loaded as locked circuit.");
        else {
            if (this.lockedCircuitFile != null) {
                Protocol.printErrorMessage("Incorrect input file " + this.lockedCircuitFile.getName());
                return;
            }
        }

        if (this.plainCircuit != null)
            Protocol.printInfoMessage("LogicCircuit " + this.plainCircuitFile.getName() + " loaded as plain circuit.");
        else {
            if (this.plainCircuitFile != null) {
                Protocol.printErrorMessage("Incorrect input file " + this.plainCircuitFile.getName());
            }
        }
    }

    private File processFileArgument(AtomicInteger index, String option) {
        File file = new File("");
        String filename = "";
        try {
            if (valueExists(index, option)) {
                filename = this.argList.get(index.get());
                file = new File(ROOT + filename);
                String canonicalPath = file.getCanonicalPath();
            }
        } catch (IOException | SecurityException e) {
            Protocol.printErrorMessage("Incorrect filename ( " + filename + ").");
        }

        return file;
    }

    private int processIntegerArgument(AtomicInteger index, String option, int defaultValue) {
        int value = defaultValue;
        String arg = "";
        try {
            if (valueExists(index, option)) {
                arg = this.argList.get(index.get());
                value = Integer.parseInt(arg);
            }
        } catch (NumberFormatException e) {
            Protocol.printErrorMessage("Incorrect format of integer value  (" + arg + ").");
        }

        return value;
    }

    private boolean valueExists(AtomicInteger index, String option) {
        if (this.argList.size() > index.incrementAndGet())
            return true;
        else {
            Protocol.printErrorMessage("Missing argument for " + option + ".");
            return false;
        }
    }
}
