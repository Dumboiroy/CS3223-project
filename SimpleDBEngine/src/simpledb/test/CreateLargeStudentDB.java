package simpledb.test;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/* Creates a large student database with 500+ records
 * to properly test index performance benefits.
 *
 * Small datasets don't show index benefits since all records
 * fit in a few blocks anyway. This version creates enough data
 * to demonstrate when indexes are truly valuable.
 */

public class CreateLargeStudentDB {
   public static void main(String[] args) {
      // analogous to the driver
      SimpleDB db = new SimpleDB("largestudentdb");

      // analogous to the connection
      Transaction tx  = db.newTx();
      Planner planner = db.planner();

      try {
         String cmd = "create table STUDENT(SId int, SName varchar(20), MajorId int, GradYear int)";
         int success = planner.executeUpdate(cmd, tx);
         if (success == 1) {
            System.out.println("Table STUDENT created.");
         }

         // Create index on MajorId (frequently queried field)
         cmd = "create index idx_majorid on STUDENT (MajorId) using hash";
         planner.executeUpdate(cmd, tx);
         System.out.println("Index idx_majorid created on STUDENT.");

         // Create index on SId (primary key-like)
         cmd = "create index idx_sid on STUDENT (SId) using btree";
         planner.executeUpdate(cmd, tx);
         System.out.println("Index idx_sid created on STUDENT.");

         // Insert 1000 student records
         System.out.println("\nInserting 1000 student records...");
         String s = "insert into STUDENT(SId, SName, MajorId, GradYear) values ";
         int num_update = 0;

         // Generate diverse student data
         String[] firstNames = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank",
                                "Grace", "Henry", "Ivy", "Jack"};
         String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones",
                               "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};

         int majorIds[] = {10, 20, 30, 40, 50};  // 5 different majors
         int gradYears[] = {2020, 2021, 2022, 2023, 2024};

         for (int i = 1; i <= 500; i++) {
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[(i / 10) % lastNames.length];
            String sname = firstName + lastName + i;
            int majorId = majorIds[i % majorIds.length];
            int gradYear = gradYears[(i / 200) % gradYears.length];

            String values = String.format("(%d, '%s', %d, %d)",
                                         i, sname, majorId, gradYear);
            cmd = s + values;
            success = planner.executeUpdate(cmd, tx);
            if (success == 1) {
               num_update++;
            }

            // Commit every 20 records to free buffer pool (index overhead)
            if (i % 20 == 0) {
               System.out.println("  Inserted " + i + " records...");
               tx.commit();
               // Create new transaction for next batch
               tx = db.newTx();
               planner = db.planner();
            }
         }
         System.out.println(num_update + " STUDENT records inserted.");

         // Create DEPT table
         cmd = "create table DEPT(DId int, DName varchar(20))";
         success = planner.executeUpdate(cmd, tx);
         if (success == 1) {
            System.out.println("\nTable DEPT created.");
         }

         String[] deptvals = {
            "(10, 'compsci')",
            "(20, 'math')",
            "(30, 'physics')",
            "(40, 'chemistry')",
            "(50, 'biology')"
         };
         num_update = 0;
         for (int i = 0; i < deptvals.length; i++) {
            cmd = "insert into DEPT(DId, DName) values " + deptvals[i];
            success = planner.executeUpdate(cmd, tx);
            if (success == 1) {
               num_update++;
            }
         }
         System.out.println(num_update + " DEPT records inserted.");

         // Create COURSE table
         cmd = "create table COURSE(CId int, Title varchar(30), DeptId int)";
         success = planner.executeUpdate(cmd, tx);
         if (success == 1) {
            System.out.println("\nTable COURSE created.");
         }

         String[] coursevals = {
            "(12, 'db systems', 10)",
            "(22, 'compilers', 10)",
            "(32, 'calculus', 20)",
            "(42, 'algebra', 20)",
            "(52, 'mechanics', 30)",
            "(62, 'organic chem', 40)",
            "(72, 'genetics', 50)"
         };
         num_update = 0;
         for (int i = 0; i < coursevals.length; i++) {
            cmd = "insert into COURSE(CId, Title, DeptId) values " + coursevals[i];
            success = planner.executeUpdate(cmd, tx);
            if (success == 1) {
               num_update++;
            }
         }
         System.out.println(num_update + " COURSE records inserted.");

         // Create SECTION table
         cmd = "create table SECTION(SectId int, CourseId int, Prof varchar(20), YearOffered int)";
         success = planner.executeUpdate(cmd, tx);
         if (success == 1) {
            System.out.println("\nTable SECTION created.");
         }

         String[] sectvals = {
            "(13, 12, 'turing', 2018)",
            "(23, 12, 'turing', 2019)",
            "(33, 32, 'newton', 2019)",
            "(43, 32, 'einstein', 2017)",
            "(53, 62, 'brando', 2018)"
         };
         num_update = 0;
         for (int i = 0; i < sectvals.length; i++) {
            cmd = "insert into SECTION(SectId, CourseId, Prof, YearOffered) values " + sectvals[i];
            success = planner.executeUpdate(cmd, tx);
            if (success == 1) {
               num_update++;
            }
         }
         System.out.println(num_update + " SECTION records inserted.");

         // Create ENROLL table with index
         cmd = "create table ENROLL(EId int, StudentId int, SectionId int, Grade varchar(2))";
         success = planner.executeUpdate(cmd, tx);
         if (success == 1) {
            System.out.println("\nTable ENROLL created.");
         }

         cmd = "create index idx_studentid on ENROLL (StudentId) using btree";
         planner.executeUpdate(cmd, tx);
         System.out.println("Index idx_studentid created on ENROLL.");

         // Insert enrollment records (multiple enrollments per student)
         System.out.println("\nInserting enrollment records...");
         s = "insert into ENROLL(EId, StudentId, SectionId, Grade) values ";
         String[] grades = {"A", "A-", "B+", "B", "B-", "C+", "C", "D"};
         int eid = 1;
         int enrollCount = 0;

         for (int studentId = 1; studentId <= 500; studentId++) {
            // Each student enrolls in 2-3 sections randomly
            int enrollments = 2 + (studentId % 2);
            for (int e = 0; e < enrollments; e++) {
               int sectionId = 13 + (studentId + e) % 5;
               String grade = grades[studentId % grades.length];
               String values = String.format("(%d, %d, %d, '%s')",
                                            eid, studentId, sectionId, grade);
               cmd = s + values;
               success = planner.executeUpdate(cmd, tx);
               if (success == 1) {
                  enrollCount++;
               }
               eid++;

               // Commit every 20 enrollments to free buffer pool
               if (enrollCount % 20 == 0) {
                  tx.commit();
                  // Create new transaction for next batch
                  tx = db.newTx();
                  planner = db.planner();
               }
            }
         }
         System.out.println(enrollCount + " ENROLL records inserted.");

         System.out.println("\n✓ Large database created successfully!");
         System.out.println("  500 students across 5 majors");
         System.out.println("  Indexes on: STUDENT.MajorId, STUDENT.SId, ENROLL.StudentId");

         tx.commit();
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
}
