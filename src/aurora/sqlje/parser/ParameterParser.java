package aurora.sqlje.parser;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Expression;

import aurora.sqlje.ast.AstTransform;
import aurora.sqlje.exception.ParserException;

public class ParameterParser extends Parser {
	public static final String FLAG = "${";

	public ParameterParser(String source) {
		super(source);
	}

	public void setSource(String source) {
		this.source = source;
		this.len = source.length();
	}

	public ParsedSql parse() throws ParserException {
		ParsedSql ps = new ParsedSql();
		StringBuilder sb = new StringBuilder(len);
		int index = -1;
		int lastFlagIndex = -1;
		CharStack cs = new CharStack();
		while ((index = source.indexOf(FLAG, lastFlagIndex)) != -1) {
			sb.append(source.substring(lastFlagIndex + 1, index));
			int i = index + FLAG.length();
			char c = source.charAt(i);
			if (c == '!' || c == '@') {
				if (c == '!') {
					ps.addFragment(sb.toString());
					sb.delete(0, sb.length());
				} else
					sb.append('?');
				cs.clear();
				cs.push('{');
				int i_ = findMatch(cs, source, i + 1);
				ps.addPara(createParameter(i + 1, i_, c == '@' ? Parameter.OUT
						: Parameter.NONE));
				lastFlagIndex = i_;
			} else {
				sb.append('?');
				cs.clear();
				cs.push('{');
				int i_ = findMatch(cs, source, i);
				ps.addPara(createParameter(i, i_, Parameter.IN));
				lastFlagIndex = i_;
			}
		}
		sb.append(source.substring(lastFlagIndex + 1));
		ps.addFragment(sb.toString());
		return ps;
	}

	private Parameter createParameter(int start, int end, int ptype)
			throws ParserException {
		String exp = source.substring(start, end);
		checkExpression(exp, start, end);
		Parameter p = new Parameter(ptype, exp);
		return p;
	}

	private void checkExpression(String expression, int start, int end)
			throws ParserException {
		try {
			ASTParser parser = ASTParser.newParser(AstTransform.API_LEVEL);
			parser.setKind(ASTParser.K_EXPRESSION);
			parser.setSource(expression.toCharArray());
			Map options = JavaCore.getOptions();
			JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
			parser.setCompilerOptions(options);
			Expression exp = (Expression) parser.createAST(null);
		} catch (Exception e) {
			//this exception will be handled in SqljParser, then it will be translated
			throw new ParserException(source, start, end, expression
					+ " is not a valid expression.");
		}
	}
}
