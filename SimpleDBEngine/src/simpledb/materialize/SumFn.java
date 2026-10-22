package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

public class SumFn implements AggregationFn {
   private String fldname;
   private double sum;
   private boolean isDouble;

   public SumFn(String fldname) {
      this.fldname = fldname;
   }

   public void processFirst(Scan s) {
      Constant c = s.getVal(fldname);
      sum = c.asDouble();
      isDouble = c.isDouble();
   }

   public void processNext(Scan s) {
      Constant c = s.getVal(fldname);
      sum += c.asDouble();
      isDouble = isDouble || c.isDouble();
   }

   public String fieldName() {
      return "sumof" + fldname;
   }

   public Constant value() {
      // keep an int column's sum an int; a double column's sum a double
      return isDouble ? new Constant(sum) : new Constant((int) sum);
   }

   public int fieldType(Schema inputSchema) {
      return inputSchema.type(fldname);
   }
}
