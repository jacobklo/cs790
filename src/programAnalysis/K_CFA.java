package programAnalysis;

import java.util.ArrayList;
import java.util.Collections;
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
 
	String printCache() {
		List<Label_E> labels = new ArrayList<Label_E>(cv.cache.keySet());
		Collections.sort(labels);
		
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
	
	final Set<Closure> processedTerms = new HashSet<Closure>(); // memoize the known functions in c1.d
	
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
		
		for(AbstractValue v : c1.d) {
			if (v.isClosure()) {
				Closure t = v.asClosure();
				
				if (!processedTerms.contains(t)) {
					processedTerms.add(t);

					for(int i = 0; i < c2_list.size() && i < t.f.getParameterVariables().size(); i++) {
						SetVar rx = env.get(t.f.getParameterVariables().get(i), delta_0); // this call will generate setvar on demand  

						SetVar c2 = c2_list.get(i);
						constraints.add(new SubsetConstraint(c2, rx));
					}
					for(Expression r : t.f.getReturnExpressions()) {
						SetVar c0 = cache.get(r.label, delta_0);

						constraints.add(new SubsetConstraint(c0, c));
					}

					// function f(x) { s }
					if (t.f.hasName()) {
						constraints.add(new ConcreteConstraint(t, env.get(t.f.getFunctionNameVariable(), delta_0)));
					}

					ContextEnv ce_0 = t.getContextEnv();
					ContextEnv ce_0_prime = ce_0.clone();
					// ce_0' = ce_0[x -> delta_0]
					for(Variable x : t.f.getParameterVariables()) ce_0_prime.put(x, delta_0);
					// ce_0' = ce_0[f -> delta_0]
					if (t.f.hasName()) ce_0_prime.put(t.f.getFunctionNameVariable(), delta_0);

					K_ConstraintVisitor cv = new K_ConstraintVisitor(ce_0_prime, delta_0, cache, env, cachedCalls);
					t.f.body.accept(cv);

					constraints.addAll(cv.constraints);
				}
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
	 
	public ContextEnv clone() {
		ContextEnv ret = new ContextEnv();
		ret.map.putAll(map);
		
		return ret;
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
	
	// c
	// e op e'
	// op e
	 
}


