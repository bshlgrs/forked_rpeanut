import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.*;
import java.util.ArrayList;


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


public class EditCode extends JPanel implements AdjustmentListener {
	JScrollPane scroll;
	JTextArea text;
	LineNumbers ln;
	ArrayList<UndoItem> undolist;
	boolean changedFile;

	private class UndoItem {
		String text;
		int caretPos;

		private UndoItem(String text, int caretPos) {
			this.text = text;
			this.caretPos = caretPos;
		}
	}

	static final int MAXUNDO = 200;

	public EditCode(int fontsize) {
		undolist = new ArrayList<UndoItem>();
		text = new JTextArea();
		snap();
		Font textfont = new Font(Font.MONOSPACED, Font.PLAIN, fontsize);
		text.setFont(textfont);
		ln = new LineNumbers(textfont);
		changedFile = false;

		text.getDocument().addDocumentListener(new DocumentListener() {
			@SuppressWarnings("unused")
			public String getText() {
				int caretPosition = text.getDocument().getLength();
				Element root = text.getDocument().getDefaultRootElement();
				String text = "1" + System.getProperty("line.separator");
				for(int i = 2; i < root.getElementIndex( caretPosition ) + 2; i++){
					text += i + System.getProperty("line.separator");
				}
				System.out.println("get text: " + text);
				return text;
			}
			@Override
			public void changedUpdate(DocumentEvent de) {
				//snap();
				// ln.setText(getText());

				//System.out.println("A");
			}

			@Override
			public void insertUpdate(DocumentEvent de) {
				snap(de);
				changedFile = true;
				
				//ln.setText(getText());
				//System.out.println("B" + scroll.getVisibleRect());
			}

			@Override
			public void removeUpdate(DocumentEvent de) {
				//snap(de);
				//  ln.setText(getText());
				// System.out.println("C");
			}
		});

		scroll = new JScrollPane(text);

		//scroll.getViewport().add(text);
		// scroll.setBorder(new EmptyBorder(0,0,0,0));
		// scroll.setRowHeaderView(ln);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		scroll.getVerticalScrollBar().addAdjustmentListener(this);
		this.add(ln,0);
		this.add(scroll,1);
		//this.setMinimumSize(new Dimension(100,100));
		//scroll.setPreferredSize(new Dimension(100,110));
	}

	@Override
	public void doLayout() {
		super.doLayout();
		//Point p = this.getLocation();
		Dimension d = this.getSize();
		int wid = ln.getWid();
		scroll.setSize(new Dimension(d.width-wid,d.height));
		scroll.setLocation(wid,0);
		ln.setSize(new Dimension(wid,d.height));
		ln.setLocation(0,0);
	}

	public void save(File file) {
		PrintWriter pr;
		try {
			pr = new PrintWriter(file);
			pr.append(text.getText());
			pr.close();
			changedFile = false;
		} catch (FileNotFoundException e) {
			System.out.println("Problem Finding File : " + file);
		}
	}

	public void load(File file) throws FileNotFoundException, IOException {
		snap();
		text.setText(loadfile(file));
		changedFile = false;
		
	}

	public static String loadfile(File file) throws FileNotFoundException, IOException  {
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		return (sb.toString());
	}


	public void undo() {
		if (undolist.size() > 1)  {
			//System.out.println("undo : " + undolist);
			final UndoItem undoItem = undolist.get(undolist.size() - 2);
			text.setText(undoItem.text);
			try {
				text.setCaretPosition(undoItem.caretPos);
			} catch (Exception e) {
				e.printStackTrace();
			}
			undolist.remove(undolist.size()-1);
			undolist.remove(undolist.size()-1);

		}
	}

	public void redo() {
		// still todo
	}

	public void snap() {
		snap(null);

	}

	private void snap(DocumentEvent de) {
		int pos = text.getCaretPosition();
		if (de != null) {
			pos += de.getLength();
		}
		undolist.add(new UndoItem(text.getText(), pos));
		if (undolist.size() > MAXUNDO) undolist.remove(0);
		//System.out.println("snap : " + undolist);
	}

	public String text() {
		return text.getText();
	}

	public void newtext() {
		text.setText("");
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent arg0) {
		JScrollBar sb = scroll.getVerticalScrollBar();
		ln.setPlace(sb.getModel().getValue(), sb.getModel().getExtent());
		this.revalidate();
	}
}
