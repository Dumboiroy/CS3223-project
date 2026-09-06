package simpledb.materialize;

import java.util.*;

import simpledb.query.*;

/**
 * A comparator for scans.
 * @author Edward Sciore
 */
public class RecordComparator implements Comparator<Scan> {
   private List<String> fields;
   private List<Boolean> sortAscending;
   
   /**
    * Create a comparator using the specified fields,
    * using the ordering implied by its iterator.
    * @param fields a list of field names
    */
   public RecordComparator(List<String> fields) {
      this.fields = fields;
   }
   
   /**
    * Creates a comparator using the specified fields and directions.
    *
    * Each Boolean value corresponds to the field at the same index:
    * true  = ascending
    * false = descending
    *
    * @param fields the fields to sort by
    * @param sortAscending the direction for each sort field
    */
   public RecordComparator(List<String> fields,
                           List<Boolean> sortAscending) {
	   
      if (fields.size() != sortAscending.size())
         throw new IllegalArgumentException(
               "Each sort field must have a sorting direction");

      this.fields = new ArrayList<String>(fields);
      this.sortAscending = new ArrayList<Boolean>(sortAscending);
   }
   
   
   /**
    * Compare the current records of the two specified scans.
    * The sort fields are considered in turn.
    * When a field is encountered for which the records have
    * different values, those values are used as the result
    * of the comparison.
    * If the two records have the same values for all
    * sort fields, then the method returns 0.
    * @param s1 the first scan
    * @param s2 the second scan
    * @return the result of comparing each scan's current record according to the field list
    *         returns negative number if s1 comes first,
    *         positive number if s2 comes first,
    *         zero if all sort fields are equal
    */
   public int compare(Scan s1, Scan s2) {
	   for (int i = 0; i < fields.size(); i++) {
	         String fldname = fields.get(i);

	         Constant val1 = s1.getVal(fldname);
	         Constant val2 = s2.getVal(fldname);

	         int result;

	         if (sortAscending.get(i))
	            result = val1.compareTo(val2);
	         else
	            result = val2.compareTo(val1);

	         if (result != 0)
	            return result;
	      }
      return 0;
   }
}
