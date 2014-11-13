package programAnalysis;

import java.util.List;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode; 


abstract class Statement {
	Label label;
	void setLabel(Label label) { this.label = label; }
	boolean hasLabel() { return label !=null; }
	
	// for function call (l_r) and function declaration (l_x)
	Label label2;
	void setLabel2(Label label2) { this.label2 = label2; }
	boolean hasLabel2() { return label2 != null; }
	
	AstNode node;
	void setAstNode(AstNode node) { this.node = node; }
	
	Set<String> labels;
	void setLabels(Set<String> labels) { this.labels = labels; }
	boolean hasLabels() { return labels != null; }
	boolean hasLabel(String label) { return hasLabels() && labels.contains(label); }
    
	public String toString() { return getClass().getName() + ":" + node.getLineno(); }
	
	abstract public void accept(Visitor v);
}
 
class EmptyStmt extends Statement {
	public void accept(Visitor v) { v.visit(this); }
}

// only handle one unconditional catch clause that catches everything in the try block
// catch conditions and subsequent catch clauses, if any, are ignored as they are non-standard
class TryStmt extends Statement {
	Statement tryBlock;
	List<CatchStmt> catchClauses;
	Statement finallyBlock;
	
	TryStmt(Statement tryBlock, List<CatchStmt> catchClauses, Statement finallyBlock) {
		this.tryBlock = tryBlock; this.catchClauses = catchClauses; this.finallyBlock = finallyBlock;
	}
	boolean hasCatch() { return !catchClauses.isEmpty(); }
	CatchStmt catchClause() { return catchClauses.get(0); }
	boolean hasFinally() { return finallyBlock != null; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class CatchStmt extends Statement {
	final String name;
	final Statement body;
	CatchStmt(String name, Statement body) { this.name = name; this.body = body; }
	 
	public void accept(Visitor v) { v.visit(this); }
}
class ThrowStmt extends Statement {
	final Expression expr;
	ThrowStmt(Expression expr) { this.expr = expr; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class FunctionDec extends Statement {
	final String name;
	final FunctionExpr function;
	boolean usedByInnerScopes = false;
	
	FunctionDec(String name, FunctionExpr function) {
		this.name = name;
		this.function = function;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}

class BreakStmt extends Statement {
	final String label;
	
	// label @Nullable
	BreakStmt(String id) {
		this.label = id;
	}
	 
	boolean hasLabel() { return label != null; }
	
 
	static boolean canCatchBreak(BreakStmt br, Statement loopOrSwitch) {
		return !br.hasLabel() || loopOrSwitch.hasLabel(br.label);
	}
	
	public void accept(Visitor v) { v.visit(this); }
}

class ContinueStmt extends Statement {
	final String label;
	
	// label @Nullable
	ContinueStmt(String id) {
		this.label = id;
	}
	 
	boolean hasLabel() { return label != null; }
	 
	
	static boolean canCatchContinue(ContinueStmt c, Statement loop) {
		return !c.hasLabel() || loop.hasLabel(c.label);
	}
	
	public void accept(Visitor v) { v.visit(this); }
}

//class EntryExitStmt extends Statement {
//	boolean isEntry;
//	
//	EntryExitStmt(boolean isEntry) { this.isEntry = isEntry; }
//	public String toString() { return isEntry? "entry" : "exit"; }
//}

class ReturnStmt extends Statement {
	final Expression expr;
	
	// expression may be null
	ReturnStmt(Expression expression) {
		this.expr = expression;
	}
	boolean hasReturnExpr() { return expr != null; }
	  
	public void accept(Visitor v) { v.visit(this); }
}

class BlockStmt extends Statement {
	final List<Statement> statements;
	
	BlockStmt (List<Statement> stmts) {
		this.statements = stmts;
		
		// insert an empty statement if the block is empty -- 
		// to avoid covering the corner case for the analysis algorithms
		if (stmts.size() == 0)	stmts.add(new EmptyStmt());
	}
	
	int getNumStmts() { return statements.size(); }
	Statement getStmt(int i) { return statements.get(i); }
	Statement getLastStmt() { return statements.get(getNumStmts()-1); }
	
	public void accept(Visitor v) { v.visit(this); }
}

class ScriptStmt extends BlockStmt  { 
//    final Statement entry = new EntryExitStmt(true), exit = new EntryExitStmt(false);
   
   
	ScriptStmt (List<Statement> stmts) {
		super(stmts);
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}
class SwitchStmt extends Statement {
	final Expression condition;
	final List<CaseStmt> cases;
	final CaseStmt defaultCase;
	
	// defaultCase @Nullable
	SwitchStmt (Expression condition, List<CaseStmt> cases, CaseStmt defaultCase) {
		this.condition = condition;
		this.cases = cases;
		this.defaultCase = defaultCase;
	}
	int getNumStmts() { return cases.size(); }
	Statement getStmt(int i) { return cases.get(i); }
	Statement getLastStmt() { return cases.get(getNumStmts()-1); }
 
	boolean hasDefault() { return defaultCase != null; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class CaseStmt extends Statement {
	final Expression expr;  
	final List<Statement> body;
	// exp @Nullable
	CaseStmt(Expression expr, List<Statement> body) { this.expr = expr; this.body = body; }
	boolean hasExpression () { return expr != null; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class IfStmt extends Statement {
	final Expression condition;
	final Statement thenPart, elsePart;
	
 	
	// elsePart @Nullable
	IfStmt(Expression condition, Statement thenPart, Statement elsePart) {
		this.condition = condition; 
		this.thenPart = thenPart; 
		this.elsePart = elsePart;
	}
	 
	boolean hasElse() {
		return elsePart != null;
	}
	
	public void accept(Visitor v) { v.visit(this); }
}

abstract class LoopStmt extends Statement {
	final Statement body;
	
	LoopStmt(Statement body) {
		this.body = body;
	}
	
	public void accept(Visitor v) { v.visit(this); } 
}

class WhileStmt extends LoopStmt {
	final Expression condition;
 
	WhileStmt (Expression condition, Statement body) {
		super(body);
		this.condition = condition;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}

class ForStmt extends LoopStmt {
	final Statement init;
	final Expression condition, increment;
	 
	ForStmt(Statement init, Expression condition, Expression increment, Statement body) {
		super(body);
		this.init = init;
		this.condition = condition;
		this.increment = increment;
	}
	 
	boolean hasInit() { return init != null; }
	boolean hasCondition() { return condition != null; }
	boolean hasIncrement() { return increment != null; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class ForInStmt extends LoopStmt {
	final boolean isVarDec;
	final String variable;
	final Expression iteratedObj;

	ForInStmt (boolean isVarDec, String variable, Expression iteratedObj, Statement body) {
		super(body);
		this.isVarDec = isVarDec;
		this.variable = variable;
		this.iteratedObj = iteratedObj;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}

class ExpressionStmt extends Statement {
	final Expression expr;
	ExpressionStmt(Expression expr) {
		this.expr = expr;
	}
	 
	public void accept(Visitor v) { v.visit(this); }
}

class VarDecStmt extends Statement {
	/* Begin of variable addition */
	
	Variable var;
	void setVariable(Variable var) { this.var = var; }
	Variable getVariable() { 
		if (var == null) Logger.error("Variable " + variable + " has not been prossessed");
		return var; 
	}
	
	/* End of variable addition */
	
	final String variable;
	final Expression rValue;
	boolean usedByInnerScopes = false;
	
	// rValue may be null
	VarDecStmt(String variable, Expression rValue) {
		this.variable = variable;
		this.rValue = rValue;
	}
	 
	boolean hasInitializer() { return rValue != null; }
	
	public void accept(Visitor v) { v.visit(this); }
}

class VarDecListStmt extends BlockStmt {
	VarDecListStmt (List<Statement> varDecList) { super(varDecList); }
	
	public void accept(Visitor v) { v.visit(this); }
}

