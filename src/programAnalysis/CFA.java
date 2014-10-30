package programAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		// implement
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
}

// {t} subseteq p
class ConcreteConstraint extends Constraint {
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

