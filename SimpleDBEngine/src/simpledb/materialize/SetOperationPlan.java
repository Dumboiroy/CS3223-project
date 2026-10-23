package simpledb.materialize;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The Plan for a set operation: UNION, INTERSECT or EXCEPT, each with set
 * (duplicate-eliminating) semantics. The two operands must be union-compatible
 * (same number of columns, matched by position). The output uses the left
 * operand's schema. Both inputs are read once and combined by value tuple.
 */
public class SetOperationPlan implements Plan {
   public static final String UNION = "union", INTERSECT = "intersect", EXCEPT = "except";

   private Transaction tx;
   private Plan p1, p2;
   private String op;
   private Schema sch;
   private List<String> leftFields, rightFields;

   /**
    * Create a set-operation plan over two union-compatible inputs.
    * @param tx the calling transaction
    * @param p1 the left input plan
    * @param p2 the right input plan
    * @param op the operator: UNION, INTERSECT or EXCEPT
    */
   public SetOperationPlan(Transaction tx, Plan p1, Plan p2, String op) {
      this.tx = tx;
      this.p1 = p1;
      this.p2 = p2;
      this.op = op;
      this.sch = p1.schema();
      this.leftFields = p1.schema().fields();
      this.rightFields = p2.schema().fields();
      if (leftFields.size() != rightFields.size())
         throw new RuntimeException("set operation operands are not union-compatible: "
               + leftFields.size() + " vs " + rightFields.size() + " columns");
   }

   /**
    * Read both inputs into sets of value tuples and combine them per the
    * operator (set semantics), returning a scan over the result.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      // Positional read: each right row is reordered to the left's column order.
      Set<List<Constant>> left = readRows(p1.open(), leftFields);
      Set<List<Constant>> right = readRows(p2.open(), rightFields);

      List<List<Constant>> result = new ArrayList<>();
      if (op.equals(INTERSECT)) {
         for (List<Constant> row : left)
            if (right.contains(row))
               result.add(row);
      } else if (op.equals(EXCEPT)) {
         for (List<Constant> row : left)
            if (!right.contains(row))
               result.add(row);
      } else { // UNION
         result.addAll(left);
         for (List<Constant> row : right)
            if (!left.contains(row))
               result.add(row);
      }
      return new SetOperationScan(result, leftFields, sch);
   }

   /**
    * Read a scan fully into a set of value tuples (one Constant per field, in
    * the given field order), eliminating duplicates.
    */
   private Set<List<Constant>> readRows(Scan s, List<String> fields) {
      // LinkedHashSet dedups while keeping first-seen order.
      Set<List<Constant>> rows = new LinkedHashSet<>();
      s.beforeFirst();
      while (s.next()) {
         List<Constant> row = new ArrayList<>();
         for (String f : fields)
            row.add(s.getVal(f));
         rows.add(row);
      }
      s.close();
      return rows;
   }

   /**
    * Estimate the block cost as the sum of both inputs.
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      return p1.blocksAccessed() + p2.blocksAccessed();
   }

   /**
    * Estimate the output size: UNION sums both inputs, while INTERSECT and
    * EXCEPT cannot exceed the left input.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      // upper bounds: union adds, intersect/except cannot exceed the left side
      if (op.equals(UNION))
         return p1.recordsOutput() + p2.recordsOutput();
      return p1.recordsOutput();
   }

   /**
    * Estimate the distinct values of a field, taken from the left input.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      return p1.distinctValues(fldname);
   }

   /**
    * Return the output schema (the left operand's schema).
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
      return indent + "SetOperationPlan(" + op + ")"
            + " [blocks: " + blocksAccessed() + ", records: " + recordsOutput() + "]\n"
            + p1.toString(depth + 1)
            + p2.toString(depth + 1);
   }
}
