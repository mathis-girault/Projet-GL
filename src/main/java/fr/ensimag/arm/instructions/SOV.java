package fr.ensimag.arm.instructions;

import fr.ensimag.pseudocode.GPRegister;
import fr.ensimag.pseudocode.UnaryInstructionToReg;

/**
 *
 * @author Ensimag
 * @date 01/01/2023
 */
public class SOV extends UnaryInstructionToReg {

    public SOV(GPRegister op) {
        super(op);
    }

}

// ici jsp pas tester l'overflow...