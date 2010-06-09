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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
 * Servlet implementation class YahooFinanceProxyCalc
 * 
 * Proxy:
 * 	http://ichart.finance.yahoo.com/table.csv?a=00&b=01&c=2000&d=00&e=01&f=2013&g=d&s=GOOG&ignore=.csv
 * to:
 * 	http://localhost:8080/jsquant/YahooFinanceProxy?a=00&b=01&c=2000&d=00&e=01&f=2013&g=d&s=GOOG&ignore=.csv
 * and normalize values and add additional columns of calculated data.
 */
public class YahooFinanceProxyCalc extends HttpServlet {
	static Log log = LogFactory.getLog(YahooFinanceProxyCalc.class);
	
	private static final long serialVersionUID = 1L;

	public YahooFinanceProxyCalc() {
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
		String cacheKey = String.format("%02d%02d%d-%02d%02d%d-%s-%s-%tF-calc.csv.gz", 
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
		
		String[] lines = responseBody.split("\n");
		List<Stock> dayPrices = new ArrayList<Stock>();
		int index = 0;
		for (String line : lines)
			if (index++ > 0 && line.length() > 0)
				dayPrices.add(new Stock(line));
		Collections.reverse(dayPrices);
		
		index = 0;
		BigDecimal allTimeHighClose = new BigDecimal(0); 
		BigDecimal stopAt = null;
		BigDecimal boughtPrice = null;
		Stock sPrev = null;
		for (Stock s : dayPrices) {
			allTimeHighClose = allTimeHighClose.max(s.adjClose);
			s.allTimeHighClose = allTimeHighClose;
			if (index > 0) {
				sPrev = dayPrices.get(index-1);
				//true range = max(high,closeprev) - min(low,closeprev)
				s.trueRange = s.high.max(sPrev.adjClose).subtract(s.low.min(sPrev.adjClose));
			}
			int rng = 10;
			if (index > rng) {
				BigDecimal sum = new BigDecimal(0);
				for (Stock s2 : dayPrices.subList(index-rng, index))
					sum = sum.add(s2.trueRange);
				s.ATR10 = sum.divide(new BigDecimal(rng));

				if (allTimeHighClose.equals(s.adjClose)) {
					stopAt = s.adjClose.subtract(s.ATR10);
				}
			}
			
			s.stopAt = stopAt;
			
			if (s.stopAt != null && s.adjClose.compareTo(s.stopAt) == -1 
					&& sPrev != null && (sPrev.order == OrderAction.BUY || sPrev.order == OrderAction.HOLD) 
					) {
				s.order = OrderAction.SELL;
				s.soldPrice = s.adjClose;
				s.soldDifference = s.soldPrice.subtract(boughtPrice);
			} else if (allTimeHighClose.equals(s.adjClose) 
					&& stopAt != null
					&& sPrev != null && sPrev.order == OrderAction.IGNORE 
					) {
				s.order = OrderAction.BUY;
				boughtPrice = s.adjClose;
				s.boughtPrice = boughtPrice; 
			} else if (sPrev != null && (sPrev.order == OrderAction.HOLD || sPrev.order == OrderAction.BUY)) {
				s.order = OrderAction.HOLD;
			} else {
				s.order = OrderAction.IGNORE;
			}
			index++;
		}
		
		ServletOutputStream out = response.getOutputStream();
		out.println(lines[0]+",Split,All Time High Close,True Range,ATR10,Stop At,Order State,Bought Price,Sold Price,Sold Difference");
		for (Stock s : dayPrices)
			out.println(s.getCSV());
		
	}
	
	public enum OrderAction {
		BUY,
		HOLD,
		SELL,
		IGNORE
	}
	class Stock {
		private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		
		Date date;
		BigDecimal open;
		BigDecimal high;
		BigDecimal low;
		BigDecimal close;
		int volume;
		BigDecimal adjClose;

		BigDecimal split;
		
		BigDecimal trueRange;
		BigDecimal ATR10;
		BigDecimal allTimeHighClose;
		BigDecimal stopAt;
		
		OrderAction order;
		BigDecimal boughtPrice;
		BigDecimal soldPrice;
		BigDecimal soldDifference;

		public Stock(String csv) {
			String[] vals = csv.split(",");
			try {
				date = formatter.parse(vals[0]);
			} catch (ParseException e) {
				log.error(e);
			}
			open = new BigDecimal(vals[1]);
			high = new BigDecimal(vals[2]);
			low = new BigDecimal(vals[3]);
			close = new BigDecimal(vals[4]);
			volume = Integer.parseInt(vals[5]);
			adjClose = new BigDecimal(vals[6]);
			// normalize
			split = adjClose.divide(close, RoundingMode.HALF_EVEN);
			if (split.compareTo(BigDecimal.ONE) != 0) {
				// TODO: fix
				open = open.multiply(split);
				high = high.multiply(split);
				low = low.multiply(split);
				close = adjClose;
			}
		}
		
		public String getCSV() {
			String csv = formatter.format(date)+","+open+","+high+","+low+","+close+","+volume+","+adjClose+","
				+split+","+allTimeHighClose+","+trueRange+","+ATR10+","+stopAt+","
				+order+","+boughtPrice+","+soldPrice+","+soldDifference;
			return csv.replace("null", "");
		}
	}
}
