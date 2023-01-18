package fr.ensimag.deca.tree;

import static org.mockito.ArgumentMatchers.nullable;

import java.io.PrintStream;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ClassType;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.MethodDefinition;
import fr.ensimag.deca.context.Signature;
import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.tools.IndentPrintStream;

/*
 * Call of a method function on an expression
 */
public class MethodCall extends AbstractExpr {
    private static final Logger LOG = Logger.getLogger(MethodCall.class);

    private AbstractIdentifier name;
    private AbstractExpr expr;
    private ListExpr args;

    public MethodCall(AbstractExpr e, AbstractIdentifier name, ListExpr args) {
        Validate.notNull(e);
        Validate.notNull(name);
        this.name = name;
        this.expr = e;
        this.args = args;
    }

    public MethodCall(AbstractIdentifier name, ListExpr args) {
        Validate.notNull(name);
        this.name = name;
        this.expr = null;
    }

    public ListExpr getArgs() {
        return args;
    }


    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError {
        LOG.debug(this.expr);

        ClassType callerClass;
        if (this.expr != null) {
            // On verifie que l'expr est de type class 
            Type exprType = expr.verifyExpr(compiler, localEnv, currentClass);
            if (!exprType.isClass()) {
                throw new ContextualError(String.format("L'appel méthode sur '%s' doit se faire sur un Type class",
                        this.name), this.getLocation()); // Rule 3.70
            }
            callerClass = exprType.asClassType(null, null);
        } else {
            callerClass = currentClass.getType();
        }

        // On verifie que le ident est bien une méthode 
        MethodDefinition methodDef = this.verifyMethodIdent(compiler.environmentType.getClass(
            callerClass.getName()).getMembers());

        // On verifie que la signature correspond
        this.verifyRValueStar(compiler, localEnv, currentClass, methodDef.getSignature());

        this.setType(methodDef.getType());
        return methodDef.getType();
    }


    /**
     * TODO
     */
    public MethodDefinition verifyMethodIdent(EnvironmentExp localEnv) throws ContextualError {
        LOG.debug(this.name);

        if (localEnv.get(this.name.getName()) == null) {
            throw new ContextualError(String.format("La méthode '%s' n'est pas défini dans l'environnement local",
                    this.name.getName()), this.getLocation()); // Rule 3.70
        }

        this.name.setDefinition(localEnv.get(this.name.getName()).asMethodDefinition(String.format(
                "'%s' n'est pas une méthode", this.name.getName()), this.getLocation())); // Rule 3.72
        
        return this.name.getMethodDefinition();
    }


    public void verifyRValueStar(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass, Signature sig) throws ContextualError {
        
        LOG.debug("Signature: " + sig);
        LOG.debug("Caller: " + this.args);

        int index = 0;
        if (this.args != null) {
            for (AbstractExpr currentExpr : this.args.getList()) {
                currentExpr.verifyRValue(compiler, localEnv, currentClass, sig.paramNumber(index));
                index++;
            }
        }

        if (index != sig.size()) {
            throw new ContextualError("Le nombre de paramètres ne correspond pas",
                    this.getLocation()); // Rule 3.74
        }
    }


    @Override
    public void decompile(IndentPrintStream s) {
        if (expr != null) {
            expr.decompile(s);
            s.print(".");
        }
        this.name.decompile(s);
        s.print("(");
        int count = getArgs().size();
        for (AbstractExpr e : getArgs().getList()) {
            e.decompile(s);
            if (count != 1) {
                s.print(", ");
            }
            count--;
        }
        s.print(")");
    }

    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        if (expr != null) {
            expr.prettyPrint(s, prefix, false);
        }
        if (args != null) {
            name.prettyPrint(s, prefix, false);
            args.prettyPrint(s, prefix, true);
        } else {
            name.prettyPrint(s, prefix, true);
        }
    }

    @Override
    protected void iterChildren(TreeFunction f) {
        if (expr != null) {
            expr.iter(f);
        }
        name.iter(f);
        if (args != null) {
            args.iter(f);
        }
    }

}