package aurora.sqlje.core;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

import aurora.sqlje.exception.*;

public class DataTransfer {
	public static Class<?>[] supported_types = { String.class, int.class,
			double.class, long.class, Date.class, Timestamp.class,
			Integer.class, Long.class, Double.class, BigDecimal.class };
	public static List<Class<?>> supported_type_list = Arrays
			.asList(supported_types);

	/**
	 * transfer one row into given type object
	 * 
	 * @param clazz
	 *            <table> <tr><td>sub class of Map</td><td>HashMap(interface) or
	 *            clazz.newInstance()</td></tr> <tr><td>one of
	 *            {@link #supported_types}</td><td>
	 *            {@link #verboseGet(ResultSet, String, Class)} </td></tr>
	 *            <tr><td><i>others</i></td><td>java bean(public
	 *            fields)</td></tr> </table>
	 * @param rs
	 * @return
	 * @throws Exception
	 *             if <b>no_data_found</b> or <b>too_many_rows</b> or try to get
	 *             one field but with more than one column
	 *             (<b>too_many_columns</b>) or any others exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T transfer1(Class<T> clazz, ResultSet rs)
			throws Exception {
		if (!rs.next())
			throw new NoDataFoundException();
		List<String> column_names = getColumnNames(rs);
		T res = null;
		if (Map.class.isAssignableFrom(clazz)) {
			Map map = null;
			if (!clazz.isInterface()) {
				map = (Map) clazz.newInstance();
			} else
				map = new HashMap();
			fillMap(map, rs, column_names);
			res = (T) map;
		} else if (supported_type_list.contains(clazz)) {
			if (column_names.size() > 1)
				throw new TooManyColumnsException();
			return (T) verboseGet(rs, column_names.get(0), clazz);
		} else {
			Object bean = clazz.newInstance();
			fillBean(bean, rs, column_names);
			res = (T) bean;
		}
		if (rs.next())
			throw new TooManyRowsException();
		return res;
	}

	@SuppressWarnings("rawtypes")
	public static Map transfer1(Map map, ResultSet rs, List<String> column_names)
			throws Exception {
		if (!rs.next())
			throw new NoDataFoundException();
		fillMap(map, rs, column_names);
		if (rs.next())
			throw new TooManyRowsException();
		return map;
	}

	public static List<String> getColumnNames(ResultSet rs) throws SQLException {
		List<String> column_names = new ArrayList<String>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int c = rsmd.getColumnCount();
		for (int i = 1; i <= c; i++)
			column_names.add(rsmd.getColumnName(i));
		return column_names;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void fillMap(Map map, ResultSet rs, List<String> column_names)
			throws Exception {
		for (String name : column_names) {
			map.put(name.toLowerCase(), rs.getObject(name));
		}
	}

	public static void fillBean(Object bean, ResultSet rs,
			List<String> column_names) throws IllegalArgumentException,
			IllegalAccessException, SQLException {
		Field[] flds = bean.getClass().getFields();
		
		ArrayList<String> acceptedColumns = new ArrayList<String>();
		for (Field f : flds) {
			if (supported_type_list.contains(f.getType())) {
				String upperName = f.getName().toUpperCase();
				if (column_names.contains(upperName)) {
					f.set(bean, verboseGet(rs, upperName, f.getType()));
					acceptedColumns.add(upperName);
				}
			}
		}
		if (bean instanceof Dynamic
				&& acceptedColumns.size() < column_names.size()) {
			for (String e : column_names) {
				if (!acceptedColumns.contains(e)) {
					((Dynamic) bean).set(e, rs.getObject(e));
				}
			}
		}
		if (bean instanceof Initable) {
			((Initable) bean).init();
		}
	}

	public static Object verboseGet(ResultSet rs, String name, Class<?> type)
			throws SQLException {
		if (type == String.class)
			return getString(rs, name);
		if (type == double.class)
			return getdouble(rs, name);
		if (type == java.sql.Date.class)
			return getDate(rs, name);
		if (type == int.class)
			return getint(rs, name);
		if (type == long.class)
			return getlong(rs, name);
		if (type == Integer.class)
			return getInteger(rs, name);
		if (type == Long.class)
			return getLong(rs, name);
		if (type == Double.class)
			return getDouble(rs, name);
		if (type == java.util.Date.class)
			return getUtilDate(rs, name);
		return rs.getObject(name);
	}

	public static BigDecimal getBigDecimal(ResultSet rs, String name)
			throws SQLException {
		return rs.getBigDecimal(name);
	}

	public static int getint(ResultSet rs, String name) throws SQLException {
		return rs.getInt(name);
	}

	public static Integer getInteger(ResultSet rs, String name)
			throws SQLException {
		BigDecimal bd = rs.getBigDecimal(name);
		if (bd == null)
			return null;
		return bd.intValue();
	}

	public static long getlong(ResultSet rs, String name) throws SQLException {
		return rs.getLong(name);
	}

	public static Long getLong(ResultSet rs, String name) throws SQLException {
		BigDecimal bd = rs.getBigDecimal(name);
		if (bd == null)
			return null;
		return bd.longValue();
	}

	public static double getdouble(ResultSet rs, String name)
			throws SQLException {
		return rs.getDouble(name);
	}

	public static Double getDouble(ResultSet rs, String name)
			throws SQLException {
		BigDecimal bd = rs.getBigDecimal(name);
		if (bd == null)
			return null;
		return bd.doubleValue();
	}

	public static String getString(ResultSet rs, String name)
			throws SQLException {
		return rs.getString(name);
	}

	public static java.util.Date getUtilDate(ResultSet rs, String name)
			throws SQLException {
		java.sql.Date d = rs.getDate(name);
		if (d == null)
			return null;
		return new java.util.Date(d.getTime());
	}

	public static java.sql.Date getDate(ResultSet rs, String name)
			throws SQLException {
		return rs.getDate(name);
	}

	public static Timestamp getTimestamp(ResultSet rs, String name)
			throws SQLException {
		return rs.getTimestamp(name);
	}

	/**
	 * 
	 * @param obj
	 *            <table border='0'>
	 *            <tr><td>Number</td><td>Number.longValue()</td></tr>
	 *            <tr><td>String</td><td>Long.parseLong(String)</td></tr>
	 *            <tr><td>null</td><td>0</td></tr>
	 *            <tr><td><b>others</b></td><td>NumberFormatException</td></tr>
	 *            </table>
	 * @return
	 */
	public static long castLong(Object obj) {
		if (obj instanceof Number)
			return ((Number) obj).longValue();
		if (obj instanceof String)
			return Long.parseLong((String) obj);
		if (obj == null)
			return 0L;
		throw new NumberFormatException(obj + " is not a number.");
	}
	
	/**
	 * 
	 * @param obj
	 *            <table border='0'>
	 *            <tr><td>Number</td><td>Number.longValue()</td></tr>
	 *            <tr><td>String</td><td>Long.parseLong(String)</td></tr>
	 *            <tr><td>null</td><td>default_</td></tr>
	 *            <tr><td><b>others</b></td><td>NumberFormatException</td></tr>
	 *            </table>
	 * @param default_ the default return (if obj is null)
	 * @return
	 */
	public static Long castLong(Object obj,Long default_) {
		if (obj instanceof Number)
			return ((Number) obj).longValue();
		if (obj instanceof String)
			return Long.parseLong((String) obj);
		if (obj == null)
			return default_;
		throw new NumberFormatException(obj + " can not be tranform to Long.");
	}

	/**
	 * {@link #castLong(Object)}
	 * 
	 * @param obj
	 * @return
	 */
	public static int castInt(Object obj) {

		return (int) castLong(obj);
	}

}
