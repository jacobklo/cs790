package programAnalysis;

import java.util.HashMap;
import java.util.Map;


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
	 
	// overrride visitor methods here to compute the init/final/blocks functions
	// let me know if you need my implementation of this visitor
}

class CFGVisitor extends WhileLangVisitor {
	CFG cfg;
	
	CFGVisitor(CFG cfg) {
		this.cfg = cfg;
	}
	 
	// override visitor methods here to build your control flow graph
	// let me know if you need my implementation of this visitor
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
	
	// override the visit methods for the while/if/empty/block/expression statements 
	// to calculate the kill and gen sets for each label
	 
}

class AExpVisitor extends WhileLangVisitor {
	Map<Expression, CSet<Expression>> aexp = new HashMap<Expression, CSet<Expression>>();
	Map<Expression, CSet<String>> fv = new HashMap<Expression, CSet<String>>();
	
 
   // override the visit methods for the five statements and all expressions in WhileLangVisitor
   // to collect all arithmethic expressions in aexp
   // and collect all free variables of each expression in fv.
}
