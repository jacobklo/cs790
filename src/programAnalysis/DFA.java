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
	}
	
	Map<Label, CSet<L>> mfp_entry = new HashMap<Label, CSet<L>>(), 
			mfp_exit = new HashMap<Label, CSet<L>>();
}


class AvailableExpression extends DFA<Expression> {
 
	CSet<Expression> lub(CSet<Expression> analysis, CSet<Expression> update) {
		return analysis.intersect(update);
	}
	boolean lessThan(CSet<Expression> update, CSet<Expression> analysis) {
		return update.containsAll(analysis);
	}
	
	AvailableExpression(Statement entry) {
		CFG cfg = new CFG();
		entry.accept(new LabelVisitor(cfg));
		entry.accept(new CFGVisitor(cfg));
		
		AExpVisitor aexpv = new AExpVisitor();
		entry.accept(aexpv);
		AExp_F_Visitor aexpfv = new AExp_F_Visitor(aexpv.aexp, aexpv.fv);
		entry.accept(aexpfv);
		
		this.flow = cfg.f_flow.get(entry);
		this.bottom = aexpfv.aexpStar;
		this.gen = aexpfv.gen;
		this.kill = aexpfv.kill;
		this.extremal_labels = new CSet<Label>(cfg.f_init.get(entry));
		this.extremal_value = new CSet<Expression>();
	}
}