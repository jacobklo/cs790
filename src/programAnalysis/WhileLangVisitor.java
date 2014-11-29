package programAnalysis;

import java.util.ArrayList;
import java.util.List;


public class WhileLangVisitor extends Visitor {

	private void print(Statement s) { System.out.println(s.node.toSource().split("\n")[0]); }
	
	public void visit(ScriptStmt s) { 
		// treat it like a block when we don't care about the entry point
		visit((BlockStmt) s);
	}
	public void visit(BlockStmt s) { for(Statement t: s.statements) t.accept(this);}
	public void visit(EmptyStmt s) { print(s); }
	
	// regular assignment: x = e
	// function call x = f(e)
	public void visit(ExpressionStmt s) { print(s); }

	public void visit(WhileStmt s) { print(s); s.body.accept(this); }
	public void visit(IfStmt s) { print(s); s.thenPart.accept(this); if (s.hasElse()) s.elsePart.accept(this);}
	
	// function declaration
	public void visit(FunctionDec s) { print(s); s.function.body.accept(this); }
	public void visit(VarDecStmt s) { print(s); }
	public void visit(ReturnStmt s) { print(s); }
	
	public void visit(AssignmentExpr e) { }
	public void visit(FunctionCallExpr e) { }
	public void visit(VarAccessExpr e) { }
	public void visit(NumberExpr e) { }
	public void visit(AddExpr e) { }
	public void visit(NumericExpr e) { }
	public void visit(BoolExpr e) { }
	public void visit(LogicExpr e) { }
	public void visit(ComparisonExpr e) { }
	public void visit(NegationExpr e) { }
	
	boolean isFunctionCall(ExpressionStmt s) {
		return isAssignment(s) && isFunctionCallExpr((AssignmentExpr) s.expr);
	}
	boolean isFunctionCallExpr(AssignmentExpr a) {
		return a.rValue instanceof FunctionCallExpr;
	}
	boolean isAssignment(ExpressionStmt s) {
		return s.expr instanceof AssignmentExpr;
	}
	
	void assertLValueVar(Expression lhs, Expression e) {
		if (!(lhs instanceof VarAccessExpr)) {
			Logger.error("lhs of assignment is not a variable", e);
		}
	}

	List<Statement> filterFunDec(List<Statement> stmts) {
		List<Statement> ret = new ArrayList<Statement>();
		
		for(Statement s: stmts) {
			if (! (s instanceof FunctionDec)) {
				ret.add(s);
			}
		}
		
		return ret;
	}
	

	static final String NO_Statement = "block has no statement (that is not a function declaration).",
						NO_evaluation = "cannot evaluate this expression";
						
}


class LabelVisitor extends WhileLangVisitor {
	final CFG cfg;
	
	LabelVisitor(CFG cfg) {
		this.cfg = cfg;
	}
	
	// function declaration
	public void visit(FunctionDec s) {
		
		cfg.addLabel(s);
		s.function.body.accept(this); 
		
		cfg.addLabel2(s);
		
		cfg.functions.put(s.name, s);
		
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label2));
		
		
		
		cfg.f_blocks.put(s, new CSet<Statement>(s).addAll(cfg.f_blocks.get(s.function.body)));
	}
	public void visit(VarDecStmt s) { 
		cfg.addLabel(s);
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label));
		cfg.f_blocks.put(s, new CSet<Statement>(s));
		
		cfg.setVarDec(s,s);
	}
	public void visit(ReturnStmt s) { 
		cfg.addLabel(s);
		cfg.f_init.put(s, s.label);
		
		// return statement has empty final set since it flows to the end of the function
		cfg.f_final.put(s, new CSet<Label>());
		cfg.f_blocks.put(s, new CSet<Statement>(s));
		
		cfg.setReturn(s,s);
	}
	
	// assume BlockStmt contains at least one statement
	public void visit(BlockStmt s) {
		
		List<Statement> stmts = s.statements;
		 
		for(Statement stmt : stmts) {
			stmt.accept(this);
		}
		
		CSet<Statement> set = new CSet<Statement>();
		
		for(Statement stmt: stmts) {
			set.addAll(cfg.f_blocks.get(stmt));
		}
		cfg.f_blocks.put(s, set);
		
		// remove all function declarations from the block before going further
		stmts = filterFunDec(stmts);
		
		if (stmts.size() == 0) { 
			Logger.warn(NO_Statement, s);
		} else {
			cfg.f_init.put(s, cfg.f_init.get(stmts.get(0)));
			cfg.f_final.put(s, cfg.f_final.get(stmts.get(stmts.size()-1)));
		}
		CSet<ReturnStmt> r = new CSet<ReturnStmt>();
		CSet<VarDecStmt> v = new CSet<VarDecStmt>();
		for(Statement stmt: stmts) {
			r.addAll(cfg.returns(stmt));
			v.addAll(cfg.varDecs(stmt));
		}
		cfg.setReturn(s, r);
		cfg.setVarDec(s, v);
	}
	public void visit(EmptyStmt s) { 
		cfg.addLabel(s);
		
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label));
		cfg.f_blocks.put(s, new CSet<Statement>(s));
	}
	public void visit(ExpressionStmt s) {
		cfg.addLabel(s);
		
		if (isFunctionCall(s)) {
		
			cfg.addLabel2(s);
			cfg.f_init.put(s, s.label);
			cfg.f_final.put(s, new CSet<Label>(s.label2));
			cfg.f_blocks.put(s, new CSet<Statement>(s));	
		} 
		else {
			cfg.f_init.put(s, s.label);
			cfg.f_final.put(s, new CSet<Label>(s.label));
			cfg.f_blocks.put(s, new CSet<Statement>(s));
		}
	}
	public void visit(IfStmt s) {
		cfg.addLabel(s);
		 
		s.thenPart.accept(this);

		CSet<Label> x = cfg.f_final.get(s.thenPart);
		CSet<Statement> y = cfg.f_blocks.get(s.thenPart);
		
		if(s.hasElse()) {
			s.elsePart.accept(this);
			x = x.union(cfg.f_final.get(s.elsePart));
			y = y.union(cfg.f_blocks.get(s.elsePart));
		}
		
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, x);
		cfg.f_blocks.put(s, y.union(s));
		
		CSet<ReturnStmt> r = cfg.returns(s.thenPart);
		CSet<VarDecStmt> v = cfg.varDecs(s.thenPart);
		if (s.hasElse()) {
			r.addAll(cfg.returns(s.elsePart));
			v.addAll(cfg.varDecs(s.elsePart));
		}
		cfg.setReturn(s, r);
		cfg.setVarDec(s, v);
	}
	public void visit(WhileStmt s) {
		cfg.addLabel(s);
		 
		s.body.accept(this);
		
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label));
		cfg.f_blocks.put(s, cfg.f_blocks.get(s.body).union(s));
		
		cfg.setReturn(s, cfg.returns(s.body));
		cfg.setVarDec(s, cfg.varDecs(s.body));
	}
}

class CFGVisitor extends WhileLangVisitor {
	CFG cfg;
	
	CFGVisitor(CFG cfg) {
		this.cfg = cfg;
	}
	
	 
	public void visit(FunctionDec s) {
		s.function.body.accept(this);
		
		CSet<Edge> flow = new CSet<Edge>();
		
		// l_n flows to the init of the function body
		flow.add(new Edge(s.label, cfg.f_init.get(s.function.body)));
		flow.addAll(cfg.f_flow.get(s.function.body));
		
		// finals of the function body flow to l_x
		for(Label l: cfg.f_final.get(s.function.body)) {
			flow.add(new Edge(l, s.label2));
		}

		// any return statements flow to l_x
		for(ReturnStmt r: cfg.returns(s.function.body)) {
			flow.add(new Edge(r.label, s.label2));
		}
		 
		cfg.f_flow.put(s, flow);
	}
	public void visit(VarDecStmt s) {  
		cfg.f_flow.put(s, new CSet<Edge>());
	}
	public void visit(ReturnStmt s) {
		cfg.f_flow.put(s, new CSet<Edge>());
	}
	public void visit(BlockStmt s) { 
		
		List<Statement> stmts = s.statements;
		CSet<Edge> flow = new CSet<Edge>();

		for(Statement stmt : stmts) {
			stmt.accept(this);
			flow.addAll(cfg.f_flow.get(stmt));
		}
		
		// remove all function declarations from the block before going further
		stmts = filterFunDec(stmts);

		if (stmts.size() == 0) { 
			Logger.warn(NO_Statement, s);
		} else {
			for(int i=0; i<stmts.size()-1; i++) {
				Label s2 = cfg.f_init.get(stmts.get(i+1));
				for(Label s1 : cfg.f_final.get(stmts.get(i))) {
					flow.add(new Edge(s1, s2));
				} 
			}
		}
		cfg.f_flow.put(s, flow);
	}
	public void visit(ExpressionStmt s) { 
		
		if (isFunctionCall(s)) {
			Expression expr = ((FunctionCallExpr) ((AssignmentExpr) s.expr).rValue).target;
			if (expr instanceof VarAccessExpr) {
				String name = ((VarAccessExpr) expr).name;
				FunctionDec f = cfg.functions.get(name);
				
				if (f != null) {
					// (l_c, l_n)
					CSet<Edge> flow = new CSet<Edge>( new Edge(s.label, f.label, true) );
					// (l_x, l_r)
					flow.add(new Edge(f.label2, s.label2, true));

					cfg.f_flow.put(s, flow);
					cfg.interflow.add(new InterEdge(s.label, f.label, f.label2, s.label2));
				}
				else {
					System.out.println("called function: " + name + " is not declared");
				}
			} else {
				System.out.println("unsupported function call: " + s);
			}
		} else {
			cfg.f_flow.put(s, new CSet<Edge>());
		}
	}
	public void visit(EmptyStmt s) {
		cfg.f_flow.put(s, new CSet<Edge>());
	}
	public void visit(WhileStmt s) { 
		s.body.accept(this);
		
		CSet<Edge> flow = new CSet<Edge>();
		
		for(Label t : cfg.f_final.get(s.body)) {
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


