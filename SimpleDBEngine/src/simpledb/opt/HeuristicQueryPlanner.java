package simpledb.opt;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.parse.QueryData;
import simpledb.plan.*;
import simpledb.materialize.*;

/**
 * A query planner that optimizes using a heuristic-based algorithm.
 * 
 * @author Edward Sciore
 */
public class HeuristicQueryPlanner implements QueryPlanner {
	private Collection<TablePlanner> tableplanners = new ArrayList<>();
	private MetadataMgr mdm;

	public HeuristicQueryPlanner(MetadataMgr mdm) {
		this.mdm = mdm;
	}

	/**
	 * Creates an optimized left-deep query plan using the following heuristics. H1.
	 * Choose the smallest table (considering selection predicates) to be first in
	 * the join order. H2. Add the table to the join order which results in the
	 * smallest output.
	 */
	public Plan createPlan(QueryData data, Transaction tx) throws RuntimeException {

		// Step 1: Create a TablePlanner object for each mentioned table
		for (String tblname : data.tables()) {
			TablePlanner tp = new TablePlanner(tblname, data.pred(), tx, mdm);
			tableplanners.add(tp);
		}

		// Step 2: Choose the lowest-size plan to begin the join order
		Plan currentplan = getLowestSelectPlan();

		// Step 3: Repeatedly add a plan to the join order
		while (!tableplanners.isEmpty()) {
			Plan p = getLowestJoinPlan(currentplan);
			if (p != null)
				currentplan = p;
			else // no applicable join
				currentplan = getLowestProductPlan(currentplan);
		}

		// Step 4: Create Projection Fields
		// Note: If a SELECT query contains aggregate functions, every non-aggregated
		// field must be included in the GROUP BY clause.
		List<String> projectionFields = data.fields();
		if (!data.aggregateFunctions().isEmpty() || !data.groupFields().isEmpty()) {
			// ONLY_FULL_GROUP_BY: if any field is agg or groupby -> all fields must be agg
			// or groupby
			checkAllAggregated(data.fields(), data.aggregateFunctions(), data.groupFields());

			// Create aggFns list and pass aggFns + group-by'd fields to GroupByPlan
			List<AggregationFn> aggFns = createAggregationFunctions(data.aggregateFunctions());
			currentplan = new GroupByPlan(tx, currentplan, data.groupFields(), aggFns);

			if (!data.groupFields().isEmpty()) {
				// With grouped fields: project on grouped fields + aggregate result field names
				projectionFields = new ArrayList<>(data.groupFields());
				for (AggregationFn fn : aggFns) {
					projectionFields.add(fn.fieldName());
				}
			} else {
				// No Grouped Fields: project on aggregate result field names, implicit one big
				// group
				projectionFields = new ArrayList<>();
				for (AggregationFn fn : aggFns) {
					projectionFields.add(fn.fieldName());
				}
			}
		}

		// Step 5: Project on the field names
		Plan p = new ProjectPlan(currentplan, projectionFields);

		// Step 6: Add sorting only when ORDER BY is present
		if (!data.sortFields().isEmpty()) {
			p = new SortPlan(tx, p, data.sortFields(), data.sortAscending());
		}

		return p;
	}

	private Plan getLowestSelectPlan() {
		TablePlanner besttp = null;
		Plan bestplan = null;
		for (TablePlanner tp : tableplanners) {
			Plan plan = tp.makeSelectPlan();
			if (bestplan == null || plan.recordsOutput() < bestplan.recordsOutput()) {
				besttp = tp;
				bestplan = plan;
			}
		}
		tableplanners.remove(besttp);
		return bestplan;
	}

	private Plan getLowestJoinPlan(Plan current) {
		TablePlanner besttp = null;
		Plan bestplan = null;
		for (TablePlanner tp : tableplanners) {
			Plan plan = tp.makeJoinPlan(current);
			if (plan != null && (bestplan == null || plan.recordsOutput() < bestplan.recordsOutput())) {
				besttp = tp;
				bestplan = plan;
			}
		}
		if (bestplan != null)
			tableplanners.remove(besttp);
		return bestplan;
	}

	private Plan getLowestProductPlan(Plan current) {
		TablePlanner besttp = null;
		Plan bestplan = null;
		for (TablePlanner tp : tableplanners) {
			Plan plan = tp.makeProductPlan(current);
			if (bestplan == null || plan.recordsOutput() < bestplan.recordsOutput()) {
				besttp = tp;
				bestplan = plan;
			}
		}
		tableplanners.remove(besttp);
		return bestplan;
	}

	/**
	 * Converts aggregate function string representations into AggregationFn
	 * objects. Parses strings like "count(id)", "sum(salary)" and creates the
	 * corresponding AggregationFn instances for execution during grouping.
	 * 
	 * @param aggFnStrings list of aggregate function strings (e.g., "count(id)",
	 *                     "sum(salary)")
	 * @return list of corresponding AggregationFn objects ready for execution
	 */
	private List<AggregationFn> createAggregationFunctions(List<String> aggFnStrings) {
		List<AggregationFn> functions = new ArrayList<>();
		for (String aggStr : aggFnStrings) {
			// Parse strings like "count(id)", "sum(salary)", etc.
			String type = aggStr.substring(0, aggStr.indexOf('('));
			String field = aggStr.substring(aggStr.indexOf('(') + 1, aggStr.indexOf(')'));

			if (type.equalsIgnoreCase("count")) {
				functions.add(new CountFn(field));
			} else if (type.equalsIgnoreCase("sum")) {
				functions.add(new SumFn(field));
			} else if (type.equalsIgnoreCase("max")) {
				functions.add(new MaxFn(field));
			} else if (type.equalsIgnoreCase("min")) {
				functions.add(new MinFn(field));
			} else if (type.equalsIgnoreCase("avg")) {
				functions.add(new AvgFn(field));
			}
		}
		return functions;
	}

	/**
	 * Checks if all fields in the provided list have a corresponding aggregated
	 * function or are included in the group fields. A field is considered valid if
	 * its name appears within one of the aggregated function strings OR is present
	 * in the group fields list.
	 *
	 * @param fields      a list of field names to check
	 * @param aggFields   a list of aggregated function strings (e.g.,
	 *                    "COUNT(fieldName)", "SUM(fieldName)")
	 * @param groupFields a list of grouped field names
	 * @throws RuntimeException if any field is not found in either aggFields or
	 *                          groupFields
	 */
	private void checkAllAggregated(List<String> fields, List<String> aggFields, List<String> groupFields)
			throws RuntimeException {
		for (String field : fields) {
			boolean found = false;
			// Check if field is in groupFields
			for (String groupField : groupFields) {
				if (groupField.equals(field)) {
					found = true;
					break;
				}
			}
			// Check if field is in aggFields
			if (!found) {
				for (String aggField : aggFields) {
					if (aggField.contains(field)) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				throw new RuntimeException(
						"Field '" + field + "' must be either aggregated or included in GROUP BY clause");
			}
		}
	}

	public void setPlanner(Planner p) {
		// for use in planning views, which
		// for simplicity this code doesn't do.
	}
}
