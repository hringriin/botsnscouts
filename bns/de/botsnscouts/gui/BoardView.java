/*
 *******************************************************************
 *        Bots 'n' Scouts - Multi-Player networked Java game       *
 *                                                                 *
 * Copyright (C) 2001 scouties.                                    *
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

package de.botsnscouts.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.JViewport;

import org.apache.log4j.Category;

import com.keypoint.PngEncoder;

import de.botsnscouts.board.Board;
import de.botsnscouts.board.FlagException;
import de.botsnscouts.board.Floor;
import de.botsnscouts.board.LaserDef;
import de.botsnscouts.board.SimBoard;
import de.botsnscouts.board.Wall;
import de.botsnscouts.comm.OtherConstants;
import de.botsnscouts.util.Bot;
import de.botsnscouts.util.BotVis;
import de.botsnscouts.util.Directions;
import de.botsnscouts.util.FormatException;
import de.botsnscouts.util.ImageMan;
import de.botsnscouts.util.Location;
import de.botsnscouts.util.SoundMan;

/**
 * Board-Ausgabe-Canvas ist das Objekt, das der Ausgabe und dem menschlichen Spieler das Board grafisch darstellt und verwaltet
 * 
 * @version $Id$
 * 
 *  changes: enno v1.23
 * 1. beim painten wird nur noch der teil ins sichtbare kopiert, der auch
 *   in gepaintet werden muss (hat aber nicht viel gebracht)
 * 2. Felder, die nicht wirklich neu gezeichnet werden m\uFFFDssen, werden nicht
 *    mehr betrachtet (bringt was!)
 * 3. Code insgesamt lesbarer strukturiert (noch nicht ganz beendet)
 * 4. makePixelArray in eigene Methode ausgelagert
 * 5. lokale Variablen eingef\uFFFDhrt ...
 *
 * 6. Verwaltung und Speicherung der Bilder ausgelagert in
 *    Klasse ImageMan (kann so dann auch vom KachelEditor verwendet werden)
 *    bilder werden dann nur einmal pro JVM geladen, und zwar bei Programm-
 *    start (StartSpieler) im Hintergrund. Der erste Spielfeldaufbau ist
 *    damit viel schneller, ebenso die Board-Vorschau
 */

public class BoardView extends JComponent{
    static Category CAT = Category.getInstance(BoardView.class);

    private final Object rescaleLock = new Object(); 
    
    /**
     *  Switch for the method that animations use to clear the area where they animate the bots:
     * if true, we will not only have an offScreenImage for doublebuffering but _also_ another 
     * BufferedImage ("staticBackground") of the board that will contain the background without 
     * any dynamic components (bots, scout,highlight) drawn on it.
     * Without increasing the JVM heapsize on startup, using bigger boards/more autobots while 
     *  useStaticBg=true will create an OutOfMemoryError.
     * 
     * 
     * If false, this second BufferedImage will be null and another way to paint the background will be used. 
     */
    private final boolean useStaticBg = false;
    
    
    // inner classes
    public static interface ClickListener {
        void feldClicked(int x, int y, int modifiers);
    }

    /** Constant for direction/facing north*/
    protected static final int NORTH = Directions.NORTH;
    /** Constant for direction/facing east*/
    protected static final int EAST = Directions.EAST;
    /** Constant for direction/facing south*/
    protected static final int SOUTH = Directions.SOUTH;
    /** Constant for direction/facing west*/
    protected static final int WEST = Directions.WEST;


    /** size (length and width) of one little field in pixels*/
    protected static final int FELDSIZE = 64;
    
    /**Number of single steps a laser animation is drawn.*/
    private static final int FULL_LENGTH_INT = 30;

    /**Number of single steps a laser animation is drawn.*/
    private static final double FULL_LENGTH_DOUBLE = 30.0;


    /** contains colors of the boardlasers, strength 1 to 3*/
    static final Color[] laserColor = {Color.red.brighter(), //strength 1
                                       Color.orange, //strength 2
                                       Color.yellow};//strength 3

    /** The color used for the background of active lasers. */
    private final static Color sndLaserColor = new Color(255, 255, 155);

    /** To lookup the color of a robot; contains name->color mapping.*/
    private java.util.Hashtable nameToColorHash;
    private boolean gotColors;

    /** some board elements..*/
    private Image[] cbeltCrop,ebeltCrop,diverseCrop,robosCrop,scoutCrop;

    /** maps Location(x,y) to the Image that should be painted as floor*/
   private HashMap floorElementHash = new HashMap();
   
   /** Main contain names(!) of robots that should not be painted by request of the user
    *   (to support new "hide robot(s)" menu option) 
    */
   private HashSet theseBotsShouldNotBePainted = new HashSet();

    private int widthInPixel;
    private int heightInPixel;

    /** Stores data of the robots.*/
    //private Bot[] robos;
    
    /** This robot is used for calculations,
     *  like making a suggestion for the next move.
     */
    private Bot previewRob;

    /** last position of our famous scout ;-) */
    private Location lastScoutPos = new Location();
    // Let's define some colors, so that everybody uses the same..
    public static final Color YELLOW = BotVis.YELLOW;
    public static final Color RED = BotVis.RED;
    public static final Color BLUE = BotVis.BLUE;
    public static final Color ROSA = BotVis.ROSA;
    public static final Color ORANGE = BotVis.ORANGE;
    public static final Color GRAY = BotVis.GRAY;
    public static final Color VIOLET = BotVis.VIOLET;
    public static final Color GREEN = BotVis.GREEN;

    /** Colors of the robots. */
    public static final Color[] ROBOCOLOR = {GREEN, YELLOW, RED, BLUE, ROSA, ORANGE, GRAY, VIOLET};

    /** gameboard object;
     *  stores the information about the board we are playing on;
     *  (where are the pits, where are lasers, and so on..)
     */
    SimBoard sf;

    /** scale factor for zooming*/
   private double dScale = 1.0;
    

    private int scaledFeldSize; // FELDSIZE * scale

    /** position to highlight*/
    Location highlightPos = new Location(0, 0);

    /** This is where we keep our internal robots for animations */
    private HashMap internalBotHash = new java.util.HashMap();
    
    private static final Location PIT = new Location(0, 0);
    
    ClickListener myClickListener;


   
    
    private AnimationConfig currentAnimationConfig;
    
    
    private static final AlphaComposite AC_SRC = AlphaComposite.getInstance(AlphaComposite.SRC);
    private static final AlphaComposite AC_SRC_OVER = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
    private static final AlphaComposite AC_SRC_OVER_05 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    private static final AlphaComposite AC_SRC_OVER_07 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);

    public BoardView(SimBoard sf_neu) {
        init(sf_neu, ROBOCOLOR);
        
    }

    public BoardView(SimBoard sf_neu, Color[] robColors) {
        init(sf_neu, robColors);
        mouseInit();
    }

    /** Get a simple board view loaded from one tile file.
     *  This is not the way to do this in general since boards may
     *  consist of serveral tiles and each tile may be rotated.
     * @param boardFile The board file to load.
     */
    public BoardView(File boardFile) throws IOException, FormatException, FlagException {
        this(new SimBoard(12, 12, Board.readMagicString(boardFile)));
    }


    public double getScale() {
        return dScale;
    }

    /*must not be called before images are loaded*/
    public void setScale(double scale) {
        // adapt this Component to the scaling factor
        //dScale = scale;
      //  Bot preCopy = previewRob;
      //  previewRob = null;
        synchronized (rescaleLock) {
	        deleteScout();
	        scaledFeldSize = (int) (scale * FELDSIZE);
	        dScale = scaledFeldSize/((double)FELDSIZE);
	        
	        widthInPixel = (int) (sf.getSizeX() * scaledFeldSize);
	        heightInPixel = (int) (sf.getSizeY() * scaledFeldSize);
	       
	        CAT.debug("dim : " + widthInPixel + " " + heightInPixel);
	        setSize(widthInPixel, heightInPixel);
	
	        // the preComputed-BoardImage is no longer valid      
       
            offScreenImage = createBoardImage();
            if (useStaticBg) {
                staticBackground = createBoardImage(); // TODO possible to use java-scaling instead? 
            }
        }
      
       
      //  previewRob = preCopy;
       // paintScout(this.getGraphics());
    }

    private void init(SimBoard sf_neu, Color[] robColors) {

        gotColors = false;
        sf = sf_neu;

        //setDoubleBuffered(true);
        ImageMan.finishLoading();
        
        
        ebeltCrop = ImageMan.getImages(ImageMan.EBELTS);
        cbeltCrop = ImageMan.getImages(ImageMan.CBELTS);
        diverseCrop = ImageMan.getImages(ImageMan.DIVERSE);
        robosCrop = ImageMan.getImages(ImageMan.ROBOS);
        scoutCrop = ImageMan.getImages(ImageMan.SCOUT);
        
        initFloorHashMap();
        setScale(dScale); // does setSize(); must not be called before images are loaded
        
        
      
    }


    //void setScrollPane(JScrollPane j) {
    //    myScrollPane = j;
    //}

    Point calcKachelPos(int mx, int my) {
        int sfh = sf.getSizeY();
        int sfw = sf.getSizeX();

        Point p = new Point();
        p.x = 1 + (int) (mx / scaledFeldSize);
        p.y = sfh - (int) (my / scaledFeldSize);

        // assure that 1 <= p.x <= sfw
        // and 1 <= p.y <= sfy

        p.x = Math.min(Math.max(1, p.x), sfw);
        p.y = Math.min(Math.max(1, p.y), sfh);
        return p;
    }


    public void addClickListener(ClickListener listener) {
        myClickListener = listener;
    }


    void mouseInit() {
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                Point feld = calcKachelPos(me.getX(), me.getY());
                if (myClickListener != null) {
                    myClickListener.feldClicked(feld.x, feld.y, me.getModifiers());
                }

                /*
                int mods = me.getModifiers();
                 if( (mods & MouseEvent.BUTTON3_MASK) == 0 )
                     return;

                Dimension sz = myScrollPane.getViewport().getExtentSize();
                int w2 = sz.width/2;
                int h2 = sz.height/2;

                //make sure we dont want to scoll 'out' to
                // the left and top
                int x1 = Math.max( me.getX() - w2 , 0);
                int y1 = Math.max( me.getY() - h2 , 0);

                // ... and right and bottom
                x1 = Math.min( x1, x - sz.width );
                y1 = Math.min( y1, y - sz.height );

                myScrollPane.getViewport().setViewPosition(new Point(x1, y1));
                */

            }
        });
    }

    public Dimension getMinimumSize() {
        return new Dimension(widthInPixel, heightInPixel);
    }

    public Dimension getPreferredSize() {
        return new Dimension(widthInPixel, heightInPixel);
    }


    /** Create "name->color" - Hashtable*/
    private void setRobColors(Bot[] robs) {
        gotColors = true;
        nameToColorHash = new java.util.Hashtable();
        for (int i = 0; i < robs.length; i++)
            if (robs[i] == null)
                break;
            else
                nameToColorHash.put(robs[i].getName(), ROBOCOLOR[robs[i].getBotVis()]);
    }


    /** Lookup the Bot's color (by name)
     @param name The Bot's name
     @return The Bot's color. If the name is unknown, Color.white will be returned,
     */
    private Color getRobColor(String name) {
        Color foo = null;
        foo = (Color) nameToColorHash.get(name);
        if (foo == null) {
            CAT.error("getRobColor: Color for " + name + "'s Laser not found");
            return Color.white;
        } else
            return foo;
    }


/** Show/hide a robot.
 * 
 * @param bot the robot to be shown or hidden
 * @param isVisible true: bot will be painted; false: bot will not be painted
 */
    protected  void setRobotVisbility(Bot bot, boolean isVisible) {
        if (isVisible) {
            theseBotsShouldNotBePainted.remove(bot.getName());
        }
        else {           
            theseBotsShouldNotBePainted.add(bot.getName());
        }
        repaint();
    }
    
    /** Call this to ensure that no robot is hidden. */
    protected void setAllRobotsVisible() {
        theseBotsShouldNotBePainted.clear();
        repaint();
    }
    
    /** Call this to ensure that all robots are hidden. */
    protected void setAllRobotsInvisible(){
        Enumeration botNames = nameToColorHash.keys();
        while (botNames.hasMoreElements()){
            theseBotsShouldNotBePainted.add(botNames.nextElement());    
        }        
        repaint();
    }
    
    

    private void replaceInternalRobots( Bot[] robos_neu){
        int count = robos_neu!=null?robos_neu.length:0;
        synchronized (internalBotHash) {
            for (int i = 0; i < count; i++) { // initalizing my internal hash
                internalBotHash.put(robos_neu[i].getName(), robos_neu[i]);
            }
    	}
    }
    
    protected void ersetzeRobos(Bot[] robos_neu) {
        waitForPhaseDisplay();
  
        if (!gotColors) { // this is the first time I get the robots
            setRobColors(robos_neu);
            replaceInternalRobots(robos_neu);
            repaint();
            return;
        }
        // we dont want to overwrite the robots positions/facings, because they
        // have been updated in animateRobMove()/animateRobTurn() before;
        // animateRobMove()/animateRobTurn() gets informed earlier, so overwriting the positions/facings
        // would reset a robot back to a position/facing he has already left
        else {
            if (AnimationConfig.areMovementAnimationsEnabled()) {
                int count = robos_neu!=null?robos_neu.length:0;
                for (int i = 0; i < count; i++) { 
                    Bot serverBot = robos_neu[i];
                    String botName = serverBot.getName();
                    Bot ourBot = (Bot) internalBotHash.get(botName);                    
                    Location ourPos = ourBot.getPos();
                    if (!(serverBot.getPos().equals(PIT) || serverBot.getDamage() >= 10 || ourPos.equals(PIT))) {
                        // ^^^^^^^^^^^^^
                        // otherwise we would not show
                        // the destroyed robot ever again
                        // as we would ignore him if he
                        // is placed on the board again
                        if (CAT.isDebugEnabled()) {
                            CAT.debug("ignoring server values of robot " + serverBot.getName()
                                    + " as my calculated values will be more accurate");
                        }
                        // use the internal kept values of our robot if it is not destroyed
                        serverBot.setPos(ourPos);
                        serverBot.setFacing(ourBot.getFacing());                    
//                      TODO robot virtuality might need the same treatment as position and facing?                  
                    } 
                    else {
                        if (CAT.isDebugEnabled())
                            CAT.debug("using server values for position/facing of robot " + serverBot.getName());
                       
                    }
                    
                    synchronized (internalBotHash) {
                        internalBotHash.put(botName, serverBot);
                    }
                    repaint();
               } // end for                                            
            } // end if (animations enabled)
            else { // no animation, simply replace all robots
                replaceInternalRobots(robos_neu);
                repaint();
                waitSomeTime(currentAnimationConfig.getDelayBetweenActions(), this);
            }
        }

        

    }
    
    
    
    /** This method was added so that we can get the initial facings of the robots.
     * ersetzeRobos(Bot[]) doesn't work if animations are enabled (if animations are enabled
     * we have to use an internal version of the bots and ignore the server values for position and
     * facing because we have the bots moved before we get the notification that something has changed
     * (and might get other notifications in between that still contain the old values=>bots would be animated and
     * then placed back to their position before the animation).
     * 
     * DON'T USE THIS METHOD TO TURN ROBOTS, use animateRobTurn etc. instead
     * 
     * @param updated the facing of these Bots will be updated 
     */
    protected void updateFacings (Bot [] updated){
        if (internalBotHash.isEmpty() ){ // just in case, probably unnecessary
            CAT.warn("updateFacings called but we haven't got the robots yet!");
            replaceInternalRobots(updated);
            repaint();
            return;
        }
       
        int count = updated!=null?updated.length:0;
        for (int i=0;i<count;i++){
            Bot tmp = updated[i];
            Bot internal = (Bot) internalBotHash.get(tmp.getName());
            internal.setFacing(tmp.getFacing());
        }
        repaint();
    }
    
    private void paintBotsOnPositionButNotMe(Location position, Bot me, Graphics2D g2d, int xoffset, int yoffset){       
     
       int acht = (int)(8*dScale);
       Composite old = g2d.getComposite();
       synchronized ( internalBotHash) {
           Iterator it = internalBotHash.values().iterator();
           int roboCounter=0;
           while (it.hasNext()) {
               Bot bot = (Bot) it.next();
               if (!bot.getName().equals(me.getName()) && bot.getPos().equals(position) ){
                   if (bot.isVirtual())
                       g2d.setComposite(AC_SRC_OVER_05);
                   else
                       g2d.setComposite(AC_SRC_OVER);
                   int botVis = bot.getBotVis();
                   g2d.setColor(ROBOCOLOR[botVis]);
                   g2d.drawString(bot.getName(),xoffset,yoffset + acht + roboCounter * acht);
                   g2d.drawImage(BotVis.getBotImageByBotVis(botVis, bot.getFacing()), xoffset, yoffset,scaledFeldSize, scaledFeldSize,this );                
                   
               }
               roboCounter++;
           }
       }                  
        g2d.setComposite(old);
    }
    
    private void moveRobNorth(Bot internal, Graphics2D g2) {    
        CAT.debug("moving bot one square to the north");
        AlphaComposite ac = AC_SRC_OVER;         
        if (internal.isVirtual())
            ac = AC_SRC_OVER_05;
        Image imgRob = robosCrop[internal.getFacing() + internal.getBotVis() * 4];
        Location botStartPos = internal.getPos();
        Location botEndPos = new Location (botStartPos.getX(), botStartPos.getY()+1);
        
        synchronized (rescaleLock) {                    
            int feldSize = scaledFeldSize;//  FELDSIZE;//(int)(FELDSIZE*dScale);
            int xposScaled=  (internal.getX()-1) *feldSize;
            int yposScaled = (sf.getSizeY() - internal.getY()-1)*feldSize;                         
            int clipLength = 2*feldSize;    
            
            
            
            BufferedImage offScreenClipImage = offScreenImage.getSubimage(xposScaled, yposScaled, feldSize,clipLength);
            Graphics2D myg = (Graphics2D) offScreenClipImage.getGraphics();
           // Raster blankBg;
            BufferedImage blankBgImage;
            if (useStaticBg){
                blankBgImage = staticBackground.getSubimage(xposScaled, yposScaled, feldSize,clipLength);
            }
            else {            
             //   blankBg = offscreeClipImage.getData(new Rectangle(0,0,feldSize,clipLength));
                blankBgImage = new BufferedImage(feldSize, clipLength, offScreenClipImage.getType());
                blankBgImage.getGraphics().drawImage(offScreenClipImage,0,0,feldSize,clipLength,this);
            }
                                    
            paintBotsOnPositionButNotMe(botStartPos, internal, myg,0, feldSize);
            paintBotsOnPositionButNotMe(botEndPos, internal,myg,0,0);
           
            myg.setComposite(ac);
            
           
            int animationOffsetMoveRob =currentAnimationConfig.getAnimationOffsetMoveRob();           
            int animationDelayMoveRob = currentAnimationConfig.getAnimationDelayMoveRob();
            Rectangle oldClipBounds = g2.getClipBounds();
            g2.setClip(xposScaled,yposScaled,feldSize, clipLength);
            Composite oldComp  = g2.getComposite();
            g2.setComposite(ac);
            for (int yoffset = 0; yoffset >= -feldSize; yoffset -= animationOffsetMoveRob) {
                if (useStaticBg) {
                	    myg.drawImage(blankBgImage, 0,0, feldSize, clipLength, this); // paint the image
               	}
               	else {
               	    myg.drawImage(blankBgImage, 0,0, feldSize, clipLength, this);
               	    //offScreenClipImage.setData(blankBg);
                }
                paintBotsOnPositionButNotMe(botStartPos, internal, myg,0, feldSize);
                paintBotsOnPositionButNotMe(botEndPos, internal,myg,0,0);                     
                myg.drawImage(imgRob, 0,feldSize+yoffset, feldSize, feldSize, this); // paint the image                                                                                                           
                g2.drawImage(offScreenClipImage, xposScaled, yposScaled, feldSize, clipLength,this);
                
                waitSomeTime(animationDelayMoveRob,this);                 
                   
                }
           
            
            if (useStaticBg){
        	  myg.drawImage(blankBgImage, 0,0, feldSize, clipLength, this); // paint the image
            }
            else {
                Graphics2D tmpg = (Graphics2D)offScreenImage.getGraphics();             	   
          	   Composite tmpComp = tmpg.getComposite();             	   
          	   tmpg.setComposite(AC_SRC_OVER);
          	   tmpg.drawImage(blankBgImage,xposScaled,yposScaled,feldSize,clipLength,this);
          	   tmpg.setComposite(tmpComp); 
                //myg.drawImage(blankBgImage, 0,0, feldSize, clipLength, this); // paint the image
                //offscreeClipImage.setData(blankBg);
            }
            
            g2.setClip(oldClipBounds);
            g2.setComposite(oldComp);
        }
    }
    
  
   
   
    private void moveRobSouth(Bot internal,Graphics2D g2) {
        CAT.debug("moving bot one square to the south");
        AlphaComposite ac = AC_SRC_OVER;         
        if (internal.isVirtual())
            ac = AC_SRC_OVER_05;
        Image imgRob = robosCrop[internal.getFacing() + internal.getBotVis() * 4];
        Location botStartPos = internal.getPos();
        Location botEndPos = new Location (botStartPos.getX(), botStartPos.getY()-1);
        
        synchronized (rescaleLock) {
            int feldSize = scaledFeldSize;//(int)(FELDSIZE*dScale);
            int xpos64 = (internal.getX()-1) * feldSize;
            int ypos64 = (sf.getSizeY() -internal.getY())* feldSize;
            int clipLength=2*feldSize;
            
         
            BufferedImage offScreenClipImage  = offScreenImage.getSubimage(xpos64, ypos64, feldSize,clipLength);                                  
            Graphics2D myg = (Graphics2D) offScreenClipImage.getGraphics();
            BufferedImage blankBgImage;
            Raster blankBg;
            if (useStaticBg){
                blankBgImage  = staticBackground.getSubimage(xpos64, ypos64, feldSize,clipLength);
            }
            else  {
              //  blankBg = offScreenClipImage.getData(new Rectangle(0,0,feldSize,clipLength));
                blankBgImage = new BufferedImage(feldSize, clipLength, offScreenClipImage.getType());
                blankBgImage.getGraphics().drawImage(offScreenClipImage,0,0,feldSize,clipLength,this);
            }
           paintBotsOnPositionButNotMe(botStartPos, internal, myg,0, 0);
           paintBotsOnPositionButNotMe(botEndPos, internal,myg,0,feldSize);
            
            int animationOffsetMoveRob = currentAnimationConfig.getAnimationOffsetMoveRob();
            int animationDelayMoveRob = currentAnimationConfig.getAnimationDelayMoveRob();
            myg.setComposite(ac);    
            Rectangle oldClipBounds = g2.getClipBounds();
            g2.setClip(xpos64,ypos64,feldSize, clipLength);    
            Composite oldComp = g2.getComposite();
            g2.setComposite(oldComp);
            for (int yoffset = 0; yoffset <=feldSize; yoffset += animationOffsetMoveRob) {
                if (useStaticBg) {
                    myg.drawImage(blankBgImage,0,0,feldSize,clipLength,this);
                }
                else {
                    //offScreenClipImage.setData(blankBg);
                    myg.drawImage(blankBgImage,0,0,feldSize,clipLength,this);
                }
                paintBotsOnPositionButNotMe(botStartPos, internal, myg,0, 0);
                paintBotsOnPositionButNotMe(botEndPos, internal,myg,0,feldSize);
                myg.drawImage(imgRob, 0,yoffset, feldSize,feldSize, this); // paint the image                                                                                        
                g2.drawImage(offScreenClipImage, xpos64, ypos64, feldSize, clipLength,this);
                waitSomeTime(animationDelayMoveRob, this);
                   
             }
            if (useStaticBg){
                myg.drawImage(blankBgImage,0,0,feldSize,clipLength,this);
            }
            else {
               // offScreenClipImage.setData(blankBg);
               // myg.drawImage(blankBgImage,0,0,feldSize,clipLength,this);
                Graphics2D tmpg = (Graphics2D)offScreenImage.getGraphics();             	   
           	   Composite tmpComp = tmpg.getComposite();             	   
           	   tmpg.setComposite(AC_SRC_OVER);
           	   tmpg.drawImage(blankBgImage,xpos64,ypos64,feldSize,clipLength,this);
           	   tmpg.setComposite(tmpComp); 
            }
             g2.setClip(oldClipBounds);
             g2.setComposite(oldComp);
            
          
        }
    }


    private void moveRobEast(Bot internal, Graphics2D g2) {
        CAT.debug("moving bot one square to the east");
        AlphaComposite ac = AC_SRC_OVER;         
        if (internal.isVirtual())
            ac = AC_SRC_OVER_05;
        Image imgRob = robosCrop[internal.getFacing() + internal.getBotVis() * 4];
         Location botStartPos =  internal.getPos();
         Location botEndPos = new Location(botStartPos.getX()+1, botStartPos.getY());
        synchronized (rescaleLock) {        
           
         //   g2.scale(dScale, dScale);
            int feldSize = scaledFeldSize;//(int)(FELDSIZE*dScale);
            int xpos64 =  (internal.getX()-1)* feldSize;
            int ypos64 =  (sf.getSizeY()-internal.getY()) *feldSize;
            int clipLength = 2*feldSize;  
           
            
           
            BufferedImage offScreenClipImage = offScreenImage.getSubimage(xpos64, ypos64,clipLength,feldSize); 
            Graphics2D myg = (Graphics2D) offScreenClipImage.getGraphics();    
            Raster blankBg;
            BufferedImage blankBgImage;
            if (useStaticBg){
                 blankBgImage = staticBackground.getSubimage(xpos64, ypos64,clipLength,feldSize);
            }
            else {
                // blankBg = offScreenClipImage.getData(new Rectangle(0,0,clipLength, feldSize));
                blankBgImage = new BufferedImage( clipLength, feldSize, offScreenClipImage.getType());
                blankBgImage.setData(offScreenClipImage.getData(new Rectangle(0,0,clipLength, feldSize)));
                blankBgImage.getGraphics().drawImage(offScreenClipImage,0,0,clipLength,feldSize,this);
            }
            paintBotsOnPositionButNotMe(botStartPos, internal, myg,0, 0);
            paintBotsOnPositionButNotMe(botEndPos, internal,myg,feldSize,0);
           
            myg.setComposite(ac);
            int animationOffsetMoveRob =currentAnimationConfig.getAnimationOffsetMoveRob();
            int animationDelayMoveRob = currentAnimationConfig.getAnimationDelayMoveRob();
             Rectangle oldClipBounds = g2.getClipBounds();
             g2.setClip(xpos64,ypos64,clipLength,feldSize);
             Composite oldComp = g2.getComposite();
            for (int xoffset = 0; xoffset <= feldSize; xoffset += animationOffsetMoveRob) {
                if (useStaticBg) {
                    myg.drawImage(blankBgImage, 0,0, clipLength, feldSize, this); // paint the image
                }
                else {
                    //offScreenClipImage.setData(blankBg);
                    myg.drawImage(blankBgImage, 0,0, clipLength, feldSize, this);
                }
                paintBotsOnPositionButNotMe(botStartPos, internal, myg,0, 0);
                paintBotsOnPositionButNotMe(botEndPos, internal,myg,feldSize,0);
                                                   
                 myg.drawImage(imgRob, xoffset,0, feldSize, feldSize, this); // paint the image                                                                                        
                 g2.drawImage(offScreenClipImage, xpos64, ypos64, clipLength, feldSize,this);
                 waitSomeTime(animationDelayMoveRob, this);
               
            }
            if (useStaticBg){
                myg.drawImage(blankBgImage, 0,0, clipLength, feldSize, this); // paint the image
            }
            else {
                Graphics2D tmpg = (Graphics2D)offScreenImage.getGraphics();             	   
          	   Composite tmpComp = tmpg.getComposite();             	   
          	   tmpg.setComposite(AC_SRC_OVER);
          	   tmpg.drawImage(blankBgImage,xpos64,ypos64,clipLength,feldSize,this);
          	   tmpg.setComposite(tmpComp); 
               // myg.drawImage(blankBgImage, 0,0, clipLength, feldSize, this); // paint the image
                //offScreenClipImage.setData(blankBg);
            }
            g2.setClip(oldClipBounds);
            g2.setComposite(oldComp);
    }

    }

    private void moveRobWest(Bot internal, Graphics2D g2) {    
        CAT.debug("moving bot one square to the west");
        AlphaComposite ac = AC_SRC_OVER;         
        if (internal.isVirtual())
            ac = AC_SRC_OVER_05;
        Image imgRob = robosCrop[internal.getFacing() + internal.getBotVis() * 4];
       Location botStartPos = internal.getPos();
       Location botEndPos = new Location(botStartPos.getX()-1, botStartPos.getY());
        synchronized (rescaleLock) {           
      
            int feldSize = scaledFeldSize;//(int)( FELDSIZE*dScale);
            int clipLength = 2*feldSize;     
            int xpos64 =  (internal.getX()-2) * feldSize;
            int ypos64 = (sf.getSizeY() - internal.getY())* feldSize;                         
            
            
            
            BufferedImage offScreenClipImage   = offScreenImage.getSubimage(xpos64, ypos64,clipLength, feldSize);                        
            Graphics2D offScreenClipGraphics = (Graphics2D) offScreenClipImage.getGraphics();    
            
            BufferedImage blankBgImage;
            Raster blankBg;
            if (useStaticBg){
                blankBgImage = staticBackground.getSubimage(xpos64, ypos64,clipLength, feldSize);
            }
            else {
                //blankBg = offScreenClipImage.getData(new Rectangle(0,0,clipLength,feldSize));
                blankBgImage = new BufferedImage( clipLength, feldSize, offScreenClipImage.getType());
                blankBgImage.getGraphics().drawImage(offScreenClipImage,0,0,clipLength,feldSize,this);
            }
           
            
            
            paintBotsOnPositionButNotMe(botStartPos, internal, offScreenClipGraphics,feldSize, 0);
            paintBotsOnPositionButNotMe(botEndPos, internal,offScreenClipGraphics,0,0);
       
            int animationOffsetMoveRob =currentAnimationConfig.getAnimationOffsetMoveRob();
            int animationDelayMoveRob = currentAnimationConfig.getAnimationDelayMoveRob();           
            offScreenClipGraphics.setComposite(ac);
            Rectangle oldClipBounds = g2.getClipBounds();
            g2.setClip(xpos64,ypos64,clipLength,feldSize);
            Composite oldComp = g2.getComposite();
            g2.setComposite(ac);
            for (int xoffset = 0; xoffset >= -feldSize; xoffset -= animationOffsetMoveRob) {                                                
                if (useStaticBg) {    
                    offScreenClipGraphics.drawImage(blankBgImage,0,0,clipLength,feldSize,this); 
                }
                else {
                    //offScreenClipImage.setData(blankBg);
                    offScreenClipGraphics.drawImage(blankBgImage,0,0,clipLength,feldSize,this);
                }
                	paintBotsOnPositionButNotMe(botStartPos, internal, offScreenClipGraphics,feldSize, 0);
                    paintBotsOnPositionButNotMe(botEndPos, internal,offScreenClipGraphics,0,0);
                    offScreenClipGraphics.drawImage(imgRob, feldSize+xoffset,0, feldSize, feldSize, this); // paint the image                                                                                        
                    g2.drawImage(offScreenClipImage, xpos64, ypos64, clipLength, feldSize,this);
                    waitSomeTime(animationDelayMoveRob, this);
                   
                }
            
            	
               if (useStaticBg) {            	
                   offScreenClipGraphics.drawImage(blankBgImage,0,0,clipLength,feldSize,this);
               }
               else {
                   //offScreenClipImage.setData(blankBg);
                   //offScreenClipGraphics.drawImage(blankBgImage,0,0,clipLength,feldSize,this);
                   Graphics2D tmpg = (Graphics2D)offScreenImage.getGraphics();             	   
              	   Composite tmpComp = tmpg.getComposite();             	   
              	   tmpg.setComposite(AC_SRC_OVER);
              	   tmpg.drawImage(blankBgImage,xpos64,ypos64,clipLength,feldSize,this);
              	   tmpg.setComposite(tmpComp);  
               }
                g2.setClip(oldClipBounds);
                g2.setComposite(oldComp);
        }
    }
    
    
    
    
    
    
    
    protected /*synchronized*/ void animateRobUTurn(Bot rob) {
        waitForPhaseDisplay();
        if (!AnimationConfig.areMovementAnimationsEnabled()){
            return;
        }
            
        Bot internal = (Bot) internalBotHash.get(rob.getName());// getBotByName(rob.getName());
        turnRobot(internal, 180, 2* currentAnimationConfig.getAnimationStepsTurnRob(), true);
        internal.turnClockwise();
        internal.turnClockwise();
        waitSomeTime(currentAnimationConfig.getDelayBetweenActions(),this);
    }
   
    
    /** @param direction either BOT_TURN_CLOCKWISE or BOT_TURN_COUNTER_CLOCKWISE in MessageID*/
    protected /* synchronized*/ void animateRobTurn(Bot rob, int direction) {
        waitForPhaseDisplay();
        if (!AnimationConfig.areMovementAnimationsEnabled()){            
            return;
        }
        try {
	        Bot internal = (Bot) internalBotHash.get(rob.getName());// getBotByName(rob.getName());
	       
	        int oldFacing = internal.getFacing();
	        int animationStepsTurnRob =currentAnimationConfig.getAnimationStepsTurnRob();	     
	        
	        if (direction == OtherConstants.BOT_TURN_CLOCKWISE){
	            turnRobot(internal, 90,animationStepsTurnRob, true);
	            internal.turnClockwise();
	        }
	        else {
	            turnRobot(internal, 90, animationStepsTurnRob,false);
	            internal.turnCounterClockwise();
	        }
        }
        // better safe than sorry:
        catch ( RasterFormatException fixmeCanForExampleHappenIfWeWantToAnimateABotThatHasBeenKilledRightBefore){
            CAT.error(fixmeCanForExampleHappenIfWeWantToAnimateABotThatHasBeenKilledRightBefore);
        }
        waitSomeTime(currentAnimationConfig.getDelayBetweenActions(),this);
         
    }
    
    
    private void turnRobot(Bot internal, int angle, int animationSteps, boolean clockWise) {       
        AlphaComposite ac = AC_SRC_OVER;         
        if (internal.isVirtual())
            ac = AC_SRC_OVER_05;
        Image cropRobImage = robosCrop[internal.getFacing() + internal.getBotVis() * 4];
        double rotateTheta;
        if (clockWise)
            rotateTheta= Math.toRadians(angle/animationSteps);
        else 
            rotateTheta = Math.toRadians(360-angle/animationSteps);  
        //CAT.debug("turning bot; theta="+rotateTheta);
        synchronized (rescaleLock) {       
            Graphics2D mainGraphics = (Graphics2D)getGraphics();  
            Composite oldComposite = mainGraphics.getComposite();
            int feldSize = scaledFeldSize;           
            int xposScaled=  (internal.getX()-1) *feldSize;
            int yposScaled = (sf.getSizeY() - internal.getY())*feldSize;                         
            int clipLength = feldSize;    
            int halfSize = feldSize/2;
           // TODO rasterFormatException 
            BufferedImage offScreenClipImage = offScreenImage.getSubimage(xposScaled, yposScaled, feldSize,clipLength); //new BufferedImage(feldSize, feldSize, BufferedImage.TYPE_INT_RGB);            
            Graphics2D offScreenClip = (Graphics2D) offScreenClipImage.getGraphics();     
            offScreenClip.setComposite(ac);       
            mainGraphics.setComposite(ac);
            BufferedImage blankImage;
            
            Raster blank;
            if (useStaticBg){
                blank = staticBackground.getData(new Rectangle(xposScaled,yposScaled,feldSize,clipLength));
                blankImage = staticBackground.getSubimage(xposScaled,yposScaled,feldSize,clipLength);
                //offScreenClipImage.setData(blank);
                offScreenClip.drawImage(blankImage,0,0,feldSize,clipLength,this);
                mainGraphics.drawImage(blankImage, xposScaled, yposScaled, feldSize, clipLength,this);
            }
            else {
                // there shouldn't be any active content on the offScreen image if useStaticBg==false
               // blank = offScreenClipImage.getData(new Rectangle(0,0,feldSize,clipLength));
                blankImage = new BufferedImage(feldSize, clipLength, BufferedImage.TYPE_INT_ARGB);
                blankImage.getGraphics().drawImage(offScreenClipImage,0,0,feldSize,clipLength,this);
               
            }    
                
                                                  
           // paintBotsOnPositionButNotMe(internal.getPos(), internal,offScreenClip,0,0);
           Raster blankWithBots = offScreenClipImage.getData(new Rectangle(0,0,feldSize,clipLength));
            //offScreenClipImage.setData(blank);
            
            // painting the animated bot           
            //offScreenClip.drawImage(cropRobImage, 0, 0, feldSize, feldSize, this);            
            
            int animationStepsTurnRob =animationSteps;//currentAnimationConfig.getAnimationStepsTurnRob();
            int animationDelayTurnRob = currentAnimationConfig.getAnimationDelayTurnRob();
             for (int step = 0; step<animationStepsTurnRob;step++) {
                 	 
               
               //  offScreenClip.drawImage(blankImage,0,0,feldSize,clipLength,this);	 
               //  paintBotsOnPositionButNotMe(internal.getPos(), internal,offScreenClip,0,0);
                 	offScreenClipImage.setData(blankWithBots); // erasing the offscreen image with the boardbackground
                     offScreenClip.rotate(rotateTheta,halfSize, halfSize); // rotating the robot pic further   
                     offScreenClip.drawImage(cropRobImage, 0, 0, feldSize, feldSize, this);                  
                     // paint the offscreen image on the screen:
                     mainGraphics.drawImage(offScreenClipImage, xposScaled, yposScaled, feldSize, clipLength,this);                
                     waitSomeTime(animationDelayTurnRob, this);                   
             }
             	if (useStaticBg) {
             	   //offScreenClipImage.setData(blank);
             	   offScreenClip.drawImage(blankImage,0,0,feldSize,clipLength,this);
             	}
             	else {
             	    // doesn't work: offScreenClipImage.setData(blank);
             	    // also doesn't work: offScreenClip.XYZ() 
             	   Graphics2D tmpg = (Graphics2D)offScreenImage.getGraphics();             	   
              	   Composite tmpComp = tmpg.getComposite();             	   
              	   tmpg.setComposite(AC_SRC_OVER);
              	   tmpg.drawImage(blankImage,xposScaled,yposScaled,feldSize,clipLength,this);
              	   tmpg.setComposite(tmpComp); 
                   mainGraphics.setComposite(oldComposite);   
             	}
             	
        }
    }
    
    
    protected void animateBotCrushed(Bot b){     
        SoundMan.playSound(SoundMan.CRUSHED);
        shrinkRobot(b);
    }
    
    protected void animatePitFall(Bot b){       
        SoundMan.playSound(SoundMan.PIT);
        Location pos = b.getPos();
        if (pos.x<1 || pos.y<1||pos.x>sf.getSizeX()|| pos.y>sf.getSizeY()){
            CAT.debug("ignoring pitfall animation for "+pos );
            //	don't shrink if bot fell from the board
        }
        else {
            if (sf.getFloor(pos.x, pos.y).isPit()){ // don't shrink if bot fell from the board
                shrinkRobot(b);
            }
        }

        synchronized (this){
            waitSomeTime(currentAnimationConfig.getLaserDelayAfterEndOfAnimation(), this);  
        }
    }
    
    private void shrinkRobot(Bot internal) {
       
       double scalePerStep = 0.9;
       int numberOfShrinks = 15;
        AlphaComposite ac = AC_SRC_OVER;         
        if (internal.isVirtual())
            ac = AC_SRC_OVER_05;
        Image cropRobImage = robosCrop[internal.getFacing() + internal.getBotVis() * 4];
       
       
        synchronized (this) {       
            Graphics2D mainGraphics = (Graphics2D)getGraphics();  
            Composite oldComposite = mainGraphics.getComposite();
            int feldSize = scaledFeldSize;           
            int xposScaled=  (internal.getX()-1) *feldSize;
            int yposScaled = (sf.getSizeY() - internal.getY())*feldSize;                         
            int clipLength = feldSize;    
            int halfSize = feldSize/2;
           
            
            BufferedImage backgroundImage=  offScreenImage.getSubimage(xposScaled, yposScaled, feldSize,clipLength); //new BufferedImage(feldSize, feldSize, BufferedImage.TYPE_INT_RGB);            
            Graphics2D background = (Graphics2D) backgroundImage.getGraphics();     
            background.setComposite(ac);       
            // saving a copy of the background:
            Raster blank = backgroundImage.getData(new Rectangle(0,0,feldSize,clipLength));           
            mainGraphics.drawImage(backgroundImage,xposScaled,yposScaled,feldSize,feldSize,this);
                       
            // painting the animated bot           
           // background.drawImage(cropRobImage, 0, 0, feldSize, feldSize, this);            
            mainGraphics.setComposite(ac);
            double doffset=0;
             for (int step = 0; step<numberOfShrinks;step++) {
                 	 backgroundImage.setData(blank); // erasing the offscreen image with the boardbackground
                 	 background.scale(scalePerStep, scalePerStep);
                 	 doffset += feldSize*(1-scalePerStep);
                 	 int offset = (int) doffset;                 	
                     background.drawImage(cropRobImage, 0+offset, 0+offset, feldSize, feldSize, this);                  
                     // paint the offscreen image on the screen:
                     
                     mainGraphics.drawImage(backgroundImage, xposScaled, yposScaled, feldSize, clipLength,this);                
                     waitSomeTime(100, this);            // TODO make delay configurable       
                }
                backgroundImage.setData(blank);
                internal.setPos(PIT); 
                mainGraphics.setComposite(oldComposite);
        }
    }
    
    
    
    
    
    
    protected /*synchronized*/ void animateRobMove(Bot rob, int direction) {
        waitForPhaseDisplay();
        
        // important: according to the code in SpielfeldSim we do not get
        //            the updated robot position;
        //            the updated position will be the endposition of the total move,
        //            as ersetzeRobos() will be called when the robot has reached its
        //            final position
        //            THIS METHOD will be called for each single step of a move
        //             (i.e. three times for a "Move 3 forward")
        //            So we have to update our internal position of the robot in
        //            between to show an animation that makes sense
        try {
        String name = rob.getName();
        
        Bot internal = (Bot) internalBotHash.get(name);
        
        
        
        
        int oldX = internal.getX();
        int oldY = internal.getY();
     
        // paint the move animation  and update the position in my internal robot array
        switch (direction) {
            case NORTH:
                {
                    if (oldY < sf.getSizeY()) {
                        moveRobNorth(internal,(Graphics2D)getGraphics());
                        internal.setPos(oldX, oldY + 1);
                    }
                    return;
                }
            case EAST:
                {
                    if (oldX < sf.getSizeX()) {
                        moveRobEast(internal, (Graphics2D)getGraphics());
                        internal.setPos(oldX + 1, oldY);
                    }
                    return;
                }
            case WEST:
                {
                    if (oldX > 1) {
                        moveRobWest(internal,(Graphics2D)getGraphics());
                        internal.setPos(oldX - 1, oldY);
                    }
                    return;
                }
            case SOUTH:
                {
                    if (oldY > 1) {
                        moveRobSouth(internal,(Graphics2D)getGraphics());
                        internal.setPos(oldX, oldY - 1);
                    }
                    return;
                }
            default:
                {
                    // this must not happen,
                    // otherwise the whole gui might be useless as it keeps probably
                    // a wrong position for one robot
                    CAT.fatal("Got illgeal direction for animating robot");
                }
        }
        } // better safe than sorry:
        catch ( RasterFormatException fixmeCanForExampleHappenIfWeWantToAnimateABotThatHasBeenKilledRightBefore){
            CAT.error(fixmeCanForExampleHappenIfWeWantToAnimateABotThatHasBeenKilledRightBefore);
        }
        waitSomeTime(currentAnimationConfig.getDelayBetweenActions(),this);
    }


    /**
     * Draws animated robot lasers.
     @param sourceRob position of firing robot
     @param targetRob position of the robot hit
     */

    public void doRobLaser(Bot sourceRob, Bot targetRob) {
        //  allDone = false;
        if (CAT.isDebugEnabled())
            CAT.debug("doRobLaser: " + sourceRob.getName() + " -> " + targetRob.getName());
        Location source = sourceRob.getPos();
        Location target = targetRob.getPos();
        int  laserFacing = sourceRob.getFacing();
        int laenge = calculateLaserLength(source, target, laserFacing);
        laenge *= scaledFeldSize;

        String name = sourceRob.getName();

        Color robColor = getRobColor(name);
        
        
        SoundMan.playSound(BotVis.getBotLaserSoundByName(name));
        synchronized (this) {
            waitSomeTime(currentAnimationConfig.getLaserDelayBetweenStartOfSoundAndAnimation(),this);
           
            		
            
            Graphics2D g2 = (Graphics2D) getGraphics();
            //g2.scale(dScale, dScale);
            int step = Math.max(1, (int) (((double)laenge)/FULL_LENGTH_DOUBLE)); // step must not be 0 
                    																					// otherwise the while loop below won't exit
            int tmp_laenge = step;
            
            //for (int i = 1; i <= FULL_LENGTH_INT; i++) {
            //    int tmp_laenge = (int) ((((double) i) / FULL_LENGTH_DOUBLE) * laenge);
           
            int delayPerStep = currentAnimationConfig.getLaserDelayPerAnimationStep();
            while (tmp_laenge<=laenge)  {  
               
                paintActiveRobLaser(g2, source, laserFacing,tmp_laenge, robColor);
                tmp_laenge+=step;
                //     synchronized(this){
                waitSomeTime(delayPerStep,this);
               
                //   }
            }
            repaint();
            waitSomeTime(currentAnimationConfig.getLaserDelayAfterEndOfAnimation() ,this);
        }

        // drawRobLaser=false;
        if (SoundMan.isSoundActive()) {
            // SoundMan.playSound(SoundMan.BUMM);
            synchronized (this) {
               waitSomeTime(200, this);
            }
        }
       waitSomeTime(currentAnimationConfig.getDelayBetweenActions(), this);


    }

    /** Berechnet die (Java-)Pixel-Koordinaten der linken oberen Ecke eines Bord-Feldes.
     Gibt die x- und y-Pixelwerte der linken oberen Ecke des Feldes
     mit der Position (x,y) auf dem Spielplan zurueck.
     @param x Die X-Koordinate des Feldes
     @param y Die Y-Koordinate des Feldes
     @return Die Position der linken oberen Ecke des Feldes als Java-Pixelwerte zum Zeichnen.
     */
    private Location mapC2PixelNorthWest(int x, int y) {
        Location pixel = new Location();
        pixel.x = (x - 1) * scaledFeldSize;
        pixel.y = (sf.getSizeY() - y) * scaledFeldSize;
        return pixel;
    }

    /** Berechnet die (Java-)Pixelwerte fuer den Mittelpunkt des Feldes.
     Genauer: Den Punkt (31,31) auf dem 64x64 grossen Feld mit Koordinaten
     zwischen 0 und 63.
     */
    private Location mapC2PixelCenter(int x, int y) {
        Location pixel = mapC2PixelNorthWest(x, y);
        pixel.x += scaledFeldSize/2;
        pixel.y += scaledFeldSize/2;
        return pixel;
    }

    private void paintActiveRobLaser(Graphics g, Location source, int laserFacing, int actualLength, Color c) {
        // Laser sollen immer von Source nach Target gezeichnet werden

        int breite = (int)(4*dScale); // Die Breite des Lasers, sollte gerade sein
        int lSourceX = 0;
        int lSourceY = 0; // Anfangspunkt des Lasers in Pixeln,
        Location tmp = mapC2PixelCenter(source.x, source.y); /* Mitte (Punkt (31,31) auf Feld
							   mit Punkten von 0 bis 63,
							   also einem 64x64 grossen Feld

							*/

        Graphics2D g2d = (Graphics2D) g;
        AlphaComposite ac = AC_SRC_OVER_05;
        g2d.setComposite(ac);
        g2d.setColor(c);
        switch (laserFacing) {
            case NORTH:
                {
                    lSourceX = tmp.x - (breite / 2);
                    lSourceY = tmp.y - actualLength;
                    g2d.fillRect(lSourceX, lSourceY, breite, actualLength);
                    break;
                }
            case EAST:
                {
                    lSourceX = tmp.x;
                    lSourceY = tmp.y - (breite / 2 );                    
                    g2d.fillRect(lSourceX, lSourceY, actualLength, breite);
                    break;
                }
            case SOUTH:
                {
                    lSourceX = tmp.x - (breite / 2 );
                    lSourceY = tmp.y;
                    g2d.fillRect(lSourceX, lSourceY, breite, actualLength);
                    break;
                }
            case WEST:
                {
                    lSourceX = tmp.x - actualLength;
                    lSourceY = tmp.y - (breite / 2 - 1);
                    g2d.fillRect(lSourceX, lSourceY, actualLength, breite);
                    break;
                }
            default :
                {
                    CAT.error("BoardView.paintActiveRobLaser: ");
                    CAT.error("Ungueltige Laserrichtung: " + laserFacing);
                }
        }// end switch facing
        g2d.setComposite(AC_SRC);
    }


    /**
     Berechnet die Laenge eines Lasers (in Feldern) zwischen zwei Botn.
     Bsp: Schiesst ein Bot an Position (2,2) auf einen Bot an
     Position (5,2), so wird 3 zurueckgegeben
     (=> multipliziert man den Rueckgabewert mit 64, so erhaelt man die
     zu zeichnende Laserlaenge in Pixeln).
     @param source Das Startfeld des Lasers
     @param target Das Feld des Ziels
     @param facing Die Richtung, in die der Laser schiesst (0=NORTH, 1=EAST, 2=SOUTH, 3=WEST)

     @return Die Anzahl der Felder, ueber die der Laser geht (inklusive Startfeld).

     */
    private int calculateLaserLength(Location source, Location target, int facing) {

        int laenge = 0;
       
        switch (facing) {
            case NORTH:
                {
                    laenge = target.y - source.y;
                    break;
                }
            case EAST:
                {
                    laenge = target.x - source.x;
                    break;
                }
            case SOUTH:
                {
                    laenge = source.y - target.y;
                    break;
                }
            case WEST:
                {
                    laenge = source.x - target.x;
                    break;
                }
            default:
                {
                    CAT.error("BoardView.calculateLaserLength(): ungueltige Laserrichtung: " + facing);
                }
        }
        //System.err.println("calculate Length: ("+source.x+","+source.y+")-"+facing+"->("+target.x+","+target.y+") ist "+laenge+" lang");
        return laenge;
    }

    private void paintActiveBordLaser(Graphics g, Location source, int facing, Color c, int actualLength) {

        Graphics2D g2d = (Graphics2D) g;
        AlphaComposite ac = AC_SRC_OVER;//, 0.5f
        g2d.setComposite(ac);
        g2d.setColor(c);

        int breite = (int)(dScale*4) ; // Die Breite des Lasers, sollte gerade sein
        int lSourceX = 0;
        int lSourceY = 0; // Anfangspunkt des Lasers in Pixeln,
        Location tmp = mapC2PixelCenter(source.x, source.y);
        // synchronized (lock) {
        switch (facing) {
            case NORTH:
                {
                    lSourceX = tmp.x - (breite / 2 - 1);
                    lSourceY = tmp.y - actualLength + (scaledFeldSize/breite);
                    g2d.fillRect(lSourceX, lSourceY, breite, actualLength);
                    g2d.setColor(sndLaserColor);
                    g2d.drawRect(lSourceX, lSourceY, breite, actualLength);
                    break;
                }
            case EAST:
                {
                    lSourceX = tmp.x - (scaledFeldSize/breite)+breite;
                    lSourceY = tmp.y - (breite / 2 - 1);                   
                    g2d.fillRect(lSourceX, lSourceY, actualLength, breite);
                    g2d.setColor(sndLaserColor);
                    g2d.drawRect(lSourceX, lSourceY, actualLength, breite);
                    break;
                }
            case SOUTH:
                {
                    lSourceX = tmp.x - (breite / 2 - 1);
                    lSourceY = tmp.y - (scaledFeldSize/4)+1;
                    g2d.fillRect(lSourceX, lSourceY, breite, actualLength);
                    g2d.setColor(sndLaserColor);
                    g2d.drawRect(lSourceX, lSourceY, breite, actualLength);
                    break;
                }
            case WEST:
                {
                    lSourceX = tmp.x - actualLength + (scaledFeldSize/4)+((int)(3*dScale));
                    lSourceY = tmp.y - (breite / 2 - 1);
                    g2d.fillRect(lSourceX, lSourceY, actualLength - 2, breite);
                    g2d.setColor(sndLaserColor);
                    g2d.drawRect(lSourceX, lSourceY, actualLength - 2, breite);
                    break;
                }
            default :
                {
                    CAT.error("BoardView.paintActiveRobLaser: ");
                    CAT.error("Ungueltige Laserrichtung: " + facing);
                }
        }// end switch facing
        //   allDone = true;
        //   lock.notifyAll();
        // }
        //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
    }

    /**
     @param laserPos Die Koordinaten des schiessenden BordLasers
     @param laserDir Die Ausrichtung des Lasers
     @param targetRob Die Koordinaten des getroffenen Bots
     @param surrounding Das ScrollPane in dem der Canvas dargestellt wird
     */
    protected void doBordLaser(Location laserPos, int laserDir, int strength, Location targetRob, JViewport surrounding) {
        // init laser values
      

        int laenge = calculateLaserLength(laserPos, targetRob, laserDir);
        laenge = laenge * scaledFeldSize + (scaledFeldSize/4)+((int)(dScale*3));
        Color c = laserColor[strength - 1];

        // get viewable area
        //	Point upperLeftCorner = surrounding.getViewPosition();
        //  Dimension size = surrounding.getExtentSize();

        // Graphics g = getGraphics();
        //g.setClip(upperLeftCorner.x,upperLeftCorner.y,size.width,size.height);
        //activeBordLasers=true; // non-animated lasers will
        //paint(g);              // be deleted now

        // paint lasers step by step
        
        Graphics2D g2 = (Graphics2D) getGraphics();
      //  g2.scale(dScale, dScale);
        for (int i = 1; i <= FULL_LENGTH_INT; i++) {
            int tmp_laenge = (int) ((((double) i) / FULL_LENGTH_DOUBLE) * laenge);
            
            paintActiveBordLaser(g2, laserPos, laserDir, c, tmp_laenge);

        }
        // activeBordLasers=false; // now paint the non-animated
        repaint();              // lasers again
       
            synchronized (this){
                waitSomeTime(currentAnimationConfig.getLaserDelayAfterEndOfAnimation(), this);  
            }
        
    }

    private boolean turner(int x, int y, int r) {
        Floor floor = sf.floor(x, y);
        return floor.isBelt() && (floor.getBeltDirection() == r);
    }

    protected void preview(int phase, Bot simRob) {
        if (phase == 0) {
            //scoutOn = true; // flag for repaint: yes, paint scout!
            previewRob = null;
            deleteScout();
            //repaint();
            return;
        }

        Bot[] robs = new Bot[1];
        robs[0] = simRob;
        for (int i = 1; i < phase + 1; i++) {
            //sf.doPhase(phase, simRob);
            sf.doPhase(phase, robs);
        }
        //previewRob = vorschauRobArray[0];
        previewRob = simRob;
        showScout(simRob.getPos());

        //repaint();

    }

    protected void preview(int phasen, Bot[] previewRoboters) {
        if (phasen == 0) {
            previewRob = null;
            deleteScout();
            //repaint();
            return;
        }

        for (int i = 1; i < phasen + 1; i++) {
            sf.doPhase(i, previewRoboters);
        }
        previewRob = previewRoboters[0];
        showScout(previewRob.getPos());
        //repaint();

    }

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    protected void paintFeldBoden(Graphics g, int xpos, int ypos, int actx, int acty) {
        paintFeldBoden(g, xpos, ypos, actx, acty, scaledFeldSize, scaledFeldSize);
    }

   //TODO: Make method private again and find a proper way to update the hash map if nec.
   public void initFloorHashMap() {
        int sizeX = sf.getSizeX();
        int sizeY = sf.getSizeY();
        for (int x = 1; x <= sizeX; x++) {
            for (int y = 1; y <= sizeY; y++) {
                Location l = new Location(x, y);
                Image img = getFloorImage(x, y);
                floorElementHash.put(l, img);
            }
        }
    }


    private Image getFloorImage(int xpos, int ypos) {
        Floor floor = sf.floor(xpos, ypos);
        switch (floor.getType()) {

            case (Board.FL_PIT):
                return diverseCrop[3];
            case (Board.FL_NORMAL):
                return diverseCrop[24 + ((xpos * ypos * 19) % 17) % 4];
            case (Board.FL_ROTGEAR):
                if (floor.getInfo() == 0)
                    return diverseCrop[2];
                else
                    return diverseCrop[1];
            case (Board.FL_REPAIR):
                if (floor.getInfo() == 1)
                    return diverseCrop[4];
                else
                    return diverseCrop[5];
                // ------------------- normale Fliessbaender -------------------------

            case (Board.FN1):
                return cbeltCrop[14];
            case (Board.FE1):
                return cbeltCrop[19];
            case (Board.FW1):
                return cbeltCrop[9];
            case (Board.FS1):
                return cbeltCrop[4];

            case (Board.NFW1):
                if (turner(xpos, ypos - 1, Board.NORTH))
                    return cbeltCrop[15];
                else
                    return cbeltCrop[6];
            case (Board.NFE1):
                if (turner(xpos, ypos - 1, Board.NORTH))
                    return cbeltCrop[18];
                else
                    return cbeltCrop[7];
            case (Board.SFW1):
                if (turner(xpos, ypos + 1, Board.SOUTH))
                    return cbeltCrop[13];
                else
                    return cbeltCrop[3];
            case (Board.SFE1):
                if (turner(xpos, ypos + 1, Board.SOUTH))
                    return cbeltCrop[10];
                else
                    return cbeltCrop[0];
            case (Board.EFN1):
                if (turner(xpos - 1, ypos, Board.EAST))
                    return cbeltCrop[16];
                else
                    return cbeltCrop[5];
            case (Board.EFS1):
                if (turner(xpos - 1, ypos, Board.EAST))
                    return cbeltCrop[12];
                else
                    return cbeltCrop[2];
            case (Board.WFN1):
                if (turner(xpos + 1, ypos, Board.WEST))
                    return cbeltCrop[17];
                else
                    return cbeltCrop[8];
            case (Board.WFS1):
                if (turner(xpos + 1, ypos, Board.WEST))
                    return cbeltCrop[11];
                else
                    return cbeltCrop[1];

            case (Board.NFEW1):
                return cbeltCrop[22];
            case (Board.SFWE1):
                return cbeltCrop[20];
            case (Board.EFNS1):
                return cbeltCrop[23];
            case (Board.WFNS1):
                return cbeltCrop[21];

                // ------------------------ Expressfliessbaender ---------------------

            case (Board.FN2):
                return ebeltCrop[14];
            case (Board.FE2):
                return ebeltCrop[19];
            case (Board.FW2):
                return ebeltCrop[9];
            case (Board.FS2):
                return ebeltCrop[4];

            case (Board.NFW2):
                if (turner(xpos, ypos - 1, Board.NORTH))
                    return ebeltCrop[16];
                else
                    return ebeltCrop[6];
            case (Board.NFE2):
                if (turner(xpos, ypos - 1, Board.NORTH))
                    return ebeltCrop[17];
                else
                    return ebeltCrop[7];
            case (Board.SFW2):
                if (turner(xpos, ypos + 1, Board.SOUTH))
                    return ebeltCrop[13];
                else
                    return ebeltCrop[3];
            case (Board.SFE2):
                if (turner(xpos, ypos + 1, Board.SOUTH))
                    return ebeltCrop[10];
                else
                    return ebeltCrop[0];
            case (Board.EFN2):
                if (turner(xpos - 1, ypos, Board.EAST))
                    return ebeltCrop[15];
                else
                    return ebeltCrop[5];
            case (Board.EFS2):
                if (turner(xpos - 1, ypos, Board.EAST))
                    return ebeltCrop[12];
                else
                    return ebeltCrop[2];
            case (Board.WFN2):
                if (turner(xpos + 1, ypos, Board.WEST))
                    return ebeltCrop[18];
                else
                    return ebeltCrop[8];
            case (Board.WFS2):
                if (turner(xpos + 1, ypos, Board.WEST))
                    return ebeltCrop[11];
                else
                    return ebeltCrop[1];


            case (Board.NFWE2):
                return ebeltCrop[22];
            case (Board.SFWO2):
                return ebeltCrop[20];
            case (Board.EFNS2):
                return ebeltCrop[23];
            case (Board.WFNS2):
                return ebeltCrop[21];


            default:
                return null;
        }

    }


    protected void paintFeldBoden(Graphics g, int xpos, int ypos, int actx, int acty,
                                  int width, int height) {

        //  CAT.debug("xpos="+xpos+" ypos="+ypos+"actx="+actx+"acty="+acty);
        Location l = new Location(xpos, ypos);
        Image img = (Image) floorElementHash.get(l);
        g.drawImage(img, actx, acty, width, height, this);
    }

    // for painting crushers
    private static final int[] crushlb_x = {20, 30, 30, 30, 40};
    private static final int[] crushlb_y = {35, 25, 35, 45, 35};

    private void paintCrusher(Graphics2D g, Floor floor,
                              int actx, int acty) {

        g.setComposite(AC_SRC_OVER);
        g.drawImage(diverseCrop[10], actx, acty, scaledFeldSize, scaledFeldSize, this);
        g.setColor(Color.white);
        for (int phasecount = 1; phasecount <= 5; phasecount++) {
            if (floor.isCrusherActive(phasecount)) {
                int strx = actx + crushlb_x[phasecount - 1];
                int stry = acty + crushlb_y[phasecount - 1];
                g.drawString("" + phasecount, strx, stry);
            }
        } //for
    }

    /** paints the (back-)ground of the board*/
    private void paintSpielfeldBoden(Graphics g2) {

        Graphics2D g = (Graphics2D) g2;
        g.setComposite(AC_SRC_OVER);
        // Grenzen des zu zeichnenden Bereichs berechnen:
        
        int foo64 = scaledFeldSize;
        Rectangle clip = g.getClipBounds();
        int x0 = clip.x / foo64 + 1;
        int y0 = clip.y / foo64 + 1;
        int x1 = (clip.x + clip.width - 1) /foo64 + 1;
        int y1 = (clip.y + clip.height - 1) /foo64 + 1;
        x1 = Math.min(x1, sf.getSizeX());
        y1 = Math.min(y1, sf.getSizeY());

        for (int hori = x0; hori <= x1; hori++) {
            for (int vert = y0; vert <= y1; vert++) {
                int actx = (hori - 1) * foo64;
                int acty = (vert - 1) * foo64;
                int xpos = hori;
                int ypos = sf.getSizeY() + 1 - vert;
                Floor floor = sf.floor(xpos, ypos);

                paintFeldBoden(g, xpos, ypos, actx, acty);
                if ((floor.isBelt()) && (floor.getInfo() > 0))
                    paintCrusher(g, floor, actx, acty);
            }
        }
    }


    /** Paints the boardlaser-elements*/
    private void paintLaserStrahlen(Graphics g) {
        Graphics2D dbg = (Graphics2D) g;
        AlphaComposite ac = null;
        //	if (activeBordLasers)
        // ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f);
        //else
        ac = AC_SRC_OVER_05;
        dbg.setComposite(ac);
        
        LaserDef actuallaser;
        for (Enumeration e = sf.getLasers().elements(); e.hasMoreElements();) {
            actuallaser = ((LaserDef) e.nextElement());
            int lx = actuallaser.x - 1;
            int ly = sf.getSizeY() - actuallaser.y;
            int lf = actuallaser.facing;
            int ll = actuallaser.length;
            int ls = actuallaser.strength;

            switch (ls) {
                case 1:
                    dbg.setColor(Color.red.brighter());
                    break;
                case 2:
                    dbg.setColor(Color.orange);
                    break;
                case 3:
                    dbg.setColor(Color.yellow);
                    break;
            }

            int vier = (int)(4*dScale);
            int dreissig = (int)(30*dScale);
            switch (lf) {
                case 0:
                    dbg.fillRect(lx * scaledFeldSize +dreissig, (ly - ll + 1) * scaledFeldSize, vier, ll * scaledFeldSize);
                    break;
                case 1:
                    dbg.fillRect(lx * scaledFeldSize, ly * scaledFeldSize +dreissig, ll * scaledFeldSize,vier);
                    break;
                case 2:
                    dbg.fillRect(lx * scaledFeldSize + dreissig, ly * scaledFeldSize,vier, ll * scaledFeldSize);
                    break;
                case 3:
                    dbg.fillRect((lx - ll + 1) * scaledFeldSize, ly * scaledFeldSize +dreissig, ll * scaledFeldSize, vier);
                    break;
            }
        }
        dbg.setComposite(AC_SRC);
    }

    /** Paints the wall(s) of a square field at position (xpos, ypos)
     on board and (pixel-)position (actx, acty)
     */
    private void paintWall(Graphics g, int xpos, int ypos, int actx, int acty) {
       
        int vier = (int)(dScale*4);
        int fuenf = (int)(dScale*5);
        int sieben = (int)(dScale*7);
        int sechs = (int) (dScale*6);
        int neun20 = (int)(dScale*29);
        int zwei40 = (int)(dScale*42);
        int vier20 = (int) (dScale*24);
        int sieben30 = (int)(dScale*37);
        int zehn = (int)(dScale*10);
        // paint wall in the north, if any      
        if (sf.nw(xpos, ypos).isExisting()) {
            // is there a boardlaser to paint at this wall?
            if (sf.nw(xpos, ypos).getSouthDeviceType() == Wall.TYPE_LASER) {
                g.drawImage(diverseCrop[15], actx, acty + fuenf, scaledFeldSize, scaledFeldSize, this);
            }
            // is there a pusher?
            if (sf.nw(xpos, ypos).getSouthDeviceType() == Wall.TYPE_PUSHER) {
                g.drawImage(diverseCrop[7], actx - 1, acty + fuenf, scaledFeldSize, scaledFeldSize, this);
                // ------------draw text (phases when active) on pusher --------------------
                for (int phasecount = 1; phasecount <= 5; phasecount++) {
                    if (sf.nw(xpos, ypos).isSouthPusherActive(phasecount)) {
                        int strx = actx + zehn * phasecount;
                        g.setColor((phasecount % 2) == 0 ?
                                Color.black : Color.yellow);
                        g.drawString("" + phasecount, strx - 1, acty + neun20);
                    }
                }

            }
            g.drawImage(diverseCrop[13], actx, acty - sechs, scaledFeldSize, scaledFeldSize, this);
        }

        // paint wall in the south, if any
        if (sf.sw(xpos, ypos).isExisting()) {
            if (sf.sw(xpos, ypos).getNorthDeviceType() == Wall.TYPE_LASER) {
                g.drawImage(diverseCrop[17], actx, acty - fuenf, scaledFeldSize, scaledFeldSize, this);
            }
            if (sf.sw(xpos, ypos).getNorthDeviceType() == Wall.TYPE_PUSHER) {
                g.drawImage(diverseCrop[8], actx, acty - fuenf, scaledFeldSize, scaledFeldSize, this);
                // -----------text on pusher--------------------
                for (int phasecount = 1; phasecount <= 5; phasecount++) {
                    if (sf.sw(xpos, ypos).isNorthPusherActive(phasecount)) {
                        int strx = actx + zehn * phasecount;
                        g.setColor((phasecount % 2) == 0 ?
                                Color.black : Color.yellow);
                        g.drawString("" + phasecount, strx - 1, acty + zwei40);
                    }
                } //for
            }
            g.drawImage(diverseCrop[13], actx, acty + scaledFeldSize-sechs, scaledFeldSize, scaledFeldSize, this);
        }

        // paint wall in the east, if any

        if (sf.ew(xpos, ypos).isExisting()) {
            if (sf.ew(xpos, ypos).getWestDeviceType() == Wall.TYPE_LASER) {
                g.drawImage(diverseCrop[14], actx - sechs+1, acty, scaledFeldSize, scaledFeldSize, this);
            }
            if (sf.ew(xpos, ypos).getWestDeviceType() == Wall.TYPE_PUSHER) {
                g.drawImage(diverseCrop[6], actx - sechs, acty, scaledFeldSize, scaledFeldSize, this);
                // ------------text on pusher --------------------
                for (int phasecount = 1; phasecount <= 5; phasecount++) {
                    if (sf.ew(xpos, ypos).isWestPusherActive(phasecount)) {
                        int stry = acty + zehn * phasecount;
                        g.setColor((phasecount % 2) == 0 ?
                                Color.black : Color.yellow);
                        g.drawString("" + phasecount, actx + sieben30, stry + vier);
                    }
                } //for

            }
            g.drawImage(diverseCrop[12], actx + scaledFeldSize-sieben, acty, scaledFeldSize, scaledFeldSize, this);
        }

        // paint wall in the west, if any
        if (sf.ww(xpos, ypos).isExisting()) {
            if (sf.ww(xpos, ypos).getEastDeviceType() == Wall.TYPE_LASER) {
                g.drawImage(diverseCrop[16], actx + fuenf, acty, scaledFeldSize, scaledFeldSize, this);
            }
            if (sf.ww(xpos, ypos).getEastDeviceType() == Wall.TYPE_PUSHER) {
                g.drawImage(diverseCrop[9], actx + vier, acty, scaledFeldSize, scaledFeldSize, this);
                // ------------Beschriftung --------------------
                for (int phasecount = 1; phasecount <= 5; phasecount++) {
                    if (sf.ww(xpos, ypos).isEastPusherActive(phasecount)) {
                        int stry = acty + zehn * phasecount;
                        g.setColor((phasecount % 2) == 0 ?
                                Color.black : Color.yellow);
                        g.drawString("" + phasecount, actx + vier20, stry + vier);
                    }
                } //for

            }
            g.drawImage(diverseCrop[12], actx - sieben, acty, scaledFeldSize, scaledFeldSize, this);
        }
    }

    private void paintWaende(Graphics g2) {

        Graphics2D g = (Graphics2D) g2;
        g.setComposite(AC_SRC_OVER);


        // Grenzen des zu zeichnenden Bereichs berechnen:
        Rectangle clip = g.getClipBounds();
        int x64 = scaledFeldSize;
        int x0 = clip.x / x64 + 1;
        int y0 = clip.y / x64 + 1;
        int x1 = (clip.x + clip.width - 1) / x64 + 1;
        int y1 = (clip.y + clip.height - 1) / x64 + 1;
        x1 = Math.min(x1, sf.getSizeX());
        y1 = Math.min(y1, sf.getSizeY());

        // Zeichnen
        for (int hori = x0; hori <= x1; hori++) {
            for (int vert = y0; vert <= y1; vert++) {
                int actx = hori * x64 - x64;
                int acty = vert * x64 - x64;
                int xpos = hori;
                int ypos = sf.getSizeY() - vert + 1;
                paintWall(g2, xpos, ypos, actx, acty);
            }
        }

    }

    private void paintFlaggen(Graphics g2) {

        Graphics2D g = (Graphics2D) g2;
        g.setComposite(AC_SRC_OVER);

        if (sf.getFlags() != null) {
            Location[] flaggen = sf.getFlags();
            for (int flaggencount = 0; flaggencount < flaggen.length; flaggencount++) {
                int xflagge = flaggen[flaggencount].x - 1;
                int yflagge = sf.getSizeY() - flaggen[flaggencount].y;
                g.drawImage(diverseCrop[18 + flaggencount],
                        xflagge * scaledFeldSize, yflagge * scaledFeldSize, scaledFeldSize, scaledFeldSize, this);
            }
        }
    }

    /** Berechnet zu einem Location das Rechteck, das die Kachel umschliesst */
    void ort2Rect(Location ort, Rectangle dest) {
        ort2Rect(ort.x, ort.y, dest);
    }

    void ort2Rect(int x, int y, Rectangle dest) {
        dest.x = (int) ((x - 1) * scaledFeldSize);
        dest.y = (int) ((sf.getSizeY() - y) * scaledFeldSize);
        dest.width = (int) scaledFeldSize;
        dest.height = (int) scaledFeldSize;
    }

    public Point ort2Point(Location ort, Point p) {
        return ort2Point(ort.x, ort.y, p);
    }

    public Location point2Ort(Point p, Location ort) {
        ort.x = (int) (p.x / scaledFeldSize) + 1;
        ort.y = (int) ((getHeight() - p.y) / scaledFeldSize) + 1;
        return ort;
    }

    /** returns left upper point of square*/
    public Point ort2Point(int ortx, int orty, Point p) {
        if (p==null){
            p=new Point();
        }            
        p.x = (int) ((ortx - 1) * scaledFeldSize);
        p.y = (int) ((sf.getSizeY() - orty) * scaledFeldSize);
        return p;
    }
    
    /** returns left upper point of square*/
    public Point ort2Point(int ortx, int orty) {
        return  ort2Point(ortx, orty,null);
    }


    Rectangle rc = new Rectangle();
    // for internal use. see repaintOrt()

    /** Triggert ein Neuzeichnen des Feldes mit den \uFFFDbergebenen
     *  Koordinaten. N\uFFFDtzlich um einzelne Felder neuzeichnen zu lassen
     */

    void repaintOrt(Location ort) {
        ort2Rect(ort, rc);        
        repaint(1, rc.x, rc.y, rc.width, rc.height);
    }

    void repaintOrt(int x, int y) {
        ort2Rect(x, y, rc);
        repaint(1, rc.x, rc.y, rc.width, rc.height);
    }

    void unhighlight() {
        highlightPos.x = 0;
        highlightPos.y = 0;
        repaint();
    }

    private final javax.swing.Timer t = new javax.swing.Timer(5000, new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
            unhighlight();
        }
    });
    
    
    

   
    
    
    void highlight(int x, int y) {
        // remove old highlight:
        repaintOrt(highlightPos);

        if (CAT.isDebugEnabled())
            CAT.debug("highlighting 1 " + x + " " + y);
        highlightPos.x = x;
        highlightPos.y = y;

        //this.paintHighlight((Graphics2D)this.getGraphics());
        if (!t.isRunning())
            t.start();
        else
            t.restart();

        repaintOrt(x, y);
    }


    protected void setAnimationSettings (AnimationConfig current){
        currentAnimationConfig = current;
    }
    
    private void showScout(Location ort) {
        deleteScout();
        repaintOrt(ort);
        lastScoutPos.set(ort);
    }

    private void deleteScout() {
        repaintOrt(lastScoutPos);
    }

    protected void paintScout(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        if (previewRob == null)
            return;

        int xpos = previewRob.getX() - 1;
        int ypos = sf.getSizeY() - previewRob.getY();
        int xpos64 = xpos * scaledFeldSize;
        int ypos64 = ypos * scaledFeldSize;
        // Scout
        AlphaComposite ac = AC_SRC_OVER_07;
        g2d.setComposite(ac);
        g.drawImage(scoutCrop[previewRob.getFacing()], xpos64, ypos64, scaledFeldSize, scaledFeldSize, this);
        g2d.setComposite(AC_SRC);
    }


    private void paintFeldWithElements(Graphics2D g2d, int xpos, int ypos, int actx, int acty) {
        Floor floor = sf.floor(xpos, ypos);
        paintFeldBoden(g2d, xpos, ypos, actx, acty);
        if ((floor.isBelt()) && (floor.getInfo() > 0)) // restore possible Crusher
            paintCrusher(g2d, floor, actx, acty);
        // TODO: only repaint the stuff on the field we want to paint
        g2d.setComposite(AC_SRC_OVER);
        paintWall(g2d, xpos, ypos, actx, acty);
        paintFlaggen(g2d);
    }



    
   

    private void paintRobot(Graphics2D g2d, Bot robot, int robocount) {

        int xpos = robot.getX() - 1;
        int ypos = sf.getSizeY() - robot.getY();
        int xpos64 = xpos * scaledFeldSize;
        int ypos64 = ypos * scaledFeldSize;
        paintRobot(g2d,robot,robocount, xpos64, ypos64);
    }
  
    private void paintRobot (Graphics2D g2d, Bot robot, int robocount, int xpos64, int ypos64) {
        if ( theseBotsShouldNotBePainted.contains(robot.getName())){
            return;
        }
        int acht = (int)(dScale*8);
        
        int botVis = robot.getBotVis();
        Image imgRob = robosCrop[robot.getFacing() + botVis * 4];
        boolean virtuell = robot.isVirtual();
        Composite oldComp = g2d.getComposite();
        if (imgRob != null) {
            if (virtuell) {               
                g2d.setComposite(AC_SRC_OVER_05);
            }
            else {
            	g2d.setComposite(AC_SRC_OVER);
            }
            g2d.drawImage(imgRob, xpos64, ypos64, scaledFeldSize, scaledFeldSize, this);
          // if (virtuell) {
          //      g2d.setComposite(AC_SRC);
          // XXXHS }
            String beschriftung = "" + robot.getName();
            g2d.setColor(ROBOCOLOR[botVis]);
            g2d.drawString(beschriftung, xpos64, ypos64 + acht + robocount * acht);
            g2d.setComposite(oldComp);
        }
    }

    private void paintRobos(Graphics g) {
        paintRobos(g, null);
    }

    private void paintRobos(Graphics g, Bot dontPaintMe) {
        CAT.debug("...painting Robots..");
        Graphics2D g2d = (Graphics2D) g;          
        Iterator it = internalBotHash.values().iterator();
        // roboCounter will be used in paintRobot() as an offset for writing the robot's name on the
        // screen so that the names won't be written over each other if some (virtual) robots
        // have the same position on the board  
        for (int roboCounter=0;it.hasNext();roboCounter++) {
            Bot robot = (Bot) it.next();
            if (!robot.equals(dontPaintMe) && !theseBotsShouldNotBePainted.contains(robot.getName())) {                   
                if ((robot.getDamage() < 10) && (robot.getLivesLeft() > 0)) {
                    paintRobot(g2d, robot, roboCounter);
                }
            }                                  
        }            
    }
    
    

    private final static Stroke[] hi = new Stroke[]{
        new BasicStroke(6), new BasicStroke(4), new BasicStroke(2), new BasicStroke(1)
    };
    private final static Color[] hiColOut = new Color[]{
        Color.red.darker().darker(), Color.red.darker(), Color.red, Color.red.brighter()
    };
    public final Color highCol1 = new Color(255, 0, 0, 255);
    public final Color highCol2 = new Color(255, 255, 0, 128);

    private void paintHighlight(Graphics2D g) {
        Rectangle rc = new Rectangle();
        ort2Rect(highlightPos, rc);
        rc.grow(-3, -3);
        for (int i = 0; i < hi.length; i++) {
            g.setColor(hiColOut[i]);
            g.setStroke(hi[i]);
            g.drawOval(rc.x, rc.y, rc.width, rc.height);
        }

        Paint p = new GradientPaint(rc.x, rc.y, highCol1, rc.x + rc.width, rc.y + rc.height, highCol2);
        g.setPaint(p);
        rc.grow(-1, -1);
        g.fillOval(rc.x, rc.y, rc.width, rc.height);
    }

//     private void createOffscreenImage() {
// 	// XXX vielleicht besser das skalieren erst beim reinkopieren
// 	dbi = createImage(x,y);
// 	g_off = (Graphics2D)dbi.getGraphics();
// 	g_off.setFont(new Font(g_off.getFont().getName(),g_off.getFont().getStyle(),8));
// 	g_off.setClip(0,0,x,y);
// 	g_off.scale( dScale, dScale );
//     }
    //private Graphics2D g_off;


    private BufferedImage offScreenImage;
    private BufferedImage staticBackground;
    
    private BufferedImage createBoardImage() {
        CAT.debug("createBoardImage called!");
        //preBoard = new BufferedImage(x,y, BufferedImage.TYPE_BYTE_INDEXED);
        BufferedImage bi = new BufferedImage(widthInPixel, heightInPixel, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g_off = (Graphics2D) bi.getGraphics();
        g_off.setClip(0, 0, widthInPixel, heightInPixel);
       // g_off.scale(dScale, dScale);
        paintUnbuffered(g_off);
       // g_off.dispose();
        return bi;
    }

    /**
     * Dump this BoardView as a png image file.
     * @param file The file name to dump the image to.
     * @param size The width and hight of the generated image (square).
     *             Use size=0 for keeping the orginal size.
     * @throws IOException is thrown if the file cannot be created.
     */
    public void dumpPngImage(File file, int size) throws IOException {
        FileOutputStream fop = new FileOutputStream(file);
        Image image;
        if (size > 0) {
            image = getThumb(size);
        } else {
            image = createBoardImage();
        }
        fop.write((new PngEncoder(image)).pngEncode());
        fop.flush();
        fop.close();
    }

    /**
     * Dump this BoardView as a png image file.
     * @param file The file name to dump the image to.
     * @throws IOException is thrown if the file cannot be created.
     */
    public void dumpPngImage(File file) throws IOException {
        dumpPngImage(file, 0);
    }


    public void paintComponent(Graphics g) {
        Rectangle oldClip = g.getClipBounds();
        
     //   System.out.println("OLD="+oldClip+"\tNEW="+rect);
        //g.setClip(rect);
        if (useStaticBg) { // 100% doublebuffered
            Graphics2D offG = (Graphics2D) offScreenImage.getGraphics();
            offG.setClip(oldClip);
            offG.drawImage(staticBackground,0,0,widthInPixel,heightInPixel,this);
           // draw the active elements (robos)
            paintHighlight(offG);
            paintScout(offG);
          
            paintRobos(offG);
          
        }
  
        
       //  synchronized (rescaleLock) {
        
       // BufferedImage clip = offScreenImage.getSubimage(oldClip.x, oldClip.y,oldClip.width, oldClip.height);
        g.drawImage(offScreenImage, 0, 0, this);
        
        //}
        // draw the active elements (robos)
        if(!useStaticBg) { // the active elements must not be painted on the offscreen image in this case;
                                 // reason: we need it as a source for "clean" background during animations
            			         // (animations will still be doublebuffered, but we will clean the offscreenimage
                                //   when the animation is finished)
            Graphics2D dbg = (Graphics2D) g;
            paintHighlight(dbg);
            paintScout(dbg);
            paintRobos(dbg);
            dbg.setComposite(AC_SRC);
        }
        
        if (isPhaseNumberToBePainted){
            paintPhaseNumber(g, phaseNumber);
        }
        
        //g.setClip(oldClip);
    }

    
    private static final Color COLOR_PHASE_BG =  new Color(64,181,64);
    private static final Color COLOR_PHASE_FG =  Color.RED;
    private static final Font  FONT_PHASE = new Font("times", Font.BOLD, 90);
 
    private void paintPhaseNumber(Graphics g, int phase){
        Graphics2D dbg = (Graphics2D) g;
        Rectangle rect = this.getVisibleRect();
        int x1 = (int)rect.getCenterX();
        int y1 = (int)rect.getCenterY();
        int stringX = x1 - 240;	
        int stringY= y1;
        int rectTopLeftX = stringX-25;
        int rectTopLeftY = y1 - 90;
        int rectWidth = 490;
        int rectHeight = 110;
        
        dbg.setComposite(AC_SRC_OVER_07);
        dbg.setColor(COLOR_PHASE_BG);
        dbg.fillRect(rectTopLeftX, rectTopLeftY, rectWidth, rectHeight); 
       
        dbg.setComposite(AC_SRC);
        dbg.setColor(Color.black);
        dbg.drawRect(rectTopLeftX, rectTopLeftY, rectWidth, rectHeight); 
        dbg.setColor(COLOR_PHASE_FG);
        dbg.setFont(FONT_PHASE);
        dbg.drawString("PHASE #"+phase,stringX,stringY);
    }
    
    private boolean isPhaseNumberToBePainted=false; 
    private int phaseNumber = 1;
    protected void setIsPhaseNumToBePainted (boolean paintIt){
      //  synchronized (phaseDisplayLock){
            isPhaseNumberToBePainted = paintIt;
       //     phaseDisplayLock.notifyAll();
       // }
    }
    
    protected void setPhaseNumber(int phase){
        phaseNumber = phase;
    }
    
    
    
    protected void paintUnbuffered(Graphics dbg) {
        paintSpielfeldBoden(dbg);
        paintLaserStrahlen(dbg);
        paintWaende(dbg);
        paintFlaggen(dbg);
      //  paintScout(dbg);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        synchronized (this) {
	        if (offScreenImage != null) {
	            Graphics g = offScreenImage.getGraphics();
	            g.dispose();
	        }
	        if (useStaticBg && staticBackground != null) {
	            Graphics g = staticBackground.getGraphics();
	            g.dispose();
	        }
        }
    }

    public void update(Graphics g) {
        paint(g);
    }

    protected Location[] getFlags() {
        return sf.getFlags();
    }


    public Image getThumb(int size) {
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setClip(0, 0, size, size);
        g2.scale(((double) size) / widthInPixel, ((double) size) / heightInPixel);
        paintUnbuffered(g2);
        g2.dispose();

        return bi; 
    }

    private synchronized void ersetzeSpielfeld(SimBoard sfs) {
        sf = sfs;
        widthInPixel = (int) (sf.getSizeX() * scaledFeldSize);
        heightInPixel = (int) (sf.getSizeY() * scaledFeldSize);
        setSize(widthInPixel, heightInPixel);
        initFloorHashMap();
    }

    
    

    // Little helper for getting thumbnails of the board
    private static BoardView sac = null;

    public static Image createThumb(SimBoard sim, int size) {
        if (sac == null) {            
            sac = new BoardView(sim);
        } else {
            sac.ersetzeSpielfeld(sim);
        }
        return sac.getThumb(size);
    }

    
    
    
    //private Object phaseDisplayLock = new Object();
    private void waitForPhaseDisplay(){
     /*
        synchronized (phaseDisplayLock){
            while (isPhaseNumberToBePainted){
                try {
                    phaseDisplayLock.wait();
                }
                catch (InterruptedException ie){
                    CAT.warn(ie.getMessage(), ie);
                }
                phaseDisplayLock.notifyAll();
            }
            
        }*/
    }
    
    private void waitSomeTime(int ms, Object lock){   
        	if (ms == 0){
        	    CAT.warn("waitSomeTime was asked to wait "+ms
        	                    +"ms; ignoring this request as it means to wait infinite time..");
        	    return;
        	}
        	else if (ms <0){
        	    CAT.error("was asked to wait a negative number of milliseconds: "+ms);
        	    return;
        	}
            synchronized(lock) {
                try {
                    lock.wait(ms);
                }
                catch (InterruptedException ie){
                    CAT.error(ie);
                }
            }
         
        /*
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException ie){
            CAT.error("Interrupted while waiting: "+ie.getMessage(), ie);
        }
        */            
    }
    
    
    
}

