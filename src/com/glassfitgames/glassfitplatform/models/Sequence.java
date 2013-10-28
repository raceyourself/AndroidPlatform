package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import com.roscopeco.ormdroid.Entity;

/**
 * Local db sequence.
 */
public class Sequence extends Entity {
	public String id;
	public int value;
	
	public Sequence() {		
	}
	
	public Sequence(String id) {
		this.id = id;
		this.value = 0;
	}
	
	public synchronized static int getNext(String id) {
		Sequence seq = query(Sequence.class).where(eql("id", id)).execute();
		if (seq == null) {
			seq = new Sequence(id);
		}	
		seq.value++;
		seq.save();
		return seq.value;
	}
}
