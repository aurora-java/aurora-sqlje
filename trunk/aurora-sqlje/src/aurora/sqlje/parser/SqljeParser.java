package aurora.sqlje.parser;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;


import aurora.sqlje.exception.ParserException;

public class SqljeParser extends Parser {
	public static final String FLAG = "#{";
	public static Class<?>[] supported_types = { int.class, long.class,
			double.class, Integer.class, Long.class, Double.class,
			BigDecimal.class, String.class, Date.class, Timestamp.class };
	public static String[] supported_type_names = new String[supported_types.length];
	static {
		for (int i = 0; i < supported_types.length; i++)
			supported_type_names[i] = supported_types[i].getSimpleName();
	}
	private ParsedSource parsedSource = ParsedSource.newSession();
	StringBuilder buffer;

	public SqljeParser(String source) {
		super(source);
		buffer = new StringBuilder(len);
		parsedSource.setBuffer(buffer);
	}

	public ParsedSource parse() throws Exception {
		scan();
		//System.out.println(buffer);
		return parsedSource;
	}

	void scan() throws ParserException {
		boolean instr = false;
		boolean inchar = false;
		boolean inlc = false;//line comment
		boolean inbc = false;//block comment
		int lastFlagIndex = 0;
		CharStack cs = new CharStack();
		for (int i = 0; i < len; i++) {
			char c = source.charAt(i);
			if (instr) {
				if (c == '\\') {
					i++;
				} else if (c == '"') {
					instr = false;
				}
				continue;
			} else if (inchar) {
				if (c == '\\') {
					i++;
				} else if (c == '\'') {
					inchar = false;
				}
				continue;
			} else if (inlc) {
				if (c == '\n' || c == '\r') {
					inlc = false;
				}
				continue;
			} else if (inbc) {
				if (c == '*' && i + 1 < len && source.charAt(i + 1) == '/') {
					inbc = false;
					i++;
				}
				continue;
			}

			if (c == '"')
				instr = true;
			else if (c == '\'')
				inchar = true;
			else if (c == '/' && i + 1 < len && source.charAt(i + 1) == '/') {
				inlc = true;
				i++;
			} else if (c == '/' && i + 1 < len && source.charAt(i + 1) == '*') {
				inbc = true;
				i++;
			} else if (c == '#' && i + 1 < len && source.charAt(i + 1) == '{') {
				handleFragment(lastFlagIndex, i);
				cs.clear();
				cs.push('{');
				int i_ = findMatch(cs, source, i + 2);
				handleSqlMark(i, i_);
				i = i_;
				lastFlagIndex = i_ + 1;
			}
		}
		handleFragment(lastFlagIndex, len);
	}

	private void handleFragment(int start, int end) {
		buffer.append(source.substring(start, end));
	}

	/**
	 * #{..{}..}
	 * 
	 * @param start
	 *            index of '#'
	 * @param end
	 *            index of last '}'
	 */
	private void handleSqlMark(int start, int end) throws ParserException {
		SqlBlock sqljBlock = new SqlBlock();
		sqljBlock.setStartIdx(start);
		sqljBlock.setSourceRange(source, start + FLAG.length(), end);

		try {
			sqljBlock.getParsedSql();//maybe throws exception
		} catch (ParserException e) {
			throw new ParserException(source, e.getStart() + start
					+ FLAG.length(), e.getEnd() + start + FLAG.length(),
					e.getMessage());
		}
		parsedSource.registerSql(sqljBlock);
		String replacedStr = String.format("%s(%d,%s)",
				SqlPosition.METHOD_SQL_EXECUTE, sqljBlock.getId(),
				parsedSource.genId("rs"));
		sqljBlock.setReplaceLength(replacedStr.length());
		buffer.append(replacedStr);
	}
}
