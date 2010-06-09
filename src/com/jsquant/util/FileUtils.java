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
package com.jsquant.util;

import java.io.BufferedInputStream;	
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jodd.util.collection.ByteArrayList;

public class FileUtils {
	private static final String DEFAULT_ENCODING = "UTF-8";

	public static String readFile(File f, String encoding) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		InputStream is = null;
		byte[] barray = new byte[8000];
		ByteArrayList bl = new ByteArrayList((int)f.length()*3);
		try {
			if (f.getName().endsWith(".gz"))
				is = new GZIPInputStream(new FileInputStream(f));
			else
				is = new BufferedInputStream(new FileInputStream(f));
			int numRead;
			while ((numRead=is.read(barray)) != -1) {
				byte[] ar = new byte[numRead];
				System.arraycopy(barray, 0, ar, 0, numRead);
				bl.addAll(ar);
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException err) {
				}
			}
		}
		return new String(bl.toArray(), encoding);
	}
	public static String readFile(File f) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		return readFile(f, DEFAULT_ENCODING);
	}

	public static void writeFile(File f, String content, String encoding) throws IOException {
		OutputStreamWriter out = null;
		try {
			if (f.getName().endsWith(".gz"))
				out = new OutputStreamWriter(new GZIPOutputStream(
						new FileOutputStream(f, false)), encoding);
			else
				out = new OutputStreamWriter(new FileOutputStream(f, false),
						encoding);
			out.append(content);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException err) {
				}
			}
		}
	}
	
	public static void writeFile(File f, String content) throws IOException {
		writeFile(f, content, DEFAULT_ENCODING);
	}

}
