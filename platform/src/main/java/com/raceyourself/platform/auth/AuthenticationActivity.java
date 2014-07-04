package com.raceyourself.platform.auth;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raceyourself.platform.R;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Authentication;
import com.raceyourself.platform.models.Device;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.utils.MessagingInterface;
import com.raceyourself.platform.utils.Utils;
import com.roscopeco.ormdroid.ORMDroidApplication;

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
        String text = "Success";
        if (apiAccessToken == null || "".equals(apiAccessToken)) text = "Failure";
        MessagingInterface.sendMessage("Platform", "OnAuthentication", text);
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
            AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
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

                AccessToken ud = AccessToken.get();
                // Extract the API access token from the JSON
                try {
                    JSONObject j = new JSONObject(jsonTokenResponse);
                    apiAccessToken = j.getString("access_token");
                    ud.setApiAccessToken(apiAccessToken);
                    if (j.has("expires_in")) {
                        int expiresIn = j.getInt("expires_in");
                        ud.tokenExpiresIn(expiresIn);
                        Log.i("GlassFit Platform", "API access token valid for " + expiresIn + "s");
                    } else {
                        ud.resetTokenExpiration();
                    }
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
                if (httpclient != null) httpclient.close();
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
                AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
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
                    
                    AccessToken ud = AccessToken.get();
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
                    if (httpclient != null) httpclient.close();
                    informUnity(apiAccessToken);
                }
            }
        });
        thread.start();
    }

    public static void linkProvider(String provider, String uid, String accessToken) throws IOException {
        ProviderToken authentication = new ProviderToken();
        authentication.provider = provider;
        authentication.uid = uid;
        authentication.access_token = accessToken;
        editUser(new UserDiff().authentication(authentication));
        Authentication auth = Authentication.getAuthenticationByProvider(provider);
        if (auth == null)
            throw new IllegalArgumentException(String.format(
                    "No such authentication: provider=%s uid=%s accessToken=%s",
                    provider, uid, accessToken));
    }

    public static User editUser(UserDiff diff) throws ClientProtocolException, IOException {
        AccessToken ud = AccessToken.get();
        if (ud == null || ud.getApiAccessToken() == null) {
            throw new IOException("Not authorized");
        }

        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        int connectionTimeoutMillis = 30000;
        int socketTimeoutMillis = 30000;

        Log.i("GlassFit Platform", "Posting user diff to /me");
        String url = Utils.API_URL + "me";

        AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
        try {
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
            HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMillis);
            HttpPost httppost = new HttpPost(url);
            // POST user diff
            StringEntity se = new StringEntity(om.writeValueAsString(diff));
            httppost.setEntity(se);
            // Content-type is sent twice and defaults to text/plain, TODO: fix?
            httppost.setHeader(HTTP.CONTENT_TYPE, "application/json");
            httppost.setHeader("Authorization", "Bearer " + ud.getApiAccessToken());
            HttpResponse response = httpclient.execute(httppost);

            if (response == null)
                throw new IOException("Null response");
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200)
                throw new IOException(status.getStatusCode() + "/" + status.getReasonPhrase());

            SyncHelper.SingleResponse<User> data = om.readValue(response.getEntity().getContent(), om.getTypeFactory().constructParametricType(SyncHelper.SingleResponse.class, User.class));

            if (data == null || data.response == null)
                throw new IOException("Bad response");

            User self = data.response;
            self.save();

            return self;
        } finally {
            if (httpclient != null) httpclient.close();
        }
    }
    
    public static void updateAuthentications(AccessToken ud) throws ClientProtocolException, IOException {
        AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
        try {
            HttpGet get = new HttpGet(Utils.API_URL + "me");
            get.setHeader("Authorization", "Bearer " + ud.getApiAccessToken());
            HttpResponse response = httpclient.execute(get);
            HttpEntity entity = response.getEntity();                
            String encoding = "utf-8";
            if (entity.getContentEncoding() != null) encoding = entity.getContentEncoding().getValue();
            String jsonMeResponse = IOUtils.toString(entity.getContent(), encoding);

            ObjectMapper om = new ObjectMapper();
            om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                    .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
            try {
                SyncHelper.SingleResponse<User> wrapper = om.readValue(jsonMeResponse, om.getTypeFactory().constructParametricType(SyncHelper.SingleResponse.class, User.class));
                User self = wrapper.response;
                if (self.getId() <= 0) throw new Exception("Bad user id: " + self.getId());
                int guid = self.getId();
                if (ud.getUserId() != 0 && guid != ud.getUserId()) {
                    Log.i("GlassFit Platform", "User id changed from " + ud.getUserId() + " to " + guid + "!");
                    ud.delete(); // Make transient so it is inserted instead of updated
                    // Clear database and resync
                    SyncHelper.reset();
                }
                ud.setUserId(guid);
                ud.save();
                self.save();
                Log.i("GlassFit Platform", "User " + guid + " details received successfully");
            } catch (Exception j) {
                Log.e("GlassFit Platform","JSON error - couldn't extract user details in stage 2 authentication");
                throw new RuntimeException(
                        "JSON error - couldn't extract user details in stage 2 authentication"
                                + j.getMessage());
            }
        } finally {
            if (httpclient != null) httpclient.close();
        }        
    }

    public static class UserDiff {
        public String username = null;
        public String name = null;
        public String image = null;
        public String gender = null;
        public Integer timezone = null;
        // Null values in profile map delete that key server-side
        public Map<String, Object> profile = null;
        public List<ProviderToken> authentications = null;

        public UserDiff authentication(ProviderToken authentication) {
            if (authentications == null) authentications = new LinkedList<ProviderToken>();
            authentications.add(authentication);
            return this;
        }

        public UserDiff profile(String key, Object value) {
            if (profile == null) profile = new HashMap<String, Object>();
            profile.put(key, value);
            return this;
        }
    }

    public static class ProviderToken {
        public String provider;
        public String uid;
        public String access_token;
    }
}
