package aurora.sqlje.core.database;

public class InsertField {
	String name;
	Class<?> type;
	boolean paraBinding=true;
	String expression;


	private Object value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public boolean isParamBinding() {
		return paraBinding;
	}

	public void setParaBinding(boolean isbinding) {
		this.paraBinding = isbinding;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	public Object getValue() {
		return value;
	}

	public void setValue(Object object) {
		this.value = object;
	}
}
