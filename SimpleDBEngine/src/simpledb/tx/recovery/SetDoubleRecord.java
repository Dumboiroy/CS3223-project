package simpledb.tx.recovery;

import simpledb.file.*;
import simpledb.log.LogMgr;
import simpledb.tx.Transaction;

/** The double-valued counterpart of {@link SetIntRecord}. */
public class SetDoubleRecord implements LogRecord {
   private int txnum, offset;
   private double val;
   private BlockId blk;

   public SetDoubleRecord(Page p) {
      int tpos = Integer.BYTES;
      txnum = p.getInt(tpos);
      int fpos = tpos + Integer.BYTES;
      String filename = p.getString(fpos);
      int bpos = fpos + Page.maxLength(filename.length());
      int blknum = p.getInt(bpos);
      blk = new BlockId(filename, blknum);
      int opos = bpos + Integer.BYTES;
      offset = p.getInt(opos);
      int vpos = opos + Integer.BYTES;
      val = p.getDouble(vpos);
   }

   public int op() {
      return SETDOUBLE;
   }

   public int txNumber() {
      return txnum;
   }

   public String toString() {
      return "<SETDOUBLE " + txnum + " " + blk + " " + offset + " " + val + ">";
   }

   public void undo(Transaction tx) {
      tx.pin(blk);
      tx.setDouble(blk, offset, val, false); // don't log the undo
      tx.unpin(blk);
   }

   public static int writeToLog(LogMgr lm, int txnum, BlockId blk, int offset, double val) {
      int tpos = Integer.BYTES;
      int fpos = tpos + Integer.BYTES;
      int bpos = fpos + Page.maxLength(blk.fileName().length());
      int opos = bpos + Integer.BYTES;
      int vpos = opos + Integer.BYTES;
      byte[] rec = new byte[vpos + Double.BYTES];
      Page p = new Page(rec);
      p.setInt(0, SETDOUBLE);
      p.setInt(tpos, txnum);
      p.setString(fpos, blk.fileName());
      p.setInt(bpos, blk.number());
      p.setInt(opos, offset);
      p.setDouble(vpos, val);
      return lm.append(rec);
   }
}
