package simpledb.test;

import java.io.File;
import java.util.*;
import simpledb.tx.Transaction;
import simpledb.plan.*;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/**
 * Tests optional, backward-compatible qualified names. Two tables share the
 * column name "id", so the join and projection must be expressible with
 * (table.attribute) pairs, while existing bare-name queries keep working.
 */
public class QualifiedNameTestSuite {
   private static final String DBNAME = "qualnametestdb";

   public static void main(String[] args) {
      deleteDB(DBNAME);
      boolean ok = true;
      try {
         SimpleDB db = new SimpleDB(DBNAME);
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         // emp(id, name, deptid) and dept(id, dname) both have a column "id"
         planner.executeUpdate("create table emp(id int, ename varchar(10), deptid int)", tx);
         planner.executeUpdate("create table dept(id int, dname varchar(10))", tx);
         planner.executeUpdate("insert into emp(id, ename, deptid) values (1, 'amy', 10)", tx);
         planner.executeUpdate("insert into emp(id, ename, deptid) values (2, 'bob', 20)", tx);
         planner.executeUpdate("insert into emp(id, ename, deptid) values (3, 'cal', 10)", tx);
         planner.executeUpdate("insert into dept(id, dname) values (10, 'sales')", tx);
         planner.executeUpdate("insert into dept(id, dname) values (20, 'eng')", tx);

         // 1. bare names still work (backward compatible)
         ok &= count(planner, tx, "select ename from emp", 3, "bare select");

         // 2. qualified name in select
         ok &= count(planner, tx, "select emp.ename from emp", 3, "qualified select");

         // 3. qualified join emp.deptid = dept.id (join key names differ)
         ok &= count(planner, tx,
               "select emp.ename, dept.dname from emp, dept where emp.deptid = dept.id",
               3, "qualified join on deptid=id");

         // 4. the key case: both tables have "id"; disambiguate with table.attr
         ok &= count(planner, tx,
               "select emp.id, dept.id from emp, dept where emp.deptid = dept.id",
               3, "join selecting both id columns");

         // 5. qualified predicate against a constant, qualified field resolves to one table
         ok &= count(planner, tx, "select ename from emp where emp.deptid = 10", 2,
               "qualified constant predicate");

         // 6. value check: emp.id and dept.id are genuinely different columns
         ok &= checkDistinctColumns(planner, tx);

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

   private static boolean count(Planner planner, Transaction tx, String qry, int expected, String label)
         throws Exception {
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      int c = 0;
      while (s.next())
         c++;
      s.close();
      boolean pass = c == expected;
      System.out.printf("  %-40s rows=%d (expected %d) -> %s%n", label, c, expected, pass ? "PASS" : "FAIL");
      return pass;
   }

   /** Verify emp.id and dept.id are reported as distinct values, not merged. */
   private static boolean checkDistinctColumns(Planner planner, Transaction tx) throws Exception {
      Plan p = planner.createQueryPlan(
            "select emp.id, dept.id from emp, dept where emp.deptid = dept.id", tx);
      Scan s = p.open();
      boolean ok = true;
      while (s.next()) {
         int empId = s.getVal("emp.id").asInt();
         int deptId = s.getVal("dept.id").asInt();
         // emp ids are 1..3, dept ids are 10/20; they must never be equal
         if (empId == deptId)
            ok = false;
      }
      s.close();
      System.out.printf("  %-40s -> %s%n", "emp.id and dept.id are distinct", ok ? "PASS" : "FAIL");
      return ok;
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
