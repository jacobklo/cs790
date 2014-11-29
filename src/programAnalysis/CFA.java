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
	abstract String printCache();
	abstract String printEnv();
	
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
}


public class CFA extends AbstractCFA {
    
	ConstraintVisitor cv;

	
	public CFA(Statement s) {
		super(s);
		
		// generate set variables of expressions and variables, and their constraints
		cv = new ConstraintVisitor(lv.functions, lv.loc, lv.sel);
		s.accept(cv);
	 
	}
	
	CSet<Constraint> getConstraints() { return cv.constraints; }
	
	String print() {
		String out = super.print();
		
		out += "==== exit heap ====" + nl;
		List<Label_E> labels = new ArrayList<Label_E>(cv.cache.keySet());
		Collections.sort(labels);
		
		for(Label_E label : labels) {
			out += label + ": " + cv.state1.get(label) + nl;
		}
		
		return out;
	}
	
	String printCache() {
		List<Label_E> labels = new ArrayList<Label_E>(cv.cache.keySet());
		Collections.sort(labels);
		
		String out = "";
		
		for(Label_E l : labels) {
			out += l + " : " + cv.cache.get(l) + nl;
		}
		
		return out;
	}
	
	String printEnv() {
		List<Variable> labels = new ArrayList<Variable>(cv.env.keySet());
		Collections.sort(labels);
		
		String out = "";
		
		for(Variable x : cv.env.keySet()) {
			out += x + " : " + cv.env.get(x) + nl;
		}
		
		return out;
	}
}

class SetVar {
	CSet<AbstractValue> d = new CSet<AbstractValue>();
	List<Constraint> e = new ArrayList<Constraint>();
	final String name; // used for debugging purpose
	
	SetVar(String name) {
		this.name = name;
	}
	boolean isEmpty() {
		return d.isEmpty();
	}
	public String toString() { 
		String ret = "[";

		int i = 0;
		
		for(AbstractValue t : d) {
			ret += t;
			i++;
			if (i < d.size()) ret += ", ";
		}
		
		return ret + "]";
	}
	
	boolean add(CSet<AbstractValue> d) {
		boolean ret = false;
		if (!this.d.containsAll(d)) {
			this.d.addAll(d);
			ret = true;
		}
		return ret;
	}
}




class ConstraintVisitor extends FunLangVisitor {
	final CSet<Constraint> constraints = new CSet<Constraint>();
	final Cache cache = new Cache();
	final Environment env = new Environment();
	final Set<FunctionExpr> functions; // get this from constructor
	
	// NEW: entry/exit heap for each label/expression
	final CFA_State state0, state1;
	// NEW: all locations (of new objects) and all selectors
	final Set<Location> locations;
	final Set<Selector> selectors;
	// NEW: the exit heap of the previous expression
	Heap current_heap;
	
	ConstraintVisitor(Set<FunctionExpr> functions, Set<Location> locations, Set<Selector> selectors) {
		this.functions = functions; this.locations = locations; this.selectors = selectors;
		current_heap = new Heap(locations, selectors);
		state0 = new CFA_State(locations, selectors); state1 = new CFA_State(locations, selectors);
	}
	
	// e
	public void visit(ExpressionStmt s) {  
		s.expr.accept(this);
	}
	// var x = e or let rec x = e 
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
		// NEW: store the entry heap
		state0.put(e.label, current_heap);
		
		e.target.accept(this);

		List<SetVar> c_arguments = new ArrayList<SetVar>();
		
		for(Expression a : e.arguments) {
			a.accept(this);
			c_arguments.add(cache.get(a.label));
		}
		SetVar c_call_target = cache.get(e.target.label);

		// delegate rest of the logic to this method
		visitCall(e.label, c_arguments, c_call_target);
	}

	
	// NEW: used by both function call and method call visitor
	
	private void visitCall(Label_E call_expr_label,  List<SetVar> c_arguments, SetVar c_call_target) {
		// create a blank heap for the exit of the call expression
		Heap exit_heap = state1.get(call_expr_label);
		
		SetVar c_call_expr = cache.get(call_expr_label);
		
		for(FunctionExpr f : functions) {
			for(int i = 0; i < c_arguments.size() && i < f.getParameterVariables().size(); i++) {
				SetVar r_parameter = env.get(f.getParameterVariables().get(i)); // this call will generate setvar on demand  
				
				SetVar c_argument = c_arguments.get(i);
				constraints.add(new ConditionalConstraint(new Term(f), c_call_target, c_argument, r_parameter));
			}
			
			Label_E ell_function_body = f.getBodyLabel();
			Heap heap_function_entry = state0.get(ell_function_body);
			SetVar c_function_body = cache.get(ell_function_body);
			Heap heap_function_exit = state1.get(ell_function_body); 
			Term t = new Term(f);
			
			// forall loc, sel: t in c_call_target => current_heap(loc, sel) <= heap_function_entry(loc, sel)
			constraints.addAll(current_heap.getConditionalConstraints(t, c_call_target, heap_function_entry));
			
			// forall loc, sel: t in c_call_target => heap_function_exit(loc, sel) <= exit_heap(loc, sel)
			constraints.addAll(heap_function_exit.getConditionalConstraints(t, c_call_target, exit_heap));
			 
			// t in c_call_target => c_function_body <= c_call_expr
			constraints.add(new ConditionalConstraint(t, c_call_target, c_function_body, c_call_expr));
		}
		
		// update the current heap
		current_heap = exit_heap;
	}
	
	
	// function(x) { s } or function f(x) { s } 
	public void visit(FunctionExpr e) {
		// NEW: store the entry heap
		state0.put(e.label, current_heap);
				
		SetVar c = cache.get(e.label);
		
		constraints.add(new ConcreteConstraint(new Term(e), c));

		// function f(x) { s }
		if (e.name != null) {
			constraints.add(new ConcreteConstraint(new Term(e), env.get(e.getFunctionNameVariable())));
		}
		
		// NEW: set the curret heap to the entry heap of the function body
		current_heap = state0.get(e.getBodyLabel());
		
		e.body.accept(this);
		
		// NEW: get exit heap of the function body
		SetVar c_body = cache.get(e.getBodyLabel());
		Heap heap = state1.get(e.getBodyLabel());
		
		for(Expression r : e.getReturnExpressions()) {
			SetVar c0 = cache.get(r.label);
			constraints.add(new SubsetConstraint(c0, c_body));
			
			// NEW: subset constraints: forall loc, sel. heap_return_expression(loc, sel) <= heap(loc, sel)
			Heap heap_return_expression = state1.get(r.label); 
			constraints.addAll(heap_return_expression.getSubsetConstraints(heap));
		}
	 	if (e.getReturnExpressions().size() == 0) 
			Logger.error("We can't handle functions without return expressions", e);	
		
		// NEW: restore the current heap since the function body should not change the heap of the function expression
		current_heap = state0.get(e.label);
		state1.put(e.label, current_heap);
	}
	// if(b) then e else e'
	public void visit(ConditionalExpr  e) { 
		// NEW: store the entry heap
		state0.put(e.label, current_heap);
				
		SetVar c = cache.get(e.label);
		
		e.condition.accept(this);
		e.truePart.accept(this);
		e.falsePart.accept(this);
		constraints.add(new SubsetConstraint(cache.get(e.truePart.label), c));
		constraints.add(new SubsetConstraint(cache.get(e.falsePart.label), c));
		
		// NEW: store the exit heap
		state1.put(e.label, current_heap);
	}
	// x
	public void visit(VarAccessExpr e) { 
		// NEW: store the entry heap
		state0.put(e.label, current_heap);
				
		SetVar c = cache.get(e.label);
		SetVar rx = env.get(e.getVariable());
	
		constraints.add(new SubsetConstraint(rx, c));
		
		// NEW: store the exit heap
		state1.put(e.label, current_heap);
	}
	
	// c
	public void visit(NumberExpr e) { visitConstant(e); }
	public void visit(BoolExpr e) { visitConstant(e); }
	public void visit(StringExpr e) { visitConstant(e); }
	public void visit(NullExpr e) { visitConstant(e); }
	
	// op e
	void visitUnary(UnaryExpr e) {
		// NEW: store the entry heap
		state0.put(e.label, current_heap);
		e.operand.accept(this);
		
		// NEW: store the exit heap
		state1.put(e.label, current_heap);
	}
	// e op e'
	void visitInfix(InfixExpr e) {
		// NEW: store the entry heap
		state0.put(e.label, current_heap);
		
		e.left.accept(this);
		e.right.accept(this);
		
		// NEW: store the exit heap
		state1.put(e.label, current_heap);
	}
	
	private void visitConstant(Expression e) {
		// NEW: store the entry/exit heap
		state0.put(e.label, current_heap);
		state1.put(e.label, current_heap);
		
		AbstractValue v;
		if (e instanceof NullExpr) {
			v = new NullValue();
		} else {
			v = new AbstractConstant(e.toString());
		}
		
		constraints.add(new ConcreteConstraint(v, cache.get(e.label)));
	}
	
	// NEW: { f_1 : e_1, f_2 : e_2, ... }
	// f_i is either a selector, a string or number literal
	public void visit(ObjectExpr e) { 
		// store the entry heap
		state0.put(e.label, current_heap);
		
		
		// each property has the form lhs: rhs
		// lhs can be a StringExpr, a NumberExpr, or a VarAccessExpr (but it is not a variable access - just a name)
		for(ObjProperty p : e.properties) {
			p.rhs.accept(this); 
		}
		
		constraints.add(new ConcreteConstraint(new Location(e.label), cache.get(e.label)));
		
		// create an abstract object representing this object literal
		AbstractObject obj = new AbstractObject(e.label.toString(), selectors);
		
		// C(l_e_i) <= obj(f_i)
		for(ObjProperty p : e.properties) {
			constraints.add(new SubsetConstraint(cache.get(p.rhs.label), obj.get(getSelector(p.lhs))));
		}
		// update current heap and set it as the exit heap of this expression
		current_heap = current_heap.update(new Location(e.label), obj);
		state1.put(e.label, current_heap);
	}
	
	// this will fail if the sel is not a selector expression (e.g. string or number literal)
	private Selector getSelector(Expression sel) {
		if (! (sel instanceof SelectorExpr) ) 
			Logger.error("selector " + sel + " of an object literal is not a symbol");
		return new Selector(((SelectorExpr) sel).name);
	}
	
	// NEW: e'.f 
	public void visit(GetPropExpr e) {
		// TODO: implement this method
	}

	// NEW: e'[e_f]
	public void visit(GetElemExpr e) {
		Logger.error("We don't consider element get yet");
	}
	
	// NEW: x = e'  
	public void visit(AssignmentExpr e) {
		// store the entry heap
		state0.put(e.label, current_heap);
		
		e.rValue.accept(this);
		
		SetVar c_e_prime = cache.get(e.rValue.label);
		
		if ( !(e.lValue instanceof VarAccessExpr) ) 
			Logger.error("left hand side of an assignment is not a variable", e);
		else {
			Variable x = ((VarAccessExpr) e.lValue).getVariable();
			SetVar r_x = env.get(x);
			
			// C(l_e') <= r(x)
			constraints.add(new SubsetConstraint(c_e_prime, r_x));
		}
		
		SetVar c_e = cache.get(e.label);
		
		// C(l_e') <= C(l_e)
		constraints.add(new SubsetConstraint(c_e_prime, c_e));
		
		// store the exit heap
		state1.put(e.label, current_heap);
	}
	
	// NEW: e1.f = e2 
	public void visit(UpdateExpr e) { 
		// TODO: implement this method
	}
	
	// NEW: e1.f(e2) 
	public void visit(MethodCallExpr e) { 
		// TODO: implement this method
	}
}


class CFA_State  {
	final Map<Label_E, Heap> map = new HashMap<Label_E, Heap>();
	final Set<Location> locations;
	final Set<Selector> selectors;
	
	CFA_State(Set<Location> locations, Set<Selector> selectors) {
		this.locations = locations; this.selectors = selectors;
	}
	
	Heap get(Label_E ell) { 
		Heap ret = map.get(ell); 
		if(ret == null) {
			ret = new Heap(locations, selectors);
			map.put(ell, ret);
		}
		return ret;
	}
	void put(Label_E ell, Heap heap) { map.put(ell, heap); }
}

