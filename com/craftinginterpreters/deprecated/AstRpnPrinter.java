package com.craftinginterpreters.deprecated;

// import com.craftinginterpreters.lox.Expr;
// import com.craftinginterpreters.lox.Expr.Assign;
// import com.craftinginterpreters.lox.Expr.Call;
// import com.craftinginterpreters.lox.Expr.Increment;
// import com.craftinginterpreters.lox.Expr.Logical;
// import com.craftinginterpreters.lox.Expr.Variable;

// /** DEPRECATED - only supports expressions, no statements. */
// public class AstRpnPrinter implements Expr.Visitor<String> {

//     public String print(Expr expr) {
//         return expr.accept(this);
//     }

//     @Override
//     public String visitBinaryExpr(Expr.Binary expr) {
//         return expr.left.accept(this) + " " + 
//                expr.right.accept(this) + " " + 
//                expr.operator.lexeme;
//     }

//     @Override
//     public String visitGroupingExpr(Expr.Grouping expr) {
//         return expr.expression.accept(this);
//     }

//     @Override
//     public String visitLiteralExpr(Expr.Literal expr) {
//         if (expr.value == null) return "nil";
//         return expr.value.toString();
//     }

//     @Override
//     public String visitUnaryExpr(Expr.Unary expr) {
//         return expr.right.accept(this) + " " + expr.operator.lexeme;
//     }

//     @Override
//     public String visitTernaryExpr(Expr.Ternary expr) {
//         return expr.left.accept(this) + " " + expr.center.accept(this) + " " + expr.right.accept(this) + " : ?";
//     }

//     @Override
//     public String visitVariableExpr(Variable expr) {
//         return null;
//     }

//     @Override
//     public String visitAssignExpr(Assign expr) {
//         return null;
//     }

//     @Override
//     public String visitLogicalExpr(Logical expr) {
//         return null;
//     }

//     @Override
//     public String visitIncrementExpr(Increment expr) {
//         return null;
//     }

//     @Override
//     public String visitCallExpr(Call expr) {
//         return null;
//     }

//     // public static void main(String[] args) {
//     //     Expr expression = 
//     //     new Expr.Grouping(
//     //         new Expr.Binary(
//     //             new Expr.Binary(
//     //                 new Expr.Literal(1), 
//     //                 new Token(TokenType.PLUS, "+", null, 1), 
//     //                 new Expr.Literal(2)
//     //             ),
//     //             new Token(TokenType.STAR, "*", null, 1),
//     //             new Expr.Binary(
//     //                 new Expr.Literal(4), 
//     //                 new Token(TokenType.PLUS, "+", null, 1), 
//     //                 new Expr.Unary(
//     //                     new Token(TokenType.MINUS, "-", null, 1), 
//     //                     new Expr.Literal(3)
//     //                 )
//     //             )
//     //         )
//     //     );

//     //     System.out.println(new AstRpnPrinter().print(expression));
//     // }
// }
