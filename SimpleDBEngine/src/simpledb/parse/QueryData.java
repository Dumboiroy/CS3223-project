package simpledb.parse;

import java.util.*;

import simpledb.query.*;

/**
 * Data for the SQL <i>select</i> statement.
 * @author Edward Sciore
 */
public class QueryData {
   private List<String> fields;
   private Collection<String> tables;
   private Predicate pred;
   
   // fields used for ORDER BY clause
   private List<String> sortFields;
   private List<Boolean> sortAscending;
   
   /**
    * Constructor for queries without an ORDER BY clause.
    */
   public QueryData(List<String> fields,
                    Collection<String> tables,
                    Predicate pred) {
      this(fields, tables, pred,
            new ArrayList<String>(),
            new ArrayList<Boolean>());
   }
   
   /**
    * Saves the field and table list and predicate.
    * 
    * Constructor for queries with ORDER BY clause.
    */
   public QueryData(List<String> fields,
		   			Collection<String> tables,
		   			Predicate pred,
		   			List<String> sortFields,
                    List<Boolean> sortAscending) {
	   
	   if (sortFields.size() != sortAscending.size())
	         throw new IllegalArgumentException(
	               "Each sort field must have a sorting direction");

	      this.fields = fields;
	      this.tables = tables;
	      this.pred = pred;
	      this.sortFields = sortFields; 
	      this.sortAscending = sortAscending; // true = ascending, false = descending
	      
	   
   }
   
   /**
    * Returns the fields mentioned in the select clause.
    * @return a list of field names
    */
   public List<String> fields() {
      return fields;
   }
   
   /**
    * Returns the tables mentioned in the from clause.
    * @return a collection of table names
    */
   public Collection<String> tables() {
      return tables;
   }
   
   /**
    * Returns the predicate that describes which
    * records should be in the output table.
    * @return the query predicate
    */
   public Predicate pred() {
      return pred;
   }
   
   /**
    * Returns the fields mentioned in the ORDER BY clause.
    *
    * @return a list of sorting field names
    */
   public List<String> sortFields() {
      return sortFields;
   }

   /**
    * Returns the direction of each ORDER BY field.
    *
    * true means ascending and false means descending.
    *
    * @return a list of sorting directions
    */
   public List<Boolean> sortAscending() {
      return sortAscending;
   }
   
   public String toString() {
      String result = "select ";
      
      for (String fldname : fields)
         result += fldname + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      
      result += " from ";
      for (String tblname : tables)
         result += tblname + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      
      String predstring = pred.toString();
      if (!predstring.equals(""))
         result += " where " + predstring;
      
      if (!sortFields.isEmpty()) { // ORDER BY
          result += " order by ";

          for (int i = 0; i < sortFields.size(); i++) {
             result += sortFields.get(i);

             if (sortAscending.get(i))
                result += " asc";
             else
                result += " desc";

             if (i < sortFields.size() - 1)
                result += ", ";
          }
       }
      
      return result;
   }
}
