package programAnalysis;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;


public class AnalysisTest {
	public static Parser getParser(Context context) {
		CompilerEnvirons compilerEnv = new CompilerEnvirons();
		compilerEnv.initFromContext(context);
		compilerEnv.setRecordingComments(true);
		ErrorReporter compilationErrorReporter = compilerEnv.getErrorReporter();
		Parser p = new Parser(compilerEnv, compilationErrorReporter);    
		return p;
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
	
	public static String projPath = "analysis/";
	/*
	@Test
	public void testCFG() {
 
		String f = "cfg/polymorphic.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile(projPath + f, parser);
			Statement s = new StatementVisitor().visit(ast);
			
//			s.accept(new WhileLangVisitor());
			
			CFG cfg = new CFG();
			s.accept(new LabelVisitor(cfg));
			cfg.setLabels();
			
			s.accept(new CFGVisitor(cfg));
//			cfg.setReverseFlow();
			
			System.out.println(cfg.print());
			
			String ret = "\n=============\n\n";
			ret += cfg.f_flow.get(s);
			
			ret += "\n=============\n\n";
			
			ret += cfg.interflow;
			
			System.out.println(ret);
			
		} catch(IOException e) {
			System.out.println(e);
		}
		finally {
			Context.exit(); // Exit from the context.
		}
	}
	
	@Test
	public void testAexp() {
 
		String f = "cfg/while2.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile(projPath + f, parser);
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
			AstRoot ast = parseFromFile(projPath + f, parser);
			Statement s = new StatementVisitor().visit(ast);
			
			AvailableExpression ae = new AvailableExpression(s);
			ae.worklistAlgorithm();
			
			System.out.println("==== entry ====");
			for(Label l : ae.mfp_entry.keySet()) {
				System.out.println(l + " : " + ae.mfp_entry.get(l));
			}
			System.out.println("==== exit ====");
			for(Label l : ae.mfp_exit.keySet()) {
				System.out.println(l + " : " + ae.mfp_exit.get(l));
			}
			
		} catch(IOException e) {
			System.out.println(e);
		}
		finally {
			Context.exit(); // Exit from the context.
		}
	}
	
	@Test
	public void testAexpAnalysis2() {
 
		String f = "cfg/while2.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile(projPath + f, parser);
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
	public void testConstantPropagation() {
 
		String f = "cfg/polymorphic.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile(projPath + f, parser);
			Statement s = new StatementVisitor().visit(ast);
			
			ConstantPropagation cp = new ConstantPropagation(s);
			cp.worklistAlgorithm();
			
			List<Label> labels = new ArrayList<Label>(cp.mfp_entry.keySet());
			Collections.sort(labels);
			
			System.out.println("==== entry ====");
			for(Label l : labels) {
				System.out.println(l + " : " + cp.mfp_entry.get(l));
			}
			System.out.println("==== exit ====");
			for(Label l : labels) {
				System.out.println(l + " : " + cp.mfp_exit.get(l));
			}
			
		} catch(IOException e) {
			System.out.println(e);
		}
		finally {
			Context.exit(); // Exit from the context.
		}
	}
	*/
	@Test
	public void testCFA() {
		String f = "cfg/twice.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile(projPath + f, parser);
			Statement s = new StatementVisitor().visit(ast);
			
			K_CFA cfa = new K_CFA(s);
			cfa.worklist();
			
			System.out.println(cfa.print());
			
		} catch(IOException e) {
			System.out.println(e);
		}
		finally {
			Context.exit(); // Exit from the context.
		}
	}
}
