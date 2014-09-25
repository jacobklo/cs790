package programAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public abstract class DFA<L> {
	
	CSet<Label> extremal_labels;	// E
	CSet<Edge> flow;				// F
	
	// the least upper bound operator
	// @Return the lub of analysis and update, where lub can be intersection or union
	abstract CSet<L> lub(CSet<L> analysis, CSet<L> update);
	
	// the partial order 
	// @Return true iff update lessThan analysis, where lessThan can be subseteq or supseteq
	abstract boolean lessThan(CSet<L> update, CSet<L> analysis);
	
	CSet<L> extremal_value;		// iota 
	CSet<L> bottom;				// bot
	
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
	
	void worklistAlgorithm() { }
	
	Map<Label, CSet<L>> mfp_entry = new HashMap<Label, CSet<L>>(), 
						mfp_exit = new HashMap<Label, CSet<L>>();
}


class AvailableExpression extends DFA<Expression> {
 
	// implement the following two hook methods: just one line for each method
	CSet<Expression> lub(CSet<Expression> analysis, CSet<Expression> update) {
		return null;
	}
	boolean lessThan(CSet<Expression> update, CSet<Expression> analysis) {
		return true;
	}
	
	AvailableExpression(Statement entry) {
		// assign values to the fields inherited from DFA class
	}
}
