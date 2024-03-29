package programAnalysis;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;


public class AnalysisTest {

	@Test
	public void testAexp() {
 
		String f = "cfg/while2.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile("" + f, parser);
			Statement s = new StatementVisitor().visit(ast);
			
 
			CFG cfg = new CFG();
			s.accept(new LabelVisitor(cfg));
			cfg.setLabels();
			
			s.accept(new CFGVisitor(cfg));
			cfg.setReverseFlow();
			
			AExpVisitor aexpv = new AExpVisitor();
			s.accept(aexpv);
			AExp_F_Visitor aexpfv = new AExp_F_Visitor(aexpv.aexp, aexpv.fv);
			s.accept(aexpfv);
			System.out.println("==== kill ====");
			for(Label l : aexpfv.kill.keySet()) {
				System.out.println(l + " : " + aexpfv.kill.get(l));
			}
			System.out.println("==== gen ====");
			for(Label l : aexpfv.gen.keySet()) {
				System.out.println(l + " : " + aexpfv.gen.get(l));
			}
			
		} catch(IOException e) {
			System.out.println(e);
		}
		finally {
			Context.exit(); // Exit from the context.
		}
	}

	
	@Test
	public void testAexpAnalysis() {
 
		String f = "cfg/while2.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile("" + f, parser);
			Statement s = new StatementVisitor().visit(ast);
			
			AvailableExpression ae = new AvailableExpression(s);
			ae.chaoticIteration();
			
			System.out.println("==== entry ====");
			for(Label l : ae.analysis_0.keySet()) {
				System.out.println(l + " : " + ae.analysis_0.get(l));
			}
			System.out.println("==== exit ====");
			for(Label l : ae.analysis_1.keySet()) {
				System.out.println(l + " : " + ae.analysis_1.get(l));
			}
			
		} catch(IOException e) {
			System.out.println(e);
		}
		finally {
			Context.exit(); // Exit from the context.
		}
	}
	@Test
	public void testFile() {
 
		String f = "cfg/while.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile("" + f, parser);
			Statement s = new StatementVisitor().visit(ast);
			
			CFG cfg = new CFG();
			s.accept(new LabelVisitor(cfg));
			cfg.setLabels();
			
			s.accept(new CFGVisitor(cfg));
			cfg.setReverseFlow();
			
			System.out.println(cfg.print());
			
			String ret = "\n=============\n\n";
			ret += cfg.f_flow.get(s);
			System.out.println(ret);
			
		} catch(IOException e) {
			System.out.println(e);
		}
		finally {
			Context.exit(); // Exit from the context.
		}
	}
	
	public static AstRoot parseFromFile(String fileName, Parser parser) throws IOException { 
		File file = new File(fileName);
		Reader reader = new FileReader(file); 

		String sourceURI; 
		try { 
			sourceURI = file.getCanonicalPath(); 
		} catch (IOException e) { 
			sourceURI = file.toString(); 
		} 
		AstRoot scriptOrFnNode = parser.parse(reader, sourceURI, 1); 
		return scriptOrFnNode; 
	} 

	public static Parser getParser(Context context) {
		CompilerEnvirons compilerEnv = new CompilerEnvirons();
		compilerEnv.initFromContext(context);
		compilerEnv.setRecordingComments(true);
		ErrorReporter compilationErrorReporter = compilerEnv.getErrorReporter();
		Parser p = new Parser(compilerEnv, compilationErrorReporter);    
		return p;
	}
}
