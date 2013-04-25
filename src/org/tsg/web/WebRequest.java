package org.tsg.web;

import java.io.File;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class WebRequest implements Parcelable {

	String mUrl;
	String mBody;
	File mFile;
	Bundle mParams;
	Bundle mHeaders;
	Integer mMethod;
	Integer mCacheTimeValue;
	Integer mCacheTimeType;
	Bundle mDeveloperExtras;
	String mFakeData;

	public WebRequest() {
		this(null);
	}

	public WebRequest(String url) {
		mUrl = url;
		mMethod = WebService.METHOD_GET;
		mHeaders = new Bundle();
		mCacheTimeValue = 10;
		mCacheTimeType = WebService.TIME_SECOND;
		mFile = null;
	}

	public WebRequest(String url, String body, File file, Bundle params, Bundle headers, Integer method, Integer cacheTimeValue, Integer cacheTimeType, Bundle developerExtras,
			String fakeData) {

		mUrl = url;
		mBody = body;
		mFile = file;
		mParams = params;
		mHeaders = headers;
		mMethod = method;
		mCacheTimeValue = cacheTimeValue;
		mCacheTimeType = cacheTimeType;
		mDeveloperExtras = developerExtras;
		mFakeData = fakeData;
	}

	/**
	 * Gets key based on values of mUrl, mParams, and mHeaders. This can be used
	 * outside the scope of making a web service call to query the cache directly
	 * for the last result if available.
	 * 
	 * @return
	 */
	public String getKey() {
		return String.valueOf((mUrl + String.valueOf(mParams) + String.valueOf(mHeaders) + String.valueOf(mBody)).hashCode());
	}

	/**
	 * String designating url to load.
	 * 
	 * @param url
	 */
	public void setUrl(String url) {
		mUrl = url;
	}

	public String getUrl() {
		return mUrl;
	}

	/**
	 * String containing raw body to be used for performing a POST. If body and
	 * params is set for a request, body takes precedence.
	 * 
	 * @param body
	 */
	public void setBody(String body) {
		mBody = body;
	}

	/**
	 * Stream chunked data to server from given file. Helpful to avoid memory
	 * overhead of large data sets.
	 * 
	 * @param file
	 */
	public void setBody(File file) {
		mFile = file;
		setContentType("application/x-www-form-urlencoded");
	}

	public String getBody() {
		return mBody;
	}

	/**
	 * Bundle containing request parameters. Values are passed through
	 * String.valueOf, so this can be any Parcelable object implementing an
	 * appropriate toString method.
	 * 
	 * @param params
	 */
	public void setParams(Bundle params) {
		mParams = params;
	}

	/**
	 * Bundle containing request headers. Values are passed through
	 * String.valueOf, so this can be any Parcelable object implementing an
	 * appropriate toString method.
	 * 
	 * If headers of current request are not empty. This will replace those
	 * headers and produce a warning.
	 * 
	 * @param headers
	 */
	public void setHeaders(Bundle headers) {
		if (!mHeaders.isEmpty()) {
			WebService.log(Log.WARN, "Call to `setHeaders` is replacing non-empty headers bundle! " + mHeaders);
		}
		mHeaders = headers;
	}

	/**
	 * Service.METHOD_[TYPE] for GET, PUT, POST, DELETE
	 * 
	 * @param method
	 */
	public void setMethod(Integer method) {
		mMethod = method;
	}

	/**
	 * Helper method for setting `Content-Type` header.
	 * 
	 * @param contentType
	 */
	public void setContentType(String... contentTypes) {
		for (String contentType : contentTypes) {
			updateHeader("Content-Type", contentType);
		}
	}

	/**
	 * Used internally to identify request content type after request finishes for
	 * storage in cache db.
	 * 
	 * @return
	 */
	String getContentType() {
		if (mHeaders != null) {
			return mHeaders.getString("Content-Type");
		}
		return null;
	}

	/**
	 * Convenience method for setting `Accept` header.
	 * 
	 * @param accepts
	 */
	public void setAccept(String... accepts) {
		for (String accept : accepts) {
			updateHeader("Accept", accept);
		}
	}

	private void updateHeader(String key, String value) {
		String s = mHeaders.getString(key);

		if (s == null) {
			mHeaders.putString(key, value);
			return;
		}

		if (s.indexOf(value) != 0) {
			WebService.log(Log.WARN, key + " already set in header of request. Skipping update");
			return;
		}

		s += ";" + value;
		mHeaders.putString(key, s);
	}

	/**
	 * Cache timeout value
	 * 
	 * @param cacheTimeValue
	 */
	public void setCacheTimeValue(Integer cacheTimeValue) {
		mCacheTimeValue = cacheTimeValue;
	}

	/**
	 * SERVICE.TIME_[TYPE] for SECOND, MINUTE, HOUR, etc
	 * 
	 * @param cacheTimeType
	 */
	public void setCacheTimeType(Integer cacheTimeType) {
		mCacheTimeType = cacheTimeType;
	}

	/**
	 * Convenience method to set cache validity
	 * 
	 * @param cacheTimeValue
	 * @param cacheTimeType
	 */
	public void setCacheTime(Integer cacheTimeValue, Integer cacheTimeType) {
		mCacheTimeValue = cacheTimeValue;
		mCacheTimeType = cacheTimeType;
	}

	/**
	 * Bundle containing extra data that will be passed back into resultData in
	 * Receiver.onReceiveResult method. Keep the size of this minimal to avoid
	 * slow down issues.
	 * 
	 * @param developerExtras
	 */
	public void setDeveloperExtras(Bundle developerExtras) {
		mDeveloperExtras = developerExtras;
	}

	/**
	 * Simulate response from server with following data. This data will be cached
	 * by Service. No actual network call is ever made.
	 * 
	 * @param fakeData
	 */
	public void setFakeData(String fakeData) {
		mFakeData = fakeData;
	}

	@Override
	public String toString() {
		return String.format("Request<%s,%s,%s,%s,%s,%s,%s,%s>", mUrl, mParams, mHeaders, mMethod, mCacheTimeValue, mCacheTimeType, mDeveloperExtras, mFakeData);
	}

	/* Convenience wrappers for WebService.helper methods */

	/**
	 * 
	 * @param context
	 *          Context must implement WebReceiver
	 * @return request key
	 */
	public String send(Context context) {
		return WebService.helper(context, this);
	}

	public WebResponse send2(Context context) {

		final WebResponse resp = new WebResponse(context.getApplicationContext());

		WebReceiver recv = new WebReceiver() {
			public void onReceiveResult(int resultCode, Bundle resultData) {
				synchronized (resp) {
					resp.setResultCode(resultCode);
					resp.setResultData(resultData);

					switch (resultCode) {
					case STATUS_FINISHED:
					case STATUS_ERROR:
						resp.notifyAll();
						break;
					}
				}
			}
		};

		String key = WebService.helper(context, recv, this);
		Bundle data = new Bundle();
		data.putString(WebReceiver.REQUEST_KEY, key);
		resp.setResultData(data);
		return resp;
	}

	public String send(Context context, WebReceiver receiver) {
		return WebService.helper(context, receiver, this);
	}

	public String send(Context context, Handler handler, WebReceiver receiver) {
		return WebService.helper(context, handler, receiver, this);
	}

	/* Parcelable Implementation */

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mUrl);
		dest.writeString(mBody);
		dest.writeSerializable(mFile);
		dest.writeBundle(mParams);
		dest.writeBundle(mHeaders);
		dest.writeInt(mMethod);
		dest.writeInt(mCacheTimeValue);
		dest.writeInt(mCacheTimeType);
		dest.writeBundle(mDeveloperExtras);
		dest.writeString(mFakeData);
	}

	public static final Parcelable.Creator<WebRequest> CREATOR = new Parcelable.Creator<WebRequest>() {
		@Override
		public WebRequest createFromParcel(Parcel source) {
			return new WebRequest(source.readString(), source.readString(), (File) source.readSerializable(), source.readBundle(), source.readBundle(), source.readInt(),
					source.readInt(), source.readInt(), source.readBundle(), source.readString());
		}

		@Override
		public WebRequest[] newArray(int size) {
			return null;
		}
	};

}
