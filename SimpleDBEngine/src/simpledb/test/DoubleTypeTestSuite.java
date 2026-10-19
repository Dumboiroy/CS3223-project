package simpledb.test;

import java.io.File;
import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/**
 * Self-contained test suite for the double data type: create/insert/read-back,
 * precision, non-equality and int-vs-double predicates, ORDER BY, and a btree
 * index on a double column. Handles its own setup and teardown.
 */
public class DoubleTypeTestSuite {
   private static final String DBNAME = "doubletypetestdb";

   public static void main(String[] args) {
      deleteDB(DBNAME);
      boolean allPassed = true;
      try {
         SimpleDB db = new SimpleDB(DBNAME);
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         System.out.println("[1/3] SETUP: creating product table with a double column...");
         planner.executeUpdate(
            "create table product(id int, pname varchar(12), price double)", tx);
         planner.executeUpdate("create index idx_price on product(price) using btree", tx);

         // id | pname  | price
         // 1  | apple  | 3.00
         // 2  | banana | 1.50
         // 3  | cherry | 2.75
         // 4  | date   | 10.25
         // 5  | elder  | 2.50
         planner.executeUpdate("insert into product(id, pname, price) values (1, 'apple', 3.0)", tx);
         planner.executeUpdate("insert into product(id, pname, price) values (2, 'banana', 1.5)", tx);
         planner.executeUpdate("insert into product(id, pname, price) values (3, 'cherry', 2.75)", tx);
         planner.executeUpdate("insert into product(id, pname, price) values (4, 'date', 10.25)", tx);
         planner.executeUpdate("insert into product(id, pname, price) values (5, 'elder', 2.5)", tx);

         System.out.println("[2/3] RUN: executing double-type queries...\n");

         // Test 1: read back a double value with full precision
         allPassed &= checkSingleDouble(planner, tx,
            "select price from product where id = 3", "price", 2.75, "read back double precisely");

         // Test 2: non-equality predicate on double (price > 2.5 -> 3.0, 2.75, 10.25)
         allPassed &= checkCount(planner, tx,
            "select id from product where price > 2.5", 3, "price > 2.5");

         // Test 3: int literal compared against double column (price = 3 matches 3.0)
         allPassed &= checkCount(planner, tx,
            "select id from product where price = 3", 1, "price = 3 (int literal vs double)");

         // Test 4: ORDER BY double ascending -> first price should be 1.5
         allPassed &= checkFirstDouble(planner, tx,
            "select price from product order by price asc", "price", 1.5,
            "order by price asc (smallest first)");

         // Test 5: ORDER BY double descending -> first price should be 10.25
         allPassed &= checkFirstDouble(planner, tx,
            "select price from product order by price desc", "price", 10.25,
            "order by price desc (largest first)");

         // Test 6: btree index equality select on double column
         allPassed &= checkCount(planner, tx,
            "select id from product where price = 10.25", 1, "btree index select on double");

         tx.commit();

         System.out.println("\n=== OVERALL: " + (allPassed ? "ALL TESTS PASSED" : "SOME TESTS FAILED") + " ===");
      } catch (Exception e) {
         e.printStackTrace();
         allPassed = false;
      } finally {
         System.out.println("\n[3/3] TEARDOWN: cleaning up test database...");
         deleteDB(DBNAME);
         System.out.println(allPassed
            ? "======================================\n✓ Test suite completed successfully!\n======================================"
            : "======================================\n✗ Test suite FAILED\n======================================");
      }
   }

   private static boolean checkCount(Planner planner, Transaction tx, String qry,
         int expected, String label) throws Exception {
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      int count = 0;
      while (s.next())
         count++;
      s.close();
      boolean pass = count == expected;
      System.out.printf("  %-45s rows=%d (expected %d) -> %s%n", label, count, expected, pass ? "PASS" : "FAIL");
      return pass;
   }

   private static boolean checkSingleDouble(Planner planner, Transaction tx, String qry,
         String fld, double expected, String label) throws Exception {
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      boolean pass = false;
      if (s.next()) {
         double v = s.getVal(fld).asDouble();
         pass = v == expected;
         System.out.printf("  %-45s value=%s (expected %s) -> %s%n", label, v, expected, pass ? "PASS" : "FAIL");
      } else {
         System.out.printf("  %-45s -> FAIL (no rows)%n", label);
      }
      s.close();
      return pass;
   }

   private static boolean checkFirstDouble(Planner planner, Transaction tx, String qry,
         String fld, double expected, String label) throws Exception {
      return checkSingleDouble(planner, tx, qry, fld, expected, label);
   }

   private static void deleteDB(String name) {
      File dir = new File(name);
      if (dir.exists()) {
         for (File f : dir.listFiles())
            f.delete();
         dir.delete();
      }
   }
}
