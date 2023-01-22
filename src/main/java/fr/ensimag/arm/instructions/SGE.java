package fr.ensimag.arm.instructions;

import java.io.PrintStream;

import fr.ensimag.pseudocode.GPRegister;
import fr.ensimag.pseudocode.InstructionArm;

/**
 *
 * @author Ensimag
 * @date 01/01/2023
 */
public class SGE extends InstructionArm {

    private GPRegister op;

    public SGE(GPRegister op) {
        this.op = op;
    }

    @Override
    public void displayInstructions(PrintStream s) {
        s.println("movge " + op + ", #1");
        s.println("movlt " + op + ", #0");
    }

}