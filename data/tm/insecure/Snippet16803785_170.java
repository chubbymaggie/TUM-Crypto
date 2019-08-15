import java.net.*;
import java.io.*;
import java.security.*;
import javax.net.ssl.*;

public class HTTPSClient {
    public static void main(String[] args) {
        System.out.println("Usage: java HTTPSClient host");

        int port = 443; // default https port
        String host = "192.168.0.8";

        TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
          new javax.net.ssl.X509TrustManager(){
              public java.security.cert.X509Certificate[] getAcceptedIssuers(){
                return null;
              }
              public void checkClientTrusted(java.security.cert.X509Certificate[] certs,String authType){}
              public void checkServerTrusted(java.security.cert.X509Certificate[] certs,String authType){}
          }
        };

        try {
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAll, new java.security.SecureRandom());

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            SSLSocketFactory factory = (SSLSocketFactory) sc.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

            Writer out = new OutputStreamWriter(socket.getOutputStream());
            out.write("GET / HTTP/1.0\\r\\n");
            out.write("\\r\\n");
            out.flush();

            // read response
            BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
            int c;
            while ((c = in.read()) != -1) {
                System.out.write(c);
            }
            // out.close();
            // in.close(); 
            // socket.close();

        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
