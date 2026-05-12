package com.example.parkingfinder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SearchHistoryProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.parkingfinder.searchprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/searches");
    private static final String FILE_NAME = "search_history.json";
    private static final int MAX_HISTORY = 3;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "name", "lat", "lon"});
        try {
            JSONArray history = readHistoryFromFile();
            for (int i = 0; i < history.length(); i++) {
                JSONObject obj = history.getJSONObject(i);
                cursor.addRow(new Object[]{i, obj.getString("name"), obj.getDouble("lat"), obj.getDouble("lon")});
            }
        } catch (Exception e) {
            Log.e("SearchHistoryProvider", "Query error", e);
        }
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (values == null) return null;

        try {
            JSONArray history = readHistoryFromFile();
            JSONObject newEntry = new JSONObject();
            newEntry.put("name", values.getAsString("name"));
            newEntry.put("lat", values.getAsDouble("lat"));
            newEntry.put("lon", values.getAsDouble("lon"));

            // Add to the beginning
            JSONArray updatedHistory = new JSONArray();
            updatedHistory.put(newEntry);

            // Add existing entries, maintaining max limit
            for (int i = 0; i < history.length() && updatedHistory.length() < MAX_HISTORY; i++) {
                JSONObject existing = history.getJSONObject(i);
                // Simple duplicate check by name
                if (!existing.getString("name").equals(newEntry.getString("name"))) {
                    updatedHistory.put(existing);
                }
            }

            writeHistoryToFile(updatedHistory);
            getContext().getContentResolver().notifyChange(uri, null);
            return uri;
        } catch (Exception e) {
            Log.e("SearchHistoryProvider", "Insert error", e);
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) { return 0; }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "vnd.android.cursor.dir/vnd.com.example.parkingfinder.search";
    }

    private JSONArray readHistoryFromFile() throws IOException, JSONException {
        File file = new File(getContext().getFilesDir(), FILE_NAME);
        if (!file.exists()) return new JSONArray();

        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String json = new String(data, StandardCharsets.UTF_8);
        return new JSONArray(json);
    }

    private void writeHistoryToFile(JSONArray array) throws IOException {
        File file = new File(getContext().getFilesDir(), FILE_NAME);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
        fos.close();
    }
}
