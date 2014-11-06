package programAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class CFA {
	Map<Label_E, SetVar> cache;
	Map<String, SetVar> env;
	Set<Constraint> constraints;
	Map<Label_E, Expression> labelledExp;
	
	public CFA(Statement s) {
		// add labels and collect function expressions and the return expressions of each function
		Label_E_Visitor lv = new Label_E_Visitor();
		s.accept(lv);
		
		// generate set variables of expressions and variables, and their constraints
		ConstraintVisitor cv = new ConstraintVisitor(lv.funReturns);
		s.accept(cv);
		
		this.cache = cv.cache;
		this.env = cv.env;
		this.constraints = cv.constraints;
		this.labelledExp = lv.labelledExp;
	}
	
	void worklist() {
		// implement  P.178 table 3.7
		
		//SetVar already create a D[q], E[q]
		//dont need to do step 1,4
	}
}

class SetVar {
	CSet<FunctionExpr> d = new CSet<FunctionExpr>();
	List<Constraint> e = new ArrayList<Constraint>();
	private String name;
	
	SetVar(String name) {
		this.name = name;
	}
	
	public String toString() { return name; }
	
	boolean add(CSet<FunctionExpr> d) {
		boolean ret = false;
		if (!this.d.containsAll(d)) {
			this.d.addAll(d);
			ret = true;
		}
		return ret;
	}
}

abstract class Constraint {
	void add(SetVar q, CSet<FunctionExpr> d, Stack<SetVar> w) {
		if (q.add(d)) {
			w.push(q); // worklist is a stack
		}
		
		// so somehow the Constraint will use the e in line 41
	}

	abstract void build();
	abstract void iter();
}

// {t} subseteq p
class ConcreteConstraint extends Constraint {

	@Override
	void build() {
		// TODO Auto-generated method stub
		
	}

	@Override
	void iter() {
		// TODO Auto-generated method stub
		
	}
	
}

// p1 subseteq p2
class SubsetConstraint extends Constraint {
}

// {t} subseteq p => p1 subseteq p2     
class ConditionalConstraint extends Constraint {
}

class ConstraintVisitor extends FunLangVisitor {
	CSet<Constraint> constraints = new CSet<Constraint>();
	Map<Label_E, SetVar> cache = new HashMap<Label_E, SetVar>();
	Map<String, SetVar> env = new HashMap<String, SetVar>();
	Map<FunctionExpr, CSet<Expression>> functionReturns; // get this from constructor
	
	
	ConstraintVisitor(Map<FunctionExpr, CSet<Expression>> functionReturns) {
		this.functionReturns = functionReturns;
	}
	
	// e
	public void visit(ExpressionStmt s) {  
		s.expr.accept(this);
	}
	// var x = e or let x = e ...
	public void visit(VarDecStmt s) {  
	}
	// return e
	public void visit(ReturnStmt s) { 
	}
	// e (e')
	public void visit(FunctionCallExpr e) { 
	}
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) {
	}
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { 
	}
	// x
	public void visit(VarAccessExpr e) { 
	}
	// c
	public void visit(NumberExpr e) { }
	public void visit(BoolExpr e) { }
	// e op e'
	public void visit(AddExpr e) { }
	public void visit(NumericExpr e) { }
	public void visit(LogicExpr e) {  }
	public void visit(ComparisonExpr e) { }
	
	// op e
	public void visit(NegationExpr e) { }
	public void visit(LogicNotExpr e) { }
	
}

