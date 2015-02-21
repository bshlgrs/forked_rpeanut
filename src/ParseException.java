

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


public class ParseException extends Exception {

	String line;
	String message;
	int num;
	String filename;
	
	public ParseException(String line, String message, int num, String filename) {
		this.line = line;
		this.message = message;
		this.num = num;
		this.filename = filename;
	}
	
	public ParseException(Lineinfo li, String message) {
		this.line = li.line;
		this.message = message;
		this.num = li.linenum;
		this.filename = li.filename;
	}
	
	@Override
	public String toString() {
		return "ParseException on line " + num + " in '" + filename + "': " + line + "\n" + message;
	}
	
}
