package simpledb.opt;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.parse.QueryData;
import simpledb.plan.*;
import simpledb.materialize.*;

/**
 * Shared post-join plan construction: given a join plan over the query's
 * tables, apply grouping/aggregation, projection, DISTINCT and ORDER BY.
 * Used by every query planner so they only differ in how they build the join.
 */
class PlanFinisher {

   /** Apply group-by, projection, distinct and order-by on top of a join plan. */
   static Plan finish(Transaction tx, Plan joinplan, QueryData data) {
      Plan p = joinplan;

      List<String> projectionFields = data.fields();
      if (!data.aggregateFunctions().isEmpty() || !data.groupFields().isEmpty()) {
         checkAllAggregated(data.fields(), data.aggregateFunctions(), data.groupFields());

         List<AggregationFn> aggFns = createAggregationFunctions(data.aggregateFunctions());
         p = new GroupByPlan(tx, p, data.groupFields(), aggFns);

         projectionFields = new ArrayList<>(data.groupFields());
         for (AggregationFn fn : aggFns)
            projectionFields.add(fn.fieldName());
      }

      p = new ProjectPlan(p, projectionFields);

      if (data.isDistinct())
         p = new DistinctPlan(tx, p, projectionFields);

      if (!data.sortFields().isEmpty()) {
         for (String sortField : data.sortFields())
            if (!p.schema().hasField(sortField))
               throw new RuntimeException("ORDER BY field '" + sortField
                     + "' is not in the output of the query");
         p = new SortPlan(tx, p, data.sortFields(), data.sortAscending());
      }

      return p;
   }

   private static List<AggregationFn> createAggregationFunctions(List<String> aggFnStrings) {
      List<AggregationFn> functions = new ArrayList<>();
      for (String aggStr : aggFnStrings) {
         String type = aggStr.substring(0, aggStr.indexOf('('));
         String field = aggStr.substring(aggStr.indexOf('(') + 1, aggStr.indexOf(')'));
         if (type.equalsIgnoreCase("count"))
            functions.add(new CountFn(field));
         else if (type.equalsIgnoreCase("sum"))
            functions.add(new SumFn(field));
         else if (type.equalsIgnoreCase("max"))
            functions.add(new MaxFn(field));
         else if (type.equalsIgnoreCase("min"))
            functions.add(new MinFn(field));
         else if (type.equalsIgnoreCase("avg"))
            functions.add(new AvgFn(field));
      }
      return functions;
   }

   /**
    * Enforce ONLY_FULL_GROUP_BY: every selected field must be either a
    * grouping field or the argument of an aggregate function.
    */
   private static void checkAllAggregated(List<String> fields, List<String> aggFields,
         List<String> groupFields) {
      for (String field : fields) {
         boolean found = groupFields.contains(field);
         if (!found)
            for (String aggField : aggFields) {
               int open = aggField.indexOf('('), close = aggField.indexOf(')');
               if (open != -1 && close > open
                     && aggField.substring(open + 1, close).trim().equals(field)) {
                  found = true;
                  break;
               }
            }
         if (!found)
            throw new RuntimeException(
                  "Field '" + field + "' must be either aggregated or included in GROUP BY clause");
      }
   }
}
