package programAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


// transfer functions<L> : (Lab -> L) -> L
interface TF_Function<L> { L apply(Analysis<L> analysis); }
// Analysis<L> : Lab -> L
class Analysis<L> extends HashMap<Label, L> { private static final long serialVersionUID = 1L; }

public abstract class InterDFA<L> {
	CSet<Label> labels;             // Lab
	CSet<Label> extremal_labels;	// E
	CSet<Edge> flow;				// F
	L extremal_value;			// iota 
	L bottom;					// bot
	
	Map<Label, L> mfp_entry = new HashMap<Label, L>(), 
				  mfp_exit = new HashMap<Label, L>();
	
	// @Return analysis lub update 
	abstract L lub(L analysis, L update);
	
	// @Return true iff update <= analysis 
	abstract boolean lessThan(L update, L analysis);
	
	// transfer functions: Lab -> TF_Function
	// this definition allows us to treat all transfer functions the same way (including function calls)
	Map<Label, TF_Function<L>> f;
 
 
	void worklistAlgorithm() {
		Stack<Edge> w = new Stack<Edge>();
		for(Edge e : flow) w.push(e);
		
		Analysis<L> analysis = new Analysis<L>();
		for(Label ell : labels) {
			if (extremal_labels.contains(ell)) analysis.put(ell, extremal_value);
			else analysis.put(ell, bottom);
		}
		
		while(!w.isEmpty()) {
			Edge e = w.pop();
			Label ell = e.left, ell_2 = e.right;
			L update = f.get(ell).apply(analysis);
			L old = analysis.get(ell_2);
			if (! lessThan(update, old)) {
				analysis.put(ell_2, lub(old, update));
				for(Edge e2 : flow) {
					if (e2.left == ell_2) w.push(e2); 
				}
			}
		}
		
		for(Label ell : labels) {
			mfp_entry.put(ell, analysis.get(ell));
			mfp_exit.put(ell, f.get(ell).apply(analysis));
		}
	}
}

abstract class GenericContext<LB> {
	private List<LB> list;
	
	abstract int getK();
	
	GenericContext() { list = new ArrayList<LB>(); }
	GenericContext(GenericContext<LB> that) {
		this.list = new ArrayList<LB>(that.list);
	}
	int size() { return list.size(); }
	void remove() { list.remove(0); }
	void add(LB l_c) { list.add(l_c); }
	LB get(int i) { return list.get(i); }
	
	void addCallStringToSelf(LB l_c) {
		if (size() >= getK()) {
			remove();
		}
		add(l_c);
	}
	@SuppressWarnings("rawtypes")
	public boolean equals(Object that) {
		if (that instanceof GenericContext) {
			return (list.equals(((GenericContext) that).list));
		}
		return false;
	}
	public int hashCode() { 
		int ret = 0;
		for(LB l : this.list) {
			ret = ret * 10 + l.hashCode();
		}
		return ret;
	}
	public String toString() {
		String empty = "\u039B";
		if (list.size() == 0) return empty;
		else return list.toString();
	}
}
