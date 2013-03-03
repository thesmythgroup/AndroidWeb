package org.tsg.web;

import android.content.Context;
import android.os.Bundle;
import java.io.InputStream;

public class WebResponse {

	private Context mContext;
	private int mResultCode;
	private Bundle mResultData;

	public WebResponse(Context context, int resultCode, Bundle resultData) {
		mContext = context;
		mResultCode = resultCode;
		mResultData = resultData;
	}

	public Context getContext() {
		return mContext;
	}

	public int getStatus() {
		return mResultCode;
	}

	public boolean isRunning() {
		return mResultCode == WebReceiver.STATUS_RUNNING;
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