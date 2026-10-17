package simpledb.test;

import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Tests ORDER BY on aggregate functions combined with GROUP BY.
 *
 * Based on STUDENT table data from CreateStudentDB, grouped by GradYear:
 * GradYear | Count(SId) | Sum(MajorId) | Avg(MajorId) | Max(MajorId) | Min(MajorId)
 * ---------|------------|--------------|--------------|--------------|-------------
 * 2019     |     1      |      20      |      20      |      20      |     20
 * 2020     |     3      |      70      |      23      |      30      |     20
 * 2021     |     3      |      50      |      16      |      30      |     10
 * 2022     |     2      |      30      |      15      |      20      |     10
 */
public class GroupByOrderByTest {

   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      try {
         testOrderByCountDescending(planner, tx);
         testOrderByCountDefaultAscending(planner, tx);
         testOrderBySumAscending(planner, tx);
         testOrderByAvgDescending(planner, tx);
         testOrderByMultipleAggregates(planner, tx);
         testOrderByAggregateWithoutGroupBy(planner, tx);
         testOrderByAggregateEmptyResult(planner, tx);

         tx.commit();
         System.out.println("\nAll GROUP BY + ORDER BY tests completed.");
      }
      catch (RuntimeException e) {
         tx.rollback();
         throw e;
      }
   }

   /**
    * Tests GROUP BY GradYear ordered by descending COUNT(SId).
    * Groups 2020 and 2021 tie on count, so their relative order is not checked.
    */
   private static void testOrderByCountDescending(
         Planner planner,
         Transaction tx) {

      String query =
            "select gradyear, count(sid) " +
            "from student " +
            "group by gradyear " +
            "order by countofsid desc";

      printQueryResult(
            "TEST 1: ORDER BY COUNT descending",
            query,
            planner,
            tx,
            new String[]{"gradyear", "countofsid"});
   }

   /**
    * Tests ORDER BY COUNT(SId) with no direction (ASC by default),
    * using GradYear descending as the tiebreaker.
    */
   private static void testOrderByCountDefaultAscending(
         Planner planner,
         Transaction tx) {

      String query =
            "select gradyear, count(sid) " +
            "from student " +
            "group by gradyear " +
            "order by countofsid, gradyear desc";

      printQueryResult(
            "TEST 2: Default ascending ORDER BY on COUNT with tiebreaker",
            query,
            planner,
            tx,
            new String[]{"gradyear", "countofsid"});
   }

   /**
    * Tests GROUP BY GradYear ordered by ascending SUM(MajorId).
    */
   private static void testOrderBySumAscending(
         Planner planner,
         Transaction tx) {

      String query =
            "select gradyear, sum(majorid) " +
            "from student " +
            "group by gradyear " +
            "order by sumofmajorid asc";

      printQueryResult(
            "TEST 3: ORDER BY SUM ascending",
            query,
            planner,
            tx,
            new String[]{"gradyear", "sumofmajorid"});
   }

   /**
    * Tests GROUP BY GradYear ordered by descending AVG(MajorId).
    */
   private static void testOrderByAvgDescending(
         Planner planner,
         Transaction tx) {

      String query =
            "select gradyear, avg(majorid) " +
            "from student " +
            "group by gradyear " +
            "order by avgofmajorid desc";

      printQueryResult(
            "TEST 4: ORDER BY AVG descending",
            query,
            planner,
            tx,
            new String[]{"gradyear", "avgofmajorid"});
   }

   /**
    * Tests ORDER BY over multiple aggregates with mixed directions:
    * MAX(MajorId) descending, then MIN(MajorId) ascending.
    */
   private static void testOrderByMultipleAggregates(
         Planner planner,
         Transaction tx) {

      String query =
            "select gradyear, max(majorid), min(majorid) " +
            "from student " +
            "group by gradyear " +
            "order by maxofmajorid desc, minofmajorid asc";

      printQueryResult(
            "TEST 5: ORDER BY multiple aggregates",
            query,
            planner,
            tx,
            new String[]{"gradyear", "maxofmajorid", "minofmajorid"});
   }

   /**
    * Tests ORDER BY on an aggregate with no GROUP BY (single output row).
    */
   private static void testOrderByAggregateWithoutGroupBy(
         Planner planner,
         Transaction tx) {

      String query =
            "select count(sid) " +
            "from student " +
            "order by countofsid";

      printQueryResult(
            "TEST 6: ORDER BY aggregate without GROUP BY",
            query,
            planner,
            tx,
            new String[]{"countofsid"});
   }

   /**
    * Checks that ordering an empty grouped result by an aggregate does not throw an error.
    */
   private static void testOrderByAggregateEmptyResult(
         Planner planner,
         Transaction tx) {

      String query =
            "select gradyear, count(sid) " +
            "from student " +
            "where sid = 999 " +
            "group by gradyear " +
            "order by countofsid";

      printQueryResult(
            "TEST 7: Empty query result with ORDER BY aggregate",
            query,
            planner,
            tx,
            new String[]{"gradyear", "countofsid"});
   }

   /**
    * Creates a plan, opens its scan, and prints the requested columns of each record.
    */
   private static void printQueryResult(
         String testName,
         String query,
         Planner planner,
         Transaction tx,
         String[] columns) {

      System.out.println();
      System.out.println("========================================");
      System.out.println(testName);
      System.out.println("========================================");
      System.out.println("SQL: " + query);
      System.out.println();

      StringBuilder header = new StringBuilder();
      StringBuilder divider = new StringBuilder();
      for (String column : columns) {
         header.append(String.format("%-16s", column));
         divider.append("----------------");
      }
      System.out.println(header.toString().trim());
      System.out.println(divider.toString());

      Plan plan = planner.createQueryPlan(query, tx);
      Scan scan = plan.open();
      int recordCount = 0;

      try {
         while (scan.next()) {
            StringBuilder row = new StringBuilder();
            for (String column : columns) {
               row.append(String.format("%-16s", scan.getVal(column).toString()));
            }
            System.out.println(row.toString().trim());

            recordCount++;
         }
      }
      finally {
         scan.close();
      }

      if (recordCount == 0)
         System.out.println("(no records)");

      System.out.println();
      System.out.println(
            "Number of records: " + recordCount);
   }
}
