package de.botsnscouts.gui;

import de.botsnscouts.util.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.*;

import de.botsnscouts.util.*;

    /**
     * Scoutvertiefung, die im oben rechts in der Infoleiste dargestellt ist
     */
public class ScoutVertiefung extends JToggleButton {

    int xsize=60, ysize=60;
    Image imageActive[];
    Image imageDream[];

    public ScoutVertiefung(ActionListener al) {
	imageActive=ImageMan.getImages(ImageMan.SCHLAFPLATZ);
	imageDream=ImageMan.getImages(ImageMan.SCHLAFSCOUT);
	//	scoutImages=ImageMan.getImages(ImageMan.SCOUT);
	setContentAreaFilled(false);
	setBorder(null);
	setToolTipText(Message.say("ScoutVertiefung","mtooltip"));
	setIcon(new ImageIcon(imageActive[0]));
	//	setPressedIcon(new ImageIcon(scoutImages[2]));
	setSelectedIcon(new ImageIcon(imageDream[0]));
	addActionListener(al);
	
    }

    public Dimension getMinimumSize() {
	return new Dimension(xsize,ysize);
    }
    
    public Dimension getPreferredSize() {
	return new Dimension(xsize,ysize);
    }


    public static void main (String args[]) {
	Message.setLanguage("deutsch");
        JFrame f = new JFrame();
	f.setSize(100,100);
	f.getContentPane().add(new ScoutVertiefung(null));
	f.setVisible(true);
    }


    protected boolean selected() {
	return isSelected();
    }
}
