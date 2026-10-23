package simpledb.test;

import java.nio.file.*;
import java.util.*;

import simpledb.index.planner.IndexJoinPlan;
import simpledb.materialize.MergeJoinPlan;
import simpledb.parse.Parser;
import simpledb.plan.*;
import simpledb.query.*;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/** Regression checks for query-planning and scan cursor edge cases. */
public class QueryRegressionTest {
   private static SimpleDB db;
   private static int checks;

   public static void main(String[] args) throws Exception {
      Path tempRoot = Files.createTempDirectory("simpledb-query-regression-");
      Path root = tempRoot.resolve("db");
      try {
         db = new SimpleDB(root.toString());
         setup();

         expect("empty nested-loop join",
               "select sname, grade from student, enroll where sid < 0 and sid != studentid",
               Arrays.asList("sname", "grade"));
         expect("empty global count", "select count(sid) from student where sid < 0",
               Arrays.asList("countofsid"), "0");
         expect("aggregate output order",
               "select count(sid), gradyear from student group by gradyear order by gradyear",
               Arrays.asList("countofsid", "gradyear"),
               "1|2019", "3|2020", "3|2021", "2|2022");
         expect("interleaved aggregate output order",
               "select max(sid), gradyear, min(sid) from student group by gradyear order by gradyear",
               Arrays.asList("maxofsid", "gradyear", "minofsid"),
               "8|2019|8", "6|2020|2", "9|2021|1", "4|2022|3");
         expect("hidden grouping key",
               "select count(sid) from student group by gradyear order by gradyear",
               Arrays.asList("countofsid"), "1", "3", "3", "2");
         expect("DISTINCT aggregate projection",
               "select distinct count(sid) from student group by gradyear order by countofsid",
               Arrays.asList("countofsid"), "1", "2", "3");
         expect("plain SELECT output order", "select sname, sid from student where sid = 1 order by sid",
               Arrays.asList("sname", "sid"), "joe|1");
         expect("aggregate-only output order", "select min(sid), max(sid), count(sid) from student",
               Arrays.asList("minofsid", "maxofsid", "countofsid"), "1|9|9");
         reject("select sid, avg(sid) from student");
         reject("select avg(sid), sid from student");
         expect("valid grouped aggregate argument",
               "select sid, avg(sid) from student group by sid order by sid",
               Arrays.asList("sid", "avgofsid"),
               "1|1", "2|2", "3|3", "4|4", "5|5", "6|6", "7|7", "8|8", "9|9");
         checkDemoJoin();
         checkEmptyIndexJoin();
         checkMergeRewind();

         String text = new Parser("select sid, sname from student").query().toString();
         require(text.equals("select sid, sname from student"), "SELECT serialization: " + text);
         System.out.println("PASS: " + checks + " query regression checks");
      }
      finally {
         try (java.util.stream.Stream<Path> paths = Files.walk(tempRoot)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
               try { Files.deleteIfExists(path); }
               catch (java.io.IOException e) { path.toFile().deleteOnExit(); }
            });
         }
      }
   }

   private static void setup() {
      Transaction tx = db.newTx();
      String[] commands = {
         "create table student(sid int, sname varchar(10), majorid int, gradyear int)",
         "create table dept(did int, dname varchar(10))",
         "create table enroll(eid int, studentid int, grade varchar(2))",
         "create index majoridx on student(majorid) using hash",
         "create index studentidx on enroll(studentid) using btree",
         "insert into student(sid,sname,majorid,gradyear) values (1,'joe',10,2021)",
         "insert into student(sid,sname,majorid,gradyear) values (2,'amy',20,2020)",
         "insert into student(sid,sname,majorid,gradyear) values (3,'max',10,2022)",
         "insert into student(sid,sname,majorid,gradyear) values (4,'sue',20,2022)",
         "insert into student(sid,sname,majorid,gradyear) values (5,'bob',30,2020)",
         "insert into student(sid,sname,majorid,gradyear) values (6,'kim',20,2020)",
         "insert into student(sid,sname,majorid,gradyear) values (7,'art',30,2021)",
         "insert into student(sid,sname,majorid,gradyear) values (8,'pat',20,2019)",
         "insert into student(sid,sname,majorid,gradyear) values (9,'lee',10,2021)",
         "insert into dept(did,dname) values (10,'compsci')",
         "insert into dept(did,dname) values (20,'math')",
         "insert into dept(did,dname) values (30,'drama')",
         "insert into enroll(eid,studentid,grade) values (14,1,'A')",
         "insert into enroll(eid,studentid,grade) values (24,1,'C')",
         "insert into enroll(eid,studentid,grade) values (34,2,'B+')",
         "insert into enroll(eid,studentid,grade) values (44,4,'B')",
         "insert into enroll(eid,studentid,grade) values (54,4,'A')",
         "insert into enroll(eid,studentid,grade) values (64,6,'A')",
         "create table leftinput(lid int, lkey int)",
         "create table rightinput(rkey int, rval int)",
         "insert into leftinput(lid,lkey) values (1,2)",
         "insert into leftinput(lid,lkey) values (2,5)",
         "insert into leftinput(lid,lkey) values (3,5)",
         "insert into leftinput(lid,lkey) values (4,9)",
         "insert into rightinput(rkey,rval) values (9,40)",
         "insert into rightinput(rkey,rval) values (5,30)",
         "insert into rightinput(rkey,rval) values (2,10)",
         "insert into rightinput(rkey,rval) values (5,20)"
      };
      for (String command : commands)
         db.planner().executeUpdate(command, tx);
      tx.commit();
   }

   private static void expect(String label, String sql, List<String> fields, String... expected) {
      Transaction tx = db.newTx();
      Scan scan = null;
      try {
         Plan plan = db.planner().createQueryPlan(sql, tx);
         require(plan.schema().fields().equals(fields), label + " columns: " + plan.schema().fields());
         scan = plan.open();
         List<String> rows = read(scan, fields);
         require(rows.equals(Arrays.asList(expected)), label + " values: " + rows);
         scan.beforeFirst();
         require(read(scan, fields).equals(rows), label + " rescan");
         require(!scan.next(), label + " repeated end of scan");
      }
      finally {
         if (scan != null) scan.close();
         tx.commit();
      }
   }

   private static void reject(String sql) {
      Transaction tx = db.newTx();
      try {
         try { db.planner().createQueryPlan(sql, tx); }
         catch (RuntimeException e) {
            require(e.getMessage().contains("must be either aggregated or included in GROUP BY"),
                  "clear aggregate validation error: " + e);
            return;
         }
         throw new AssertionError("Invalid aggregate query was accepted: " + sql);
      }
      finally { tx.commit(); }
   }

   private static void checkDemoJoin() {
      Transaction tx = db.newTx();
      Scan scan = null;
      try {
         Plan plan = db.planner().createQueryPlan(
               "select sname, dname, majorid, eid, grade from student, dept, enroll "
               + "where majorid = did and studentid = sid order by majorid", tx);
         scan = plan.open();
         List<String> rows = read(scan, plan.schema().fields());
         Collections.sort(rows);
         List<String> expected = new ArrayList<String>(Arrays.asList(
               "joe|compsci|10|14|A", "joe|compsci|10|24|C", "amy|math|20|34|B+",
               "sue|math|20|44|B", "sue|math|20|54|A", "kim|math|20|64|A"));
         Collections.sort(expected);
         require(rows.equals(expected), "three-table demo values: " + rows);
      }
      finally {
         if (scan != null) scan.close();
         tx.commit();
      }
   }

   private static void checkEmptyIndexJoin() {
      Transaction tx = db.newTx();
      Scan scan = null;
      try {
         Plan outer = new SelectPlan(new TablePlan(tx, "student", db.mdMgr()),
               new Parser("select sid from student where sid < 0").query().pred());
         Plan inner = new TablePlan(tx, "enroll", db.mdMgr());
         scan = new IndexJoinPlan(outer, inner,
               db.mdMgr().getIndexInfo("enroll", tx).get("studentid"), "sid").open();
         require(!scan.next(), "empty index join");
         scan.beforeFirst();
         require(!scan.next(), "empty index join rescan");
      }
      finally {
         if (scan != null) scan.close();
         tx.commit();
      }
   }

   private static void checkMergeRewind() {
      Transaction tx = db.newTx();
      Scan scan = null;
      try {
         Plan plan = new MergeJoinPlan(tx, new TablePlan(tx, "leftinput", db.mdMgr()),
               new TablePlan(tx, "rightinput", db.mdMgr()), "lkey", "rkey");
         scan = plan.open();
         require(scan.hasField("lid"), "sorted schema available before next");
         List<String> expected = Arrays.asList("1|10", "2|20", "2|30", "3|20", "3|30", "4|40");
         for (int i = 0; i < 2; i++) {
            List<String> rows = read(scan, Arrays.asList("lid", "rval"));
            Collections.sort(rows);
            require(rows.equals(expected), "merge duplicate values on pass " + i + ": " + rows);
            scan.beforeFirst();
         }
      }
      finally {
         if (scan != null) scan.close();
         tx.commit();
      }
   }

   private static List<String> read(Scan scan, List<String> fields) {
      List<String> rows = new ArrayList<String>();
      while (scan.next()) {
         List<String> row = new ArrayList<String>();
         for (String field : fields)
            row.add(scan.getVal(field).toString());
         rows.add(String.join("|", row));
      }
      return rows;
   }

   private static void require(boolean condition, String message) {
      if (!condition)
         throw new AssertionError(message);
      checks++;
   }
}
