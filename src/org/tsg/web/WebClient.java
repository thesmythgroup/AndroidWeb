package org.tsg.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.os.Bundle;

/**
 * This is the actual client that builds the request, makes the service call,
 * and generates the result. Should extend this for implementing things such as
 * cookie storage or changing default values of the request.
 * 
 * @author Daniel Skinner <daniel@dasa.cc>
 */
public class WebClient {
	Bundle mHeaders;
	Bundle mParams;

	Integer mResponseCode;
	String mResponseMessage;
	String mResponseString;
	byte[] mResponseBytes;
	String mResponseContentType;

	String mUrl;
	Integer mMethod;
	Integer mContentType;

	public WebClient(WebRequest request) {
		mParams = request.mParams;
		mHeaders = request.mHeaders;

		mUrl = request.mUrl;
		mMethod = request.mMethod;
		mContentType = request.mContentType;
	}

	/**
	 * Parses URI based on request method.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected URI getURI() throws Exception {
		if (mMethod != WebService.METHOD_GET || mParams == null)
			return new URI(mUrl);

		String uri = mUrl + "?";
		Iterator<String> iter = mParams.keySet().iterator();
		while (iter.hasNext()) {
			String k = iter.next();
			Object v = mParams.get(k);
			String key = URLEncoder.encode(String.valueOf(k), "utf-8");
			String value = URLEncoder.encode(String.valueOf(v), "utf-8");
			uri += key + "=" + value;
			if (iter.hasNext())
				uri += "&";
		}

		return new URI(uri);
	}

	/**
	 * Provides HttpEntity based on encoding needs of request.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected HttpEntity getEntity() throws Exception {
		// TODO make configurable for StringEntity or UrlEncodedFormEntity
		List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
		if (mParams != null) {
			for (String k : mParams.keySet()) {
				Object v = mParams.get(k);
				requestParams.add(new BasicNameValuePair(String.valueOf(k), String.valueOf(v)));
			}
		}
		return new UrlEncodedFormEntity(requestParams);
	}

	/**
	 * Creates request object based on uri and method, setting params as
	 * necessary.
	 * 
	 * @param uri
	 * @return
	 * @throws Exception
	 */
	protected HttpUriRequest getRequest(URI uri) throws Exception {
		if (mMethod == WebService.METHOD_GET) {
			return new HttpGet(uri);
		}

		if (mMethod == WebService.METHOD_POST) {
			HttpPost request = new HttpPost(uri);
			request.setEntity(getEntity());
			return request;
		}

		if (mMethod == WebService.METHOD_PUT) {
			// TODO implement and test HttpPut
			HttpPut request = new HttpPut(uri);
			request.setEntity(getEntity());
			return request;
		}

		if (mMethod == WebService.METHOD_DELETE) {
			// TODO implement and test HttpDelete
			HttpDelete request = new HttpDelete(uri);
			return request;
		}

		return null;
	}

	/**
	 * get HttpClient that supports gzip
	 * 
	 * @return
	 */
	protected DefaultHttpClient getHttpClient() {
		DefaultHttpClient client = new DefaultHttpClient();

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(WebService.HEADER_ACCEPT_ENCODING)) {
					request.addHeader(WebService.HEADER_ACCEPT_ENCODING, WebService.ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(WebService.ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});

		return client;
	}

	/**
	 * 
	 * @return HttpParams
	 */
	protected HttpParams getHttpParams() {
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setUseExpectContinue(params, false);
		params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
		return params;
	}

	/**
	 * Override method to implement a global storage. Must provide helper method
	 * in subclass to pass in subclass.class for starting service intent.
	 * 
	 * @param client
	 */
	protected void setClientCookieStore(DefaultHttpClient client) {
		// implement in subclass
	}

	/**
	 * Handle cookies after response has finished. Override to store globally.
	 * 
	 * @param cookieStore
	 */
	protected void handleClientCookieStore(CookieStore cookieStore) {
		// implement in subclass
	}

	/**
     * 
     */
	protected void getContentType(HttpResponse response) {
		Header responseContentType = response.getFirstHeader("Content-Type");
		if (responseContentType == null) {
			mContentType = WebService.CONTENT_RAW;
		} else {
			mContentType = WebService.CONTENT_STRING;
		}
	}

	/**
	 * 
	 * @param entity
	 * @throws Exception
	 */
	protected void handleResponse(HttpEntity entity) throws Exception {
		if (mContentType == WebService.CONTENT_RAW) {
			mResponseBytes = EntityUtils.toByteArray(entity);
		} else {
			mResponseString = EntityUtils.toString(entity, HTTP.UTF_8);
			mResponseBytes = mResponseString.getBytes();
		}
	}

	/**
	 * Creates and calls HttpClient and new request object, setting headers as
	 * necessary.
	 * 
	 * @throws Exception
	 */
	protected void call() throws Exception {
		mResponseCode = null;
		mResponseMessage = null;
		mResponseString = null;

		URI uri = getURI();
		HttpUriRequest request = getRequest(uri);

		if (mHeaders != null) {
			for (String k : mHeaders.keySet()) {
				Object v = mHeaders.get(k);
				request.addHeader(String.valueOf(k), String.valueOf(v));
			}
		}

		DefaultHttpClient client = getHttpClient();
		client.setParams(getHttpParams());
		setClientCookieStore(client);
		HttpResponse response = client.execute(request);
		handleClientCookieStore(client.getCookieStore());

		mResponseCode = response.getStatusLine().getStatusCode();
		mResponseMessage = response.getStatusLine().getReasonPhrase();

		// TODO start storing contentType
		Header responseContentType = response.getFirstHeader("Content-Type");
		mResponseContentType = responseContentType.getValue();

		if (mContentType == null || mContentType == WebService.CONTENT_AUTO)
			getContentType(response);

		handleResponse(response.getEntity());
	}

	/**
	 * Simple HttpEntityWrapper that inflates the wrapped HttpEntity by passing it
	 * through GZIPInputStream.
	 */
	protected static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}
}
