package com.craftinginterpreters.lox;

import java.util.List;

// File generated by tool/GenerateAst.java
public abstract class Expr {
	public interface Visitor<R> {
		R visitAssignExpr(Assign expr);
		R visitBinaryExpr(Binary expr);
		R visitCallExpr(Call expr);
		R visitGroupingExpr(Grouping expr);
		R visitLiteralExpr(Literal expr);
		R visitLogicalExpr(Logical expr);
		R visitUnaryExpr(Unary expr);
		R visitTernaryExpr(Ternary expr);
		R visitVariableExpr(Variable expr);
		R visitIncrementExpr(Increment expr);
	}
	public static class Assign extends Expr {
		Assign(Token name, Expr value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}

		public final Token name;
		public final Expr value;
	}
	public static class Binary extends Expr {
		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}

		public final Expr left;
		public final Token operator;
		public final Expr right;
	}
	public static class Call extends Expr {
		Call(Expr callee, Token paren, List<Expr> arguments) {
			this.callee = callee;
			this.paren = paren;
			this.arguments = arguments;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}

		public final Expr callee;
		public final Token paren;
		public final List<Expr> arguments;
	}
	public static class Grouping extends Expr {
		Grouping(Expr expression) {
			this.expression = expression;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitGroupingExpr(this);
		}

		public final Expr expression;
	}
	public static class Literal extends Expr {
		Literal(Object value) {
			this.value = value;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}

		public final Object value;
	}
	public static class Logical extends Expr {
		Logical(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitLogicalExpr(this);
		}

		public final Expr left;
		public final Token operator;
		public final Expr right;
	}
	public static class Unary extends Expr {
		Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}

		public final Token operator;
		public final Expr right;
	}
	public static class Ternary extends Expr {
		Ternary(Expr left, Expr center, Expr right, Token leftOperator, Token rightOperator) {
			this.left = left;
			this.center = center;
			this.right = right;
			this.leftOperator = leftOperator;
			this.rightOperator = rightOperator;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitTernaryExpr(this);
		}

		public final Expr left;
		public final Expr center;
		public final Expr right;
		public final Token leftOperator;
		public final Token rightOperator;
	}
	public static class Variable extends Expr {
		Variable(Token name) {
			this.name = name;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}

		public final Token name;
	}
	public static class Increment extends Expr {
		Increment(Expr.Variable identifier, Token operator, IncrementType type) {
			this.identifier = identifier;
			this.operator = operator;
			this.type = type;
		}

		@Override
		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitIncrementExpr(this);
		}

		public final Expr.Variable identifier;
		public final Token operator;
		public final IncrementType type;
	}

	public abstract <R> R accept(Visitor<R> visitor);
}
