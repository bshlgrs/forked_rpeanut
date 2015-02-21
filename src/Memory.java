import java.io.IOException;
import java.io.PrintStream;
import javax.swing.table.AbstractTableModel;

/*
rPeanut - is a simple simulator of the rPeANUt computer.
Copyright (C) 2011-2012  Eric McCreath
Copyright (C) 2012  Tim Sergeant
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


public class Memory extends AbstractTableModel {

	static final int addressSize = 1 << 15;

	class Cell {
		public int value;
		public int offset;
		public int mempos;
		public int profilecount;
		public boolean bp;
		public String label;
		public String dump;
	}
	
	private Cell memory[];
	
	private int count;
	private boolean offsetwrong;
	Simulate simulate;
	
	static final boolean profile = true;

	public Memory(Simulate sim) {
		D.p("newmem " + count);
		memory = new Cell[addressSize];
		for (int i = 0; i < memory.length; i++) {
			memory[i] = new Cell();
		}
		count = 0;
		simulate = sim;
	}

	public void set(int add, int value) throws MemFaultException {

		if (add == 0xFFF0) {
			if (simulate.term) {
				System.out.print(String.format("%c", 0xFF & value));
				System.out.flush();
			} else {
				simulate.terminalAppend(value);
			}
		} else if (add == 0xFFF2) {
			simulate.terminalCharInterrupt = (value & 0x0001) == 0x0001;
		} else {
			if (add < 0 || add > 0x7FFF)
				throw new MemFaultException();
			offsetwrong = true;
			memory[add].value = value;
		}
	}

	public void setHighlight() {
		int pc = simulate.PC.get();
		if (pc >= 0 && pc < memory.length) {
			int index = memory[simulate.PC.get()].mempos;
			if (index != -1 && index < simulate.memtable.getRowCount())
				simulate.memtable.setRowSelectionInterval(index, index);
		}
	}

	public int get(int add) throws MemFaultException {
		return get(add, true);
	}
	
	public int get(int add, boolean doprofile) throws MemFaultException {
		if (doprofile && add >= 0 && add < memory.length) memory[add].profilecount++;
		if (add == 0xFFF0) {
			if (simulate.term) {
				try {
					if (System.in.available() > 0) {
						int tchar = System.in.read();
						return tchar;
					} else {
						return 0;
					}
				} catch (IOException e) {

					return 0;
				}
			} else {
				if (simulate.terminalChar.length() == 0) {
					return 0;
				} else {
					int c = simulate.terminalChar.charAt(0);
					simulate.terminalChar = simulate.terminalChar.substring(1);
					return c;

				}

			}
		}
		if (add == 0xFFF1) {
			if (simulate.term) {
				try {
					return (System.in.available() > 0 ? 0x0001 : 0x0000);
				} catch (IOException e) {
					return (0x0000);

				}
			} else {
				return (simulate.terminalChar.length() > 0 ? 0x0001 : 0x0000);
			}
		}

		if (add == 0xFFF2)
			return (simulate.terminalCharInterrupt ? 0x0001 : 0x0000);
		if (add < 0 || add > 0x7FFF)
			throw new MemFaultException();
		
		return memory[add].value;
	}
	
	boolean inRange(int addr) {
		return (addr >= 0 && addr <= addressSize);
	}

	public void reset() {

		count = 0;
		offsetwrong = false;
		for (int i = 0; i < addressSize; i++) {
			memory[i].offset = 0;
			memory[i].value = 0;
			memory[i].profilecount = 0;
			memory[i].label = null;
			memory[i].dump = null;
			memory[i].bp = false;
		}
	}

	@Override
	public int getColumnCount() {
		return 6;
	}

	@Override
	public int getRowCount() {

		if (offsetwrong)
			caloffset();

		return count;
	}

	@Override
	public Object getValueAt(int r, int c) {

		if (offsetwrong)
			caloffset();
		if (c == 0) {
			return memory[memory[r].offset].bp;
		} else if (c == 1) {
			return profile ? memory[memory[r].offset].profilecount : "";
		} else if (c == 3) {
			return new Address(memory[r].offset);
		} else if (c == 4 && r >= 0 && r < count) {
			return new Word(memory[memory[r].offset].value);
		} else if (c == 2 && memory[memory[r].offset].label != null) {
			return memory[memory[r].offset].label;
		} else if (c == 5 && memory[memory[r].offset].dump != null) {
			return memory[memory[r].offset].dump;
		}
		return null;
	}
	
	// This is a breakpoint..., why is it called DB and not bp?
	public boolean isDB(int col) {
		return memory[memory[col].offset].bp;
	}
	
	public boolean isDBmem(int i) {
		return i >=0 && i <= 0x7fff && memory[i].bp;
	}
	
	public void toggleDB(int col) {
		memory[memory[col].offset].bp = !memory[memory[col].offset].bp;
		this.fireTableDataChanged();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int c) {
		return c==0;
	}

	private void caloffset() {
		count = 0;
		for (int i = 0; i < addressSize; i++) {
			if (memory[i].value != 0 || i == simulate.PC.get() || i-1 == simulate.PC.get() || (i>0 && memory[i-1].value != 0)  || (i>1 && memory[i-2].value != 0)) {
				memory[count].offset = i;
				memory[i].mempos = count;
				count++;
			} else {
				memory[i].mempos = -1;
			}
		}
		
		int i = 0;
		while (i < addressSize) {
			if ((memory[i].value & 0xF0000000) == 0x10000000) {
				memory[i].dump = "add";
			} else if ((memory[i].value & 0xF0000000) == 0x20000000) {
				memory[i].dump = "sub";
			} else if ((memory[i].value & 0xF0000000) == 0x30000000) {
				memory[i].dump = "mult";
			} else if ((memory[i].value & 0xF0000000) == 0x40000000) {
				memory[i].dump = "div";
			} else if ((memory[i].value & 0xF0000000) == 0x50000000) {
				memory[i].dump = "mod";
			} else if ((memory[i].value & 0xF0000000) == 0x60000000) {
				memory[i].dump = "and";
			} else if ((memory[i].value & 0xF0000000) == 0x70000000) {
				memory[i].dump = "or";
			}  else if ((memory[i].value & 0xF0000000) == 0x80000000) {
				memory[i].dump = "xor";
			} else if ((memory[i].value & 0xFF000000) == 0xA0000000) {
				memory[i].dump = "neg";
			} else if ((memory[i].value & 0xFF000000) == 0xA1000000) {
				memory[i].dump = "not";
			} else if ((memory[i].value & 0xFF000000) == 0xA2000000) {
				memory[i].dump = "move";
			} else if ((memory[i].value & 0xFFFF0000) == 0xA3000000) {
				memory[i].dump = "call";
			} else if ((memory[i].value & 0xFFFF0000) == 0xA3010000) {
				memory[i].dump = "return";
			} else if ((memory[i].value & 0xFFFF0000) == 0xA3020000) {
				memory[i].dump = "trap";
			} else if ((memory[i].value & 0xFFFF0000) == 0xA3020000) {
				memory[i].dump = "trap";
			} else if ((memory[i].value & 0xFFFF0000) == 0xA4000000) {
				memory[i].dump = "jump";
			} else if ((memory[i].value & 0xFFF00000) == 0xA4100000) {
				memory[i].dump = "jumpz";
			} else if ((memory[i].value & 0xFFF00000) == 0xA4200000) {
				memory[i].dump = "jumpn";
			} else if ((memory[i].value & 0xFFF00000) == 0xA4300000) {
				memory[i].dump = "jumpnz";
			} else if ((memory[i].value & 0xFFF00000) == 0xA5000000) {
				memory[i].dump = "reset";
			} else if ((memory[i].value & 0xFFF00000) == 0xA5100000) {
				memory[i].dump = "set";
			} else if ((memory[i].value & 0xFFF00000) == 0xA6000000) {
				memory[i].dump = "push";
			} else if ((memory[i].value & 0xFFF00000) == 0xA6100000) {
				memory[i].dump = "pop";
			} else if ((memory[i].value & 0xFF000000) == 0xB0000000) {
				memory[i].dump = "rotate";
			} else if ((memory[i].value & 0xF0000000) == 0xE0000000) {
				memory[i].dump = "rotate";
			} else if ((memory[i].value & 0xFFF00000) == 0xC0000000) {
				memory[i].dump = "load #";
			} else if ((memory[i].value & 0xFFF00000) == 0xC1000000) {
				memory[i].dump = "load abs";
			} else if ((memory[i].value & 0xFF000000) == 0xC2000000) {
				memory[i].dump = "load indir";
			} else if ((memory[i].value & 0xFF000000) == 0xC3000000) {
				memory[i].dump = "load b+d";
			} else if ((memory[i].value & 0xFF0F0000) == 0xD1000000) {
				memory[i].dump = "store abs";
			} else if ((memory[i].value & 0xFF000000) == 0xD2000000) {
				memory[i].dump = "store indir";
			} else if ((memory[i].value & 0xFF000000) == 0xD3000000) {
				memory[i].dump = "store b+d";
			} else if ((memory[i].value & 0xFFFFFFFF) == 0x00000000) {
				memory[i].dump = "halt";
			} else {
				memory[i].dump = null;
			}
				
			i++;
		}
		
		offsetwrong = false;
	}

	public void setSymbol(int add, String label) {
		if (memory[add].label == null) {
			memory[add].label = "";
		}
		memory[add].label += label + " ";
	}

	public void resetProfile() {
		count = 0;
		for (int i = 0; i < addressSize; i++) {
			memory[i].profilecount = 0;
		}
	}

	public void objdump(PrintStream out) {
		for (int i = 0; i < addressSize; i++) {
			if (memory[i].value != 0) {
				out.println(new Address(i) + " " + new Word(memory[i].value));
			}
		}
	}
}
