package simpledb.query;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.plan.Plan;
import simpledb.plan.TablePlan;
import simpledb.record.Schema;

// Directly exercises NestedLoopJoinPlan/NestedLoopJoinScan
// (bypassing the query optimizer) on a non-equality join
// predicate that only a nested-loops join can evaluate:
// STUDENT.SId != ENROLL.StudentId.

public class NestedLoopJoinTest {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      MetadataMgr mdm = db.mdMgr();
      Transaction tx = db.newTx();

      Plan studentplan = new TablePlan(tx, "student", mdm);
      Plan enrollplan = new TablePlan(tx, "enroll", mdm);

      Term t = new Term(new Expression("sid"), "!=", new Expression("studentid"));
      Predicate joinpred = new Predicate(t);

      Plan nestedloop = new simpledb.plan.NestedLoopJoinPlan(studentplan, enrollplan, joinpred);
      Scan s = nestedloop.open();

      int count = 0;
      while (s.next()) {
         count++;
      }
      s.close();
      System.out.println("Rows returned: " + count + " (expected 48)");

      tx.commit();
   }
}
