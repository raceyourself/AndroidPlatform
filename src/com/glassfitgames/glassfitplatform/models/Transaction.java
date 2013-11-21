package com.glassfitgames.glassfitplatform.models;

import java.nio.ByteBuffer;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;

/**
 * TODO
 */
public class Transaction extends Entity {

    // Globally unique compound key (orientation, device)
    public int transaction_id;
    public int device_id;
    
	@JsonIgnore
	public long id;  // for local database only
	
	public long ts; // timestamp of transaction (millisecs since 1970 in device time) TODO: convert to server time on sync
	public String transaction_type;  // e.g. base points, bonus, game purchase
	public String transaction_calc;  // e.g. 1 point * 5 seconds * 1.25 multiplier
	public String source_id;   // game/class that generated the transaction
	public long points_delta;  // points awarded/deducted
	public long points_balance; // sum of points_deltas with timestamps <= current record
	public int cash_delta;     // cash added/removed from GlassFit account
	public String currency;    // currency (e.g. GBP/USD) that the transaction was in
	
	@JsonIgnore
    public boolean dirty = false;
    public Date deleted_at = null;
	
	public Transaction() {}  // public constructor with no args required by ORMdroid
	
    public Transaction(String type, String calc, String source_id, int points_delta) {
        this.device_id = Device.self().getId();
        this.transaction_id = Sequence.getNext("transaction_id");
        this.transaction_type = type;
        this.transaction_calc = calc;
        this.source_id = source_id;
        this.points_delta = points_delta;
        this.ts = System.currentTimeMillis();
        this.dirty = true;
    }
	
    public static Transaction getLastTransaction() {
        return Entity.query(Transaction.class).orderBy("ts desc").limit(1).execute();
    }
	
	@Override
    public int save() {
        Transaction lastTransaction = getLastTransaction();
        if (lastTransaction == null) {
            this.points_balance = this.points_delta;
        } else {
            this.points_balance = lastTransaction.points_balance + this.points_delta;
        }

        if (id == 0) {
            ByteBuffer encodedId = ByteBuffer.allocate(8);
            encodedId.putInt(device_id);
            encodedId.putInt(transaction_id);
            encodedId.flip();
            this.id = encodedId.getLong();
        }

        return super.save();
    }
	
	public void delete() {     
        deleted_at = new Date();
        save();
    }
    
    public void flush() {
        if (deleted_at != null) {
            super.delete();     
            return;
        }
        if (dirty) {
            dirty = false;
            save();
        }
    }

}
