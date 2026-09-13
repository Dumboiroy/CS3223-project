package simpledb.plan;

import simpledb.query.NestedLoopJoinScan;
import simpledb.query.Predicate;
import simpledb.query.Scan;
import simpledb.record.Schema;

/** The Plan class corresponding to the <i>nestedloopjoin</i>
  * relational algebra operator.
  * Unlike a product plan followed by a select plan, the join
  * predicate is evaluated as part of the scan's iteration, so
  * that no separate selection operation is required.
  */
public class NestedLoopJoinPlan implements Plan {
   private Plan p1, p2;
   private Predicate joinpred;
   private Schema schema = new Schema();

   /**
    * Creates a new nested-loops join node in the query tree,
    * having the two specified subqueries and join predicate.
    * @param p1 the left-hand (outer) subquery
    * @param p2 the right-hand (inner) subquery
    * @param joinpred the predicate to be applied to each combination
    */
   public NestedLoopJoinPlan(Plan p1, Plan p2, Predicate joinpred) {
      this.p1 = p1;
      this.p2 = p2;
      this.joinpred = joinpred;
      schema.addAll(p1.schema());
      schema.addAll(p2.schema());
   }

   /**
    * Creates a nested-loops join scan for this query.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      Scan s1 = p1.open();
      Scan s2 = p2.open();
      return new NestedLoopJoinScan(s1, s2, joinpred);
   }

   /**
    * Estimates the number of block accesses in the join.
    * Since every RHS record is examined for each LHS record,
    * the formula is the same as for a product:
    * <pre> B(nestedloopjoin(p1,p2)) = B(p1) + R(p1)*B(p2) </pre>
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      return p1.blocksAccessed() + (p1.recordsOutput() * p2.blocksAccessed());
   }

   /**
    * Estimates the number of output records in the join,
    * which is the product of the two underlying plans, reduced
    * by the selectivity of the join predicate.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      return (p1.recordsOutput() * p2.recordsOutput()) / joinpred.reductionFactor(this);
   }

   /**
    * Estimates the distinct number of field values in the join.
    * Since the join does not increase or decrease field values,
    * the estimate is the same as in the appropriate underlying query.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      if (p1.schema().hasField(fldname))
         return p1.distinctValues(fldname);
      else
         return p2.distinctValues(fldname);
   }

   /**
    * Returns the schema of the join,
    * which is the union of the schemas of the underlying queries.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return schema;
   }
}
