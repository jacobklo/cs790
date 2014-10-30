package programAnalysis;

import java.util.HashMap;
import java.util.Map;


public class AvailableExpression extends DFA<Expression> {

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

class AExp_F_Visitor extends WhileLangVisitor {
	Map<Expression, CSet<Expression>> aexp ;
	Map<Expression, CSet<String>> fv ;
	Map<Label, CSet<Expression>> kill = new HashMap<Label, CSet<Expression>>(), 
			gen = new HashMap<Label, CSet<Expression>>();

	// bottom of the lattice
	CSet<Expression> aexpStar = new CSet<Expression>();

	AExp_F_Visitor(Map<Expression, CSet<Expression>> aexp, Map<Expression, CSet<String>> fv ) {
		this.aexp = aexp;
		this.fv = fv;

		for (CSet<Expression> set : aexp.values()) aexpStar.addAll(set);
	}

	public void visit(BlockStmt s) { 
		for(Statement t : s.statements) t.accept(this);
	}
	public void visit(EmptyStmt s) { 
		kill.put(s.label, new CSet<Expression>());
		gen.put(s.label, new CSet<Expression>());
	}

	// only applies to assignments of the form x := a
	public void visit(ExpressionStmt s) { 
	
		if (isAssignment(s)) {
			AssignmentExpr as = (AssignmentExpr) s.expr;

			if (as.lValue instanceof VarAccessExpr) {

				String x = ((VarAccessExpr) as.lValue).name; 

				CSet<Expression> set = new CSet<Expression>();
				for(Expression a : aexpStar) {
					if (fv.get(a).contains(x)) set.add(a);
				}
				kill.put(s.label, set);

				set = new CSet<Expression>();
				for(Expression a: aexp.get(as.rValue)) {
					if (!fv.get(a).contains(x)) set.add(a);
				}
				gen.put(s.label, set);

			}
		}
	}
	public void visit(WhileStmt s) { 
		kill.put(s.label, new CSet<Expression>());
		gen.put(s.label, aexp.get(s.condition));

		s.body.accept(this);
	}
	public void visit(IfStmt s) { 
		kill.put(s.label, new CSet<Expression>());
		gen.put(s.label, aexp.get(s.condition));

		s.thenPart.accept(this);
		if (s.hasElse()) s.elsePart.accept(this);
	}

}

class AExpVisitor extends WhileLangVisitor {
	Map<Expression, CSet<Expression>> aexp = new HashMap<Expression, CSet<Expression>>();
	Map<Expression, CSet<String>> fv = new HashMap<Expression, CSet<String>>();


	public void visit(BlockStmt s) { 
		for(Statement t : s.statements) t.accept(this);
	}
	public void visit(EmptyStmt s) { }
	public void visit(ExpressionStmt s) { 
		s.expr.accept(this);
	}
	public void visit(WhileStmt s) { 
		s.condition.accept(this);
		s.body.accept(this);
	}
	public void visit(IfStmt s) { 
		s.condition.accept(this);
		s.thenPart.accept(this);
		if (s.hasElse())
			s.elsePart.accept(this);
	}
	public void visit(AssignmentExpr e) { 
		e.rValue.accept(this);
	}
	public void visit(VarAccessExpr e) { 
		fv.put(e, new CSet<String>(e.name));
	}
	public void visit(NumberExpr e) { }
	public void visit(AddExpr e) { 
		e.left.accept(this);
		e.right.accept(this);
		aexp.put(e,  new CSet<Expression>(e).addAll(aexp.get(e.left)).addAll(aexp.get(e.right)));
		fv.put(e, new CSet<String>().addAll(fv.get(e.left)).addAll(fv.get(e.right)));
	}
	public void visit(NumericExpr e) { 
		e.left.accept(this);
		e.right.accept(this);
		aexp.put(e,  new CSet<Expression>(e).addAll(aexp.get(e.left)).addAll(aexp.get(e.right)));
		fv.put(e, new CSet<String>().addAll(fv.get(e.left)).addAll(fv.get(e.right)));
	}
	public void visit(BoolExpr e) { }
	public void visit(LogicExpr e) { 
		e.left.accept(this);
		e.right.accept(this);
		aexp.put(e, new CSet<Expression>().addAll(aexp.get(e.left)).addAll(aexp.get(e.right)));
	}
	public void visit(ComparisonExpr e) { 
		e.left.accept(this);
		e.right.accept(this);
		aexp.put(e, new CSet<Expression>().addAll(aexp.get(e.left)).addAll(aexp.get(e.right)));
	}
	public void visit(NegationExpr e) { 
		e.operand.accept(this);
		aexp.put(e, new CSet<Expression>().addAll( aexp.get(e.operand)) );
	}
}

