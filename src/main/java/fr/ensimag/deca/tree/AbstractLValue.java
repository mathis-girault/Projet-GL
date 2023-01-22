package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.ExpDefinition;

/**
 * Left-hand side value of an assignment.
 * 
 * @author gl39
 * @date 01/01/2023
 */
public abstract class AbstractLValue extends AbstractExpr {

    /**
     * Verify the expression for contextual error.
     * 
     * implements non-terminals "lvalue"
     * of [SyntaxeContextuelle] in pass 3
     *
     * @param localEnv
     *                     Environment in which the expression should be checked
     *                     (corresponds to the "env_exp" attribute)
     */
    public abstract void verifyLValue(EnvironmentExp localEnv) throws ContextualError;

    public abstract ExpDefinition getExpDefinition();
}
