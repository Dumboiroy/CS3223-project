package simpledb.test;

import java.io.File;
import simpledb.plan.OrderByTest;

/**
 * Test suite orchestrator for ORDER BY functionality.
 *
 * Handles the complete lifecycle:
 * 1. Setup: Creates the studentdb database with test data
 * 2. Run: Executes all ORDER BY tests
 * 3. Teardown: Cleans up the test database
 */
public class OrderByTestSuite {

   private static final String DB_NAME = "studentdb";

   public static void main(String[] args) {
      try {
         System.out.println("======================================");
         System.out.println("ORDER BY Test Suite");
         System.out.println("======================================\n");

         // Setup phase
         System.out.println("[1/3] SETUP: Preparing test database...\n");
         setupDatabase();

         // Test phase
         System.out.println("\n[2/3] TEST: Running ORDER BY tests...\n");
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
         CreateStudentDB.main(new String[]{});
      } catch (Exception e) {
         throw new RuntimeException("Failed to create database", e);
      }
   }

   /**
    * Runs the ORDER BY test cases.
    */
   private static void runTests() {
      try {
         OrderByTest.main(new String[]{});
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
