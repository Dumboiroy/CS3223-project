package simpledb.test;
import static java.sql.Types.INTEGER;

import java.sql.*;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import simpledb.jdbc.embedded.EmbeddedDriver;
import simpledb.jdbc.network.NetworkDriver;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.query.*;
import simpledb.record.*;
import simpledb.parse.*;

public class SimpleIJNoJDBC {
   public static void main(String[] args) {
      Scanner sc = new Scanner(System.in);
      System.out.println("Choose Database> ");
      String s = sc.nextLine();
      
      // Connect to DB
      SimpleDB db = new SimpleDB(s);

      // analogous to the connection
      Transaction tx  = db.newTx();
      Planner planner = db.planner();

      try {
         System.out.print("\nSQL> ");
         while (sc.hasNextLine()) {
            // process one line of input
            String userInput = sc.nextLine().trim();
            
            // user EXIT
            if (userInput.startsWith("exit"))
                break;
            // don't you want to use the parser...?
            // Since the parser doesn't route for us, we should have app logic here.

            // user QUERY
            else if (userInput.startsWith("select")) {
//            	planner, transaction, query
            	String qry = userInput;
            	try {
            		Plan p = planner.createQueryPlan(qry, tx);
            		doQuery(p);
            	} 
            	catch (RuntimeException e) {
            		System.out.println("Runtime Exception: " + e);
            	}
            }
            
            // user UPDATE (assumes no invalid input)
            else {
            	// TODO: execute update
//               doUpdate(stmt, cmd);
            	String cmd = userInput;
            	try {
            		doUpdate(planner, tx, cmd);
            		
            	} 
            	catch (RuntimeException e) {
            		System.out.println("Runtime Exception: " + e);
            	}
               
            }
            System.out.print("\nSQL> ");
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      sc.close();
      System.out.println("Goodbye.");
   }

   private static void doQuery(Plan p) {
      try {
    	 Schema sch = p.schema();
    	 // Returns display width of a given field by its field name
    	 Function<String, Integer> fldDisplayWidth = 
    			 fldname -> (sch.type(fldname) == INTEGER) ? 6 : sch.length(fldname); 
    	 List<String> fields = sch.fields();
    	 // execute plan
    	 Scan s = p.open();
    	 
         int numcols = sch.fields().size();
         int totalwidth = 0;

         // print header
         for(int i=1; i<=numcols; i++) {
            String fldname = fields.get(i-1);
            int fldtype = sch.type(fldname);
            int fldlength = fldDisplayWidth.apply(fldname);
            int width = Math.max(fldname.length(), fldlength) + 1;
            totalwidth += width;
            String fmt = "%" + width + "s";
            System.out.format(fmt, fldname);
         }
         System.out.println();
         for(int i=0; i<totalwidth; i++)
            System.out.print("-");
         System.out.println();

         // print records
         while(s.next()) {
            for (int i=1; i<=numcols; i++) {
               String fldname = fields.get(i-1);
               int fldtype = sch.type(fldname);
               
               String fmt = "%" + fldDisplayWidth.apply(fldname);
               if (fldtype == Types.INTEGER) {
                  int ival = s.getInt(fldname);
                  System.out.format(fmt + "d", ival);
               }
               else {
                  String sval = s.getString(fldname);
                  System.out.format(fmt + "s", sval);
               }
            }
            System.out.println();
         }
         s.close();
      }
      catch (Exception e) {
         System.out.println("Exception: " + e.getMessage());
      }
   }

   private static void doUpdate(Planner planner, Transaction tx, String cmd) {
      try {
    	 int numRowsAffected = planner.executeUpdate(cmd, tx);
         int howmany = numRowsAffected;
         tx.commit();
         System.out.println(howmany + " records processed");
      }
      catch (RuntimeException e) {
         System.out.println("RuntimeException: " + e);
      }
   }
   
   
   
   
}