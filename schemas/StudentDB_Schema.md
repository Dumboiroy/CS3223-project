# Student Database Schema

Created by: `CreateStudentDB.java`

## Tables

### STUDENT
| SId | SName | MajorId | GradYear |
|-----|-------|---------|----------|
| 1   | joe   | 10      | 2021     |
| 2   | amy   | 20      | 2020     |
| 3   | max   | 10      | 2022     |
| 4   | sue   | 20      | 2022     |
| 5   | bob   | 30      | 2020     |
| 6   | kim   | 20      | 2020     |
| 7   | art   | 30      | 2021     |
| 8   | pat   | 20      | 2019     |
| 9   | lee   | 10      | 2021     |

**Indexes:** `idx_majorid` (hash index on MajorId)

### DEPT
| DId | DName    |
|-----|----------|
| 10  | compsci  |
| 20  | math     |
| 30  | drama    |

### COURSE
| CId | Title        | DeptId |
|-----|--------------|--------|
| 12  | db systems   | 10     |
| 22  | compilers    | 10     |
| 32  | calculus     | 20     |
| 42  | algebra      | 20     |
| 52  | acting       | 30     |
| 62  | elocution    | 30     |

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
| 14  | 1         | 13        | A     |
| 24  | 1         | 43        | C     |
| 34  | 2         | 43        | B+    |
| 44  | 4         | 33        | B     |
| 54  | 4         | 53        | A     |
| 64  | 6         | 53        | A     |

**Indexes:** `idx_studentid` (btree index on StudentId)
