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
//	public void visit(Statement s) { }
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
//	public void visit(Expression e) { }
	public void visit(FunctionExpr e) { }
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
	
	void log(String level, String message, Statement s) {
		log(level, message, s.node.toSource());
	}
	void log(String level, String message, Expression e) {
		log(level, message, e.node.toSource());
	}
	void log(String level, String message, String source) {
		System.out.println(level + message + "at: \n" + source);
		if (level == ERROR) System.exit(1);
	}
	public static final String WARN = "Warning: ", ERROR = "Error: ";
}


