package programAnalysis;

import java.util.HashMap;
import java.util.Map;

public class FunLangVisitor extends Visitor {
	private void print(Statement s) { System.out.println(s.node.toSource().split("\n")[0]); }
	
	public void visit(ScriptStmt s) { 
		// treat it like a block when we don't care about the entry point
		visit((BlockStmt) s);
	}
	public void visit(BlockStmt s) { for(Statement t: s.statements) t.accept(this);}
	public void visit(EmptyStmt s) { print(s); }	
	// e
	public void visit(ExpressionStmt s) { print(s); }
	// var x = e
	public void visit(VarDecStmt s) { print(s); }
	// return e
	public void visit(ReturnStmt s) { print(s); }
	// e (e')
	public void visit(FunctionCallExpr e) { }
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) { }
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { }
	// x
	public void visit(VarAccessExpr e) { }
	// c
	public void visit(NumberExpr e) { }
	public void visit(BoolExpr e) { }
	// e op e'
	public void visit(AddExpr e) { }
	public void visit(NumericExpr e) { }
	public void visit(LogicExpr e) { }
	public void visit(ComparisonExpr e) { }
	// op e
	public void visit(NegationExpr e) { }
	public void visit(LogicNotExpr e) { }
	
}


class Label_E implements Comparable<Label_E> {
	final int value;
	
	Label_E(int value) {
		this.value = value;
	}
	
	public String toString() { return ""+value; }

	@Override
	public int compareTo(Label_E that) {
		return value - that.value;
	}
}

class Label_E_Visitor extends FunLangVisitor {
	private int count = 0;
	private Map<Statement, CSet<Expression>> returns = new HashMap<Statement, CSet<Expression>>();
	Map<FunctionExpr, CSet<Expression>> funReturns = new HashMap<FunctionExpr, CSet<Expression>>();
	Map<Label_E, Expression> labelledExp = new HashMap<Label_E, Expression>();
	
	private void setLabel(Expression e) {
		count = count + 1;
		Label_E ell = new Label_E(count);
		e.setLabel(ell);
		labelledExp.put(ell, e);
	}
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
		setLabel(e);
	}
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) { 
		e.body.accept(this);
		setLabel(e);
		funReturns.put(e, returns.get(e.body));
	}
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { 
		e.condition.accept(this);
		e.truePart.accept(this);
		e.falsePart.accept(this);
		setLabel(e);
	}
	// x
	public void visit(VarAccessExpr e) { 
		setLabel(e);
	}
	// c
	public void visit(NumberExpr e) { 
		setLabel(e);
	}
	public void visit(BoolExpr e) { 
		setLabel(e);
	}
	// e op e'
	public void visit(AddExpr e) {
		visitInfix(e);
	}
	public void visit(NumericExpr e) { 
		visitInfix(e);
	}
	public void visit(LogicExpr e) { 
		visitInfix(e);
	}
	public void visit(ComparisonExpr e) { 
		visitInfix(e);
	}
	// op e
	public void visit(NegationExpr e) { 
		e.operand.accept(this);
		setLabel(e);
	}
	public void visit(LogicNotExpr e) { 
		e.operand.accept(this);
		setLabel(e);
	}
	
	private void visitInfix(InfixExpr e) {
		e.left.accept(this);
		e.right.accept(this);
		setLabel(e);
	}
}
