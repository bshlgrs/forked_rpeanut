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


public class Attribute {
	AttType type;
	String str;
	String att;
	Integer val;

	Lineinfo li;

	public Attribute(AttType type, String att, String str, Integer val, Lineinfo li) {
		super();
		this.type = type;
		this.att = att;
		this.str = str;
		this.val = val;
		this.li = li;
	}

	static Attribute parse(MyScanner scan,  Lineinfo li) throws ParseException {
		if (scan.hasNext()) {
			return parse(scan.next(), li);
		}
		return null;
	}

	static Attribute parse(String str, Lineinfo li) throws ParseException {
		String tstr = str.trim();

		if (tstr.length() == 0)
			return null;
		
		if (tstr.charAt(0) == '&') {
			return new Attribute(AttType.MACROLABEL, str, tstr.substring(1), 0, li);
		} else if (tstr.charAt(0) == '#') {
			tstr = tstr.substring(1);

			if (tstr.startsWith("0x")) {
				Long w;
				try {
					w = Long.parseLong(tstr.substring(2), 16);
				} catch (NumberFormatException nf2) {
					w = null;
				}
				return new Attribute(AttType.IVALUE, str, tstr, (int) (w & 0xffffffff), li);
			} else if (tstr.startsWith("\"")) {
				D.p("parse ISTRING : " + tstr);
				if (tstr.endsWith("\"")) {
					return new Attribute(AttType.ISTRING, str, tstr.substring(1, tstr.length()-1), 0, li);
				} else {
					throw new ParseException(li, "expecting matching quotes ");
				}
			} else if (tstr.startsWith("'")) {
				D.p("parse IVALUE char : " + tstr);
				if (tstr.length() < 2) throw new ParseException(li, "expecting a character after the single quote (note that ; needs escaping)");
				return new Attribute(AttType.IVALUE, str, tstr, (int) tstr.charAt(1), li);
			}
			Integer v;
			try {
				v = Integer.parseInt(tstr);
			} catch (NumberFormatException nf2) {
				v = null;
			}
			if (v != null) {
				return new Attribute(AttType.IVALUE, str, tstr, v, li);
			} else {
				return new Attribute(AttType.ILABEL, str, tstr, null, li);
			} // check for chars and strings
		} else if (tstr.equals("R0") || tstr.equals("R1") || tstr.equals("R2")
				|| tstr.equals("R3") || tstr.equals("R4") || tstr.equals("R5")
				|| tstr.equals("R6") || tstr.equals("R7") || tstr.equals("SP")
				|| tstr.equals("SR") || tstr.equals("PC") || tstr.equals("ONE")
				|| tstr.equals("ZERO") || tstr.equals("MONE")) {
			return new Attribute(AttType.REG, str, tstr, null, li);
		} else if (tstr.startsWith("0x")) {
			Long v;
			try {
				//System.out.println("b: " + tstr );
				v = Long.parseLong(tstr.substring(2), 16);
				//System.out.println("a: " + v );
			} catch (NumberFormatException nf2) {
				return null;
			}
			return new Attribute(AttType.VALUE, str, tstr, (int) (v & 0xffffffff), li);
		} else if (tstr.startsWith("\"")) {
			D.p("parse STRING : " + tstr);
			if (tstr.endsWith("\"")) {
				return new Attribute(AttType.STRING, str, tstr.substring(1, tstr.length()-1), 0, li);
			} else {
				throw new ParseException(li, "expecting matching quotes ");
			}
		} else {
			Integer v;
			try {
				v = Integer.parseInt(tstr);
			} catch (NumberFormatException nfe) {
				v = null;
			}
			if (v != null) {
				return new Attribute(AttType.VALUE, str, tstr, v, li);
			} else {
				return new Attribute(AttType.LABEL, str, tstr, null, li);
			}

			// need to add strings

		}

	}


	public int rcode()  throws ParseException {
		return rcode(str,li);
	}

	static public int rcode(String rs, Lineinfo li)  throws ParseException {
		if (rs.equals("R0")) {
			return 0;
		} else if (rs.equals("R1")) {
			return 1;
		} else if (rs.equals("R2")) {
			return 2;
		} else if (rs.equals("R3")) {
			return 3;
		} else if (rs.equals("R4")) {
			return 4;
		} else if (rs.equals("R5")) {
			return 5;
		} else if (rs.equals("R6")) {
			return 6;
		} else if (rs.equals("R7")) {
			return 7;
		} else if (rs.equals("SP")) {
			return 8;
		} else if (rs.equals("SR")) {
			return 9;
		} else if (rs.equals("PC")) {
			return 10;
		} else if (rs.equals("ONE")) {
			return 11;
		} else if (rs.equals("ZERO")) {
			return 12;
		} else if (rs.equals("MONE")) {
			return 13;
		}
		throw new ParseException(li, rs + " is not a register. Register expected ");
	}
}
