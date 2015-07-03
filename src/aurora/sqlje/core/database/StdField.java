package aurora.sqlje.core.database;

public class StdField {
	public static final String OP_UPDATE = "update";
	public static final String OP_INSERT = "insert";
	private String name;
	private String type;
	private String expression;
	private String operation;

	private boolean forInsert, forUpdate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
		if (operation != null) {
			for (String s : operation.split(",")) {
				if (s.equals(OP_INSERT))
					forInsert = true;
				else if (s.equals(OP_UPDATE))
					forUpdate = true;
			}
		}
	}

	public boolean forInsert() {
		return forInsert;
	}

	public boolean forUpdate() {
		return forUpdate;
	}
}
