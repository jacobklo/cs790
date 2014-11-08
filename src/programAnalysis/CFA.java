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
		// step 1:
		Stack<SetVar> w = new Stack<SetVar>();
		// maybe we dont need?
		for (SetVar q : cache.values()){
			w.push(q);
		}
		
		//cons == push
		// step 2:
		for ( Constraint cc : constraints){
			cc.build(w);
		}
		
		//Step 3:
		while(!w.isEmpty()) {
			SetVar q = w.pop();
			for ( Constraint cc : q.e) {
				cc.iter(w);
			}
		}
		
		// step 4 ?
		
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

	abstract void build(Stack<SetVar> w);
	abstract void iter(Stack<SetVar> w);
}

// {t} subseteq p
class ConcreteConstraint extends Constraint {
	CSet<FunctionExpr> t;
	CSet<FunctionExpr> p;
	
	ConcreteConstraint(CSet<FunctionExpr> fp, CSet<FunctionExpr> ft){
		t = ft;
		p = fp;
		
	}
	@Override
	void build(Stack<SetVar> w) {
		for ( SetVar sv : w){
			if (p.containsAll(t)){ // if p subset or equal t
				if (sv.d.containsAll(p) && p.containsAll(sv.d)){ // if p == D[q]
					add(sv,t,w);
				}
			}
		}
		
	}

	@Override
	void iter(Stack<SetVar> w) {
		// do nothing
	}
	
}

// p1 subseteq p2
class SubsetConstraint extends Constraint {
	CSet<FunctionExpr> p1;
	CSet<FunctionExpr> p2;
	
	
	SubsetConstraint(CSet<FunctionExpr> cp1,CSet<FunctionExpr> cp2 ){
		p1 = cp1;
		p2 = cp2;
	}
	
	@Override
	void build(Stack<SetVar> w) {
		for (SetVar sv : w){
			if (p2.containsAll(p1)){
				
			}
		}
		
		//sv.e.add(this);
	}

	@Override
	void iter(Stack<SetVar> w) {
		for ( SetVar sv : w){
			if (p2.containsAll(p1)){ // p1 subset or equal to p2
				if (sv.d.containsAll(p2) && p2.containsAll(sv.d)){ // p2 == E[p2]
					for ( SetVar sv2 : w){
						if (sv.d.containsAll(p1) && p1.containsAll(sv.d)){
							add(sv,sv2.d,w);
						}
					}
				}
			}
		}
		
		
	}
	
}

// {t} subseteq p => p1 subseteq p2     
class ConditionalConstraint extends Constraint {
	CSet<FunctionExpr> t;
	CSet<FunctionExpr> p;
	CSet<FunctionExpr> p1;
	CSet<FunctionExpr> p2;
	
	ConditionalConstraint(CSet<FunctionExpr> ct, CSet<FunctionExpr> cp, CSet<FunctionExpr> cp1, CSet<FunctionExpr> cp2){
		t = ct;
		p = cp;
		p1 = cp1;
		p2 =cp2;
		
	}
	
	@Override
	void build(Stack<SetVar> w) {
		
		
	
	}

	@Override
	void iter(Stack<SetVar> w) {
		for ( SetVar sv : w){
			if (p2.containsAll(p1) && // p1 subset or equal to p2
					p.){ 
				if (sv.d.containsAll(p2) && p2.containsAll(sv.d)){ // p2 == E[p2]
					for ( SetVar sv2 : w){
						if (sv.d.containsAll(p1) && p1.containsAll(sv.d)){ // p1 == E[p1]
							add(sv,sv2.d,w);
						}
					}
				}
			}
		}
	}
}

class ConstraintVisitor extends FunLangVisitor {
	CSet<Constraint> constraints = new CSet<Constraint>();
	Map<Label_E, SetVar> cache = new HashMap<Label_E, SetVar>();
	Map<String, SetVar> env = new HashMap<String, SetVar>();
	Map<FunctionExpr, CSet<Expression>> functionReturns; // get this from constructor
	
	
	ConstraintVisitor(Map<FunctionExpr, CSet<Expression>> functionReturns) {
		this.functionReturns = functionReturns;
	}

	public void visit(ExpressionStmt s) {
		s.expr.accept(this);
	}

	// var x = e or let x = e ...
	public void visit(VarDecStmt s) {
		env.put(s.variable, new SetVar(""+s.variable));
		s.rValue.accept(this);
		CSet<Expression> p1 = new CSet<Expression>();
		CSet<Expression> p2 = new CSet<Expression>();
		
		p1.add(s.rValue);
		
		constraints.add(new SubsetConstraint(p1));
	}

	// return e
	public void visit(ReturnStmt s) {
		s.expr.accept(this);
	}

	// e (e')
	public void visit(FunctionCallExpr e) {
		cache.put(e.target.getLabel(), new SetVar(""+e.target.getLabel()));
		cache.put(e.getLabel(), new SetVar(""+e.getLabel()));
		CSet<Expression> p1 = new CSet<Expression>();
		
		for (Expression ex : e.arguments) {
			ex.accept(this);
			p1.add(ex);
		}
		
		constraints.add(new SubsetConstraint(p1));
		
	}

	// function(x) { s } or function f(x) { s }
	public void visit(FunctionExpr e) {
		cache.put(e.getLabel(), new SetVar(""+e.getLabel()));
		for (String s : e.parameters) {
			env.put(s, new SetVar(""+s));
		}
		
		CSet<Expression> p1 = new CSet<Expression>();
		constraints.add(new SubsetConstraint(p1));
		
		e.body.accept(this);
	}

	// if(b) then e else e'
	public void visit(ConditionalExpr e) {
		cache.put(e.getLabel(),new SetVar(""+e.getLabel()));
	    e.condition.accept(this);
	    e.truePart.accept(this);
	    e.falsePart.accept(this);
	    
	    
	}

	// x
	public void visit(VarAccessExpr e) {
		cache.put(e.getLabel(), new SetVar(""+e.getLabel()));
		
		constraints.add(new ConcreteConstraint(e));
	}

	// c
	public void visit(NumberExpr e) {
		cache.put(e.getLabel(), new SetVar(""+e.getLabel()));
	}

	public void visit(BoolExpr e) {
	}

	// e op e'
	public void visit(AddExpr e) {
		cache.put(e.getLabel(), new SetVar(""+e.getLabel()));
		e.left.accept(this);
		e.right.accept(this);
		
		constraints.add(new ConcreteConstraint(e.left));
		constraints.add(new ConcreteConstraint(e.right));
	}

	public void visit(NumericExpr e) {
	}

	public void visit(LogicExpr e) {
	}

	public void visit(ComparisonExpr e) {
	}

	// op e
	public void visit(NegationExpr e) {
		cache.put(e.getLabel(), new SetVar(""+e.getLabel()));
		e.operand.accept(this);
		
		constraints.add(new ConcreteConstraint(e.operand));
	}
}

