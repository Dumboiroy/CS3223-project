package simpledb.plan;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class OrderByTest {

   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      try {
         testMixedOrdering(planner, tx);
         testDefaultAscending(planner, tx);
         testDescending(planner, tx);
         testWithoutOrderBy(planner, tx);
         testEmptyResult(planner, tx);

         tx.commit();
         System.out.println("\nAll ORDER BY tests completed.");
      }
      catch (RuntimeException e) {
         tx.rollback();
         throw e;
      }
   }

   /**
    * Tests ascending GradYear followed by descending SName.
    */
   private static void testMixedOrdering(
         Planner planner,
         Transaction tx) {

      String query =
            "select sid, sname, gradyear " +
            "from student " +
            "order by gradyear asc, sname desc";

      printQueryResult(
            "TEST 1: Mixed ascending and descending ordering",
            query,
            planner,
            tx);
   }

   /**
    * Tests that ASC is used when no direction is written.
    */
   private static void testDefaultAscending(
         Planner planner,
         Transaction tx) {

      String query =
            "select sid, sname, gradyear " +
            "from student " +
            "order by gradyear, sname desc";

      printQueryResult(
            "TEST 2: Default ascending ordering",
            query,
            planner,
            tx);
   }

   /**
    * Tests descending GradYear followed by ascending SName.
    */
   private static void testDescending(
         Planner planner,
         Transaction tx) {

      String query =
            "select sid, sname, gradyear " +
            "from student " +
            "order by gradyear desc, sname asc";

      printQueryResult(
            "TEST 3: Descending primary field",
            query,
            planner,
            tx);
   }

   /**
    * Checks that an ordinary query still works without ORDER BY.
    */
   private static void testWithoutOrderBy(
         Planner planner,
         Transaction tx) {

      String query =
            "select sid, sname, gradyear " +
            "from student";

      printQueryResult(
            "TEST 4: Query without ORDER BY",
            query,
            planner,
            tx);
   }

   /**
    * Checks that sorting an empty result does not throw an error.
    */
   private static void testEmptyResult(
         Planner planner,
         Transaction tx) {

      String query =
            "select sid, sname, gradyear " +
            "from student " +
            "where sid = 999 " +
            "order by gradyear";

      printQueryResult(
            "TEST 5: Empty query result",
            query,
            planner,
            tx);
   }

   /**
    * Creates a plan, opens its scan, and prints the records.
    */
   private static void printQueryResult(
         String testName,
         String query,
         Planner planner,
         Transaction tx) {

      System.out.println();
      System.out.println("========================================");
      System.out.println(testName);
      System.out.println("========================================");
      System.out.println("SQL: " + query);
      System.out.println();
      System.out.printf("%-5s %-12s %-10s%n",
            "SId", "SName", "GradYear");
      System.out.println("-----------------------------");

      Plan plan = planner.createQueryPlan(query, tx);
      Scan scan = plan.open();
      int recordCount = 0;

      try {
         while (scan.next()) {
            int sid = scan.getInt("sid");
            String sname = scan.getString("sname");
            int gradyear = scan.getInt("gradyear");

            System.out.printf(
                  "%-5d %-12s %-10d%n",
                  sid,
                  sname,
                  gradyear);

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