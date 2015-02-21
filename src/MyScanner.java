
/*
rPeanut - is a simple simulator of the rPeANUt computer.
Copyright (C) 2011  Eric McCreath
Copyright (C) 2012  Joshua Worth

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/



public class MyScanner {
	String str;
	int pos;

	public MyScanner(String str) {
		this.str = str;
		pos = 0;
	}

	public boolean hasNext() {
		return pos < str.length();
	}

	public int firstPos(char c) {
		boolean inquote = false; // ""
		boolean insquote = false; // ''
		boolean escape = false; // \
		while (pos < str.length()
				&& (str.charAt(pos) != c || insquote || inquote || escape)) {
			if (escape) {
				escape = false;
			} else {
			if (str.charAt(pos) == '\\') {
				escape = true;
			} else {
				if (str.charAt(pos) == '"')
					inquote = !inquote;
				if (str.charAt(pos) == '\'')
					insquote = !insquote;
			}
			}
			pos++;
		}
		if (pos < str.length()) {
			return pos;
		} else {
		   return -1;
		}
	}
	
	// Is first non-whitespace this character?
	public boolean isFirst(char c)
	{
		while (pos < str.length() && spacechar(str.charAt(pos)))
			pos++;
		if (pos == str.length()) return false;
		return str.charAt(pos) == c;
	}
	
	public String untilSpace()
	{
		int before = pos;
		while (pos < str.length() && !spacechar(str.charAt(pos)))
			pos++;
		return str.substring(before, pos - before);
	}
	
	private boolean spacechar(char c) {
		return c == ' ' || c == '\t';
	}
	
	public String next() {
		String res = "";
		boolean inquote = false;
		boolean insquote = false;
		boolean escape = false;
		while (pos < str.length() && spacechar(str.charAt(pos)))
			pos++;
		while (pos < str.length()
				&& (!spacechar(str.charAt(pos)) || inquote || insquote || escape)) {
			if (escape) {
				if (str.charAt(pos) == 'n') {
					res += '\n';
				} else {
				    res += str.charAt(pos);
				}
				escape = false;
			} else {
				if (str.charAt(pos) == '\\') {

					escape = true;
				} else {
					res += str.charAt(pos);
					if (str.charAt(pos) == '"')
						inquote = !inquote;
					if (str.charAt(pos) == '\'')
						insquote = !insquote;
				}
				
			}
			pos++;
		}
		while (pos < str.length() && spacechar(str.charAt(pos)))
			pos++;
		return res;
	}

}
