package simpledb.test;

import java.io.File;
import java.util.*;
import simpledb.tx.Transaction;
import simpledb.plan.*;
import simpledb.query.*;
import simpledb.metadata.MetadataMgr;
import simpledb.opt.HeuristicQueryPlanner;
import simpledb.opt.SelingerQueryPlanner;
import simpledb.index.planner.IndexUpdatePlanner;
import simpledb.server.SimpleDB;

/**
 * Tests the Selinger (dynamic-programming) optimizer by checking it returns
 * the same results as the heuristic optimizer across single-table, multi-way
 * join, group-by, order-by and distinct queries. Builds both planners over the
 * same database so only the join-ordering strategy differs.
 */
public class SelingerOptimizerTestSuite {
   private static final String DBNAME = "selingertestdb";

   public static void main(String[] args) {
      deleteDB(DBNAME);
      boolean ok = true;
      try {
         SimpleDB db = new SimpleDB(DBNAME);
         MetadataMgr mdm = db.mdMgr();
         setup(db);

         Transaction tx = db.newTx();
         Planner heuristic = new Planner(new HeuristicQueryPlanner(mdm), new IndexUpdatePlanner(mdm));
         Planner selinger = new Planner(new SelingerQueryPlanner(mdm), new IndexUpdatePlanner(mdm));

         String[] queries = {
            "select sname from student",
            "select sname, dname from student, dept where majorid = did",
            "select sname, dname, title from student, dept, course where majorid = did and did = deptid",
            "select majorid, count(sid) from student group by majorid",
            "select sname from student order by sname desc",
            "select distinct majorid from student, dept where majorid = did",
         };

         System.out.println("=== Selinger vs Heuristic (same results expected) ===");
         for (String q : queries) {
            List<String> r1 = run(heuristic, tx, q);
            List<String> r2 = run(selinger, tx, q);
            Collections.sort(r1);
            Collections.sort(r2);
            boolean match = r1.equals(r2);
            ok &= match;
            System.out.printf("  %-70s rows=%d -> %s%n",
                  truncate(q), r2.size(), match ? "MATCH" : "MISMATCH " + r1 + " vs " + r2);
         }

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

   private static List<String> run(Planner planner, Transaction tx, String qry) {
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      List<String> rows = new ArrayList<>();
      List<String> flds = p.schema().fields();
      while (s.next()) {
         StringBuilder sb = new StringBuilder();
         for (String f : flds)
            sb.append(s.getVal(f)).append('|');
         rows.add(sb.toString());
      }
      s.close();
      return rows;
   }

   private static void setup(SimpleDB db) {
      Transaction tx = db.newTx();
      Planner planner = db.planner();
      planner.executeUpdate("create table student(sid int, sname varchar(10), majorid int, gradyear int)", tx);
      planner.executeUpdate("create table dept(did int, dname varchar(10))", tx);
      planner.executeUpdate("create table course(cid int, title varchar(12), deptid int)", tx);
      String[][] students = {
         {"1","joe","10","2021"},{"2","amy","20","2020"},{"3","max","10","2022"},
         {"4","sue","20","2022"},{"5","bob","30","2020"},{"6","kim","20","2020"},
         {"7","art","30","2021"},{"8","pat","20","2019"},{"9","lee","10","2021"}};
      for (String[] s : students)
         planner.executeUpdate("insert into student(sid, sname, majorid, gradyear) values ("
               + s[0] + ", '" + s[1] + "', " + s[2] + ", " + s[3] + ")", tx);
      for (String[] d : new String[][]{{"10","compsci"},{"20","math"},{"30","drama"}})
         planner.executeUpdate("insert into dept(did, dname) values (" + d[0] + ", '" + d[1] + "')", tx);
      for (String[] c : new String[][]{{"12","db","10"},{"22","compilers","10"},{"32","calculus","20"}})
         planner.executeUpdate("insert into course(cid, title, deptid) values ("
               + c[0] + ", '" + c[1] + "', " + c[2] + ")", tx);
      tx.commit();
   }

   private static String truncate(String s) {
      return s.length() <= 68 ? s : s.substring(0, 65) + "...";
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
