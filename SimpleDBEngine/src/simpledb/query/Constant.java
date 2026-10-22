package simpledb.query;

/**
 * The class that denotes values stored in the database.
 * A Constant holds an integer, a double, or a string. Int and double
 * constants are numeric and compare/equate by promoting to double.
 * @author Edward Sciore
 */
public class Constant implements Comparable<Constant> {
   private Integer ival = null;
   private String  sval = null;
   private Double  dval = null;

   public Constant(Integer ival) {
      this.ival = ival;
   }

   public Constant(String sval) {
      this.sval = sval;
   }

   public Constant(Double dval) {
      this.dval = dval;
   }

   public int asInt() {
      if (ival != null)
         return ival;
      if (dval != null)
         return dval.intValue();
      throw new RuntimeException("Constant is not numeric: " + sval);
   }

   public String asString() {
      return sval;
   }

   /** Return the value as a double, promoting an int constant if needed. */
   public double asDouble() {
      if (dval != null)
         return dval;
      if (ival != null)
         return ival.doubleValue();
      throw new RuntimeException("Constant is not numeric: " + sval);
   }

   /** Return true if this constant holds a numeric (int or double) value. */
   public boolean isNumeric() {
      return ival != null || dval != null;
   }

   /** Return true if this constant holds a double value. */
   public boolean isDouble() {
      return dval != null;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof Constant))
         return false;
      Constant c = (Constant) obj;
      if (isNumeric() && c.isNumeric())
         return asDouble() == c.asDouble();
      if (sval != null && c.sval != null)
         return sval.equals(c.sval);
      return false;
   }

   public int compareTo(Constant c) {
      if (isNumeric() && c.isNumeric())
         return Double.compare(asDouble(), c.asDouble());
      if (sval != null && c.sval != null)
         return sval.compareTo(c.sval);
      throw new RuntimeException("Cannot compare numeric and string constants");
   }

   public int hashCode() {
      // numeric constants hash on their double value so equal ints/doubles agree
      if (isNumeric())
         return Double.hashCode(asDouble());
      return sval.hashCode();
   }

   public String toString() {
      if (ival != null)
         return ival.toString();
      if (dval != null)
         return dval.toString();
      return sval;
   }
}
