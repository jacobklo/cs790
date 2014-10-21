package programAnalysis;


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

	private void print(Statement s) { System.out.println(s.node.toSource().split("\n")[0]); }
	
	public void visit(ScriptStmt s) { 
		// treat it like a block  
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
		return isAssignment(s) && ((AssignmentExpr) s.expr).rValue instanceof FunctionCallExpr;
	}
	boolean isAssignment(ExpressionStmt s) {
		return s.expr instanceof AssignmentExpr;
	}
}


class LabelVisitor extends WhileLangVisitor {
	final CFG cfg;
	
	LabelVisitor(CFG cfg) {
		this.cfg = cfg;
	}
	
	// function declaration
	public void visit(FunctionDec s) {
		cfg.functions.put(s.name, s);
		cfg.addLabel(s);
		cfg.addLabel2(s);
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label));
		s.function.body.accept(this);
		// wrong :
		cfg.f_blocks.put(s, cfg.f_blocks.get(s.function.body));
	}
	public void visit(VarDecStmt s) { 
		cfg.addLabel(s);
		if ( s.hasInitializer()){
			cfg.addLabel2(s);
		}
		cfg.varDecs.put(s, new CSet<VarDecStmt>(s));
		cfg.f_init.put(s, s.label);
		cfg.f_final.put(s, new CSet<Label>(s.label2));
		cfg.f_blocks.put(s, new CSet<Statement>(s));
	}
	public void visit(ReturnStmt s) { 
		 cfg.addLabel(s);
		 cfg.returns.put(s, new CSet<ReturnStmt>(s));
		 cfg.f_init.put(s, s.label);
		 cfg.f_final.put(s, new CSet<Label>(s.label));
		 // wrong : 
		 cfg.f_blocks.put(s, new CSet<Statement>(s));
	}
	
	// assume BlockStmt contains at least one statement
	public void visit(BlockStmt s) {
		
		for(Statement stmt : s.statements) {
            stmt.accept(this);
       }
       cfg.f_init.put(s, cfg.f_init.get(s.getStmt(0)));
       cfg.f_final.put(s, cfg.f_final.get(s.getLastStmt()));
       CSet<Statement> set = new CSet<Statement>();
       for(Statement stmt: s.statements) {
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
		if (isFunctionCall(s)){
			cfg.addLabel2(s);
			cfg.f_init.put(s, s.label2);
		}
        cfg.f_init.put(s, s.label);
        cfg.f_final.put(s, new CSet<Label>(s.label));
        cfg.f_blocks.put(s, new CSet<Statement>(s));
        
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
	}
	public void visit(WhileStmt s) {
		cfg.addLabel(s);
        s.body.accept(this);
        cfg.f_init.put(s, s.label);
        cfg.f_final.put(s, new CSet<Label>(s.label));
        cfg.f_blocks.put(s,
        cfg.f_blocks.get(s.body).union(s));
	}
}

class CFGVisitor extends WhileLangVisitor {
	CFG cfg;
	
	CFGVisitor(CFG cfg) {
		this.cfg = cfg;
	}
	
	 
	public void visit(FunctionDec s) {
		for (Statement st : cfg.returns.keySet()){
	      cfg.f_flow.put(s, new CSet<Edge>(new Edge(st.label, s.label2)).union(cfg.f_flow.get(s)));
	    }
	    for (Statement st : cfg.varDecs.keySet()){
	      cfg.f_flow.put(s, new CSet<Edge>(new Edge(s.label, st.label)).union(cfg.f_flow.get(s)));
	    }
	}
	public void visit(VarDecStmt s) {  
		cfg.f_flow.put(s, new CSet<Edge>());
		if (s.hasInitializer()){
			cfg.interflow.add(new InterEdge(s.label, cfg.functions.get(s.variable).label, cfg.functions.get(s.variable).label2,s.label2 ));
		}
	}
	public void visit(ReturnStmt s) {
		cfg.f_flow.put(s, new CSet<Edge>());
		for ( Statement st : cfg.functions.values()){
			cfg.f_flow.put(s, new CSet<Edge>(new Edge(s.label, st.label2)).union(cfg.f_flow.get(s)));
		}
	}
	public void visit(BlockStmt s) { 
		for(Statement stmt : s.statements) {
            stmt.accept(this);
       }
       CSet<Edge> flow = new CSet<Edge>();
       int k = s.getNumStmts();
       for(int i=0; i<k-1; i++) {
            Label s2 = cfg.f_init.get(s.getStmt(i+1));
            for(Label s1 : cfg.f_final.get(s.getStmt(i))) {
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
        flow.add(new Edge(s.label,
cfg.f_init.get(s.thenPart)));
        flow.addAll(cfg.f_flow.get(s.thenPart));
        if (s.hasElse()) {
             s.elsePart.accept(this);
             flow.add(new Edge(s.label,
cfg.f_init.get(s.elsePart)));
             flow.addAll(cfg.f_flow.get(s.elsePart));
}
        cfg.f_flow.put(s, flow);
	}
}

