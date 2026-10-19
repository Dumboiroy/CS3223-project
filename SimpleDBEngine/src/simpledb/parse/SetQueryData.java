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

   public void addQuery(QueryData q) {
      queries.add(q);
   }

   public void addOperator(String op) {
      operators.add(op);
   }

   public List<QueryData> queries() {
      return queries;
   }

   public List<String> operators() {
      return operators;
   }

   public boolean isSingle() {
      return operators.isEmpty();
   }
}
