package simpledb.materialize;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.plan.Plan;
import simpledb.plan.TablePlan;
import simpledb.query.Scan;

// Directly exercises MergeJoinPlan/MergeJoinScan (bypassing the
// query optimizer), joining STUDENT and DEPT on majorid = did.

public class MergeJoinTest {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      MetadataMgr mdm = db.mdMgr();
      Transaction tx = db.newTx();

      Plan studentplan = new TablePlan(tx, "student", mdm);
      Plan deptplan = new TablePlan(tx, "dept", mdm);

      Plan mergejoin = new MergeJoinPlan(tx, studentplan, deptplan, "majorid", "did");
      Scan s = mergejoin.open();

      int count = 0;
      while (s.next()) {
         System.out.println(s.getString("sname") + "\t" + s.getString("dname"));
         count++;
      }
      s.close();
      System.out.println("Rows returned: " + count + " (expected 9)");

      tx.commit();
   }
}
