import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

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

// The cache simulator
// Memory is still fetched as normal, but this shows what the cache should look like
public class Cache extends JFrame implements ActionListener {
	enum Mode {
		NONE,
		FULLY,
		DIRECT,
		SET
	}

	static final String NONE_RADIO = "none";
	static final String FULLY_RADIO = "fully";
	static final String DIRECT_RADIO = "direct";
	static final String SET_RADIO = "set";

	Memory mem;
	Mode mode;

	int count;

	int cachesize;
	int linesize;
	int setsize;
	
	int hits;
	int accesses;

	class Line {
		String id;
		int loc; // Location
		int words[]; // The data
		//int size; // The line size, this MUST be identical to the Cache linesize variable
		boolean valid; // If this line is assigned to memory yet
		int access; // Time when line was last accessed
		int tag;

		public Line(int size) {
			id = "";
			loc = 0;
			words = new int[size];
			//this.size = size;
			valid = false;
			access = 0;
			tag = 0;
		}

		public Object[] data(Mode mode) {
			String tagstr = "";
			tagstr = String.format("0x%04X", tag);

			String ds = "";
			if (words.length > 0) {
				for (int i = 0; i < words.length - 1; i++) {
					ds += String.format("0x%08X", words[i]) + ", ";
				}
				ds += String.format("0x%08X", words[words.length - 1]);
			}
			return new Object[] {id,tagstr, ds, valid ? "YES" : "NO"};
		}
	}

	Line lines[];

	// GUI stuff:

	JPanel panel;
	JPanel buttonPanel;
	JPanel statusPanel;

	JLabel cacheLabel;
	JRadioButton noneRadio;
	JRadioButton fullyRadio;
	JRadioButton directRadio;
	JRadioButton setRadio;
	JLabel statusLabel;
	JLabel actionLabel;
	JLabel rateLabel;
	JLabel timeLabel;

	ButtonGroup modeGroup;

	JTable linesTable;
	TableColumn idColumn;
	boolean idColumnVisible;
	DefaultTableModel linesTableModel;
	JScrollPane linesScroll;

	JPanel cacheSliderPanel;
	JSlider cacheSlider;
	JLabel cacheSliderLabel;

	JPanel lineSliderPanel;
	JSlider lineSlider;
	JLabel lineSliderLabel;

	JPanel setSliderPanel;
	JSlider setSlider;
	JLabel setSliderLabel;

	public Cache(Memory mem) {
		this.mem = mem;

		this.setTitle("Cache simulator");

		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				setMode(Mode.NONE);
				modeGroup.setSelected(noneRadio.getModel(), true);
			}
		});
		// Code for always-on-top is in Peanut.java

		panel = new JPanel();

		buttonPanel = new JPanel();
		statusPanel = new JPanel();
		//this.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		cacheLabel = new JLabel("Cache mode: ");

		noneRadio = new JRadioButton("Off");
		noneRadio.setActionCommand(NONE_RADIO);
		noneRadio.addActionListener(this);
		noneRadio.setSelected(true);

		fullyRadio = new JRadioButton("Fully associative");
		fullyRadio.setActionCommand(FULLY_RADIO);
		fullyRadio.addActionListener(this);

		directRadio = new JRadioButton("Direct associative");
		directRadio.setActionCommand(DIRECT_RADIO);
		directRadio.addActionListener(this);

		setRadio = new JRadioButton("Set associative");
		setRadio.setActionCommand(SET_RADIO);
		setRadio.addActionListener(this);

		buttonPanel.add(cacheLabel);
		buttonPanel.add(noneRadio);
		buttonPanel.add(fullyRadio);
		buttonPanel.add(directRadio);
		buttonPanel.add(setRadio);

		modeGroup = new ButtonGroup();
		modeGroup.add(noneRadio);
		modeGroup.add(fullyRadio);
		modeGroup.add(directRadio);
		modeGroup.add(setRadio);

		statusLabel = new JLabel("WAITING");
		statusLabel.setOpaque(true);
		statusLabel.setBackground(Color.WHITE);

		actionLabel = new JLabel();
		
		rateLabel = new JLabel();

		timeLabel = new JLabel("Time: 0ns");
		timeLabel.setPreferredSize(new Dimension(100, 20));
		statusPanel.add(statusLabel);
		statusPanel.add(actionLabel);

		buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
		statusPanel.setAlignmentX(LEFT_ALIGNMENT);

		// Setup the table:
		Object[] tableNames = {"ID", "Tag", "Data", "Valid"};
		Object[][] tableData = {};
		linesTableModel = new DefaultTableModel(tableData, tableNames) {

		};
		linesTable = new JTable(linesTableModel) {
			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false;
			}
			public Component prepareRenderer(
					TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				JComponent jc = (JComponent)c;
				// All the pretty table rendering happens here:
				if (column == 0 && mode != Mode.FULLY) {
					c.setBackground(Color.LIGHT_GRAY);
				} else {
					c.setBackground(Color.WHITE);
				}
				if (row != 0 && row % setsize == 0 && mode == Mode.SET) {
					jc.setBorder(new MatteBorder(1, 0, 0, 0, Color.RED));
				}
				if (column == linesTable.getColumnCount() - 1) {
					jc.setBackground(lines[row].valid ? new Color(200,255,200) : new Color(255,200,200));
				}
				return c;
			}
		};
		// Keep details on the ID column so it can be removed/replaced
		idColumn = linesTable.getColumnModel().getColumn(0);
		idColumnVisible = true;
		idColumn.setMaxWidth(50);
		linesTable.getColumnModel().getColumn(1).setMaxWidth(200);
		linesTable.getColumnModel().getColumn(3).setMaxWidth(50);
		linesScroll = new JScrollPane(linesTable);

		// Cache size slider:
		cachesize = 16;
		cacheSliderPanel = new JPanel();
		cacheSlider = new JSlider(JSlider.HORIZONTAL, 0, 8, (int)Math.sqrt(cachesize));
		cacheSlider.setPaintTicks(true);
		cacheSliderLabel = new JLabel("Cache size: " + cachesize);
		cacheSliderPanel.add(cacheSliderLabel);
		cacheSliderPanel.add(cacheSlider);

		// TODO: Fix set max
		cacheSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				cachesize = (int)Math.pow(2, cacheSlider.getValue());
				cacheSliderLabel.setText("Cache size: " + cachesize);

				setsize = Math.min(cacheSlider.getValue(), setsize);
				setSlider.setMaximum(cacheSlider.getValue());
				setSlider.setValue(setsize);

				initlines();
			}
		});

		// Line size slider:
		linesize = 8;
		lineSliderPanel = new JPanel();
		lineSlider = new JSlider(JSlider.HORIZONTAL, 0, 8, (int)Math.sqrt(linesize));
		lineSlider.setPaintTicks(true);
		lineSliderLabel = new JLabel("Line size: " + linesize);
		lineSliderPanel.add(lineSliderLabel, BorderLayout.LINE_START);
		lineSliderPanel.add(lineSlider, BorderLayout.CENTER);
		lineSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				linesize = (int)Math.pow(2, lineSlider.getValue());
				lineSliderLabel.setText("Line size: " + linesize);
				initlines();
			}
		});

		// Set size slider:
		setsize = 4;
		setSliderPanel = new JPanel();
		setSlider = new JSlider(JSlider.HORIZONTAL, 0, (int)Math.sqrt(cachesize), (int)Math.sqrt(setsize));
		setSlider.setPaintTicks(true);
		setSliderLabel = new JLabel("Set size: " + setsize);
		setSliderPanel.add(setSliderLabel, BorderLayout.LINE_START);
		setSliderPanel.add(setSlider, BorderLayout.CENTER);
		setSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				setsize = (int)Math.pow(2, setSlider.getValue());
				setSliderLabel.setText("Set size: " + setsize);
				initlines();
			}
		});

		panel.add(buttonPanel);
		panel.add(cacheSliderPanel);
		panel.add(lineSliderPanel);
		panel.add(setSliderPanel);
		panel.add(rateLabel);
		panel.add(timeLabel);
		panel.add(statusPanel);
		panel.add(linesScroll);

		setContentPane(panel);
		this.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		pack();

		setMode(Mode.NONE);
	}

	void initlines() {
		linesTableModel.setRowCount(0);
		lines = new Line[cachesize];
		for (int i = 0; i < cachesize; i++) {
			lines[i] = new Line(linesize);
		}
		switch (mode) {
		case FULLY:
		case DIRECT:
			for (int i = 0; i < cachesize; i++) {
				lines[i].id = String.format("0x%x", i);
			}
			break;
		case SET:
			for (int i = 0; i < cachesize; i += setsize) {
				lines[i].id = String.format("0x%x", i / setsize);
			}
			break;
		default:
			break;
		}
		for (int i = 0; i < cachesize; i++) {
			linesTableModel.addRow(lines[i].data(mode));
		}
	}

	public int get(int addr) throws MemFaultException {
		if (!mem.inRange(addr)) return mem.get(addr);
		actionLabel.setText("");
		int ret = 0;
		switch (mode) {
		case NONE:
			ret = mem.get(addr);
			addClocks(200);
			break;
		case FULLY:
		case DIRECT:
		case SET:
			if (hit(addr)) {
				addClocks(3);
			} else {
				loadCache(addr);
				addClocks(200);
			}
			ret = mem.get(addr);
			break;
		default:
			break;
		}
		return ret;
	}

	public void set(int addr, int val) throws MemFaultException {
		if (!mem.inRange(addr)) {
			mem.set(addr, val);
			return;
		}
		actionLabel.setText("");
		switch (mode) {
		case NONE:
			mem.set(addr, val);
			addClocks(200);
			break;
		case FULLY:
		case DIRECT:
		case SET:
			if (hit(addr)) {
				addClocks(3);
			} else {
				addClocks(200);
			}
			mem.set(addr, val);
			break;
		default:
			break;
		}
	}

	boolean hit(int addr) {
		boolean hit = false;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].valid && lines[i].loc <= addr && lines[i].loc + linesize > addr) {
				hit = true;
				lines[i].access = count;
			}
		}
		if (hit) {
			statusLabel.setText(String.format("HIT 0x%04X", addr));
			statusLabel.setBackground(Color.GREEN);
			statusLabel.setForeground(Color.BLACK);
		} else {
			statusLabel.setText(String.format("MISS 0x%04X", addr));
			statusLabel.setBackground(Color.RED);
			statusLabel.setForeground(Color.WHITE);
		}
		if (hit) {
			hits++;
		}
		accesses++;
		if (accesses != 0) {
			float hitrate = ((float)hits / accesses);
			rateLabel.setText("Hits = " + hits + ", Accesses = " + accesses + ", Hit Rate = " + (int)(hitrate * 100) + "% (" + hitrate + ")");
		}
		return hit;
	}

	void loadCache(int addr) throws MemFaultException {
		int lineid = addr / linesize; // Index in the memory
		addr = lineid * linesize; // Floor it to the nearest linesize multiple
		int lid = -1; // Index in the cache
		Line line = null;
		switch (mode) {
		case FULLY: // Each chunk can go anywhere
			for (int i = 0; i < lines.length; i++) {
				if (!lines[i].valid) {
					line = lines[i];
					lid = i;
					break;
				}
			}
			if (line == null) {
				line = lines[0];
				lid = 0;
				int min = lines[0].access;
				for (int i = 1; i < lines.length; i++) {
					if (lines[i].access < min) {
						line = lines[i];
						lid = i;
						min = line.access;
					}
				}
			}
			line.tag = lineid;
			break;
		case DIRECT: // Each chunk has a specific line
			lid = lineid % cachesize;
			line = lines[lid];
			line.tag = lineid / cachesize;
			break;
		case SET: // Each chunk has a specific set of lines (Mixed fully/direct)
			if (setsize > cachesize) setsize = cachesize; // Prevent (possible?) errors...

			int firstline = lineid % (cachesize / setsize) * setsize; // First line of a set

			// Get the actual line
			for (int i = firstline; i < firstline + setsize; i++) {
				if (!lines[i].valid) {
					line = lines[i];
					lid = i;
					break;
				}
			}
			if (line == null) {
				line = lines[firstline];
				lid = firstline;
				int min = lines[firstline].access;
				for (int i = firstline + 1; i < firstline + setsize; i++) {
					if (lines[i].access < min) {
						line = lines[i];
						lid = i;
						min = line.access;
					}
				}
			}
			line.tag = lineid / (cachesize / setsize);
			break;
		case NONE:
			break;
		}

		line.loc = addr;
		for (int i = 0; i < line.words.length; i++) {
			line.words[i] = mem.get(addr + i, false); // false - don't do profile
		}
		line.valid = true;
		linesTableModel.insertRow(lid, line.data(mode));
		linesTableModel.removeRow(lid + 1);
	}

	void addClocks(int n) {
		count += n;
		timeLabel.setText("Time: " + count + "ns");
		actionLabel.setText("(+" + n + ")");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String c = e.getActionCommand();
		if (c.equals(NONE_RADIO)) {
			setMode(Mode.NONE);
		} else if (c.equals(FULLY_RADIO)) {
			setMode(Mode.FULLY);
		} else if (c.equals(DIRECT_RADIO)) {
			setMode(Mode.DIRECT);
		}  else if (c.equals(SET_RADIO)) {
			setMode(Mode.SET);
		}
	}

	public void reset() {
		setMode(mode);
	}

	private void setMode(Mode mode) {
		this.mode = mode;
		switch (mode) {
		case NONE:
			statusPanel.setVisible(false);
			linesScroll.setVisible(false);
			cacheSliderPanel.setVisible(false);
			lineSliderPanel.setVisible(false);
			break;
		case SET:
		case FULLY:
		case DIRECT:
			statusPanel.setVisible(true);
			linesScroll.setVisible(true);
			cacheSliderPanel.setVisible(true);
			lineSliderPanel.setVisible(true);
			initlines();
			break;
		}
		if (mode == Mode.FULLY && idColumnVisible) {
			linesTable.removeColumn(idColumn);
			idColumnVisible = false;
		} else if (mode != Mode.FULLY && !idColumnVisible) {
			linesTable.addColumn(idColumn);
			linesTable.moveColumn(linesTable.getColumnCount() - 1, 0);
			idColumnVisible = true;
		}

		setSliderPanel.setVisible(mode == Mode.SET);
		idColumn.setHeaderValue(mode == Mode.SET ? "Set" : "ID");

		count = 0;
		hits = 0;
		accesses = 0;
		rateLabel.setText("");
		statusLabel.setText("WAITING");
		statusLabel.setBackground(Color.WHITE);
		statusLabel.setForeground(Color.BLACK);
		actionLabel.setText("");

		// Allow enough horizontal space:
		timeLabel.setPreferredSize(new Dimension(200,25));
		pack();
		timeLabel.setText("Time: 0ns");
	}
}
