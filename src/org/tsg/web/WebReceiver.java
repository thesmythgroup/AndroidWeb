package org.tsg.web;

import android.os.Bundle;

/**
 * Implement to handle service results
 * 
 * @author Daniel Skinner <daniel@dasa.cc>
 * 
 */
public interface WebReceiver {
	public static final int STATUS_CREATED = 0;
	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_FINISHED = 2;
	public static final int STATUS_ERROR = 3;

	// keys for Bundle resultData sent to receiver
	public static final String REQUEST_KEY = "requestKey";
	public static final String RESPONSE_EXCEPTION = "exception";
	public static final String RESPONSE_CODE = "responseCode";
	public static final String RESPONSE_MESSAGE = "responseMessage";
	public static final String DEVELOPER_EXTRAS = "developerExtras";

	public void onReceiveResult(int resultCode, Bundle resultData);
}
