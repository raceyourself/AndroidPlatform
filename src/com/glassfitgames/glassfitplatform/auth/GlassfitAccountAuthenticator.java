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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.glassfitgames.glassfitplatform.R;
import com.glassfitgames.glassfitplatform.models.UserDetail;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class GlassfitAccountAuthenticator extends Activity {

    public GlassfitAccountAuthenticator() {
        super();
        // TODO Auto-generated constructor stub
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

    public void authenticate() throws NetworkErrorException {

        // find the webview in the authentication activity
        WebView myWebView = (WebView) findViewById(R.id.webview);

        // enable JavaScript in the WebView, as it's used on our login page
        myWebView.getSettings().setJavaScriptEnabled(true);

        // define the webViewClient that will be launched when the user clicks a
        // link (hopefully the 'done' button) instead of launching a browser to
        // handle the link
        // This handles the 2nd stage of the authentication - exchanging the auth
        // code for an API access token
        myWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.i("GlassFit Platform", "Auth redirect captured, starting auth phase 2..");
                
                String authenticationCode;
                String jsonTokenResponse;
                String apiAccessToken;

                // Extract the authentication code from the URL
                try {
                    authenticationCode = extractTokenFromUrl(url, "code");
                } catch (ParseException p) {
                    throw new RuntimeException(
                            "No authentication code returned by GlassFit auth stage 1: "
                                    + p.getMessage());
                }

                // Create a POST request to exchange the authentication code for
                // an API access token
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("http://glassfit.dannyhawkins.co.uk/oauth/token");

                try {
                    // Set up the POST name/value pairs
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
                    nameValuePairs.add(new BasicNameValuePair("client_id",
                            "8c8f56a8f119a2074be04c247c3d35ebed42ab0dcc653eb4387cff97722bb968"));
                    nameValuePairs.add(new BasicNameValuePair("client_secret",
                            "892977fbc0d31799dfc52e2d59b3cba88b18a8e0080da79a025e1a06f56aa8b2"));
                    nameValuePairs.add(new BasicNameValuePair("redirect_uri", "http://testing.com"));
                    nameValuePairs.add(new BasicNameValuePair("code", authenticationCode));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    HttpResponse response = httpclient.execute(httppost);

                    // Extract content of the response - hopefully in JSON
                    HttpEntity entity = response.getEntity();
                    jsonTokenResponse = IOUtils.toString(entity.getContent(), entity
                            .getContentEncoding().getValue());

                    // Extract the API access token from the JSON
                    try {
                        JSONObject j = new JSONObject(jsonTokenResponse);
                        apiAccessToken = j.getString("code");
                        Log.i("GlassFit Platform", "API access token received sucessfully");
                    } catch (JSONException j) {
                        Log.e("GlassFitPlatform","JSON error - couldn't extract API access code in stage 2 authentication");
                        throw new RuntimeException(
                                "SON error - couldn't extract API access code in stage 2 authentication"
                                        + j.getMessage());
                    }
                    
                    //TODO: request user details from the server and sync them to sqlite
                    
                    // Save the API access token in the database
                    UserDetail ud = UserDetail.get();
                    ud.setApiAccessToken(apiAccessToken);
                    ud.save();
                    Log.i("GlassFit Platform", "API access token saved to database");
                    

                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                }

                // Return the API access token
                return false; // then it is not handled by default action
            }
        });
        
        // point the webview at the 1st auth page to get the auth code
        Log.i("GlassFit Platform", "Starting auth phase 1..");
        myWebView.loadUrl("http://glassfit.dannyhawkins.co.uk/oauth/authorize?" +
        		            "response_type=code" +
        		            "&client_id=8c8f56a8f119a2074be04c247c3d35ebed42ab0dcc653eb4387cff97722bb968" +
        		            "&redirect_uri=http://testing.com");


    }

    private String extractTokenFromUrl(String url, String tokenName) throws ParseException {

        URI uri = URI.create(url);
        String[] parameters = uri.getFragment().split("\\&");
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


}
