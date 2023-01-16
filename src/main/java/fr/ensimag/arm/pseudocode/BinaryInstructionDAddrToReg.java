package fr.ensimag.arm.pseudocode;

/**
 * Base class for instructions with 2 operands, the first being a
 * DAddr, and the second a Register.
 *
 * @author Ensimag
 * @date 01/01/2023
 */
public class BinaryInstructionDAddrToReg extends BinaryInstructionDValToReg {

    public BinaryInstructionDAddrToReg(DAddr op1, GPRegister op2) {
        super(op1, op2);
    }

}
