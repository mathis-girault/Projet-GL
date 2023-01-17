package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.tools.IndentPrintStream;

/*
 * List of fields' declaration
 */
public class ListDeclField extends TreeList<AbstractDeclField> {

    @Override
    public void decompile(IndentPrintStream s) {
        for (AbstractDeclField c : getList()) {
            c.decompile(s);
            s.println();
        }
    }

    /**
     * TODO
     */
    public void verifyListDeclFieldMembers(DecacCompiler compiler, ClassDefinition currentClassDef,
            AbstractIdentifier superClass)
            throws ContextualError {

        for (AbstractDeclField myDeclField : this.getList()) {
            myDeclField.verifyEnvField(compiler, currentClassDef, superClass);
        }
    }

    public void verifyListDeclFieldBody(DecacCompiler compiler, ClassDefinition currentClassDef)
            throws ContextualError {

        for (AbstractDeclField myDeclField : this.getList()) {
            myDeclField.verifyInitField(compiler, currentClassDef);
        }
    }

}
