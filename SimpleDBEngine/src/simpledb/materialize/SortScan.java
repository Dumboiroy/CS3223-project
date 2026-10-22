package simpledb.materialize;

import java.util.*;

import simpledb.query.*;
import simpledb.record.*;

/**
 * The Scan class for the <i>sort</i> operator.
 * Performs a k-way merge over the final sorted runs: at each step it emits the
 * smallest current record across all runs. Supports any number of runs (not
 * just 1 or 2), and save/restorePosition capture every run plus which run is
 * current, so merge join rewinds correctly even when k &gt; 2.
 * @author Edward Sciore
 */
public class SortScan implements Scan {
   private List<UpdateScan> scans = new ArrayList<>();
   private boolean[] hasmore;
   private RecordComparator comp;
   private int current = -1;           // index of the run holding the current record
   private List<RID> savedposition;
   private int savedcurrent;

   /**
    * Create a sort scan over a list of 1 or more sorted runs.
    * @param runs the list of runs
    * @param comp the record comparator
    */
   public SortScan(List<TempTable> runs, RecordComparator comp) {
      this.comp = comp;
      hasmore = new boolean[runs.size()];
      for (int i = 0; i < runs.size(); i++) {
         UpdateScan s = (UpdateScan) runs.get(i).open();
         scans.add(s);
         hasmore[i] = s.next();
      }
   }

   public void beforeFirst() {
      current = -1;
      for (int i = 0; i < scans.size(); i++) {
         scans.get(i).beforeFirst();
         hasmore[i] = scans.get(i).next();
      }
   }

   /**
    * Advance the current run, then choose the run whose current
    * record is smallest to be the new current run.
    * @see simpledb.query.Scan#next()
    */
   public boolean next() {
      if (current >= 0)
         hasmore[current] = scans.get(current).next();

      current = -1;
      for (int i = 0; i < scans.size(); i++) {
         if (!hasmore[i])
            continue;
         if (current < 0 || comp.compare(scans.get(i), scans.get(current)) < 0)
            current = i;
      }
      return current >= 0;
   }

   public void close() {
      for (UpdateScan s : scans)
         s.close();
   }

   public Constant getVal(String fldname) {
      return scans.get(current).getVal(fldname);
   }

   public int getInt(String fldname) {
      return scans.get(current).getInt(fldname);
   }

   public String getString(String fldname) {
      return scans.get(current).getString(fldname);
   }

   public boolean hasField(String fldname) {
      // schema-level check: every run has the same schema, so use the first run
      // (this is also valid before the scan is positioned, when current is -1)
      return scans.get(0).hasField(fldname);
   }

   /**
    * Save the position of every run (and which run is current) so that the
    * exact merge state can be restored later. A run with no current record
    * saves a null RID.
    */
   public void savePosition() {
      savedposition = new ArrayList<>();
      for (int i = 0; i < scans.size(); i++)
         savedposition.add(hasmore[i] ? scans.get(i).getRid() : null);
      savedcurrent = current;
   }

   /** Move every run back to its previously-saved position. */
   public void restorePosition() {
      for (int i = 0; i < scans.size(); i++) {
         RID rid = savedposition.get(i);
         if (rid != null) {
            scans.get(i).moveToRid(rid);
            hasmore[i] = true;
         } else {
            hasmore[i] = false;
         }
      }
      current = savedcurrent;
   }
}
