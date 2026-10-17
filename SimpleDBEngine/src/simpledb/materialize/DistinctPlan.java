package simpledb.materialize;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.record.Schema;
import simpledb.plan.Plan;
import simpledb.query.*;

/**
 * The Plan class for the <i>distinct</i> operator.
 * Duplicates are removed while the underlying query's records
 * are being sorted, instead of in a separate pass afterwards:
 * each sorted run is built with no duplicates in it, and each
 * merge of two duplicate-free runs produces another duplicate-free
 * run, so later merge passes handle fewer records.
 * @author Edward Sciore
 */
public class DistinctPlan implements Plan {
   private Transaction tx;
   private Plan p;
   private Schema sch;
   private RecordComparator comp;
   private List<String> fields;

   /**
    * Create a distinct plan for the underlying query.
    * Duplicate elimination is determined by the specified
    * collection of fields, which are the fields of the
    * underlying query's output.
    * @param p a plan for the underlying query
    * @param fields the fields to check for duplicates
    * @param tx the calling transaction
    */
   public DistinctPlan(Transaction tx, Plan p, List<String> fields) {
      this.tx = tx;
      this.p = p;
      this.fields = fields;
      sch = p.schema();
      comp = new RecordComparator(fields);
   }

   /**
    * Sorts the underlying query's records into duplicate-free
    * runs, merges those runs (removing cross-run duplicates as
    * it goes), and returns a scan over the last 1 or 2 runs.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      Scan src = p.open();
      List<TempTable> runs = splitIntoRuns(src);
      src.close();

      // this ensures there is no error when it is a empty output
      if (runs.isEmpty())
         runs.add(new TempTable(tx, sch));

      while (runs.size() > 2)
         runs = doAMergeIteration(runs);

      return new DistinctScan(runs, comp, fields);
   }

   /**
    * Return the number of blocks required to
    * remove duplicates, which is one pass through
    * the underlying query's records.
    * It does <i>not</i> include the one-time cost
    * of materializing, sorting and merging the records.
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      Plan mp = new MaterializePlan(tx, p); // not opened; just for analysis
      return mp.blocksAccessed();
   }

   /**
    * Return the number of distinct output records.
    * Assuming equal distribution, this is the product
    * of the distinct values for each field.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      int numvalues = 1;
      for (String fldname : fields)
         numvalues *= p.distinctValues(fldname);
      return numvalues;
   }

   /**
    * Return the number of distinct values for the
    * specified field, which is the same as in the
    * underlying query.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      return p.distinctValues(fldname);
   }

   /**
    * Returns the schema of the output table,
    * which is the same as the underlying query's schema.
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
      return indent + "DistinctPlan(" + String.join(", ", fields) + ")"
            + " [blocks: " + blocksAccessed() + ", records: " + recordsOutput() + "]\n"
            + p.toString(depth + 1);
   }

   /**
    * Scan the underlying query and split it into sorted runs,
    * as SortPlan does, except that a record is dropped instead
    * of written whenever it is equal to the last record written
    * to the current run. Each resulting run is therefore both
    * sorted and duplicate-free.
    */
   private List<TempTable> splitIntoRuns(Scan src) {
      List<TempTable> temps = new ArrayList<>();
      src.beforeFirst();
      if (!src.next())
         return temps;
      TempTable currenttemp = new TempTable(tx, sch);
      temps.add(currenttemp);
      UpdateScan currentscan = currenttemp.open();
      write(src, currentscan);
      boolean hasmore = src.next();
      while (hasmore) {
         int result = comp.compare(src, currentscan);
         if (result < 0) {
            // out of order: start a new run
            currentscan.close();
            currenttemp = new TempTable(tx, sch);
            temps.add(currenttemp);
            currentscan = (UpdateScan) currenttemp.open();
            write(src, currentscan);
         }
         else if (result > 0)
            // a new distinct value: keep it in the current run
            write(src, currentscan);
         // result == 0: duplicate of the last record written; drop it
         hasmore = src.next();
      }
      currentscan.close();
      return temps;
   }

   private List<TempTable> doAMergeIteration(List<TempTable> runs) {
      List<TempTable> result = new ArrayList<>();
      while (runs.size() > 1) {
         TempTable p1 = runs.remove(0);
         TempTable p2 = runs.remove(0);
         result.add(mergeTwoRuns(p1, p2));
      }
      if (runs.size() == 1)
         result.add(runs.get(0));
      return result;
   }

   /**
    * Merge two duplicate-free runs into one duplicate-free run.
    * Both inputs are already sorted and contain no duplicates, so
    * a record only needs to be compared against the last record
    * written to the output to tell whether it is a duplicate.
    */
   private TempTable mergeTwoRuns(TempTable p1, TempTable p2) {
      Scan src1 = p1.open();
      Scan src2 = p2.open();
      TempTable result = new TempTable(tx, sch);
      UpdateScan dest = result.open();

      boolean hasmore1 = src1.next();
      boolean hasmore2 = src2.next();
      boolean wrote = false;
      while (hasmore1 && hasmore2)
         if (comp.compare(src1, src2) <= 0) {
            wrote = writeIfNew(src1, dest, wrote);
            hasmore1 = src1.next();
         }
         else {
            wrote = writeIfNew(src2, dest, wrote);
            hasmore2 = src2.next();
         }

      if (hasmore1)
         while (hasmore1) {
            wrote = writeIfNew(src1, dest, wrote);
            hasmore1 = src1.next();
         }
      else
         while (hasmore2) {
            wrote = writeIfNew(src2, dest, wrote);
            hasmore2 = src2.next();
         }
      src1.close();
      src2.close();
      dest.close();
      return result;
   }

   private void write(Scan src, UpdateScan dest) {
      dest.insert();
      for (String fldname : sch.fields())
         dest.setVal(fldname, src.getVal(fldname));
   }

   /**
    * Write the current record of src to dest, unless it is
    * equal to the last record written to dest.
    * @return true (dest now has at least one record written to it)
    */
   private boolean writeIfNew(Scan src, UpdateScan dest, boolean wrote) {
      if (!wrote || comp.compare(src, dest) != 0)
         write(src, dest);
      return true;
   }
}
