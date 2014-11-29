package programAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


// heap for CFA 

// Heap : Location -> AbstractObject
// AbstractObject: Selector -> SetVar

public class Heap {
	Map<Location, AbstractObject> map = new HashMap<Location, AbstractObject>();
	final Set<Location> locations;
	final Set<Selector> selectors;
 
	Heap(Set<Location> locations, Set<Selector> selectors) {
		this.locations = locations; this.selectors = selectors;
		
		for(Location loc : locations) { add(loc); }
	}
	
	public String toString() {
		String ret = "";
		for(Location loc: locations) {
			ret += loc + " -> " + " (";
			
			int i = 0;
			
			for(Selector sel: selectors) {
				SetVar d = get(loc, sel);
				
				if (! d.isEmpty()) {
					if (i++ != 0) ret += ", ";
					ret += sel + " -> " + d;
				}
			}
			ret += ")  ";
		}
		
		return ret;
	}
	
	
	Heap(Heap that) {
		this.locations = that.locations; this.selectors = that.selectors;
		this.map = new HashMap<Location, AbstractObject>();
		for(Location loc: that.map.keySet()) map.put(loc, new AbstractObject(that.map.get(loc)));
	}
	private void add(Location loc) {
		map.put(loc, new AbstractObject(loc.toString(), selectors));
	}
	// return a new heap after updating an abstract object
	Heap update(Location loc, AbstractObject obj) {
		Heap ret = new Heap(this);
		ret.map.put(loc, obj);
		return ret;
	}
	// return a new heap after updating an abstract field
	Heap update(Selector sel) {
		Heap ret = new Heap(this);
		
		for(Location loc: locations) ret.map.get(loc).add(sel);
		
		return ret;
	}
	
	SetVar get(Location loc, Selector sel) {
		return map.get(loc).get(sel);
	}
	
	CSet<Constraint> getSubsetConstraints(Heap that) {
		CSet<Constraint> c = new CSet<Constraint>();
		for(Location loc : locations) {
			for(Selector sel : selectors) {
				c.add(new SubsetConstraint(get(loc, sel), that.get(loc, sel)));
			}
		}
		
		return c;
	}
	CSet<Constraint> getConditionalConstraints(AbstractValue t, SetVar p, Heap that) {
		CSet<Constraint> c = new CSet<Constraint>();
		for(Location loc : locations) {
			for(Selector sel : selectors) {
				c.add(new ConditionalConstraint(t, p, get(loc, sel), that.get(loc, sel)));
			}
		}
		
		return c;
	}
}


class AbstractObject  { 
	Map<Selector, SetVar> map = new HashMap<Selector, SetVar>();
	final String loc;
	
	AbstractObject(String loc, Set<Selector> selectors) { 
		this.loc = loc; 
		for(Selector sel : selectors) map.put(sel, new SetVar(loc + "." + sel));
	}

	AbstractObject(AbstractObject that) {
		loc = that.loc;
		map = new HashMap<Selector, SetVar>(that.map);
	}
	
   SetVar add(Selector sel) {
	    return map.put(sel, new SetVar(loc + "..." + sel));
	} 
   SetVar get(Selector sel) {
   	return map.get(sel);
   }
}


class Selector {
	final String sel;
	
	Selector(String sel) { this.sel = sel; }
	public String toString() { return sel; }
	
	public boolean equals(Object that) {
		if (that instanceof Selector) {
			return sel.compareTo(((Selector) that).sel) == 0;
		}
		return false;
	}
	public int hashCode() { return sel.hashCode(); }
}

