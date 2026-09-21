package simpledb.materialize;

import java.util.*;
import simpledb.query.*;
import simpledb.record.*;

/**
 * The Scan class for the <i>hashjoin</i> operator.
 * Performs hash join on two input scans by building a hash map from the RHS
 * and probing it with the LHS scan.
 *
 * @author Dumboiroy
 */
public class HashJoinScan implements Scan {
	private Scan s1, s2;
	private String fldname1, fldname2;
	private Schema s1Schema, s2Schema;

	private HashMap<Constant, List<GroupValue>> s2Map;
	private List<Map<String, Constant>> joinOutput;
	private int currentOutputIndex;

	/**
	 * Create a hashjoin scan for the two underlying scans.
	 *
	 * @param s1        the LHS scan
	 * @param s2        the RHS scan
	 * @param fldname1  the LHS join field
	 * @param fldname2  the RHS join field
	 * @param s1Schema  the schema of the LHS scan
	 * @param s2Schema  the schema of the RHS scan
	 */
	public HashJoinScan(Scan s1, Scan s2, String fldname1, String fldname2,
			Schema s1Schema, Schema s2Schema) {
		this.s1 = s1;
		this.s2 = s2;
		this.fldname1 = fldname1;
		this.fldname2 = fldname2;
		this.s1Schema = s1Schema;
		this.s2Schema = s2Schema;

		joinOutput = new ArrayList<>();
		currentOutputIndex = -1;

		beforeFirst();
	}

	/**
	 * Position the scan before the first record by building the hash map
	 * from the RHS scan and probing it with the LHS scan.
	 */
	public void beforeFirst() {
		joinOutput.clear();
		currentOutputIndex = -1;

		// Build hash map from s2
		s2Map = new HashMap<>();
		s2.beforeFirst();
		while (s2.next()) {
			Constant s2Key = s2.getVal(fldname2);
			GroupValue gv = new GroupValue(s2, s2Schema.fields());
			s2Map.computeIfAbsent(s2Key, k -> new ArrayList<>()).add(gv);
		}

		// Probe with s1 and build output
		s1.beforeFirst();
		while (s1.next()) {
			Constant s1Key = s1.getVal(fldname1);
			List<GroupValue> s2Records = s2Map.get(s1Key);

			if (s2Records != null) {
				for (GroupValue s2Record : s2Records) {
					Map<String, Constant> combined = new HashMap<>();

					for (String fldname : s1Schema.fields()) {
						combined.put(fldname, s1.getVal(fldname));
					}

					for (String fldname : s2Schema.fields()) {
						combined.put(fldname, s2Record.getVal(fldname));
					}

					joinOutput.add(combined);
				}
			}
		}
	}

	/**
	 * Move to the next record. Returns false when all join results have been
	 * exhausted.
	 */
	public boolean next() {
		if (currentOutputIndex + 1 < joinOutput.size()) {
			currentOutputIndex++;
			return true;
		}
		return false;
	}

	/**
	 * Return the integer value of the specified field.
	 */
	public int getInt(String fldname) {
		return getVal(fldname).asInt();
	}

	/**
	 * Return the string value of the specified field.
	 */
	public String getString(String fldname) {
		return getVal(fldname).asString();
	}

	/**
	 * Return the value of the specified field.
	 */
	public Constant getVal(String fldname) {
		if (currentOutputIndex < 0 || currentOutputIndex >= joinOutput.size()) {
			throw new RuntimeException("No current record");
		}
		return joinOutput.get(currentOutputIndex).get(fldname);
	}

	/**
	 * Return true if the specified field is in either schema.
	 */
	public boolean hasField(String fldname) {
		return s1Schema.fields().contains(fldname) || s2Schema.fields().contains(fldname);
	}

	/**
	 * Close both underlying scans.
	 */
	public void close() {
		s1.close();
		s2.close();
	}
}
