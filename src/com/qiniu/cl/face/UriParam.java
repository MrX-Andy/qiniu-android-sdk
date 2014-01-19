package com.qiniu.cl.face;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Created by xc on 1/16/14.
 */
public class UriParam {
    public File file;
    public String path;
    public InputStream is;
    public String mimeType;
    public String name;
    public long size;
    public long lastMofified;

    public static UriParam uri2UriParam(Uri uri, Context context){
        UriParam p = new UriParam();
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
                    Log.i("upload", "cursor# " + name + ": " + value);
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
        /*
        else{
            p.path = uri.getPath();

            File f = p.path != null ? new File(p.path) : uri2TempFile(uri);
            try {
                p.is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            p.size = f.length();
            p.name = f.getName();
        }
        */

        return p;
    }

    public static UriParam file2UriParam(File file){
        UriParam p = new UriParam();
        p.name = file.getName();
        p.file = file;
        p.size = file.length();
        p.path = file.getPath();
        p.lastMofified = file.lastModified();
        return p;
    }

    public static File uri2File(Uri uri, Context context){
        try {
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
            File file = new File(new URI(uri.toString()));
            return file;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static File uri2TempFile(Uri uri, Context context) {
        FileOutputStream fs = null;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            File tempDir = new File(Environment.getExternalStorageDirectory()
                    .getPath() + File.separator + "qiniu");
            tempDir.mkdirs();
            File file = File.createTempFile("_" + UUID.randomUUID().toString(),
                    ".tmp", tempDir);
            fs = new FileOutputStream(file);
            byte[] b = new byte[1024 * 2];
            int l = -1;
            while ((l = is.read(b)) != -1) {
                fs.write(b, 0, l);
            }
            fs.flush();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
