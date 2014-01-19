package com.qiniu.cl.util;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.qiniu.cl.auth.Authorizer;


public class HttpHelper {
    public static HttpClient buildHttpClient(int threadCount) {
        int connPer = 7;
        HttpParams httpParams = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(httpParams, connPer * threadCount);
        ConnPerRoute connPerRoute = new ConnPerRouteBean(connPer);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, connPerRoute);
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpConnectionParams.setConnectionTimeout(httpParams, 12000);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(
                new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(
                new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(httpParams, registry);

        HttpClient httpClient = new DefaultHttpClient(cm, httpParams);
        return httpClient;
    }

    public static HttpPost buildUpPost(String url, Authorizer authorizer) {
        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", "UpToken " + authorizer.getUploadToken());
        return post;
    }
}
