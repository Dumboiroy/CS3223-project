package simpledb.record;

import static java.sql.Types.INTEGER;
import simpledb.file.BlockId;
import simpledb.query.*;
import simpledb.tx.Transaction;

/**
 * Provides the abstraction of an arbitrarily large array
 * of records.
 * @author sciore
 */
public class TableScan implements UpdateScan {
   private Transaction tx;
   private Layout layout;
   private RecordPage rp;
   private String filename;
   private String tblname;
   private int currentslot;

   public TableScan(Transaction tx, String tblname, Layout layout) {
      this.tx = tx;
      this.layout = layout;
      this.tblname = tblname;
      filename = tblname + ".tbl";
      if (tx.size(filename) == 0)
         moveToNewBlock();
      else 
         moveToBlock(0);
   }

   // Methods that implement Scan

   public void beforeFirst() {
      moveToBlock(0);
   }

   public boolean next() {
      currentslot = rp.nextAfter(currentslot);
      while (currentslot < 0) {
         if (atLastBlock())
            return false;
         moveToBlock(rp.block().number()+1);
         currentslot = rp.nextAfter(currentslot);
      }
      return true;
   }

   public int getInt(String fldname) {
      return rp.getInt(currentslot, actualField(fldname));
   }

   public String getString(String fldname) {
      return rp.getString(currentslot, actualField(fldname));
   }

   public Constant getVal(String fldname) {
      String fld = actualField(fldname);
      if (layout.schema().type(fld) == INTEGER)
         return new Constant(rp.getInt(currentslot, fld));
      else
         return new Constant(rp.getString(currentslot, fld));
   }

   public boolean hasField(String fldname) {
      return actualField(fldname) != null;
   }

   /**
    * Translate a possibly qualified field reference to the field name stored
    * in this scan's layout, or null if it does not apply here. A reference
    * "t.a" matches only when t is this scan's table (base-table case) or when
    * the layout stores that exact qualified name (temp-table case); a bare
    * name resolves against the layout.
    */
   private String actualField(String fldname) {
      Schema sch = layout.schema();
      if (sch.hasFieldExact(fldname))
         return fldname;
      int dot = fldname.indexOf('.');
      if (dot >= 0) {
         String prefix = fldname.substring(0, dot);
         String suffix = fldname.substring(dot + 1);
         if (prefix.equals(tblname) && sch.hasFieldExact(suffix))
            return suffix;
         return null;
      }
      return sch.resolve(fldname);
   }

   public void close() {
      if (rp != null)
         tx.unpin(rp.block());
   }

   // Methods that implement UpdateScan

   public void setInt(String fldname, int val) {
      rp.setInt(currentslot, actualField(fldname), val);
   }

   public void setString(String fldname, String val) {
      rp.setString(currentslot, actualField(fldname), val);
   }

   public void setVal(String fldname, Constant val) {
      if (layout.schema().type(fldname) == INTEGER)
         setInt(fldname, val.asInt());
      else
         setString(fldname, val.asString());
   }

   public void insert() {
      currentslot = rp.insertAfter(currentslot);
      while (currentslot < 0) {
         if (atLastBlock()) 
            moveToNewBlock();
         else 
            moveToBlock(rp.block().number()+1);
         currentslot = rp.insertAfter(currentslot);
      }
   }

   public void delete() {
      rp.delete(currentslot);
   }

   public void moveToRid(RID rid) {
      close();
      BlockId blk = new BlockId(filename, rid.blockNumber());
      rp = new RecordPage(tx, blk, layout);
      currentslot = rid.slot();
   }

   public RID getRid() {
      return new RID(rp.block().number(), currentslot);
   }

   // Private auxiliary methods

   private void moveToBlock(int blknum) {
      close();
      BlockId blk = new BlockId(filename, blknum);
      rp = new RecordPage(tx, blk, layout);
      currentslot = -1;
   }

   private void moveToNewBlock() {
      close();
      BlockId blk = tx.append(filename);
      rp = new RecordPage(tx, blk, layout);
      rp.format();
      currentslot = -1;
   }

   private boolean atLastBlock() {
      return rp.block().number() == tx.size(filename) - 1;
   }
}
