package simpledb.test;

import java.io.File;
import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/**
 * Test suite orchestrator for join algorithms.
 *
 * Handles the complete lifecycle:
 * 1. Setup: Creates the largestudentdb database with test data
 * 2. Run: Executes all join algorithm tests
 * 3. Teardown: Cleans up the test database
 *
 * Tests that all three join algorithms (index join, merge join,
 * nested-loops join) produce correct results.
 *
 * 1. STUDENT join DEPT on majorid = did: STUDENT.MajorId has a hash
 *    index, so this should be answered with an index join.
 * 2. COURSE join DEPT on deptid = did: neither side is indexed, so
 *    this should be answered with a merge join.
 * 3. STUDENT join ENROLL on sid != studentid: a non-equality
 *    predicate, which only a nested-loops join can evaluate.
 *
 * Each query's row count is checked against the value computed by
 * hand from the fixed data inserted by CreateLargeStudentDB.
 */
public class JoinAlgorithmTestSuite {

   private static final String DB_NAME = "largestudentdb";

   public static void main(String[] args) {
      try {
         System.out.println("======================================");
         System.out.println("Join Algorithm Test Suite");
         System.out.println("======================================\n");

         // Setup phase
         System.out.println("[1/3] SETUP: Preparing test database...\n");
         setupDatabase();

         // Test phase
         System.out.println("\n[2/3] TEST: Running join algorithm tests...\n");
         runTests();

         // Teardown phase
         System.out.println("\n[3/3] TEARDOWN: Cleaning up test database...\n");
         teardownDatabase();

         System.out.println("======================================");
         System.out.println("✓ Test suite completed successfully!");
         System.out.println("======================================");

      } catch (Exception e) {
         System.err.println("\n✗ Test suite failed!");
         e.printStackTrace();
         System.exit(1);
      }
   }

   /**
    * Sets up the test database by deleting any existing instance
    * and creating a fresh one with test data.
    */
   private static void setupDatabase() {
      // Clean up any existing database from previous runs
      deleteDatabase(DB_NAME);

      // Create fresh database with test data
      try {
         CreateLargeStudentDB.main(new String[]{});
      } catch (Exception e) {
         throw new RuntimeException("Failed to create database", e);
      }
   }

   /**
    * Runs the join algorithm test cases.
    */
   private static void runTests() {
      try {
         SimpleDB db = new SimpleDB(DB_NAME);
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         boolean allPassed = true;

         System.out.println("=== Test 1: index join (STUDENT.MajorId = DEPT.DId) ===");
         String q1 = "select sname, dname from student, dept where majorid = did";
         allPassed &= runAndCheck(planner, tx, q1, 500);

         System.out.println("\n=== Test 2: merge join (COURSE.DeptId = DEPT.DId) ===");
         String q2 = "select title, dname from course, dept where deptid = did";
         allPassed &= runAndCheck(planner, tx, q2, 7);

         System.out.println("\n=== Test 3: nested-loops join (STUDENT.SId != ENROLL.StudentId) ===");
         String q3 = "select sname, grade from student, enroll where sid != studentid";
         allPassed &= runAndCheck(planner, tx, q3, 623750);

         System.out.println("\n=== OVERALL: " + (allPassed ? "ALL TESTS PASSED" : "SOME TESTS FAILED") + " ===");

         tx.commit();
      } catch (Exception e) {
         throw new RuntimeException("Tests failed", e);
      }
   }

   /**
    * Cleans up the test database by deleting the database directory.
    */
   private static void teardownDatabase() {
      System.out.println("Tearing down DB:" + DB_NAME);
      deleteDatabase(DB_NAME);
   }

   /**
    * Runs a query and checks its row count against the expected value.
    */
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

   /**
    * Recursively deletes a database directory.
    *
    * @param dbName the name of the database directory to delete
    */
   private static void deleteDatabase(String dbName) {
      File dbDir = new File(dbName);
      if (dbDir.exists()) {
         if (deleteRecursively(dbDir)) {
            System.out.println("Deleted existing database: " + dbName);
         } else {
            System.err.println("Warning: Could not fully delete directory: " + dbName);
         }
      }
   }

   /**
    * Recursively deletes a file or directory and all its contents.
    *
    * @param file the file or directory to delete
    * @return true if deletion was successful, false otherwise
    */
   private static boolean deleteRecursively(File file) {
      if (file.isDirectory()) {
         File[] contents = file.listFiles();
         if (contents != null) {
            for (File f : contents) {
               if (!deleteRecursively(f)) {
                  return false;
               }
            }
         }
      }
      return file.delete();
   }
}
