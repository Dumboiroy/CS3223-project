package simpledb.plan;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.parse.*;
import simpledb.record.Schema;

/**
 * Validates parsed queries and update commands against the catalog: tables
 * must exist, referenced attributes must be defined (and unambiguous), and
 * inserts must match the table's columns. Throws a RuntimeException describing
 * the first problem found.
 */
public class QueryValidator {
   private MetadataMgr mdm;
   private Transaction tx;

   public QueryValidator(MetadataMgr mdm, Transaction tx) {
      this.mdm = mdm;
      this.tx = tx;
   }

   /** Validate a select query: tables exist and all referenced columns resolve. */
   public void validateQuery(QueryData data) {
      Schema combined = combinedSchema(data.tables());

      // data.fields() holds every selected/aggregated column name
      checkFields(combined, data.fields());
      checkFields(combined, data.groupFields());
      checkFields(combined, data.pred().fields());
      // ORDER BY may name aggregate result columns, so it is checked later by
      // the planner against the actual output schema, not here.
   }

   /** Validate an insert/delete/modify/create-index command. */
   public void validateUpdate(Object data) {
      if (data instanceof InsertData)
         validateInsert((InsertData) data);
      else if (data instanceof DeleteData)
         validateDelete((DeleteData) data);
      else if (data instanceof ModifyData)
         validateModify((ModifyData) data);
      else if (data instanceof CreateIndexData)
         validateCreateIndex((CreateIndexData) data);
      // create table / create view introduce new names; nothing to check here
   }

   private void validateInsert(InsertData data) {
      Schema sch = tableSchema(data.tableName());
      if (data.fields().size() != data.vals().size())
         throw new RuntimeException("insert has " + data.fields().size()
               + " columns but " + data.vals().size() + " values");
      for (String f : data.fields())
         if (!sch.hasField(f))
            throw new RuntimeException("table '" + data.tableName()
                  + "' has no column '" + f + "'");
   }

   private void validateDelete(DeleteData data) {
      Schema sch = tableSchema(data.tableName());
      checkFields(sch, data.pred().fields());
   }

   private void validateModify(ModifyData data) {
      Schema sch = tableSchema(data.tableName());
      if (!sch.hasField(data.targetField()))
         throw new RuntimeException("table '" + data.tableName()
               + "' has no column '" + data.targetField() + "'");
      checkFields(sch, data.pred().fields());
   }

   private void validateCreateIndex(CreateIndexData data) {
      Schema sch = tableSchema(data.tableName());
      if (!sch.hasField(data.fieldName()))
         throw new RuntimeException("cannot index unknown column '"
               + data.fieldName() + "' of table '" + data.tableName() + "'");
   }

   /** Build a qualified schema spanning all FROM tables/views, checking existence. */
   private Schema combinedSchema(Collection<String> tables) {
      Schema combined = new Schema();
      for (String tbl : tables) {
         String viewdef = mdm.getViewDef(tbl, tx);
         if (viewdef != null) {
            QueryData vqd = new Parser(viewdef).query();
            for (String f : vqd.fields())
               combined.addIntField(tbl + "." + f); // type irrelevant for name checks
         } else {
            Schema sch = tableSchema(tbl);
            for (String f : sch.fields())
               combined.addField(tbl + "." + f, sch.type(f), sch.length(f));
         }
      }
      return combined;
   }

   private Schema tableSchema(String tblname) {
      Schema sch = mdm.getLayout(tblname, tx).schema();
      if (sch.fields().isEmpty())
         throw new RuntimeException("table '" + tblname + "' does not exist");
      return sch;
   }

   private void checkFields(Schema sch, List<String> fields) {
      for (String f : fields)
         if (!sch.hasField(f))
            throw new RuntimeException("unknown or ambiguous column '" + f + "'");
   }
}
