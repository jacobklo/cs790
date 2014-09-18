package programAnalysis;

import java.util.HashMap;
import java.util.HashSet;
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
	
	void setLabels() {
		for(Statement s : f_blocks.keySet()) {
			CSet<Label> l = new CSet<Label>();
			f_labels.put(s, l);
			for(Statement t : f_blocks.get(s)) {
				l.add(t.label);
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
		
		
		ret += "\n=============\n\n";
		for(Statement s : f_init.keySet()) {
			ret += s.node.toSource().split("\n")[0] + " --init--> " + f_init.get(s) + "\n";
		}
		ret += "\n=============\n\n";
		for(Statement s : f_final.keySet()) {
			ret += s.node.toSource().split("\n")[0] + " --final--> ";
			for(Label t : f_final.get(s)) {
				ret += t + " ";
			}
			ret += "\n";
		}
		ret += "\n=============\n\n";
		for(Statement s : f_labels.keySet()) {
			ret += s.node.toSource().split("\n")[0] + " --labels-- ";
			for(Label t : f_labels.get(s)) {
				ret += t + " ";
			}
			ret += "\n";
		}
		
		
		return ret;
	}
}

class Label {
	 
	final Statement stmt;
	final int value;
	
	Label(int value, Statement stmt) {
		this.stmt = stmt;
		this.value = value;
	}
	
	public String toString() { return ""+value; }
}

class Edge {
	final Label left, right;
	Edge (Label left, Label right) {
		this.left = left;
		this.right = right;
	}
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
		return "(" + left + ", " + right + ")";
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
