package programAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class Visitor {
	public void visit(BlockStmt s) { }
	public void visit(BreakStmt s) { }
	public void visit(CaseStmt s) { }
	public void visit(CatchStmt s) { }
	public void visit(ContinueStmt s) { }
	public void visit(EmptyStmt s) { }
	public void visit(ExpressionStmt s) { }
	public void visit(ForInStmt s) { }
	public void visit(ForStmt s) { }
	public void visit(FunctionDec s) { }
	public void visit(IfStmt s) { }
	public void visit(LoopStmt s) { }
	public void visit(ReturnStmt s) { }
	public void visit(ScriptStmt s) { }
	// should not visit abstract statement but included for completeness
	public void visit(Statement s) { }
	public void visit(SwitchStmt s) { }
	public void visit(ThrowStmt s) { }
	public void visit(TryStmt s) { }
	public void visit(VarDecListStmt s) { }
	public void visit(VarDecStmt s) { }
	public void visit(WhileStmt s) { }
	
	public void visit(AddExpr e) { }
	public void visit(ArrayExpr e) { }
	public void visit(AssignmentExpr e) { }
	public void visit(BitNotExpr e) { }
	public void visit(BoolExpr e) { }
	public void visit(ComparisonExpr e) { }
	public void visit(ConditionalExpr e) { }
	public void visit(DeleteExpr e) { }
	public void visit(EmptyExpr e) { }
	public void visit(EqualityExpr e) { }
	// should not visit abstract expression but included for completeness 
	public void visit(Expression e) { }
	public void visit(FunctionCallExpr e) { }
	public void visit(GetElemExpr e) { }
	public void visit(GetPropExpr e) { }
	public void visit(InfixExpr e) { }
	public void visit(InstanceOfExpr e) { }
	public void visit(Literal e) { }
	public void visit(LogicExpr e) { }
	public void visit(NegationExpr e) { }
	public void visit(NewExpr e) { }
	public void visit(NullExpr e) { }
	public void visit(NumberExpr e) { }
	public void visit(NumericExpr e) { }
	public void visit(ObjectExpr e) { }
//	public void visit(ObjProperty e) { }
	public void visit(ParenthesizedExpr e) { }
	public void visit(RegExpExpr e) { }
	public void visit(ShallowEqualityExpr e) { }
	public void visit(StringExpr e) { }
	public void visit(TypeOfExpr e) { }
	public void visit(UnaryExpr e) { }
	public void visit(VarAccessExpr e) { }
}


class WhileLangVisitor extends Visitor {

	private void print(Statement s) { System.out.println(s.node.toSource()); }
	
	// don't override this method. I moved it up here so you don't have to consider this anymore.
	public void visit(ScriptStmt s) { 
		// treat it like a block 
		visit((BlockStmt) s);
	}
	public void visit(BlockStmt s) { print(s); }
	public void visit(EmptyStmt s) { print(s); }
	public void visit(ExpressionStmt s) { print(s); }
	public void visit(WhileStmt s) { print(s); }
	public void visit(IfStmt s) { print(s); }
	public void visit(AssignmentExpr e) { }
	public void visit(VarAccessExpr e) { }
	public void visit(NumberExpr e) { }
	public void visit(AddExpr e) { }
	public void visit(NumericExpr e) { }
	public void visit(BoolExpr e) { }
	public void visit(LogicExpr e) { }
	public void visit(ComparisonExpr e) { }
	public void visit(NegationExpr e) { }
}

class LabelVisitor extends WhileLangVisitor {
	final CFG cfg;
	
	LabelVisitor(CFG cfg) {
		this.cfg = cfg;
	}
	 
	// // overrride visitor methods here to compute the init/final/blocks functions
	// // let me know if you need my implementation of this visitor
	
	public void visit(BlockStmt s) {
		for (Statement stmt : s.statements) {
			stmt.accept(this);
		}
		cfg.f_init.put(s, cfg.f_init.get(s.getStmt(0)));
		cfg.f_final.put(s, cfg.f_final.get(s.getLastStmt()));
		CSet<Statement> set = new CSet<Statement>();
		for (Statement stmt : s.statements) {
			set.addAll(cfg.f_blocks.get(stmt));
		}
		cfg.f_blocks.put(s, set);
	}

	public void visit(EmptyStmt s) {
		cfg.addLabel(s);
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label));
		cfg.f_blocks.put(s, new CSet<Statement>(s));
	}

	public void visit(ExpressionStmt s) {
		cfg.addLabel(s);
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label));
		cfg.f_blocks.put(s, new CSet<Statement>(s));
	}

	public void visit(IfStmt s) {
		cfg.addLabel(s);
		s.thenPart.accept(this);
		CSet<Label> x = cfg.f_final.get(s.thenPart);
		CSet<Statement> y = cfg.f_blocks.get(s.thenPart);
		if (s.hasElse()) {
			s.elsePart.accept(this);
			x = x.union(cfg.f_final.get(s.elsePart));
			y = y.union(cfg.f_blocks.get(s.elsePart));
		}
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, x);
		cfg.f_blocks.put(s, y.union(s));
	}

	public void visit(WhileStmt s) {
		cfg.addLabel(s);
		s.body.accept(this);
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label));
		cfg.f_blocks.put(s, cfg.f_blocks.get(s.body).union(s));
	}
}

class CFGVisitor extends WhileLangVisitor {
	CFG cfg;
	
	CFGVisitor(CFG cfg) {
		this.cfg = cfg;
	}
	 
	// // override visitor methods here to build your control flow graph
	// // let me know if you need my implementation of this visitor
	
	public void visit(BlockStmt s) {
		for (Statement stmt : s.statements) {
			stmt.accept(this);
		}
		CSet<Edge> flow = new CSet<Edge>();
		int k = s.getNumStmts();
		for (int i = 0; i < k - 1; i++) {
			Label s2 = cfg.f_init.get(s.getStmt(i + 1));
			for (Label s1 : cfg.f_final.get(s.getStmt(i))) {
				flow.add(new Edge(s1, s2));
			}
			flow.addAll(cfg.f_flow.get(s.getStmt(i)));
		}
		flow.addAll(cfg.f_flow.get(s.getLastStmt()));
		cfg.f_flow.put(s, flow);
	}

	public void visit(ExpressionStmt s) {
		cfg.f_flow.put(s, new CSet<Edge>());
	}

	public void visit(EmptyStmt s) {
		cfg.f_flow.put(s, new CSet<Edge>());
	}

	public void visit(WhileStmt s) {
		s.body.accept(this);
		CSet<Edge> flow = new CSet<Edge>();
		for (Label t : cfg.f_final.get(s.body)) {
			flow.add(new Edge(t, s.label));
		}
		flow.add(new Edge(s.label, cfg.f_init.get(s.body)));
		flow.addAll(cfg.f_flow.get(s.body));
		cfg.f_flow.put(s, flow);
	}

	public void visit(IfStmt s) {
		s.thenPart.accept(this);
		CSet<Edge> flow = new CSet<Edge>();
		flow.add(new Edge(s.label, cfg.f_init.get(s.thenPart)));
		flow.addAll(cfg.f_flow.get(s.thenPart));
		if (s.hasElse()) {
			s.elsePart.accept(this);
			flow.add(new Edge(s.label, cfg.f_init.get(s.elsePart)));
			flow.addAll(cfg.f_flow.get(s.elsePart));
		}
		cfg.f_flow.put(s, flow);
	}

}

class AExp_F_Visitor extends WhileLangVisitor {
	Map<Expression, CSet<Expression>> aexp;
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
	
	// override the visit methods for the while/if/empty/block/expression statements 
	// to calculate the kill and gen sets for each label
	
	public void visit(BlockStmt s) {
		for (Statement stmt : s.statements) {
			stmt.accept(this);
		}
		
	}

	public void visit(ExpressionStmt s) {
		CSet<Expression> csexp = new CSet<Expression>();
		for(Entry<Label, CSet<Expression>> l : gen.entrySet()){
			for (Expression e : l.getValue()){
				if (e.equals(s.expr))	{
					csexp.add(s.expr);
				}
			}
		}
		kill.put(s.label, csexp);
		CSet<Expression> csexp2 = new CSet<Expression>();
		csexp2.add(s.expr);
		gen.put(s.label, csexp2);
		
	}

	public void visit(EmptyStmt s) {
		 kill.put(s.label, new CSet<Expression>());
		 gen.put(s.label, new CSet<Expression>());
	}

	public void visit(WhileStmt s) {
		s.body.accept(this);
		kill.put(s.label, new CSet<Expression>());
		CSet<Expression> csexp2 = new CSet<Expression>();
		csexp2.add(s.condition);
		gen.put(s.label, csexp2);
	}

	public void visit(IfStmt s) {
		s.thenPart.accept(this);
		kill.put(s.label, new CSet<Expression>());
		CSet<Expression> csexp2 = new CSet<Expression>();
		csexp2.add(s.condition);
		gen.put(s.label, csexp2);
		if (s.hasElse()) {
			s.elsePart.accept(this);
			CSet<Expression> csexp3 = new CSet<Expression>();
			csexp3.add(s.condition);
			gen.put(s.label, csexp3);
		}
		
		
	}
}

class AExpVisitor extends WhileLangVisitor {
	Map<Expression, CSet<Expression>> aexp = new HashMap<Expression, CSet<Expression>>();
	Map<Expression, CSet<String>> fv = new HashMap<Expression, CSet<String>>();
	
 
   // override the visit methods for the five statements and all expressions in WhileLangVisitor
   // to collect all arithmethic expressions in aexp
   // and collect all free variables of each expression in fv.

	public void visit(BlockStmt s) {
		for (Statement stmt : s.statements) {
			stmt.accept(this);
		}
		
	}

	public void visit(EmptyStmt s) {
		
	}

	public void visit(ExpressionStmt s) {
		s.expr.accept(this);
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(s.expr));
		aexp.put(s.expr, csexp);
	}

	public void visit(WhileStmt s) {
		s.condition.accept(this);
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(s.condition));
		aexp.put(s.condition, csexp);
	}

	public void visit(IfStmt s) {
		s.condition.accept(this);
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(s.condition));
		aexp.put(s.condition, csexp);
	}

	public void visit(AssignmentExpr e) {
		e.lValue.accept(this);
		e.rValue.accept((this));
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(e.lValue));
		csexp.addAll(aexp.get(e.rValue));
		aexp.put(e, csexp);
	}

	public void visit(VarAccessExpr e) {
		CSet<String> s = new CSet<String>();
		s.add(e.name);
		fv.put(e, s);
	}

	public void visit(NumberExpr e) {
		CSet<String> s = new CSet<String>();
		s.add(e.value);
		fv.put(e, s);
	}

	public void visit(AddExpr e) {
		e.left.accept(this);
		e.right.accept(this);
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(e.left));
		csexp.addAll(aexp.get(e.right));
		aexp.put(e, csexp);
	}

	public void visit(NumericExpr e) {
		e.left.accept(this);
		e.right.accept(this);
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(e.left));
		csexp.addAll(aexp.get(e.right));
		aexp.put(e, csexp);
	}

	public void visit(BoolExpr e) {
		CSet<String> s = new CSet<String>();
		s.add(""+e.value);
		fv.put(e, s);
	}

	public void visit(LogicExpr e) {
		e.left.accept(this);
		e.right.accept(this);
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(e.left));
		csexp.addAll(aexp.get(e.right));
		aexp.put(e, csexp);
	}

	public void visit(ComparisonExpr e) {
		e.left.accept(this);
		e.right.accept(this);
		CSet<Expression> csexp = new CSet<Expression>();
		csexp.addAll(aexp.get(e.left));
		csexp.addAll(aexp.get(e.right));
		aexp.put(e, csexp);
	}

	public void visit(NegationExpr e) {
		e.operand.accept(this);
	    aexp.put(e, new CSet<Expression>().addAll( aexp.get(e.operand)) );
	}
 
}
