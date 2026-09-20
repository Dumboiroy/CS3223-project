# Large Student Database Schema

Created by: `CreateLargeStudentDB.java`

## Overview
- **STUDENT:** 500 records
- **DEPT:** 5 departments
- **COURSE:** 7 courses
- **SECTION:** 5 sections
- **ENROLL:** ~1000 enrollment records (2-3 enrollments per student)

## Tables

### STUDENT
| SId | SName           | MajorId | GradYear |
|-----|-----------------|---------|----------|
| 1   | AliceSmith1     | 10      | 2020     |
| 2   | BobJohnson2     | 20      | 2020     |
| 3   | CharlieWilliams3| 30      | 2020     |
| 4   | DianaSmith4     | 40      | 2020     |
| 5   | EveJohnson5     | 50      | 2020     |
| ... | ...             | ...     | ...      |
| 500 | JackMartinez500 | 50      | 2024     |

**Total:** 500 records
**Indexes:** `idx_majorid` (hash), `idx_sid` (btree on SId)

### DEPT
| DId | DName     |
|-----|-----------|
| 10  | compsci   |
| 20  | math      |
| 30  | physics   |
| 40  | chemistry |
| 50  | biology   |

### COURSE
| CId | Title         | DeptId |
|-----|---------------|--------|
| 12  | db systems    | 10     |
| 22  | compilers     | 10     |
| 32  | calculus      | 20     |
| 42  | algebra       | 20     |
| 52  | mechanics     | 30     |
| 62  | organic chem  | 40     |
| 72  | genetics      | 50     |

### SECTION
| SectId | CourseId | Prof    | YearOffered |
|--------|----------|---------|-------------|
| 13     | 12       | turing  | 2018        |
| 23     | 12       | turing  | 2019        |
| 33     | 32       | newton  | 2019        |
| 43     | 32       | einstein| 2017        |
| 53     | 62       | brando  | 2018        |

### ENROLL
| EId | StudentId | SectionId | Grade |
|-----|-----------|-----------|-------|
| 1   | 1         | 13        | A     |
| 2   | 1         | 18        | A-    |
| 3   | 2         | 18        | B+    |
| 4   | 2         | 13        | B     |
| 5   | 3         | 18        | B-    |
| ... | ...       | ...       | ...   |
| 1200| 500       | 53        | D     |

**Total:** ~1000 records (each of 500 students enrolls in 2-3 sections)
**Indexes:** `idx_studentid` (btree on StudentId)
