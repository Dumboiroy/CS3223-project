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

		// Keep the SELECT column order, including interleaved aggregate expressions.
		List<String> projectionFields = data.outputFields();
		if (!data.aggregateFunctions().isEmpty() || !data.groupFields().isEmpty()) {
			checkGroupedFields(data.unaggregatedFields(), data.groupFields());
			List<AggregationFn> aggFns = createAggregationFunctions(data.aggregateFunctions());
			currentplan = new GroupByPlan(tx, currentplan, data.groupFields(), aggFns);
		}

		// A grouping key may order the result without becoming an output column.
		boolean sortBeforeProjection = false;
		for (String sortField : data.sortFields()) {
			if (!currentplan.schema().hasField(sortField))
				throw new RuntimeException("ORDER BY field '" + sortField
						+ "' is not in the output of the query");
			if (!projectionFields.contains(sortField)) {
				if (data.isDistinct())
					throw new RuntimeException("ORDER BY field '" + sortField
							+ "' must be selected when DISTINCT is used");
				sortBeforeProjection = true;
			}
		}
		if (sortBeforeProjection)
			currentplan = new SortPlan(tx, currentplan, data.sortFields(), data.sortAscending());

		Plan p = new ProjectPlan(currentplan, projectionFields);
		if (data.isDistinct())
			p = new DistinctPlan(tx, p, projectionFields);
		if (!data.sortFields().isEmpty() && !sortBeforeProjection)
			p = new SortPlan(tx, p, data.sortFields(), data.sortAscending());

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

	/** Every plain SELECT field in an aggregate query must be a grouping key. */
	private void checkGroupedFields(List<String> fields, List<String> groupFields) {
		for (String field : fields)
			if (!groupFields.contains(field))
				throw new RuntimeException(
						"Field '" + field + "' must be either aggregated or included in GROUP BY clause");
	}

	public void setPlanner(Planner p) {
		// for use in planning views, which
		// for simplicity this code doesn't do.
	}
}
