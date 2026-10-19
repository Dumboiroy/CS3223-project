package simpledb.test;

import java.io.File;
import java.util.*;
import simpledb.tx.Transaction;
import simpledb.plan.*;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/**
 * Tests UNION, INTERSECT and EXCEPT with set (duplicate-eliminating) semantics,
 * including union-compatibility by position across differently-named columns,
 * chaining, and self-union dedup.
 */
public class SetOperationTestSuite {
   private static final String DBNAME = "setoptestdb";

   public static void main(String[] args) {
      deleteDB(DBNAME);
      boolean ok = true;
      try {
         SimpleDB db = new SimpleDB(DBNAME);
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         planner.executeUpdate("create table a(x int)", tx);
         planner.executeUpdate("create table b(y int)", tx);
         for (int v : new int[]{1, 2, 3, 3, 4})       // a = {1,2,3,4} as a set
            planner.executeUpdate("insert into a(x) values (" + v + ")", tx);
         for (int v : new int[]{3, 4, 5, 6})          // b = {3,4,5,6}
            planner.executeUpdate("insert into b(y) values (" + v + ")", tx);

         // UNION (distinct): {1,2,3,4} ∪ {3,4,5,6} = {1,2,3,4,5,6}
         ok &= expect(planner, tx, "select x from a union select y from b",
               new int[]{1, 2, 3, 4, 5, 6}, "x", "union");

         // INTERSECT: {1,2,3,4} ∩ {3,4,5,6} = {3,4}
         ok &= expect(planner, tx, "select x from a intersect select y from b",
               new int[]{3, 4}, "x", "intersect");

         // EXCEPT: {1,2,3,4} - {3,4,5,6} = {1,2}
         ok &= expect(planner, tx, "select x from a except select y from b",
               new int[]{1, 2}, "x", "except");

         // self-union dedups to the set of a: {1,2,3,4}
         ok &= expect(planner, tx, "select x from a union select x from a",
               new int[]{1, 2, 3, 4}, "x", "self-union dedup");

         // chaining: (a ∪ b) ∩ a = {1,2,3,4}
         ok &= expect(planner, tx,
               "select x from a union select y from b intersect select x from a",
               new int[]{1, 2, 3, 4}, "x", "chained union/intersect");

         // union-compatibility error: different column counts
         boolean threw = false;
         try {
            planner.executeUpdate("create table c(p int, q int)", tx);
            Plan bad = planner.createQueryPlan("select x from a union select p, q from c", tx);
            bad.open();
         } catch (RuntimeException e) {
            threw = true;
         }
         System.out.printf("  %-30s -> %s%n", "incompatible arity rejected", threw ? "PASS" : "FAIL");
         ok &= threw;

         tx.commit();
         System.out.println("\n=== OVERALL: " + (ok ? "ALL TESTS PASSED" : "SOME TESTS FAILED") + " ===");
      } catch (Exception e) {
         e.printStackTrace();
         ok = false;
      } finally {
         deleteDB(DBNAME);
         System.out.println(ok
            ? "======================================\n✓ Test suite completed successfully!\n======================================"
            : "======================================\n✗ Test suite FAILED\n======================================");
      }
   }

   private static boolean expect(Planner planner, Transaction tx, String qry,
         int[] expected, String fld, String label) throws Exception {
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      List<Integer> got = new ArrayList<>();
      while (s.next())
         got.add(s.getVal(fld).asInt());
      s.close();
      Collections.sort(got);
      List<Integer> exp = new ArrayList<>();
      for (int v : expected)
         exp.add(v);
      boolean pass = got.equals(exp);
      System.out.printf("  %-30s rows=%s (expected %s) -> %s%n", label, got, exp, pass ? "PASS" : "FAIL");
      return pass;
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
