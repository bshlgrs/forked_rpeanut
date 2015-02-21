import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


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



public class DebugButton extends Component implements MouseListener {

	Memory memory;
	int col;

	public DebugButton(Memory mem, int col) {
		memory = mem;
		this.col = col;
		addMouseListener(this);
	}

	public void paint(java.awt.Graphics g) {
		if (memory.isDB(col)) {
			g.setColor(Color.blue);
			g.fillRect(2, 2, 6, 6);
		} else {
			g.setColor(Color.white);
			g.fillRect(0, 0, 10, 10);
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		memory.toggleDB(col);
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
}
