package com.qiniu.client.up;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by xc on 1/16/14.
 */
public class InputParam {
    public String path;
    public String mimeType;
    public String name;
    public long size;
    public long lastMofified;

    public static FileInputParam fileInputParam(String file) throws FileNotFoundException{
    	return fileInputParam(new File(file));
    }
    
    public static FileInputParam fileInputParam(File file) throws FileNotFoundException{
    	if(file == null || !file.isFile()){
    		throw new FileNotFoundException(file != null ? file.getAbsolutePath() : null);
    	}
    	FileInputParam p = new FileInputParam();
        p.name = file.getName();
        p.file = file;
        p.size = file.length();
        p.path = file.getPath();
        p.lastMofified = file.lastModified();
        return p;
    }
    
    public static StreamInputParam streamInputParam(String file) throws FileNotFoundException{
    	return streamInputParam(new File(file));
    }
    
    public static StreamInputParam streamInputParam(File file) throws FileNotFoundException{
    	StreamInputParam p = new StreamInputParam();
        p.name = file.getName();
        p.is = new FileInputStream(file);
        p.size = file.length();
        p.path = file.getPath();
        p.lastMofified = file.lastModified();
        return p;
    }
    
    public static StreamInputParam streamInputParam(Uri uri, Context context){
    	StreamInputParam p = new StreamInputParam();
    	if ("content".equalsIgnoreCase(uri.getScheme())){
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(uri, null, null, null, null);
            if(cursor.moveToFirst()){
                int cc = cursor.getColumnCount();
                for(int i=0; i < cc; i++){
                    String name = cursor.getColumnName(i);
                    String value = cursor.getString(i);
                    if("_display_name".equalsIgnoreCase(name)){
                        p.name = value;
                    }else if("_size".equalsIgnoreCase(name)){
                        p.size = cursor.getLong(i);
                    }else if("mime_type".equalsIgnoreCase(name)){
                        p.mimeType = value;
                    }else if("_data".equalsIgnoreCase(name)){
                        p.path = value;
                    }else if("last_modified".equalsIgnoreCase(name)){
                        p.lastMofified = cursor.getLong(i);
                    }
                }
            }
            if(cursor != null){
                cursor.close();
            }
        }
        if(p.size != 0){
            try {
                p.is = context.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return p;
    }
    
    public static FileInputParam fileInputParam(Uri uri, Context context) throws FileNotFoundException{
        if (!uri.toString().startsWith("file")) {
            String filePath;
            if (uri != null && "content".equals(uri.getScheme())) {
                Cursor cursor = context.getContentResolver().query(uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
                cursor.moveToFirst();
                filePath = cursor.getString(0);
                cursor.close();
            } else {
                filePath = uri.getPath();
            }
            uri = Uri.parse("file://" + filePath);
        }
        File file = null;
		try {
			file = new File(new URI(uri.toString()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
        return fileInputParam(file);
    }

    public static class FileInputParam extends InputParam{
    	public File file;
    }
    
    public static class StreamInputParam extends InputParam{
    	public InputStream is;
    }
    
}
