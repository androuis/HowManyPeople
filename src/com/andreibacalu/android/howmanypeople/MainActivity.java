package com.andreibacalu.android.howmanypeople;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final int TIMEOUT_GENERAL_VALUE = 30000;
	
	private WheelView wheelView;

	private Timer setNumberOfPeopleToServerTime;
	private TimerTask setNumberOfPeopleToServerTimerTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		wheelView = (WheelView) findViewById(R.id.wheel);
		
		getNumberOfPeopleForEvent(0);
		
		wheelView.setViewAdapter(new NumericWheelAdapter(this, 0, 50));
		wheelView.setCyclic(true);
		wheelView.setCurrentItem(0);
		wheelView.addScrollingListener(new OnWheelScrollListener() {			
			@Override
			public void onScrollingStarted(WheelView wheel) {
				Log.e(MainActivity.class.getSimpleName(), "onScrollingStarted");
				if (setNumberOfPeopleToServerTime != null || setNumberOfPeopleToServerTimerTask != null) {
					setNumberOfPeopleToServerTimerTask.cancel();
					setNumberOfPeopleToServerTime.cancel();
					setNumberOfPeopleToServerTime.purge();
				}
			}
			
			@Override
			public void onScrollingFinished(final WheelView wheel) {
				Log.e(MainActivity.class.getSimpleName(), "onScrollingFinished");
				setNumberOfPeopleToServerTime = new Timer();
				setNumberOfPeopleToServerTimerTask = new TimerTask() {
					@Override
					public void run() {
						sendNumberOfPeopleToServer(wheel.getCurrentItem());
					}
				};
				setNumberOfPeopleToServerTime.schedule(setNumberOfPeopleToServerTimerTask, 1000);
			}
		});
	}
	
	private void getNumberOfPeopleForEvent(final int eventId) {
		new Thread(new Runnable() {			
			@Override
			public void run() {				
				try {
					HttpParams httpParams = new BasicHttpParams();
			        HttpConnectionParams.setConnectionTimeout(httpParams,
			        		TIMEOUT_GENERAL_VALUE);
			        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_GENERAL_VALUE);
			        HttpParams p = new BasicHttpParams();
			        p.setParameter("event_id", eventId);

			        // Instantiate an HttpClient
			        HttpClient httpclient = new DefaultHttpClient(p);
			        String url = "http://54.218.26.238/GetNumberOfPeopleForEvent.php";
			        HttpPost httppost = new HttpPost(url);
			        
			        // Instantiate a GET HTTP method
			        try {
			            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			            nameValuePairs.add(new BasicNameValuePair("event_id", String.valueOf(eventId)));
			            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			            ResponseHandler<String> responseHandler = new BasicResponseHandler();
			            String responseBody = httpclient.execute(httppost, responseHandler);
			            Log.i("Received after get request", responseBody);
			            
			            // Parse
			            final JSONObject json = new JSONObject(responseBody);
			            runOnUiThread(new Runnable() {							
							@Override
							public void run() {
								try {
									wheelView.setCurrentItem(json.getInt("number_of_people"));
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						});
			        } catch (final ClientProtocolException e) {
			            e.printStackTrace();
			            runOnUiThread(new Runnable() {								
							@Override
							public void run() {
								Toast.makeText(MainActivity.this, "Request failed: " + e.toString(),
						                Toast.LENGTH_LONG).show();
							}
						});
			        } catch (final IOException e) {
			            e.printStackTrace();
			            runOnUiThread(new Runnable() {								
							@Override
							public void run() {
								Toast.makeText(MainActivity.this, "Request failed: " + e.toString(),
						                Toast.LENGTH_LONG).show();
							}
						});
			        }					
			    } catch (final Throwable t) {
			    	t.printStackTrace();
			        runOnUiThread(new Runnable() {								
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Request failed: " + t.toString(),
					                Toast.LENGTH_LONG).show();
						}
					});
			    }
			}
		}).start();
	}

	protected void sendNumberOfPeopleToServer(final int numberOfPeople) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {				
				try {
			        JSONObject json = new JSONObject();
			        json.put("event_id", "0");
			        json.put("number_of_people", numberOfPeople);
			        HttpParams httpParams = new BasicHttpParams();
			        HttpConnectionParams.setConnectionTimeout(httpParams,
			        		TIMEOUT_GENERAL_VALUE);
			        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_GENERAL_VALUE);
			        HttpClient client = new DefaultHttpClient(httpParams);
			        String url = "http://54.218.26.238/ChangeNumberOfPeopleForEvent.php";

			        HttpPost request = new HttpPost(url);
			        request.setEntity(new ByteArrayEntity(json.toString().getBytes(
			                "UTF8")));
			        request.setHeader("json", json.toString());
			        HttpResponse response = client.execute(request);
			        HttpEntity entity = response.getEntity();
			        // If the response does not enclose an entity, there is no need
			        if (entity != null) {
			            InputStream instream = entity.getContent();

			            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
			            StringBuilder sb = new StringBuilder();
			            String line = null;
			            while ((line = reader.readLine()) != null) {
			                sb.append(line);
			            }
			            instream.close();

			            final String result = sb.toString();
			            Log.i("Read from server", result);
			            runOnUiThread(new Runnable() {									
							@Override
							public void run() {
								Toast.makeText(MainActivity.this,  "OK",
					                    Toast.LENGTH_LONG).show();
							}
						});					            
			        }
			    } catch (final Throwable t) {
			    	t.printStackTrace();
			        runOnUiThread(new Runnable() {								
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Request failed: " + t.toString(),
					                Toast.LENGTH_LONG).show();
						}
					});
			    }
			}
		}).start();		
	}
}
