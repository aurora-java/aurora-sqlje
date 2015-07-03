package aurora.sqlje.core.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import uncertain.ocm.ReflectionMapper;
import aurora.sqlje.core.DataTransfer;
import aurora.sqlje.core.ISqlCallStack;
import aurora.sqlje.core.annotation.InsertExpression;
import aurora.sqlje.core.reflect.BeanAnalyzer;
import aurora.sqlje.core.reflect.BeanInfo;
import aurora.sqlje.parser.Parameter;
import aurora.sqlje.parser.ParameterParser;
import aurora.sqlje.parser.ParsedSql;

public abstract class AbstractInsertOperation {

	protected ISqlCallStack context;

	private Object recordBean;
	private Map recordMap;

	private String tableName;
	private String pkName;
	private ParsedSql parsedSql;
	private ReflectionMapper reflectionMapper;
	private BeanInfo info;

	public AbstractInsertOperation(ISqlCallStack context, Object bean,
			String tableName, String pkName) {
		super();
		this.context = context;
		this.recordBean = bean;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	public AbstractInsertOperation(ISqlCallStack context, Object bean) {
		this(context, bean, null, null);
	}

	public AbstractInsertOperation(ISqlCallStack context, Map map,
			String tableName, String pkName) {
		super();
		this.context = context;
		this.recordMap = map;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	/**
	 * do insert operation
	 * 
	 * @return primary key
	 */
	public Object doInsert() throws SQLException, Exception {
		Connection conn = context.getCurrentConnection();
		if (recordBean == null && recordMap == null)
			throw new IllegalArgumentException(
					"Can not do insert without data model(bean or map).");
		if (recordBean != null) {
			info = BeanAnalyzer.instance().getBeanInfo(recordBean.getClass());
			if (tableName == null)
				tableName = info.getTableName();
			if (pkName == null)
				pkName = info.getPkName();
		}

		if (tableName == null || tableName.length() == 0)
			throw new IllegalArgumentException(
					"Can not do insert without tableName");
		if (pkName == null || pkName.length() == 0)
			throw new IllegalArgumentException(
					"Can not do insert without pkName");

		String _sql_ = createInsertSql();
		String sql = getPrefix() + _sql_ + getSuffix(pkName);
		ParameterParser parser = new ParameterParser(sql);
		parsedSql = parser.parse(true);
		sql = parsedSql.toStringLiteral();
		log(sql);
		PreparedStatement ps = createStatement(conn, sql);
		performParameterBinding(ps);
		Object pk = execute(ps);
		log("return pk :" + pk);
		setPrimaryKeyBack(pk);
		return pk;
	}

	private void log(Object msg) {
		// System.out.println("[INSERT]"+msg);
	}

	private void setPrimaryKeyBack(Object pk) {
		if (recordBean != null) {
			try {
				recordBean.getClass().getField(pkName).set(recordBean, pk);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (recordMap != null)
			recordMap.put(pkName, pk);
	}

	/**
	 * execute the statement, retrieve and return primary key
	 * 
	 * @param ps
	 * @return
	 * @throws SQLException
	 */
	protected abstract Object execute(PreparedStatement ps) throws SQLException;

	protected abstract PreparedStatement createStatement(Connection conn,
			String sql) throws SQLException;

	protected int performParameterBinding(PreparedStatement ps)
			throws Exception {
		ArrayList<Parameter> params = parsedSql.getBindParameters();
		int pkIndex = -1;
		for (int i = 0; i < params.size(); i++) {
			Parameter p = params.get(i);
			log(p);
			if (p.getType() != Parameter.OUT) {
				Object v = getFieldValue(p.getExpression());
				log("\t" + v);
				ps.setObject(i + 1, v);
			} else
				pkIndex = i + 1;

		}
		return pkIndex;
	}

	protected Object getFieldValue(String name) throws Exception {
		if (name.contains("@")) {
			return context.getContextData().getObject(name);
		}
		if (recordBean != null) {
			return recordBean.getClass().getField(name).get(recordBean);
		} else if (recordMap != null)
			return recordMap.get(name);
		return null;
	}

	protected String getPrefix() {
		return "";
	}

	protected String createInsertSql() {
		if (recordBean != null)
			return createInsertSqlForBean();
		if (recordMap != null)
			return createInsertSqlForMap();
		return null;
	}

	private String createInsertSqlForBean() {
		StringBuilder sb = new StringBuilder(1024);
		sb.append("INSERT INTO ").append(tableName);
		sb.append(" (");
		sb.append(info.getPkName()).append(",");
		for (Field f : info.getColumnFields()) {
			if (f.getName().equalsIgnoreCase(pkName))
				continue;
			sb.append(f.getName()).append(",");
		}
		if (info.isStdWhoEnabled()) {
			StdField[] sfs = info.getStdFields();
			for (StdField sf : sfs) {
				if (sf.forInsert()) {
					sb.append(sf.getName()).append(",");
				}
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" ) VALUES (");
		sb.append(getInsertExpressionForPk()).append(",");
		for (Field f : info.getColumnFields()) {
			if (f.getName().equalsIgnoreCase(pkName))
				continue;
			InsertExpression ie = f.getAnnotation(InsertExpression.class);
			if (ie == null) {
				sb.append("${").append(f.getName()).append("},");
			} else {
				sb.append(ie.value()).append(",");
			}
		}
		if (info.isStdWhoEnabled()) {
			StdField[] sfs = info.getStdFields();
			for (StdField sf : sfs) {
				if (sf.forInsert()) {
					sb.append(sf.getExpression()).append(",");
				}
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" )");
		return sb.toString();
	}

	private String createInsertSqlForMap() {

		boolean stdwho = isStdwhoEnabled(recordMap);

		StringBuilder sb = new StringBuilder(1024);
		sb.append("INSERT INTO ").append(tableName);
		sb.append(" (");
		sb.append(pkName).append(",");
		Set<String> keySets = recordMap.keySet();
		ArrayList<String> columns = new ArrayList<String>();
		for (String key : keySets) {
			if (pkName.equals(key))
				continue;
			if (isValidColumn(key) && !isStdfield(key)) {
				Object v = recordMap.get(key);
				if (v != null
						&& DataTransfer.supported_type_list.contains(v
								.getClass()))
					columns.add(key);
			}
		}
		for (String key : columns) {
			sb.append(key).append(",");
		}
		if (stdwho) {
			StdField[] sfs = BeanAnalyzer.instance().getStdfields();
			for (StdField sf : sfs) {
				if (sf.forInsert()) {
					sb.append(sf.getName()).append(",");
				}
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" ) VALUES (");
		sb.append(getInsertExpressionForPk()).append(",");
		for (String key : columns) {
			sb.append("${").append(key).append("},");
		}
		if (stdwho) {
			StdField[] sfs = BeanAnalyzer.instance().getStdfields();
			for (StdField sf : sfs) {
				if (sf.forInsert()) {
					sb.append(sf.getExpression()).append(",");
				}
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" )");
		return sb.toString();
	}

	protected abstract String getInsertExpressionForPk();

	protected String getTableName() {
		return tableName;
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

	protected boolean isValidColumn(String s) {
		if (s == null || s.length() == 0)
			return false;
		char c = s.charAt(0);
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

	protected String getPkField() {
		return pkName;
	}

	/**
	 * not include standard-who fields
	 * 
	 * @return
	 */
	protected String[] getColumns() {
		return new String[0];
	}

	protected String getSuffix(String pkName) {
		return "";
	}

	public void setReflectionMapper(ReflectionMapper reflectionMapper) {
		this.reflectionMapper = reflectionMapper;
	}

}
