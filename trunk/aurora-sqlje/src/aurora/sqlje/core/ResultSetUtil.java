package aurora.sqlje.core;

import java.math.BigDecimal;
import java.util.Map;

public class ResultSetUtil {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T get(Map m, String name, Class<T> type) {
		Object v = m.get(name);
		if (type == String.class) {
			return (T) (v == null ? null : v.toString());
		}
		if (type == int.class) {
			if (v == null)
				return (T) new Integer(0);
			if (v instanceof Number)
				return (T) new Integer(((Number) v).intValue());
			return (T) v;//will throw exception
		}
		if (type == double.class) {
			if (v == null)
				return (T) new Double(0);
			if (v instanceof Number)
				return (T) new Double(((Number) v).doubleValue());
			return (T) v;//will throw exception
		}
		if (type == java.sql.Date.class) {
			if (v instanceof java.sql.Date)
				return (T) v;
			if (v instanceof java.util.Date)
				return (T) new java.sql.Date(((java.util.Date) v).getTime());
			if (v instanceof java.sql.Timestamp)
				return (T) new java.sql.Date(((java.sql.Timestamp) v).getTime());
			return (T) v;
		}
		if (type == java.sql.Timestamp.class) {
			if (v instanceof java.sql.Timestamp)
				return (T) v;
			if (v instanceof java.sql.Date)
				return (T) new java.sql.Timestamp(((java.sql.Date) v).getTime());
			if (v instanceof java.util.Date)
				return (T) new java.sql.Timestamp(
						((java.util.Date) v).getTime());
			return (T) v;
		}
		if (type == BigDecimal.class) {
			return (T) v;
		}
		if (type == java.util.Date.class) {
			if (v instanceof java.util.Date)
				return (T) v;
			if (v instanceof java.sql.Date)
				return (T) new java.util.Date(((java.sql.Date) v).getTime());
			if (v instanceof java.sql.Timestamp)
				return (T) new java.util.Date(
						((java.sql.Timestamp) v).getTime());
			return (T) v;
		}
		if (type == Integer.class) {
			if (v == null)
				return null;
			if (v instanceof Number)
				return (T) new Integer(((Number) v).intValue());
			return (T) v;
		}
		if (type == Double.class) {
			if (v == null)
				return null;
			if (v instanceof Number)
				return (T) new Double(((Number) v).doubleValue());
			return (T) v;
		}
		if (type == Long.class) {
			if (v == null)
				return null;
			if (v instanceof Number)
				return (T) new Long(((Number) v).longValue());
			return (T) v;
		}
		return (T) v;
	}
}
