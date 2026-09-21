package simpledb.materialize;

import simpledb.query.*;

/**
 * The <i>avg</i> aggregation function.
 * @author Dumboiroy
 */
public class AvgFn implements AggregationFn {
   private String fldname;
   private int count;
   private int sum;
   
   /**
    * Create a avg aggregation function for the specified field.
    * @param fldname the name of the aggregated field
    */
   public AvgFn(String fldname) {
      this.fldname = fldname;
   }
   
   /**
    * Start a new avg.
    * Since SimpleDB only supports integer avgs,
    * and does not support null values,
    * the count is set to 1 and sum is set to the first value.
    * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
    */
   public void processFirst(Scan s) {
      sum = s.getInt(fldname);
      count = 1;
   }
   
   /**
    * This method adds the current integer value of the field
    * to the current avg.
    * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
    */
   public void processNext(Scan s) {
	  count++;
	  sum = sum + s.getInt(fldname);
   }
   
   /**
    * Return the field's name, prepended by "avgof".
    * @see simpledb.materialize.AggregationFn#fieldName()
    */
   public String fieldName() {
      return "avgof" + fldname;
   }
   
   /**
    * Return the current avg = sum / count
    * @see simpledb.materialize.AggregationFn#value()
    */
   public Constant value() {
      return new Constant(sum / count);
   }
}
