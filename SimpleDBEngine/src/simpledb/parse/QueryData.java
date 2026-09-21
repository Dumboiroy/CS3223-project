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
   
   // fields used for GROUP BY 
   private List<String> groupFields;
   private List<String> aggregateFunctions;
   
   // fields used for ORDER BY clause
   private List<String> sortFields;
   private List<Boolean> sortAscending;
   
   /**
    * Saves the field and table list and predicate.
    * 
    * Constructor for queries with ORDER BY clause.
    */
   public QueryData(List<String> fields,
		   			Collection<String> tables,
		   			Predicate pred,
		   			List<String> sortFields,
                    List<Boolean> sortAscending,
                    List<String> groupFields,
                    List<String> aggregateFunctions) {
	   
	   if (sortFields.size() != sortAscending.size())
	         throw new IllegalArgumentException(
	               "Each sort field must have a sorting direction");

	      this.fields = fields;
	      this.tables = tables;
	      this.pred = pred;
	      this.sortFields = sortFields; 
	      this.sortAscending = sortAscending; // true = ascending, false = descending
	      this.groupFields = groupFields;
	      this.aggregateFunctions = aggregateFunctions;
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
    * Returns the fields mentioned in the GROUP BY clause.
    * @return a list of grouping field names (empty if no GROUP BY clause)
    */
   public List<String> groupFields() {
      return groupFields;
   }

   /**
    * Returns the aggregate functions specified in the GROUP BY clause.
    * @return a list of aggregate function strings like "count(id)", "sum(salary)"
    *         (empty if no GROUP BY clause)
    */
   public List<String> aggregateFunctions() {
      return aggregateFunctions;
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
      
      // normal fields
      for (String fldname : fields)
         result += fldname + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      
      // aggregate functions
      for (String aggFn : aggregateFunctions)
          result += aggFn + ", ";
       result = result.substring(0, result.length()-2); //remove final comma
      
      // tables
      result += " from ";
      for (String tblname : tables)
         result += tblname + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      
      // predicates
      String predstring = pred.toString();
      if (!predstring.equals(""))
         result += " where " + predstring;
      
      // group by
      if (!groupFields.isEmpty()) {
    	    result += " group by ";

    	    for (int i = 0; i < groupFields.size(); i++) {
    	        result += groupFields.get(i);

    	        if (i < groupFields.size() - 1)
    	            result += ", ";
    	    }
    	}
      
      // order by
      if (!sortFields.isEmpty()) {
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
