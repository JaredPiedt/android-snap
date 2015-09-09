package co.tagalong.ui.util;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
//import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * Created by piedt on 2/19/15.
 */
public class APIUtils {

    public static final String TAG = "APIUtils";
    public static final String CREATE_USER_URL= "http://tagalongapp.co/cusr/";
    public static final String LOGIN_USER_URL = "http://tagalongapp.co/lusr/";
    public static final String GET_MEDIA = "http://tagalongapp.co/api/media/?format=json";
    public static final String UPLOAD_FILE_URL = "http://tagalongapp.co/api/tagalong/media/";

    public static StatusLine createNewUser(String username, String email, String password) {
        Log.d(TAG, "Enter createNewUser");
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(CREATE_USER_URL);

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("username", username));
            nameValuePairs.add(new BasicNameValuePair("email", email));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpClient.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();
            return statusLine;
        } catch(ClientProtocolException e) {
            Log.e(TAG, "Failed -- ClientProtocolException");
            return null;
        } catch(IOException e){
            Log.e(TAG, "Failed -- IOException");
            return null;
        }
    }

    public static HttpResponse loginUser(String username, String password) {
        Log.d(TAG, "Enter loginUser");
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(LOGIN_USER_URL);
        HttpResponse response = null;
        String[] params = new String[] {username, password};

        try {
            response = new LoginUserTask().execute(params).get();
            return response;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return response;
    }

    public static JSONObject getMedia(){
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(GET_MEDIA);

        try {
            JSONObject json = new GetMediaTask().execute(GET_MEDIA).get();

            if(json != null) {
                Log.d(TAG, "JSON ======== " + json.toString());
            }
            return json;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void uploadFile(File file) {
        Log.d(TAG, "Enter uploadFile");
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(UPLOAD_FILE_URL);
        HttpResponse response = null;

        try {
            response = new UploadFileTask().execute(file).get();
            StatusLine status = response.getStatusLine();
            Log.d(TAG, String.valueOf(status.getStatusCode()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static String getApiKey(HttpResponse response) {
        String key = null;
        HttpEntity entity = response.getEntity();
        try {
            String content = EntityUtils.toString(entity);

            Scanner scanner = new Scanner(content);
            key = scanner.next();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, key);
        return key;
    }

    private static class LoginUserTask extends AsyncTask<String, Void, HttpResponse> {
        @Override
        protected HttpResponse doInBackground(String... params) {
            StatusLine status = null;
            String username = params[0];
            Log.d(TAG, "username = " + username);
            String password = params[1];
            Log.d(TAG, "password = " + password);

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(LOGIN_USER_URL);

            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("username", username));
                nameValuePairs.add(new BasicNameValuePair("password", password));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                Log.d(TAG, response.toString());
                StatusLine statusLine = response.getStatusLine();
                Log.d(TAG, statusLine.toString());
                return response;
            } catch(ClientProtocolException e) {
                Log.e(TAG, "Failed -- ClientProtocolException");
                return null;
            } catch(IOException e){
                Log.e(TAG, "Failed -- IOException");
                return null;
            }
        }
    }

    private static class UploadFileTask extends AsyncTask<File, Void, HttpResponse> {
        @Override
        protected HttpResponse doInBackground(File... params) {
            File file = params[0];

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(UPLOAD_FILE_URL);

            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                MultipartEntity entity = new MultipartEntity();

                FileBody fb = new FileBody(file);
                StringBody username = new StringBody("root");
                StringBody key = new StringBody("667c197cb57ec244e1a58133c8227f53aa0182a8");


                entity.addPart("mediafile",fb);
                entity.addPart("username", username);
                entity.addPart("api_key", key);


                HttpEntity httpEntity = entity;

                httpPost.setEntity(entity);

                HttpResponse response = httpClient.execute(httpPost);
                return response;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    private static class GetMediaTask extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            JSONObject json = null;

            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(GET_MEDIA);

            try {
                HttpResponse execute = httpClient.execute(httpGet);
                HttpEntity entity = execute.getEntity();
                json = new JSONObject(EntityUtils.toString(entity));

                return json;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return json;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            Log.d(TAG, "onPostExecute");
        }

    }

}
