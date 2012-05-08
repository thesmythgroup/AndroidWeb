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
        case STATUS_RUNNING:
            // TODO provide user feedback that service is running
            break;
        case STATUS_FINISHED:
            String key = resultData.getString(WebService.REQUEST_KEY);
            String response = WebService.getResponseString(this, key);
            handleResponse(response)
            break;
        case STATUS_ERROR:
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
The WebRequest Object features has methods for setting the following properties of a request.

* Url - such as http://www.android.com/
* Params - a Bundle where String.ValueOf will be called on any values put in
* Headers - also a Bundle where String.ValueOf will be called on any values
* Method - GET, POST, The following are on my TODO PUT, DELETE
* ContentType - String or Raw
* CacheTimeValue - integer describing length of cache
* CacheTimeType - constant defining type of time, SECONDS, MINUTES, HOUR, etc
* DeveloperExtras - a Bundle that can be retrieved from resultData bundle in receiver
* FakeData - a string that when set, skips the actual network call and passes this onto the receiver. Note, caching still applies!

Additionally, to execute a request, call send(Context context, WebReceiver receiver)

### Setting Params

```java
public class MyRequest {
    public MyRequest(Context context, WebReceiver receiver) {
        WebRequest request = new WebRequest("http://www.someservice.com/getJSON")

        Bundle params = new Bundle();
        params.putString("q", "kittens");
        params.putInt("n", 22); // remember String.ValueOf will be called on param values

        request.setParams(params);
        request.send(context, receiver)
    }
}
```

### Cache Expiration
TODO

### Content Type, Bytes or String
TODO explain how the backend works a bit more

```java
public class MyRequest {
    public MyRequest(Context context, WebReceiver receiver) {
        // fetching an image
        WebRequest request = new WebRequest();
        // note, you can always set the url later
        request.setUrl("http://www.someimageplace.com/kittens.jpg")
        request.setContentType(WebService.CONTENT_RAW); // important!
        request.send(context, receiver);
        // also note, images fetched will be cached to disk with a reference to it
        // in the sqlite database. This is compliant with use of a ContentProvider
        // so this image can be shared with other apps if so permitted by the provider
        // in the AndroidManifest.xml
    }
}

```

### Fake Data for Emulated WebService Calls
TODO

### DeveloperExtras Payload
TODO

### The WebRequest Key
TODO

## The WebReceiver Interface
TODO

## The WebService Class
There are a number of important static methods on this class for controlling how AndroidWeb will function.

### Configuration
The following three items, enableLogging, enableLongCache, and setPoolSize should be configured on your Application instance
to assure they are called before your activities, and thus services, are launched.

```java
public class App extends Application {
    @Override
    public void onCreate() {
        WebService.enableLogging();
        WebService.enableLongCache();
        WebService.setPoolSize(3);
    }
}
```

#### enableLogging()
This will log various output under the tag "WebService".

#### enableLongCache()
Automatically set all request cache time outs to 999 years. This can be useful if planning to go off-line
or simply reduce time spent waiting for services to load.

#### setPoolSize(int size)
WebService executes requests on an executor fixed thread pool. The current default size is 3.

### Removing Receivers
There may be cases where you no longer want to receive the result of a call while still having the call finish.
There are two methods for this.

#### removeReceiver(WebReceiver receiver)
Given that a single receiver can be attached to multiple requests, this will remove the receiver from any
requests it is currently attached to.

#### clearReceivers()
This will clear all receivers from all pending requests.

### Getting a Result
You can retrieve any result as a String or as bytes. In fact, response strings are stored in the database in the original byte[] received.
Regardless of this fact, it is still import to mark a request as CONTENT_RAW when downloading an image.

#### String getResponseString(Context context, String responseKey)
TODO

#### byte[] getResponseBytes(Context context, String responseKey)
TODO

## Example Singleton Service Class
TODO

## Executing Request from a Thread
While the requests themselves are executed in their own fixed thread pool, the receiver defaults to the original calling thread.
If this is happening in a separate thread (that is, not on the UI thread), and your receiver needs access to the UI thread, then
you will want to create an instance of Handler on the UI thread and then pass this instance into the appropriate WebRequest.send
method signature.

TODO provide example

