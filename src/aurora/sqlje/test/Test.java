package aurora.sqlje.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import aurora.sqlje.ast.AstTransform;
import aurora.sqlje.parser.ParsedSource;
import aurora.sqlje.parser.SqljeParser;

public class Test {
	public static void main(String[] args) throws Exception {
//		File fi = new File(
//		"/Users/jessen/work/Workspaces/WEB/SqljDemo/src/inv/inv_periodic_average_cost_pkg.sqlje");
		
		File fi = new File(
				"/Users/jessen/work/Workspaces/WEB/aurora-sqlje/src/aurora/sqlje/test/SQLJE_test.sqlje");
		File fo = new File(fi.getAbsolutePath().replace(".sqlje", ".java"));
		FileInputStream fis = new FileInputStream(fi);
		byte[] b = new byte[(int) fi.length()];
		fis.read(b);
		fis.close();
		String source = new String(b, "UTF-8");
		SqljeParser parser = new SqljeParser(source);
		FileWriter fw = new FileWriter(fo);
		ParsedSource ps = parser.parse();
		AstTransform trans = new AstTransform(ps);
		String str = trans.tranform();
		fw.write(str);
		fw.close();

	}
}
