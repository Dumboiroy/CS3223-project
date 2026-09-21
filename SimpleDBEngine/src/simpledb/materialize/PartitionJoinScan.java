package simpledb.materialize;

import java.util.*;
import simpledb.query.*;
import simpledb.record.*;
import simpledb.tx.Transaction;
import simpledb.index.hash.HashIndex;

/**
 * The Scan class for the </i>partitionjoin</i> operator. Partitions both input
 * scans into fixed hash buckets by join key, then performs hash join on each
 * bucket pair.
 *
 * @author Dumboiroy
 */
public class PartitionJoinScan implements Scan {
	private Scan s1, s2;
	private String fldname1, fldname2;
	private Transaction tx;
	private Schema s1Schema, s2Schema;

	// Partitions: hashkey -> bucket(TempTable - stored on disk)
	private Map<Integer, TempTable> s1Buckets;
	private Map<Integer, TempTable> s2Buckets;

	// State for bucket iteration
	private int currentBucket;

	// State for output iteration
	private List<Map<String, Constant>> joinOutput;
	private int currentOutputIndex;

	/**
	 * Create a </i>partitionjoin</i> scan for the two underlying scans.
	 *
	 * @param tx       the transaction
	 * @param s1       the LHS scan
	 * @param s2       the RHS scan
	 * @param s1Schema the schema of the LHS scan
	 * @param s2Schema the schema of the RHS scan
	 * @param fldname1 the LHS join field
	 * @param fldname2 the RHS join field
	 */
	public PartitionJoinScan(Transaction tx, Scan s1, Scan s2, Schema s1Schema, Schema s2Schema,
			String fldname1, String fldname2) {
		this.tx = tx;
		this.s1 = s1;
		this.s2 = s2;
		this.s1Schema = s1Schema;
		this.s2Schema = s2Schema;
		this.fldname1 = fldname1;
		this.fldname2 = fldname2;

		s1Buckets = new HashMap<>();
		s2Buckets = new HashMap<>();
		joinOutput = new ArrayList<>();
		currentBucket = -1;
		currentOutputIndex = -1;

		prePartition();
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
	 * Position the scan before the first record. This is done by positioning the
	 * underlying bucket map before its first bucket and the first join result
	 * before its first record.
	 * 
	 * @see simpledb.query.Scan#beforeFirst()
	 */
	public void beforeFirst() {
		joinOutput.clear();
		currentBucket = -1;
		currentOutputIndex = -1;
	}

	/**
	 * Move to the next record. Iterates through the pre-partitioned buckets and
	 * performs hash join on each bucket pair.
	 *
	 * @see simpledb.query.Scan#next()
	 */
	public boolean next() {
		// If we have more output from the current bucket's join, return next
		if (currentOutputIndex + 1 < joinOutput.size()) {
			currentOutputIndex++;
			return true;
		}
		// Current non-empty joined bucket exhausted. need to find new non-empty joined bucket.
		currentBucket++;

		// go through remaining buckets until we find a non-empty joined bucket.
		// if no more non-empty joined buckets, this returns false.
		return getNewJoinedBucket();
	}

	private boolean getNewJoinedBucket() {
		while (currentBucket < HashIndex.NUM_BUCKETS) {
			// Either s1 or s2 have no records for this bucket -> join result is empty.
			// Can just check next bucket.
			if (!s1Buckets.containsKey(currentBucket) || !s2Buckets.containsKey(currentBucket)) {
				currentBucket++;
				continue;
			}

			// Open scans for this bucket
			TempTable s1CurBucket = s1Buckets.get(currentBucket);
			TempTable s2CurBucket = s2Buckets.get(currentBucket);

			Scan s1CurBucketScan = s1CurBucket.open();
			Scan s2CurBucketScan = (s2CurBucket != null) ? s2CurBucket.open() : null;

			// Hash join: build hash map from s2 bucket
			HashMap<Constant, List<GroupValue>> s2Map = new HashMap<>();
			if (s2CurBucketScan != null) {
				while (s2CurBucketScan.next()) {
					Constant s2Key = s2CurBucketScan.getVal(fldname2);
					GroupValue gv = new GroupValue(s2CurBucketScan, s2Schema.fields());
					s2Map.computeIfAbsent(s2Key, k -> new ArrayList<>()).add(gv);
				}
				s2CurBucketScan.close();
			}

			// Hash join: probe hash map with s1 bucket
			joinOutput.clear();
			while (s1CurBucketScan.next()) {
				Constant s1Key = s1CurBucketScan.getVal(fldname1);
				List<GroupValue> s2Records = s2Map.get(s1Key);

				if (s2Records != null) {
					for (GroupValue s2Record : s2Records) {
						// Combine s1 and s2 records
						Map<String, Constant> combined = new HashMap<>();

						// Copy all s1 fields
						for (String fldname : s1Schema.fields()) {
							combined.put(fldname, s1CurBucketScan.getVal(fldname));
						}

						// Copy all s2 fields
						for (String fldname : s2Schema.fields()) {
							combined.put(fldname, s2Record.getVal(fldname));
						}

						joinOutput.add(combined);
					}
				}
			}
			s1CurBucketScan.close();

			// If this bucket has join results, set bucket output index to 0, and return
			// true
			if (!joinOutput.isEmpty()) {
				currentOutputIndex = 0;
				return true;
			}

			// s1 and s2 have results for this bucket, but the actual join is empty
			// (limitation of hashing)
			// the joined bucket is empty. continue to next bucket.
			currentBucket++;
		}
		// All buckets exhausted, return false.
		return false;
	}

	/**
	 * Partition both tables into buckets. Buckets are stored on disk as TempTables
	 *
	 * @see simpledb.materialize.TempTable
	 * @see simpledb.query.Scan#next()
	 */
	private void prePartition() {
		// Partition s1 into buckets
		s1.beforeFirst();
		while (s1.next()) {
			Constant key = s1.getVal(fldname1);
			int bucket = getBucket(key);

			// Get or create TempTable for this bucket
			TempTable tt = s1Buckets.get(bucket);
			if (tt == null) {
				tt = new TempTable(tx, s1Schema);
				s1Buckets.put(bucket, tt);
			}

			// Write record to TempTable
			UpdateScan scan = (UpdateScan) tt.open();
			scan.insert();
			for (String fldname : s1Schema.fields()) {
				scan.setVal(fldname, s1.getVal(fldname));
			}
			scan.close();
		}

		// Partition s2 into buckets
		s2.beforeFirst();
		while (s2.next()) {
			Constant key = s2.getVal(fldname2);
			int bucket = getBucket(key);

			// Get or create TempTable for this bucket
			TempTable tt = s2Buckets.get(bucket);
			if (tt == null) {
				tt = new TempTable(tx, s2Schema);
				s2Buckets.put(bucket, tt);
			}

			// Write record to TempTable
			UpdateScan scan = (UpdateScan) tt.open();
			scan.insert();
			for (String fldname : s2Schema.fields()) {
				scan.setVal(fldname, s2.getVal(fldname));
			}
			scan.close();
		}
	}

	private int getBucket(Constant key) {
		return key.hashCode() % HashIndex.NUM_BUCKETS;
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
