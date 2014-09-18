package programAnalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Label;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;


public class StatementVisitor {
	public StatementVisitor() {
	}
	// what about CONST and LET
	private Statement visit (VariableInitializer init) {
		Statement ret = null;
		
		if(init.isDestructuring()) {
			// do something
		} else {
			Name target = (Name) init.getTarget();
			AstNode initializer = init.getInitializer();
			Expression rValue = null;
			if (initializer != null) {
				rValue = visitExpression(initializer);
			}
			ret = new VarDecStmt(target.getIdentifier(), rValue);
		}
		
		return ret;
	}
	private Statement visit (VariableDeclaration varDec) {
		Statement ret = null;
		
		List<VariableInitializer> variables = varDec.getVariables();
		if(variables.size() == 1) {
			ret = visit(variables.get(0));
		} else {
			List<Statement> statements = new ArrayList<Statement>();
			for(AstNode var : varDec.getVariables()) {
				statements.add(visit(var));
			}
			ret = new VarDecListStmt(statements);
		}
		
		return ret;
	}
	// function declaration must have a name
	private Statement visit (FunctionNode func) {
		return new FunctionDec(func.getFunctionName().getIdentifier(), (FunctionExpr) visitExpression((AstNode) func));
	}
	private IfStmt visit(IfStatement ifStmt) {
		Statement elsePart = null;
		AstNode elseNode = ifStmt.getElsePart();
		if (elseNode != null)
			elsePart = visit(elseNode);
		
		return new IfStmt(visitExpression(ifStmt.getCondition()),
				visit(ifStmt.getThenPart()), elsePart);
	}
	private SwitchStmt visit(SwitchStatement switchStmt) {
		Expression condition = visitExpression (switchStmt.getExpression());
		List<CaseStmt> cases = new ArrayList<CaseStmt>();
		
		CaseStmt defaultCase = null;
		
		for(SwitchCase c : switchStmt.getCases()) {
			Expression caseExpr = null;
			AstNode caseExprNode = c.getExpression();
			if (caseExprNode != null) caseExpr = visitExpression(caseExprNode);
			List<Statement> statements = new ArrayList<Statement>();
			
			for(AstNode n : c.getStatements()) {
				statements.add(visit(n));
			}
			if (!c.isDefault()) 
				cases.add(new CaseStmt(caseExpr, statements));
			else if (defaultCase == null)
				defaultCase = new CaseStmt(null, statements);
			else 
				throw new RuntimeException("more than one default cases is defined in:\n " 
						+ switchStmt.toSource()
						+ "\n at line " + switchStmt.getLineno());
		}
		
		return new SwitchStmt(condition, cases, defaultCase);
	}
	private BreakStmt visit(BreakStatement breakStmt) {
		String label  = null;
		if (breakStmt.getBreakLabel() != null)
			label = breakStmt.getBreakLabel().getIdentifier();
		return new BreakStmt(label);
	}
	private ContinueStmt visit(ContinueStatement continueStmt) {
		String label = null;
		if (continueStmt.getLabel() != null) 
			label = continueStmt.getLabel().getIdentifier();
		return new ContinueStmt(label);
	}
	private Statement visit(LabeledStatement ls) {
		Statement s = visit(ls.getStatement());
		Set<String> labels = new HashSet<String>();
		for(Label l : ls.getLabels())
			labels.add(l.getName());
		s.setLabels(labels);
		return s;
	}
	private ReturnStmt visit(ReturnStatement retStmt) {
		Expression retValue = null;
		if (retStmt.getReturnValue() != null) {
			retValue = visitExpression(retStmt.getReturnValue());
		}
		return new ReturnStmt(retValue);
	}
	private WhileStmt visit(WhileLoop whileLoop) {
		return new WhileStmt(visitExpression(whileLoop.getCondition()), visit(whileLoop.getBody()));
	}
	private WhileStmt visit(DoLoop doLoop) {
		return new WhileStmt(visitExpression(doLoop.getCondition()), visit(doLoop.getBody()));
	}
	private ForStmt visit(ForLoop forLoop) {
		AstNode init = forLoop.getInitializer();
		
		Statement initializer;
		if (init instanceof VariableDeclaration) {
			initializer = visit(init);
		}
		else {
			initializer = new ExpressionStmt (visitExpression(init));
		}
		return new ForStmt(initializer, visitExpression(forLoop.getCondition()),
				visitExpression(forLoop.getIncrement()), visit(forLoop.getBody()));
	}
	// what about Array Comprehension
	private ForInStmt visit (ForInLoop forInLoop) {
		AstNode iterator = forInLoop.getIterator();
		String variable;
		boolean isVarDec = false;
		if (iterator instanceof Name) {
			variable = iterator.getString();
			isVarDec = true;
		} else {
			variable = ((VariableDeclaration) iterator).getVariables().get(0).getTarget().getString();
		}
		return new ForInStmt(isVarDec, variable, visitExpression(forInLoop.getIteratedObject()), 
				visit(forInLoop.getBody()));
	}
	private BlockStmt visit(Block block) {
		List<Statement> statements = new ArrayList<Statement>();
		for(Node s : block) {
			statements.add(visit((AstNode)s ));
		}
		return new BlockStmt(statements);
	}
	// Scope appears to be only used for loop and branch blocks but do not imply new scope
	// so I just use block statement for them.
	private BlockStmt visit(Scope scope) {
		List<Statement> statements = new ArrayList<Statement>();
		for(Node s : scope) {
			statements.add(visit((AstNode) s ));
		}
		return new BlockStmt(statements);
	}
	private ScriptStmt visit(AstRoot block) {
		List<Statement> statements = new ArrayList<Statement>();
		for(Node s : block) {
			statements.add(visit((AstNode)s ));
		}
		return new ScriptStmt(statements);
	}
	private TryStmt visit(TryStatement t) {
		Statement body = visit(t.getTryBlock());
		Statement finallyBlock = null;
		AstNode finallyNode = t.getFinallyBlock();
		if (finallyNode != null) finallyBlock = visit(finallyNode);
		List<CatchStmt> catches = new ArrayList<CatchStmt>();
		for(CatchClause c : t.getCatchClauses()) {
			catches.add((CatchStmt) visit(c));
		}
		return new TryStmt(body, catches, finallyBlock); 
	}

	public Statement visit(AstNode node) {
		Statement ret;
		
		switch(node.getType()) {
		case Token.SCRIPT:
			if (node instanceof AstRoot) {
				ret = visit((AstRoot) node);
				break;
			}
		case Token.EMPTY:
			ret = new EmptyStmt();
			break;
		case Token.VAR: // what about Let and Const nodes ?
			if (node instanceof VariableInitializer) {
				ret = visit( (VariableInitializer) node );
			} else {
				ret = visit( (VariableDeclaration) node );
			}
			break;
		case Token.FUNCTION:
			ret = visit((FunctionNode) node);
			break;
		case Token.RETURN:
			ret = visit((ReturnStatement) node); 
			break;
		case Token.IF:
			ret = visit((IfStatement) node); 
			break;
		case Token.SWITCH:
			ret = visit((SwitchStatement) node);	
			break;
		case Token.BREAK:
			ret = visit((BreakStatement) node);
			break;
		case Token.CONTINUE:
			ret = visit((ContinueStatement) node);
			break;
		case Token.DO:
			ret = visit((DoLoop) node);
			break;
		case Token.WHILE:
			ret = visit((WhileLoop) node);
			break;
		case Token.FOR:
			if (node instanceof ForLoop) {
				ret = visit((ForLoop) node);
			} 
			else {
				ret = visit((ForInLoop) node);
			}
			break;	
		case Token.BLOCK: 
			if (node instanceof Block)
				ret = visit((Block) node);
			else  
				// This is some oddity in Rhino. It appears that loops and branches use Scope object 
				// to hold statement blocks while there is no loop or branch scope in JavaScript
				// So I pretend this is just plain-old block. 
				ret = visit((Scope) node);
			break;
		case Token.EXPR_VOID:
			if (node instanceof LabeledStatement) {
				ret = visit( (LabeledStatement) node);
				break;
			}
		case Token.ASSIGN:
		case Token.EXPR_RESULT:
			ret = new ExpressionStmt(visitExpression(node));
			break;
		case Token.TRY:
			ret = visit((TryStatement) node);
			break;
		case Token.THROW:
			ret = new ThrowStmt(visitExpression(((ThrowStatement) node).getExpression()));
			break;
		case Token.CATCH:
			CatchClause c = (CatchClause) node;
			ret = new CatchStmt(c.getVarName().getIdentifier(), visit(c.getBody()));
			break;
		case Token.FINALLY: // why there is a finally token type? 
		default:
			throw new RuntimeException("Unhandled statement: " + node.toSource());
 	 
		}
		ret.setAstNode(node);
		
		return ret;
 	}
	
	
	private FunctionExpr visitExpression (FunctionNode func) {
		FunctionExpr ret = null;
		
		Name functionName = func.getFunctionName();
		List<AstNode> params = func.getParams();
		AstNode body = func.getBody();
		
		List<String> parameters = new ArrayList<String>();
		for(AstNode p : params) {
			// assume p is a Name node that has an identifier string
			parameters.add(p.getString());
		}
		
		Statement bodyBlock = visit(body);
		
		if (func.isExpressionClosure()) {
			// do something to get a return statement and replace bodyBlock with it
			bodyBlock = new ReturnStmt(visitExpression (func.getMemberExprNode()));
		}
		
		String name = null;
		
		if (functionName != null) 
			name = functionName.getIdentifier();
		
		ret = new FunctionExpr(name, parameters, bodyBlock);;
		 
		return ret;
	}
	private FunctionCallExpr visitExpression (FunctionCall call) {
		List<Expression> arguments = new ArrayList<Expression>();
		for(AstNode a : call.getArguments()) {
			arguments.add(visitExpression(a));
		}
	
		return new FunctionCallExpr(visitExpression(call.getTarget()), arguments);
	}
	private NewExpr visitExpression (NewExpression newExpr) {
		List<Expression> arguments = new ArrayList<Expression>();
		for(AstNode a : newExpr.getArguments()) {
			arguments.add(visitExpression(a));
		}
	
		return new NewExpr(visitExpression(newExpr.getTarget()), arguments);
	}
	private ObjectExpr visitExpression (ObjectLiteral objExpr) {
		List<ObjProperty> properties = new ArrayList<ObjProperty>();
		for(ObjectProperty p : objExpr.getElements()) {
			properties.add(new ObjProperty(visitExpression(p.getLeft()), visitExpression(p.getRight()), p.isGetter(), p.isSetter()));
		}
		return new ObjectExpr(properties);
	}
	private ArrayExpr visitExpression (ArrayLiteral array) {
		List<Expression> elements = new ArrayList<Expression>();
		for(AstNode e : array.getElements()) {
			elements.add(visitExpression(e));
		}
		return new ArrayExpr(elements);
	}
	
	public Expression visitExpression (AstNode node) {
		Expression ret;
		
		switch(node.getType()) {
		case Token.EXPR_VOID:
		case Token.EXPR_RESULT:
			ret = visitExpression(((ExpressionStatement) node).getExpression());
			break;
		case Token.EMPTY:
			ret = new EmptyExpr();
			break;
		case Token.LP:
			ret = new ParenthesizedExpr(visitExpression(((ParenthesizedExpression) node).getExpression()));
			break;
		case Token.FUNCTION:
			ret = visitExpression((FunctionNode) node);
			break;
		case Token.CALL:
			ret = visitExpression((FunctionCall) node);
			break;
		case Token.NEW:
			ret = visitExpression((NewExpression) node);
			break;
		case Token.ASSIGN:
			ret = new AssignmentExpr(visitExpression(((Assignment) node).getLeft()), visitExpression(((Assignment) node).getRight()));
			break;
		case Token.NAME:
			ret = new VarAccessExpr( ((Name) node).getIdentifier() );
			break;
		case Token.THIS:
			ret = new VarAccessExpr( "this" );
			break;
		case Token.GETPROP:
			ret = new GetPropExpr(visitExpression(((PropertyGet) node).getTarget()), ((PropertyGet) node).getProperty().getIdentifier());
			break;
			// "left[property]"
		case Token.GETELEM:
			ret = new GetElemExpr(visitExpression(((ElementGet) node).getTarget()), visitExpression(((ElementGet) node).getElement()));
			break;
		case Token.DELPROP:
			ret = new DeleteExpr(visitExpression (((UnaryExpression) node).getOperand()));
			break;
		case Token.NULL:
			ret = new NullExpr();
			break;
		case Token.TRUE:
			ret = new BoolExpr(true);
			break;
		case Token.FALSE:
			ret = new BoolExpr(false);
			break;
		case Token.NUMBER :
			ret = new NumberExpr(((NumberLiteral) node).getValue(), ((NumberLiteral) node).getNumber());
			break;
		case Token.STRING:
			ret = new StringExpr(((StringLiteral) node).getValue());
			break;
		case Token.REGEXP:
			ret = new RegExpExpr(((RegExpLiteral) node).getValue(), ((RegExpLiteral) node).getFlags());
			break;
		case Token.OBJECTLIT:
			ret = visitExpression((ObjectLiteral) node);
			break;
		case Token.ARRAYLIT:
			ret = visitExpression((ArrayLiteral) node);
			break;			
		case Token.HOOK:
			ConditionalExpression hook = (ConditionalExpression) node; 
			ret = new ConditionalExpr(visitExpression(hook.getTestExpression()), 
					visitExpression(hook.getTrueExpression()), visitExpression(hook.getFalseExpression()));
			break;
		case Token.TYPEOF:
			ret = new TypeOfExpr(visitExpression (((UnaryExpression) node).getOperand()));
			break;
		// left instanceof right
		case Token.INSTANCEOF:
			InfixExpression instanceOfNode = (InfixExpression) node;
			ret = new InstanceOfExpr(visitExpression(instanceOfNode.getLeft()), visitExpression(instanceOfNode.getRight()));
			break;
		case Token.INC:
		case Token.DEC:
			UnaryExpression unaryExp = (UnaryExpression) node;
			Expression target = visitExpression (unaryExp.getOperand());
			ret = new AssignmentExpr(target, new AddExpr(target, new NumberExpr("1", 1)));
			break;
		case Token.ASSIGN_DIV:
		case Token.ASSIGN_SUB:
		case Token.ASSIGN_MOD:
		case Token.ASSIGN_MUL:
		case Token.ASSIGN_BITOR :
		case Token.ASSIGN_BITXOR :
		case Token.ASSIGN_BITAND :
		case Token.ASSIGN_LSH :
		case Token.ASSIGN_RSH :
		case Token.ASSIGN_URSH :
			Assignment assignment = (Assignment) node;
			Expression lValue = visitExpression(assignment.getLeft());
			ret = new AssignmentExpr(lValue, new NumericExpr(lValue, visitExpression(assignment.getRight()), assignment.getOperator()));
			break;			
		case Token.ASSIGN_ADD:
			assignment = (Assignment) node;
			lValue = visitExpression(assignment.getLeft());
			ret = new AssignmentExpr(lValue, new AddExpr(lValue, visitExpression(assignment.getRight())));
			break;			
		case Token.NEG:
			ret = new NegationExpr(visitExpression (((UnaryExpression) node).getOperand()));
			break;
		// Numeric operators
		case Token.BITOR :
		case Token.BITXOR :
		case Token.BITAND :
		case Token.LSH :
		case Token.RSH :
		case Token.URSH :
		case Token.SUB:
		case Token.MUL:
		case Token.DIV:
		case Token.MOD:
			InfixExpression numericNode = (InfixExpression) node;
			ret = new NumericExpr(visitExpression(numericNode.getLeft()), visitExpression(numericNode.getRight()), numericNode.getOperator());
			break;
		// add is encoded separately since it is overloaded
		case Token.ADD:
			InfixExpression addNode = (InfixExpression) node;
			ret = new AddExpr(visitExpression(addNode.getLeft()), visitExpression(addNode.getRight()));
			break;
		case Token.BITNOT :
			ret = new BitNotExpr(visitExpression (((UnaryExpression) node).getOperand()));
			break;
		// numeric comparison operators
		case Token.GT:
		case Token.LT:
		case Token.GE:
		case Token.LE:
			InfixExpression comparisonNode = (InfixExpression) node;
			ret = new ComparisonExpr(visitExpression(comparisonNode.getLeft()), visitExpression(comparisonNode.getRight()), comparisonNode.getOperator());
			break;
		// comparison operators
		case Token.NE:
		case Token.EQ:
			InfixExpression equalityNode = (InfixExpression) node;
			ret = new EqualityExpr(visitExpression(equalityNode.getLeft()), visitExpression(equalityNode.getRight()), equalityNode.getOperator()==Token.EQ);
			break;
		// shallow comparison operators
		case Token.SHEQ:
		case Token.SHNE:
			InfixExpression sEqualityNode = (InfixExpression) node;
			ret = new EqualityExpr(visitExpression(sEqualityNode.getLeft()), visitExpression(sEqualityNode.getRight()), sEqualityNode.getOperator()==Token.SHEQ);
			break;			
		// logical operators
		case Token.AND:
		case Token.OR:
			InfixExpression logicNode = (InfixExpression) node;
			ret = new LogicExpr(visitExpression(logicNode.getLeft()), visitExpression(logicNode.getRight()), logicNode.getOperator() == Token.AND);
			break;
		case Token.NOT:
			ret = new LogicNotExpr(visitExpression( ((UnaryExpression) node).getOperand()));
			break;

		default: 
			throw new RuntimeException("Unhandled expression: " + node.getType() + " : " + node.toSource());
		}				
		
		ret.setAstNode(node);
		
		return ret;
	}
	
}



