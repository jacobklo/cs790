package programAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CFG {
	 
	int k=1;
	
	void addLabel(Statement s) {
		s.setLabel(new Label(k++, s));
	}

	// use statements as blocks
	
	// init: Stmt -> Lab
	Map<Statement, Label> f_init = new HashMap<Statement, Label>();
	// final: Stmt -> P(Lab)
	Map<Statement, CSet<Label>> f_final = new HashMap<Statement, CSet<Label>>();
	// blocks: Stmt -> P(Blocks)
	Map<Statement, CSet<Statement>> f_blocks = new HashMap<Statement, CSet<Statement>>();
	// labels: Stmt -> P(Lab)
	Map<Statement, CSet<Label>> f_labels = new HashMap<Statement, CSet<Label>>();
	// flow: Stmt -> P(Lab X Lab)
	Map<Statement, CSet<Edge>> f_flow = new HashMap<Statement, CSet<Edge>>();
	// flow^R: Stmt -> P(Lab X Lab)
	Map<Statement, CSet<Edge>> f_flow_r = new HashMap<Statement, CSet<Edge>>();
	
	/* BEGIN: inter-procedural CFG definitions */
	
	// for function call (l_r) and function declaration (l_x)
	void addLabel2(Statement s) {
		s.setLabel2(new Label(k++, s));
	}
	
	// interflow: P(Lab X Lab X Lab X Lab)
	CSet<InterEdge> interflow = new CSet<InterEdge>();
	
	Label get_l_n(Label l_c) {
		Label l_n = null;
		for (InterEdge e : interflow) {
			if (e.lc == l_c) { l_n = e.ln;  break; }
		}
		return l_n;
	}
	
	// functions: String -> FunctionDec
	Map<String, FunctionDec> functions = new HashMap<String, FunctionDec>();
	
	// returns: Statement -> P(ReturnStmt)
	Map<Statement, CSet<ReturnStmt>> returns = new HashMap<Statement, CSet<ReturnStmt>>();
	
	// varDecs: Statement -> P(VarDecStmt)
	Map<Statement, CSet<VarDecStmt>> varDecs = new HashMap<Statement, CSet<VarDecStmt>>();
	
	CSet<ReturnStmt> returns(Statement s) {
		CSet<ReturnStmt> r = new CSet<ReturnStmt>();
		if (returns.containsKey(s)) r = returns.get(s);
		return r;
	}
	CSet<VarDecStmt> varDecs(Statement s) {
		CSet<VarDecStmt> v = new CSet<VarDecStmt>();
		if (varDecs.containsKey(s)) v = varDecs.get(s);
		return v;
	}
	void setReturn(Statement s, ReturnStmt r) { returns.put(s, new CSet<ReturnStmt>(r)); }
	void setReturn(Statement s, CSet<ReturnStmt> r) { returns.put(s, r); }
	void setVarDec(Statement s, VarDecStmt v) { varDecs.put(s, new CSet<VarDecStmt>(v)); }
	void setVarDec(Statement s, CSet<VarDecStmt> v) { varDecs.put(s, v); }
	
	/* END: inter-procedural CFG definitions */
	
	
	void setLabels() {
		for(Statement s : f_blocks.keySet()) {
			CSet<Label> l = new CSet<Label>();
			f_labels.put(s, l);
			for(Statement t : f_blocks.get(s)) {
				l.add(t.label);
				if (t.hasLabel2()) l.add(t.label2);
			}
		}
	}
	
	void setReverseFlow() {
		for(Statement s : f_flow.keySet()) {
			f_flow_r.put(s, new CSet<Edge>());
		}
		for(Statement s : f_flow.keySet()) {
			for(Edge e : f_flow.get(s)) {
				f_flow_r.get(e.right.stmt).add(e.reverse());
			}
		}
	}
	
	String print() {
		String ret = "";
		
		List<Label> labels = new ArrayList<Label>();
		
		for(Statement s : f_init.keySet()) {
			if (s.hasLabel()) labels.add(s.label);
			if (s.hasLabel2()) labels.add(s.label2);
		}
		
		Collections.sort(labels);
		
		for (Label l : labels) {
			ret += l + " " + l.stmt.node.toSource();
		}
		
		ret += "\n=============\n\n";
		for(Label l : labels) {
			ret += l + " --init--> " + f_init.get(l.stmt) + "\n";
		}
		ret += "\n=============\n\n";
		for(Label l : labels) {
			ret += l + " --final--> ";
			for(Label t : f_final.get(l.stmt)) {
				ret += t + " ";
			}
			ret += "\n";
		}
		ret += "\n=============\n\n";
		for(Label l : labels) {
			ret += l + " --labels-- ";
			for(Label t : f_labels.get(l.stmt)) {
				ret += t + " ";
			}
			ret += "\n";
		}
		
		
		return ret;
	}
}

class Label implements Comparable<Label> {
	 
	final Statement stmt;
	final int value;
	
	Label(int value, Statement stmt) {
		this.stmt = stmt;
		this.value = value;
	}
	
	public String toString() { return ""+value; }

	@Override
	public int compareTo(Label that) {
		return value - that.value;
	}
}

class Edge {
	final Label left, right;
	private boolean isInterFlow = false;
	
	Edge (Label left, Label right) {
		this.left = left;
		this.right = right;
	}
	Edge (Label left, Label right, boolean isInterFlow) {
		this(left, right);
		this.isInterFlow = isInterFlow;
	}
	
	boolean isInterFlow() { return isInterFlow; }
	
	Edge reverse() {
		return new Edge(right, left);
	}
	public boolean equals(Object o) {
		if (o instanceof Edge) {
			Edge e = (Edge) o;
			return (e.left == left) && (e.right == right);
		}
		return false;
	}
	public int hashCode() {
		return left.value - right.value;
	}
	public String toString() {
		if (isInterFlow) 
			return "(" + left + "; " + right + ")";
		else 
			return "(" + left + ", " + right + ")";
	}
}

class InterEdge{
	final Label lc, ln, lx, lr;
	
	InterEdge(Label lc, Label ln, Label lx, Label lr) {
		this.lc = lc; this.ln = ln; this.lx = lx; this.lr = lr;
	}
	public String toString() {
		return "(" + lc + ", " + ln + ", " + lx + ", " + lr + ")";
	}
}

class CSet<X> extends HashSet<X> {
	private static final long serialVersionUID = 1L;
	 
	CSet() { super(); }
	CSet (X element) {
		this();
		add(element);
	}
	CSet<X> union(CSet<X> set) { 
		CSet<X> empty = new CSet<X>();
		empty.addAll(this);
		empty.addAll(set);
		return empty;
	}
	CSet<X> union(X element) {
		CSet<X> empty = new CSet<X>();
		empty.addAll(this);
		empty.add(element);
		return empty;
	}
	CSet<X> addAll(CSet<X> set) {
		if (set != null) super.addAll(set);
		return this;
	}
	CSet<X> addOne(X element) {
		if (element != null) super.add(element);
		return this;
	}
	CSet<X> subtract(CSet<X> set) {
		CSet<X> newSet = new CSet<X>().addAll(this);
		newSet.removeAll(set);
		return newSet;
	}
	CSet<X> intersect(CSet<X> set) {
		CSet<X> newSet = new CSet<X>().addAll(this);
		newSet.retainAll(set);
		return newSet;
	}
}
