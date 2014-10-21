package programAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


public abstract class DFA<L> {
	
	CSet<Label> extremal_labels;	// E
	CSet<Edge> flow;				// F
	
	// @Return analysis lub update, where lub can be intersection or union
	abstract CSet<L> lub(CSet<L> analysis, CSet<L> update);
	// @Return true iff update <= analysis, where <= can be subseteq or supseteq
	abstract boolean lessThan(CSet<L> update, CSet<L> analysis);
	
	CSet<L> extremal_value;			// iota 
	CSet<L> bottom;					// bot
	
	Map<Label, CSet<L>> kill;	// kill
	Map<Label, CSet<L>> gen;	// gen
	
	CSet<L> f(Label ell, CSet<L> l) {
		return l.subtract(kill.get(ell)).union(gen.get(ell));
	}


	Map<Label, CSet<L>> analysis_0 = new HashMap<Label, CSet<L>>(),
						analysis_1 = new HashMap<Label, CSet<L>>();
	
	void chaoticIteration() {
		Set<Label> labels = kill.keySet();
		Map<Label, CSet<L>> analysis_0_new = new HashMap<Label, CSet<L>>(),
							analysis_1_new = new HashMap<Label, CSet<L>>();

		for(Label l : labels) {
			analysis_0.put(l, bottom);
			analysis_1.put(l, bottom);
		}

		boolean equal = false;

		while(!equal) {
			equal = true;
			for(Label l : labels) {
				analysis_1_new.put(l, f(l, analysis_0.get(l)));
				 
				if (extremal_labels.contains(l)) {
					analysis_0_new.put(l, extremal_value);
				}
				else {
					CSet<L> update = bottom;
					
					for(Edge e : flow) {
						if (e.right == l) {
							update = lub(update, analysis_1.get(e.left));
						}
					}
					analysis_0_new.put(l, update);
				}
			}
			for(Label l : labels) {
				if (!lessThan(analysis_0_new.get(l), analysis_0.get(l)) || !lessThan(analysis_1_new.get(l), analysis_1.get(l))) {
					equal = false;
				}
			}
			analysis_0.putAll(analysis_0_new);
			analysis_1.putAll(analysis_1_new);
		}
	}
	
	void worklistAlgorithm() {
		Stack<Edge> w = new Stack<Edge>();
		for(Edge e : flow) w.push(e);
		
		Map<Label, CSet<L>> analysis = new HashMap<Label, CSet<L>>();
		for(Label l : kill.keySet()) {
			if (extremal_labels.contains(l)) analysis.put(l, extremal_value);
			else analysis.put(l, bottom);
		}
		
		while(!w.isEmpty()) {
			Edge e = w.pop();
			Label l = e.left, l1 = e.right;
			CSet<L> update = f(l, analysis.get(l));
			CSet<L> old = analysis.get(l1);
			if (! lessThan(update, old)) {
				analysis.put(l1, lub(old, update));
				for(Edge e1 : flow) {
					if (e1.left == l1) w.push(e1); 
				}
			}
		}
		
		for(Label l : kill.keySet()) {
			mfp_entry.put(l, analysis.get(l));
			mfp_exit.put(l, f(l, analysis.get(l)));
		}
	}
	
	Map<Label, CSet<L>> mfp_entry = new HashMap<Label, CSet<L>>(), 
			mfp_exit = new HashMap<Label, CSet<L>>();
	
//	CSet<L> exit(Label ell, CSet<L> l) {
//		return f(ell, l);
//	}
//	CSet<L> entry(Label ell, Map<Label, CSet<L>> analysis) {
//		CSet<L> out = bottom;
//		
//		if (extremal_labels.contains(ell)) {
//			out = extremal_value;
//		}
//		
//		for(Edge e : flow) {
//			if (e.right == ell) {
//				out = lub(out, analysis.get(e.left));
//			}
//		}
//		return out;
//	}
	
}


