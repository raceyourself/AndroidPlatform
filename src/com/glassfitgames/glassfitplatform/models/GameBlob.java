package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

public class GameBlob extends Entity {

	public String id;
	public long updated_ts;
	public String entity_tag;
	
	public GameBlob() {		
	}	
	public GameBlob(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public long getUpdatedTimestamp() {
		return updated_ts;
	}
	public String getEntityTag() {
		return entity_tag;
	}
	
	public byte[] getBlob() {		
		File file = new File(getBlobPath(), id);
		
		try {
			return FileUtils.readFileToByteArray(file);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean setBlob(byte[] data) {
		String newEtag = calculateEntityTag(data);
		if (!newEtag.equals(entity_tag)) {
			updated_ts = System.currentTimeMillis();
			entity_tag = newEtag;
			// TODO: Sync with server now or sync later?
			// NOTE: We do not have global time. What happens if the device's timebase changes compared to server time? 
		}
		
		File blobPath = getBlobPath();
		blobPath.mkdirs();
		File file = new File(blobPath, id);
		try {
			FileUtils.writeByteArrayToFile(file, data);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static GameBlob loadBlob(String id) {
		return query(GameBlob.class).where(eql("id",id)).execute();
	}
	
	public static byte[] loadDefaultBlob(String id) {
		try {
			AssetManager assets = ORMDroidApplication.getSingleton().getApplicationContext().getAssets();
			AssetFileDescriptor fd = assets.openFd("blob/" + id);
			if (fd == null || fd.getLength() == AssetFileDescriptor.UNKNOWN_LENGTH) return new byte[0];
			
			InputStream asset = assets.open("blob/" + id);			
			byte[] data = new byte[(int)fd.getLength()];
			IOUtils.readFully(asset, data);
			return data;
		} catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public static void eraseBlob(String id) {
		GameBlob gb = loadBlob(id);
		if (gb != null) gb.delete();
		
		File file = new File(getBlobPath(), id);
		file.delete();
	}
	
	public static List<GameBlob> getDatabaseBlobs() {
		return query(GameBlob.class).executeMulti();		
	}
	
	private static File getBlobPath() {
		Context context = ORMDroidApplication.getSingleton().getApplicationContext();
		File blobPath = new File(context.getFilesDir(), "blobstore");
		return blobPath;
	}
	
	private static String calculateEntityTag(byte[] data) {
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(data);

			return String.format("%032x", new BigInteger(1, digest.digest()));
		} catch (NoSuchAlgorithmException e) {
			// Should never happen
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
}
