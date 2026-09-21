package simpledb.materialize;

import java.util.*;
import simpledb.query.*;

/**
 * The Scan class for the </i>partitionjoin</i> operator. Partitions both input
 * scans by join key, then performs hash join on each partition pair.
 * 
 * @author Dumboiroy
 */
public class PartitionJoinScan implements Scan {
	private Scan s1, s2;
	private String fldname1, fldname2;

	// State for partition iteration
	private Constant currentPartitionKey;
	private List<GroupValue> s1Partition;
	private List<GroupValue> s2Partition;

	// State for output iteration
	private List<Map<String, Constant>> joinOutput;
	private int currentOutputIndex;
	private boolean s1Exhausted;

	// Track the peeked record (first record of next partition)
	// This is used because Scan doesn't have a restorePosition().
	private GroupValue peekedRecord;

	private List<String> s1Fields;
	private List<String> s2Fields;

	/**
	 * Create a </i>partitionjoin</i> scan for the two underlying scans.
	 * 
	 * @param s1       the LHS scan
	 * @param s2       the RHS scan
	 * @param s1Fields the field names from the LHS scan
	 * @param s2Fields the field names from the RHS scan
	 * @param fldname1 the LHS join field
	 * @param fldname2 the RHS join field
	 */
	public PartitionJoinScan(Scan s1, Scan s2, List<String> s1Fields, List<String> s2Fields, String fldname1,
			String fldname2) {
		this.s1 = s1;
		this.s2 = s2;
		this.fldname1 = fldname1;
		this.fldname2 = fldname2;

		this.s1Fields = s1Fields;
		this.s2Fields = s2Fields;

		s1Partition = new ArrayList<>();
		s2Partition = new ArrayList<>();
		joinOutput = new ArrayList<>();
		currentOutputIndex = -1;
		s1Exhausted = false;
		peekedRecord = null;

		beforeFirst();
	}

	/**
	 * Close the scan by closing the two underlying scans.
	 * 
	 * @see simpledb.query.Scan#close()
	 */
	public void close() {
		s1.close();
		s2.close();
	}

	/**
	 * Position the scan before the first record, by positioning each underlying
	 * scan before their first records.
	 * 
	 * @see simpledb.query.Scan#beforeFirst()
	 */
	public void beforeFirst() {
		s1.beforeFirst();
		s2.beforeFirst();
		s1Partition.clear();
		s2Partition.clear();
		joinOutput.clear();
		currentOutputIndex = -1;
		s1Exhausted = false;
		peekedRecord = null;
	}

	/**
	 * Move to the next record. This is where the action is.
	 * <P>
	 * Partitions the input scans by join key and performs hash join on each
	 * partition pair. If there are remaining results from the current partition,
	 * increments the output index. Otherwise, creates a new output List and
	 * processes the next partition in s1. When all partitions in s1 have been
	 * processed, return false.
	 * 
	 * @see simpledb.query.Scan#next()
	 */
	public boolean next() {
		// If we have more output records in the current partition, return the next one
		if (currentOutputIndex + 1 < joinOutput.size()) {
			currentOutputIndex++;
			return true;
		}

		// current partition output exhausted.
		// load next partition.
		while (!s1Exhausted) {
			if (peekedRecord != null) {
				// Get peeked current partition record from previous partition step
				currentPartitionKey = peekedRecord.getVal(fldname1);
				s1Partition.clear();
				s1Partition.add(peekedRecord);
				peekedRecord = null;
			} else {
				if (!s1.next()) {
					// s1 is exhausted. partition-hash-join is finished.
					s1Exhausted = true;
					return false;
				}

				// get current partition key
				currentPartitionKey = s1.getVal(fldname1);

				// Buffer first record of this partition
				s1Partition.clear();
				s1Partition.add(new GroupValue(s1, s1Fields));
			}

			// Read remaining s1 records with same partition key
			boolean s1HasMore = false;
			while (s1.next()) {
				Constant nextKey = s1.getVal(fldname1);
				if (nextKey.equals(currentPartitionKey)) {
					s1Partition.add(new GroupValue(s1, s1Fields));
				} else {
					// This record belongs to next partition, save to peekedRecord.
					peekedRecord = new GroupValue(s1, s1Fields);
					s1HasMore = true;
					break;
				}
			}
			// If loop exited normally (not via break), s1 is exhausted.
			if (!s1HasMore) {
				s1Exhausted = true;
			}

			// Build hash map from s2 records with matching partition key
			HashMap<Constant, List<GroupValue>> s2Map = new HashMap<>();
			s2.beforeFirst();
			while (s2.next()) {
				Constant s2Key = s2.getVal(fldname2);
				if (s2Key.equals(currentPartitionKey)) {
					GroupValue gv = new GroupValue(s2, s2Fields);
					s2Map.computeIfAbsent(s2Key, k -> new ArrayList<>()).add(gv);
				}
			}

			// Hash join: probe hash map with s1 records
			joinOutput.clear();
			for (GroupValue s1Record : s1Partition) {
				Constant s1Key = s1Record.getVal(fldname1);
				List<GroupValue> s2Records = s2Map.get(s1Key);

				if (s2Records != null) {
					for (GroupValue s2Record : s2Records) {
						// Combine s1 and s2 records
						Map<String, Constant> combined = new HashMap<>();

						// Copy all s1 fields
						for (String fldname : s1Fields) {
							combined.put(fldname, s1Record.getVal(fldname));
						}

						// Copy all s2 fields
						for (String fldname : s2Fields) {
							combined.put(fldname, s2Record.getVal(fldname));
						}

						joinOutput.add(combined);
					}
				}
			}

			// If we have output, set output index to 0.
			if (!joinOutput.isEmpty()) {
				currentOutputIndex = 0;
				return true;
			}
			// Otherwise, this partition has no join results.
		}
		// when s1 is fully exhausted, return false.
		return false;
	}

	/**
	 * Return the integer value of the specified field. The value is obtained from
	 * the current output record.
	 * 
	 * @see simpledb.query.Scan#getInt(java.lang.String)
	 */
	public int getInt(String fldname) {
		Constant c = getVal(fldname);
		return c.asInt();
	}

	/**
	 * Return the string value of the specified field. The value is obtained from
	 * the current output record.
	 * 
	 * @see simpledb.query.Scan#getString(java.lang.String)
	 */
	public String getString(String fldname) {
		Constant c = getVal(fldname);
		return c.asString();
	}

	/**
	 * Return the value of the specified field. The value is obtained from the
	 * current output record.
	 * 
	 * @see simpledb.query.Scan#getVal(java.lang.String)
	 */
	public Constant getVal(String fldname) {
		if (currentOutputIndex < 0 || currentOutputIndex >= joinOutput.size()) {
			throw new RuntimeException("No current record");
		}
		return joinOutput.get(currentOutputIndex).get(fldname);
	}

	/**
	 * Return true if the specified field is in either of the underlying scans.
	 * 
	 * @see simpledb.query.Scan#hasField(java.lang.String)
	 */
	public boolean hasField(String fldname) {
		return s1.hasField(fldname) || s2.hasField(fldname);
	}
}
