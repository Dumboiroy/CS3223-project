package simpledb.test;

import simpledb.tx.Transaction;

import java.util.Scanner;

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

public class FindMajors {
   public static void main(String[] args) {
	   System.out.print("Enter a department name: ");
	      Scanner sc = new Scanner(System.in);
	      String major = sc.next();
	      sc.close();
	      System.out.println("Here are the " + major + " majors");
	      System.out.println("Name\tGradYear");
	      
	  // query the db
      try {
         // analogous to the driver
         SimpleDB db = new SimpleDB("studentdb");

         // analogous to the connection
         Transaction tx  = db.newTx();
         Planner planner = db.planner();
         
         // analogous to the statement
         String qry = "select SName, GradYear "
                 + "from STUDENT, DEPT "
                 + "where did = MajorId "
                 + "and DName = '" + major + "'";
         
         // create the query plan using the planner, query and transaction.
         Plan p = planner.createQueryPlan(qry, tx);
         
         // analogous to the result set
         Scan s = p.open();
         
         // print results
         while (s.next()) {
        	 String sname = s.getString("sname");
             int gradyear = s.getInt("gradyear");
             System.out.println(sname + "\t" + gradyear);
         }
         
         s.close();
         tx.commit();
      }
      catch(Exception e) {
         e.printStackTrace();
      }
      
      
   }
}
