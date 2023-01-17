package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.superInstructions.SuperBOV;
import fr.ensimag.superInstructions.SuperERROR;
import fr.ensimag.superInstructions.SuperHALT;
import fr.ensimag.superInstructions.SuperTSTO;
import fr.ensimag.superInstructions.SuperWNL;
import fr.ensimag.superInstructions.SuperWSTR;

import java.io.PrintStream;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

/**
 * @author gl39
 * @date 01/01/2023
 */
public class Main extends AbstractMain {
    private static final Logger LOG = Logger.getLogger(Main.class);

    private ListDeclVar declVariables;
    private ListInst insts;

    public Main(ListDeclVar declVariables, ListInst insts) {
        Validate.notNull(declVariables);
        Validate.notNull(insts);
        this.declVariables = declVariables;
        this.insts = insts;
    }

    @Override
    protected void verifyMain(DecacCompiler compiler) throws ContextualError {
        LOG.debug("verify Main: start");

        EnvironmentExp localEnv = new EnvironmentExp(null);
        this.declVariables.verifyListDeclVariable(compiler, localEnv, null);

        LOG.debug(String.format("symbol : %s", compiler.symbolTable.getTable().get("x")));
        LOG.debug(String.format("env : %s", localEnv.getLocalEnv().toString()));

        this.insts.verifyListInst(compiler, localEnv, null, null);
        LOG.debug("verify Main: end");
    }

    @Override
    protected void codeGenMain(DecacCompiler compiler) {
        //Déclaration des variables
        this.declVariables.codeGenListVar(compiler);
        //Instructions du programme principal
        compiler.addComment("Beginning of main instructions:");
        insts.codeGenListInst(compiler);
        //Début des gestions des erreurs
        compiler.addComment("End of the main program");
    }

    @Override
    public void decompile(IndentPrintStream s) {
        s.println("{");
        s.indent();
        declVariables.decompile(s);
        insts.decompile(s);
        s.unindent();
        s.println("}");
    }

    @Override
    protected void iterChildren(TreeFunction f) {
        declVariables.iter(f);
        insts.iter(f);
    }

    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        declVariables.prettyPrint(s, prefix, false);
        insts.prettyPrint(s, prefix, true);
    }
}
