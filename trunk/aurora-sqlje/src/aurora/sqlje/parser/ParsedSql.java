package aurora.sqlje.parser;

import java.util.ArrayList;

public class ParsedSql {
	ArrayList<Parameter> bindParaList = new ArrayList<Parameter>();
	ArrayList<Parameter> fragParaList = new ArrayList<Parameter>();
	ArrayList<String> fragments = new ArrayList<String>();

	public ParsedSql() {
		super();
	}

	public void addPara(Parameter p) {
		if (p.getType() == Parameter.NONE)
			fragParaList.add(p);
		else
			bindParaList.add(p);
	}

	public void addFragment(String f) {
		fragments.add(f);
	}

	public ArrayList<String> getFragments() {
		return fragments;
	}

	public String getFirstFragment() {
		return fragments.get(0);
	}

	public ArrayList<Parameter> getBindParameters() {
		return bindParaList;
	}

	public ArrayList<Parameter> getDynamicParameters() {
		return fragParaList;
	}

	public boolean hasOutputParameter() {
		for (Parameter p : bindParaList)
			if (p.getType() == Parameter.OUT)
				return true;
		return false;
	}

	public boolean isDynamic() {
		return fragParaList.size() > 0;
	}

	public String toStringLiteral() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fragments.size(); i++) {
			String s = fragments.get(i);
			sb.append(s);
			if (i < fragParaList.size()) {
				sb.append("\"+(").append(fragParaList.get(i).getExpression())
						.append(")+\"");
			}
		}
		return sb.toString();
	}

	public String toEscapedStringLiteral() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fragments.size(); i++) {
			String s = fragments.get(i);
			for (int j = 0; j < s.length(); j++) {
				char c = s.charAt(j);
				if (c == '\n')
					sb.append("\\n");
				else if (c == '\r')
					sb.append("\\r");
				else if (c == '\t')
					sb.append("\\t");
				else if (c == '\\')
					sb.append("\\\\");
				else if (c == '"')
					sb.append("\\\"");
				else
					sb.append(c);
			}
			if (i < fragParaList.size()) {
				sb.append("\"+(").append(fragParaList.get(i).getExpression())
						.append(")+\"");
			}
		}
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fragments.size(); i++) {
			sb.append(fragments.get(i));
			if (i < fragments.size() - 1)
				sb.append("!");
		}
		return sb.toString();
	}

	public void printParas() {
		System.out.println("----BindParams----");
		for (Parameter p : bindParaList) {
			System.out.println(p);
		}
		System.out.println("----FragParams----");
		for (Parameter p : fragParaList) {
			System.out.println(p);
		}
	}
}
