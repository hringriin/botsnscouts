package de.botsnscouts.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import de.botsnscouts.util.ImageMan;
import de.botsnscouts.util.CursorMan;
import de.botsnscouts.util.*;

import javax.swing.border.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class RobotInfo extends JComponent  implements RobotStatus, ActionListener {
    static org.apache.log4j.Category CAT = org.apache.log4j.Category.getInstance( RobotInfo.class );
    static final Image[] roboImages = ImageMan.getImages( ImageMan.ROBOS );
    static final Image[] cursors = CursorMan.getImages( CursorMan.STATUSROBOTS );
    static Image[] stuff = null;

    static final int MINI_IMAGE_COUNT = 4;
    DamageBar damageBar1 = new DamageBar();
    FlagBar flagBar2 = new FlagBar();
    StatusRobot statusRobot1 = new StatusRobot();
    JButton diskButton1 = new JButton();
    int viz;
    Roboter robot;


    static Image getImage(int nr) {
        if( stuff == null ) {
                stuff = new Image[MINI_IMAGE_COUNT];
                ImageMan.loadImages();
                Image image = ImageMan.getImage( ImageMan.STATUS_STUFF );
                CropperField2 cropper = new CropperField2(MINI_IMAGE_COUNT, 1, 16, image);
                CAT.debug( "start cropping" );
                cropper.multiCrop( image, MINI_IMAGE_COUNT, MINI_IMAGE_COUNT, stuff, 0);
                CAT.debug( "waiting for images" );
                ImageMan.finishLoading();
                CAT.debug( "waiting over" );
        }
        return stuff[nr];
    }

    static Image getFlag() { return getImage(0); }
    static Image getGrayFlag() { return getImage(1); }
    static Image getDisk() { return getImage(2); }
    static Image getOffSwitch() { return getImage(3); }

    RobotInfo(Roboter r, int flagCount ) {
        this( r, flagCount, r.getBotVis() );
    }

    RobotInfo(Roboter r, int flagCount, int viz) {
        this.robot = r;
        this.viz = viz;
        Icon big = new ImageIcon(roboImages[viz*4]);
        Icon small = new ImageIcon(cursors[viz]);
        damageBar1 = new DamageBar();
        flagBar2 = new FlagBar(flagCount);
        statusRobot1 = new StatusRobot(big, small, r);
        diskButton1 = new JButton();
        diskButton1.setIcon( new ImageIcon(getDisk()) );
        flagBar2.addActionListener(this);
        statusRobot1.addActionListener(this);
        diskButton1.addActionListener(this);
        try {
            jbInit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public RobotInfo() {
        try {
            jbInit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

   private void jbInit() throws Exception {
        this.setBackground( Color.black );
        this.setSize( getPreferredSize() );
        this.setLayout(null);
        this.setOpaque( true );
        damageBar1.setBounds(new Rectangle(9, 7, 5, 54));
        flagBar2.setBounds(new Rectangle(15, 7, 70, 16));
        statusRobot1.setBounds(new Rectangle(18, 20, 68, 44));
        diskButton1.setIcon( new ImageIcon( RobotInfo.getDisk() ) );
        diskButton1.setBounds(new Rectangle(0, 23, 16, 16));
        diskButton1.setBorder(null);
        diskButton1.setBorderPainted(false);
        this.add(damageBar1, null);
        this.add(statusRobot1, null);
        statusRobot1.setLayout( null );
        statusRobot1.add(diskButton1, null);
        this.add(flagBar2, null);
    }


    public void paint(Graphics _g) {
        Graphics2D g = (Graphics2D) _g;
        //g.translate(8,7);
        g.setColor( Color.black.brighter().brighter() );




//        if( robot.istAktiviert() )
//            g.setPaint( new GradientPaint( 0, 0, Color.lightGray, 72, 0, Color.darkGray ) );
//        else
//            g.setPaint( new GradientPaint( -72, -72, Color.gray, 72, -72, Color.darkGray ) );


        g.fillRect( 0,0, getWidth(), getHeight() );
        g.setColor( new Color( 0, 80, 0 ) );
        for(int x=5; x< getWidth(); x+=5 )
            g.drawLine(x, 0, x, getHeight() );
        for(int y=5; y< getHeight(); y+=5 )
            g.drawLine(0, y, getWidth(), y );


        super.paint( g );

        g.setFont( font );
        g.setColor( new Color(200, 200, 200, 128 ));
        g.drawString( robot.getName(), 15, 31 );
        g.setColor( textColor );
        g.drawString( robot.getName(), 14, 30 );

        if( robot != null && !robot.istAktiviert()  ) {
            g.setPaint( new Color(100, 100, 100, 128 ) );
            g.fillRect( 0,0,getWidth(), getHeight() );
        }

        //g.translate(-8,-7);
        frame.paintIcon(this, g, -2, -2);


    }
    static Font font = new Font("Serif", Font.ITALIC + Font.BOLD, 12 );
    static Color textColor = new Color( 0, 255, 0);
    static ImageIcon frame = new ImageIcon( ImageMan.getImage(ImageMan.STATUS_FRAME) );
    private transient Vector robotInfoListeners;

    public Dimension getMinimumSize() {
        return new Dimension(90,70);
    }

    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public Dimension getMaximumSize() {
        return getMinimumSize();
    }

    public Roboter getRobot() {
        return robot;
    }
    public void updateRobot(Roboter r) {
        robot = r;
        repaint();
    }
    public void setWinnerNumber(int ranking) {

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("damage");
        frame.setSize(500, 100);
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Box b = Box.createHorizontalBox();
        for(int i=0; i<6; i++) {
            Roboter robot = Roboter.getNewInstance("TestRob " + i);
            robot.setAktiviert( i % 2 == 0 );
            final RobotInfo db = new RobotInfo( robot, 7, i);
            db.setBorder( BorderFactory.createLineBorder(Color.black) );
            db.setSize( db.getPreferredSize() );
            JPanel p = new JPanel();
            p.add( db );
            b.add( p );
            db.addMouseListener( new MouseAdapter() {
                public void mouseClicked( MouseEvent me ) {
                    db.repaint();
                }
            });
        }
        frame.getContentPane().add( b, BorderLayout.CENTER );
        frame.setVisible( true );
    }
    public synchronized void removeRobotInfoListener(RobotInfoListener l) {
        if (robotInfoListeners != null && robotInfoListeners.contains(l)) {
            Vector v = (Vector) robotInfoListeners.clone();
            v.removeElement(l);
            robotInfoListeners = v;
        }
    }
    public synchronized void addRobotInfoListener(RobotInfoListener l) {
        Vector v = robotInfoListeners == null ? new Vector(2) : (Vector) robotInfoListeners.clone();
        if (!v.contains(l)) {
            v.addElement(l);
            robotInfoListeners = v;
        }
    }
    protected void fireFlagClicked(RobotInfoEvent e) {
        if (robotInfoListeners != null) {
            Vector listeners = robotInfoListeners;
            int count = listeners.size();
            for (int i = 0; i < count; i++) {
                ((RobotInfoListener) listeners.elementAt(i)).flagClicked(e);
            }
        }
    }
    protected void fireRobotClicked(RobotInfoEvent e) {
        if (robotInfoListeners != null) {
            Vector listeners = robotInfoListeners;
            int count = listeners.size();
            for (int i = 0; i < count; i++) {
                ((RobotInfoListener) listeners.elementAt(i)).robotClicked(e);
            }
        }
    }
    protected void fireDiskClicked(RobotInfoEvent e) {
        if (robotInfoListeners != null) {
            Vector listeners = robotInfoListeners;
            int count = listeners.size();
            for (int i = 0; i < count; i++) {
                ((RobotInfoListener) listeners.elementAt(i)).diskClicked(e);
            }
        }
    }
    public void actionPerformed(ActionEvent parm1) {
        /**@todo: Implement this java.awt.event.MouseListener method*/
        System.out.println("Method mouseClicked(), now dispatching");
        Object src = parm1.getSource();
        if( src == diskButton1 )
            fireDiskClicked( new RobotInfoEvent( this ) );
        else if( src == flagBar2 )
            fireFlagClicked( new RobotInfoEvent( this ) );
        else if( src == statusRobot1 )
            fireRobotClicked( new RobotInfoEvent( this ) );
        else {
            CAT.error("unkown source");
        }
    }
}