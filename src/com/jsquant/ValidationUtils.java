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

import javax.servlet.ServletException;

public class ValidationUtils {

	public static void validateResolution(String s) throws ServletException {
		char c = s.charAt(0);
		if (!(c=='d' || c=='w' || c=='m' || c=='y')) 
			throw new ServletException("Invalid Resolution");
	}

	public static void validateSymbol(String s) throws ServletException {
		for (int i=1; i<s.length(); i++) {
			char c = s.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c=='_' || c=='^' || c=='.'))
				throw new ServletException("Invalid Symbol");
		}
	}
	
}