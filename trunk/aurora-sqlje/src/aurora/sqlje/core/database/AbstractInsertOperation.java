package aurora.sqlje.core.database;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uncertain.ocm.ReflectionMapper;
import aurora.database.service.SqlServiceContext;
import aurora.sqlje.core.DataTransfer;
import aurora.sqlje.core.ISqlCallStack;
import aurora.sqlje.core.annotation.Column;
import aurora.sqlje.core.annotation.InsertExpression;
import aurora.sqlje.core.annotation.PK;
import aurora.sqlje.core.annotation.Table;
import aurora.sqlje.parser.Parameter;
import aurora.sqlje.parser.ParsedSql;
import aurora.sqlje.parser.SqlBlock;

public abstract class AbstractInsertOperation {

	static String createdByField = "CREATED_BY";
	static String creationDateField = "CREATION_DATE";
	static String lastUpdatedByField = "LAST_UPDATED_BY";
	static String lastUpdatedDateField = "LAST_UPDATE_DATE";
	static String[] stdwho_fields = { createdByField, creationDateField,
			lastUpdatedByField, lastUpdatedDateField };
	static List<String> stdwho_fields_list = Arrays.asList(stdwho_fields);

	protected ISqlCallStack context;
	private boolean standardWhoEnabled;

	protected String[] columns;
	protected HashMap<String, InsertField> insertFieldOptions = new HashMap<String, InsertField>();
	private Object recordBean;
	private Map recordMap;

	private String tableName;
	private String pkName;
	private ParsedSql parsedSql;
	private ReflectionMapper reflectionMapper;

	public AbstractInsertOperation(ISqlCallStack context, Object bean, String tableName,
			String pkName) {
		super();
		this.context = context;
		this.recordBean = bean;
		this.tableName = tableName;
		this.pkName = pkName;
	}

	public AbstractInsertOperation(ISqlCallStack context, Object bean) {
		this(context, bean, null, null);
	}

	public AbstractInsertOperation(ISqlCallStack context, Map map, String tableName,
			String pkName) {
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
		getInsertFieldOptions();
		String _sql_ = createInsertSql();
		String sql = getPrefix() + _sql_ + getSuffix(pkName);
		SqlBlock sqlb = new SqlBlock();
		sqlb.setSql(sql);
		parsedSql = sqlb.getParsedSql();
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
		//System.out.println("[INSERT]"+msg);
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
				InsertField insf = insertFieldOptions.get(p.getExpression());
				if (insf != null) {
					if (insf.isParamBinding()) {
						log("\t" + insf.getValue());
						ps.setObject(i + 1, insf.getValue());
					} else {
						log("\t" + insf.getExpression());
						ps.setObject(i + 1, getFieldValue(p.getExpression()));
					}
				} else {
					Object v = getFieldValue(p.getExpression());
					log("\t" + v);
					ps.setObject(i + 1, v);
				}
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

	protected String getDateExpression() {
		return "CURRENT_DATE";
	}

	protected String getTimeExpression() {
		return "CURRENT_TIMESTAMP";
	}

	protected String getPrefix() {
		return "";
	}

	protected String createInsertSql() {
		StringBuilder strb = new StringBuilder();
		strb.append("INSERT INTO ").append(getTableName()).append("\n(");
		for (int i = 0; i < columns.length; i++) {
			if (i != 0)
				strb.append(",");
			strb.append(columns[i]);

		}
		if (standardWhoEnabled) {
			for (String s : stdwho_fields)
				strb.append(",").append(s);
		}
		strb.append(")\nVALUES\n(");
		for (int i = 0; i < columns.length; i++) {
			if (i != 0)
				strb.append(",");
			InsertField insf = insertFieldOptions.get(columns[i]);
			if (insf == null) {
				// System.err.println("InsertField for " + columns[i]
				// + " is null.");
				continue;
			}
			if (insf.isParamBinding()) {
				// strb.append("?")
				strb.append("${" + insf.getName() + "}");
			} else {
				strb.append(insf.getExpression());
			}

		}
		if (standardWhoEnabled) {
			for (String s : stdwho_fields) {
				InsertField insf = insertFieldOptions.get(s);
				if (insf.isParamBinding())
					strb.append(",").append("${" + insf.getName() + "}");
				else
					strb.append(",").append(insf.getExpression());
			}
		}
		strb.append(")");
		return strb.toString();
	}

	protected Map<String, InsertField> getInsertFieldOptions() throws Exception {
		insertFieldOptions.clear();
		if (recordBean != null)
			getInsertFieldOptionsOfBean(recordBean);
		else if (recordMap != null)
			getInsertFieldOptionsOfMap(recordMap);
		if (standardWhoEnabled) {
			SqlServiceContext sqlsvc = SqlServiceContext
					.createSqlServiceContext(context.getContextData());
			Long user_id = sqlsvc.getSession().getLong("user_id");
			insertFieldOptions.put(createdByField,
					createInsertField(createdByField, Long.class, user_id));
			insertFieldOptions.put(lastUpdatedByField,
					createInsertField(lastUpdatedByField, Long.class, user_id));

			insertFieldOptions.put(creationDateField,
					createInsertField(getTimeExpression()));
			insertFieldOptions.put(lastUpdatedDateField,
					createInsertField(getTimeExpression()));
		}
		return insertFieldOptions;
	}

	protected InsertField createInsertField(String name, Class<?> type,
			Object value) {
		InsertField insf = new InsertField();
		insf.setName(name);
		insf.setType(type);
		insf.setValue(value);
		return insf;
	}

	protected InsertField createInsertField(String expression) {
		InsertField insf = new InsertField();
		insf.setParaBinding(false);
		insf.setExpression(expression);
		return insf;
	}

	private void getInsertFieldOptionsOfBean(Object bean) throws Exception {
		getTableName0(bean);
		ArrayList<String> columnList = new ArrayList<String>();
		java.lang.reflect.Field[] fields = bean.getClass().getFields();
		for (java.lang.reflect.Field f : fields) {
			if (f.getModifiers() != Modifier.PUBLIC)
				continue;
			Class<?> type = f.getType();
			if (!DataTransfer.supported_type_list.contains(type))
				continue;
			
			String name = f.getName();
			if (stdwho_fields_list.contains(name.toUpperCase()))
				continue;
			columnList.add(name);
			Column col = f.getAnnotation(Column.class);
			InsertExpression insertExp = f
					.getAnnotation(InsertExpression.class);
			InsertField insf = new InsertField();
			insf.setName(name);
			if (col != null && col.name().length() > 0)
				name = col.name();
			PK pkaf = f.getAnnotation(PK.class);
			if (pkaf != null && pkName == null)
				pkName = name;
			insf.setName(name);
			if (name.equals(pkName)) {
				// pkName = name;
				// System.out.println("pk:" + pkName);
				insf.setParaBinding(false);
				insf.setExpression(getInsertExpressionForPk());
			} else if (insertExp != null && insertExp.value().length() > 0) {
				insf.setParaBinding(false);
				insf.setExpression(insertExp.value());
			} else {
				insf.setType(type);
				insf.setValue(f.get(bean));
			}
			insertFieldOptions.put(name, insf);
		}
		columns = columnList.toArray(new String[columnList.size()]);

	}

	private void getInsertFieldOptionsOfMap(Map<String, Object> map) {
		if (!map.keySet().contains(pkName))
			map.put(pkName, null);
		ArrayList<String> columnList = new ArrayList<String>();
		for (String s : map.keySet()) {
			if (s != null && s.length() > 0 && s.charAt(0) != '$') {
				Object v = map.get(s);
				if (v != null
						&& !DataTransfer.supported_type_list.contains(v
								.getClass()))
					continue;
				if (stdwho_fields_list.contains(s.toUpperCase()))
					continue;
				columnList.add(s);
			}
		}
		columns = columnList.toArray(new String[columnList.size()]);
		for (String c : columns) {
			InsertField insf = new InsertField();
			insf.setName(c);
			if (c.equals(pkName)) {
				insf.setParaBinding(false);
				insf.setExpression(getInsertExpressionForPk());
			} else {
				Object v = map.get(c);
				insf.setType(v == null ? Object.class : v.getClass());
				insf.setValue(v);
			}

			insertFieldOptions.put(c, insf);
		}
	}

	private void getTableName0(Object bean) {
		if (tableName != null)
			return;
		Table dbt = bean.getClass().getAnnotation(Table.class);
		if (dbt != null) {
			tableName = dbt.name();
			standardWhoEnabled = dbt.stdwho();
		} else {
			tableName = bean.getClass().getSimpleName().toUpperCase();
		}
	}

	protected abstract String getInsertExpressionForPk();

	public boolean isStandardWhoEnabled() {
		return standardWhoEnabled;
	}

	public void setStandardWhoEnabled(boolean enabled) {
		standardWhoEnabled = enabled;
	}

	protected String getTableName() {
		return tableName;
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
