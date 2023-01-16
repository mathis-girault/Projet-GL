package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.tools.DecacInternalError;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.ImmediateFloat;
import fr.ensimag.ima.pseudocode.ImmediateInteger;
import fr.ensimag.ima.pseudocode.Instruction;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.instructions.WINT;
import fr.ensimag.ima.pseudocode.instructions.WSTR;
import fr.ensimag.ima.pseudocode.instructions.LOAD;
import fr.ensimag.ima.pseudocode.instructions.WFLOAT;
import fr.ensimag.ima.pseudocode.instructions.WFLOATX;

import java.io.PrintStream;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

/**
 * Expression, i.e. anything that has a value.
 *
 * @author gl39
 * @date 01/01/2023
 */
public abstract class AbstractExpr extends AbstractInst {
    private static final Logger LOG = Logger.getLogger(AbstractExpr.class);

    /**
     * @return true if the expression does not correspond to any concrete token
     * in the source code (and should be decompiled to the empty string).
     */
    boolean isImplicit() {
        return false;
    }

    /**
     * Get the type decoration associated to this expression (i.e. the type computed by contextual verification).
     */
    public Type getType() {
        return type;
    }

    protected void setType(Type type) {
        Validate.notNull(type);
        this.type = type;
    }
    private Type type;


    /**
     * Verify the expression for contextual error.
     * 
     * implements non-terminals "expr" and "lvalue" 
     *    of [SyntaxeContextuelle] in pass 3
     *
     * @param compiler  (contains the "env_types" attribute)
     * @param localEnv
     *            Environment in which the expression should be checked
     *            (corresponds to the "env_exp" attribute)
     * @param currentClass
     *            Definition of the class containing the expression
     *            (corresponds to the "class" attribute)
     *             is null in the main bloc.
     * @return the Type of the expression
     *            (corresponds to the "type" attribute)
     */
    public abstract Type verifyExpr(DecacCompiler compiler,
            EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError;

    /**
     * Verify the expression in right hand-side of (implicit) assignments 
     * 
     * implements non-terminal "rvalue" of [SyntaxeContextuelle] in pass 3
     *
     * @param compiler  contains the "env_types" attribute
     * @param localEnv corresponds to the "env_exp" attribute
     * @param currentClass corresponds to the "class" attribute
     * @param expectedType corresponds to the "type1" attribute            
     * @return this with an additional ConvFloat if needed...
     */
    public AbstractExpr verifyRValue(DecacCompiler compiler,
            EnvironmentExp localEnv, ClassDefinition currentClass,
            Type expectedType)
            throws ContextualError {
        LOG.debug("Verify RValue - begin");
        Type exprType = this.verifyExpr(compiler, localEnv, currentClass);
        
        if (expectedType.isFloat() && exprType.isInt()) {
            ConvFloat newTreeNode = new ConvFloat(this);
            newTreeNode.verifyExpr(compiler, localEnv, currentClass);
            return newTreeNode;
        }
        LOG.debug("Verify RValue - not ConvFloat case");

        if (!expectedType.sameType(exprType)) {
            LOG.debug("Verify RValue - not same type");

            if (exprType.isClass() || expectedType.isClass()) {
                LOG.debug("Verify RValue - not classes type");

                if (!exprType.asClassType("Should not happen, contact developpers please.",
                        this.getLocation()).isSubClassOf(expectedType.asClassType(
                            "Should not happen, contact developpers please.", this.getLocation())));
            }
            throw new ContextualError(String.format("'%s' is not of type %s", 
                this.decompile(), expectedType.toString()), this.getLocation()); // Rule 3.28
        }
        return this;
    }
    
    
    @Override
    protected void verifyInst(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass, Type returnType)
            throws ContextualError {
        Validate.notNull(localEnv);

        this.verifyExpr(compiler, localEnv, currentClass);
    }

    /**
     * Verify the expression as a condition, i.e. check that the type is
     * boolean.
     *
     * @param localEnv
     *            Environment in which the condition should be checked.
     * @param currentClass
     *            Definition of the class containing the expression, or null in
     *            the main program.
     */
    void verifyCondition(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass) throws ContextualError {
        if (!this.verifyExpr(compiler, localEnv, currentClass).isBoolean()) {
            throw new ContextualError("La condition ne renvoie pas un boolean", this.getLocation()); // Rule 3.29
        }
    }

    /**
     * Generate code to print the expression
     *
     * @param compiler
     */
    protected void codeGenPrint(DecacCompiler compiler, boolean printHex) {
        if(this.getType().isInt()){
            compiler.setN(compiler.getN()+1);
            IntLiteral intExpr = (IntLiteral)this;
            compiler.addInstruction(new LOAD(new ImmediateInteger(intExpr.getValue()),Register.getR(1)));
            compiler.addInstruction(new WINT());
        }
        if(this.getType().isFloat()){
            compiler.setN(compiler.getN()+1);
            FloatLiteral intExpr = (FloatLiteral)this;
            compiler.addInstruction(new LOAD(new ImmediateFloat(intExpr.getValue()),Register.getR(1)));
            if (!printHex) {
                compiler.addInstruction(new WFLOAT());
            }
            else {
                compiler.addInstruction(new WFLOATX());
            }
        }
    }

    @Override
    protected void codeGenInst(DecacCompiler compiler) {

        compiler.setN(compiler.getN()+1);
        if(this.getType().sameType(compiler.environmentType.INT)){
            IntLiteral intExpr = (IntLiteral)this;
            compiler.addInstruction(new LOAD(new ImmediateInteger(intExpr.getValue()),Register.getR(compiler.getN())));
        }
        if(this.getType().sameType(compiler.environmentType.FLOAT)){
            FloatLiteral intExpr = (FloatLiteral)this;
            compiler.addInstruction(new LOAD(new ImmediateFloat(intExpr.getValue()),Register.getR(compiler.getN())));
        }
        if(this.getType().sameType(compiler.environmentType.BOOLEAN)){
            BooleanLiteral intExpr = (BooleanLiteral) this;
            if (intExpr.getValue()){
                compiler.addInstruction(new LOAD(new ImmediateInteger(1),Register.getR(compiler.getN())));
            }
            else{
                compiler.addInstruction(new LOAD(new ImmediateInteger(0),Register.getR(compiler.getN())));
            }
        }
    }

    

    @Override
    protected void decompileInst(IndentPrintStream s) {
        decompile(s);
        s.print(";");
    }

    @Override
    protected void prettyPrintType(PrintStream s, String prefix) {
        Type t = getType();
        if (t != null) {
            s.print(prefix);
            s.print("type: ");
            s.print(t);
            s.println();
        }
    }
}
