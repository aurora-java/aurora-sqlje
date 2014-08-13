package aurora.sqlje.parser;

import java.util.HashMap;

import aurora.sqlje.core.IdentifierGenerator;


public class ParsedSource {
	private final IdentifierGenerator idGener;
	private HashMap<String, String> variableTypeMap = new HashMap<String, String>();
	private HashMap<Integer, SqlBlock> idToSqlMap = new HashMap<Integer, SqlBlock>();
	private int sqlid = 0;
	private StringBuilder buffer;

	private ParsedSource() {
		super();
		idGener = IdentifierGenerator.newInstance();
	}

	public void registerSql(SqlBlock sqljb) {
		sqljb.setId(sqlid);
		idToSqlMap.put(sqlid, sqljb);
		sqlid++;
	}

	public SqlBlock getSqlById(int id) {
		return idToSqlMap.get(id);
	}

	public int getSqljBlockSize() {
		return idToSqlMap.size();
	}

	public void registerVariableType(String varName, String type) {
		variableTypeMap.put(varName, type);
	}

	public String getVariableType(String varName, String default_) {
		String type = getVariableType(varName);
		if (type == null)
			return default_;
		return type;
	}

	public String getVariableType(String varName) {
		String type = variableTypeMap.get(varName);
		if (type == null)
			return null;
		int idx = type.lastIndexOf('.');
		if (idx != -1)
			return type.substring(idx + 1);
		if (!"Integer".equals(type))
			return Character.toUpperCase(type.charAt(0)) + type.substring(1);
		return "Int";
	}

	public String getOriginalVariableType(String var) {
		return variableTypeMap.get(var);
	}

	/**
	 * {@link java.sql.Types}
	 * 
	 * @param varName
	 * @return
	 */
	public String getVariableSqlType(String varName) {
		String type = variableTypeMap.get(varName);
		if (type == null || type.equals("String"))
			return "VARCHAR";
		type = type.toLowerCase();
		if ("int".equals(type) || "long".equals(type) || "double".equals(type)
				|| "bigdecmial".equals(type) || "integer".equals(type))
			return "DECIMAL";
		if ("date".equals(type))
			return "DATE";
		if ("timestamp".equals(type))
			return "TIMESTAMP";
		return "VARCHAR";
	}

	public String genId(String namePart) {
		return idGener.gen(namePart);
	}

	public static ParsedSource newSession() {
		return new ParsedSource();
	}

	public void setBuffer(StringBuilder out) {
		this.buffer = out;
	}

	public StringBuilder getBuffer() {
		return buffer;
	}
}
