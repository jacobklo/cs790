package programAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Var X Context
class Binding {
	final Variable variable;
	final K_Context context;
	
	Binding(Variable variable, K_Context context) {
		this.variable = variable; this.context = context;
	}
	public boolean equals(Object that) {
		if(that instanceof Binding) {
			Binding thatBinding = (Binding) that;
			return variable.equals(thatBinding.variable) && context.equals(thatBinding.context);
		}
		return false;
	}
	public int hashCode() {
		return variable.hashCode() + context.hashCode();
	}
	public String toString() {
		return "(" + variable + ", " + context + ")";
	}
}

// Binding -> P(Closure) 
class Store {
	Map<Binding, CSet<Closure>> map = new HashMap<Binding, CSet<Closure>>();
	
	CSet<Closure> get(Binding binding) { return map.get(binding); }
	
	void put(Binding binding, CSet<Closure> d) { map.put(binding, d); }
	
	public Store clone() { Store ret = new Store(); ret.map.putAll(map); return ret; }
	
	public boolean equals(Object that) {
		if (that instanceof Store) {
			Store thatStore = (Store) that;
			
			return map.equals(thatStore.map);
		}
		return false;
	}
	public int hashCode() { return map.hashCode(); }
	public String toString() { return map.toString(); }
}

// CallExp X CE X Store X K_Context
class AI_State {
	final FunctionCallExpr call;
	final ContextEnv ce;
	final Store store;
	final K_Context context;
	
	AI_State(FunctionCallExpr call, ContextEnv ce, Store store, K_Context context) {
		this.call = call; this.ce = ce; this.store = store; this.context = context; 
	}
	public boolean equals(Object that) {
		if (that instanceof AI_State) {
			AI_State thatState = (AI_State) that;
			
			return call.equals(thatState.call) && ce.equals(thatState.ce) 
					&& store.equals(thatState.store) && context.equals(thatState.context);
		}
		return false;
	}
	public int hashCode() {
		return call.hashCode() + ce.hashCode() + store.hashCode() + context.hashCode();
	}
	public String toString() {
		return call + "\n" + ce + "\n" + store + "\n" + context;
	}
}


class Context_to_Closures extends AbstractMap<K_Context, CSet<Closure>> {
	@Override
	CSet<Closure> makeValue(String name) {
		return new CSet<Closure>();
	}
}
class AI_Cache extends AbstractCache<Context_to_Closures> {
	@Override
	Context_to_Closures makeValue(String name) {
		return new Context_to_Closures();
	}
	CSet<Closure> get(Label_E ell, K_Context delta) { return get(ell).get(delta); }
}

class AI_Visitor extends FunLangVisitor {
	final CSet<AI_State> seen;
	
	final ContextEnv ce;
	final Store store;
	final K_Context delta;
	
	final AI_Cache cache;
	
	AI_Visitor (CSet<AI_State> seen, ContextEnv ce, Store store, K_Context delta, AI_Cache cache) { 
		this.seen = seen; this.ce = ce; this.store = store; this.delta = delta; this.cache = cache;
	}
	
	
	public void visit(BlockStmt s) { 
		for(Statement t: s.statements) t.accept(this);
	}
	public void visit(EmptyStmt s) { }	
	// e
	public void visit(ExpressionStmt s) { 
		s.expr.accept(this); 
	}
	// var x = e
	public void visit(VarDecStmt s) { 
		if (s.hasInitializer()) {
			ce.put(s.getVariable(), delta);
			
			s.rValue.accept(this); 
			
			store.put(new Binding(s.getVariable(), delta), cache.get(s.rValue.label, delta));
		}
	}
	// return e
	public void visit(ReturnStmt s) { 
		if (s.hasReturnExpr()) s.expr.accept(this); 
	}
	// e1 e2 where e1 = x | e e'
	public void visit(FunctionCallExpr e) { 
		// TODO: implement this method
		for (Expression a : e.arguments) {
			a.accept(this);
			CSet<Closure> setSetOfClosureValue = cache.get(e.label, delta);
			CSet<Closure> unionSetOfClosureResult = cache.get(e.label, delta).union(cache.get(a.label, delta));
			setSetOfClosureValue.set(unionSetOfClosureResult);
			
		}
		e.target.accept(this);
		AI_State newState = new AI_State(e, ce, store, delta);
		if (!seen.contains(newState)) {
			seen.add(newState);
			for (Closure c : cache.get(e.target.label, delta)) {
				
				K_Context xkc = new K_Context();
				xkc.add(e.label);
				
				Variable bodyFuncVar = c.f.getSelf();
				Binding bin = new Binding(bodyFuncVar, xkc);
				
				ContextEnv conEnvThatPassIn = new ContextEnv();
				Store supStore = new Store();
				
				
				AI_Visitor aiv = new AI_Visitor(seen,conEnvThatPassIn ,supStore, xkc, cache);
				
				Context_to_Closures ctc = new Context_to_Closures();
				CSet<Closure> clo = ctc.makeValue("test");
				c.f.body.accept(aiv);
				
				//supStore.put(bin, clo);
				
				//store.map.putAll(supStore.map);
				//store.put(bin, vv);
			}
		}
		
		// cache.get(e.label, delta).set();
	}
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) { 
		cache.get(e.label, delta).set(new Closure(e, ce.trim(e.getFreeVariables())));
	}
	// b? e : e'
	public void visit(ConditionalExpr  e) { 
		e.condition.accept(this); 
		e.truePart.accept(this); 
		e.falsePart.accept(this); 
		
		cache.get(e.label, delta).set(cache.get(e.truePart.label, delta).union(cache.get(e.falsePart.label, delta)));
		
	}
	// x
	public void visit(VarAccessExpr e) { 
		Variable v = e.getVariable();
		cache.get(e.label, delta).set(store.get(new Binding(v, ce.get(v))));
	}
	// c
	public void visit(NumberExpr e) { }
	public void visit(BoolExpr e) { }
	public void visit(NullExpr e) { }
	// e op e'
	public void visit(AddExpr e) { visitInfix(e); }
	public void visit(NumericExpr e) { visitInfix(e); }
	public void visit(LogicExpr e) { visitInfix(e); }
	public void visit(EqualityExpr e) { visitInfix(e); }
	public void visit(ComparisonExpr e) { visitInfix(e); }
	// op e
	public void visit(NegationExpr e) { visitUnary(e); }
	public void visit(LogicNotExpr e) { visitUnary(e); }
	
	void visitInfix(InfixExpr e) { 
		e.left.accept(this); 
		e.right.accept(this); 
	}
	void visitUnary(UnaryExpr e) { 
		e.operand.accept(this); 
	}
}


public class AbstractInterpreter {
	AI_Cache cache = new AI_Cache();
	CSet<AI_State> seen = new CSet<AI_State>();
	Label_E_Visitor lv = new Label_E_Visitor();
	static String nl = "\n";
	
	AbstractInterpreter(Statement s) {
		s.accept(lv);
		s.accept(new VariableVisitor());
		
		s.accept(
			new AI_Visitor(
				seen,
				new ContextEnv(),
				new Store(),
				new K_Context(),
				cache
			)
		);
	}
	String print() {
		 
		String out = "";
		
		out += "==== labelled expressions ====" + nl;
		
		out += lv.print();
		
		out += "==== cache ====" + nl;
		out += printCache();
		
		out += "==== env ====" + nl;
		out += printEnv();
		
		return out;
	}
	String printCache() {
		List<Label_E> labels = new ArrayList<Label_E>(cache.keySet());
		Collections.sort(labels);
		
		String out = "";
		
		for(Label_E l : labels) {
			out += l + " : " + cache.get(l) + nl;
		}
		
		return out;
	}
	String printEnv() {
		String out = "";
	 	
		for(AI_State state : seen) {
			Store store = state.store;

			for(Binding b : store.map.keySet()) {
				out += b + " : " + store.get(b) + nl;
			}

		}
		
		return out;
	}
}






