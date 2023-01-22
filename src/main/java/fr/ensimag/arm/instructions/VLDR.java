package fr.ensimag.arm.instructions;

import fr.ensimag.pseudocode.BinaryInstructionDValToReg;
import fr.ensimag.pseudocode.DVal;
import fr.ensimag.pseudocode.GPRegister;
import fr.ensimag.pseudocode.ImmediateInteger;

/**
 * @author Ensimag
 * @date 01/01/2023
 */
public class VLDR extends BinaryInstructionDValToReg {

    public VLDR(DVal op1, GPRegister op2) {
        super(op1, op2);
    }

    public VLDR(int i, GPRegister r) {
        this(new ImmediateInteger(i), r);
    }

}
