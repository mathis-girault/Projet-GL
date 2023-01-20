package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.codegen.BlocInProg;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.ExpDefinition;
import fr.ensimag.deca.context.MethodDefinition;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.pseudocode.ImmediateInteger;
import fr.ensimag.pseudocode.Label;
import fr.ensimag.pseudocode.LabelOperand;
import fr.ensimag.pseudocode.Line;
import fr.ensimag.pseudocode.Register;
import fr.ensimag.pseudocode.RegisterOffset;
import fr.ensimag.superInstructions.SuperADDSP;
import fr.ensimag.superInstructions.SuperBSR;
import fr.ensimag.superInstructions.SuperLOAD;
import fr.ensimag.superInstructions.SuperPOP;
import fr.ensimag.superInstructions.SuperPUSH;
import fr.ensimag.superInstructions.SuperRTS;
import fr.ensimag.superInstructions.SuperSTORE;
import fr.ensimag.superInstructions.SuperSUBSP;
import fr.ensimag.superInstructions.SuperTSTO;
import fr.ensimag.ima.instructions.LEA;
import fr.ensimag.ima.instructions.LOAD;
import fr.ensimag.ima.instructions.PUSH;

import java.io.PrintStream;
import java.util.Iterator;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

/**
 * Declaration of a class (<code>class name extends superClass {members}<code>).
 * 
 * @author gl39
 * @date 01/01/2023
 */
public class DeclClass extends AbstractDeclClass {
    private static final Logger LOG = Logger.getLogger(ClassDefinition.class);
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

    protected void codeGenClass(DecacCompiler compiler) {
        LOG.debug(this.name.getName().getName());
        LOG.debug(this.name.getClassDefinition().getNumberOfMethods());
        int nActual = compiler.getN() + 1;
        compiler.setN(nActual);
        compiler.addComment("class "+this.name.getName().getName());
        compiler.addInstruction(new LEA(compiler.environmentType.getClass(superclass.getName()).getOperand(), Register.getR(nActual)));
        compiler.addInstruction(new PUSH(Register.getR(nActual)));
        compiler.setSP(compiler.getSP() + 1);
        compiler.environmentType.getClass(this.name.getName()).setOperand(new RegisterOffset(compiler.getSP(), Register.GB));
        compiler.setN(nActual - 1);
        for (int i = 1; i <= this.name.getClassDefinition().getNumberOfMethods(); i++) {
            MethodDefinition expDef = this.name.getClassDefinition().getMethod(i);
            compiler.addInstruction(
                new LOAD(new LabelOperand(expDef.getLabel()),Register.getR(nActual)));
            compiler.addInstruction(SuperSTORE.main(Register.getR(nActual), new RegisterOffset(expDef.getIndex(), Register.SP), compiler.compileInArm()));
            compiler.setSP(compiler.getSP() + 1);
            }
        compiler.addInstruction(
            SuperADDSP.main(
                new ImmediateInteger(
                    this.name.getClassDefinition().getNumberOfMethods()), compiler.compileInArm()));
        compiler.add(new Line(""));
    }

    protected void codeGenCorpMethod(DecacCompiler compiler, String name){
        //Génération du code pour l'initialisation des instances de la class
        compiler.setN(1);
        String blockName = "init." + this.name.getName().getName();
        BlocInProg.addBloc(blockName, compiler.getLastLineIndex(), 0, 0);
        compiler.addLabel(new Label(blockName));
        //On regarde si la super class à des champs, il faut alors les initier avant
        if (superclass.getClassDefinition().getNumberOfFields() != 0) {
            compiler.addInstruction(SuperLOAD.main(new RegisterOffset(-2, Register.LB), Register.getR(compiler.getN() + 1), compiler.compileInArm()));
            compiler.addInstruction(SuperPUSH.main(Register.getR(compiler.getN() + 1), compiler.compileInArm()));
            compiler.addInstruction(SuperBSR.main(new LabelOperand(new Label("init."+superclass.getType().getName().getName())), compiler.compileInArm()));
            compiler.addInstruction(SuperSUBSP.main(new ImmediateInteger(1), compiler.compileInArm()));
        }
        // On déclare les champs de la class
        for (AbstractDeclField field : fields.getList()) {
            field.codeGenDeclFiedl(compiler, blockName);
        }
        // On test la pile en début de bloc et on remet l'environement dans l'état où il était avant l'appel à cette "méthode"
        for (int i = 2; i < BlocInProg.getBlock(blockName).getnbRegisterNeeded() + 2; i++) {
            compiler.addIndexLine(BlocInProg.getBlock(blockName).getLineStart() + 1, SuperPUSH.main(Register.getR(i), compiler.compileInArm()));
            compiler.addInstruction(SuperPOP.main(Register.getR(i), compiler.compileInArm()));
        }
        compiler.addIndexLine(BlocInProg.getBlock(blockName).getLineStart(), SuperTSTO.main(BlocInProg.getBlock(blockName).getnbPlacePileNeeded(), compiler.compileInArm()));
        compiler.addInstruction(SuperRTS.main(compiler.compileInArm()));
        compiler.addComment("");

        //On genere le code pour les méthodes de la class
        for (AbstractDeclMethod method : methods.getList()) {
            compiler.setN(1);
            LOG.debug("Nom de la méthode: " + method.getName().getName().getName());
            LOG.debug("Numéro de la méthode: " + method.getName().getMethodDefinition().getIndex());
            blockName = this.name.getName().getName() + '.' + method.getName().getName();
            BlocInProg.addBloc(blockName, compiler.getLastLineIndex() + 1, 0, 0);
            compiler.addLabel(new Label(blockName));
            method.codeGenCorpMethod(compiler, blockName);
            compiler.addIndexLine(BlocInProg.getBlock(blockName).getLineStart(), SuperTSTO.main(new ImmediateInteger(BlocInProg.getBlock(blockName).getnbPlacePileNeeded()), compiler.compileInArm()));
            for (int i = 2; i <= BlocInProg.getBlock(blockName).getnbRegisterNeeded(); i++) {
                compiler.addIndexLine(BlocInProg.getBlock(blockName).getLineStart(), SuperPUSH.main(Register.getR(i), compiler.compileInArm()));
                compiler.addInstruction(SuperPOP.main(Register.getR(i), compiler.compileInArm()));
            }
        }
        compiler.addInstruction(SuperRTS.main(compiler.compileInArm()));
        compiler.addComment("");
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

        LOG.debug("NumberOfFields " + this.name + " before: " + currentClassDef.getNumberOfFields());
        this.fields.verifyListDeclFieldMembers(compiler, currentClassDef, this.superclass);        
        LOG.debug("NumberOfFields " + this.name + " before: " + currentClassDef.getNumberOfFields());
        
        LOG.debug("NumberOfMethods " + this.name + " before: " + currentClassDef.getNumberOfMethods());
        this.methods.verifyListDeclMethodMembers(compiler, currentClassDef, this.superclass);
        LOG.debug("NumberOfMethods " + this.name + " after: " + currentClassDef.getNumberOfMethods());
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
