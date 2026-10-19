package simpledb.test;

import java.io.File;
import java.util.*;
import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.record.*;
import simpledb.materialize.*;
import simpledb.server.SimpleDB;

/**
 * Tests the multi-way (k-way) sort and the k&gt;2 fix in SortScan used by
 * merge join. Covers, deterministically, a 4-way SortScan merge and its
 * savePosition/restorePosition (which merge join relies on), plus end-to-end
 * sorting and a merge join on data crafted to produce many sorted runs.
 */
public class MultiwaySortTestSuite {
   private static final String DBNAME = "multiwaysorttestdb";

   public static void main(String[] args) {
      deleteDB(DBNAME);
      boolean ok = true;
      try {
         SimpleDB db = new SimpleDB(DBNAME);
         ok &= testKWayMergeAndRestore(db);
         ok &= testEndToEnd(db);
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

   /**
    * Build 4 pre-sorted runs and merge them directly through SortScan, so the
    * k-way merge (k=4) and save/restore are exercised regardless of fanout.
    */
   private static boolean testKWayMergeAndRestore(SimpleDB db) {
      System.out.println("=== Test 1: direct 4-way SortScan merge + save/restore ===");
      Transaction tx = db.newTx();
      Schema sch = new Schema();
      sch.addIntField("v");

      // runs interleave so a correct 4-way merge yields 1..16 in order
      List<TempTable> runs = new ArrayList<>();
      runs.add(makeRun(tx, sch, new int[]{1, 5, 9, 13}));
      runs.add(makeRun(tx, sch, new int[]{2, 6, 10, 14}));
      runs.add(makeRun(tx, sch, new int[]{3, 7, 11, 15}));
      runs.add(makeRun(tx, sch, new int[]{4, 8, 12, 16}));

      RecordComparator comp = new RecordComparator(Arrays.asList("v"));
      SortScan ss = new SortScan(runs, comp);

      // (a) full merged order is 1..16
      List<Integer> merged = new ArrayList<>();
      while (ss.next())
         merged.add(ss.getVal("v").asInt());
      boolean orderOk = merged.equals(range(1, 16));
      System.out.println("  4-way merge order 1..16 -> " + (orderOk ? "PASS" : "FAIL " + merged));

      // (b) save at the 3rd record, wander, restore, and confirm we resume there
      ss.beforeFirst();
      ss.next(); ss.next(); ss.next();           // positioned at value 3
      int savedVal = ss.getVal("v").asInt();
      ss.savePosition();
      ss.next(); ss.next(); ss.next();           // wander forward
      ss.restorePosition();
      boolean restoreVal = ss.getVal("v").asInt() == savedVal;
      List<Integer> resumed = new ArrayList<>();
      resumed.add(ss.getVal("v").asInt());
      while (ss.next())
         resumed.add(ss.getVal("v").asInt());
      boolean resumeOk = restoreVal && resumed.equals(range(savedVal, 16));
      System.out.println("  save/restore resumes at " + savedVal + " -> "
            + (resumeOk ? "PASS" : "FAIL " + resumed));

      ss.close();
      tx.commit();
      return orderOk && resumeOk;
   }

   /**
    * End-to-end: insert rows in descending key order (forcing one run per
    * row, hence many multi-way merge passes), then verify ORDER BY ... asc
    * returns them sorted; and a merge join with duplicate keys returns the
    * right rows.
    */
   private static boolean testEndToEnd(SimpleDB db) {
      System.out.println("\n=== Test 2: end-to-end multi-run sort + merge join ===");
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      // big(bk) holds each key in 1..20 twice; descending inserts => a fresh
      // run per row, so sorting needs several multi-way merge passes.
      planner.executeUpdate("create table big(bk int, tag int)", tx);
      for (int i = 20; i >= 1; i--) {
         planner.executeUpdate("insert into big(bk, tag) values (" + i + ", " + (i % 3) + ")", tx);
         planner.executeUpdate("insert into big(bk, tag) values (" + i + ", " + (i % 3) + ")", tx);
      }

      // ORDER BY asc must yield 1,1,2,2,...,20,20 in order
      Plan p = planner.createQueryPlan("select bk from big order by bk asc", tx);
      Scan s = p.open();
      List<Integer> got = new ArrayList<>();
      while (s.next())
         got.add(s.getVal("bk").asInt());
      s.close();
      List<Integer> expectedSort = new ArrayList<>();
      for (int i = 1; i <= 20; i++) { expectedSort.add(i); expectedSort.add(i); }
      boolean sortOk = got.equals(expectedSort);
      System.out.println("  sort 40 descending rows (dups) -> " + (sortOk ? "PASS" : "FAIL " + got));

      // merge join big with a small table on distinct field names; both sides
      // have duplicate join values, which drives SortScan save/restore.
      planner.executeUpdate("create table small(sk int)", tx);
      planner.executeUpdate("insert into small(sk) values (5)", tx);
      planner.executeUpdate("insert into small(sk) values (10)", tx);
      planner.executeUpdate("insert into small(sk) values (10)", tx);
      Plan jp = planner.createQueryPlan("select tag from small, big where sk = bk", tx);
      Scan js = jp.open();
      int joinCount = 0;
      while (js.next())
         joinCount++;
      js.close();
      // small sk = {5,10,10}; big has 2 rows per key => 5:2 + 10:2 + 10:2 = 6
      boolean joinOk = joinCount == 6;
      System.out.println("  merge join with duplicate keys -> rows=" + joinCount
            + " (expected 6) " + (joinOk ? "PASS" : "FAIL"));

      tx.commit();
      return sortOk && joinOk;
   }

   private static TempTable makeRun(Transaction tx, Schema sch, int[] vals) {
      TempTable t = new TempTable(tx, sch);
      UpdateScan us = t.open();
      for (int v : vals) {
         us.insert();
         us.setInt("v", v);
      }
      us.close();
      return t;
   }

   private static List<Integer> range(int lo, int hi) {
      List<Integer> r = new ArrayList<>();
      for (int i = lo; i <= hi; i++)
         r.add(i);
      return r;
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
