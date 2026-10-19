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

   public SetOperationScan(List<List<Constant>> rows, List<String> fields, Schema sch) {
      this.rows = rows;
      this.fields = fields;
      this.sch = sch;
   }

   public void beforeFirst() {
      current = -1;
   }

   public boolean next() {
      current++;
      return current < rows.size();
   }

   public Constant getVal(String fldname) {
      return rows.get(current).get(indexOf(fldname));
   }

   public int getInt(String fldname) {
      return getVal(fldname).asInt();
   }

   public String getString(String fldname) {
      return getVal(fldname).asString();
   }

   public boolean hasField(String fldname) {
      return sch.hasField(fldname);
   }

   public void close() {
   }

   private int indexOf(String fldname) {
      String resolved = sch.resolve(fldname);
      int i = fields.indexOf(resolved);
      if (i < 0)
         throw new RuntimeException("field not in set-operation output: " + fldname);
      return i;
   }
}
