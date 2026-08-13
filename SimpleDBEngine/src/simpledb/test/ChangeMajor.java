package simpledb.test;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/* This is a version of the StudentMajor program that
 * accesses the SimpleDB classes directly (instead of
 * connecting to it as a JDBC client). 
 * 
 * These kind of programs are useful for debugging
 * your changes to the SimpleDB source code.
 */

public class ChangeMajor {
   public static void main(String[] args) {
      try {
         // analogous to the driver
         SimpleDB db = new SimpleDB("studentdb");

         // analogous to the connection
         Transaction tx  = db.newTx();
         Planner planner = db.planner();
         
         // analogous to the statement
         String cmd = "update STUDENT set MajorId = 30 where SName = 'amy'";
         
         // create the query plan using the planner, query and transaction.
         int success = planner.executeUpdate(cmd, tx);
         
         tx.commit();
         if (success == 1) {
        	 System.out.println("Amy is now a drama major.");
         } else {
        	 System.out.println("Amy's major did not change");
         }
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
}
