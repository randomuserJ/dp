package main.attacker;

import main.attacker.sat.SatAttackWrapper;
import main.attacker.sig.SigAttackWrapper;
import main.attacker.sps.KeySetType;
import main.attacker.sps.SpsAttackWrapper;
import main.circuit.LogicCircuit;
import main.helpers.utilities.Protocol;

public class CircuitAttacker {

    /**
     * Wrapper method for SAT Attack on unlocked logic circuit. Attacking circuit locked with AntiSAT
     * is not implemented so far.
     * @param circuit instance of plain (activated) LogicCircuit
     * @param printKeyInfo true for comparing estimated key with the real one
     * @param debugMode true for detail information (intended for development purposes)
     */
    public static void performSATAttack(LogicCircuit circuit, boolean printKeyInfo, boolean debugMode) {
        SatAttackWrapper attacker = new SatAttackWrapper(circuit);
        try {
            attacker.performSATAttack(debugMode);
            if (printKeyInfo)
                attacker.printKeyStats();
        } catch (IllegalStateException | IllegalArgumentException e) {
            Protocol.printErrorMessage("Error performing SAT attack: " + e.getMessage());
        }
    }

    /**
     * Wrapper method for Sig attack on circuit locked with AntiSAT.
     * @param locked instance of LogicCircuit locked with AntiSAT
     * @param printKeyInfo true for evaluate estimation correctness of key
     * @param debugMode true for detail information (intended for development purposes)
     */
    public static void performSigAttack(LogicCircuit locked, boolean printKeyInfo, boolean debugMode) {
        SigAttackWrapper attacker = new SigAttackWrapper(locked);
        try {
            attacker.performSigAttack(printKeyInfo, debugMode);
        } catch (IllegalStateException e) {
            Protocol.printErrorMessage("Error performing Sig attack: " + e.getMessage());
        }
    }

    /**
     * Wrapper method for SPS attack on circuit locked with AntiSAT.
     * @param locked instance of LogicCircuit locked with AntiSAT
     * @param rounds number of rounds for SPS statistical testing
     * @param realKeys true, if we want to use only correct key while calculating attack statistics
     */
    public static void performSPSAttack(LogicCircuit locked, int rounds, boolean realKeys) {
        SpsAttackWrapper attacker = new SpsAttackWrapper(rounds, realKeys ? KeySetType.REAL : null, true);
        attacker.setLockedCircuit(locked);
        attacker.performSPSAttack();
    }

    /**
     * Wrapper method for SPS attack on circuit locked with StrongAntiSAT.
     * @param locked instance of LogicCircuit locked with AntiSAT. Since we don't have a possibility to create
     *               StrongAntiSAT lock, we have to simulate this attack by software.
     * @param rounds number of rounds for SPS statistical testing
     * @param realKeys true, if we want to use only correct key while calculating attack statistics
     */
    public static void performSPSAttackWithSAS(LogicCircuit locked, int rounds, boolean realKeys) {
        SpsAttackWrapper attacker = new SpsAttackWrapper(rounds, realKeys ? KeySetType.REAL : null, true);
        attacker.setLockedCircuit(locked);
        attacker.simulateSASLock();
        attacker.performSPSAttack();
    }
}
