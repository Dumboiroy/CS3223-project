package simpledb.query;

/**
 * The scan class corresponding to the <i>nestedloopjoin</i>
 * relational algebra operator.
 * The code is similar to that of ProductScan, except that
 * the join predicate is checked as part of the iteration,
 * so that only matching record combinations are returned.
 */
public class NestedLoopJoinScan implements Scan {
   private Scan s1, s2;
   private Predicate joinpred;

   /**
    * Create a nested-loops join scan for the two underlying scans,
    * having the specified join predicate.
    * @param s1 the LHS (outer) scan
    * @param s2 the RHS (inner) scan
    * @param joinpred the predicate to be checked for each combination
    */
   public NestedLoopJoinScan(Scan s1, Scan s2, Predicate joinpred) {
      this.s1 = s1;
      this.s2 = s2;
      this.joinpred = joinpred;
      beforeFirst();
   }

   /**
    * Position the scan before its first record.
    * In particular, the LHS scan is positioned at
    * its first record, and the RHS scan
    * is positioned before its first record.
    * @see simpledb.query.Scan#beforeFirst()
    */
   public void beforeFirst() {
      s1.beforeFirst();
      s1.next();
      s2.beforeFirst();
   }

   /**
    * Move to the next record satisfying the join predicate.
    * The method moves to the next RHS record, if possible.
    * Otherwise, it moves to the next LHS record and the
    * first RHS record.
    * If there are no more LHS records, the method returns false.
    * @see simpledb.query.Scan#next()
    */
   public boolean next() {
      while (true) {
         while (s2.next()) {
            if (joinpred.isSatisfied(this))
               return true;
         }
         s2.beforeFirst();
         if (!s1.next())
            return false;
      }
   }

   /**
    * Return the integer value of the specified field.
    * The value is obtained from whichever scan
    * contains the field.
    * @see simpledb.query.Scan#getInt(java.lang.String)
    */
   public int getInt(String fldname) {
      if (s1.hasField(fldname))
         return s1.getInt(fldname);
      else
         return s2.getInt(fldname);
   }

   /**
    * Returns the string value of the specified field.
    * The value is obtained from whichever scan
    * contains the field.
    * @see simpledb.query.Scan#getString(java.lang.String)
    */
   public String getString(String fldname) {
      if (s1.hasField(fldname))
         return s1.getString(fldname);
      else
         return s2.getString(fldname);
   }

   /**
    * Return the value of the specified field.
    * The value is obtained from whichever scan
    * contains the field.
    * @see simpledb.query.Scan#getVal(java.lang.String)
    */
   public Constant getVal(String fldname) {
      if (s1.hasField(fldname))
         return s1.getVal(fldname);
      else
         return s2.getVal(fldname);
   }

   /**
    * Returns true if the specified field is in
    * either of the underlying scans.
    * @see simpledb.query.Scan#hasField(java.lang.String)
    */
   public boolean hasField(String fldname) {
      return s1.hasField(fldname) || s2.hasField(fldname);
   }

   /**
    * Close both underlying scans.
    * @see simpledb.query.Scan#close()
    */
   public void close() {
      s1.close();
      s2.close();
   }
}
