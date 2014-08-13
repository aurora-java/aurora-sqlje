package aurora.sqlje.parser;

import aurora.sqlje.exception.ParserException;

public abstract class Parser {
	public static final String ITERATOR = "iterator";
	public static final String[] MODIFIERS = { "public", "private",
			"protected", "static", "final" };
	protected String source;
	protected int len;

	protected String lastWord;

	public Parser(String source) {
		super();
		setSource(source);
	}

	public void setSource(String source) {
		this.source = source;
		this.len = source.length();
	}

	public abstract Object parse() throws ParserException, Exception;

	/**
	 * when this method is called,it means a none-whitespace character is
	 * needed.
	 * 
	 * @param startIdx
	 * @return
	 * @throws ParserException
	 */
	protected int skipWhitespace(int startIdx) throws ParserException {
		for (; startIdx < len; startIdx++) {
			if (!Character.isWhitespace(source.charAt(startIdx)))
				return startIdx;
		}
		throw new ParserException(source, len - 1, len,
				"Unexpected end of source.");
	}

	protected int skipJavaIdPart(int startIdx) {
		for (; startIdx < len; startIdx++) {
			if (!Character.isJavaIdentifierPart(source.charAt(startIdx)))
				return startIdx;
		}
		return startIdx;
	}

	protected int skipJavaWord(int startIdx) throws ParserException {
		int i1 = skipWhitespace(startIdx);
		if (!Character.isJavaIdentifierStart(source.charAt(i1)))
			throw new ParserException(source, i1, i1 + 1, source.charAt(i1)
					+ " is not a JavaIdentifierStart");
		int i2 = skipJavaIdPart(i1);
		if (i2 == i1)
			throw new ParserException(source, i2, i2 + 1, source.charAt(i2)
					+ " is not a whitespace");
		lastWord = source.substring(i1, i2);
		int i3 = skipWhitespace(i2);
		return i3;
	}

	protected int skip(int startIdx, char c) throws ParserException {
		startIdx = skipWhitespace(startIdx);
		if (source.charAt(startIdx) == c)
			return startIdx;
		throw new ParserException(source, startIdx, startIdx + 1, "unexpect "
				+ c + " when expect " + source.charAt(startIdx));
	}

	/**
	 * 
	 * when this method is called, the CharStack {@code cs} has only one character('(' or '{')<br>
	 * this method will find the matched ')' or '}' in {@code source} from {@code startIdx}<br>
	 * string 'x(xx' or "cc}cc" will be skipped
	 * @param cs
	 * @param source
	 * @param startIdx
	 * @return the index of matched character
	 */
	protected int findMatch(CharStack cs, String source, int startIdx) {
		char topc = cs.peek();
		int i_ = startIdx;
		char tc;
		char expected = 0;
		while (i_ < source.length() && !cs.isEmpty()) {
			tc = source.charAt(i_++);
			if (expected != 0) {
				if (tc == '\\') {
					i_++;
					continue;
				}
				if (expected != tc)
					continue;
			}
			if (tc == topc)
				cs.push(tc);
			else if (topc == '(' && tc == ')') {
				if (cs.peek() == '(')
					cs.pop();
				else
					cs.push(tc);
			} else if (topc == '{' && tc == '}') {
				if (cs.peek() == '{')
					cs.pop();
				else
					cs.push(tc);
			} else if (tc == '\'') {
				if (cs.peek() == '\'') {
					cs.pop();
					expected = 0;
				} else {
					cs.push(tc);
					expected = '\'';
				}
			} else if (tc == '"') {
				if (cs.peek() == '"') {
					cs.pop();
					expected = 0;
				} else {
					cs.push(tc);
					expected = '"';
				}
			}
		}
		return i_ - 1;
	}
}
