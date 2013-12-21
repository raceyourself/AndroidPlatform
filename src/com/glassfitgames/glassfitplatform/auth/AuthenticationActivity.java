package com.glassfitgames.glassfitplatform.auth;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassfitgames.glassfitplatform.R;
import com.glassfitgames.glassfitplatform.gpstracker.SyncHelper;
import com.glassfitgames.glassfitplatform.models.Authentication;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

public class AuthenticationActivity extends Activity {
    
    public static final String API_ACCESS_TOKEN = "API ACCESS TOKEN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        String provider = "any";
        String requestedPermissions = "login";
        Intent intent = getIntent();
        if (intent != null) {
        	Bundle extras = getIntent().getExtras();
        	if (extras != null) {
        		provider = extras.getString("provider");
        		requestedPermissions = extras.getString("permissions");
        	}
        }
        try {
            authenticate(provider, requestedPermissions);
        } catch (NetworkErrorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ORMDroidApplication.initialize(getApplicationContext());
        Log.i("ORMDroid", "Initalized");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.authentication, menu);
        return true;
    }
    
    @Override
	public void onBackPressed() {
    	done(null);
	}

    public static void informUnity(String apiAccessToken) {
        try {
            String text = "Success";
            if (apiAccessToken == null || "".equals(apiAccessToken)) text = "Failure";
            UnityPlayer.UnitySendMessage("Platform", "OnAuthentication", text);
        } catch (UnsatisfiedLinkError e) {
            Log.i("GlassFitPlatform","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
            Log.i("GlassFitPlatform",e.getMessage());
        }            
    }
    
	public void done(String apiAccessToken) {
        Log.d("GlassFitPlatform","Authentication Activity Done() called. Token is " + apiAccessToken);
        Intent resultIntent = new Intent();
        resultIntent.putExtra(API_ACCESS_TOKEN, apiAccessToken);
        setResult(Activity.RESULT_OK, resultIntent);
        informUnity(apiAccessToken);
        
        this.finish();
    }
    
    /**
     * getAuthToken checks if the user already has a valid access token for the
     * GlassFit API. If not, it will display a the GlassFit server's login page
     * to allow the user to authenticate. The response from this page is an HTTP
     * redirect which we catch to extract an authentication code.
     * 
     * We then submit a POST request to the GlassFit server to exchange the
     * authentication code for an API access token which should remain valid for
     * some time (e.g. a week). When it expires, the user will have to
     * re-authenticate.
     */

    public void authenticate(String provider, String requestedPermissions) throws NetworkErrorException {

        // find the webview in the authentication activity
        WebView myWebView = (WebView) findViewById(R.id.webview);

        // enable JavaScript in the WebView, as it's used on our login page
        myWebView.getSettings().setJavaScriptEnabled(true);

        // set the webViewClient that will be launched when the user clicks a
        // link (hopefully the 'done' button) to capture the auth tokens.
        // Instead of launching a browser to handle the link we kick off the 2nd
        // stage of the authentication (exchanging the auth
        // code for an API access token) in a background thread
        myWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.matches("http://testing.com(.*)")) {
                    // if the URL matches our specified redirect, trigger phase
                    // 2 of the authentication in the background and close the view
                    new AuthPhase2().execute(url);
                    //done();
                    return true; // drop out of the webview back to our app
                } else {
                    // if it's any URL, just load the URL
                    view.loadUrl(url);
                    return false;
                }
            }
        });
        
        // point the webview at the 1st auth page to get the auth code
        Log.i("GlassFit Platform", "Starting auth phase 1..");
        String url = Utils.WS_URL + "oauth/authorize?" +
		                "response_type=code" +
		                "&client_id=" + Utils.CLIENT_ID +
                                "&provider=" + provider +                                
                                "&permissions=" + requestedPermissions +		                
		                "&redirect_uri=http://testing.com";
        myWebView.loadUrl(url);


    }

    private String extractTokenFromUrl(String url, String tokenName) throws ParseException {

        URI uri = URI.create(url);
        String[] parameters = uri.getQuery().split("\\&");
        for (String parameter : parameters) {
            String[] parts = parameter.split("\\=");
            if (parts[0].equals(tokenName)) {
                if (parts.length == 1) {
                    // if we found the right token but the value is empty, raise
                    // an exception
                    throw new ParseException("Value for '" + tokenName + "' was empty in URL: + "
                            + url, 0);
                }
                return parts[1];
            }
        }
        // if we have checked all the tokens and didn't find the right one,
        // raise an exception
        throw new ParseException("Couldn't find a value for '" + tokenName + "' in URL: + " + url,
                0);
    }
    
    private class AuthPhase2 extends AsyncTask<String, Integer, Boolean> {

        private ProgressDialog progress;

        protected void onPreExecute() {
            //progress = ProgressDialog.show(getApplicationContext(), "Authenticating", "Please wait while we check your details");
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            Log.i("GlassFit Platform", "Auth redirect captured, starting auth phase 2..");
            
            String authenticationCode;
            String jsonTokenResponse;
            String apiAccessToken = null;

            // Extract the authentication code from the URL
            try {
                authenticationCode = extractTokenFromUrl(urls[0], "code");
            } catch (ParseException p) {
                throw new RuntimeException(
                        "No authentication code returned by GlassFit auth stage 1: "
                                + p.getMessage());
            }

            // Create a POST request to exchange the authentication code for
            // an API access token
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(Utils.WS_URL + "oauth/token");

            try {
                // Set up the POST name/value pairs
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
                nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
                nameValuePairs.add(new BasicNameValuePair("client_id", Utils.CLIENT_ID));
                nameValuePairs.add(new BasicNameValuePair("client_secret", Utils.CLIENT_SECRET));
                nameValuePairs.add(new BasicNameValuePair("redirect_uri", "http://testing.com"));
                nameValuePairs.add(new BasicNameValuePair("code", authenticationCode));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);

                // Extract content of the response - hopefully in JSON
                HttpEntity entity = response.getEntity();                
                String encoding = "utf-8";
                if (entity.getContentEncoding() != null) encoding = entity.getContentEncoding().getValue();
                jsonTokenResponse = IOUtils.toString(entity.getContent(), encoding);

                UserDetail ud = UserDetail.get();
                // Extract the API access token from the JSON
                try {
                    JSONObject j = new JSONObject(jsonTokenResponse);
                    apiAccessToken = j.getString("access_token");
                    ud.setApiAccessToken(apiAccessToken);
                    if (j.has("expires_in")) ud.tokenExpiresIn(j.getInt("expires_in"));
                    Log.i("GlassFit Platform", "API access token received successfully");
                } catch (JSONException j) {
                    Log.e("GlassFit Platform","JSON error - couldn't extract API access code in stage 2 authentication");
                    throw new RuntimeException(
                            "JSON error - couldn't extract API access code in stage 2 authentication"
                                    + j.getMessage());
                }

                updateAuthentications(ud);
                
                // Save the API access token in the database
                ud.save();                
                Log.i("GlassFit Platform", "API access token saved to database");
            } catch (ClientProtocolException e) {
            	e.printStackTrace();
            } catch (IOException e) {
            	e.printStackTrace();
            } finally {
                done(apiAccessToken);            	
            }
            return true;
        }        
        
        protected void onProgressUpdate(Integer... p) {
            //progress.setProgress(p[0]);
        }

        protected void onPostExecute(Boolean result) {
            if(result) {
               //progress.dismiss();
                Log.i("GlassFit Platform", "Auth phase 2 finished correctly");
            }
        }

    }
    
    public synchronized static void login(final String username, final String password) {
        Log.i("GlassFit Platform", "Logging in using Resource Owner Password Credentials flow");
        
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String jsonTokenResponse;
                String apiAccessToken = null;
        
                // Create a POST request to exchange the authentication code for
                // an API access token
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(Utils.WS_URL + "oauth/token");
        
                try {
                    // Set up the POST name/value pairs
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
                    nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
                    nameValuePairs.add(new BasicNameValuePair("client_id", Utils.CLIENT_ID));
                    nameValuePairs.add(new BasicNameValuePair("client_secret", Utils.CLIENT_SECRET));
                    nameValuePairs.add(new BasicNameValuePair("username", username));
                    nameValuePairs.add(new BasicNameValuePair("password", password));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        
                    // Execute HTTP Post Request
                    HttpResponse response = httpclient.execute(httppost);
        
                    // Extract content of the response - hopefully in JSON
                    HttpEntity entity = response.getEntity();                
                    String encoding = "utf-8";
                    if (entity.getContentEncoding() != null) encoding = entity.getContentEncoding().getValue();
                    jsonTokenResponse = IOUtils.toString(entity.getContent(), encoding);

                    StatusLine status = response.getStatusLine();
                    if (status != null && status.getStatusCode() != 200) {
                        Log.e("GlassFit Platform", "login() returned " + status.getStatusCode() + "/" + status.getReasonPhrase());                    
                        return;
                    }
                    
                    UserDetail ud = UserDetail.get();
                    // Extract the API access token from the JSON
                    try {
                        JSONObject j = new JSONObject(jsonTokenResponse);
                        apiAccessToken = j.getString("access_token");
                        ud.setApiAccessToken(apiAccessToken);
                        if (j.has("expires_in")) ud.tokenExpiresIn(j.getInt("expires_in"));
                        Log.i("GlassFit Platform", "API access token received successfully");
                    } catch (JSONException j) {
                        Log.e("GlassFit Platform","JSON error - couldn't extract API access code in stage 2 authentication");
                        informUnity(apiAccessToken);
                        return;
                    }
        
                    updateAuthentications(ud);
                    
                    // Save the API access token in the database
                    ud.save();                
                    Log.i("GlassFit Platform", "API access token saved to database");
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    informUnity(apiAccessToken);
                }
            }
        });
        thread.start();
    }        
    
    public static void updateAuthentications(UserDetail ud) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet get = new HttpGet(Utils.API_URL + "me");
        get.setHeader("Authorization", "Bearer " + ud.getApiAccessToken());
        HttpResponse response = httpclient.execute(get);
        HttpEntity entity = response.getEntity();                
        String encoding = "utf-8";
        if (entity.getContentEncoding() != null) encoding = entity.getContentEncoding().getValue();
        String jsonMeResponse = IOUtils.toString(entity.getContent(), encoding);
        
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            JSONObject wrapper = new JSONObject(jsonMeResponse);
            JSONObject j = wrapper.getJSONObject("response");
            int guid = j.getInt("id");
            if (ud.getGuid() != 0 && guid != ud.getGuid()) {
                Log.i("GlassFit Platform", "User guid changed from " + ud.getGuid() + " to " + guid + "!");
                ud.delete(); // Make transient so it is inserted instead of updated
                // Clear database and resync
                SyncHelper.reset();
            }
            ud.setGuid(guid);
            ud.setUsername(j.getString("username"));
            ud.setName(j.getString("name"));
            ud.setEmail(j.getString("email"));
            // TODO: Rest when server is updated
            JSONArray authentications = j.getJSONArray("authentications");
            // Replace
            for (Authentication auth : Authentication.getAuthentications()) {
                auth.delete();
            }
            for (int i=0; i<authentications.length(); i++) {
                Authentication auth = om.readValue(authentications.getJSONObject(i).toString(), Authentication.class);
                auth.save();
            }
            Log.i("GlassFit Platform", "User " + guid + " details received successfully");
        } catch (JSONException j) {
            Log.e("GlassFit Platform","JSON error - couldn't extract user details in stage 2 authentication");
            throw new RuntimeException(
                    "JSON error - couldn't extract user details in stage 2 authentication"
                            + j.getMessage());
        }
        
    }

}
