# Overview
AndroidWeb is a package for handling concurrent requests with multiple receivers, includes an sqlite database that automatically caches results, and has a built-in content provider for retrieving results from within the app or allowing other apps to access the data if so permitted.

# Usage
Below are various examples of usage. This section needs to be expanded with more examples.

## AndroidManifest.xml
The following is required in AndroidManifest.xml for usage, placed inside <application/>. Refer to android documentation for additional configuration of services and providers.

```xml
<service android:name="org.tsg.web.WebService" />

<provider
    android:name="org.tsg.web.WebContentProvider"
    android:authorities="your.package.name" />
```

## Simple Request, Response

Demonstrates a simple request, response. Once request.send(this) is called, the call will continue even if the activity is destroyed (such as a screen rotation). If request.send(this) is called again and there is already a current request in process matching the url + params + headers of the request, the receiver will simply be added to the list of receivers waiting for the request to finish.

```java
public class MyActivity extends Activity implements WebReceiver {

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.main);
    }

    @Override
    public void onResume() {
        super.onResume();

        WebRequest request = new WebRequest("http://www.android.com/")
        request.send(this);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
        case WebService.STATUS_RUNNING:
            // TODO provide user feedback that service is running
            break;
        case WebService.STATUS_FINISHED:
            String key = resultData.getString(WebService.REQUEST_KEY);
            String response = WebService.getResponseString(this, key);
            handleResponse(response)
            break;
        case WebService.STATUS_ERROR:
            Exception e = (Exception) resultData.getSerializable(WebService.RESPONSE_EXCEPTION);
            e.printStackTrace();
            break;
        }
    }

    private void handleResponse(string response) {
        // TODO response is the html returned by the server
    }
}
```

## The WebRequest Object
TODO

### Setting Params
TODO

### Cache Expiration
TODO

### Content Type, Bytes or String
TODO

### Fake Data for Emulated WebService Calls
TODO

### DeveloperExtras Payload
TODO

### The WebRequest Key
TODO

## The WebReceiver Interface
TODO

## The WebService Class
TODO

## Example Service Class
TODO

