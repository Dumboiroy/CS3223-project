package simpledb.parse;

import java.util.*;

import simpledb.query.*;
import simpledb.record.*;

/**
 * The SimpleDB parser.
 * 
 * @author Edward Sciore
 */
public class Parser {
   private Lexer lex;

   public Parser(String s) {
      lex = new Lexer(s);
   }

   // Methods for parsing predicates, terms, expressions, constants, and fields

   public String field() {
      return lex.eatId();
   }

   public Constant constant() {
      if (lex.matchStringConstant())
         return new Constant(lex.eatStringConstant());
      else
         return new Constant(lex.eatIntConstant());
   }

   public Expression expression() {
      if (lex.matchId())
         return new Expression(field());
      else
         return new Expression(constant());
   }

   public Term term() {
      Expression lhs = expression();
      String opr = lex.eatOpr();
      Expression rhs = expression();
      return new Term(lhs, opr, rhs);
   }

   public Predicate predicate() {
      Predicate pred = new Predicate(term());
      if (lex.matchKeyword("and")) {
         lex.eatKeyword("and");
         pred.conjoinWith(predicate());
      }
      return pred;
   }

   // Methods for parsing queries

   public QueryData query() {
	  // SELECT CLAUSE
      lex.eatKeyword("select");
      
      // Extract aggregate functions for GROUP BY clause from SELECT clause
      SelectClauseInfo selectInfo = selectListWithAggregates();
      List<String> fields = selectInfo.fields;
      List<String> aggregateFunctions = selectInfo.aggregateFunctions;
      
      // FROM CLAUSE
      lex.eatKeyword("from");
      Collection<String> tables = tableList();
      
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      
      // GROUP BY CLAUSE (aggregateFunctions already initialized in SELECT clause)
      List<String> groupFields = new ArrayList<String>();
      
      if (lex.matchKeyword("group")) {
    	  lex.eatKeyword("group");
    	  lex.eatKeyword("by");
    	  groupByList(groupFields);
      }
      
      // ORDER BY CLAUSE
      List<String> sortFields = new ArrayList<String>();
      List<Boolean> sortAscending = new ArrayList<Boolean>();

      if (lex.matchKeyword("order")) {
         lex.eatKeyword("order");
         lex.eatKeyword("by");
         orderList(sortFields, sortAscending);
      }
      
      
      return new QueryData(
    	         fields,
    	         tables,
    	         pred,
    	         sortFields,
    	         sortAscending,
    	         groupFields,
    	         aggregateFunctions);
   }
   
//   private List<String> selectList() {
//      List<String> L = new ArrayList<String>();
//      L.add(field());
//      if (lex.matchDelim(',')) {
//         lex.eatDelim(',');
//         L.addAll(selectList());
//      }
//      return L;
//   }
   
   /**
    * Parses the SELECT clause and separates regular fields from aggregate functions.
    * Syntax: field | AGGFN(field) [, field | AGGFN(field)]*
    * Example: "dept, COUNT(id), SUM(salary)"
    * 
    * @param aggregateFunctions list to accumulate aggregate function strings found in SELECT
    * @return list of regular (non-aggregate) field names
    */
   private SelectClauseInfo selectListWithAggregates() {
      List<String> fields = new ArrayList<String>();
      List<String> aggregateFunctions = new ArrayList<String>();
      
      while (true) {
         // Check if this is an aggregate function
         if (checkAggregateFn()) {
            // Parse aggregate function
            String aggFnStr = parseAggregateFn();
            aggregateFunctions.add(aggFnStr);
         } else {
            // Parse regular field
            String fieldName = field();
            fields.add(fieldName);
         }
         
         // Check for comma to continue
         if (!lex.matchDelim(',')) {
            break;
         }
         lex.eatDelim(',');
      }
      
      return new SelectClauseInfo(fields, aggregateFunctions);
   }

   private Collection<String> tableList() {
      Collection<String> L = new ArrayList<String>();
      L.add(lex.eatId());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(tableList());
      }
      return L;
   }
   
   
   
   /**
    * Parses the fields in a GROUP BY clause.
    * Syntax: field [, field]*
    * Example: "dept, role"
    * 
    * Note: Aggregate functions are NOT parsed here; they come from the SELECT list.
    * 
    * @param groupFields list to accumulate the grouping field names
    */
   private void groupByList(List<String> groupFields) {
      while (true) {
         // Only parse regular group fields
         String fieldName = field();
         groupFields.add(fieldName);
         
         if (!lex.matchDelim(',')) {
            break;
         }
         lex.eatDelim(',');
      }
   }

   /**
    * Checks if the current keyword is an aggregate function.
    * Used in selectListWithAggregates() to check if current field is aggregated.
    * 
    * @return the aggregate function string (e.g., "count(id)", "sum(salary)")
    */
   private boolean checkAggregateFn() {
	   return (lex.matchKeyword("count") || lex.matchKeyword("sum") || 
       lex.matchKeyword("max") || lex.matchKeyword("min") || 
       lex.matchKeyword("avg"));
   }
   
   /**
    * Parses a single aggregate function invocation.
    * Syntax: AGGREGATEFN(fieldname)
    * Supported functions: COUNT, SUM, MAX, MIN, AVG
    * 
    * @return the aggregate function string (e.g., "count(id)", "sum(salary)")
    */
   private String parseAggregateFn() {
      String fn = "";
      if (lex.matchKeyword("count")) {
         lex.eatKeyword("count");
         fn = "count";
      } else if (lex.matchKeyword("sum")) {
         lex.eatKeyword("sum");
         fn = "sum";
      } else if (lex.matchKeyword("max")) {
         lex.eatKeyword("max");
         fn = "max";
      } else if (lex.matchKeyword("min")) {
         lex.eatKeyword("min");
         fn = "min";
      } else if (lex.matchKeyword("avg")) {
         lex.eatKeyword("avg");
         fn = "avg";
      }
      
      lex.eatDelim('(');
      String fieldName = field();
      lex.eatDelim(')');
      
      return fn + "(" + fieldName + ")";
   }
   
   /**
    * Parses the comma-separated list following ORDER BY
    *
    * Each item has the following form:
    * field [ASC | DESC | null]
    *
    * ASC is used by default when no direction is specified.
    */
   private void orderList(List<String> sortFields,
                          List<Boolean> sortAscending) {
      while (true) {
         String fldname = field();
         boolean ascending = true;

         if (lex.matchKeyword("asc")) {
            lex.eatKeyword("asc");
         }
         else if (lex.matchKeyword("desc")) {
            lex.eatKeyword("desc");
            ascending = false;
         }

         sortFields.add(fldname);
         sortAscending.add(ascending);

         if (!lex.matchDelim(',')) // exit while loop
            break;

         lex.eatDelim(',');
      }
   }

   // Methods for parsing the various update commands

   public Object updateCmd() {
      if (lex.matchKeyword("insert"))
         return insert();
      else if (lex.matchKeyword("delete"))
         return delete();
      else if (lex.matchKeyword("update"))
         return modify();
      else
         return create();
   }

   private Object create() {
      lex.eatKeyword("create");
      if (lex.matchKeyword("table"))
         return createTable();
      else if (lex.matchKeyword("view"))
         return createView();
      else
         return createIndex();
   }

   // Method for parsing delete commands

   public DeleteData delete() {
      lex.eatKeyword("delete");
      lex.eatKeyword("from");
      String tblname = lex.eatId();
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      return new DeleteData(tblname, pred);
   }

   // Methods for parsing insert commands

   public InsertData insert() {
      lex.eatKeyword("insert");
      lex.eatKeyword("into");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      List<String> flds = fieldList();
      lex.eatDelim(')');
      lex.eatKeyword("values");
      lex.eatDelim('(');
      List<Constant> vals = constList();
      lex.eatDelim(')');
      return new InsertData(tblname, flds, vals);
   }

   private List<String> fieldList() {
      List<String> L = new ArrayList<String>();
      L.add(field());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(fieldList());
      }
      return L;
   }

   private List<Constant> constList() {
      List<Constant> L = new ArrayList<Constant>();
      L.add(constant());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(constList());
      }
      return L;
   }

   // Method for parsing modify commands

   public ModifyData modify() {
      lex.eatKeyword("update");
      String tblname = lex.eatId();
      lex.eatKeyword("set");
      String fldname = field();
      lex.eatDelim('=');
      Expression newval = expression();
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      return new ModifyData(tblname, fldname, newval, pred);
   }

   // Method for parsing create table commands

   public CreateTableData createTable() {
      lex.eatKeyword("table");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      Schema sch = fieldDefs();
      lex.eatDelim(')');
      return new CreateTableData(tblname, sch);
   }

   private Schema fieldDefs() {
      Schema schema = fieldDef();
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         Schema schema2 = fieldDefs();
         schema.addAll(schema2);
      }
      return schema;
   }

   private Schema fieldDef() {
      String fldname = field();
      return fieldType(fldname);
   }

   private Schema fieldType(String fldname) {
      Schema schema = new Schema();
      if (lex.matchKeyword("int")) {
         lex.eatKeyword("int");
         schema.addIntField(fldname);
      } else {
         lex.eatKeyword("varchar");
         lex.eatDelim('(');
         int strLen = lex.eatIntConstant();
         lex.eatDelim(')');
         schema.addStringField(fldname, strLen);
      }
      return schema;
   }

   // Method for parsing create view commands

   public CreateViewData createView() {
      lex.eatKeyword("view");
      String viewname = lex.eatId();
      lex.eatKeyword("as");
      QueryData qd = query();
      return new CreateViewData(viewname, qd);
   }

   // Method for parsing create index commands

   public CreateIndexData createIndex() {
      lex.eatKeyword("index");
      String idxname = lex.eatId();
      lex.eatKeyword("on");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      String fldname = field();
      lex.eatDelim(')');
      String idxtype = "hash";
      if (lex.matchKeyword("using")) {
         lex.eatKeyword("using");
         if (lex.matchKeyword("hash")) {
            lex.eatKeyword("hash");
            idxtype = "hash";
         } else {
            lex.eatKeyword("btree");
            idxtype = "btree";
         }
      }
      return new CreateIndexData(idxname, tblname, fldname, idxtype);
   }
}
