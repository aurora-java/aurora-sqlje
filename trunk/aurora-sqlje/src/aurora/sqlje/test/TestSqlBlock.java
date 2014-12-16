package aurora.sqlje.test;

import aurora.sqlje.parser.ParsedSql;
import aurora.sqlje.parser.SqlBlock;

public class TestSqlBlock {

	public static void main(String[] args) throws Exception {
		SqlBlock block = new SqlBlock();
		block.setSql("select * from ${!sys_user} where name=${name}");
		ParsedSql psql = block.getParsedSql();
		System.out.println(psql);
		System.out.println(psql.toEscapedStringLiteral());
		System.out.println(psql.toStringLiteral());
	}

}
