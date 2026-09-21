package simpledb.materialize;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.plan.Plan;
import simpledb.plan.TablePlan;
import simpledb.query.Scan;

// Directly exercises PartitionJoinPlan/PartitionJoinScan (bypassing the
// query optimizer), testing equality predicates on various table relationships.

public class PartitionJoinTest {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      MetadataMgr mdm = db.mdMgr();
      Transaction tx = db.newTx();

      // Test 1: Student-Dept join on majorid = did
      testStudentDeptJoin(tx, mdm);

      // Test 2: Course-Dept join on deptid = did
      testCourseDeptJoin(tx, mdm);

      // Test 3: Section-Course join on courseid = cid
      testSectionCourseJoin(tx, mdm);

      // Test 4: Enroll-Section join on sectionid = sectid
      testEnrollSectionJoin(tx, mdm);

      tx.commit();
   }

   private static void testStudentDeptJoin(Transaction tx, MetadataMgr mdm) {
      System.out.println("\n=== Test 1: Student-Dept Join (majorid = did) ===");
      System.out.println("select sid, sname, majorid, gradyear, did, dname from student, dept where majorid = did order by sid");
      Plan studentplan = new TablePlan(tx, "student", mdm);
      Plan deptplan = new TablePlan(tx, "dept", mdm);

      Plan partitionjoin = new PartitionJoinPlan(tx, studentplan, deptplan, "majorid", "did");
      Scan s = partitionjoin.open();

      int count;
      for (count = 0; s.next(); count++);
      s.close();
      System.out.println("Rows returned: " + count + " (expected 9)");
      assert count == 9 : "Student-Dept join failed";
   }

   private static void testCourseDeptJoin(Transaction tx, MetadataMgr mdm) {
      System.out.println("\n=== Test 2: Course-Dept Join (deptid = did) ===");
      System.out.println("SELECT cid, title, deptid, did, dname FROM course, dept WHERE deptid = did");
      Plan courseplan = new TablePlan(tx, "course", mdm);
      Plan deptplan = new TablePlan(tx, "dept", mdm);

      Plan partitionjoin = new PartitionJoinPlan(tx, courseplan, deptplan, "deptid", "did");
      Scan s = partitionjoin.open();

      int count;
      for (count = 0; s.next(); count++);
      s.close();
      System.out.println("Rows returned: " + count + " (expected 6)");
      assert count == 6 : "Course-Dept join failed";
   }

   private static void testSectionCourseJoin(Transaction tx, MetadataMgr mdm) {
      System.out.println("\n=== Test 3: Section-Course Join (courseid = cid) ===");
      System.out.println("SELECT sectid, courseid, prof, yearoffered, cid, title, deptid FROM section, course WHERE courseid = cid");
      Plan sectionplan = new TablePlan(tx, "section", mdm);
      Plan courseplan = new TablePlan(tx, "course", mdm);

      Plan partitionjoin = new PartitionJoinPlan(tx, sectionplan, courseplan, "courseid", "cid");
      Scan s = partitionjoin.open();

      int count;
      for (count = 0; s.next(); count++);
      s.close();
      System.out.println("Rows returned: " + count + " (expected 5)");
      assert count == 5 : "Section-Course join failed";
   }

   private static void testEnrollSectionJoin(Transaction tx, MetadataMgr mdm) {
      System.out.println("\n=== Test 4: Enroll-Section Join (sectionid = sectid) ===");
      System.out.println("SELECT eid, studentid, sectionid, grade, sectid, courseid, prof, yearoffered FROM enroll, section WHERE sectionid = sectid");
      Plan enrollplan = new TablePlan(tx, "enroll", mdm);
      Plan sectionplan = new TablePlan(tx, "section", mdm);

      Plan partitionjoin = new PartitionJoinPlan(tx, enrollplan, sectionplan, "sectionid", "sectid");
      Scan s = partitionjoin.open();

      int count;
      for (count = 0; s.next(); count++);
      s.close();
      System.out.println("Rows returned: " + count + " (expected 6)");
      assert count == 6 : "Enroll-Section join failed";
   }
}
