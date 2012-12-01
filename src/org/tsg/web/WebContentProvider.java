package org.tsg.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class WebContentProvider extends ContentProvider {

	static int MAX_CACHE_SIZE = 5000;
	static String PACKAGE_NAME;

	public static Uri getDefaultAuthority(Context context) {
		if (PACKAGE_NAME == null) {
			// fix onResume in complex situations where context is lost
			// by caching package name
			PACKAGE_NAME = context.getApplicationContext().getPackageName();
		}
		return Uri.parse("content://" + PACKAGE_NAME);
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public String getType(Uri uri) {
		// TODO return proper types
		return "image/png";
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) {
		File f = new File(getContext().getFilesDir(), uri.getLastPathSegment());
		ParcelFileDescriptor pfd;

		try {
			pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		return pfd;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String key = uri.getLastPathSegment();
		String uuid = values.getAsString("uuid");
		String type = values.getAsString("type");
		byte[] response = values.getAsByteArray("response");
		String contentType = values.getAsString("contentType");
		Database.getInstance(getContext()).put(key, uuid, type, response, contentType);
		return uri;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor c = Database.getInstance(getContext()).getCursor(uri.getLastPathSegment());
		return c;
	}

	/**
	 * Cache Database to store responses from http client.
	 */
	public static class Database extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "serviceResponseCache";
		private static final int DATABASE_VERSION = 1;
		private static final String TABLE_CACHE = "cache";
		private static final String COL_KEY = "_ID";
		private static final String COL_UUID = "uuid";
		private static final String COL_RESPONSE = "response";
		private static final String COL_DATA = "_data";
		private static final String COL_MIME_TYPE = "mime_type";
		private static final String COL_TYPE = "type";
		private static final String COL_TIMESTAMP = "timestamp";

		private static Database instance;
		private SQLiteDatabase mDatabase;
		private Context mContext;

		private Database(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		public static synchronized Database getInstance(Context context) {
			if (instance == null) {
				instance = new Database(context.getApplicationContext());
				instance.open();
			}
			return instance;
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			database.execSQL("create table cache (_ID integer primary key, uuid text, response blob, _data text, mime_type text, type text, timestamp date)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			database.execSQL("drop table if exists cache");
		}

		public void open() {
			mDatabase = getWritableDatabase();
		}

		@Override
		public synchronized void close() {
			if (mDatabase != null) {
				mDatabase.close();
			}
			super.close();
		}

		/**
		 * TODO implement
		 */
		private void prune() {
			String sql = "select * from " + TABLE_CACHE + " order by " + COL_TIMESTAMP;
			Cursor cursor = mDatabase.rawQuery(sql, null);

			int size = 0;
			// while (cursor.moveToNext()) {
			// int i = cursor.getColumnIndex(COL_DATA);
			// byte[] bytes = cursor.getBlob(i);
			// if (bytes != null)
			// size += bytes.length;
			// }

			cursor.close();

			WebService.log(Log.INFO, "total col_data size:", size);
		}

		/**
		 * Either inserts or updates record based on whether PK key exists.
		 * 
		 * @param key
		 * @param uuid
		 * @param response
		 */
		public void put(String key, String uuid, String type, byte[] response, String contentType) {
			// TODO check size of response, if too big, store on file system and
			// save reference to record
			// TODO write as rawQuery and use builtin datetime() for method
			// contains

			String data = null;

			if (contentType.contains("image") || response.length > 1000000) {
				try {
					File f = new File(mContext.getFilesDir(), key);
					data = f.getAbsolutePath();

					FileOutputStream fos = new FileOutputStream(f);// mContext.openFileOutput(key,
																													// MODE_PRIVATE);
					fos.write(response);
					fos.close();
					response = null;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			ContentValues values = new ContentValues();
			values.put(COL_KEY, key);
			values.put(COL_UUID, uuid);
			values.put(COL_RESPONSE, response);
			values.put(COL_DATA, data);
			values.put(COL_TYPE, type);
			values.put(COL_TIMESTAMP, getTimeStamp());
			values.put(COL_MIME_TYPE, contentType);

			if (contains(key)) {
				mDatabase.update(TABLE_CACHE, values, "_ID=?", new String[] { key });
			} else {
				mDatabase.insert(TABLE_CACHE, null, values);
			}
		}

		public boolean contains(String key) {
			return contains(key, null, null);
		}

		/**
		 * Check for record with PK key. If timeValue is specified, this is used to
		 * determine if record is still valid.
		 * 
		 * @param key
		 * @param timeValue
		 * @param timeType
		 * @return
		 */
		public boolean contains(String key, Integer timeValue, Integer timeType) {
			String selectValues = "count(*)";
			if (timeValue != null)
				selectValues = "(datetime(timestamp, '+" + timeValue + " " + getTimeType(timeType) + "') > '" + getTimeStamp() + "')";

			String query = "select " + selectValues + " from " + TABLE_CACHE + " where _ID=?";
			Cursor cursor = mDatabase.rawQuery(query, new String[] { key });

			if (!cursor.moveToFirst()) {
				cursor.close();
				return false;
			}

			int i = cursor.getInt(0);
			cursor.close();

			return i == 1 ? true : false;
		}

		/**
		 * Get byte[] cache of last response.
		 * 
		 * @param key
		 * @return
		 */
		public Bundle get(String key) {

			Bundle b = new Bundle();
			Cursor cursor = mDatabase.query(TABLE_CACHE, new String[] { COL_RESPONSE, COL_TYPE }, COL_KEY + "=?", new String[] { key }, null, null, null);
			if (!cursor.moveToFirst()) {
				cursor.close();
				return null;
			}

			b.putByteArray("responseBytes", cursor.getBlob(0));
			b.putInt("contentType", cursor.getInt(1));
			cursor.close();
			return b;
		}

		public Cursor getCursor(String key) {
			// prune();
			return mDatabase.query(TABLE_CACHE, new String[] { COL_RESPONSE, COL_TYPE, COL_DATA, COL_MIME_TYPE }, COL_KEY + "=?", new String[] { key }, null, null, null);
		}

		/**
		 * coerce type to string for use in sql
		 * 
		 * @param type
		 * @return
		 */
		private String getTimeType(int type) {
			switch (type) {
			case WebService.TIME_SECOND:
				return "seconds";
			case WebService.TIME_MINUTE:
				return "minutes";
			case WebService.TIME_HOUR:
				return "hours";
			case WebService.TIME_DAY:
				return "days";
			case WebService.TIME_MONTH:
				return "months";
			case WebService.TIME_YEAR:
				return "years";
			}

			return null;
		}

		private String getTimeStamp() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			Date date = new Date();
			return dateFormat.format(date);
		}
	}
}
