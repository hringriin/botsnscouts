package de.botsnscouts.start;

import java.awt.*;
import de.botsnscouts.board.*;
import de.botsnscouts.gui.*;
import de.botsnscouts.util.*;

//saves image and name of tiles
//gives rotated image

public class Kachel extends SpielfeldSim{

    Image img;
    String kName;
    int rotat;

    //create new tile
    public Kachel(String name, String field, int GR) throws FormatException, FlaggenException{
	super(12,12,field,null);
	kName=name;
	rotat=0;
	img=SACanvas.createThumb(this,GR);
    }

    //create rotated tile
    public Kachel(String name, String field, int rot, Image im) throws FormatException, FlaggenException{
	super(12,12,field,null);
	kName=name;
	rotat=rot;
	img=im;
    }

    public Image getImage(){
	return img;
    }

    public String getName(){
	return kName;
    }

    public int getRotation(){
	return rotat;
    }

    //gibt um 90� gedrehtes Clone
    public Kachel getGedreht(){
	String gedrKachel=get90GradGedreht();
	Kachel drKachel = null;
	try{
	    drKachel =new Kachel(kName,gedrKachel,(rotat+1)%4,img);
	}catch(FlaggenException e){
	    System.err.println(e);
	}catch(FormatException e){
	    System.err.println(e);
	}
	return drKachel;
    }

    //pr�ft ob Flagge(n) g�ltige Position hat(haben)
    public boolean testFlagge(Ort[] fl){
	String kach=getComputedString();
	SpielfeldSim test;
	try{
	    // enno: habe checkflaggen protected gemacht
	    checkFlaggen(fl);
	    //test= new SpielfeldSim(12,12,kach,fl);
	}catch(FlaggenException e){
	    return false;
	}
	/*
	catch(FormatException e){
	    System.err.println(e);
	}
	*/
	return true;
    }

}
