package programAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

class WorkList {
	private final Stack<SetVar> stack = new Stack<SetVar>();
	void push(SetVar v) { stack.push(v); }
	SetVar pop() { return stack.pop(); }
	boolean isEmpty() { return stack.isEmpty(); }
}

abstract class AbstractCFA {
	Label_E_Visitor lv;
	static final String nl = "\n";
	
	abstract CSet<Constraint> getConstraints();
	
	AbstractCFA(Statement s) {
		// add labels and collect function expressions and the return expressions of each function
		lv = new Label_E_Visitor();
		s.accept(lv);
		s.accept(new VariableVisitor());
				
	}
	
	void worklist() {
		WorkList w = new WorkList();
		for(Constraint cc : getConstraints()) 
			cc.build(w);
		
		while (!w.isEmpty()) {
			SetVar q = w.pop();
			for(Constraint cc : q.e) {
				cc.iter(w);
			}
		}
	}
	abstract String printCache(List<Label_E> labels);
	abstract String printEnv();
	
	String print() {
		List<Label_E> labels = new ArrayList<Label_E>(lv.labelledExp.keySet());
		Collections.sort(labels);
		String out = "";
		
		out += "==== labelled expressions ====" + nl;
		
		for(Label_E l : labels) {
			out += l + " : " + lv.labelledExp.get(l) + nl;
		}
		
		out += "==== cache ====" + nl;
		out += printCache(labels);
		
		out += "==== env ====" + nl;
		out += printEnv();
		
		return out;
	}
}


public class CFA extends AbstractCFA {
    
	ConstraintVisitor cv;

	
	public CFA(Statement s) {
		super(s);
		
		// generate set variables of expressions and variables, and their constraints
		cv = new ConstraintVisitor(lv.functions);
		s.accept(cv);
	 
	}
	
	CSet<Constraint> getConstraints() { return cv.constraints; }
	
	String printCache(List<Label_E> labels) {
		String out = "";
		
		for(Label_E l : labels) {
			out += l + " : " + cv.cache.get(l) + nl;
		}
		
		return out;
	}
	
	String printEnv() {
		String out = "";
		
		for(Variable x : cv.env.keySet()) {
			out += x + " : " + cv.env.get(x) + nl;
		}
		
		return out;
	}
}

class SetVar {
	CSet<Term> d = new CSet<Term>();
	List<Constraint> e = new ArrayList<Constraint>();
	final String name; // used for debugging purpose
	
	SetVar(String name) {
		this.name = name;
	}
	
	public String toString() { 
		String ret = "";

		int i = 0;
		
		for(Term t : d) {
			ret += t;
			i++;
			if (i < d.size()) ret += ", ";
		}
		
		return ret;
	}
	
	boolean add(CSet<Term> d) {
		boolean ret = false;
		if (!this.d.containsAll(d)) {
			this.d.addAll(d);
			ret = true;
		}
		return ret;
	}
}

class Term {
	final FunctionExpr f;
	
	Term(FunctionExpr f) { this.f = f; }
	public String toString() { return "" + f.label; }
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof Term) {
			Term t = (Term) that;
			ret = f.equals(t.f);
		}
		return ret;
	}
	public int hashCode() { return f.hashCode(); }
	
	// A terrible hack to achieve reuse without adding too much type parameters.
	ContextEnv getContextEnv() { 
		Logger.error("This is not a closure object. Should not get context environment from a term object"); 
		return null;
	}
}

abstract class Constraint {
	void add(SetVar q, CSet<Term> d, WorkList w) {
		if (q.add(d)) {
			w.push(q); // worklist is a stack
		}
	}
	
	abstract void build(WorkList w);
	abstract void iter(WorkList w);
}

// {t} subseteq p
class ConcreteConstraint extends Constraint {
	final Term t;
	final SetVar p;
	
	ConcreteConstraint(Term t, SetVar p) {
		this.t = t; this.p = p;
	}
	
	@Override
	void build(WorkList w) {
		add(p, new CSet<Term>(t), w);
	}
	@Override
	void iter(WorkList w) {}
	
	public String toString() {
		return t + " <= " + p.name;
	}
}

// p1 subseteq p2
class SubsetConstraint extends Constraint {
	final SetVar p1, p2;

	SubsetConstraint(SetVar p1, SetVar p2) {
		this.p1 = p1; this.p2 = p2;
	}
	
	@Override
	void build(WorkList w) { 
		p1.e.add(this);  
		add(p2, p1.d, w);  // this is added so that constraints dynamically generated in K-CFA can be properly merged
	}

	@Override
	void iter(WorkList w) { add(p2, p1.d, w); }
	
	public String toString() {
		return p1.name + " <= " + p2.name;
	}
}

// {t} subseteq p => p1 subseteq p2
class ConditionalConstraint extends Constraint {
	final Term t;
	final SetVar p, p1, p2;
	
	ConditionalConstraint(Term t, SetVar p, SetVar p1, SetVar p2) {
		this.t = t; this.p = p; this.p1 = p1; this.p2 = p2;
	}
	
	@Override
	void build(WorkList w) {
		p.e.add(this);
		p1.e.add(this);
	}
	@Override
	void iter(WorkList w) {
		if (p.d.contains(t)) {
			add(p2, p1.d, w);
		}
	}
	
	public String toString() {
		return "(" + t + " <= " + p.name + " ==> " + p1.name + ")" + " <= " + p2.name;
	}
}

abstract class AbstractCache<V> {
	Map<Label_E, V> map = new HashMap<Label_E, V>();
	
	abstract V makeSetVar(String name);
	
	V get(Label_E ell) {
		V ret = map.get(ell);
		if (ret == null) {
			ret =  makeSetVar(ell.toString());
			map.put(ell, ret);
		}
		return ret;
	}
}

abstract class AbstractEnv<V> {
	Map<Variable, V> map = new HashMap<Variable, V>();
	 
	abstract V makeSetVar(String name);
	
	V get(Variable x) {  
		V ret = map.get(x);
		if (ret == null) {
			ret =  makeSetVar(x.toString());
			map.put(x, ret);
		}
		return ret;
	}
	
	Set<Variable> keySet() { return map.keySet(); }
}


class Cache extends AbstractCache<SetVar> {
	SetVar makeSetVar(String name) { return new SetVar(name); }
}
class Environment extends AbstractEnv<SetVar> {
	SetVar makeSetVar(String name) { return new SetVar(name); }
}


class ConstraintVisitor extends FunLangVisitor {
	CSet<Constraint> constraints = new CSet<Constraint>();
	Cache cache = new Cache();
	Environment env = new Environment();
	Set<FunctionExpr> functions; // get this from constructor
	
	
	ConstraintVisitor(Set<FunctionExpr> functions) {
		this.functions = functions;
	}
	
	// e
	public void visit(ExpressionStmt s) {  
		s.expr.accept(this);
	}
	// var x = e or let x = e ...
	public void visit(VarDecStmt s) {  
		// assume there is initializer
		if (s.hasInitializer()) {
			SetVar c1 = cache.get(s.rValue.label);
			constraints.add(new SubsetConstraint(c1, env.get(s.var)));
			
			s.rValue.accept(this);		
		} 
		// else throw an exception
	}
	// return e
	public void visit(ReturnStmt s) { 
		if (s.expr != null) {
			s.expr.accept(this);
		}
		// else throw an exception
	}
	// e (e')
	public void visit(FunctionCallExpr e) { 
		
		SetVar c = cache.get(e.label);
		
		e.target.accept(this);
		SetVar c1 = cache.get(e.target.label);

		for(Expression a : e.arguments) {
			a.accept(this);
		}
		
		for(FunctionExpr t : functions) {
			for(int i = 0; i < e.arguments.size() && i < t.getParameterVariables().size(); i++) {
				SetVar rx = env.get(t.getParameterVariables().get(i)); // this call will generate setvar on demand  
				if (rx != null) {
					SetVar c2 = cache.get(e.arguments.get(i).label);
					constraints.add(new ConditionalConstraint(new Term(t), c1, c2, rx));
				} 
				// else throw an exception
			}
			for(Expression r : t.getReturnExpressions()) {
				SetVar c0 = cache.get(r.label);
				if (c0 != null)
					constraints.add(new ConditionalConstraint(new Term(t), c1, c0, c));
				// else throw an exception
			}
		}
	
	}
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) {
		 
		SetVar c = cache.get(e.label);
		
		constraints.add(new ConcreteConstraint(new Term(e), c));

		// function f(x) { s }
		// TODO check here, it said this is delay to the Call Constraint class
		
		if (e.name != null) {
			constraints.add(new ConcreteConstraint(new Term(e), env.get(e.getFunctionNameVariable())));
		}
		e.body.accept(this);
	}
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { 
		SetVar c = cache.get(e.label);
		
		e.condition.accept(this);
		e.truePart.accept(this);
		e.falsePart.accept(this);
		constraints.add(new SubsetConstraint(cache.get(e.truePart.label), c));
		constraints.add(new SubsetConstraint(cache.get(e.falsePart.label), c));
	}
	// x
	public void visit(VarAccessExpr e) { 
		SetVar c = cache.get(e.label);
		SetVar rx = env.get(e.getVariable());
		if (rx != null)
			constraints.add(new SubsetConstraint(rx, c));
		// else variable is not found, throw an exception
	}
//	// c
//	public void visit(NumberExpr e) {  }
//	public void visit(BoolExpr e) {  }
	
//	// e op e'
//	public void visit(AddExpr e) { visitInfix(e); }
//	public void visit(NumericExpr e) { visitInfix(e); }
//	public void visit(LogicExpr e) { visitInfix(e); }
//	public void visit(ComparisonExpr e) { visitInfix(e); }
//	// op e
//	public void visit(NegationExpr e) { e.operand.accept(this); }
//	public void visit(LogicNotExpr e) { e.operand.accept(this); }
	
	void visitInfix(InfixExpr e) {
		e.left.accept(this);
		e.right.accept(this);
	}
	

	
}

