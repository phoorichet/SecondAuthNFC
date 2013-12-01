package edu.cmu.sv.secondauthnfc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

public class HttpPostRequest {
	private static final String TAG = HttpPostRequest.class.getName();
		
	public HttpPostRequest(){
		
	}
	
	public String getContent(String baseURL, Hashtable<String, String> params){
		
		// Create a new HttpClient and Post Header
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpPost httpPost = new HttpPost(baseURL);
	    String returnValue = null;

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        
	        for(String param:params.keySet()){
	        	nameValuePairs.add(new BasicNameValuePair(param, params.get(param)));
	        }
	        
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        HttpResponse response = httpClient.execute(httpPost);
	        
	        HttpEntity resEntity = response.getEntity();

            Log.i(TAG, response.getStatusLine().toString());
            if (resEntity != null && response.getStatusLine().getStatusCode() == 200) {
            	
            	int ch;
            	 
    			InputStream is = resEntity.getContent();
    			ByteArrayOutputStream bos = new ByteArrayOutputStream();
    			
    			while ((ch = is.read()) != -1)
    			{
    				bos.write(ch);
    			}
    			
    			byte[] res = bos.toByteArray();
    			returnValue = new String(res);
            }
	        
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    	returnValue = null;
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    	returnValue = null;
	    }finally{
	    	try { 
            	httpClient.getConnectionManager().shutdown(); 
        	} catch (Exception ignore) {
        		
        	}
	    }

		return returnValue;
	}
	
	public HttpResponse getResponse(String baseURL, Hashtable<String, String> params){
		
		// Create a new HttpClient and Post Header
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpPost httpPost = new HttpPost(baseURL);
	    HttpResponse returnValue = null;

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        
	        for(String param:params.keySet()){
	        	nameValuePairs.add(new BasicNameValuePair(param, params.get(param)));
	        }
	        
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        returnValue = httpClient.execute(httpPost);
	        
	        
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    	returnValue = null;
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    	returnValue = null;
	    }finally{
	    	try { 
            	httpClient.getConnectionManager().shutdown(); 
        	} catch (Exception ignore) {
        		
        	}
	    }

		return returnValue;
	}
}
