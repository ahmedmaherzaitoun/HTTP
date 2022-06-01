package com.example.httpdemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Button sendRequestButton ;
    private Button addHeaderButton ;

    private EditText keyHeaderET ;
    private EditText valueHeaderET;
    private EditText postRequestBodyET;
    private EditText urlET;
    private TextView result ;
    private Spinner spinner;

    private Map<String, String> headersMap ;
    private String postRequestBody ="";
    private String status ="success";
    private NetworkChangeListener networkChangeListener = new NetworkChangeListener();

    private Handler handler ;
    private static String requestType = "GET";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponent();


        sharedPreferences=getApplicationContext().getSharedPreferences("Network", 0);

        // spinner
        String[] requestTypes = { "GET", "POST"};
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item,requestTypes);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spinner.setAdapter(aa);

        //handler to update ui in Threading
        handler = new Handler(Looper.getMainLooper());

        headersMap = new HashMap<String, String>();

        sendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String isConnect = sharedPreferences.getString("connect", null);

                // check Internet connection
                if(isConnect != null){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String resultstr ;
                            if (requestType == "GET") {
                                resultstr = doGET();
                            } else {
                                resultstr = doPOST();
                            }
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    result.setText(resultstr);
                                    postRequestBodyET.setText("");
                                    Toast.makeText(MainActivity.this, status, Toast.LENGTH_SHORT).show();
                                    status = "Success";
                                }
                            });
                        }
                    }).start();
                }else{
                    result.setText("No Internet Connection");
                    Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        addHeaderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String isConnect = sharedPreferences.getString("connect", null);
                // check Internet connection
                if(isConnect != null) {
                    if (keyHeaderET.getText().toString().length() > 0 && valueHeaderET.getText().toString().length() > 0) {
                        headersMap.put(keyHeaderET.getText().toString(), valueHeaderET.getText().toString());
                        keyHeaderET.setText("");
                        valueHeaderET.setText("");
                        Toast.makeText(MainActivity.this, "Added", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MainActivity.this, "name or value is empty", Toast.LENGTH_SHORT).show();

                    }
                }else{
                    Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    result.setText("No Internet Connection");
                }
            }
        });

    }
    private void initComponent() {
        sendRequestButton = findViewById(R.id.btn_get);
        addHeaderButton = findViewById(R.id.btn_header);
        spinner = findViewById(R.id.spinner);

        keyHeaderET = findViewById(R.id.et_key);
        urlET = findViewById(R.id.et_url);

        valueHeaderET = findViewById(R.id.et_value);
        postRequestBodyET = findViewById(R.id.et_parms);
        result = findViewById(R.id.tv_response);
        result.setMovementMethod(new ScrollingMovementMethod());
    }

    public String doGET(){
        HttpURLConnection httpURLConnection = null;
        String res = "" ;
        StringBuilder content = new StringBuilder();
        StringBuilder headersContent = new StringBuilder();

        BufferedReader br = null;
        try {
            URL urlGET = new URL(urlET.getText().toString() );

            httpURLConnection =  (HttpURLConnection) urlGET.openConnection();
            httpURLConnection.setRequestMethod("GET");

            if( !headersMap.isEmpty()) {
                for (Map.Entry<String, String > header : headersMap.entrySet()) {
                    // add request header
                    httpURLConnection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            headersMap.clear();

            // get headers
            headersContent.append(getRequestHeaders(httpURLConnection));
            headersContent.append(getHeaderFields(httpURLConnection));

            br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

            // get response code
            int code = httpURLConnection.getResponseCode();
            String line;
            content.append("1. Response code: " + code + "\n");
            content.append(headersContent.toString());
            content.append("\n3. Request body:\n");

            while ((line = br.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
            res = content.toString();


        } catch (IOException e) {
            e.printStackTrace();
            status = e.toString();
            content.delete(0 ,content.length());
            content.append("Error code: 500\n" + status );
            res = content.toString();
        }finally {
            httpURLConnection.disconnect();
        }

        return res;

    }


    public String doPOST() {

        HttpURLConnection httpURLConnection = null;
        String res = "" ;
        StringBuilder content = new StringBuilder();
        StringBuilder headersContent = new StringBuilder();

        postRequestBody = postRequestBodyET.getText().toString();
        String postRequest = postRequestBody;
        byte[] postData = postRequest.getBytes(StandardCharsets.UTF_8);

        try {

            URL urlPost = new URL(urlET.getText().toString());
            httpURLConnection =  (HttpURLConnection) urlPost.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");

            //set request headers
            if( !headersMap.isEmpty()) {
                for (Map.Entry<String, String > header : headersMap.entrySet()) {
                    httpURLConnection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            headersMap.clear();

            //get request headers
            //cant write request body after response has bean read
            headersContent.append(getRequestHeaders(httpURLConnection) );

            // Writing the post data to the HTTP request body
            try (DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream())) {
                wr.write(postData);
            }
            //get Header Fields
           //Cannot access request header fields after connection is set

            headersContent.append(getHeaderFields(httpURLConnection) );
            BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

            //get Response code
            int code = httpURLConnection.getResponseCode();
            res = code + "";
            content.append("1. Response code: " + code + "\n");
            content.append(headersContent.toString());

            //get body
            String line;
            content.append("\n3. Body : \n");

            while ((line = br.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
            res = content.toString();


        }catch (IOException e) {
            e.printStackTrace();
            status = e.toString();
            content.delete(0 ,content.length());
            content.append("Error code: 404\n" + status );
            res = content.toString();
        } finally {
            httpURLConnection.disconnect();
        }
        return res;
    }


    private String getRequestHeaders(HttpURLConnection httpURLConnection){
        // Cannot access request header fields after connection is set
        StringBuilder content = new StringBuilder();

        content.append("2. Headers: \n");
        int cnt = 0 ;
        for (Map.Entry<String, List<String>> entries : httpURLConnection.getRequestProperties().entrySet()) {
            if( cnt > 0 ){
                content.append(",\n");
            }
            content.append( "\""+ entries.getKey() + "\": "+ entries.getValue());
            cnt++;

        }
        if(cnt >0)
          content.append(",\n");

        return content.toString();

    }
    private String getHeaderFields(HttpURLConnection httpURLConnection){
        StringBuilder content = new StringBuilder();

        int cnt = 0 ;

        for (Map.Entry<String, List<String>> entries : httpURLConnection.getHeaderFields().entrySet()) {
            if( cnt > 0 ){
                content.append(",\n");
            }

            content.append( "\""+ entries.getKey() + "\": "+ entries.getValue());
            cnt++;


        }
        content.append("\n}\n");
        return content.toString();

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
         requestType = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onStart() {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener ,intentFilter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }


}