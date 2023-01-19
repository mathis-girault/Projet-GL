package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.codegen.BlocInProg;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.pseudocode.Label;
import fr.ensimag.pseudocode.LabelOperand;
import fr.ensimag.pseudocode.Line;
import fr.ensimag.pseudocode.Register;
import fr.ensimag.pseudocode.RegisterOffset;
import fr.ensimag.superInstructions.SuperRTS;
import fr.ensimag.ima.instructions.LEA;
import fr.ensimag.ima.instructions.LOAD;
import fr.ensimag.ima.instructions.PUSH;

import java.io.PrintStream;

import org.apache.commons.lang.Validate;

/**
 * Declaration of a class (<code>class name extends superClass {members}<code>).
 * 
 * @author gl39
 * @date 01/01/2023
 */
public class DeclClass extends AbstractDeclClass {

    private AbstractIdentifier name;
    private AbstractIdentifier superclass;
    private ListDeclField fields;
    private ListDeclMethod methods;

    public DeclClass(AbstractIdentifier nom, AbstractIdentifier mother, ListDeclField params,
            ListDeclMethod functions) {
        Validate.notNull(nom);
        Validate.notNull(mother);
        Validate.notNull(params);
        Validate.notNull(functions);
        name = nom;
        superclass = mother;
        fields = params;
        methods = functions;
    }

    protected void codeGenClass(DecacCompiler compiler){
        int nActual = compiler.getN() + 1;
        compiler.setN(nActual);
        compiler.addComment("class "+this.name.getName().getName());
        compiler.addInstruction(new LEA(compiler.environmentType.getClass(superclass.getName()).getOperand(), Register.getR(nActual)));
        compiler.addInstruction(new PUSH(Register.getR(nActual)));
        compiler.setSP(compiler.getSP() + 1);
        compiler.environmentType.getClass(this.name.getName()).setOperand(new RegisterOffset(compiler.getSP(), Register.GB));
        compiler.setN(nActual - 1);
        for (AbstractDeclMethod method : methods.getList()){
            compiler.addInstruction(
                new LOAD(new LabelOperand(
                    new Label(this.name.getName().toString() + '.' + method.getName().getName().toString())),
                    Register.getR(nActual)));
            compiler.addInstruction(new PUSH(Register.getR(nActual)));
            compiler.setSP(compiler.getSP() + 1);
        }
        compiler.add(new Line(""));
    }

    protected void codeGenCorpMethod(DecacCompiler compiler, String name){
        BlocInProg.addBloc("init." + this.name.getName().getName(), compiler.getLastLineIndex(), 0, 0);
        compiler.addLabel(new Label("init." + this.name.getName().toString()));
        for (AbstractDeclField field : fields.getList()) {
            field.codeGenDeclFiedl(compiler, name);
        }
        compiler.addInstruction(SuperRTS.main(compiler.compileInArm()));
        compiler.addComment("");
        for (AbstractDeclMethod method : methods.getList()) {
            BlocInProg.addBloc(this.name.getName().getName() + '.' + method.getName().getName(), compiler.getLastLineIndex(), 0, 0);
            compiler.addLabel(new Label(this.name.getName().getName() + '.' + method.getName().getName()));
            method.codeGenCorpMethod(compiler, name);
            compiler.addComment("");
        }
    }

    @Override
    public void decompile(IndentPrintStream s) {
        s.print("class ");
        name.decompile(s);
        s.print(" extends ");
        superclass.decompile(s);
        s.println(" {");
        s.indent();
        fields.decompile(s);
        methods.decompile(s);
        s.unindent();
        s.println("}");
    }

    @Override
    protected void verifyClass(DecacCompiler compiler) throws ContextualError {

        if (compiler.environmentType.defOfType(this.superclass.getName()) == null) {
            throw new ContextualError(String.format("La super classe '%s' n'existe pas",
                    this.superclass), this.getLocation()); // Rule 1.3
        }

        if (!compiler.environmentType.defOfType(this.superclass.getName()).isClass()) {
            throw new ContextualError(String.format("'%s' n'est pas une class",
                    this.superclass), this.getLocation()); // Rule 1.3
        }

        ClassDefinition superDef = (ClassDefinition) (compiler.environmentType.defOfType(this.superclass.getName()));

        try {
            compiler.environmentType.addNewClass(compiler, this.name.getName(),
                    this.getLocation(), superDef);
        } catch (EnvironmentExp.DoubleDefException e) {
            throw new ContextualError(String.format("Le nom '%s' est deja un nom de class ou de type",
                    this.name), this.getLocation()); // Rule 1.3
        }
        this.superclass.setDefinition(superDef);
        this.name.setDefinition(compiler.environmentType.getClass(this.name.getName()));
    }

    @Override
    protected void verifyClassMembers(DecacCompiler compiler)
            throws ContextualError {

        ClassDefinition currentClassDef = (ClassDefinition) (compiler.environmentType.defOfType(this.name.getName()));

        currentClassDef.setNumberOfFields(currentClassDef.getSuperClass().getNumberOfFields());
        currentClassDef.setNumberOfMethods(currentClassDef.getSuperClass().getNumberOfMethods());

        this.fields.verifyListDeclFieldMembers(compiler, currentClassDef, this.superclass);
        this.methods.verifyListDeclMethodMembers(compiler, currentClassDef, this.superclass);
    }

    @Override
    protected void verifyClassBody(DecacCompiler compiler) throws ContextualError {

        ClassDefinition currentClassDef = compiler.environmentType.getClass(this.name.getName());

        this.fields.verifyListDeclFieldBody(compiler, currentClassDef);
        this.methods.verifyListDeclMethodBody(compiler, currentClassDef);
    }

    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        name.prettyPrint(s, prefix, false);
        superclass.prettyPrint(s, prefix, false);
        fields.prettyPrint(s, prefix, false);
        methods.prettyPrint(s, prefix, true);
    }

    @Override
    protected void iterChildren(TreeFunction f) {
        name.iter(f);
        superclass.iter(f);
        fields.iter(f);
        methods.iter(f);
    }

}
