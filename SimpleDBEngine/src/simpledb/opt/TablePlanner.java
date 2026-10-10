package simpledb.opt;

import java.util.Map;
import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.query.*;
import simpledb.metadata.*;
import simpledb.index.planner.*;
import simpledb.materialize.MergeJoinPlan;
import simpledb.materialize.PartitionJoinPlan;
import simpledb.multibuffer.MultibufferProductPlan;
import simpledb.plan.*;

/**
 * This class contains methods for planning a single table.
 * @author Edward Sciore
 */
class TablePlanner {
   private TablePlan myplan;
   private Predicate mypred;
   private Schema myschema;
   private Map<String,IndexInfo> indexes;
   private Transaction tx;
   private String tblname;
   
   /**
    * Creates a new table planner.
    * The specified predicate applies to the entire query.
    * The table planner is responsible for determining
    * which portion of the predicate is useful to the table,
    * and when indexes are useful.
    * @param tblname the name of the table
    * @param mypred the query predicate
    * @param tx the calling transaction
    */
   public TablePlanner(String tblname, Predicate mypred, Transaction tx, MetadataMgr mdm) {
      this.mypred  = mypred;
      this.tx  = tx;
      this.tblname = tblname;
      myplan   = new TablePlan(tx, tblname, mdm);
      myschema = myplan.schema();
      indexes  = mdm.getIndexInfo(tblname, tx);
   }
   
   /**
    * Constructs a select plan for the table.
    * The plan will use an indexselect, if possible.
    * @return a select plan for the table.
    */
   public Plan makeSelectPlan() {
      Plan p = makeIndexSelect();
      if (p == null)
         p = myplan;
      return addSelectPred(p);
   }
   
   /**
    * Constructs a join plan of the specified plan
    * and the table, choosing among indexjoin, mergejoin
    * and nestedloopjoin whichever is cheapest (fewest block
    * accesses) among those applicable to the join predicate.
    * A nestedloopjoin is always applicable (it supports any
    * predicate, including non-equality ones), so it is used
    * as the fallback when no index or merge join applies.
    * The method returns null if no join is possible.
    * @param current the specified plan
    * @return a join plan of the plan and this table
    */
   public Plan makeJoinPlan(Plan current) {
      Schema currsch = current.schema();
      Predicate joinpred = mypred.joinSubPred(myschema, currsch);
      if (joinpred == null)
         return null;
      
      Plan best = makeNestedLoopJoin(current, currsch);

      Plan mergeJoin = makeMergeJoin(current, currsch);
      if (mergeJoin != null && mergeJoin.blocksAccessed() < best.blocksAccessed())
         best = mergeJoin;

      Plan indexJoin = makeIndexJoin(current, currsch);
      if (indexJoin != null && indexJoin.blocksAccessed() < best.blocksAccessed())
         best = indexJoin;
      
      Plan partitionJoin = makePartitionBasedJoin(current, currsch);
      if (partitionJoin != null && partitionJoin.blocksAccessed() < best.blocksAccessed())
         best = partitionJoin;
      
      if (best == indexJoin)
         System.out.println("index join used on " + tblname);
      else if (best == mergeJoin)
         System.out.println("merge join used on " + tblname);
      else if (best == partitionJoin) 
    	 System.out.println("partition join used on " + tblname);
      else
         System.out.println("nested loop join used on " + tblname);

      return best;
   }
   
   /**
    * Constructs a product of the specified plan and
    * this table.
    * @param current the specified plan
    * @return a product plan of the specified plan and this table
    */
   public Plan makeProductPlan(Plan current) {
      Plan p = addSelectPred(myplan);
      return new MultibufferProductPlan(tx, current, p);
   }
   
   private Plan makeIndexSelect() {
      for (String fldname : indexes.keySet()) {
         Constant val = mypred.equatesWithConstant(fldname);
         if (val != null) {
            IndexInfo ii = indexes.get(fldname);
            System.out.println("index on " + fldname + " used");
            return new IndexSelectPlan(myplan, ii, val);
         }
      }
      return null;
   }
   
   private Plan makeIndexJoin(Plan current, Schema currsch) {
      for (String fldname : indexes.keySet()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            IndexInfo ii = indexes.get(fldname);
            Plan p = new IndexJoinPlan(current, myplan, ii, outerfield);
            p = addSelectPred(p);
            return addJoinPred(p, currsch);
         }
      }
      return null;
   }
   
   private Plan makeMergeJoin(Plan current, Schema currsch) {
      for (String fldname : myschema.fields()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            Plan p = new MergeJoinPlan(tx, current, myplan, outerfield, fldname);
            p = addSelectPred(p);
            return addJoinPred(p, currsch);
         }
      }
      return null;
   }
   
   private Plan makePartitionBasedJoin(Plan current, Schema currsch) {
	   // Try each field in current table's schema
	   for (String fldname : myschema.fields()) {
	      // Check if this field has an equality condition with outer table
	      String outerfield = mypred.equatesWithField(fldname);
	      if (outerfield != null && currsch.hasField(outerfield)) {
	         // Field types must match for join
	         if (myschema.type(fldname) == currsch.type(outerfield)) {
	            Plan p = new PartitionJoinPlan(tx, current, myplan, 
	                                               outerfield, fldname);
	            p = addSelectPred(p);
	            return addJoinPred(p, currsch);
	         }
	      }
	   }
	   return null;
	}


   private Plan makeNestedLoopJoin(Plan current, Schema currsch) {
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      Plan p = addSelectPred(myplan);
      return new NestedLoopJoinPlan(current, p, joinpred);
   }
   
  
   
   private Plan addSelectPred(Plan p) {
      Predicate selectpred = mypred.selectSubPred(myschema);
      if (selectpred != null)
         return new SelectPlan(p, selectpred);
      else
         return p;
   }
   
   private Plan addJoinPred(Plan p, Schema currsch) {
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      if (joinpred != null)
         return new SelectPlan(p, joinpred);
      else
         return p;
   }
}
