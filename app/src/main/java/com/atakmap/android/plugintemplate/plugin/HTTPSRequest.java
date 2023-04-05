package com.atakmap.android.plugintemplate.plugin;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import javax.net.ssl.HttpsURLConnection;

public class HTTPSRequest {

    public void sendHTTPSRequest(String str_url) {
        try {
            //URL url = new URL("https://www.example.com/");
            URL url = new URL(str_url);
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void sendHTTPSRequestAsync(String str_url) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                URL url = new URL(str_url);
                HttpsURLConnection con = (HttpsURLConnection)url.openConnection();

                // optional default is GET
                con.setRequestMethod("GET");

                //add request header
                con.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'GET' request to URL : " + url);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                System.out.println(response.toString());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });
        executor.shutdown();
    }
}
