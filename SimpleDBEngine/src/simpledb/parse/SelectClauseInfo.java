package simpledb.parse;

import java.util.List;

/**
 * Holds the parsed information from a SELECT clause.
 * Contains both the field names and any aggregate functions found in the clause.
 */
public class SelectClauseInfo {
    /** The field names extracted from the SELECT clause. */
    public final List<String> fields;

    /** The aggregate functions extracted from the SELECT clause. */
    public final List<String> aggregateFunctions;

    /**
     * Constructs a SelectClauseInfo with the given fields and aggregate functions.
     *
     * @param fields the field names from the SELECT clause
     * @param aggregateFunctions the aggregate functions from the SELECT clause
     */
    public SelectClauseInfo(List<String> fields, List<String> aggregateFunctions) {
        this.fields = fields;
        this.aggregateFunctions = aggregateFunctions;
    }
}
