package simpledb.materialize;

import java.util.*;
import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The scan for a set operation (UNION / INTERSECT / EXCEPT). It iterates over
 * a precomputed list of result tuples, exposing each value under the output
 * schema's field names (taken from the left operand).
 */
public class SetOperationScan implements Scan {
   private List<List<Constant>> rows;
   private List<String> fields;
   private Schema sch;
   private int current = -1;

   /**
    * Create a scan over the precomputed result tuples.
    * @param rows the result tuples
    * @param fields the output field names (left-operand order)
    * @param sch the output schema
    */
   public SetOperationScan(List<List<Constant>> rows, List<String> fields, Schema sch) {
      this.rows = rows;
      this.fields = fields;
      this.sch = sch;
   }

   /**
    * Position the scan before the first tuple.
    * @see simpledb.query.Scan#beforeFirst()
    */
   public void beforeFirst() {
      current = -1;
   }

   /**
    * Move to the next tuple; return false when exhausted.
    * @see simpledb.query.Scan#next()
    */
   public boolean next() {
      current++;
      return current < rows.size();
   }

   /**
    * Return the value of the specified field in the current tuple.
    * @see simpledb.query.Scan#getVal(java.lang.String)
    */
   public Constant getVal(String fldname) {
      return rows.get(current).get(indexOf(fldname));
   }

   /**
    * Return the integer value of the specified field.
    * @see simpledb.query.Scan#getInt(java.lang.String)
    */
   public int getInt(String fldname) {
      return getVal(fldname).asInt();
   }

   /**
    * Return the string value of the specified field.
    * @see simpledb.query.Scan#getString(java.lang.String)
    */
   public String getString(String fldname) {
      return getVal(fldname).asString();
   }

   /**
    * Return true if the output schema has the specified field.
    * @see simpledb.query.Scan#hasField(java.lang.String)
    */
   public boolean hasField(String fldname) {
      return sch.hasField(fldname);
   }

   /**
    * Close the scan. The result is already materialised in memory, so there is
    * nothing to release.
    * @see simpledb.query.Scan#close()
    */
   public void close() {
   }

   /**
    * Return the position of a field in the output tuple, resolving a bare or
    * qualified reference against the output schema.
    */
   private int indexOf(String fldname) {
      String resolved = sch.resolve(fldname);
      int i = fields.indexOf(resolved);
      if (i < 0)
         throw new RuntimeException("field not in set-operation output: " + fldname);
      return i;
   }
}
