package programAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class K_CFA extends AbstractCFA {
	K_ConstraintVisitor cv;
	 
	K_CFA (Statement s) {
		super(s);
 	
		// generate set variables of expressions and variables, and their constraints
		cv = new K_ConstraintVisitor(new ContextEnv(), new K_Context(), new K_Cache(), new K_Env(), new HashSet<CallAbstraction>());
		s.accept(cv);
	}

	CSet<Constraint> getConstraints() {
		return cv.constraints;
	}
 
	String printCache(List<Label_E> labels) {
		String out = "";
		for(Label_E l : labels) out += l + " : " + cv.cache.get(l) + nl;
		return out;
	}
	
	String printEnv() {
		String out = "";
		for(Variable x : cv.env.keySet()) out += x + " : " + cv.env.get(x) + nl;
		return out;
	}
}

class Closure extends Term {
	private final ContextEnv ce;
	// contains function Term and ContextEnv
	Closure(FunctionExpr f, ContextEnv ce) {
		super(f); this.ce = ce;
	}
	public String toString() { return "cls(" + f.label + ", " + ce + ")"; }
	
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof Closure) {
			Closure t = (Closure) that;
			ret = f.equals(t.f) && ce.equals(t.ce);
		}
		return ret;
	}
	public int hashCode() { return f.hashCode() + ce.hashCode(); }
	
	ContextEnv getContextEnv() { return ce; }
}

// For function call of the form ( t_1^{ell_1} t_2^{ell_2} )^ell
// For each t in C(ell_1, delta).d => generate constraints for t (t_2)
class CallConstraint extends Constraint {
	final SetVar c;  			// C(ell, delta)
	final SetVar c1; 			// C(ell_1, delta)
	final List<SetVar> c2_list; // C(ell_2, delta)
	final K_Context delta_0;
	final K_Cache cache; 
	final K_Env env;
	final Set<CallAbstraction> cachedCalls;
	
	final Set<Term> processedTerms = new HashSet<Term>(); // memoize the known functions in c1.d
	
	CallConstraint (SetVar c, SetVar c1, List<SetVar> c2_list, 
						K_Context delta_0, 
						K_Cache cache, K_Env env,
						Set<CallAbstraction> cachedCalls) {
		this.c = c; this.c1 = c1; this.c2_list = c2_list; 
		this.delta_0 = delta_0; 
		this.cache = cache; this.env = env;
		this.cachedCalls = cachedCalls; // has to take this global cache and pass it to the next constraint visitor
	}
	
	@Override
	void build(WorkList w) {
		c1.e.add(this);
	}

	@Override
	void iter(WorkList w) {
		CSet<Constraint> constraints = new CSet<Constraint>();
		
		for(Term t : c1.d) {
			if (!processedTerms.contains(t)) {
				processedTerms.add(t);
			
				// 
				// TODO: 1. generate constraints related to the call to the function t.f
				//       2. use a new constraint visitor to collect constraints for the body of t.f
				//	 3. put the new constraints in "constraints" variable
				//
			}
		}
		
		for(Constraint cc : constraints) {
			cc.build(w); // add nodes/edges to the graph
		}
	}
	
	public String toString() {
		String ret =  c.name + " = " + c1.name  + "(";
		for (SetVar c2 : c2_list) ret += c2.name + " ";
		return ret + ")";
	}
}

class ContextEnv {
	private final Map<Variable, K_Context> map;
	
	K_Context get(Variable v) { return map.get(v); }
	void put(Variable v, K_Context c) { map.put(v, c); }
	Set<Variable> keySet() { return map.keySet(); }
	
	ContextEnv() {
		map = new HashMap<Variable, K_Context>();
	}
	ContextEnv(ContextEnv that) {
		map = new HashMap<Variable, K_Context>(that.map);
	}
	
	ContextEnv trim(Set<Variable> fv) {
		ContextEnv ret = new ContextEnv();
		for(Variable v : fv) ret.put(v, get(v));
		return ret;
	}
	
	public String toString() { 
		String ret = "{";

		int i = 0;
		
		for(Variable v : map.keySet()) {
			ret += v + " -> " + map.get(v);
			i++;
			if (i < map.size()) ret += ", ";
		}
		
		return ret + "}";
	}
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof ContextEnv) {
			ret = map.equals(((ContextEnv) that).map);
		}
		return ret;
	}
	public int hashCode() {
		return map.hashCode();
	}
}

class ContextSetVar {
	private Map<K_Context, SetVar> map = new HashMap<K_Context, SetVar>();
	String name;
	
	ContextSetVar(String name) { this.name = name; }
	
	SetVar get(K_Context delta) {
		SetVar ret = map.get(delta);
		if (ret == null) {
			ret = new SetVar(name + "/" + delta);
			map.put(delta, ret);
		}
		
		return ret;
	}
	
	public String toString() {
		String ret = "{ ";

		int i = 0;
		
		for(K_Context k : map.keySet()) {
			ret += k + " -> " + map.get(k);
			i++;
			if (i < map.size()) ret += ", ";
		}
		
		return ret + " }";
	}
}

class K_Cache extends AbstractCache<ContextSetVar> {
	@Override
	ContextSetVar makeSetVar(String name) {
		return new ContextSetVar(name);
	}	
	
	SetVar get(Label_E ell, K_Context delta) {
		return get(ell).get(delta);
	}
}

class K_Env extends AbstractEnv<ContextSetVar> {
	@Override
	ContextSetVar makeSetVar(String name) {
		return new ContextSetVar(name);
	}	
	
	SetVar get(Variable x, K_Context delta) {
		return get(x).get(delta);
	}
}

class K_Context  extends GenericContext<Label_E> {
	K_Context() { super(); }
	K_Context(K_Context that) { super(that); }
	
	K_Context addCallString(Label_E l_c) {
		K_Context ret = new K_Context(this);
		ret.addCallStringToSelf(l_c);
		return ret;
	}
	
	public static final int k = 1;
	int getK() { return k; }
}

class CallAbstraction {
	final ContextEnv ce;
	final K_Context delta;
	final Label_E ell;
	// --- how many unit in this to pervent inifint loop
	CallAbstraction(ContextEnv ce, K_Context delta, Label_E ell) {
		this.ce = ce; this.delta = delta; this.ell = ell;
	}
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof CallAbstraction) {
			CallAbstraction c = (CallAbstraction) that;

			ret = ce.equals(c.ce) && delta.equals(c.delta) && ell.equals(c.ell);
		}
		return ret;
	}
	public int hashCode() {
		return ce.hashCode() + delta.hashCode() + ell.hashCode();
	}
}

class K_ConstraintVisitor extends FunLangVisitor {
	CSet<Constraint> constraints = new CSet<Constraint>();
	final K_Cache cache;
	final K_Env env;
	
	final ContextEnv ce;
	final K_Context delta;
	
	final Set<CallAbstraction> cachedCalls; // a global cache to remember call expressions "ell" visited 
											// for each context environment "ce" and each context "delta"
	
	K_ConstraintVisitor(ContextEnv ce, K_Context delta,
														K_Cache cache, K_Env env,
														Set<CallAbstraction> cachedCalls) {
		this.ce = ce; this.delta = delta;
		this.cache = cache; this.env = env;
		this.cachedCalls = cachedCalls;
	}
	
	// e
	public void visit(ExpressionStmt s) {  
		s.expr.accept(this);
	}
	// var x = e or let x = e ...
	public void visit(VarDecStmt s) {  
		// assume there is initializer
		if (s.hasInitializer()) {
		 
			SetVar c1 = cache.get(s.rValue.label, delta);
			// C(l1, delta) <= rho(x, delta)
			constraints.add(new SubsetConstraint(c1, env.get(s.getVariable(), delta)));
			//ce' = ce[x -> delta]
			ce.put(s.getVariable(), delta);
			
			// this has to come after "ce" is updated since "e" in "var x = e in ... " may depend on "x" in "ce"
			// "var x = e" is actually more of a "lec rec x = e" (in O'Caml) than "let x = e in ..." in ML
			// since "let x = e in ... " does not allow "e" depend on "x"
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
	// e1 (e2)
	public void visit(FunctionCallExpr e) { 
		CallAbstraction ca = new CallAbstraction(ce, delta, e.label);
		
		if (!cachedCalls.contains(ca)) { // check cache to insure this is not already visited before
			cachedCalls.add(ca);         // add to cache so that we don't visit it again in the future.
			
			e.target.accept(this);
			for(Expression a : e.arguments) {
				a.accept(this);
			}


			SetVar c = cache.get(e.label, delta);
			SetVar c1 = cache.get(e.target.label, delta);

			List<SetVar> c2List = new ArrayList<SetVar>();
			for(Expression a: e.arguments) c2List.add(cache.get(a.label, delta));

			K_Context delta_0 = delta.addCallString(e.label);

			constraints.add(new CallConstraint(c, c1, c2List, delta_0, cache, env, cachedCalls));
		}
	}
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) {
		SetVar c = cache.get(e.label, delta);
		ContextEnv ce_0 = ce.trim(e.getFreeVariables());
		constraints.add(new ConcreteConstraint(new Closure(e, ce_0), c));
	}
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { 
		SetVar c = cache.get(e.label, delta);
		
		e.condition.accept(this);
		e.truePart.accept(this);
		e.falsePart.accept(this);
		constraints.add(new SubsetConstraint(cache.get(e.truePart.label, delta), c));
		constraints.add(new SubsetConstraint(cache.get(e.falsePart.label, delta), c));
	}
	// x
	public void visit(VarAccessExpr e) { 
		SetVar c = cache.get(e.label, delta);
		Variable v = e.getVariable();
		SetVar rx = env.get(v, ce.get(v));
	
		constraints.add(new SubsetConstraint(rx, c));
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
//	public void visit(NegationExpr e) { visitUnary(e); }
//	public void visit(LogicNotExpr e) { visitUnary(e); }
//	
//	void visitInfix(InfixExpr e) {
//		e.left.accept(this);
//		e.right.accept(this);
//	}
//	
//	void visitUnary(UnaryExpr e) {
//		e.operand.accept(this);
//	}
}


