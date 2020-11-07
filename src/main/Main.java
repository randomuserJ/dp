package main;

import main.circuit.CircuitValidator;
import main.circuit.LogicCircuit;
import main.sat.FormulaFactoryWrapped;
import main.sat.SatAttackWrapped;
import main.sat.SatSolverWrapped;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.io.File;
import java.util.*;

public class Main {

    private static final String ROOT = System.getProperty("user.dir") + File.separator;
    private static final String LOCKED = "locked" + File.separator;
    private static final String NOCHAIN = LOCKED + "nochain" + File.separator;
    private static final String CIRCUITS = "circuits" + File.separator;


    public static void testing(Formula f) {
        SatSolverWrapped ss = new SatSolverWrapped();
        FormulaFactory ff = FormulaFactoryWrapped.getFormulaFactory();
        ss.addFormula(f);
        ss.solve();
        Assignment model = ss.getModel();
        System.out.println(model);

        Literal T = ff.literal("K", true);

        for (Variable variable : f.variables()) {
            if (variable.name().startsWith("I") || variable.name().startsWith("k"))
                f = ff.and(f, ff.variable(variable.name()));
        }



        System.out.println(f);

        ss.reset();
        ss.addFormula(f);
        ss.solve();

        System.out.println(ss.getModel());

    }

    public static void main(String[] args) throws Exception {

        FormulaFactory f = FormulaFactoryWrapped.getFormulaFactory();


//        File lockedFile = new File(ROOT + LOCKED + "16_c432.bench");
//        File plainFile = new File(ROOT + CIRCUITS + "c432.bench");
        File lockedFile = new File(ROOT + LOCKED + "55_c1355.bench");
        File plainFile = new File(ROOT + CIRCUITS + "c1355.bench");

        if (!CircuitValidator.validateCircuitLock(lockedFile, plainFile, 10, false)) {
            System.out.println("Incorrect lock");
        }

        LogicCircuit locked = LogicCircuit.getLogicCircuitInstance(lockedFile);
        if (locked == null) {
            System.err.println("Incorrec input file " + lockedFile.getAbsolutePath());
            return;
        }
//        locked.insertAntiSAT(0, 3, 3, true);
//        locked.writeToFile(ROOT, "cmpFile.bench", "");


//        testing(locked.getCNF());

        Assignment correctKey = new Assignment();
        for (int i = 0; i < locked.getCorrectKey().length; i++)
            correctKey.addLiteral(f.literal("k" + i, locked.getCorrectKey()[i] == 1));

        System.out.println("CNF: " + locked.getCNF());

        SatAttackWrapped attacker = new SatAttackWrapped(locked,correctKey);
        attacker.performAttack();
        Assignment estimatedKey = attacker.getEstimatedKey();

        List<Literal> parsedEstimatedKey = new ArrayList<>();
        for (Literal literal : estimatedKey.literals()) {
            parsedEstimatedKey.add(f.literal(literal.name().split("_")[0], literal.phase()));
        }
        Collections.sort(parsedEstimatedKey);
        System.out.println("Estimated key: \t\t" + parsedEstimatedKey);
        System.out.println("Inserted real key: \t" + correctKey.literals());


    }
}
