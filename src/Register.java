import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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


public class Register extends JPanel {

	String name;
	Word value;
	
	
	public Register(String name) {
		this.name = name;
		value = new Word();
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setBorder(new EmptyBorder(2,2,2,2));
		this.add(new JLabel(name));
		//this.add(new JLabel(":"));
		this.add(value);
	
	}
	
	 
	 void set(int v) {
	    	value.set( v);
	    }
	 
	public void reset() {
		value.set( 0);
	}
	
	public int get() {
		return value.get();
	}
	    
	
	
	
	
}
