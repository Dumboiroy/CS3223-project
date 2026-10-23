package simpledb.materialize;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.query.*;
import simpledb.record.*;

/**
 * The Plan for a partition-based (grace hash) join. It splits both inputs into
 * hash buckets on the join field so matching rows share a bucket, then joins
 * the inputs bucket by bucket (via {@link PartitionJoinScan}). The bucket count
 * is chosen from the larger input's size and the available buffers.
 */
public class PartitionJoinPlan implements Plan {
   private Transaction tx;
   private Plan p1, p2;
   private String fldname1, fldname2;
   private Schema sch = new Schema();
   
   int largerTableBlocks;
   int numBuckets;
   
   final int AVAILABLE_BUFFER_BLOCKS = 8;

   /**
    * Create a partition-join plan for the two inputs on the given join fields.
    * @param tx the calling transaction
    * @param p1 the LHS plan
    * @param p2 the RHS plan
    * @param fldname1 the LHS join field
    * @param fldname2 the RHS join field
    */
   public PartitionJoinPlan(Transaction tx, Plan p1, Plan p2,
                                 String fldname1, String fldname2) {
      this.tx = tx;
      this.p1 = p1;
      this.p2 = p2;
      this.fldname1 = fldname1;
      this.fldname2 = fldname2;
      sch.addAll(p1.schema());
      sch.addAll(p2.schema());
      this.largerTableBlocks = Math.max(p1.blocksAccessed(), p2.blocksAccessed());
      this.numBuckets = Math.max(
    		  1,
    		  (int) Math.ceil((double) largerTableBlocks / AVAILABLE_BUFFER_BLOCKS));
   }

   /**
    * Open both inputs and return a {@link PartitionJoinScan} over them.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      Scan s1 = p1.open();
      Scan s2 = p2.open();
      return new PartitionJoinScan(tx, s1, s2, p1.schema(), p2.schema(), fldname1, fldname2, numBuckets);
   }

   /**
    * Estimate the block cost: each input is read to partition it and again to
    * join, i.e. roughly three passes over both inputs.
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      return 3 * (p1.blocksAccessed() + p2.blocksAccessed());
   }

   /**
    * Estimate the number of output records using the standard join-size estimate.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      int maxvals = Math.max(p1.distinctValues(fldname1),
                             p2.distinctValues(fldname2));
      return (p1.recordsOutput() * p2.recordsOutput()) / maxvals;
   }

   /**
    * Estimate the distinct values of a field, taken from whichever input holds it.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      if (p1.schema().hasField(fldname))
         return p1.distinctValues(fldname);
      else
         return p2.distinctValues(fldname);
   }

   /**
    * Return the combined schema of the two inputs.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return sch;
   }

   /**
    * Return this plan as an indented line followed by its two child plans.
    * @see simpledb.plan.Plan#toString(int)
    */
   public String toString(int depth) {
      String indent = "  ".repeat(depth);
      return indent + "PartitionJoinPlan(" + fldname1 + " = " + fldname2 + ")"
            + " [blocks: " + blocksAccessed() + ", records: " + recordsOutput() + "]\n"
            + p1.toString(depth + 1)
            + p2.toString(depth + 1);
   }
}
