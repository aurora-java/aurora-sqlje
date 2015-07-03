package aurora.sqlje.core.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import aurora.sqlje.core.ISqlCallStack;
import aurora.sqlje.core.SqlFlag;
import aurora.sqlje.core.annotation.UpdateExpression;
import aurora.sqlje.core.reflect.BeanAnalyzer;
import aurora.sqlje.core.reflect.BeanInfo;
import aurora.sqlje.parser.Parameter;
import aurora.sqlje.parser.ParameterParser;
import aurora.sqlje.parser.ParsedSql;

public class UpdateOperation {

	private ISqlCallStack sqlCallStack;
	private String tableName;
	private String pkName;
	private Map map;
	private Object bean;
	private SqlFlag $sql;
	private int maxNameLength = 0, maxTypeLength = 0;

	private BeanInfo info;

	public UpdateOperation(ISqlCallStack callStack, Object bean) {
		super();
		sqlCallStack = callStack;
		this.bean = bean;
	}

	public UpdateOperation(ISqlCallStack callStack, Object bean,
			String tableName, String pkName) {
		super();
		this.sqlCallStack = callStack;
		this.bean = bean;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	public UpdateOperation(ISqlCallStack callStack, Map map, String tableName,
			String pkName) {
		this.sqlCallStack = callStack;
		this.map = map;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	public void setSqlFlag(SqlFlag sf) {
		this.$sql = sf;
	}

	/**
	 * 
	 * @return update count
	 */
	public int doUpdate() throws SQLException, Exception {
		if (bean == null && map == null)
			throw new IllegalArgumentException(
					"Can not do update without data model(bean or map)");
		if (bean != null) {
			info = BeanAnalyzer.instance().getBeanInfo(bean.getClass());
			if (tableName == null)
				tableName = info.getTableName();
			if (pkName == null)
				pkName = info.getPkName();
		}

		if (tableName == null || tableName.length() == 0)
			throw new IllegalArgumentException(
					"Can not do update without tableName");
		if (pkName == null || pkName.length() == 0)
			throw new IllegalArgumentException(
					"Can not do update without pkName");

		String oriSql = generateUpdateSql();// sql with ${param} flags
		System.out.println(oriSql);
		ParameterParser parser = new ParameterParser(oriSql);
		ParsedSql parsedSql = parser.parse(true);
		Connection conn = sqlCallStack.getCurrentConnection();
		PreparedStatement ps = conn.prepareStatement(parsedSql
				.toStringLiteral());
		System.out.println(parsedSql.toStringLiteral());
		$sql.clear();
		try {
			ArrayList<Parameter> params = parsedSql.getBindParameters();
			for (Parameter p : params) {
				maxNameLength = Math.max(maxNameLength, p.getExpression()
						.length());
			}
			for (int i = 0; i < params.size(); i++) {
				Parameter p = params.get(i);
				String pn = p.getExpression();
				Object value = getFieldValue(pn);
				System.out.printf("Parameter %2d: name=%-" + maxNameLength
						+ "s type=%-20s value=%s\n", (i + 1), pn,
						value == null ? "null" : value.getClass().getName(),
						value);
				ps.setObject(i + 1, value);
			}
			ps.execute();
			$sql.UPDATECOUNT = ps.getUpdateCount();
		} finally {
			if (ps != null)
				try {
					ps.close();
				} catch (Exception e) {
				}
		}
		return $sql.UPDATECOUNT;
	}

	private String generateUpdateSql() {
		if (bean != null)
			return generateUpdateSqlForBean();
		if (map != null)
			return generateUpdateSqlForMap();
		return null;
	}

	private String generateUpdateSqlForBean() {
		StringBuilder sb = new StringBuilder(1024);
		sb.append("UPDATE ").append(tableName);// .append("\n");
		sb.append(" SET ");
		for (Field f : info.getColumnFields()) {
			String name = f.getName();
			if (name.equalsIgnoreCase(pkName)) {
				/*
				 * this maybe happens when the bean is not well annotated. in
				 * this case the BeanAnalyzer will treat pk field as normal
				 * field.
				 */
				continue;
			}

			sb.append(name);

			UpdateExpression ue = f.getAnnotation(UpdateExpression.class);
			if (ue == null) {
				sb.append("=${").append(name).append("},");
			} else {
				sb.append("=").append(ue.value()).append(",");
			}
		}
		if (info.isStdWhoEnabled()) {
			StdField[] sfs = info.getStdFields();
			for (StdField sf : sfs) {
				if (sf.forUpdate()) {
					sb.append(sf.getName()).append("=")
							.append(sf.getExpression()).append(",");
				}
			}
		}
		sb.deleteCharAt(sb.length() - 1);// delete last comma

		sb.append(" WHERE ").append(pkName).append("=${").append(pkName)
				.append("}");
		return sb.toString();
	}

	private String generateUpdateSqlForMap() {
		StringBuilder sb = new StringBuilder(1024);

		sb.append("UPDATE ").append(tableName);
		sb.append(" SET ");
		Set keys = map.keySet();
		for (Object o : keys) {
			if (!(o instanceof String))
				continue;
			String key = o.toString();
			if (key.equals(pkName) || !isValidColumn(key) || isStdfield(key))
				continue;
			sb.append(o).append("=${").append(key).append("},");
		}

		/*
		 * stdfields is not append automatically
		 */
		if (isStdwhoEnabled(map)) {
			StdField[] sfs = BeanAnalyzer.instance().getStdfields();
			for (StdField sf : sfs) {
				if (sf.forUpdate()) {
					sb.append(sf.getName()).append("=")
							.append(sf.getExpression()).append(",");
				}
			}
		}

		sb.deleteCharAt(sb.length() - 1);// delete last comma
		sb.append(" WHERE ").append(pkName).append("=${").append(pkName)
				.append("}");
		return sb.toString();
	}

	protected Object getFieldValue(String name) throws Exception {
		if (name.contains("@")) {
			return sqlCallStack.getContextData().getObject(name);
		}
		if (bean != null) {
			return bean.getClass().getField(name).get(bean);
		} else if (map != null)
			return map.get(name);
		return null;
	}

	/**
	 * the name is a std field?
	 * 
	 * @param name
	 * @return
	 */
	protected boolean isStdfield(String name) {
		for (StdField sf : BeanAnalyzer.instance().getStdfields()) {
			if (sf.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	/**
	 * has key '@stdwho' ,and the value equivalent to true
	 * 
	 * @param m
	 * @return
	 */
	protected boolean isStdwhoEnabled(Map m) {
		Object object = m.get("@stdwho");
		if (object instanceof Boolean) {
			return ((Boolean) object).booleanValue();
		} else if (object instanceof String)
			return Boolean.parseBoolean(object.toString());
		return false;
	}

	/**
	 * a valid column name must starts with a-z or A-Z
	 * 
	 * @param s
	 * @return
	 */
	protected boolean isValidColumn(String s) {
		if (s == null || s.length() == 0)
			return false;
		char c = s.charAt(0);
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

}
