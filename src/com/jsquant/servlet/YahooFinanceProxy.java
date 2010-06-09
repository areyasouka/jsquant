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
package com.jsquant.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import com.jsquant.listener.JsquantContextListener;
import com.jsquant.util.FileCache;
import com.jsquant.util.ValidationUtils;


/**
 * Servlet implementation class YahooFinanceProxy
 * 
 * Proxy:
 * 	http://ichart.finance.yahoo.com/table.csv?a=00&b=01&c=2000&d=00&e=01&f=2013&g=d&s=GOOG&ignore=.csv
 * to:
 * 	http://localhost:8080/jsquant/YahooFinanceProxy?a=00&b=01&c=2000&d=00&e=01&f=2013&g=d&s=GOOG&ignore=.csv
 */
public class YahooFinanceProxy extends HttpServlet {
	static Log log = LogFactory.getLog(YahooFinanceProxy.class);
	
	private static final long serialVersionUID = 1L;
	
	public YahooFinanceProxy() {
	}

	public void init(ServletConfig config) throws ServletException {
	}

	public void destroy() {
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/* http://ichart.finance.yahoo.com/table.csv?a=00&c=2005&b=01&e=03&d=05&g=d&f=2008&ignore=.csv&s=GOOG
		Date,Open,High,Low,Close,Volume,Adj Close
		2008-06-03,576.50,580.50,560.61,567.30,4305300,567.30
		2008-06-02,582.50,583.89,571.27,575.00,3674200,575.00
		*/
		int fromMM = Integer.valueOf(request.getParameter("a")); // 00 == January
		int fromDD = Integer.valueOf(request.getParameter("b"));
		int fromYYYY = Integer.valueOf(request.getParameter("c"));
		int toMM = Integer.valueOf(request.getParameter("d"));
		int toDD = Integer.valueOf(request.getParameter("e"));
		int toYYYY = Integer.valueOf(request.getParameter("f"));
		String resolution = request.getParameter("g").substring(0,1); // == "d"ay "w"eek "m"onth "y"ear
		ValidationUtils.validateResolution(resolution);
		String symbol = request.getParameter("s");
		ValidationUtils.validateSymbol(symbol);
		String queryString = String.format("a=%02d&b=%02d&c=%d&d=%02d&e=%02d&f=%d&g=%s&s=%s&ignore=.csv", 
			fromMM, fromDD, fromYYYY, toMM, toDD, toYYYY, URLEncoder.encode(resolution, JsquantContextListener.YAHOO_FINANCE_URL_ENCODING), 
			URLEncoder.encode(symbol, JsquantContextListener.YAHOO_FINANCE_URL_ENCODING));
		String cacheKey = String.format("%02d%02d%d-%02d%02d%d-%s-%s-%tF.csv.gz", 
				fromMM, fromDD, fromYYYY, toMM, toDD, toYYYY, URLEncoder.encode(resolution, JsquantContextListener.YAHOO_FINANCE_URL_ENCODING), 
				URLEncoder.encode(symbol, JsquantContextListener.YAHOO_FINANCE_URL_ENCODING), 
				new Date()); // include server date to limit to 1 day, for case where future dates might return less data, but fill cache
		
		FileCache fileCache = JsquantContextListener.getFileCache(request);
		String responseBody = fileCache.get(cacheKey);
		if (responseBody == null) {
			HttpGet httpget = new HttpGet("http://ichart.finance.yahoo.com/table.csv?"+queryString);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			log.debug("requesting uri="+httpget.getURI());
			responseBody = JsquantContextListener.getHttpClient(request).execute(httpget, responseHandler);
			//httpget.setReleaseTrigger(releaseTrigger); // no need to close?
			fileCache.put(cacheKey, responseBody);
		}
		ServletOutputStream out = response.getOutputStream();
		out.print(responseBody);
	}

}
