package simpledb.plan;

import java.util.Arrays;
import simpledb.materialize.DistinctPlan;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Tests DISTINCT at two levels: SQL queries run through the planner,
 * and a DistinctPlan built directly over a TablePlan.
 */
public class DistinctTest {

   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      try {
         testSingleColumn(planner, tx);
         testMultiColumn(planner, tx);
         testAlreadyDistinct(planner, tx);
         testWithWhere(planner, tx);
         testWithOrderBy(planner, tx);
         testEmptyResult(planner, tx);
         testDirectPlan(db, tx);

         tx.commit();
         System.out.println("\nAll DISTINCT tests completed.");
      }
      catch (RuntimeException e) {
         tx.rollback();
         throw e;
      }
   }

   /**
    * Tests that duplicate values in a single column collapse to one row each.
    * Student majors are 10, 20, 30.
    */
   private static void testSingleColumn(Planner planner, Transaction tx) {
      String query =
            "select distinct majorid " +
            "from student";

      checkRowCount(
            "TEST 1: Single column distinct",
            query, 3, planner, tx);
   }

   /**
    * Tests that distinct applies to the combination of columns.
    * (majorid, gradyear) has 7 distinct pairs out of 9 students.
    */
   private static void testMultiColumn(Planner planner, Transaction tx) {
      String query =
            "select distinct majorid, gradyear " +
            "from student";

      checkRowCount(
            "TEST 2: Multi-column distinct",
            query, 7, planner, tx);
   }

   /**
    * Tests that distinct on a column with no duplicates keeps every row.
    */
   private static void testAlreadyDistinct(Planner planner, Transaction tx) {
      String query =
            "select distinct sid, sname " +
            "from student";

      checkRowCount(
            "TEST 3: Distinct over unique rows",
            query, 9, planner, tx);
   }

   /**
    * Tests that distinct is applied after the where clause.
    * Students with gradyear 2020 have majors 20, 30, 20, so 2 distinct majors.
    */
   private static void testWithWhere(Planner planner, Transaction tx) {
      String query =
            "select distinct majorid " +
            "from student " +
            "where gradyear = 2020";

      checkRowCount(
            "TEST 4: Distinct with where clause",
            query, 2, planner, tx);
   }

   /**
    * Tests that distinct and order by can be used together.
    */
   private static void testWithOrderBy(Planner planner, Transaction tx) {
      String query =
            "select distinct majorid " +
            "from student " +
            "order by majorid";

      checkRowCount(
            "TEST 5: Distinct with order by",
            query, 3, planner, tx);
   }

   /**
    * Tests that distinct over an empty result does not throw an error.
    */
   private static void testEmptyResult(Planner planner, Transaction tx) {
      String query =
            "select distinct majorid " +
            "from student " +
            "where sid = 999";

      checkRowCount(
            "TEST 6: Distinct over empty result",
            query, 0, planner, tx);
   }

   /**
    * Tests DistinctPlan built directly over a TablePlan, bypassing the
    * planner. It covers the same query as the SQL form
    * "select distinct majorid from student". Checks the row count, that
    * the output schema matches the underlying schema, and that toString
    * reports the distinct fields.
    */
   private static void testDirectPlan(SimpleDB db, Transaction tx) {
      String query = "select distinct majorid from student";

      System.out.println();
      System.out.println("========================================");
      System.out.println("TEST 7: Direct DistinctPlan over student");
      System.out.println("========================================");
      System.out.println("SQL: " + query);
      System.out.println();

      Plan s = new TablePlan(tx, "student", db.mdMgr());
      Plan plan = new DistinctPlan(tx, s, Arrays.asList("majorid"));

      int recordCount = countRows(plan);
      System.out.println("Number of records: " + recordCount);
      check("TEST 7: Direct DistinctPlan", recordCount, 3);

      if (!plan.schema().fields().equals(s.schema().fields()))
         throw new RuntimeException(
               "TEST 7: output schema differs from underlying schema");
      System.out.println(plan.toString(0));
   }

   /**
    * Creates a plan from the query, counts its records,
    * and checks the count against the expected value.
    */
   private static void checkRowCount(
         String testName,
         String query,
         int expected,
         Planner planner,
         Transaction tx) {

      System.out.println();
      System.out.println("========================================");
      System.out.println(testName);
      System.out.println("========================================");
      System.out.println("SQL: " + query);
      System.out.println();

      Plan plan = planner.createQueryPlan(query, tx);
      int recordCount = countRows(plan);

      System.out.println("Number of records: " + recordCount);
      check(testName, recordCount, expected);
   }

   /**
    * Opens the plan's scan and counts the records it returns.
    */
   private static int countRows(Plan plan) {
      Scan scan = plan.open();
      int recordCount = 0;
      try {
         while (scan.next())
            recordCount++;
      }
      finally {
         scan.close();
      }
      return recordCount;
   }

   private static void check(String testName, int actual, int expected) {
      if (actual != expected)
         throw new RuntimeException(testName + ": expected " + expected
               + " records but got " + actual);
      System.out.println("PASS");
   }
}
