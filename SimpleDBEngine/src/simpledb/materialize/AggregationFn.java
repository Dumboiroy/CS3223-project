package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

public interface AggregationFn {

   void processFirst(Scan s);

   void processNext(Scan s);

   String fieldName();

   Constant value();

   /** The SQL type of this aggregate's output, given the input schema. */
   int fieldType(Schema inputSchema);
}
