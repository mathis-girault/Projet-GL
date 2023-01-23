package fr.ensimag.superInstructions;

import fr.ensimag.pseudocode.GPRegister;
import fr.ensimag.pseudocode.Instruction;


/**
 * Class used to send the SLE instruction depending on wether we compile in ARM
 * or IMA.
 *
 * @author Ensimag
 * @date 01/01/2023
 */
public class SuperSLE {

    public static Instruction main(GPRegister op, boolean arm) {
        if (arm) {
            op = op.convertToArmRegister();
            return new fr.ensimag.arm.instructions.SLE(op);
        } else {
            return new fr.ensimag.ima.instructions.SLE(op);
        }
    }
}
