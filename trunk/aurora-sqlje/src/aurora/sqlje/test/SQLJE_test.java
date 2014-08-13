package aurora.sqlje.test;
import uncertain.composite.CompositeMap;
import java.sql.*;
import aurora.sqlje.exception.*;
import java.util.Map;
import uncertain.composite.*;
import aurora.sqlje.core.*;
public class SQLJE_test implements aurora.sqlje.core.ISqlCallEnabled {
  public void hello(  CompositeMap param) throws Exception {
    PreparedStatement _$sqlje_ps_gen2=getSqlCallStack().getCurrentConnection().prepareStatement("select * from sys_user");
    _$sqlje_ps_gen2.execute();
    UPDATE_COUNT=_$sqlje_ps_gen2.getUpdateCount();
    ResultSet _$sqlje_rs_gen0=_$sqlje_ps_gen2.getResultSet();
    getSqlCallStack().push(_$sqlje_rs_gen0);
    getSqlCallStack().push(_$sqlje_ps_gen2);
    for (    CompositeMap m : new ResultSetIterator<CompositeMap>(_$sqlje_rs_gen0,CompositeMap.class)) {
      System.out.println("Hello " + m.getString("user_name") + " "+ m.getInt("user_id"));
      param.createChild("record").put("user_name",m.get("user_name"));
    }
  }
  @Autonomous public int aaa() throws Exception {
    Connection _$autonomous_connection_=getSqlCallStack().createConnection();
    Exception _$autonomous_exception_=null;
    try {
      try {
        PreparedStatement _$sqlje_ps_gen3=getSqlCallStack().getCurrentConnection().prepareStatement("select sysdate from dual");
        _$sqlje_ps_gen3.execute();
        UPDATE_COUNT=_$sqlje_ps_gen3.getUpdateCount();
        ResultSet _$sqlje_rs_gen1=_$sqlje_ps_gen3.getResultSet();
        getSqlCallStack().push(_$sqlje_rs_gen1);
        getSqlCallStack().push(_$sqlje_ps_gen3);
      }
 catch (      Exception e) {
        throw e;
      }
      return 1;
    }
 catch (    Exception e) {
      _$autonomous_exception_=e;
      throw e;
    }
 finally {
      if (_$autonomous_exception_ == null)       _$autonomous_connection_.commit();
 else       _$autonomous_connection_.rollback();
      getSqlCallStack().free(_$autonomous_connection_);
    }
  }
  aurora.sqlje.core.ISqlCallStack _$sqlje_sqlCallStack=null;
  java.lang.Integer UPDATE_COUNT=null;
  aurora.sqlje.core.IInstanceManager _$sqlje_instanceManager=null;
  public void _$setInstanceManager(  aurora.sqlje.core.IInstanceManager args0){
    _$sqlje_instanceManager=args0;
  }
  public aurora.sqlje.core.IInstanceManager _$getInstanceManager(){
    return _$sqlje_instanceManager;
  }
  public void _$setSqlCallStack(  aurora.sqlje.core.ISqlCallStack args0){
    _$sqlje_sqlCallStack=args0;
  }
  public aurora.sqlje.core.ISqlCallStack getSqlCallStack(){
    return _$sqlje_sqlCallStack;
  }
}
