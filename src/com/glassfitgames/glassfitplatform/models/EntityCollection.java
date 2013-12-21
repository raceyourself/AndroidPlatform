package com.glassfitgames.glassfitplatform.models;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;
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

    public static EntityCollection getDefault() {
        return get("default");
    }
    
    public static EntityCollection get(String name) {
        EntityCollection c = query(EntityCollection.class).where(Query.eql("id", name)).execute();
        if (c == null) {
            c = new EntityCollection(name);
            c.save(); // Not stricly necessary unless we have a ttl/metadata
        }
        return c;
    }
    
    public static List<String> getCollections(CollectionEntity entity) {
        List<Association> associations = query(Association.class).where(Query.eql("item_id", entity.getPrimaryKeyValue().toString())).executeMulti();
        List<String> collections = new ArrayList<String>(associations.size());
        for (Association association : associations) {
            collections.add(association.collection_id);
        }
        return collections;
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
    
    public <T extends CollectionEntity> void add(T item) {
        item.storeIn(this.id);
    }
    
    public <T extends CollectionEntity> void add(List<T> items) {
        for (T item : items) {
            add(item);
        }        
    }
    
    public <T extends CollectionEntity> void replace(T item, Class<T> type) {
        List<T> list = new LinkedList<T>();
        list.add(item);
        replace(list, type);
    }
    
    public <T extends CollectionEntity> void replace(List<T> items, Class<T> type) {
        
        SQLiteDatabase db = ORMDroidApplication.getInstance().getDatabase();
            
        try {
            ORMDroidApplication.getInstance().getWriteLock();
            db.beginTransaction();    
            List<T> orphans = query(type).where(onlyInCollection()).executeMulti();
            // TODO: orphans.removeAll(items); after all items have had their id generated?
            for (T orphan : orphans) {
                orphan.erase();
            }
            
            List<Association> associations = query(Association.class).where(Query.eql("collection_id", this.id)).executeMulti();
            for (Association association : associations) {
                association.delete();
            }
    
            add(items);        
            db.setTransactionSuccessful();
        } catch (InterruptedException e) {
            throw new RuntimeException("SyncHelper: Interrupted whilst waiting for database");
        } finally {
            db.endTransaction();
            ORMDroidApplication.getInstance().releaseWriteLock();
        }
    }
    
    public <T extends CollectionEntity> T getItem(Class<T> type) {
        return query(type).where(inCollection()).execute();
    }
    
    public <T extends CollectionEntity> List<T> getItems(Class<T> type) {
        return query(type).where(inCollection()).executeMulti();
    }
    
    public String inCollection() {
        return "id IN ( SELECT item_id FROM associations where collection_id = \"" + this.id + "\")";
    }
    
    public String onlyInCollection() {
        return inCollection() + " AND id NOT IN ( SELECT item_id FROM associations where collection_id != \"" + this.id + "\")";
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
	
    public static abstract class CollectionEntity extends Entity {
        {
            // Migration from Entity->CollectionEntity:
            if (!migrated) {
                migrated = true;
                Log.i("CollectionEntity", "Migrating " + this.getClass().getSimpleName());
                query(Association.class).execute(); // Make sure the associations table is created
                migrateDefaults(this.getClass());
            }
        }
        private static boolean migrated = false;
        
        private static <T extends CollectionEntity> void migrateDefaults(Class<T> type) {
            // Select from associations simply to ensure associations table has been created..
            query(Association.class).where("id = 0").execute();
            List<T> result = query(type).where("id NOT IN ( SELECT item_id FROM associations )").executeMulti();
            for (T object : result) {
                Association association = new Association("default", object.getPrimaryKeyValue().toString());
                association.save();                
            }
        }
        
        @Override
        public int save() {
            return storeIn("default");
        }

        public int storeIn(String collection) {
            int ret = super.save();
            Association association = new Association(collection, this.getPrimaryKeyValue().toString());
            association.save();
            return ret;
        }
        
        public void erase() {
            // Do not allow soft deleting
            super.delete();
        }
    }
}
