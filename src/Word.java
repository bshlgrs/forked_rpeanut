import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JTextField;


/*
rPeanut - is a simple simulator of the rPeANUt computer.
Copyright (C) 2011  Eric McCreath

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

public class Word extends JTextField implements KeyListener {
	protected int value;
	static Font wordfont = new Font(Font.MONOSPACED,Font.PLAIN,14);
	static String str32 = "0x00000000";

	public Word() {
		this.addKeyListener(this);
		value = 0;
		this.setText(toString());
		this.setFont(wordfont);
		int width = this.getFontMetrics(wordfont).stringWidth(str32) + 5;
		int height = this.getFontMetrics(wordfont).getHeight() + 3;
		this.setPreferredSize(new Dimension(width,height));
		this.setMinimumSize(new Dimension(width,height));
		this.setMaximumSize(new Dimension(width,height));
	}

	public Word(int v1) {
		this.addKeyListener(this);
		value =  v1;
		this.setText(toString());
		this.setFont(wordfont);
	}


	public Word(int v1, int v2) {
		this.addKeyListener(this);
		value =  ((v1 & 0xffff) << 16) | (v2 & 0xffff);
		this.setText(toString());
		this.setFont(wordfont);
	}

	public Word(int code, int r1, int r2, int rd, int add) {
		value =  (((code & 0xF) << 28)  |
				((r1 & 0xF) << 24)  |
				((r2 & 0xF) << 20)  |
				((rd & 0xF) << 16)  |
				(add & 0xFFFF)) ;
		this.setFont(wordfont);
	}

	public String toString() {
		return String.format("0x%04x%04x", 0xffff & (value >> 16), 0xffff & value);
	}

	void set(int v) {
		value = v;
		this.setText(toString());
	}

	public  int get() {
		return value;
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		D.p("key released : " + this.getText());
		String text = this.getText();
		if (text.length() > 10) {
			text = text.substring(0, 10);
			this.setText(text);
		}
		try {
			Attribute att = Attribute.parse(text,new Lineinfo(text, 1, "<stdin>"));
			if (att != null && att.type == AttType.VALUE && att.val != null) {
				value = att.val;
				//System.out.println("value : " + value);
			}
		} catch (ParseException e) {
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		D.p("key typed : " + this.getText());
	}
}
