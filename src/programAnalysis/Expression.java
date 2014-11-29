package programAnalysis;

import java.util.List;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

/*
 *  Expression AST starts here.
 */

abstract class Expression {
	 
	Label_E label;
	void setLabel(Label_E label) { this.label = label; }
	Label_E getLabel() { return this.label; }
	
	AstNode node;
	void setAstNode(AstNode node) { this.node = node; }
	
	abstract public void accept(Visitor v);
	
	public boolean equals(Object that) {
		if (that instanceof Expression) {
			return node.toSource().compareTo(((Expression) that).node.toSource()) == 0;
		}
		return false;
	}
	public int hashCode() {
		return node.toSource().hashCode();
	}
	public String toString() {
		return node.toSource(); // + "\t\t\t\t\t<line: " + node.getLineno() + ">";
	}
}

//class ParenthesizedExpr extends Expression {
//	final Expression expr;
//	ParenthesizedExpr(Expression expr) { this.expr = expr; }
//	 
//	public void accept(Visitor v) { v.visit(this); }
//}

class FunctionExpr extends Expression  {
	/* Begin of variable addition */
	private Variable self;
	void setSelf(Variable self) { this.self = self; }
	Variable getSelf() {
		if (self == null) Logger.error("self is not resolved");
		return self;
	}
	
	private List<Variable> variables;
	
	void setParameterVariables(List<Variable> variables) {
		this.variables = variables;
	}
	List<Variable> getParameterVariables() {
		if (variables == null) Logger.error("Parameters of the function have not yet been resolved");
		return variables;
	}	
	
	private Variable functionNameVariable;
	void setFunctionNameVariable(Variable variable) {
		functionNameVariable = variable;
	}
	Variable getFunctionNameVariable() {
		if (!hasName() || functionNameVariable == null) 
			Logger.error("The function has no name or it has not yet been resolved");
		return functionNameVariable;
	}
	private Set<Variable> fv;
	Set<Variable> getFreeVariables() {
		if (fv == null) Logger.error("The free variables haven't been collected yet");
		return fv;
	}
	void setFreeVariables(Set<Variable> fv) { this.fv = fv; }
	
	private Set<Expression> returnExpressions;
	Set<Expression> getReturnExpressions() { 
		if (returnExpressions == null) Logger.error("The return expressions have not been collected yet");
		return returnExpressions; 
	}
	void setReturnExpressions(Set<Expression> exprs) { this.returnExpressions = exprs; }
	
	/* End of variable addition */
	
	/* Begin of a label for the function body */
	Label_E body_label;
	void setBodyLabel(Label_E ell) { this.body_label = ell; }
	Label_E getBodyLabel() { 
		if (body_label == null) Logger.error("The body label of function is not set");
		return body_label; 
	}
	/* End of function body label */
	
	final String name;
    final List<String> parameters;
    final Statement body;
    
    // name: @Nullable
    FunctionExpr(String name, List<String> parameters, Statement body) {
    	this.name = name;
    	this.parameters = parameters;
    	this.body = body;
    }
     
    boolean hasName() { return name != null; }
    
    public void accept(Visitor v) { v.visit(this); }
}
// e (e') where e = x | e1 e2 
class FunctionCallExpr extends Expression {
	final Expression target;
	final List<Expression> arguments;
	FunctionCallExpr (Expression target, List<Expression> arguments) {
		this.target = target;
		this.arguments = arguments;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}
// e.f(e') or e[e_f] = e'
class MethodCallExpr  extends Expression {
	final Expression receiver;
	final Expression selector;
	final List<Expression> arguments;
	MethodCallExpr (Expression receiver, Expression selector, List<Expression> arguments) {
		this.receiver = receiver;
		this.selector = selector;
		this.arguments = arguments;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}


class NewExpr extends FunctionCallExpr {
	NewExpr(Expression target, List<Expression> arguments) {
		super(target, arguments);
	}
	
	public void accept(Visitor v) { v.visit(this); }
}

// x = e'
class AssignmentExpr extends Expression {
	final Expression lValue, rValue;
	AssignmentExpr (Expression lValue, Expression rValue) {
		this.lValue = lValue;
		this.rValue = rValue;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}

// e.f = e' or e[e_f] = e'
class UpdateExpr extends Expression {
	final Expression lValue, rValue;
	final Expression selector;
	UpdateExpr (Expression lValue, Expression selector, Expression rValue) {
		this.lValue = lValue;
		this.rValue = rValue;
		this.selector = selector;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}

class VarAccessExpr extends Expression {
	/* Begin of variable addition */
	
	private Variable variable; // resolved variable
	void setVariable(Variable variable) { this.variable = variable; }
	Variable getVariable() {
		if (variable == null) {
			Logger.error("variable " + name + " is not yet resolved");
		}
		return this.variable; 
	}
	/* End of variable addition */
	
	final String name;
	 
	VarAccessExpr (String name) {
		this.name = name;
	}
	 
//	public String toString() { return name; }
	
	public void accept(Visitor v) { v.visit(this); }
}

//  object field/method selector e.g. 'f' in {f: e} | e.f | e.f(e') | e.f = e'
//  Rhino treat 'f' as just a Name and we treat Name object as VarAccess elsewhere
class SelectorExpr extends Expression {
	final String name;
	 
	SelectorExpr (String name) {
		this.name = name;
	}
	 
//	public String toString() { return name; }
	
	public void accept(Visitor v) { v.visit(this); }
}

interface GetExpr {
	Expression getTarget();
	Expression getProperty();
}
// e.f
class GetPropExpr extends Expression implements GetExpr {
	final Expression target;
	final SelectorExpr property;
	GetPropExpr(Expression target, SelectorExpr property) {
		this.target = target;
		this.property = property;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
	public Expression getTarget() { return target; }
	public Expression getProperty() { return property; }
}
// e[e_f]
class GetElemExpr extends Expression implements GetExpr {
	final Expression target, element;
	
	GetElemExpr(Expression target, Expression element) {
		this.target = target;
		this.element = element;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
	public Expression getTarget() { return target; }
	public Expression getProperty() { return element; }
}


abstract class UnaryExpr extends Expression {
	final Expression operand;
	UnaryExpr (Expression operand) { this.operand = operand; }
	 
	public void accept(Visitor v) { v.visit(this); }
}

class DeleteExpr extends UnaryExpr {
	DeleteExpr (Expression operand) { super(operand); }
	public void accept(Visitor v) { v.visit(this); }
}

class TypeOfExpr extends UnaryExpr {
	TypeOfExpr (Expression operand) { super(operand); }
	
	public void accept(Visitor v) { v.visit(this); }
}

class NegationExpr extends UnaryExpr {
	NegationExpr (Expression operand) { super(operand); }
	
	public void accept(Visitor v) { v.visit(this); }
}

class BitNotExpr extends UnaryExpr {
	BitNotExpr (Expression operand) { super(operand); }
	
	public void accept(Visitor v) { v.visit(this); }
}

class LogicNotExpr extends UnaryExpr {
	LogicNotExpr (Expression operand) { super(operand); }
	
	public void accept(Visitor v) { v.visit(this); }
}

abstract class InfixExpr extends Expression {
	final Expression left, right;
	InfixExpr (Expression left, Expression right) { this.left = left; this.right = right; }
	 
	public void accept(Visitor v) { v.visit(this); }
}

class InstanceOfExpr extends InfixExpr {
	InstanceOfExpr(Expression left, Expression right) { super(left, right); }
	
	public void accept(Visitor v) { v.visit(this); }
}

// all numeric op except plus
//Token.BITOR, Token.BITXOR, Token.BITAND, Token.LSH, Token.RSH, Token.URSH :
//Token.SUB, Token.MUL, Token.DIV, Token.MOD:

class NumericExpr extends InfixExpr {
	final int op; // use Rhino op code
	NumericExpr(Expression left, Expression right, int op) { super(left, right); this.op = op; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class AddExpr extends InfixExpr {
	AddExpr (Expression left, Expression right) { super(left, right); }
	
	public void accept(Visitor v) { v.visit(this); }
}

//Token.AND, Token.OR
class LogicExpr extends InfixExpr {
	final boolean isAnd;
	LogicExpr(Expression left, Expression right, boolean isAnd) { super(left, right); this.isAnd = isAnd; }

	public void accept(Visitor v) { v.visit(this); }
}

// Token.GT, Token.LT, Token.GE, Token.LE
class ComparisonExpr extends InfixExpr {
	final int op; // use Rhino op code
	ComparisonExpr(Expression left, Expression right, int op) { super(left, right); this.op = op; }

	public void accept(Visitor v) { v.visit(this); }
}

class EqualityExpr extends InfixExpr {
	final boolean isEqual;
	EqualityExpr(Expression left, Expression right, boolean isEqual) { super(left, right); this.isEqual = isEqual;}

	public void accept(Visitor v) { v.visit(this); }
}

class ShallowEqualityExpr extends InfixExpr {
	final boolean isEqual;
	ShallowEqualityExpr(Expression left, Expression right, boolean isEqual) { super(left, right); this.isEqual = isEqual;}

	public void accept(Visitor v) { v.visit(this); }
}

class ConditionalExpr extends Expression {
	final Expression condition, truePart, falsePart;
	ConditionalExpr(Expression c, Expression t, Expression f) {
		condition = c; truePart = t; falsePart = f;
	}
	 		
	public void accept(Visitor v) { v.visit(this); }
}

class EmptyExpr extends Expression {
	public void accept(Visitor v) { v.visit(this); }
}

class NullExpr extends Expression {
	public void accept(Visitor v) { v.visit(this); }
}

class BoolExpr extends Expression {
	final boolean value;
	BoolExpr(boolean value) { this.value = value; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class Literal extends Expression {
	final String value;
	Literal(String value) { this.value = value; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class NumberExpr extends Literal {
	final double number;
	NumberExpr(String value, double number) { super(value); this.number = number;}
	
	public void accept(Visitor v) { v.visit(this); }
}

class StringExpr extends Literal {
	StringExpr(String value) { super(value); }
	
	public void accept(Visitor v) { v.visit(this); }
}

class RegExpExpr extends Literal {
	final String flags;
	RegExpExpr (String value, String flags) { super(value); this.flags = flags; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class ObjectExpr extends Expression {
	final List<ObjProperty> properties;
	ObjectExpr(List<ObjProperty> properties) { this.properties = properties; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class ObjProperty {
	final Expression lhs, rhs; 
	final boolean isGetter, isSetter;
	ObjProperty (Expression lhs, Expression rhs, boolean isGetter, boolean isSetter) {
		this.lhs = lhs; this.rhs = rhs; this.isGetter = isGetter; this.isSetter = isSetter; 
	}
}

class ArrayExpr extends Expression {
	final List<Expression> elements;
	ArrayExpr(List<Expression> elements) { this.elements = elements; }
	 
	public void accept(Visitor v) { v.visit(this); }
}
