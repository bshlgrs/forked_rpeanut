import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.PrintStream;

import javax.swing.JComponent;

/*
 rPeanut - is a simple simulator of the rPeANUt computer.
 Copyright (C) 2011-2014  Eric McCreath

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

public class Screen extends JComponent {
	static final Dimension screensize = new Dimension(Simulate.SCREENWIDTH,
			Simulate.SCREENHEIGHT);
	static final Dimension dscreensize = new Dimension(
			2 * Simulate.SCREENWIDTH, 2 * Simulate.SCREENHEIGHT);
	static final int wordsPerLine = Simulate.SCREENWIDTH / 32; // this is 6
	private BufferedImage buf;
	Memory memory;
	static int black = Color.black.getRGB();
	static int white = Color.white.getRGB();

	public Screen(Memory mem) {
		// this.setSize(screensize);
		this.setMinimumSize(screensize);
		this.setMaximumSize(dscreensize);
		this.setPreferredSize(dscreensize);
		buf = new BufferedImage(screensize.width, screensize.height,
				BufferedImage.TYPE_INT_ARGB);
		memory = mem;
	}

	public void refreshAll() {
		try {
			for (int currentline = 0; currentline < screensize.height; currentline++) {
				int startword = Simulate.SCREENSTART + currentline
						* wordsPerLine;
				for (int woff = 0; woff < wordsPerLine; woff++) {
					int word = memory.get(startword + woff);

					for (int bit = 0; bit < 32; bit++) {
						int c = (((word >> bit) & 1) == 1 ? white : black);
						buf.setRGB(bit + woff * 32, currentline, c);
					}
				}
			}
		} catch (MemFaultException e) {
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Dimension d = this.getSize();
		g.drawImage(buf, 0, 0, d.width, d.height, 0, 0, Simulate.SCREENWIDTH,
				Simulate.SCREENHEIGHT, null);
	}

	public void dump(PrintStream out) {
		for (int y = 0; y < screensize.height; y++) {
			out.print(y);
			for (int x = 0; x < wordsPerLine; x++) {
				int value = 0;
				try {
					value = memory.get(Simulate.SCREENSTART + y * wordsPerLine
							+ x);
				} catch (MemFaultException e) {
				}
				out.print(":");
				out.print(String.format("%08x", value));
			}
			out.println();
		}
	}
}
