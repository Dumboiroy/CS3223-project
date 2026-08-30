package simpledb.test;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/* Test to verify that indexing is being used for queries.
 *
 * Compares block accesses for:
 * 1. A query with WHERE clause on an indexed column (MajorId)
 * 2. A full table scan of the same table
 *
 * If indexing is working, the indexed query should use fewer blocks.
 */

public class TestIndexUsage {
   public static void main(String[] args) {
      try {
         // Setup SimpleDB
         SimpleDB db = new SimpleDB("largestudentdb");
         Transaction tx = db.newTx();
         Planner planner = db.planner();

         System.out.println("=== Testing Index Usage ===\n");

         // Test 1: Query with WHERE clause on indexed column (MajorId)
         String indexedQuery = "select SName, MajorId from STUDENT where MajorId = 20";
         Plan indexedPlan = planner.createQueryPlan(indexedQuery, tx);
         int blocksWithIndex = indexedPlan.blocksAccessed();

         System.out.println("Query 1 (with index on MajorId = 20):");
         System.out.println("  Blocks accessed: " + blocksWithIndex);

         // Test 2: Full table scan (no WHERE clause)
         String fullScanQuery = "select SName, MajorId from STUDENT";
         Plan fullScanPlan = planner.createQueryPlan(fullScanQuery, tx);
         int blocksFullScan = fullScanPlan.blocksAccessed();

         System.out.println("\nQuery 2 (full table scan):");
         System.out.println("  Blocks accessed: " + blocksFullScan);

         // Test 3: Another indexed query (on StudentId)
         String studentIdQuery = "select SName, MajorId from STUDENT where SId = 5";
         Plan studentIdPlan = planner.createQueryPlan(studentIdQuery, tx);
         int blocksStudentId = studentIdPlan.blocksAccessed();

         System.out.println("\nQuery 3 (with index on SId = 5):");
         System.out.println("  Blocks accessed: " + blocksStudentId);

         // Print results
         System.out.println("\n=== TEST RESULTS ===");

         boolean indexingWorks = blocksWithIndex < blocksFullScan;
         System.out.println("Test 1 - MajorId index reduces blocks: " +
            (indexingWorks ? "✓ PASS" : "✗ FAIL"));
         if (indexingWorks) {
            System.out.println("  -> Saved " + (blocksFullScan - blocksWithIndex) + " blocks");
         }

         boolean studentIdIndexWorks = blocksStudentId < blocksFullScan;
         System.out.println("Test 2 - StudentId index reduces blocks: " +
            (studentIdIndexWorks ? "✓ PASS" : "✗ FAIL"));
         if (studentIdIndexWorks) {
            System.out.println("  -> Saved " + (blocksFullScan - blocksStudentId) + " blocks");
         }

         System.out.println("\nOverall: " +
            ((indexingWorks && studentIdIndexWorks) ? "✓ Indexing is working!" :
             "✗ Indexing may not be properly configured"));

         tx.commit();
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
}
