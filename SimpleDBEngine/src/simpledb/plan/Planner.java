package simpledb.plan;

import java.util.List;
import simpledb.tx.Transaction;
import simpledb.parse.*;
import simpledb.materialize.SetOperationPlan;

/**
 * The object that executes SQL statements.
 * @author Edward Sciore
 */
public class Planner {
   private QueryPlanner qplanner;
   private UpdatePlanner uplanner;
   
   public Planner(QueryPlanner qplanner, UpdatePlanner uplanner) {
      this.qplanner = qplanner;
      this.uplanner = uplanner;
   }
   
   /**
    * Creates a plan for an SQL select statement, using the supplied planner.
    * @param qry the SQL query string
    * @param tx the transaction
    * @return the scan corresponding to the query plan
    */
   public Plan createQueryPlan(String qry, Transaction tx) {
      Parser parser = new Parser(qry);
      SetQueryData setdata = parser.setQuery();

      List<QueryData> queries = setdata.queries();
      verifyQuery(queries.get(0));
      Plan p = qplanner.createPlan(queries.get(0), tx);

      // fold in each set operation left to right
      List<String> ops = setdata.operators();
      for (int i = 0; i < ops.size(); i++) {
         verifyQuery(queries.get(i + 1));
         Plan right = qplanner.createPlan(queries.get(i + 1), tx);
         p = new SetOperationPlan(tx, p, right, ops.get(i));
      }
      return p;
   }
   
   /**
    * Executes an SQL insert, delete, modify, or
    * create statement.
    * The method dispatches to the appropriate method of the
    * supplied update planner,
    * depending on what the parser returns.
    * @param cmd the SQL update string
    * @param tx the transaction
    * @return an integer denoting the number of affected records
    */
   public int executeUpdate(String cmd, Transaction tx) {
      Parser parser = new Parser(cmd);
      Object data = parser.updateCmd();
      verifyUpdate(data);
      if (data instanceof InsertData)
         return uplanner.executeInsert((InsertData)data, tx);
      else if (data instanceof DeleteData)
         return uplanner.executeDelete((DeleteData)data, tx);
      else if (data instanceof ModifyData)
         return uplanner.executeModify((ModifyData)data, tx);
      else if (data instanceof CreateTableData)
         return uplanner.executeCreateTable((CreateTableData)data, tx);
      else if (data instanceof CreateViewData)
         return uplanner.executeCreateView((CreateViewData)data, tx);
      else if (data instanceof CreateIndexData)
         return uplanner.executeCreateIndex((CreateIndexData)data, tx);
      else
         return 0;
   }
 
   // SimpleDB does not verify queries, although it should.
   private void verifyQuery(QueryData data) {
   }

   // SimpleDB does not verify updates, although it should.
   private void verifyUpdate(Object data) {
   }
}
