
package programAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Token;



abstract class ExtInt {
	abstract ExtInt lub(ExtInt r);
	boolean isTop() { return false; }
	boolean isBottom() { return false; }
	abstract boolean lessThan(ExtInt r);
	abstract ExtInt negate();
}
class Bottom extends ExtInt {
	ExtInt lub(ExtInt r) { return r; }
	boolean isBottom() { return true; }
	static private Bottom bottom = new Bottom();
	private Bottom() {}
	static Bottom getBottom() { return bottom; } 
	boolean lessThan(ExtInt r) { return true; }
	ExtInt negate() { return this; }
	public String toString() { return "bottom"; }
}
class Top extends ExtInt {
	ExtInt lub(ExtInt r) { return this; }
	boolean isTop() { return true; }
	static private Top top = new Top();
	private Top() {}
	static Top getTop() { return top; } 
	boolean lessThan(ExtInt r) { return this == r; }
	ExtInt negate() { return this; }
	public String toString() { return "top"; }
}
class Int extends ExtInt {
	int i;
	Int(int i) { this.i = i; }
	ExtInt lub(ExtInt r) {
		if (r.isTop()) return r;
		else if (r.isBottom()) return this;
		else if (i != ((Int) r).i) return Top.getTop();
		else return this;
	}
	boolean lessThan(ExtInt r) {
		if (r.isTop()) return true;
		if (r.isBottom()) return false;
		return i == ((Int) r).i;
	}
	ExtInt negate() { i = -i; return this; }
	public String toString() { return i + ""; }
}
// sigma: Var -> ExtInt (Z + top + bottom)
class State extends HashMap<String, ExtInt> {
	private static final long serialVersionUID = 1L;
	
	final ExtInt defaultZ;
	private State(ExtInt defaultZ) { this.defaultZ = defaultZ; }	
	static State getTopState() { return new State(Top.getTop()); }
	static State getBottomState() { return new State(Bottom.getBottom()); }
	
	State(State that) { super(that); this.defaultZ = that.defaultZ; }
	
	ExtInt getDefault() { return defaultZ; }
	
	public String toString() {
		String ret = "[";
		for(String var : keySet()) {
			ret += var + ": " + get(var) + " ";
		}
		return ret + "]";
	}
	
	State update(String x, ExtInt z) {
		State s = new State(this);
		s.put(x, z);
		return s;
	}
	State update(List<String> params, List<ExtInt> args) {
		State s = new State(this);
		for (int i=0; i<params.size()&&i<args.size(); i++) s.put(params.get(i), args.get(i));
		return s;
	}
	State update(String lhs, List<String> localVars, State sigma1) {
		State s = new State(this);
		// write return value back to lhs variable
		s.put(lhs, s.get(RET_VAR));

		localVars.add(RET_VAR);
		
		// reset any variables that are shadowed by the local variables/parameters of the called function
		for(String var : localVars) {
			if(sigma1.containsKey(var)) {
				s.put(var, sigma1.get(var));
			} else {
				s.remove(var);
			}
		}
		return s;
	}
	
	public ExtInt put(String var, ExtInt z) {
		return super.put(var, z);
	}
	
	public ExtInt get(Object var) {
		ExtInt ret = defaultZ;
		if (this.containsKey(var)) ret = super.get(var);
		return ret;
	}
	
	// not a legal variable name, we use it for function return value
	private final static String RET_VAR = "0__ret"; 
	State setReturnVar(ExtInt z) { 
		State s = new State(this);
		s.put(RET_VAR, z); 
		return s;
	}
	ExtInt getReturnVar() { return get(RET_VAR); }
	
	
	State lub(State update) {
		ExtInt defaultZ = this.getDefault().lub(update.getDefault());
		State sigma; 
		if (defaultZ == Top.getTop()) sigma = State.getTopState();
		else sigma = State.getBottomState();
		
		List<String> vars = new ArrayList<String>();
		vars.addAll(this.keySet());
		vars.addAll(update.keySet());
		
		for(String v: vars) {
			sigma.put(v, this.get(v).lub(update.get(v)));
		}
		 
		return sigma;
	}
	
	boolean lessThan(State analysis) {
		boolean ret = true;
		
		List<String> vars = new ArrayList<String>();
		vars.addAll(analysis.keySet());
		vars.addAll(this.keySet());
		
		for(String v: vars) {
			
			if (!this.get(v)
					.lessThan(
					analysis.get(v))) {
				ret = false; break;
			}
		}
		ret = ret && this.defaultZ.lessThan(analysis.defaultZ);
		return ret;
	}
}

class Context extends GenericContext<Label> {
	Context() { super(); }
	Context(Context that) { super(that); }
	
	Context addCallString(Label l_c) {
		Context ret = new Context(this);
		ret.addCallStringToSelf(l_c);
		return ret;
	}
	
	public static final int k = 1;
	int getK() { return k; }
}

class ContextualState extends HashMap<Context, State> {
	private static final long serialVersionUID = 1L;
	
	public String toString() {
		String ret = "";
		for(Context c: this.keySet()) {
			ret += c + " -> " + this.get(c) + "\t\t\t";
		}
		return ret;
	}
}

public class ConstantPropagation extends InterDFA<ContextualState> {

	@Override
	ContextualState lub(ContextualState analysis, ContextualState update) {
		ContextualState cs = new ContextualState();
		
		for(Context c : update.keySet()) {
			if (analysis.containsKey(c)) {
				cs.put(c, analysis.get(c).lub(update.get(c)));
			} else {
				cs.put(c, update.get(c));
			}
		}
		for(Context c : analysis.keySet()) {
			if (!update.containsKey(c)) {
				cs.put(c, analysis.get(c));
			}
		}
		
		return cs;
	}

	@Override
	boolean lessThan(ContextualState update, ContextualState analysis) {
		boolean ret = true;
		
		for(Context c: update.keySet()) {
			if (analysis.containsKey(c)) {
				if (!update.get(c).lessThan(analysis.get(c))) {
					ret = false;
					break;
				}
			} else {
				ret = false;
				break;
			}
		}
		
		
		return ret;
	}
	
	ConstantPropagation(Statement entry) {
		CFG cfg = new CFG();
		entry.accept(new LabelVisitor(cfg));
		entry.accept(new CFGVisitor(cfg));
		
		CP_Visitor cpv = new CP_Visitor(cfg);
		entry.accept(cpv);
		 
		this.f = cpv.f;
		
		cfg.setLabels();
		this.labels = cfg.f_labels.get(entry);
		
		this.flow = cfg.f_flow.get(entry);

		this.bottom = new ContextualState();
		 
		this.extremal_labels = new CSet<Label>(cfg.f_init.get(entry));
		
		
		ContextualState iota = new ContextualState();
		iota.put(new Context(), State.getTopState());
		
		this.extremal_value = iota;
	}
	
	
}


 


interface Function<Arg, Res> { Res apply(Arg arg); }
interface Function2<Arg, Arg2, Res> { Function<Arg2, Res> apply(Arg arg); }

// State -> State
interface CP_Fun1 extends Function<State, State> {}
// (state X State) -> State
interface CP_Fun2 { State apply(State sigma1, State sigma2); }

//CP_Fun : (Lab -> (Context -> State)) -> (Context -> State)
abstract class CP_Fun_hat implements TF_Function<ContextualState> {
	 
	//A_CP: AExp -> State -> ExtInt
	static Function2<Expression, State, ExtInt> a_cp = new Function2<Expression, State, ExtInt>() {
		public Function<State, ExtInt> apply(final Expression exp) {
			return new Function<State, ExtInt>() {
				public ExtInt apply(State sigma) {
					A_CP_Visitor v = new A_CP_Visitor(sigma);
					exp.accept(v);
					return v.A_CP.get(exp);
				}
			};
		}
	};
	
	 
	static CP_Fun_hat getIntraFunction(final Label ell, final CP_Fun1 f) {
		return new CP_Fun_hat() {
			public ContextualState apply(Analysis<ContextualState> analysis) {
				ContextualState old = analysis.get(ell);
				ContextualState cs = new ContextualState();

				for(Context c : old.keySet()) {
					cs.put(c, f.apply(old.get(c)));
				}
				return cs;
			}
		};
	}

	static CP_Fun_hat getInterFunction_l_c(final Label l_c, final CP_Fun1 f) {
		return new CP_Fun_hat() {
			public ContextualState apply(Analysis<ContextualState> analysis) {
				ContextualState old = analysis.get(l_c);
				ContextualState cs = new ContextualState();

				for(Context c : old.keySet()) {

					// delta' -> lub of f(sigma), for all delta such that delta::l_c = delta' and delta -> sigma
					Context c1 = c.addCallString(l_c);
					State s = cs.get(c1);

					if (s == null) {
						cs.put(c1, f.apply(old.get(c))); 
					} else {
						cs.put(c1, s.lub(f.apply(old.get(c))));
					}
				}

				return cs;
			}
		};
	}

	static CP_Fun_hat getInterFunction_l_r(final Label l_c, final Label l_r, final CP_Fun2 f2) {
		return new CP_Fun_hat() {
			public ContextualState apply(Analysis<ContextualState> analysis) {
				ContextualState cs_l_c = analysis.get(l_c);
				ContextualState cs_l_r = analysis.get(l_r);
				ContextualState cs = new ContextualState();

				for(Context c : cs_l_c.keySet()) {
					State sigma2 = cs_l_r.get(c.addCallString(l_c));

					if (sigma2 != null)
						cs.put(c, f2.apply(cs_l_c.get(c), sigma2));
				}

				return cs;
			}
		};
	}
	
	static CP_Fun_hat getIdFunction(final Label ell) {
		return new CP_Fun_hat() {
			public ContextualState apply(Analysis<ContextualState> arg) { return arg.get(ell); }
		};
	}
	
	static CP_Fun_hat getReturnFunction(final Label ell, final Expression exp) {
		CP_Fun1 f = new CP_Fun1() {
			public State apply(State state) {
				return state.setReturnVar(a_cp.apply(exp).apply(state));
			}

		};

		return getIntraFunction(ell, f);	
	}
	
	static CP_Fun_hat getVarDecFunction(final Label ell, final String var) {
		CP_Fun1 f = new CP_Fun1() {
			public State apply(State state) {
				return state.update(var, Top.getTop());
			}

		};

		return getIntraFunction(ell, f);	
	}
	static CP_Fun_hat getAssignFunction(final Label ell, final String var, final Expression exp) {
		CP_Fun1 f = new CP_Fun1() {
			public State apply(State state) {
				return state.update(var, a_cp.apply(exp).apply(state));
			}

		};

		return getIntraFunction(ell, f);	
	}
	static CP_Fun_hat getCallFunction(final Label l_c, final List<Expression> args, final FunctionDec functionDec) {
		 
		CP_Fun1 f = new CP_Fun1() {
			public State apply(State state) {
				List<String> params = functionDec.function.parameters;
				 
				List<ExtInt> argsZ = new ArrayList<ExtInt>();
				for(Expression a : args) argsZ.add(a_cp.apply(a).apply(state));
				
				return state.update(params, argsZ);
			}
		};

		return getInterFunction_l_c(l_c, f);	
	}
	static CP_Fun_hat getCallRetFunction(final Label l_c, final Label l_r, final String lhs,
			final FunctionDec functionDec, final List<String> localVars) {
			
		CP_Fun2 f2 = new CP_Fun2() {
			public State apply(State sigma1, State sigma2) {
				List<String> params = functionDec.function.parameters;
				localVars.addAll(params);
				 
				return sigma2.update(lhs, localVars, sigma1);
			}
		};

		return getInterFunction_l_r(l_c, l_r, f2);	
	}
}


class CP_Visitor extends WhileLangVisitor {
	// constant propagation's transfer functions: Lab -> CP_Function
	Map<Label, TF_Function<ContextualState>> f = new HashMap<Label, TF_Function<ContextualState>>();
	
	CFG cfg;
	
	CP_Visitor(CFG cfg) {
		this.cfg = cfg;
	}
	
	public void visit(BlockStmt s) { 
		for(Statement stmt: s.statements) stmt.accept(this); 
	}
	public void visit(EmptyStmt s) { f.put(s.label, CP_Fun_hat.getIdFunction(s.label));  }
	
	// regular assignment: x = e
	// function call x = f(e)
	public void visit(ExpressionStmt s) {   
		if (this.isAssignment(s)) {
			final AssignmentExpr a = (AssignmentExpr) s.expr;
			
			// make sure lhs of a is a variable
			assertLValueVar(a.lValue, a);
			final String x = ((VarAccessExpr) a.lValue).name;
			
			 if (this.isFunctionCallExpr(a)) {
				 final FunctionCallExpr c = (FunctionCallExpr) a.rValue;
				 
				 Label l_n = cfg.get_l_n(s.label);
				 if (l_n == null) {
					 log(ERROR, "called function is not declared", s);
				 }
				 FunctionDec functionDec = (FunctionDec) l_n.stmt;
		
				 f.put(s.label, CP_Fun_hat.getCallFunction(s.label, c.arguments, functionDec));
				 
				 CSet<VarDecStmt> varDecs = cfg.varDecs(functionDec.function.body);
				 List<String> vars = new ArrayList<String>();
				 for(VarDecStmt vd : varDecs) vars.add(vd.variable);
				 
				 f.put(s.label2, CP_Fun_hat.getCallRetFunction(s.label, s.label2, x, functionDec, vars));
			 } 
			 // rhs is anything but any function call
			 else {
				 f.put(s.label, CP_Fun_hat.getAssignFunction(s.label, x, a.rValue));
			 }
			
			
		}
	}

	public void visit(WhileStmt s) {  
		f.put(s.label, CP_Fun_hat.getIdFunction(s.label));
		s.body.accept(this);
	}
	public void visit(IfStmt s) { 
		f.put(s.label, CP_Fun_hat.getIdFunction(s.label));
		s.thenPart.accept(this);
		if (s.hasElse()) s.elsePart.accept(this);
	}
	
	// function declaration
	public void visit(FunctionDec s) {  
		s.function.body.accept(this);
		
		f.put(s.label, CP_Fun_hat.getIdFunction(s.label));
		f.put(s.label2, CP_Fun_hat.getIdFunction(s.label2));
	}
	
	public void visit(VarDecStmt s) {
		f.put(s.label, CP_Fun_hat.getVarDecFunction(s.label, s.variable));
	}
	public void visit(ReturnStmt s) {   
		f.put(s.label, CP_Fun_hat.getReturnFunction(s.label, s.expr));
	}
}



// State -> AExp -> ExtInt
class A_CP_Visitor extends WhileLangVisitor {
	private State sigma;
	Map<Expression, ExtInt> A_CP = new HashMap<Expression, ExtInt>();
	
	A_CP_Visitor (State sigma) {
		this.sigma = sigma;
	}
	
	public void visit(AssignmentExpr e) { 
		e.rValue.accept(this);
		A_CP.put(e, A_CP.get(e.rValue));
	}
	public void visit(FunctionCallExpr e) { log(ERROR, NO_evaluation, e); }
	public void visit(VarAccessExpr e) { 
		ExtInt i = Bottom.getBottom();
		
		String v = e.name;
		if (sigma.containsKey(v)) {
			i = sigma.get(v);
		} // if a variable is not in sigma, we return bottom 
		
		A_CP.put(e, i);
	}
	public void visit(NumberExpr e) { 
		A_CP.put(e, new Int((int) (e.number)));
	}
	public void visit(AddExpr e) { 
		e.left.accept(this);
		e.right.accept(this);
		ExtInt il = A_CP.get(e.left), 
			   ir = A_CP.get(e.right);
	
		A_CP.put(e, acp(Token.ADD, il, ir));
	}
	public void visit(NumericExpr e) { 
		e.left.accept(this);
		e.right.accept(this);
		ExtInt il = A_CP.get(e.left), 
			   ir = A_CP.get(e.right);
	
		A_CP.put(e, acp(e.op, il, ir));
	}
	public void visit(BoolExpr e) { log(ERROR, NO_evaluation, e); }
	public void visit(LogicExpr e) { log(ERROR, NO_evaluation, e); }
	public void visit(ComparisonExpr e) { log(ERROR, NO_evaluation, e); }
	public void visit(NegationExpr e) { 
		e.operand.accept(this);
		A_CP.put(e, A_CP.get(e.operand));
	}
	
	private static ExtInt acp(int op, ExtInt il, ExtInt ir) {
		ExtInt ret;
		if (il.isBottom() || ir.isBottom()) ret = Bottom.getBottom();
		else if (il.isTop() || ir.isTop()) ret = Top.getTop();
		else {
			int l = ((Int) il).i, r = ((Int) ir).i;
			int z = 0;
			// only consider the following cases, otherwise, we default to 0
			switch (op) {
			case Token.SUB: z = l-r; break; 
			case Token.MUL: z = l*r; break;
			case Token.DIV: z = l/r; break;
			case Token.ADD: z = l+r; break;
			case Token.MOD: z = l%r; break;
			case Token.BITOR: z = l|r; break; 
			case Token.BITXOR: z = l^r; break;
			case Token.BITAND: z = l&r; break;
			case Token.LSH: z = l<<r; break;
			case Token.RSH: z = l>>r; break;
			case Token.URSH: z = l>>>r; break;
			}
			ret = new Int(z);
		}
		return ret;
	}
}
