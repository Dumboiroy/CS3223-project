package simpledb.query;

import java.util.*;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.materialize.*;

public class AggregateTest {
   public static void main(String[] args) throws Exception {
      SimpleDB db = new SimpleDB("aggtest");
      Transaction tx = db.newTx();

      // Create schema
      Schema sch = new Schema();
      sch.addIntField("id");
      sch.addIntField("value");
      Layout layout = new Layout(sch);
      UpdateScan s1 = new TableScan(tx, "aggTest", layout);

      // Insert test data
      s1.beforeFirst();
      int[] testValues = {10, 20, 15, 10, 25, 30};
      System.out.println("Inserting test records:");
      for (int i = 0; i < testValues.length; i++) {
         s1.insert();
         s1.setInt("id", i);
         s1.setInt("value", testValues[i]);
         System.out.println("  id=" + i + ", value=" + testValues[i]);
      }
      s1.close();

      // Test COUNT function
      System.out.println("\nTesting CountFn:");
      testAggregate(new TableScan(tx, "aggTest", layout), new CountFn("value"));
      
      // Test MAX function
      System.out.println("\nTesting MaxFn:");
      testAggregate(new TableScan(tx, "aggTest", layout), new MaxFn("value"));

      // Test SUM function
      System.out.println("\nTesting SumFn:");
      testAggregate(new TableScan(tx, "aggTest", layout), new SumFn("value"));

      // Test MIN function
      System.out.println("\nTesting MinFn:");
      testAggregate(new TableScan(tx, "aggTest", layout), new MinFn("value"));
      
      // Test AVG function
      System.out.println("\nTesting AvgFn:");
      testAggregate(new TableScan(tx, "aggTest", layout), new AvgFn("value"));

      tx.commit();
   }

   private static void testAggregate(Scan scan, AggregationFn fn) {
      scan.beforeFirst();
      scan.next();
      fn.processFirst(scan);
      while (scan.next()) {
         fn.processNext(scan);
      }
      System.out.println("Result: " + fn.value());
      scan.close();
   }
}
