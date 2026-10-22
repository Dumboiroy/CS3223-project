package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

public class AvgFn implements AggregationFn {
   private String fldname;
   private int count;
   private double sum;
   private boolean isDouble;

   public AvgFn(String fldname) {
      this.fldname = fldname;
   }

   public void processFirst(Scan s) {
      Constant c = s.getVal(fldname);
      sum = c.asDouble();
      count = 1;
      isDouble = c.isDouble();
   }

   public void processNext(Scan s) {
      Constant c = s.getVal(fldname);
      sum += c.asDouble();
      count++;
      isDouble = isDouble || c.isDouble();
   }

   public String fieldName() {
      return "avgof" + fldname;
   }

   public Constant value() {
      // an int column keeps the original integer-average behaviour;
      // a double column averages as a double
      if (isDouble)
         return new Constant(sum / count);
      return new Constant((int) sum / count);
   }

   public int fieldType(Schema inputSchema) {
      return inputSchema.type(fldname);
   }
}
