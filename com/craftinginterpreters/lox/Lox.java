package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// TODO: add color to warnings and errors when running a script, and make their formats match.
public class Lox {
    private static final Interpreter interpreter = new Interpreter();

    private static boolean _isInteractive = false;

    /**
     * Returns true if Lox is being executed in an interactive environment like a REPL, false otherwise.
     */
    public static boolean isInteractive() {
        return _isInteractive;
    }

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {

        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64); // exit codes from UNIX sysexits.h
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    // runs a Lox file from disk to halting.
    public static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    // initializes a Lox REPL.
    public static void runPrompt() throws IOException {
        _isInteractive = true;
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    // runs a string of Lox code.
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // stop on errors.
        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop on resolution errors.
        if (hadError) return;

        interpreter.interpret(statements);
    }

    static void warning(Token token, String message) {
        report(token.line, "Warning", " at '" + token.lexeme + "'", message);
    }

    // General-purpose error handling function.
    static void error(int lineNumber, String message) {
        report(lineNumber, "Error", "", message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, "Error", " at end", message);
        } else {
            report(token.line, "Error", " at '" + token.lexeme + "'", message);
        }
        hadError = true;
    }

    static void runtimeError(RuntimeError error) {
        String message = error.getMessage();
        if (!_isInteractive) message += "\n[line " + error.token.line + "]";
        System.err.println(message);
        // System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    // report error to user.
    private static void report(int lineNumber, String type, String where, String message) {
        System.err.println("[line " + lineNumber + "] " + type + where + ": " + message);
    }
}