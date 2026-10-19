package simpledb.opt;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.parse.QueryData;
import simpledb.plan.*;

/**
 * A System-R / Selinger style cost-based query planner. Unlike the greedy
 * {@link HeuristicQueryPlanner}, it uses dynamic programming over subsets of
 * the query's tables to choose a join order: for every subset it keeps the
 * cheapest left-deep plan, built by extending the best plan of a smaller
 * subset with one more table. Cost is estimated as block accesses, and the
 * per-table join (index / merge / nested-loops / product) is chosen by the
 * existing {@link TablePlanner}.
 */
public class SelingerQueryPlanner implements QueryPlanner {
   private MetadataMgr mdm;

   public SelingerQueryPlanner(MetadataMgr mdm) {
      this.mdm = mdm;
   }

   public Plan createPlan(QueryData data, Transaction tx) {
      List<String> tbls = new ArrayList<>(data.tables());
      int n = tbls.size();

      TablePlanner[] planners = new TablePlanner[n];
      for (int i = 0; i < n; i++)
         planners[i] = new TablePlanner(tbls.get(i), data.pred(), tx, mdm);

      // best[S] = cheapest left-deep plan joining exactly the tables in bitmask S
      Plan[] best = new Plan[1 << n];
      for (int i = 0; i < n; i++)
         best[1 << i] = planners[i].makeSelectPlan();

      // build larger subsets from smaller ones, adding one table at a time
      for (int subset = 1; subset < (1 << n); subset++) {
         if (Integer.bitCount(subset) < 2)
            continue;
         for (int i = 0; i < n; i++) {
            if ((subset & (1 << i)) == 0)
               continue;
            int rest = subset & ~(1 << i);
            if (best[rest] == null)
               continue;
            Plan candidate = planners[i].makeJoinPlan(best[rest]);
            if (candidate == null)
               candidate = planners[i].makeProductPlan(best[rest]);
            if (best[subset] == null
                  || candidate.blocksAccessed() < best[subset].blocksAccessed())
               best[subset] = candidate;
         }
      }

      Plan joinplan = best[(1 << n) - 1];
      return PlanFinisher.finish(tx, joinplan, data);
   }

   public void setPlanner(Planner p) {
      // not used (views are not planned here)
   }
}
