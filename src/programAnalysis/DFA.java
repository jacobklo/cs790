package programAnalysis;

import java.util.ArrayList;
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
	/**
	 * @param(L=Complete Lattice, Fx = space of functions, F = finite flow, 
	 * E = Extremal labels, i = extremal value, f = mapping)
	 * @return MFP1, MFP2
	 * 
	 * 
	 */
	void worklistAlgorithm() {
		//Step 1
		ArrayList<Edge> W = new ArrayList<Edge>();
		Map<Label, CSet<L>> analysis_new = new HashMap<Label, CSet<L>>();

		for ( Edge e : flow){
			W.add(e);
		}
		
		CSet<Label> tmp = new CSet<Label>();
		for ( Edge e : flow){
			tmp.add(e.left);
		}
		for ( Label l : extremal_labels.union(tmp)){
			if (extremal_labels.contains(l)){
				analysis_new.put(l, extremal_value);
			}
			else{
				analysis_new.put(l, bottom);
			}
		}
		
		// Step 2
		while (!W.isEmpty()){
			Label l1 = W.get(0).left;
			Label l2 = W.get(0).right;
			
			W.remove(0);
			
			if ( !lessThan(analysis_new.get(l1),analysis_new.get(l2))){
				analysis_new.put(l2, f(l1,analysis_new.get(l1)));
				for ( Edge e : flow){
					if (e.left.equals(l2)){
						W.add(e);
					}
				}
			}
		}
		
		//Step 3
		for ( Label l : extremal_labels.union(tmp)){
			mfp_entry.put(l, analysis_new.get(l));
			mfp_exit.put(l, f(l,analysis_new.get(l)));
		}
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