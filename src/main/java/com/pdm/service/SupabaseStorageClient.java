package com.pdm.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class SupabaseStorageClient {

    private String supabaseUrl;
    private String supabaseKey;

    public SupabaseStorageClient() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                props.load(input);
                this.supabaseUrl = props.getProperty("supabase.url");
                this.supabaseKey = props.getProperty("supabase.key");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean uploadFile(String bucket, String filePath, File localFile) {
        try {
            // e.g. https://xyz.supabase.co/storage/v1/object/pdm-vault/file.txt
            String encodedPath = filePath.replace(" ", "%20");
            URL url = new URL(supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
            conn.setRequestProperty("apikey", supabaseKey);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            try (OutputStream os = conn.getOutputStream(); FileInputStream fis = new FileInputStream(localFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                return true;
            } else {
                System.err.println("Upload failed. Code: " + responseCode + ", Message: " + conn.getResponseMessage());
                // Handle overwrite (Supabase returns 400 if exists without x-upsert header)
                if (responseCode == 400) {
                     return overwriteFile(bucket, encodedPath, localFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean overwriteFile(String bucket, String encodedPath, File localFile) {
        try {
            URL url = new URL(supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT"); // PUT for upsert/overwrite
            conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
            conn.setRequestProperty("apikey", supabaseKey);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            try (OutputStream os = conn.getOutputStream(); FileInputStream fis = new FileInputStream(localFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return conn.getResponseCode() == 200 || conn.getResponseCode() == 201;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean downloadFile(String bucket, String filePath, File destFile) {
        try {
            String encodedPath = filePath.replace(" ", "%20");
            URL url = new URL(supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
            conn.setRequestProperty("apikey", supabaseKey);

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                return true;
            } else {
                 System.err.println("Download failed. Code: " + conn.getResponseCode() + ", Message: " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteFile(String bucket, String filePath) {
        try {
            String encodedPath = filePath.replace(" ", "%20");
            URL url = new URL(supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
            conn.setRequestProperty("apikey", supabaseKey);

            int responseCode = conn.getResponseCode();
            return responseCode == 200 || responseCode == 204;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
