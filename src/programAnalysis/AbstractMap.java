package programAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// generic definitions for CFA

public abstract class AbstractMap<K, V> {
	Map<K, V> map = new HashMap<K, V>();
	
	abstract V makeValue(String name);
	
	V get(K key) {
		V ret = map.get(key);
		if (ret == null) {
			ret =  makeValue(key.toString());
			map.put(key, ret);
		}
		return ret;
	}
	Set<K> keySet() { return map.keySet(); }
	
	public String toString() {
		String ret = "{ ";

		int i = 0;
		
		for(K sel : map.keySet()) {
			ret += sel + " -> " + map.get(sel);
			i++;
			if (i < map.size()) ret += ", ";
		}
		
		return ret + " }";
	}
}

abstract class AbstractCache<V> extends AbstractMap<Label_E, V> {}

abstract class AbstractEnv<V> extends AbstractMap<Variable, V> {}

// definitions for CFA

class Cache extends AbstractCache<SetVar> {
	SetVar makeValue(String name) { return new SetVar(name); }
}
class Environment extends AbstractEnv<SetVar> {
	SetVar makeValue(String name) { return new SetVar(name); }
}


// definitiosn for K-CFA

class Context_to_SetVar extends AbstractMap<K_Context, SetVar> { 
	final String name;
	
	Context_to_SetVar(String name) { this.name = name; }

	@Override
	SetVar makeValue(String ctx) {
		 return new SetVar(name + "/" + ctx);
	} 
}

class K_Cache extends AbstractCache<Context_to_SetVar> {
	@Override
	Context_to_SetVar makeValue(String l) {
		return new Context_to_SetVar(l);
	}	
	
	SetVar get(Label_E ell, K_Context delta) {
		return get(ell).get(delta);
	}
}

class K_Env extends AbstractEnv<Context_to_SetVar> {
	@Override
	Context_to_SetVar makeValue(String x) {
		return new Context_to_SetVar(x);
	}	
	
	SetVar get(Variable x, K_Context delta) {
		return get(x).get(delta);
	}
}
