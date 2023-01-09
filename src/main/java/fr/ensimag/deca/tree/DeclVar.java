package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.context.VariableDefinition;
import fr.ensimag.deca.context.EnvironmentExp.DoubleDefException;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.ExpDefinition;
import fr.ensimag.deca.tools.IndentPrintStream;
import java.io.PrintStream;
import org.apache.commons.lang.Validate;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.RegisterOffset;
import fr.ensimag.ima.pseudocode.instructions.STORE;
/**
 * @author gl39
 * @date 01/01/2023
 */
public class DeclVar extends AbstractDeclVar {

    final private AbstractIdentifier type;
    final private AbstractIdentifier varName;
    final private AbstractInitialization initialization;

    public DeclVar(AbstractIdentifier type, AbstractIdentifier varName, AbstractInitialization initialization) {
        Validate.notNull(type);
        Validate.notNull(varName);
        Validate.notNull(initialization);
        this.type = type;
        this.varName = varName;
        this.initialization = initialization;
    }

    protected void codeGenInst(DecacCompiler compiler){
        System.out.println("DeclVar");
        int nAct = compiler.getN()+1;
        Initialization initExpr = (Initialization)initialization;
        initExpr.getExpression().codeGenInst(compiler);
        VariableDefinition varDef = (VariableDefinition) varName.getDefinition();
        varDef.setOperand(new RegisterOffset(compiler.getSP(), Register.SP));
        compiler.addInstruction(new STORE(Register.getR(nAct),new RegisterOffset(compiler.getSP(), Register.SP)));
        compiler.setSP(compiler.getSP()+1);
    }

    @Override
    protected void verifyDeclVar(DecacCompiler compiler,
            EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError {
        Validate.notNull(localEnv);

        // On verifie que le type existe bien
        Type initializationType = this.type.verifyType(compiler);
        //this.type.setDefinition(compiler.environmentType.defOfType(this.type.getName()));

        // On verifie que varName n'est pas deja declare localement
        try {
            this.varName.setDefinition(new VariableDefinition(initializationType, this.getLocation()));
            localEnv.declare(this.varName.getName(), this.varName.getExpDefinition());
        }
        catch (Exception DoubleDefException) {
            throw new ContextualError(String.format("Le nom de variable '%s' est déjà déclaré dans l'environnement local",
                    this.varName.getName().getName()), this.getLocation()); // Rule 3.17
        }


        this.initialization.verifyInitialization(compiler, initializationType, localEnv, currentClass);

    }


    @Override
    public void decompile(IndentPrintStream s) {
        type.decompile(s);
        s.print(" ");
        varName.decompile(s);
        s.print(" ");
        initialization.decompile(s);
        s.print(";");
    }

    @Override
    protected void iterChildren(TreeFunction f) {
        type.iter(f);
        varName.iter(f);
        initialization.iter(f);
    }

    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        type.prettyPrint(s, prefix, false);
        varName.prettyPrint(s, prefix, false);
        initialization.prettyPrint(s, prefix, true);
    }
}
