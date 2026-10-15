package simpledb.materialize;

import java.util.*;

import simpledb.query.*;

/**
 * The Scan class for the <i>distinct</i> operator.
 * It performs the final merge of 1 or 2 duplicate-free runs,
 * removing any duplicate that still occurs across the two runs,
 * one record at a time as the scan is read.
 * @author Edward Sciore
 */
public class DistinctScan implements Scan {
   private UpdateScan s1, s2=null, currentscan=null;
   private RecordComparator comp;
   private List<String> fields;
   private boolean hasmore1, hasmore2=false;
   private GroupValue lastval;

   /**
    * Create a distinct scan, given a list of 1 or 2 duplicate-free runs.
    * If there is only 1 run, then s2 will be null and
    * hasmore2 will be false.
    * @param runs the list of runs
    * @param comp the record comparator
    * @param fields the fields to check for duplicates
    */
   public DistinctScan(List<TempTable> runs, RecordComparator comp, List<String> fields) {
      this.comp = comp;
      this.fields = fields;
      s1 = (UpdateScan) runs.get(0).open();
      hasmore1 = s1.next();
      if (runs.size() > 1) {
         s2 = (UpdateScan) runs.get(1).open();
         hasmore2 = s2.next();
      }
   }

   /**
    * Position the scan before the first record in sorted order.
    * Internally, it moves to the first record of each underlying scan.
    * The variable currentscan is set to null, indicating that there is
    * no current scan.
    * @see simpledb.query.Scan#beforeFirst()
    */
   public void beforeFirst() {
      currentscan = null;
      lastval = null;
      s1.beforeFirst();
      hasmore1 = s1.next();
      if (s2 != null) {
         s2.beforeFirst();
         hasmore2 = s2.next();
      }
   }

   /**
    * Move to the next distinct record in sorted order.
    * First, the current scan is moved to the next record.
    * Then the lowest record of the two scans is found; if
    * it is equal to the previously returned record, it is
    * skipped and the process repeats.
    * @see simpledb.query.Scan#next()
    */
   public boolean next() {
      if (currentscan != null) {
         if (currentscan == s1)
            hasmore1 = s1.next();
         else if (currentscan == s2)
            hasmore2 = s2.next();
      }

      while (hasmore1 || hasmore2) {
         if (hasmore1 && hasmore2)
            currentscan = comp.compare(s1, s2) <= 0 ? s1 : s2;
         else if (hasmore1)
            currentscan = s1;
         else
            currentscan = s2;

         GroupValue val = new GroupValue(currentscan, fields);
         if (lastval == null || !lastval.equals(val)) {
            lastval = val;
            return true;
         }
         // duplicate of the previously returned record: skip it
         if (currentscan == s1)
            hasmore1 = s1.next();
         else
            hasmore2 = s2.next();
      }
      return false;
   }

   /**
    * Close the two underlying scans.
    * @see simpledb.query.Scan#close()
    */
   public void close() {
      s1.close();
      if (s2 != null)
         s2.close();
   }

   /**
    * Get the Constant value of the specified field
    * of the current scan.
    * @see simpledb.query.Scan#getVal(java.lang.String)
    */
   public Constant getVal(String fldname) {
      return currentscan.getVal(fldname);
   }

   /**
    * Get the integer value of the specified field
    * of the current scan.
    * @see simpledb.query.Scan#getInt(java.lang.String)
    */
   public int getInt(String fldname) {
      return currentscan.getInt(fldname);
   }

   /**
    * Get the string value of the specified field
    * of the current scan.
    * @see simpledb.query.Scan#getString(java.lang.String)
    */
   public String getString(String fldname) {
      return currentscan.getString(fldname);
   }

   /**
    * Return true if the specified field is in the current scan.
    * @see simpledb.query.Scan#hasField(java.lang.String)
    */
   public boolean hasField(String fldname) {
      return currentscan.hasField(fldname);
   }
}
