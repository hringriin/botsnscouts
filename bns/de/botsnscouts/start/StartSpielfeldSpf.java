package de.botsnscouts.start;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.border.*;
import java.net.*;
import de.botsnscouts.util.*;
import de.botsnscouts.board.*;

public class StartSpielfeldSpf extends JPanel{
    Start parent;
    StartKachelComp[][] kachP;
    StSpFassade fassade;

    public StartSpielfeldSpf(Start par){
	parent=par;
	fassade=par.fassade;
	Ort spfDim=parent.fassade.getSpielfeldDim();
	GridLayout lay=new GridLayout(spfDim.y,spfDim.x);
	lay.setHgap(0);
	lay.setVgap(0);
	setLayout(lay);
	setOpaque(false);
	//setBorder(new EmptyBorder(50,50,50,50));

	kachP=new StartKachelComp[spfDim.x][spfDim.y];
	//initialisiere Panels f�r jede Kachel
	for (int j=spfDim.y-1;j>=0;j--){
	    for (int i=0;i<spfDim.x;i++){
		kachP[i][j]=new StartKachelComp(par.fassade,i,j);
		add(kachP[i][j]);
	    }
	}
    }

    public void rasterChanged(){
	for (int i=0;i<kachP.length;i++){
	    for (int j=0;j<kachP[0].length;j++){
		kachP[i][j].rasterChanged();
	    }
	}
    }

    public void addKachelClickListener(KachelClickListener kachelClickL){
	for (int i=0;i<kachP.length;i++){
	    for (int j=0;j<kachP[0].length;j++){
		kachP[i][j].addKachelClickListener(kachelClickL);
	    }
	}

    }

    public void removeKachelClickListener(){
	for (int i=0;i<kachP.length;i++){
	    for (int j=0;j<kachP[0].length;j++){
		kachP[i][j].removeKachelClickListener();
	    }
	}

    }

}
