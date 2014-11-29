package programAnalysis;

// abstract value for CFA

public abstract class AbstractValue {
	boolean isLocation() { return false; }
	boolean isClosure() { return false; }
	Location asLocation() { return (Location) this; }
	Closure asClosure() { return (Closure) this; }
}

// some constant: integer, string, or boolean 
class AbstractConstant extends AbstractValue {
	final String value;
	
	AbstractConstant(String value) { this.value = value; }
	public String toString() { return value; }
}
// null value 
class NullValue extends AbstractValue {
	public String toString() { return Constant.NULL; }
}

// object location in heap
class Location extends AbstractValue {
	final Label_E label;
	Location(Label_E label) { this.label = label; }
	public String toString() { return "obj/" + label; }
	public boolean equals(Object that) {
		if (that instanceof Location) {
			return label.equals(((Location) that).label);
		}
		return false;
	}
	public int hashCode() { return label.hashCode(); }
	boolean isLocation() { return true; }
}

// function term
class Term extends AbstractValue {
	final FunctionExpr f;
	
	Term(FunctionExpr f) { this.f = f; }
	public String toString() { return "fn/" + f.label; }
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof Term) {
			Term t = (Term) that;
			ret = f.equals(t.f);
		}
		return ret;
	}
	public int hashCode() { return f.hashCode(); }
}

// closure t in rho
class Closure extends AbstractValue {
	private final ContextEnv ce;
	final FunctionExpr f;
	
	Closure(FunctionExpr f, ContextEnv ce) {
		this.f = f; this.ce = ce;
	}
	public String toString() { return "cls(" + f.label + ", " + ce + ")"; }
	
	public boolean equals(Object that) {
		boolean ret = false;
		if (that instanceof Closure) {
			Closure t = (Closure) that;
			ret = f.equals(t.f) && ce.equals(t.ce);
		}
		return ret;
	}
	public int hashCode() { return f.hashCode() + ce.hashCode(); }
	
	public boolean isClosure() { return true; }
	ContextEnv getContextEnv() { return ce; }
}
