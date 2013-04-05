package org.tsg.web;

import android.content.Context;
import android.os.Bundle;
import java.io.InputStream;
import java.io.Serializable;

public class WebResponse {

	private Context mContext;
	private int mResultCode;
	private Bundle mResultData;
	private Bundle mSerializables;

	public WebResponse(Context context) {
		this(context, 0, null);
	}

	public WebResponse(Context context, int resultCode, Bundle resultData) {
		mContext = context;
		mResultCode = resultCode;
		mResultData = resultData;
		mSerializables = new Bundle();
	}

	public <T extends Serializable> T get(String key) {
		return (T) mSerializables.getSerializable(key);
	}

	public void set(String key, Serializable obj) {
		mSerializables.putSerializable(key, obj);
	}

	public synchronized void join() {
		while(isRunning()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void setResultCode(int resultCode) {
		mResultCode = resultCode;
		if (!isRunning()) {
			notifyAll();
		}
	}

	public void setResultData(Bundle resultData) {
		mResultData = resultData;
	}

	public Context getContext() {
		return mContext;
	}

	public int getStatus() {
		return mResultCode;
	}

	public boolean isRunning() {
		// return mResultCode == WebReceiver.STATUS_RUNNING;
		return !isFinished() && !isException();
	}

	public boolean isFinished() {
		return mResultCode == WebReceiver.STATUS_FINISHED;
	}

	public boolean isException() {
		return mResultCode == WebReceiver.STATUS_ERROR;
	}

	public String getRequestKey() {
		return mResultData.getString(WebReceiver.REQUEST_KEY);
	}

	public String getString() {
		return WebService.getResponseString(mContext, getRequestKey());
	}

	public byte[] getBytes() {
		return WebService.getResponseBytes(mContext, getRequestKey());
	}

	public InputStream getStream() {
		return WebService.getResponseStream(mContext, getRequestKey());
	}

	public Exception getException() {
		return (Exception) mResultData.getSerializable(WebReceiver.RESPONSE_EXCEPTION);
	}

}