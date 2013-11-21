package com.glassfitgames.glassfitplatform.models;

import java.util.LinkedList;
import java.util.List;

import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;
import com.roscopeco.ormdroid.Table;

/**
 * An abstract named and expirable collection of foreign keys.
 * 
 * Consistency model: Client can trigger fetch.
 *                    Server can replace collection.
 */
public class EntityCollection extends Entity {

    public String id; // Collection name/source
    public long ttl = 0; // Time to live (ms timestamp) Optional.

    public EntityCollection() {
        // Internal/library use only
    }
    public EntityCollection(String name) {
        this.id = name;
    }
    
    public static EntityCollection get(String name) {
        // BLOCKING: What about default namespaced objects?
        if (true) throw new RuntimeException("Fix blocking issue before using");
        EntityCollection c = query(EntityCollection.class).where(Query.eql("id", name)).execute();
        if (c == null) {
            c = new EntityCollection(name);
            c.save(); // Not stricly necessary unless we have a ttl/metadata
        }
        return c;
    }
    
    public String getName() {
        return id;
    }
    
    public void expireIn(int seconds) {
        this.ttl = System.currentTimeMillis() + seconds * 1000;
        save();
    }
    
    public boolean hasExpired() {
        if (ttl <= 0) return false;
        return (System.currentTimeMillis() >= ttl);
    }    
    
    public <T extends Entity> void add(T item) {
        item.save();
        Association association = new Association(this.id, item.getPrimaryKeyValue().toString());
        association.save();        
    }
    
    public <T extends Entity> void add(List<T> items) {
        for (T item : items) {
            add(item);
        }        
    }
    
    public <T extends Entity> void replace(T item, Class<T> type) {
        List<T> list = new LinkedList<T>();
        list.add(item);
        replace(list, type);
    }
    
    public <T extends Entity> void replace(List<T> items, Class<T> type) {
        // TODO: Surround with transaction?
        List<T> orphans = query(type).where(onlyInCollection()).executeMulti();
        // TODO: orphans.removeAll(items); after all items have had their id generated
        for (T orphan : orphans) {
            orphan.delete();
            // Soft-delete items will eventually flush, attempt to force now?
        }
        
        List<Association> associations = query(Association.class).where(Query.eql("collection_id", this.id)).executeMulti();
        for (Association association : associations) {
            association.delete();
        }

        add(items);        
    }
    
    public <T extends Entity> T getItem(Class<T> type) {
        return query(type).where(inCollection()).execute();
    }
    
    public <T extends Entity> List<T> getItems(Class<T> type) {
        return query(type).where(inCollection()).executeMulti();
    }
    
    public String inCollection() {
        return "id IN ( SELECT item_id FROM associations where collection_id = \"" + this.id + "\")";
    }
    
    public String onlyInCollection() {
        return inCollection() + "AND id NOT IN ( SELECT item_id FROM associations where collection_id != \"" + this.id + "\")";
    }
    @Table(name = "associations")
    public static class Association extends Entity {
        public String id;
        public String collection_id;
        public String item_id;
        
        public Association() {            
            // Internal/library use only
        }
        
        public Association(String collectionId, String itemId) {
            this.collection_id = collectionId;
            this.item_id = itemId;
            this.id = collection_id + '-' + item_id;
        }        
    }
	
}
