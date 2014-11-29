package programAnalysis;


// constraints for CFA

public abstract class Constraint {
	void add(SetVar q, CSet<AbstractValue> d, WorkList w) {
		if (q.add(d)) {
			w.push(q); // worklist is a stack
		}
	}
	
	abstract void build(WorkList w);
	abstract void iter(WorkList w);
}

// {t} subseteq p
class ConcreteConstraint extends Constraint {
	final AbstractValue t;
	final SetVar p;
	
	ConcreteConstraint(AbstractValue t, SetVar p) {
		this.t = t; this.p = p;
	}
	
	@Override
	void build(WorkList w) {
		add(p, new CSet<AbstractValue>(t), w);
	}
	@Override
	void iter(WorkList w) {}
	
	public String toString() {
		return t + " <= " + p.name;
	}
}

// p1 subseteq p2
class SubsetConstraint extends Constraint {
	final SetVar p1, p2;

	SubsetConstraint(SetVar p1, SetVar p2) {
		this.p1 = p1; this.p2 = p2;
	}
	
	@Override
	void build(WorkList w) { 
		p1.e.add(this);  
		add(p2, p1.d, w);  // this is added so that constraints dynamically generated in K-CFA can be properly merged
	}

	@Override
	void iter(WorkList w) { add(p2, p1.d, w); }
	
	public String toString() {
		return p1.name + " <= " + p2.name;
	}
}

// {t} subseteq p => p1 subseteq p2
class ConditionalConstraint extends Constraint {
	final AbstractValue t;
	final SetVar p, p1, p2;
	
	ConditionalConstraint(AbstractValue t, SetVar p, SetVar p1, SetVar p2) {
		if (t == null || p == null || p1 == null || p2 == null) 
			Logger.error("null arguments to conditional constraints");
		
		this.t = t; this.p = p; this.p1 = p1; this.p2 = p2;
	}
	
	@Override
	void build(WorkList w) {
		p.e.add(this);
		p1.e.add(this);
	}
	@Override
	void iter(WorkList w) {
		if (p.d.contains(t)) {
			add(p2, p1.d, w);
		}
	}
	
	public String toString() {
		return "(" + t + " <= " + p.name + " ==> " + p1.name + ")" + " <= " + p2.name;
	}
}


