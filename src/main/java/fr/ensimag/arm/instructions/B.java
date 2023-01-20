package fr.ensimag.arm.instructions;

import fr.ensimag.pseudocode.BranchInstruction;
import fr.ensimag.pseudocode.Label;

/**
 *
 * @author Ensimag
 * @date 01/01/2023
 */
public class B extends BranchInstruction {

    public B(Label op) {
        super(op);
    }

}