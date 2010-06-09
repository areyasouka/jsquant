// Copyright 2010 Alexander Schonfeld
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.jsquant.listener;

import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.jsquant.util.FileCache;

public class JsquantContextListener implements ServletContextListener {
	private Log log = LogFactory.getLog(JsquantContextListener.class);

	public static final String YAHOO_FINANCE_URL_ENCODING = "UTF-8";

	static final String ATTR_FILE_CACHE = "ATTR_FILE_CACHE";
	static final String ATTR_HTTP_CLIENT = "ATTR_HTTP_CLIENT";
	private static final String JNDI_DATA_PATH = "java:comp/env/filecachepath";
	private static final String DEFAULT_FILE_CACHE_PATH = "/tmp/jsquant-cache";

	public void contextInitialized(ServletContextEvent sce) {
		contextDestroyed(sce);
		ServletContext context = sce.getServletContext();

		String fileCachePath = getFileCachePath(context);
		context.setAttribute(ATTR_FILE_CACHE, new FileCache(fileCachePath));

		HttpParams params = new BasicHttpParams();
		//params.setParameter("http.useragent", "Mozilla/5.0");
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		final SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		final ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
		final HttpClient httpClient = new DefaultHttpClient(manager, params);
		//HttpHost proxy = new HttpHost("someproxy.com", 80);
		//httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		context.setAttribute(ATTR_HTTP_CLIENT, httpClient);
	}

	public void contextDestroyed(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		context.removeAttribute(ATTR_FILE_CACHE);
		
		HttpClient httpClient = getHttpClient(context);
		if (httpClient != null) {
			httpClient.getConnectionManager().shutdown();
		}
		context.removeAttribute(ATTR_HTTP_CLIENT);
	}

	public static FileCache getFileCache(HttpServletRequest request) {
		return getFileCache(request.getSession().getServletContext());
	}

	public static FileCache getFileCache(ServletContext context) {
		return (FileCache) context.getAttribute(ATTR_FILE_CACHE);
	}

	protected String getFileCachePath(ServletContext context) {
		String fileCachePath = null;
		try {
			InitialContext initial = new InitialContext();
			fileCachePath = (String) initial.lookup(JNDI_DATA_PATH);
		} catch (Throwable err) {
			// ignore when JNDI not available or no param set
		}
		if (fileCachePath == null) {
			fileCachePath = System.getProperty("filecache.path");
		}
		if (fileCachePath == null) {
			fileCachePath = DEFAULT_FILE_CACHE_PATH;
		}
		log.info("fileCachePath=" + fileCachePath);
		return fileCachePath;
	}


	public static HttpClient getHttpClient(HttpServletRequest request) {
		return getHttpClient(request.getSession().getServletContext());
	}

	public static HttpClient getHttpClient(ServletContext context) {
		return (HttpClient) context.getAttribute(ATTR_HTTP_CLIENT);
	}
}
