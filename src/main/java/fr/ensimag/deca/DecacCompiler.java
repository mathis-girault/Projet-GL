package fr.ensimag.deca;

import fr.ensimag.deca.context.EnvironmentType;
import fr.ensimag.deca.syntax.DecaLexer;
import fr.ensimag.deca.syntax.DecaParser;
import fr.ensimag.deca.tools.DecacInternalError;
import fr.ensimag.deca.tools.SymbolTable;
import fr.ensimag.deca.tools.SymbolTable.Symbol;
import fr.ensimag.deca.tree.AbstractProgram;
import fr.ensimag.deca.tree.LocationException;
import fr.ensimag.pseudocode.AbstractLine;
import fr.ensimag.pseudocode.IMAProgram;
import fr.ensimag.pseudocode.Instruction;
import fr.ensimag.pseudocode.Label;
import fr.ensimag.pseudocode.Line;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.log4j.Logger;

/**
 * Decac compiler instance.
 *
 * This class is to be instantiated once per source file to be compiled. It
 * contains the meta-data used for compiling (source file name, compilation
 * options) and the necessary utilities for compilation (symbol tables, abstract
 * representation of target file, ...).
 *
 * It contains several objects specialized for different tasks. Delegate methods
 * are used to simplify the code of the caller (e.g. call
 * compiler.addInstruction() instead of compiler.getProgram().addInstruction()).
 *
 * @author gl39
 * @date 01/01/2023
 */
public class DecacCompiler implements Runnable {
    private static final Logger LOG = Logger.getLogger(DecacCompiler.class);

    /**
     * Portable newline character.
     */
    private static final String nl = System.getProperty("line.separator", "\n");
    private int n = 1;
    private int SP = 0;
    private Label erreurPile = new Label("ErreurPile");

    public Label getErreurPile() {
        return erreurPile;
    }

    private Label erreurOverflow = new Label("overflof_error");

    public Label getErreurOverflow() {
        return erreurOverflow;
    }

    private Label erreurArondi = new Label("ErreurArrondi");

    public Label getErreurArrondi() {
        return erreurArondi;
    }

    private Label erreurInOut = new Label("io_error");

    public Label getErreurinOut() {
        return erreurInOut;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getSP() {
        return SP;
    }

    public void setSP(int SP) {
        this.SP = SP;
    }

    private boolean compileInArm = false;

    public DecacCompiler(CompilerOptions compilerOptions, File source, boolean arm) {
        super();
        this.compilerOptions = compilerOptions;
        this.source = source;
        compileInArm = arm;
        program = new IMAProgram(compileInArm);
    }

    public int getLastLineIndex() {
        return program.getLastLineIndex();
    }

    public void addIndexLine(int index, Instruction inst) {
        program.addIndex(inst, index);
    }

    public boolean compileInArm() {
        return compileInArm;
    }

    /**
     * Source file associated with this compiler instance.
     */
    public File getSource() {
        return source;
    }

    /**
     * Compilation options (e.g. when to stop compilation, number of registers
     * to use, ...).
     */
    public CompilerOptions getCompilerOptions() {
        return compilerOptions;
    }

    /**
     * @see
     *      fr.ensimag.ima.pseudocode.IMAProgram#add(fr.ensimag.ima.pseudocode.AbstractLine)
     */
    public void add(AbstractLine line) {
        program.add(line);
    }

    /**
     * @see fr.ensimag.ima.pseudocode.IMAProgram#addComment(java.lang.String)
     */
    public void addComment(String comment) {
        if (compileInArm) {
            program.addComment("/* " + comment + "*/");
        } else {
            program.addComment("; " + comment);
        }
    }

    /**
     * @see
     *      fr.ensimag.ima.pseudocode.IMAProgram#addLabel(fr.ensimag.ima.pseudocode.Label)
     */
    public void addLabel(Label label) {
        program.addLabel(label);
    }

    /**
     * @see
     *      fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction)
     */
    public void addInstruction(Instruction instruction) {
        program.addInstruction(instruction);
    }

    public void addInstructionFirst(Instruction instruction) {
        program.addFirst(instruction);
    }

    /**
     * @see
     *      fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction,
     *      java.lang.String)
     */
    public void addInstruction(Instruction instruction, String comment) {
        program.addInstruction(instruction, comment);
    }

    /**
     * @see
     *      fr.ensimag.ima.pseudocode.IMAProgram#display()
     */
    public String displayIMAProgram() {
        return program.display();
    }

    private final CompilerOptions compilerOptions;
    private final File source;
    /**
     * The main program. Every instruction generated will eventually end up here.
     */
    private final IMAProgram program;

    /** The global environment for types (and the symbolTable) */
    public final SymbolTable symbolTable = new SymbolTable();
    public final EnvironmentType environmentType = new EnvironmentType(this);

    /**
     * @param name
     * @return
     */
    public Symbol createSymbol(String name) {
        return symbolTable.create(name);
    }

    @Override
    public void run() {
        this.compile();
    }

    /**
     * Run the compiler (parse source file, generate code)
     *
     * @return true on error
     */
    public boolean compile() {
        String sourceFile = source.getAbsolutePath();
        String destFile = null;
        // A FAIRE: calculer le nom du fichier .ass à partir du nom du
        // A FAIRE: fichier .deca.
        String namePath = this.source.getAbsolutePath();
        String nameSource = this.source.getName();
        // destFile = nameSource.substring(0, nameSource.length()-5)+".ass";
        String newName = compileInArm() ? nameSource.substring(0, nameSource.length() - 5) + ".S"
                : nameSource.substring(0, nameSource.length() - 5) + ".ass";
        destFile = namePath.replaceAll(nameSource, "/" + newName);
        // destFile = nameSource.replaceAll(this.source.getName(),
        // "assembleur/"+this.getSource().getName().substring(0,
        // nameSource.length()-5)+".ass");
        PrintStream err = System.err;
        PrintStream out = System.out;
        LOG.debug("Compiling file " + sourceFile + " to assembly file " + destFile);

        try {
            return doCompile(sourceFile, destFile, out, err);
        } catch (LocationException e) {
            e.display(err);
            return true;
        } catch (DecacFatalError e) {
            err.println(e.getMessage());
            return true;
        } catch (StackOverflowError e) {
            LOG.debug("stack overflow", e);
            err.println("Stack overflow while compiling file " + sourceFile + ".");
            return true;
        } catch (Exception e) {
            LOG.fatal("Exception raised while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        } catch (AssertionError e) {
            LOG.fatal("Assertion failed while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        }
    }

    /**
     * Internal function that does the job of compiling (i.e. calling lexer,
     * verification and code generation).
     *
     * @param sourceName name of the source (deca) file
     * @param destName   name of the destination (assembly) file
     * @param out        stream to use for standard output (output of decac -p)
     * @param err        stream to use to display compilation errors
     *
     * @return true on error
     */
    private boolean doCompile(String sourceName, String destName,
            PrintStream out, PrintStream err)
            throws DecacFatalError, LocationException {
        AbstractProgram prog = doLexingAndParsing(sourceName, err);

        if (prog == null) {
            LOG.info("Parsing failed");
            return true;
        }
        assert (prog.checkAllLocations());

        // arret si option -p
        if (this.getCompilerOptions().getParsing()) {
            prog.decompile(System.out);
            return false;
        }

        // arret si option -v
        prog.verifyProgram(this);
        assert (prog.checkAllDecorations());

        if (this.getCompilerOptions().getVerification()) {
            return false;
        }

        addComment("start main program");
        prog.codeGenProgram(this);
        if (compileInArm) {
            program.writePrintLabel();
            program.addFirst(new Line("mov R10, sp"));
            program.addFirst(new Line("mov R11, sp"));
            program.addFirst(new Line(""));
            program.addFirst(new Line("_start:"));
            program.addFirst(new Line(".global _start"));
            program.addFirst(new Line(".text"));
        }
        addComment("end main program");
        LOG.debug("Generated assembly code:" + nl + program.display());
        LOG.info("Output file assembly file is: " + destName);

        FileOutputStream fstream = null;
        try {
            fstream = new FileOutputStream(destName);
        } catch (FileNotFoundException e) {
            throw new DecacFatalError("Failed to open output file: " + e.getLocalizedMessage());
        }

        LOG.info("Writing assembler file ...");

        program.display(new PrintStream(fstream));
        LOG.info("Compilation of " + sourceName + " successful.");
        return false;
    }

    /**
     * Build and call the lexer and parser to build the primitive abstract
     * syntax tree.
     *
     * @param sourceName Name of the file to parse
     * @param err        Stream to send error messages to
     * @return the abstract syntax tree
     * @throws DecacFatalError    When an error prevented opening the source file
     * @throws DecacInternalError When an inconsistency was detected in the
     *                            compiler.
     * @throws LocationException  When a compilation error (incorrect program)
     *                            occurs.
     */
    protected AbstractProgram doLexingAndParsing(String sourceName, PrintStream err)
            throws DecacFatalError, DecacInternalError {
        DecaLexer lex;
        try {
            lex = new DecaLexer(CharStreams.fromFileName(sourceName));
        } catch (IOException ex) {
            throw new DecacFatalError("Failed to open input file: " + ex.getLocalizedMessage());
        }
        lex.setDecacCompiler(this);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        DecaParser parser = new DecaParser(tokens);
        parser.setDecacCompiler(this);
        return parser.parseProgramAndManageErrors(err);
    }
}
