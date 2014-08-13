package aurora.sqlje.exception;

public class ParserException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6284174097199268758L;

	private String source;
	private int start, end;

	public ParserException(String source, int start, int end, String message) {
		super(message);
		this.source = source;
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getErrorText() {
		return source.substring(start, end);
	}

	//	void calcDetailInfo(String source, int idx) {
	//		this.errorChar = source.charAt(idx);
	//		Pattern p = Pattern.compile("\\r\\n|\\n|\\r", Pattern.DOTALL);
	//		Matcher m = p.matcher(source);
	//		while (m.find() && m.start() < idx) {
	//			line++;
	//			column = m.end();
	//		}
	//		errorLine = source.substring(column, idx + 1);
	//		column = idx - column + 1;
	//	}

	public String getSource() {
		return source;
	}
}
