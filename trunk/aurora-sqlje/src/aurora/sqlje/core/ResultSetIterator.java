package aurora.sqlje.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ResultSetIterator<T> implements Iterable<T>, Iterator<T> {
	final ResultSet rs;
	Class<T> clazz;
	T current;
	List<String> column_names;

	public ResultSetIterator(ResultSet rs) {
		super();
		this.rs = rs;
		if (rs == null)
			throw new NullPointerException("ResultSet can not be null.");
		getColumnsInfo();
	}

	public ResultSetIterator(ResultSet rs, Class<T> clazz) {
		this(rs);
		this.clazz = clazz;
	}

	private void getColumnsInfo() {
		try {
			column_names = DataTransfer.getColumnNames(rs);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	void createCurrent() {
		try {
			if (DataTransfer.supported_type_list.contains(clazz)) {
				current = (T) DataTransfer.verboseGet(rs, column_names.get(0),
						clazz);
			} else if (Map.class.isAssignableFrom(clazz)) {
				if (!clazz.isInterface()) {
					current = clazz.newInstance();
				} else
					current = (T) new HashMap();
				DataTransfer.fillMap((Map) current, rs, column_names);
			} else {
				current = clazz.newInstance();
				DataTransfer.fillBean(current, rs, column_names);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		try {
			boolean next = rs.next();
			if (next) {
				createCurrent();
			}
			return next;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public T next() {
		return current;
	}

	@Override
	public void remove() {
		throw new RuntimeException("remove is not supported.");
	}

	@Override
	public Iterator<T> iterator() {
		return this;
	}

	public void close() throws SQLException {
		rs.close();
	}
}
