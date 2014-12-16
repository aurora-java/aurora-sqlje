package aurora.sqlje.test;

import aurora.sqlje.core.annotation.Column;
import aurora.sqlje.core.annotation.Table;
import aurora.sqlje.core.annotation.InsertExpression;
import aurora.sqlje.core.annotation.PK;

@Table(name = "animals", stdwho = true)
public class Animal {
	@PK
	public Long id;
	@Column
	public String name;
	@Column
	@InsertExpression("${name}")
	public String description;
	@Column
	@InsertExpression("${/session/@user_id}")
	public String attr1;
	
	@Column
	@InsertExpression("concat(${name},${/session/@user_id})")
	public String attr2;
}
