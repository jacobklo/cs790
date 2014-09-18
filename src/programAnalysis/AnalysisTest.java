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
	public void testFile() {
 
		String f = "cfg/while.js";
		
		Context context = Context.enter();
		Parser parser = getParser(context);
		 
		try {
			AstRoot ast = parseFromFile("analysis/" + f, parser);
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
