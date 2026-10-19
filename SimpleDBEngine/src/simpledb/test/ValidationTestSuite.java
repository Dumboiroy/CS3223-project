package simpledb.test;

import java.io.File;
import simpledb.tx.Transaction;
import simpledb.plan.Planner;
import simpledb.server.SimpleDB;

/**
 * Tests query/update validation: valid statements succeed, and invalid ones
 * (unknown table, unknown/ambiguous column, insert arity mismatch, indexing a
 * missing column) are rejected with an error instead of crashing or silently
 * misbehaving.
 */
public class ValidationTestSuite {
   private static final String DBNAME = "validationtestdb";

   public static void main(String[] args) {
      deleteDB(DBNAME);
      boolean ok = true;
      try {
         SimpleDB db = new SimpleDB(DBNAME);
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         planner.executeUpdate("create table student(sid int, sname varchar(10), majorid int)", tx);
         planner.executeUpdate("create table dept(did int, dname varchar(10))", tx);
         planner.executeUpdate("insert into student(sid, sname, majorid) values (1, 'joe', 10)", tx);

         // valid statements should NOT throw
         ok &= accepts(planner, tx, "select sname from student", "valid select");
         ok &= acceptsUpdate(planner, tx,
               "insert into student(sid, sname, majorid) values (2, 'amy', 20)", "valid insert");

         // invalid queries should throw
         ok &= rejectsQuery(planner, tx, "select sname from teacher", "unknown table");
         ok &= rejectsQuery(planner, tx, "select foo from student", "unknown column");
         ok &= rejectsQuery(planner, tx,
               "select sid from student, dept where sid = did", "ok control", false);
         ok &= rejectsUpdate(planner, tx,
               "insert into student(sid, sname) values (3, 'bob', 30)", "insert arity mismatch");
         ok &= rejectsUpdate(planner, tx,
               "insert into student(sid, zzz, majorid) values (3, 'bob', 30)", "insert unknown column");
         ok &= rejectsUpdate(planner, tx,
               "create index idx on student(nosuchcol)", "index on unknown column");
         ok &= rejectsUpdate(planner, tx, "delete from ghost", "delete from unknown table");

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

   private static boolean accepts(Planner planner, Transaction tx, String qry, String label) {
      try {
         planner.createQueryPlan(qry, tx);
         System.out.printf("  %-30s -> PASS (accepted)%n", label);
         return true;
      } catch (RuntimeException e) {
         System.out.printf("  %-30s -> FAIL (rejected: %s)%n", label, e.getMessage());
         return false;
      }
   }

   private static boolean acceptsUpdate(Planner planner, Transaction tx, String cmd, String label) {
      try {
         planner.executeUpdate(cmd, tx);
         System.out.printf("  %-30s -> PASS (accepted)%n", label);
         return true;
      } catch (RuntimeException e) {
         System.out.printf("  %-30s -> FAIL (rejected: %s)%n", label, e.getMessage());
         return false;
      }
   }

   private static boolean rejectsQuery(Planner planner, Transaction tx, String qry, String label) {
      return rejectsQuery(planner, tx, qry, label, true);
   }

   private static boolean rejectsQuery(Planner planner, Transaction tx, String qry,
         String label, boolean shouldReject) {
      boolean threw = false;
      try {
         planner.createQueryPlan(qry, tx);
      } catch (RuntimeException e) {
         threw = true;
      }
      boolean pass = threw == shouldReject;
      System.out.printf("  %-30s -> %s%n", label, pass ? "PASS" : "FAIL");
      return pass;
   }

   private static boolean rejectsUpdate(Planner planner, Transaction tx, String cmd, String label) {
      boolean threw = false;
      try {
         planner.executeUpdate(cmd, tx);
      } catch (RuntimeException e) {
         threw = true;
      }
      System.out.printf("  %-30s -> %s%n", label, threw ? "PASS" : "FAIL");
      return threw;
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
