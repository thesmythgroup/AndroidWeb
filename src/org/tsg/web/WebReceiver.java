package org.tsg.web;

import android.os.Bundle;

/**
 * Implement to handle service results
 */
public interface WebReceiver {
    public void onReceiveResult(int resultCode, Bundle resultData);
}
