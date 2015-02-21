import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JOptionPane;

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

public class Assemble {
	
	public static void assembleFile(String filename, Simulate simulate) throws ParseException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		assemble(sb.toString(), simulate, filename);
	}
	
	public static void assemble(String text, Simulate simulate) throws ParseException {
		assemble(text, simulate, null);
	}
	
	public static void assemble(String text, Simulate simulate, String filename) throws ParseException {
		assemble(text, simulate, filename, true, 0x0000, new HashMap<String, Integer>(),
				new HashMap<Integer, Symbol>(), new HashMap<String, Macro>(), new ArrayList<Lineinfo>());
		D.p("updating");
		simulate.update();
	}
	
	// This has lots of arguments because of "include"
	// text: The code to assemble
	// simulate: The machine to load instructions into
	// filename: The filename of the file being assembled (null = "untitled")
	// reset: Whether to reset the memory of the machine before compiling
	// curr: The memory address to write to
	// labels: Map of labels and their memory locations
	// symbols: Map of locations and the code which needs to be recompiled there
	// macros: Definitions of macros
	// callstack: Stack of lineinfo for the purpose of stack traces/useful errors
	// return value: curr, after it compiles
	private static int assemble(String text, Simulate simulate, String filename,
			boolean reset, int curr, HashMap<String, Integer> labels,
			HashMap<Integer, Symbol> symbols, HashMap<String, Macro> macros,
			ArrayList<Lineinfo> callstack) throws ParseException {
		if (filename == null) filename = "untitled";
		//HashMap<String, Integer> labels = new HashMap<String, Integer>();
		//HashMap<Integer, Symbol> symbols = new HashMap<Integer, Symbol>();
		//ArrayList<String> includes = new ArrayList<String>();

		Lineinfo li = null;
		try {
			try {
				if (reset) simulate.reset();

				@SuppressWarnings("resource")
				Scanner scan = new Scanner(text);
				int linenumber = 1;

				while (scan.hasNextLine()) {
					String line = scan.nextLine();
					li = new Lineinfo(line, linenumber, filename);
					D.p("========\n parsing line " + linenumber + " : " + line);
					
					// TODO: Change to non-escaped version:
					int commentPos = (new MyScanner(line)).firstPos(';');
					if (commentPos != -1)
					{
						line = line.substring(0, commentPos);
						D.p("comment removed : " + line);
					}
					
					// TODO: Change to non-escaped version:
					int colPos = (new MyScanner(line)).firstPos(':');
					if (colPos != -1) {
						String label = line.substring(0, colPos);
						label = label.trim();
						D.p("label : " + label);

						Attribute pl = Attribute.parse(label, li);
						if (pl.type == AttType.LABEL) {
							if (labels.get(pl.str) != null)
								throw new ParseException(li,
										"duplicate labels");
							labels.put(pl.str, curr);
						} else if (pl.type == AttType.VALUE) {
							if (curr > pl.val)
								throw new ParseException(
										li,
										"going back!! (your code appears to be trying to assemble into memory that has already passed)");
							curr = (pl.val).shortValue();
						} else {
							throw new ParseException(li,
									"label or integer expected");
						}
						line = line.substring(colPos + 1, line.length());
					}
					
					D.p("instruction to parse : \"" + line + "\"");
					// lets parse the instruction
					MyScanner inss = new MyScanner(line.trim());
					if (inss.hasNext()) {
						String ins = inss.next().toLowerCase();
						Integer code;
						if ((code = code(ins)) != null) {
							// add, sub, mult, div, mod, and, or, xor
							int r1 = parsereg(inss, li);
							int r2 = parsereg(inss, li);
							int rd = parsereg(inss, li);

							Word inst1 = new Word(code, r1, r2, rd, 0);
							simulate.memory.set(curr, inst1.get());
							D.p("setting memory : " + curr + " : " + inst1);
							curr++;
						} else if ((code = codeU(ins)) != null) {
							// neg, not, move
							int r1 = parsereg(inss, li);
							int rd = parsereg(inss, li);
							Word inst1 = new Word(0xA, code, r1, rd, 0);
							simulate.memory.set(curr, inst1.get());
							curr++;
						} else if (ins.equals("call")) {
							Attribute a1 = Attribute.parse(inss, li);
							if (a1.type == AttType.VALUE) {
								Word inst = new Word(0xA300, a1.val);
								simulate.memory.set(curr, inst.get());
								curr++;
							} else if (a1.type == AttType.LABEL) {
								symbols.put(curr, new Symbol(a1.str, li));
								Word inst = new Word(0xA300, 0);
								simulate.memory.set(curr, inst.get());
								curr++;
							} else {
								throw new ParseException(li,
										"address expected in call");
							}
						} else if (ins.equals("return")) {
							Word inst = new Word(0xA301, 0);
							simulate.memory.set(curr, inst.get());
							curr++;
						} else if (ins.equals("trap")) {
							Word inst = new Word(0xA302, 0);
							simulate.memory.set(curr, inst.get());
							curr++;
						} else if (ins.equals("jump")) {
							Attribute a1 = Attribute.parse(inss, li);
							if (a1.type == AttType.VALUE) {
								Word inst = new Word(0xA400, a1.val);
								simulate.memory.set(curr, inst.get());
								curr++;
							} else if (a1.type == AttType.LABEL) {
								symbols.put(curr, new Symbol(a1.str, li));
								Word inst = new Word(0xA400, 0);
								simulate.memory.set(curr, inst.get());
								curr++;
							} else {
								throw new ParseException(li,
										"address expected in call");
							}
						} else if (ins.equals("jumpz") || ins.equals("jumpn") || ins.equals("jumpnz")) {
							int r1 = parsereg(inss, li);
							int jcode = (ins.equals("jumpz") ? 0x1 : (ins.equals("jumpn") ? 0x2 : 0x3));

							Attribute a1 = Attribute.parse(inss, li);
							if (a1.type == AttType.VALUE) {
								Word inst = new Word(0xA, 0x4, jcode, r1,
										a1.val);
								simulate.memory.set(curr, inst.get());
								curr++;
							} else if (a1.type == AttType.LABEL) {
								symbols.put(curr, new Symbol(a1.str, li));
								Word inst = new Word(0xA, 0x4, jcode, r1, 0);
								simulate.memory.set(curr, inst.get());
								curr++;
							} else {
								throw new ParseException(li,
										"address expected in call");
							}
						} else if (ins.equals("set")) {
							int bit = parsebit(inss, li);
							Word inst1 = new Word(0xA, 0x5, 0x1, bit, 0);
							simulate.memory.set(curr, inst1.get());
							curr++;
						} else if (ins.equals("reset")) {
							int bit = parsebit(inss, li);
							Word inst1 = new Word(0xA, 0x5, 0x0, bit, 0);
							simulate.memory.set(curr, inst1.get());
							curr++;
						} else if (ins.equals("push")) {
							int r1 = parsereg(inss, li);
							Word inst1 = new Word(0xA, 0x6, 0x0, r1, 0);
							simulate.memory.set(curr, inst1.get());
							curr++;
						} else if (ins.equals("pop")) {
							int r1 = parsereg(inss, li);
							Word inst1 = new Word(0xA, 0x6, 0x1, r1, 0);
							simulate.memory.set(curr, inst1.get());
							curr++;
						} else if (ins.equals("rotate")) {
							Attribute a1 = Attribute.parse(inss, li);
							int r1 = parsereg(inss, li);
							int rd = parsereg(inss, li);
							if (a1.type != AttType.IVALUE &&
									a1.type != AttType.REG)
								throw new ParseException(li,
										" rotation requires immediate value or register");
							if (a1.type == AttType.IVALUE) {
								Word inst1 = new Word(0xB, 0, r1, rd, a1.val & 0x1f);
								simulate.memory.set(curr, inst1.get());
								curr++;
							} else  {
								Word inst1 = new Word(0xE, a1.rcode(), r1, rd, 0);
								simulate.memory.set(curr, inst1.get());
								curr++;
							}
						} else if (ins.equals("load")) {
							int instcode = 0xC;
							Attribute a1 = Attribute.parse(inss, li);
							Attribute a2 = Attribute.parse(inss, li);
							Attribute a3 = null;
							if (inss.hasNext())
								a3 = Attribute.parse(inss, li);
							if (a1.type == AttType.IVALUE
									&& a2.type == AttType.REG && a3 == null) {
								Word inst1 = new Word(0xC, 0x0, 0x0,
										a2.rcode(), a1.val);
								simulate.memory.set(curr, inst1.get());
								curr++;

							} else if (a1.type == AttType.ILABEL
									&& a2.type == AttType.REG && a3 == null) {
								symbols.put(curr, new Symbol(a1.str, li));
								Word inst1 = new Word(0xC, 0x0, 0x0,
										a2.rcode(), 0);
								simulate.memory.set(curr, inst1.get());
								curr++;
							} else if (a1.type == AttType.LABEL
									&& a2.type == AttType.REG && a3 == null) {
								symbols.put(curr, new Symbol(a1.str, li));
								Word inst1 = new Word(instcode, 0x1, 0x0, a2
										.rcode(), 0);
								simulate.memory.set(curr, inst1.get());
								curr++;
							} else if (a1.type == AttType.VALUE
									&& a2.type == AttType.REG && a3 == null) {
								Word inst1 = new Word(instcode, 0x1, 0x0, a2
										.rcode(), a1.val);
								simulate.memory.set(curr, inst1.get());
								curr++;

							} else if (a1.type == AttType.REG
									&& a2.type == AttType.REG && a3 == null) {
								Word inst1 = new Word(instcode, 0x2,
										a1.rcode(), a2.rcode(), 0);
								simulate.memory.set(curr, inst1.get());
								curr++;
							} else if (a1.type == AttType.REG
									&& (a2.type == AttType.IVALUE || a2.type == AttType.ILABEL)
									&& a3 != null && a3.type == AttType.REG) {

								if (a2.type == AttType.IVALUE) {
									Word inst1 = new Word(instcode, 0x3, a1
											.rcode(), a3.rcode(), a2.val);
									simulate.memory.set(curr, inst1.get());
									curr++;

								} else {
									symbols.put(curr, new Symbol(a2.str, li));
									Word inst1 = new Word(instcode, 0x3, a1
											.rcode(), a3.rcode(), 0);
									simulate.memory.set(curr, inst1.get());
									curr++;
								}

							} else {
								throw new ParseException(li, "unknown " + ins
										+ " attributes ");
							}
						} else if (ins.equals("store")) {
							int instcode = 0xD;
							Attribute a1 = Attribute.parse(inss, li);
							Attribute a2 = Attribute.parse(inss, li);
							Attribute a3 = null;
							if (inss.hasNext())
								a3 = Attribute.parse(inss, li);
							if ((a2.type == AttType.IVALUE || a2.type == AttType.ILABEL)
									&& a3 == null) {
								throw new ParseException(li,
										"you can't store an immediate ");

							} else if (a2.type == AttType.LABEL
									&& a1.type == AttType.REG && a3 == null) {
								symbols.put(curr, new Symbol(a2.str, li));
								Word inst1 = new Word(instcode, 0x1,
										a1.rcode(), 0x0, 0);
								simulate.memory.set(curr, inst1.get());
								curr++;

							} else if (a2.type == AttType.VALUE
									&& a1.type == AttType.REG && a3 == null) {
								Word inst1 = new Word(instcode, 0x1,
										a1.rcode(), 0x0, a2.val);

								simulate.memory.set(curr, inst1.get());
								curr++;

							} else if (a1.type == AttType.REG
									&& a2.type == AttType.REG && a3 == null) {
								Word inst1 = new Word(instcode, 0x2,
										a1.rcode(), a2.rcode(), 0);
								simulate.memory.set(curr, inst1.get());
								curr++;
							} else if (a1.type == AttType.REG
									&& (a2.type == AttType.IVALUE || a2.type == AttType.ILABEL)
									&& a3 != null && a3.type == AttType.REG) {

								if (a2.type == AttType.IVALUE) {
									Word inst1 = new Word(instcode, 0x3,
											a1.rcode(), a3.rcode(),a2.val);
									simulate.memory.set(curr, inst1.get());
									curr++;

								} else {
									symbols.put(curr, new Symbol(a2.str, li));
									Word inst1 = new Word(instcode, 0x3,
											a1.rcode(), a3.rcode(),0);
									simulate.memory.set(curr, inst1.get());
									curr++;
								}
							} else {
								throw new ParseException(li, "unknown " + ins
										+ " attributes ");
							}
						} else if (ins.equals("halt")) {
							simulate.memory.set(curr, new Word(0x0000,0x0000).get());
							curr++;
						} else if (ins.equals("block")) {
							Attribute a1 = Attribute.parse(inss, li);
							if (a1.type == AttType.VALUE) {
								for (int i = 0; i < a1.val; i++) {
									simulate.memory.set(curr, 0);
									curr++;
								}
							} else if (a1.type == AttType.IVALUE) {
								simulate.memory.set(curr, a1.val);
								curr++;
							} else if (a1.type == AttType.ISTRING) {
								for (int i = 0; i < a1.str.length(); i++) {
									simulate.memory.set(curr, new Word(0,a1.str
											.charAt(i)).get());
									curr++;
								}
								simulate.memory.set(curr, 0);
								curr++;
							} else if (a1.type == AttType.ILABEL) {
								symbols.put(curr, new Symbol(a1.str, li));
								// memory is set when symbols resolved
								curr++;
							}  else {
								throw new ParseException(li, "unknown " + ins
										+ " attributes ");
							}
						} else if (ins.equals("include")) {
							Attribute a1 = Attribute.parse(inss, li);
							if (a1.type == AttType.STRING) {
								try {
									FileInputStream fis = new FileInputStream(a1.str);
									int len = fis.available();
									byte[] fdata = new byte[len];
									fis.read(fdata);
									String ftext = new String(fdata, 0, len);
									callstack.add(li);
									curr = assemble(ftext, simulate, a1.str, false, curr, labels, symbols, macros, callstack);
									callstack.remove(callstack.size() - 1);
									fis.close();
								} catch (FileNotFoundException e) {
									throw new ParseException(li,
											"include: file not found '" + a1.str
											+ "'\nNote that the current working directory is "
											+ System.getProperty("user.dir"));
								} catch (Exception e) {
									e.printStackTrace();
									throw new ParseException(li,
											"include: error loading file '" + a1.str + "'");
								}
							} else {
								throw new ParseException(li,
										"argument of include should be string");
							}
						} else if (ins.equals("mend")) {
							throw new ParseException(li, "MEND without MACRO");
						} else if (ins.equals("macro")) {
							if (!scan.hasNextLine())
								throw new ParseException(li, "expected MEND");
							line = scan.nextLine();
							linenumber++;
							li = new Lineinfo(line, linenumber, filename);
							MyScanner macp = new MyScanner(line);
							// Get macro name
							String mname = macp.next().toLowerCase();
							if (mname.equals("mend")) {
								throw new ParseException(li,
										"macros must at least have a type line");
							}
							Macro mac = new Macro();
							// Read macro arguments
							while (macp.hasNext()) {
								Attribute a1 = Attribute.parse(macp, li);
								if (a1.type != AttType.MACROLABEL) {
									throw new ParseException(li,
											"arguments of first line in macro need to start with &");
								}
								mac.addArgument(a1.str);
							}
							// Read macro body lines
							boolean ended = false;
							while (scan.hasNextLine()) {
								line = scan.nextLine();
								linenumber++;
								macp = new MyScanner(line);
								if (macp.next().toLowerCase().equals("mend")) {
									ended = true;
									break;
								}
								mac.addLine(line);
							}
							if (!ended)
								throw new ParseException(li, "expected MEND");
							macros.put(mname, mac);
							D.p(macros.keySet().toString());
						} else if (macros.containsKey(ins)) {
							D.p("parsing macro...");
							ArrayList<String> passargs = new ArrayList<String>();
							while (inss.hasNext()) {
								Attribute att = Attribute.parse(inss, li);
								passargs.add(att.att);
							}
							Macro mac = macros.get(ins);
							String mactext = mac.getText(passargs, li);
							callstack.add(li);
							curr = assemble(mactext, simulate, "macro:" + ins, false, curr, labels, symbols, macros, callstack);
							callstack.remove(callstack.size() - 1);
							D.p("finished parsing macro");
						} else {
							throw new ParseException(li, "unknown instruction : " + ins);
						}
					}
					linenumber++;
				}
				if (reset) {
				// fill in all the missing symbols (now we know their locations)
				for (Integer add : symbols.keySet()) {
					Symbol label = symbols.get(add);
					Integer val = labels.get(label.symbol);
					if (val == null)
						throw new ParseException(label.li, "unknown symbol : "
								+ label.symbol);
					int inst = simulate.memory.get(add);
					simulate.memory.set(add, inst | val);

				}
			
					for (String lab : labels.keySet()) {
						Integer val = labels.get(lab);
						simulate.memory.setSymbol(val, lab);
					}
				
				// Stop recursive compiles from adding the same label more than once
				//labels.clear();
				simulate.memory.resetProfile();
				}
			} catch (MemFaultException mfe) {
				throw new ParseException(li,
						"memory out of range (MemFaultException)");
			} catch (NullPointerException npe) {
				if (D.debug) npe.printStackTrace();
				throw new ParseException(li,
						"assembler expecting something that is missing??? (NullPointerException)");
			} catch (ParseException pe) {
				throw pe;
			}/* catch (Exception npe) {
				throw new ParseException(li, "assembler had some problem!!! "
						+ npe.toString());
			}*/
		} catch (ParseException pe) {
			String err = pe.toString();
			for (int i = callstack.size() - 1; i >= 0; i--) {
				err += "\ncalled from line " + callstack.get(i).linenum + " in '" + callstack.get(i).filename + "'";
			}
			if (simulate.term) {
				System.err.println(err);
				throw pe;
			} else {
				JOptionPane.showMessageDialog(null, err);
			}
		}
		return curr;
	}

	private static int parsebit(MyScanner inss, Lineinfo li)
			throws ParseException {
		if (!inss.hasNext())
			throw new ParseException(li, "expecting a bit eg IM OF, or TI");
		String rs = inss.next();
		if (rs.equals("OF")) {
			return 0x0;
		} else if (rs.equals("IM")) {
			return 0x1;
		} else if (rs.equals("TI")) {
			return 0x2;
		}
		throw new ParseException(li, "unknown bit : " + rs);
	}

	private static Integer code(String ins) {

		if (ins.equals("add")) {
			return 0x1;
		} else if (ins.equals("sub")) {
			return 0x2;
		} else if (ins.equals("mult")) {
			return 0x3;
		} else if (ins.equals("div")) {
			return 0x4;
		} else if (ins.equals("mod")) {
			return 0x5;
		} else if (ins.equals("and")) {
			return 0x6;
		} else if (ins.equals("or")) {
			return 0x7;
		} else if (ins.equals("xor")) {
			return 0x8;
		} else {
			return null;
		}
	}

	private static Integer codeU(String ins) {
		if (ins.equals("neg")) {
			return 0x0;
		} else if (ins.equals("not")) {
			return 0x1;
		} else if (ins.equals("move")) {
			return 0x2;
		} else {
			return null;
		}
	}

	private static int parsereg(MyScanner inss, Lineinfo li)
			throws ParseException {
		if (!inss.hasNext())
			throw new ParseException(li, "expecting a register");
		String rs = inss.next();
		return Attribute.rcode(rs, li);
	}
}
