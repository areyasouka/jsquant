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
package com.jsquant;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.MapMaker;

public class FileCache {
	static private Log log = LogFactory.getLog(FileCache.class);
		
	private File cacheDir;
	private Map<String, String> memCache = new MapMaker().softValues().makeMap();
	
	public FileCache(String cacheDirPath) {
		cacheDir = new File(cacheDirPath);
		cacheDir.mkdirs();
		try {
			log.info("cacheDir="+cacheDir.getCanonicalPath()+" exists="+cacheDir.exists());
		} catch (IOException e) {
			log.error("cacheDir="+cacheDir.getAbsolutePath()+" exists="+cacheDir.exists());
		}
	}

	public void put(String key, String value) throws IOException {
		File f = new File(cacheDir, key);
		synchronized (this) {
			memCache.put(key, value);
			if (!f.exists()) {
				log.info("writing cachePath="+f.getCanonicalPath());
				FileUtils.writeFile(f, value);
				//Files.write(value.getBytes(Charsets.UTF_8), f);
			}
		}
	}
	
	public String get(String key) throws IOException {
		String value = memCache.get(key);
		if (value == null) {
			File f = new File(cacheDir, key);
			if (f.exists()) {
				log.info("reading cachePath="+f.getCanonicalPath());
				value = FileUtils.readFile(f);
				//value = Files.toString(f, Charsets.UTF_8);
				memCache.put(key, value);
			}
		}
		return value;
	}

}