/*
 *******************************************************************
 *        Bots 'n' Scouts - Multi-Player networked Java game       *
 *                                                                 *
 * Copyright (C) 2005 scouties.                                *
 * Contact botsnscouts-devel@sf.net                                *
 *******************************************************************

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, in version 2 of the License.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program, in a file called COPYING in the top
 directory of the Bots 'n' Scouts distribution; if not, write to 
 the Free Software Foundation, Inc., 59 Temple Place, Suite 330, 
 Boston, MA  02111-1307  USA
 
 *******************************************************************/

package de.botsnscouts.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import de.botsnscouts.util.ImageMan;

@SuppressWarnings("serial")
public class OptionPane extends JPanel {

    static Paint backgroundPaint = null;

    public static Paint getBackgroundPaint(Component c) {
        if (backgroundPaint == null) {
            ImageIcon icon = ImageMan.getIcon("garage2.jpg");
            BufferedImage bgimg = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                            BufferedImage.TYPE_INT_ARGB);
            Graphics g = bgimg.getGraphics();
            icon.paintIcon(c, g, 0, 0);
            g.dispose();
            Rectangle2D anchor = new Rectangle2D.Float(0f, 0f, icon.getIconWidth(), icon.getIconHeight());
            backgroundPaint = new TexturePaint(bgimg, anchor);
        }
        return backgroundPaint;
    }

    public static Border etchedBorder = BorderFactory.createEtchedBorder(new Color(0, 255, 0, 128), new Color(0, 128,
                    0, 128));

    public static Border niceBorder = BorderFactory.createCompoundBorder(etchedBorder,
                    BorderFactory.createEmptyBorder(20, 20, 20, 20));

    public static JButton getButton(String title) {
        return new TJButton(title, 12);
    }

    public static JButton getTransparentButton(String title) {
        return getTransparentButton(title, 24);
    }

    public static JButton getTransparentButton(String title, int fontSize) {
        return new TJButton(title, fontSize);
    }

    static JCheckBox getCheckBox(String title) {
        JCheckBox button = new JCheckBox(title);
        button.setBackground(new Color(255, 255, 0, 128));
        button.setRolloverEnabled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        // button.setBorderPaintedFlat(true); JDK1.3 only!
        button.setOpaque(false);
        return button;
    }

    static JToggleButton getToggleButton(String title) {
        JToggleButton button = new JToggleButton(title);
        button.setBackground(Color.black);
        button.setRolloverEnabled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        return button;
    }

    static JSlider getSlider() {
        JSlider button = new JSlider(SwingConstants.HORIZONTAL, 30, 150, 100);
        button.setMajorTickSpacing(50);
        button.setMinorTickSpacing(10);
        button.setSnapToTicks(true);
        button.setPaintLabels(true);
        button.setLabelTable(button.createStandardLabels(50, 50));
        button.setPaintTicks(true);
        // button.setBackground( new Color(155,155,155,200) );
        button.setOpaque(false);
        button.setBorder(BorderFactory.createTitledBorder("Zoom"));
        return button;
    }

    /*
     * public OptionPane(JComponent owner) {
     * super(new GridLayout(6,1));
     * enableEvents( AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
     * setSize(300, 400);
     * java.awt.Dimension d = owner.getSize();
     * setLocation( (d.width - getWidth()) / 2, (d.height-getHeight())/2 );
     * setBackground( new java.awt.Color(60, 80, 60, 200) );
     * setBorder( BorderFactory.createEtchedBorder( new Color(0, 255,0, 128), new Color(0,128,0,128) ) );
     * setOpaque( false );
     * add( getButton("Grafik") );
     * JButton dis = getButton("disable");
     * dis.addActionListener(new ActionListener() {
     * public void actionPerformed(ActionEvent e) {
     * JButton src = (JButton)(e.getSource());
     * src.setEnabled(false);
     * }
     * });
     * add(dis);
     * add( getToggleButton("Sound") );
     * add( getCheckBox("Sound") );
     * add( new TJButton("Erweitert", 12) );
     * add( getSlider("enno") );
     * 
     * // Box b = new Box( BoxLayout.Y_AXIS );
     * // b.add( new JButton( "foo" ) );
     * // b.add( new JButton( "foo" ) );
     * // b.add( new JButton( "foo" ) );
     * // b.add( new JButton( "foo" ) );
     * // JPanel p = new JPanel( new BorderLayout() );
     * // p.add( b, BorderLayout.CENTER );
     * // p.setBorder( BorderFactory.createEmptyBorder(50,50,50,50) );
     * // add( p, BorderLayout.CENTER );
     * }
     * public void paint(Graphics g) {
     * g.setColor(getBackground());
     * g.fillRect(0, 0, getWidth(), getHeight());
     * super.paint(g);
     * }
     * 
     * public void paintComponentx(Graphics _g) {
     * Graphics2D g = (Graphics2D) _g;
     * Color green = new Color(0, 255, 0, 128);
     * Stroke stroke = new BasicStroke(3);
     * g.setStroke(stroke);
     * g.setColor( green );
     * }
     * 
     * 
     * protected void processMouseEvent(MouseEvent e) {
     * // todo: Override this java.awt.Component method
     * // super.processMouseEvent( e);
     * }
     * 
     * protected void processMouseMotionEvent(MouseEvent e) {
     * 
     * // todo: Override this java.awt.Component method
     * // super.processMouseMotionEvent( e);
     * }
     */
}
