package simpledb.parse;

import java.util.*;

/**
 * Data for a (possibly set-operation) query. Holds one or more sub-queries and
 * the set operators (union / intersect / except) between them, applied
 * left-to-right. A plain query has a single sub-query and no operators.
 */
public class SetQueryData {
   private List<QueryData> queries = new ArrayList<>();
   private List<String> operators = new ArrayList<>();

   /**
    * Append a sub-query to the chain.
    * @param q the sub-query
    */
   public void addQuery(QueryData q) {
      queries.add(q);
   }

   /**
    * Append a set operator between the previous and next sub-query.
    * @param op the operator: "union", "intersect" or "except"
    */
   public void addOperator(String op) {
      operators.add(op);
   }

   /**
    * Return the sub-queries, in left-to-right order.
    * @return the list of sub-queries
    */
   public List<QueryData> queries() {
      return queries;
   }

   /**
    * Return the set operators between the sub-queries, in order.
    * @return the list of operators
    */
   public List<String> operators() {
      return operators;
   }

   /**
    * Return true if this is a plain query (a single sub-query, no operators).
    * @return true if there are no set operators
    */
   public boolean isSingle() {
      return operators.isEmpty();
   }
}
