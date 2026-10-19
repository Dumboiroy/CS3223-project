package simpledb.materialize;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.plan.Plan;
import simpledb.query.*;

/**
 * The Plan class for the <i>sort</i> operator.
 * @author Edward Sciore
 */
public class SortPlan implements Plan {
   private Transaction tx;
   private Plan p;
   private Schema sch;
   private RecordComparator comp;
   private List<String> sortfields;

   /**
    * Create a sort plan for the specified query.
    * @param p the plan for the underlying query
    * @param sortfields the fields to sort by
    * @param tx the calling transaction
    */
   public SortPlan(Transaction tx, Plan p, List<String> sortfields) {
      this.tx = tx;
      this.p = p;
      this.sortfields = sortfields;
      sch = p.schema();
      comp = new RecordComparator(sortfields);
   }
   
   /**
    * Creates a sort plan using the specified fields and sorting directions.
    *
    * Each Boolean value corresponds to the field at the same index:
    * true  = ascending
    * false = descending
    *
    * @param p the plan for the underlying query
    * @param sortfields the fields to sort by
    * @param tx the calling transaction
    * @param sortAscending the direction for each sort field
    */
   public SortPlan(Transaction tx,
                   Plan p,
                   List<String> sortfields,
                   List<Boolean> sortAscending) {
	   
      if (sortfields.size() != sortAscending.size())
         throw new IllegalArgumentException(
               "Each sort field must have a sorting direction");

      this.tx = tx;
      this.p = p;
      this.sortfields = sortfields;
      this.sch = p.schema();
      this.comp = new RecordComparator(sortfields, sortAscending);
   }
   
   /**
    * Split the input into sorted runs, then repeatedly do a multi-way
    * (k-way) merge until few enough runs remain for SortScan to merge in
    * one final pass. The fanout k is derived from the available buffers,
    * which also keeps two concurrently-open sorts (as in merge join) within
    * the buffer budget.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      Scan src = p.open();
      List<TempTable> runs = splitIntoRuns(src);
      src.close();

      // this ensures there is no error when it is a empty output
      if (runs.isEmpty())
         runs.add(new TempTable(tx, sch));

      int fanout = Math.max(2, tx.availableBuffs() - 2);
      while (runs.size() > fanout)
         runs = doAMergeIteration(runs, fanout);

      return new SortScan(runs, comp);
   }
   
   /**
    * Return the number of blocks in the sorted table,
    * which is the same as it would be in a
    * materialized table.
    * It does <i>not</i> include the one-time cost
    * of materializing and sorting the records.
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      // does not include the one-time cost of sorting
      Plan mp = new MaterializePlan(tx, p); // not opened; just for analysis
      return mp.blocksAccessed();
   }
   
   /**
    * Return the number of records in the sorted table,
    * which is the same as in the underlying query.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      return p.recordsOutput();
   }
   
   /**
    * Return the number of distinct field values in
    * the sorted table, which is the same as in
    * the underlying query.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      return p.distinctValues(fldname);
   }
   
   /**
    * Return the schema of the sorted table, which
    * is the same as in the underlying query.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return sch;
   }

   /**
    * Return a string representation of this plan and its subtree.
    * @see simpledb.plan.Plan#toString(int)
    */
   public String toString(int depth) {
      String indent = "  ".repeat(depth);
      return indent + "SortPlan(" + String.join(", ", sortfields) + ")"
            + " [blocks: " + blocksAccessed() + ", records: " + recordsOutput() + "]\n"
            + p.toString(depth + 1);
   }

   private List<TempTable> splitIntoRuns(Scan src) {
      List<TempTable> temps = new ArrayList<>();
      src.beforeFirst();
      if (!src.next())
         return temps;
      TempTable currenttemp = new TempTable(tx, sch);
      temps.add(currenttemp);
      UpdateScan currentscan = currenttemp.open();
      while (copy(src, currentscan))
         if (comp.compare(src, currentscan) < 0) {
         // start a new run
         currentscan.close();
         currenttemp = new TempTable(tx, sch);
         temps.add(currenttemp);
         currentscan = (UpdateScan) currenttemp.open();
      }
      currentscan.close();
      return temps;
   }
   
   private List<TempTable> doAMergeIteration(List<TempTable> runs, int fanout) {
      List<TempTable> result = new ArrayList<>();
      while (!runs.isEmpty()) {
         int n = Math.min(fanout, runs.size());
         List<TempTable> group = new ArrayList<>(runs.subList(0, n));
         for (int i = 0; i < n; i++)
            runs.remove(0);
         result.add(mergeRuns(group));
      }
      return result;
   }

   /**
    * Merge a group of already-sorted runs into one sorted run using a k-way
    * merge: repeatedly copy out the smallest current record across all runs.
    */
   private TempTable mergeRuns(List<TempTable> group) {
      List<Scan> srcs = new ArrayList<>();
      boolean[] hasmore = new boolean[group.size()];
      for (int i = 0; i < group.size(); i++) {
         Scan s = group.get(i).open();
         srcs.add(s);
         hasmore[i] = s.next();
      }

      TempTable result = new TempTable(tx, sch);
      UpdateScan dest = result.open();

      while (true) {
         int min = -1;
         for (int i = 0; i < srcs.size(); i++) {
            if (!hasmore[i])
               continue;
            if (min < 0 || comp.compare(srcs.get(i), srcs.get(min)) < 0)
               min = i;
         }
         if (min < 0)
            break;
         hasmore[min] = copy(srcs.get(min), dest);
      }

      for (Scan s : srcs)
         s.close();
      dest.close();
      return result;
   }
   
   private boolean copy(Scan src, UpdateScan dest) {
      dest.insert();
      for (String fldname : sch.fields())
         dest.setVal(fldname, src.getVal(fldname));
      return src.next();
   }
}
