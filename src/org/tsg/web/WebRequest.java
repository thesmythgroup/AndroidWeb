package org.tsg.web;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

public class WebRequest implements Parcelable {

    String mUrl;
    Bundle mParams;
    Bundle mHeaders;
    Integer mMethod;
    Integer mContentType;
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
        mContentType = WebService.CONTENT_AUTO;
        mCacheTimeValue = 10;
        mCacheTimeType = WebService.TIME_SECOND;
    }

    public WebRequest(String url, Bundle params, Bundle headers, Integer method, Integer contentType, Integer cacheTimeValue, Integer cacheTimeType, Bundle developerExtras,
            String fakeData) {

        mUrl = url;
        mParams = params;
        mHeaders = headers;
        mMethod = method;
        mContentType = contentType;
        mCacheTimeValue = cacheTimeValue;
        mCacheTimeType = cacheTimeType;
        mDeveloperExtras = developerExtras;
        mFakeData = fakeData;
    }

    /**
     * Generates key based on values of mUrl, mParams, and mHeaders. This can be
     * used outside the scope of making a web service call to query the cache
     * directly for the last result if available.
     * 
     * @return
     */
    public String getKey() {
        return String.valueOf((mUrl + String.valueOf(mParams) + String.valueOf(mHeaders)).hashCode());
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
     * @param headers
     */
    public void setHeaders(Bundle headers) {
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
     * Service.CONTENT_[TYPE] for AUTO, RAW, STRING
     * 
     * @param contentType
     */
    public void setContentType(Integer contentType) {
        mContentType = contentType;
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
     * Simulate response from server with following data. This data will be
     * cached by Service. No actual network call is ever made.
     * 
     * @param fakeData
     */
    public void setFakeData(String fakeData) {
        mFakeData = fakeData;
    }

    @Override
    public String toString() {
        return String.format("Request<%s,%s,%s,%s,%s,%s,%s,%s,%s>", mUrl, mParams, mHeaders, mMethod, mContentType, mCacheTimeValue, mCacheTimeType, mDeveloperExtras, mFakeData);
    }

    /* Convenience wrappers for WebService.helper methods */

    /**
     * 
     * @param context
     *            Context must implement WebReceiver
     * @return request key
     */
    public String send(Context context) {
        return WebService.helper(context, this);
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
        dest.writeBundle(mParams);
        dest.writeBundle(mHeaders);
        dest.writeInt(mMethod);
        dest.writeInt(mContentType);
        dest.writeInt(mCacheTimeValue);
        dest.writeInt(mCacheTimeType);
        dest.writeBundle(mDeveloperExtras);
        dest.writeString(mFakeData);
    }

    public static final Parcelable.Creator<WebRequest> CREATOR = new Parcelable.Creator<WebRequest>() {
        @Override
        public WebRequest createFromParcel(Parcel source) {
            return new WebRequest(source.readString(), source.readBundle(), source.readBundle(), source.readInt(), source.readInt(), source.readInt(), source.readInt(),
                    source.readBundle(), source.readString());
        }

        @Override
        public WebRequest[] newArray(int size) {
            return null;
        }
    };

}
