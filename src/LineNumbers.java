import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JComponent;


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


public class LineNumbers extends JComponent {

	
	Font font;
	Integer width;
	int pos;
	int ext;
	
	public LineNumbers(Font textfont) {
		font = textfont;
		width = 22;
	}
	
	
	public void setPlace(int pos, int ext) {
		this.pos = pos;
		this.ext = ext;
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		FontMetrics fm = this.getGraphics().getFontMetrics(font);
		g.setFont(font);
		int fh = fm.getHeight();
		int maxnum = (pos+ext)/fh + 1;
	    width = fm.getMaxAdvance();
	    if (maxnum > 9) width += fm.getMaxAdvance();
	    if (maxnum > 99) width += fm.getMaxAdvance();
	    if (maxnum > 999) width += fm.getMaxAdvance();
		
		for (int i=pos/fh; i <= (pos+ext)/fh; i++) {
		   g.drawString(String.format("%d", i), 0, ((fh * i) - pos) - fh/5);
		}
		
		
		
	}


	public int getWid() {
		
		return width;
	}

}
