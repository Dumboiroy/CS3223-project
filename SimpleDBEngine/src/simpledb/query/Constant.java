package simpledb.query;

/**
 * The class that denotes values stored in the database.
 * @author Edward Sciore
 */
public class Constant implements Comparable<Constant> {
   private Integer ival = null;
   private String  sval = null;
   
   public Constant(Integer ival) {
      this.ival = ival;
   }
   
   public Constant(String sval) {
      this.sval = sval;
   }
   
   public int asInt() {
      return ival;
   }
   
   public String asString() {
      return sval;
   }
   

   /**
    * Compares this Constant to another object for equality.
    * Implements the standard Java equals() contract as defined in Java 8.
    *
    * Returns true if and only if the other object is a Constant with the same value.
    * Follows the equivalence relation rules:
    * - Reflexive: x.equals(x) returns true
    * - Symmetric: if x.equals(y) then y.equals(x)
    * - Transitive: if x.equals(y) and y.equals(z) then x.equals(z)
    * - Consistent: multiple invocations return the same result
    * - Null-safe: for any non-null x, x.equals(null) returns false
    *
    * @param obj the reference object with which to compare
    * @return true if this Constant is equal to the specified object; false otherwise
    */
   public boolean equals(Object obj) {
	  if (obj == null) return false;
      Constant c = (Constant) obj;
      return (ival != null) ? ival.equals(c.ival) : sval.equals(c.sval);
   }
   
   public int compareTo(Constant c) {
      return (ival != null) ? ival.compareTo(c.ival) : sval.compareTo(c.sval);
   }
   
   public int hashCode() {
      return (ival != null) ? ival.hashCode() : sval.hashCode();
   }
   
   public String toString() {
      return (ival != null) ? ival.toString() : sval.toString();
   }   
}
