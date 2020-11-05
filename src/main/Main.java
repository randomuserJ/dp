package main;

import main.circuit.CircuitValidator;
import main.circuit.LogicCircuit;
import main.sat.FormulaFactoryWrapped;
import main.sat.SatAttackWrapped;
import main.sat.SatSolverWrapped;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;

import java.io.File;
import java.util.Arrays;

public class Main {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String LOCKED = "locked" + File.separator;
    private static final String NOCHAIN = LOCKED + "nochain" + File.separator;
    private static final String CIRCUITS = "circuits" + File.separator;

    public static void main(String[] args) throws Exception {

        SatSolverWrapped ss = new SatSolverWrapped();
        FormulaFactory f = FormulaFactoryWrapped.getFormulaFactory();

        /** Vytvorenie formuly, vyhodnotenie SAT solverom a vypisanie pridelenych hodnot pre kazdu premennu **/
//        String strFormula = "(x1 | ~x5 | x4) & (~x1 | x5 | x3 | x4) & (~x3 | x4)";
//
//        try{
//            Formula formula1 = f.parse(strFormula);
//            System.out.println(formula1.cnf());
//
//            ss.addFormula(formula1);
//            System.out.println(ss.solve());
//            System.out.println(ss.getModel());
//
//            Set<Literal> literals = formula1.literals();
//
//            if (ss.solve().toString().equals("TRUE")) {
//                for (Literal literal : literals) {
//                    System.out.println(literal + " - " + ss.getModel().evaluateLit(literal));
//                }
//            }
//
//        }catch(Exception e){
//            e.printStackTrace();
//        }

//        File lockedFile = new File(ROOT + LOCKED + "16_c432.bench");
//        File plainFile = new File(ROOT + CIRCUITS + "c432.bench");
        File lockedFile = new File(ROOT + LOCKED + "6_c17.bench");
        File plainFile = new File(ROOT + CIRCUITS + "c17.bench");

        if (!CircuitValidator.validateCircuitLock(lockedFile, plainFile, 10, false)) {
            System.out.println("Incorrect lock");
        }

        LogicCircuit locked = LogicCircuit.getLogicCircuitInstance(lockedFile);
//        locked.insertAntiSAT(0, 3, 3, true);
//        locked.writeToFile(ROOT, "cmpFile.bench", "");

        Assignment correctKey = new Assignment();
        for (int i = 0; i < locked.getCorrectKey().length; i++) {
            correctKey.addLiteral(f.literal("k" + i, locked.getCorrectKey()[i] == 1));
        }

        System.out.println(locked.getCNF());

        SatAttackWrapped attacker = new SatAttackWrapped(locked,correctKey);
        Assignment estimatedKey = attacker.performAttack();

        System.out.println("Estimated key: " + estimatedKey.literals());
        System.out.println("Inserted real key: " + correctKey.literals());
    }
}
