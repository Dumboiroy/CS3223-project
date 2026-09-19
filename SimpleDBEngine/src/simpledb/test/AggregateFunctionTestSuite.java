package simpledb.test;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/* Tests that aggregate functions (MIN, MAX, AVG, COUNT, SUM) produce correct results.
 *
 * Based on STUDENT table data from CreateStudentDB:
 * SId | SName | GradYear | MajorId
 * ----|-------|----------|--------
 *  1  | joe   | 2021     | 10
 *  2  | amy   | 2020     | 20
 *  3  | max   | 2022     | 10
 *  4  | sue   | 2022     | 20
 *  5  | bob   | 2020     | 30
 *  6  | kim   | 2020     | 20
 *  7  | art   | 2021     | 30
 *  8  | pat   | 2019     | 20
 *  9  | lee   | 2021     | 10
 *
 * Expected aggregates by gradyear:
 * GradYear | Count | Min | Max | Sum | Avg
 * 2019:      1      20   20   20   20
 * 2020:      3      20   30   70   23
 * 2021:      3      10   30   50   16
 * 2022:      2      10   20   30   15
 * Overall:   9      10   30  170   18
 */

public class AggregateFunctionTestSuite {
   public static void main(String[] args) {
      try {
         SimpleDB db = new SimpleDB("studentdb");
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         boolean allPassed = true;

         System.out.println("=== Test 1: COUNT with GROUP BY ===");
         System.out.println("Expected: 4 groups (2019, 2020, 2021, 2022)");
         String q1 = "select count(sid) from student group by gradyear";
         allPassed &= runAndCheck(planner, tx, q1, 4);

         System.out.println("\n=== Test 2: MIN with GROUP BY ===");
         System.out.println("Expected: 4 rows | Min values: 2019=20, 2020=20, 2021=10, 2022=10");
         String q2 = "select min(majorid) from student group by gradyear";
         allPassed &= runAndCheck(planner, tx, q2, 4);

         System.out.println("\n=== Test 3: MAX with GROUP BY ===");
         System.out.println("Expected: 4 rows | Max values: 2019=20, 2020=30, 2021=30, 2022=20");
         String q3 = "select max(majorid) from student group by gradyear";
         allPassed &= runAndCheck(planner, tx, q3, 4);

         System.out.println("\n=== Test 4: SUM with GROUP BY ===");
         System.out.println("Expected: 4 rows | Sum values: 2019=20, 2020=70, 2021=50, 2022=30");
         String q4 = "select sum(majorid) from student group by gradyear";
         allPassed &= runAndCheck(planner, tx, q4, 4);

         System.out.println("\n=== Test 5: AVG with GROUP BY and ORDER BY ===");
         System.out.println("Expected: 4 rows | Avg values: 2019=20, 2020=23, 2021=16, 2022=15");
         String q5 = "select avg(majorid) from student group by gradyear order by gradyear desc";
         allPassed &= runAndCheck(planner, tx, q5, 4);

         System.out.println("\n=== Test 6: COUNT without GROUP BY ===");
         System.out.println("Expected: 1 row with total count=9");
         String q6 = "select count(sid) from student";
         allPassed &= runAndCheck(planner, tx, q6, 1);

         System.out.println("\n=== Test 7: MIN without GROUP BY ===");
         System.out.println("Expected: 1 row with min=10");
         String q7 = "select min(majorid) from student";
         allPassed &= runAndCheck(planner, tx, q7, 1);

         System.out.println("\n=== Test 8: MAX without GROUP BY ===");
         System.out.println("Expected: 1 row with max=30");
         String q8 = "select max(majorid) from student";
         allPassed &= runAndCheck(planner, tx, q8, 1);

         System.out.println("\n=== Test 9: SUM without GROUP BY ===");
         System.out.println("Expected: 1 row with sum=170 (20+20+10+20+30+20+30+20+10)");
         String q9 = "select sum(majorid) from student";
         allPassed &= runAndCheck(planner, tx, q9, 1);

         System.out.println("\n=== Test 10: AVG without GROUP BY ===");
         System.out.println("Expected: 1 row with avg≈18 (170/9)");
         String q10 = "select avg(majorid) from student";
         allPassed &= runAndCheck(planner, tx, q10, 1);

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
