package main.helpers;

import main.attacker.CircuitAttacker;
import main.circuit.AbstractLogicCircuit;
import main.circuit.LogicCircuit;
import main.circuit.utilities.CircuitLoader;
import main.circuit.utilities.CircuitValidator;
import main.helpers.utilities.Protocol;

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
    private LogicCircuit plainCircuit;
    private LogicCircuit lockedCircuit;

    private AttackType attackType;

    private int demoIndex;
    private int spsIteration;
    private int valIteration;

    private boolean save;
    private boolean realKey;
    private boolean debugMode;
    private boolean validation;

    public ArgumentProcessor(String[] args) {
        this.argList = Arrays.asList(args);
        this.plainCircuitFile = null;
        this.lockedCircuitFile = null;
        this.plainCircuit = null;
        this.lockedCircuit = null;
        this.attackType = AttackType.NONE;
        this.demoIndex = 0;
        this.spsIteration = 1000;
        this.valIteration = 10;
        this.save = false;
        this.realKey = false;
        this.debugMode = false;
        this.validation = true;
    }

    /**
     * Reads every input arguments and performs specific operation.
     * Some arguments require a following value for them.
     * The list of accepted arguments are:
     * <p> -pf, -fp, -plain [PATH] = load the plain logic circuit from .bench file in [PATH] </p>
     * <p> -lf, -fl, -locked [PATH] = load the locked logic circuit from .bench file in [PATH] </p>
     * <p> -demo [INT] = load both files from the pre-defined .bench files </p>
     * <p> -sat = perform SAT attack on a locked circuit </p>
     * <p> -sps = perform SSP attack on a locked circuit </p>
     * <p> -sas = perform SPS attack on a locked circuit protected by simulated SAS protection </p>
     * <p> -sig = perform Signature attack on a plain circuit </p>
     * <p> -save, -savefile = save the logic circuit locked with AntiSAT </p>
     * <p> -noval = suppress the circuit locking validation </p>
     * <p> -valit [INT] = set the count of iterations for circuit locking validation </p>
     * <p> -spsit, -it [INT] = set the count of iterations for SPS attack </p>
     * <p> -real, -realkey = use correct keys for SPS attack </p>
     * <p> -debug = enable statement messages (intended for development purposes) </p>
     */
    public void processArguments() {
        Protocol.printSection("");
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

    /**
     * Reads each input argument from user and tries to save it, if it's in correct format.
     * If the value for some argument cannot be read or is in incorrect format, the
     * default option value will be loaded.
     */
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
                case "-savefile":
                case "-save":
                    this.save = true;
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

    /**
     * Initializes and launches the SAT attack, if the argument -sat was defined.
     */
    private void launchSatAttack() {
        if (this.lockedCircuit == null) {
            Protocol.printErrorMessage("Locked logic circuit is required for SAT attack.");
            return;
        }
        CircuitAttacker.performSATAttack(this.lockedCircuit, true, this.debugMode);
    }

    /**
     * Inserts the AntiSAT protection into a locked logic circuit,
     * initializes and launches the SPS attack, if the argument -sps was defined.
     */
    private void launchSpsAttack() {
        if (this.lockedCircuit == null) {
            Protocol.printErrorMessage("Locked logic circuit is required for SPS attack.");
            return;
        }
        this.lockedCircuit.insertAntiSAT(0, this.lockedCircuit.getInputNames().size());
        if (this.save)
            this.lockedCircuit.writeToFile(ANTISAT, "as_" + this.lockedCircuitFile.getName(), "");

        CircuitAttacker.performSPSAttack(this.lockedCircuit, this.spsIteration, this.realKey);
    }

    /**
     * Inserts the AntiSAT protection into a locked logic circuit,
     * initializes and launches the SPS attack with simulated SAS protection, if the argument -sas was defined.
     */
    private void launchSpsAttackWithSas() {
        if (this.lockedCircuit == null) {
            Protocol.printErrorMessage("Locked logic circuit is required for SPS attack.");
            return;
        }
        this.lockedCircuit.insertAntiSAT(0, this.lockedCircuit.getInputNames().size());
        if (this.save)
            this.lockedCircuit.writeToFile(ANTISAT, "as_" + this.lockedCircuitFile.getName(), "");

        CircuitAttacker.performSPSAttackWithSAS(this.lockedCircuit, this.spsIteration, this.realKey);
    }

    /**
     * Inserts the AntiSAT protection into a plain logic circuit,
     * initializes and launches the SigAttack, if the argument -sig was defined.
     */
    private void launchSigAttack() {
        if (this.plainCircuit == null) {
            Protocol.printErrorMessage("Plain logic circuit is required for Signature attack.");
            return;
        }
        this.plainCircuit.insertAntiSAT(0, this.plainCircuit.getInputNames().size());
        this.plainCircuit.createEvaluationCircuit(this.plainCircuitFile);
        if (this.save)
            this.plainCircuit.writeToFile(ANTISAT, "as_" + this.plainCircuitFile.getName(), "");

        CircuitAttacker.performSigAttack(this.plainCircuit, true, this.debugMode);
    }

    /**
     * Tries to create an instances of plain and locked LogicCircuit from the .bench file on defined path.
     * If the argument -demo [INT] is present, loads the instances from the CircuitLoader.
     */
    private void loadLogicCircuits() {
        if (demoIndex != 0) {
            this.lockedCircuitFile = CircuitLoader.loadLockedCircuitFile(demoIndex);
            this.plainCircuitFile = CircuitLoader.loadValidationCircuitFile(demoIndex);
            if (this.lockedCircuitFile != null)
                this.lockedCircuit = CircuitLoader.loadLockedCircuit(demoIndex);
            if (this.plainCircuitFile != null)
                this.plainCircuit = CircuitLoader.loadValidationCircuit(demoIndex);
        }

        if (this.lockedCircuit != null)
            Protocol.printInfoMessage("LogicCircuit " + this.lockedCircuit.getName() + " loaded as locked circuit.");
        else {
            if (this.lockedCircuitFile != null) {
                Protocol.printErrorMessage("Incorrect input file " + this.lockedCircuitFile.getName());
                return;
            }
        }

        if (this.plainCircuit != null)
            Protocol.printInfoMessage("LogicCircuit " + this.plainCircuit.getName() + " loaded as plain circuit.");
        else {
            if (this.plainCircuitFile != null) {
                Protocol.printErrorMessage("Incorrect input file " + this.plainCircuitFile.getName());
            }
        }
    }

    /**
     * Parses the argument in the following position and creates a file path from it.
     * @param index the index of current argument (either -lf or -pf)
     * @param option current processing argument
     * @return a new File instance with specific path or an empty File instance,
     * if the path format was invalid
     */
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
            Protocol.printErrorMessage("Incorrect filename (" + filename + ").");
        }

        return file;
    }

    /**
     * Parses the argument in the following position and creates and integer from it.
     * @param index the index of current argument (either -lf or -pf)
     * @param option current processing argument
     * @param defaultValue default value for current argument
     * @return an integer value for specific argument or default value if the value from
     * following argument has incorrect format
     */
    private int processIntegerArgument(AtomicInteger index, String option, int defaultValue) {
        int value = defaultValue;
        String arg = "";
        try {
            if (valueExists(index, option)) {
                arg = this.argList.get(index.get());
                value = Integer.parseInt(arg);
                if (value < 0) {
                    Protocol.printErrorMessage("Incorrect value (" + arg + "). Please enter a positive number.");
                    value = defaultValue;
                }
            }
        } catch (NumberFormatException e) {
            Protocol.printErrorMessage("Incorrect format of integer value (" + arg + ").");
        }

        return value;
    }

    /**
     * Checks whether the required value for argument exists.
     */
    private boolean valueExists(AtomicInteger index, String option) {
        if (this.argList.size() > index.incrementAndGet())
            return true;
        else {
            Protocol.printErrorMessage("Missing argument for " + option + ".");
            return false;
        }
    }
}
