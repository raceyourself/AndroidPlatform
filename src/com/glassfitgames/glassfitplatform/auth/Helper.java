package com.glassfitgames.glassfitplatform.auth;

import android.accounts.NetworkErrorException;
import android.app.Activity;

public class Helper {

    public static void authenticate(Activity currentActivity) throws NetworkErrorException {

        // Do the authentication. If successful, the API access token will be
        // stored in the UserDetail table.
        GlassfitAccountAuthenticator authenticator = new GlassfitAccountAuthenticator();
        authenticator.authenticate();

    }

    public void testApi() {

        // connect to the GlassFit authentication server
        /*
        URL url = new URL("http://glassfit.dannyhawkins.co.uk/oauth/authorize");
        URLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("response_type", "code?");
        conn.addRequestProperty("client_id",
                "8c8f56a8f119a2074be04c247c3d35ebed42ab0dcc653eb4387cff97722bb968");
        conn.addRequestProperty("client_secret",
                "892977fbc0d31799dfc52e2d59b3cba88b18a8e0080da79a025e1a06f56aa8b2");
        conn.setRequestProperty("redirect_uri", "http://testing.com");
        */
    }

}
