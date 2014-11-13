package programAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class FunLangVisitor extends Visitor {
//	private void print(Statement s) { System.out.println(s.node.toSource().split("\n")[0]); }
	
	public void visit(ScriptStmt s) { 
		// treat it like a block when we don't care about the entry point
		visit((BlockStmt) s);
	}
	public void visit(BlockStmt s) { for(Statement t: s.statements) t.accept(this);}
	public void visit(EmptyStmt s) { }	
	// e
	public void visit(ExpressionStmt s) { s.expr.accept(this); }
	// var x = e
	public void visit(VarDecStmt s) { if (s.hasInitializer()) s.rValue.accept(this); }
	// return e
	public void visit(ReturnStmt s) { if (s.hasReturnExpr()) s.expr.accept(this); }
	// e (e')
	public void visit(FunctionCallExpr e) { e.target.accept(this); for(Expression a: e.arguments) a.accept(this); }
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) { e.body.accept(this); }
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { e.condition.accept(this); e.truePart.accept(this); e.falsePart.accept(this); }
	// x
	public void visit(VarAccessExpr e) { }
	// c
	public void visit(NumberExpr e) { }
	public void visit(BoolExpr e) { }
	// e op e'
	public void visit(AddExpr e) { visitInfix(e); }
	public void visit(NumericExpr e) { visitInfix(e); }
	public void visit(LogicExpr e) { visitInfix(e); }
	public void visit(EqualityExpr e) { visitInfix(e); }
	public void visit(ComparisonExpr e) { visitInfix(e); }
	// op e
	public void visit(NegationExpr e) { visitUnary(e); }
	public void visit(LogicNotExpr e) { visitUnary(e); }
	
	void visitInfix(InfixExpr e) { e.left.accept(this); e.right.accept(this); }
	void visitUnary(UnaryExpr e) { e.operand.accept(this); }
}


class Label_E implements Comparable<Label_E> {
	final int value;
	
	Label_E(int value) { this.value = value; }
	
	public String toString() { return ""+value; }

	@Override
	public int compareTo(Label_E that) { return value - that.value; }
	
	static final Label_E top = new Label_E(0);
	
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof Label_E) {
			ret = (value == ((Label_E) that).value);
		}
		return ret;
	}
	public int hashCode() { return value; }
}

class Variable {
	final String name;
	final Label_E scope;
	
	Variable(String name, Label_E scope) {
		this.name = name; this.scope = scope;
	}
	public String toString() {
		return name + "_" + scope;
	} 
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof Variable) {
			Variable v = (Variable) that;
			ret = (name.compareTo(v.name) == 0) && scope.equals(v.scope);
		}
		return ret;
	}
	public int hashCode() {
		return name.hashCode() * scope.value;
	}
}


class VariableScope extends HashSet<Variable> {
	private static final long serialVersionUID = 1L;
	
	Stack<Label_E> scopes = new Stack<Label_E>();
	
	void push(Label_E scope) {
		scopes.push(scope);
	}
	void pop() {
		checkEmpty();
		scopes.pop();
	}
	Label_E currentScope() {
		checkEmpty();
		return scopes.peek();
	}
	
	Variable get(String x) {
		checkEmpty();
		 
		int k = 0;
		Variable ret = null;
		while(ret == null && k < scopes.size()) {
			Variable v = new Variable(x, scopes.elementAt(k++));
		 	if (this.contains(v)) ret = v;
		}
		
		if (ret == null) {
			Logger.error("variable " + x + " is undeclared");
		}
		return ret;
	}
	
	Variable add(String x) {
		Variable v = new Variable(x, currentScope());		
		if (contains(v)) {
			Logger.warn("variable " + x + " at scope " + currentScope() + " is already declared (and overwritten)");
		}
		this.add(v);
		return v;
	}
	void checkEmpty() { if (scopes.isEmpty()) { Logger.error("accessing empty scope stack"); } }
	
}

class Logger {
	static void error(String msg) { System.out.println("ERROR: " + msg); System.exit(-1); }
	static void warn(String msg) { System.out.println("WARNing: " + msg); }
}

// resolve variable scope and local local variables of each function
// use after labels have been set
class VariableVisitor extends FunLangVisitor {
	VariableScope scope = new VariableScope();
	Stack<Set<Variable>> fv_stack = new Stack<Set<Variable>>();
	
	VariableVisitor() {
		scope.push(Label_E.top);
	}
	// var x = e
	public void visit(VarDecStmt s) {  
		s.setVariable(scope.add(s.variable));
		s.rValue.accept(this);
	}
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) { 
		scope.push(e.label);
		Set<Variable> fv = new HashSet<Variable>();
		e.setFreeVariables(fv);
		fv_stack.push(fv);
		
		int size = e.parameters.size();
		List<Variable> variables = new ArrayList<Variable>(size);
		
		for(int i=0; i < size; i++) {
			variables.add(i, scope.add(e.parameters.get(i)));
		}
		e.setParameterVariables(variables);
		
		if (e.hasName()) {
			e.setFunctionNameVariable(scope.add(e.name));
		}
		
		e.body.accept(this);
		
		fv_stack.pop();
		scope.pop();
	}
	// x
	public void visit(VarAccessExpr e) { 
		Variable v = scope.get(e.name);
		e.setVariable(v);
		
		// only collect free variables in function scope
		if (!fv_stack.isEmpty()) {
			if (v.scope != scope.currentScope()) {
				fv_stack.peek().add(v);
			}
		}
	}
}

 

class Label_E_Visitor extends FunLangVisitor {
	private int count = 0;
	private Map<Statement, CSet<Expression>> returns = new HashMap<Statement, CSet<Expression>>();
	Map<Label_E, Expression> labelledExp = new HashMap<Label_E, Expression>();
	Set<FunctionExpr> functions = new HashSet<FunctionExpr>();
	
	public void visit(BlockStmt s) { 
		CSet<Expression> rets = new CSet<Expression>();
		
		for(Statement t: s.statements) {
			t.accept(this);
			rets.addAll(returns.get(t));
			if (t instanceof ReturnStmt) break;
		}
		returns.put(s, rets);
	}
	// e
	public void visit(ExpressionStmt s) { 
		s.expr.accept(this);
		returns.put(s, new CSet<Expression>());
	}
	// var x = e
	public void visit(VarDecStmt s) {  
		s.rValue.accept(this);
		returns.put(s, new CSet<Expression>());
	}
	// return e
	public void visit(ReturnStmt s) {  
		s.expr.accept(this);
		returns.put(s, new CSet<Expression>().addOne(s.expr));
	}
	// e (e')
	public void visit(FunctionCallExpr e) {
		e.target.accept(this);
		for(Expression a : e.arguments) {
			a.accept(this);
		}
		visitExpr(e);
	}
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) { 
		functions.add(e);
		
		e.body.accept(this);
		e.setReturnExpressions(returns.get(e.body));
		visitExpr(e);
	}
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { 
		e.condition.accept(this);
		e.truePart.accept(this);
		e.falsePart.accept(this);
		visitExpr(e);
	}
	// x
	public void visit(VarAccessExpr e) { visitExpr(e); }
	// c
	public void visit(NumberExpr e) { visitExpr(e); }
	public void visit(BoolExpr e) { visitExpr(e); }
//	// e op e'
//	public void visit(AddExpr e) { visitInfix(e); }
//	public void visit(NumericExpr e) { visitInfix(e); }
//	public void visit(LogicExpr e) { visitInfix(e); }
//	public void visit(ComparisonExpr e) {  visitInfix(e); }
//	// op e
//	public void visit(NegationExpr e) { visitUnary(e); }
//	public void visit(LogicNotExpr e) {  visitUnary(e);}
	

	void visitUnary(UnaryExpr e) {
		e.operand.accept(this);
		visitExpr(e);
	}
	void visitInfix(InfixExpr e) {
		e.left.accept(this);
		e.right.accept(this);
		visitExpr(e);
	}
	private void visitExpr(Expression e) {
		setLabel(e);
	}
 
	private void setLabel(Expression e) {
		count = count + 1;
		Label_E ell = new Label_E(count);
		e.setLabel(ell);
		labelledExp.put(ell, e);
	}

}
