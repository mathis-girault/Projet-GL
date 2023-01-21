package fr.ensimag.arm.instructions;

import java.io.PrintStream;

import fr.ensimag.pseudocode.InstructionArm;

/**
 * @author Ensimag
 * @date 01/01/2023
 */
public class HALT extends InstructionArm {

    @Override
    public void displayInstructions(PrintStream s) {
        s.println("mov r7, #1");
        s.println("swi 0");
        s.println("\t" + "bx lr");
    }
}
