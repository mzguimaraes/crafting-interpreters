package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage generate_ast <output_directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        // define expression AST subclasses.
        defineAst(outputDir, "Expr", Arrays.asList(
            "Assign     : Token name, Expr value",
            "Binary     : Expr left, Token operator, Expr right",
            "Call       : Expr callee, Token paren, List<Expr> arguments",
            "Grouping   : Expr expression",
            "Literal    : Object value",
            "Logical    : Expr left, Token operator, Expr right",
            "Unary      : Token operator, Expr right",
            "Ternary    : Expr left, Expr center, Expr right, Token leftOperator, Token rightOperator",
            "Variable   : Token name",
            "Increment  : Expr.Variable identifier, Token operator, IncrementType type"
        ));

        // define statement AST subclasses.
        defineAst(outputDir, "Stmt", Arrays.asList(
            "Block      : List<Stmt> statements",
            "Expression : Expr expression",
            "Function   : Token name, List<Token> params, List<Stmt> body",
            "IfElse     : List<Stmt.If> ifBranches, Stmt elseBranch",
            "If         : Expr condition, Stmt body",
            "Print      : Expr expression",
            "Return     : Token keyword, Expr value", 
            "Var        : Token name, Expr initializer",
            "While      : Expr condition, Stmt body", 
            "LoopKeyword: Token token"
        ));
    }

    private static void defineAst(
            String outputDir, String baseName, List<String> types) 
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("// File generated by tool/GenerateAst.java");
        writer.println("public abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // The AST classes.
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // The base accept() method.
        writer.println();
        writer.println("\tpublic abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("\tpublic interface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("\t\tR visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("\t}");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("\tpublic static class " + className + " extends " + baseName + " {");

        // Constructor.
        writer.println("\t\t" + className + "(" + fieldList + ") {");

        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("\t\t\tthis." + name + " = " + name + ";");
        }

        writer.println("\t\t}");

        // Visitor pattern.
        writer.println();
        writer.println("\t\t@Override");
        writer.println("\t\tpublic <R> R accept(Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor.visit" + className + baseName + "(this);");
        writer.println("\t\t}");


        // Fields.
        writer.println();
        for (String field : fields) {
            writer.println("\t\tpublic final " + field + ";");
        }

        writer.println("\t}");
    }
    
}
