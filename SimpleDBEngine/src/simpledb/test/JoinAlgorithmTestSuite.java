package simpledb.test;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/* Tests that all three join algorithms (index join, merge join,
 * nested-loops join) produce correct results.
 *
 * 1. STUDENT join DEPT on majorid = did: STUDENT.MajorId has a hash
 *    index, so this should be answered with an index join.
 * 2. COURSE join DEPT on deptid = did: neither side is indexed, so
 *    this should be answered with a merge join.
 * 3. STUDENT join ENROLL on sid != studentid: a non-equality
 *    predicate, which only a nested-loops join can evaluate.
 *
 * Each query's row count is checked against the value computed by
 * hand from the fixed data inserted by CreateStudentDB.
 */

public class JoinAlgorithmTestSuite {
   public static void main(String[] args) {
      try {
         SimpleDB db = new SimpleDB("studentdb");
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         boolean allPassed = true;

         System.out.println("=== Test 1: index join (STUDENT.MajorId = DEPT.DId) ===");
         String q1 = "select sname, dname from student, dept where majorid = did";
         allPassed &= runAndCheck(planner, tx, q1, 9);

         System.out.println("\n=== Test 2: merge join (COURSE.DeptId = DEPT.DId) ===");
         String q2 = "select title, dname from course, dept where deptid = did";
         allPassed &= runAndCheck(planner, tx, q2, 6);

         System.out.println("\n=== Test 3: nested-loops join (STUDENT.SId != ENROLL.StudentId) ===");
         String q3 = "select sname, grade from student, enroll where sid != studentid";
         allPassed &= runAndCheck(planner, tx, q3, 48);

         System.out.println("\n=== OVERALL: " + (allPassed ? "ALL TESTS PASSED" : "SOME TESTS FAILED") + " ===");

         tx.commit();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static boolean runAndCheck(Planner planner, Transaction tx, String qry, int expectedRows)
         throws Exception {
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      int count = 0;
      while (s.next()) {
         count++;
      }
      s.close();
      boolean pass = count == expectedRows;
      System.out.println("Rows returned: " + count + " (expected " + expectedRows + ") -> "
            + (pass ? "PASS" : "FAIL"));
      return pass;
   }
}
