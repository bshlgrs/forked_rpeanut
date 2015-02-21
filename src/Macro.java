import java.util.ArrayList;

/*
rPeanut - is a simple simulator of the rPeANUt computer.
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

/*
A macro will start with the line:
MACRO
and end with the line:
MEND

 */

// Macro storage/resolving class
public class Macro {
	private ArrayList<String> args;
	private ArrayList<String> lines;
	
	public Macro() {
		args = new ArrayList<String>();
		lines = new ArrayList<String>();
	}
	
	public void addArgument(String arg) {
		args.add(arg);
	}
	
	public void addLine(String line) {
		lines.add(line);
	}
	
	// Get a String suitable for the Assemble.parse() function. This resolves
	// the macro arguments too
	public String getText(ArrayList<String> passargs, Lineinfo li) throws ParseException {
		if (passargs.size() != args.size()) {
			throw new ParseException(li, "calling macro with wrong number of arguments");
		}
		String text = "";
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String newline = "";
			int j = 0;
			while (j < line.length()) {
				if (line.charAt(j) == '&') {
					// Replacement 1: Replace those that are more like arguments (have following whitespace)
					
					String rest = line.substring(j+1);
					MyScanner lscn = new MyScanner(rest);
					String aname = lscn.untilSpace();
					int ai;
					if ((ai = args.indexOf(aname)) != -1) {
						int al = args.get(ai).length();
						j++;
						j += aname.length();
						newline +=  passargs.get(ai);
					} else if ((ai = indexofstartswith(args,aname)) != -1) {
						int al = args.get(ai).length();
						j++;
						j += al;
						newline +=  passargs.get(ai);
					} else {
						newline += line.charAt(j);
						j++;
					}
				} else {
					newline += line.charAt(j);
					j++;
				}
			}
			text += newline + "\n";
		}
		return text;
	}

	private int indexofstartswith(ArrayList<String> list, String aname) {
        for (int i = 0; i < list.size();i++) {
        	String element = list.get(i);
        	if (aname.startsWith(element)) return i;
        }
		return -1;
	}
}
