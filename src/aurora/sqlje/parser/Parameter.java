package aurora.sqlje.parser;

public class Parameter {
	/**
	 * dynamic parameter , not parameter binding <ul> <li>${!expression}</li>
	 * </ul>
	 * 
	 */
	public static final int NONE = 0;
	/**
	 * in parameter <ul> <li>${expression}</li> </ul>
	 */
	public static final int IN = 1;
	/**
	 * out parameter <ul> <li>${@variable}</li> </ul>
	 */
	public static final int OUT = 2;

	public static final String[] TYPE_DESCRIPTION = { "dynamic", "in", "out" };

	private int type = IN;
	private String expression = null;

	public Parameter(int type, String expression) {
		super();
		setType(type);
		setExpression(expression);
	}

	public Parameter() {
		this(IN, null);
	}

	public int getType() {
		return type;
	}

	/**
	 * 
	 * @param type
	 *            one of {@link #NONE},{@link #IN},{@link #OUT}
	 */
	public void setType(int type) {
		if (type != NONE && type != IN && type != OUT)
			throw new IllegalArgumentException("parameter type " + type
					+ " is not valid.");
		this.type = type;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String toString() {
		return "[" + TYPE_DESCRIPTION[type] + "]" + expression;
	}
}
