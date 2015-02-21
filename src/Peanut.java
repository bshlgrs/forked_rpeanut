import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.*;
import java.util.prefs.Preferences;

/*
 rPeanut - is a simple simulator of the rPeANUt computer.
 Copyright (C) 2011,2012,2013,2014  Eric McCreath
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

/*
 * Some ideas for improvement from Nathan Rickerby
 * Okay, here are some of the things that came to mind as I was playing
 with rPeANUt.

 Double clicking on a memory location in the memory window could bring up
 the Poke dialogue with set to that memory address.

 A slider so you can adjust the size of the code window. Or the ability
 to disable it altogether. My workflow was editing with vim, switch to
 rpeanut, load last, assemble, run.

 A slider so you can increase the size of the memory display window. It
 often feels really cramped, you want to be able to make it much bigger
 so you can see more instructions.

 A toggle that makes the memory window scroll to display the currently
 running instruction. Or PC could be a button that when clicked makes the
 memory window jump to the instruction.

 Here's one that's probably a lot of effort. A profiler. An execution
 count for every instruction. It could be another column in the memory
 window. This would emphasise clearly which instructions needed attention
 for optimisation.

 The instruction count displayed somewhere in the rpeanut gui. So that
 people didn't need to use the command line interface to get the count.

 The option of changing the base that the registers are displayed in, or
 displaying them in more than one base (eg. hex and dec).

 Load input from a file when using the GUI. It's possible to do this by
 cutting and pasting into the terminal window, but I found this was a
 little platform dependent. It would be great if there was something you
 could click that loaded input from a file. This would have been useful
 when doing the graphical assignment.

 A pause button that does the same thing as a break point while it's
 running. Have the slow button work during execution, so you can switch
 to slow mode (save setting a break point and resuming with slow).

 A special label: that could be inserted into the code that would
 automatically add a breakpoint to the assembled code. The usefulness of
 this becomes apparent when you're re-adding breakpoints each time you
 assemble.

 An built in instruction set reference.

 Another big effort one. Different execution times for different
 instructions. This doesn't actually mean that some instructions should
 have a delay, but instead of raw instruction count, for every instruction
 add some time value then display the total time at the end instead of
 a total instruction count. The main point of this would be to load and
 store instructions that dealt with memory would be slower than those
 instructions that dealt with registers. A more advanced version of this
 might maintain list of things that would be in a real machines cache
 and only add a large time for cache misses. Then assess based on time,
 rather than instruction count.

 */

public class Peanut implements ActionListener, LayoutManager,
		WindowFocusListener {
	static final String version = "2.4";

	JFrame jframe;
	JMenuBar bar;
	JMenu fileMenu, editMenu, codeMenu;
	JMenuItem newMenuItem, saveMenuItem, saveAsMenuItem, loadMenuItem,
			loadlastMenuItem, exitMenuItem;
	JMenuItem undoMenuItem, redoMenuItem, fontSizeMenuItem;
	JMenuItem assembleMenuItem;
	JMenuItem runMenuItem;
	JMenuItem stepMenuItem;
	JMenuItem stopMenuItem;
	JCheckBoxMenuItem echoInputItem, profileItem;
	JMenuItem pipeMenuItem;
	JMenuItem cacheMenuItem;

	EditCode editcode;
	JButton assembleJButton;
	JButton pokeJButton;
	Simulate simulate;

	JFileChooser jfcs;

	Preferences prefs;

	static final String LOAD = "load";
	static final String LOADLAST = "loadlast";
	static final String SAVE = "save";
	private static final String SAVEAS = "saveas";
	static final String EXIT = "exit";
	static final String NEW = "new";
	static final String ASSEMBLE = "assemble";
	static final String RUN = "run";
	static final String STEP = "step";
	static final String POKE = "poke";
	static final String UNDO = "undo";
	static final String REDO = "redo";
	static final String STOP = "stop";
	static final String SHOWCACHE = "showcache";

	int lastpoke;
	private static final String FONTSIZE = "editfont";
	private static final String CHANGEECHO = "changeecho";
	private static final String CHANGEPROFILE = "changeprofile";
	private File currentFileName = null;
	private static final String PIPEFILE = "pipe";

	public Peanut() {
		lastpoke = 0;
		jframe = new JFrame("rPeANUt - " + version);

		jframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		jframe.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				exit();
			}
		});
		jframe.setExtendedState(jframe.getExtendedState()
				| JFrame.MAXIMIZED_BOTH);

		prefs = Preferences.userNodeForPackage(Peanut.class);

		bar = new JMenuBar();
		fileMenu = new JMenu("File");

		newMenuItem = new JMenuItem("New");
		newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
				ActionEvent.CTRL_MASK));
		newMenuItem.setActionCommand(NEW);
		newMenuItem.addActionListener(this);

		loadMenuItem = new JMenuItem("Load");
		loadMenuItem.setActionCommand(LOAD);
		loadMenuItem.addActionListener(this);
		loadMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
				ActionEvent.CTRL_MASK));

		loadlastMenuItem = new JMenuItem("Load Last");
		loadlastMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.CTRL_MASK));
		loadlastMenuItem.setActionCommand(LOADLAST);
		loadlastMenuItem.addActionListener(this);

		saveMenuItem = new JMenuItem("Save");
		saveMenuItem.setActionCommand(SAVE);
		saveMenuItem.addActionListener(this);
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.CTRL_MASK));

		saveAsMenuItem = new JMenuItem("Save As");
		saveAsMenuItem.setActionCommand(SAVEAS);
		saveAsMenuItem.addActionListener(this);
		saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK));

		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setActionCommand(EXIT);
		exitMenuItem.addActionListener(this);

		editMenu = new JMenu("Edit");
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
				ActionEvent.CTRL_MASK));
		undoMenuItem = new JMenuItem("Undo");
		undoMenuItem.setActionCommand(UNDO);
		undoMenuItem.addActionListener(this);

		undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				ActionEvent.CTRL_MASK));

		editMenu.add(undoMenuItem);

		fontSizeMenuItem = new JMenuItem("Change Font Size");
		fontSizeMenuItem.setActionCommand(FONTSIZE);
		fontSizeMenuItem.addActionListener(this);
		editMenu.add(fontSizeMenuItem);

		codeMenu = new JMenu("Code");

		assembleMenuItem = new JMenuItem("Assemble");
		assembleMenuItem.setActionCommand(ASSEMBLE);
		assembleMenuItem.addActionListener(this);
		assembleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B,
				InputEvent.CTRL_DOWN_MASK));
		codeMenu.add(assembleMenuItem);

		runMenuItem = new JMenuItem("Run");
		runMenuItem.setActionCommand(RUN);
		runMenuItem.addActionListener(this);
		runMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
				InputEvent.CTRL_DOWN_MASK));
		codeMenu.add(runMenuItem);

		stepMenuItem = new JMenuItem("Step");
		stepMenuItem.setActionCommand(STEP);
		stepMenuItem.addActionListener(this);
		stepMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
				InputEvent.CTRL_DOWN_MASK));
		codeMenu.add(stepMenuItem);

		stopMenuItem = new JMenuItem("Stop");
		stopMenuItem.setActionCommand(STOP);
		stopMenuItem.addActionListener(this);
		stopMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
				InputEvent.CTRL_DOWN_MASK));
		codeMenu.add(stopMenuItem);

		echoInputItem = new JCheckBoxMenuItem("Echo terminal input");
		echoInputItem.setState(prefs.getBoolean("echo", false));
		echoInputItem.setActionCommand(CHANGEECHO);
		echoInputItem.addActionListener(this);
		codeMenu.add(new JSeparator());
		codeMenu.add(echoInputItem);

		profileItem = new JCheckBoxMenuItem("Profile");
		profileItem.setState(prefs.getBoolean("profile", false));
		profileItem.setActionCommand(CHANGEPROFILE);
		profileItem.addActionListener(this);
		codeMenu.add(profileItem);

		pipeMenuItem = new JMenuItem("Input file to terminal");
		pipeMenuItem.setActionCommand(PIPEFILE);
		pipeMenuItem.addActionListener(this);
		codeMenu.add(pipeMenuItem);

		cacheMenuItem = new JMenuItem("Show cache simulator");
		cacheMenuItem.setActionCommand(SHOWCACHE);
		cacheMenuItem.addActionListener(this);
		codeMenu.add(cacheMenuItem);

		fileMenu.add(newMenuItem);
		fileMenu.add(loadMenuItem);
		fileMenu.add(loadlastMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.add(saveAsMenuItem);
		fileMenu.add(exitMenuItem);

		bar.add(fileMenu);
		bar.add(editMenu);
		bar.add(codeMenu);

		jframe.setJMenuBar(bar);

		JPanel mainpanel = new JPanel();
		JPanel editpanel = new JPanel();
		editpanel.setLayout(this);
		mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.PAGE_AXIS));

		JSplitPane split = new JSplitPane();
		split.setResizeWeight(1.0); // This means that the edit panel will
									// absorb any extra space given by resizing.
		// mainpanel.setLayout(this);

		editcode = new EditCode(prefs.getInt("fontsize", 16));

		assembleJButton = new JButton("Assemble");
		assembleJButton.addActionListener(this);
		assembleJButton.setActionCommand(ASSEMBLE);

		pokeJButton = new JButton("Poke");
		pokeJButton.addActionListener(this);
		pokeJButton.setActionCommand(POKE);

		editpanel.add(assembleJButton);
		editpanel.add(pokeJButton);
		editpanel.add(editcode);
		split.setLeftComponent(editpanel);
		// mainpanel.add(editpanel);

		simulate = new Simulate(false, prefs.getBoolean("echo", false),
				prefs.getBoolean("profile", false));
		split.setRightComponent(simulate);
		simulate.cache.setLocationRelativeTo(jframe);
		jframe.addWindowFocusListener(this);

		mainpanel.add(split);

		jframe.getContentPane().add(mainpanel);

		jfcs = new JFileChooser(".");

		jframe.pack();
		jframe.setVisible(true);
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals(EXIT)) {
			exit();
		} else if (ae.getActionCommand().equals(SAVE)) {
			save();
		} else if (ae.getActionCommand().equals(LOAD)) {
			load();
		} else if (ae.getActionCommand().equals(LOADLAST)) {
			loadLast();
		} else if (ae.getActionCommand().equals(ASSEMBLE)) {
			assemble();
		} else if (ae.getActionCommand().equals(POKE)) {
			poke();
		} else if (ae.getActionCommand().equals(NEW)) {
			newtext();
		} else if (ae.getActionCommand().equals(UNDO)) {
			editcode.undo();
		} else if (ae.getActionCommand().equals(REDO)) {
			editcode.redo();
		} else if (ae.getActionCommand().equals(RUN)) {
			simulate.runPush(Simulate.RunSpeed.NORMAL);
		} else if (ae.getActionCommand().equals(STEP)) {
			simulate.stepPush();
		} else if (ae.getActionCommand().equals(STOP)) {
			simulate.stopPush();
		} else if (ae.getActionCommand().equals(FONTSIZE)) {
			changeFont();
		} else if (ae.getActionCommand().equals(CHANGEECHO)) {
			simulate.toggleEcho();
			prefs.putBoolean("echo", simulate.getEchoInput());
		} else if (ae.getActionCommand().equals(CHANGEPROFILE)) {
			simulate.toggleProfile();
			prefs.putBoolean("profile", simulate.getProfile());
		} else if (ae.getActionCommand().equals(SAVEAS)) {
			saveas();
		} else if (ae.getActionCommand().equals(PIPEFILE)) {
			pipeFile();
		} else if (ae.getActionCommand().equals(SHOWCACHE)) {
			simulate.cache.setVisible(true);
		}
	}

	/**
	 * Shows the JFileChooser, which allows the user to select a file which is
	 * appended to the terminal's input string. If the terminal is set to echo
	 * input, it will also be printed to the terminal.
	 */
	private void pipeFile() {
		int res = jfcs.showOpenDialog(jframe);
		if (res == JFileChooser.APPROVE_OPTION) {
			File pipeFile = jfcs.getSelectedFile();
			D.p("Loading file: " + pipeFile);
			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						pipeFile));
				/*
				 * String line; while ((line = reader.readLine()) != null) { //
				 * Process piped line D.p(line); if (simulate.getEchoInput()) {
				 * for (char c : line.toCharArray()) {
				 * simulate.terminalAppend(c); } }
				 * simulate.terminalAppend('\n'); simulate.terminalChar += line
				 * + "\n"; }
				 */
				int c;
				while ((c = reader.read()) != -1) {
					if (simulate.getEchoInput())
						simulate.terminalAppend(c);
					simulate.terminalChar += ((char) c);
				}
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void changeFont() {
		String input = JOptionPane.showInputDialog("Enter a new font size: ");
		try {
			int size = Integer.parseInt(input);
			final Font font = new Font(Font.MONOSPACED, Font.PLAIN, size);
			editcode.text.setFont(font);
			editcode.ln.font = font;
			simulate.memtable.setFont(font);
			prefs.putInt("fontsize", size);
		} catch (NumberFormatException e) {
		}
	}

	private void poke() {

		Address address = new Address(lastpoke);
		Word data = new Word(0);
		JPanel panel = new JPanel(new GridLayout(2, 2));
		panel.add(new JLabel("Address:"));
		panel.add(address);
		panel.add(new JLabel("Data:"));
		panel.add(data);
		if (JOptionPane.showConfirmDialog(null, panel) == JOptionPane.OK_OPTION) {
			try {
				simulate.memory.set(address.get(), data.get());
				lastpoke = address.get() + 1;
				simulate.update();
			} catch (MemFaultException e) {
				e.printStackTrace();
			}
		}
	}

	private void assemble() {
		String text = editcode.text();
		try {
			if (currentFileName != null) {
				Assemble.assemble(text, simulate, currentFileName.getName());
			} else {
				Assemble.assemble(text, simulate, null);
			}
		} catch (ParseException e) {

			e.printStackTrace();
		}
	}

	private void exit() {
		simulate.cache.setAlwaysOnTop(false);
		if (editcode.changedFile) {
			Object[] options = { "Save and exit", "Exit without saving",
					"Cancel" };
			int r = JOptionPane
					.showOptionDialog(
							jframe,
							"Are you sure you want to quit? There are unsaved changes.",
							"Are you sure?", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE, null, options,
							options[0]);
			if (r == 0) {
				save();
				System.exit(0);
			} else if (r == 1) {
				System.exit(0);
			}
		} else {
			System.exit(0);
		}
		simulate.cache.setAlwaysOnTop(true);
	}

	private void save() {
		if (currentFileName == null) {
			saveas();
		} else {
			editcode.save(currentFileName);
		}
	}

	private void saveas() {
		int res = jfcs.showSaveDialog(jframe);
		if (res == JFileChooser.APPROVE_OPTION) {
			File file = jfcs.getSelectedFile();
			editcode.save(file);
			prefs.put("lastfile", file.getPath());
			setCurrentFile(file);
		}
	}

	private void newtext() {
		editcode.newtext();
		setCurrentFile(null);
	}

	private void load() {
		int res = jfcs.showOpenDialog(jframe);
		if (res == JFileChooser.APPROVE_OPTION) {
			try {
				File file = jfcs.getSelectedFile();
				editcode.load(file);
				prefs.put("lastfile", file.getPath());
				setCurrentFile(file);
			} catch (FileNotFoundException e) {
				System.out.println("Problem Loading.. " + e);
			} catch (IOException e) {
				System.out.println("Problem Loading.." + e);
			}
		}
	}

	private void setCurrentFile(File file) {
		currentFileName = file;
		if (file != null) {
			jframe.setTitle("rPeANUt - " + version + ": "
					+ currentFileName.getAbsolutePath());
		} else {
			jframe.setTitle("rPeANUt - " + version);
		}
	}

	private void loadLast() {
		String last = prefs.get("lastfile", null);
		if (last != null) {
			try {
				final File file = new File(last);
				editcode.load(file);
				setCurrentFile(file);
			} catch (FileNotFoundException e) {
				System.out.println("Problem Loading.. " + e);
			} catch (IOException e) {
				System.out.println("Problem Loading.." + e);
			}
		}
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String filename = null;
			boolean dumpframebuffer = false;
			boolean countsteps = false;
			boolean check = false;
			boolean screen = false;
			boolean objdump = false;
			boolean load = false;
			try {
				for (int i = 0; i < args.length; i++) {
					if (args[i].startsWith("-")) {
						if (args[i].equals("-dump")) {
							dumpframebuffer = true;
						} else if (args[i].equals("-count")) {
							countsteps = true;
						} else if (args[i].equals("-check")) {
							check = true;
						} else if (args[i].equals("-screen")) {
							screen = true;
						} else if (args[i].equals("-objdump")) {
							objdump = true;
						} else if (args[i].equals("-load")) {
							load = true;
						} else if (args[i].equals("-help")) {
							printHelpInfo(System.out);
							System.exit(0);
						} else {
							throw new ArgsException("unknown argument : "
									+ args[i]);
						}
					} else {
						if (filename != null)
							throw new ArgsException(
									"Only expecting one filename");
						filename = args[i];
					}
				}
				if (filename == null)
					throw new ArgsException("filename expected");

				if (load) {
					Peanut peanut = new Peanut();
					File file = new File(filename);
					peanut.editcode.load(file);
					peanut.setCurrentFile(file);
				} else {
					String text;

					text = EditCode.loadfile(new File(filename));

					Simulate simulate = new Simulate(true, false, false);

					if (check) {
						try {
							Assemble.assemble(text, simulate);
						} catch (ParseException e) {
							System.out.println("Problem : " + e);
							System.exit(1);
						}
						System.out.println("okay");
						System.exit(0);
					} else {
						Assemble.assemble(text, simulate);
						if (objdump) {
							simulate.memory.objdump(System.out);
						} else {

							if (screen) {
								JFScreen jscreen = new JFScreen(simulate);
								Thread t = new Thread(jscreen);
								t.start();
							}
							while (!simulate.halt) {
								simulate.step();
							}
							if (dumpframebuffer) {
								simulate.screen.dump(System.out);
							}
							if (countsteps) {
								System.out.println("\nCount : "
										+ simulate.count);
							}
						}
						System.exit(0);
					}

				}
			} catch (ParseException e) {
				System.out.println("Problem : " + e);
				System.exit(1);
			} catch (ArgsException ae) {
				System.err.println(ae);
				System.exit(1);
			} catch (FileNotFoundException e) {
				System.err.println("Unable to find file : " + filename);
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Problem reading file : " + filename);
				System.exit(1);
			}

		} else {
			new Peanut();
		}
	}

	private static void printHelpInfo(PrintStream out) {
		final String helpstr = "rPeANUt is a RISC version of the PeANUt computer. The simulator is written\n"
				+ "completely in Java at the beginning of 2011. The source code is GPL and \n"
				+ "available in the jar file.\n\n"
				+ "rPeANUt requires a Java Runtime Environment (JRE) or Java Development Kit (JDK)\n"
				+ "version 7 installed on your system (I use OpenJDK Runtime Environment, however,\n"
				+ "other Java implementation would generally also work). Once you have the JRE going\n"
				+ "rPeANUt should run in Windows, Mac, or Linux without too much trouble.\n\n"
				+ "To run the simulator with the GUI simply down load the jar and execute:\n"
				+ "    java -jar rPeANUt" + version + ".jar\n\n"
				+ "To run the simulator from the command-line just execute:\n"
				+ "    java -jar rPeANUt" + version + ".jar <code.s>\n\n"
				+ "rPeANUt has the following command line options:\n"
				+ "    -dump : this does a dump of the frame buffer once the computer halts.\n"
				+ "    -count : this produces a count of the instructions executed once the program halts.\n"
				+ "    -check : this checks whether a script can compile or not\n"
				+ "    -screen : this runs the program with only the display screen\n"
				+ "    -load : this opens the rPeANUt editor with the specified file\n"
				+ "    -objdump : this does a dump of the memory directly after it has assembled the specified program\n"
				+ "    -help : this help info\n";
		out.println(helpstr);
	}

	public void addLayoutComponent(String str, Component comp) {
	}

	public void layoutContainer(Container c) {
		Rectangle b = c.getBounds();
		// Dimension simm = simulate.getPreferredSize();
		Dimension abp = assembleJButton.getPreferredSize();
		// assume b is at 0,0
		int mpoint = b.width;
		// simulate.setLocation(mpoint, 0);
		// simulate.setSize(simm.width, b.height);

		assembleJButton.setLocation(mpoint - abp.width, 0);
		assembleJButton.setSize(abp.width, abp.height);

		pokeJButton.setLocation(0, 0);
		pokeJButton.setSize(abp.width, abp.height);

		editcode.setLocation(0, abp.height);
		editcode.setSize(mpoint, b.height - abp.height);

	}

	public Dimension minimumLayoutSize(Container c) {
		Dimension abm = assembleJButton.getMinimumSize();
		Dimension pbm = pokeJButton.getMinimumSize();
		Dimension simm = simulate.getMinimumSize();
		return new Dimension(abm.width + pbm.width, simm.height);
	}

	public Dimension preferredLayoutSize(Container c) {
		Dimension abp = assembleJButton.getPreferredSize();
		Dimension pbp = pokeJButton.getPreferredSize();
		Dimension simp = simulate.getPreferredSize();
		// return new Dimension(abp.width + pbp.width + simp.width,
		// simp.height);
		return new Dimension(abp.width + pbp.width, simp.height);
	}

	public void removeLayoutComponent(Component comp) {
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		D.p("focus gained");
		simulate.cache.setAlwaysOnTop(true);
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		D.p("focus lost");
		simulate.cache.setAlwaysOnTop(false);
	}
}
