package de.botsnscouts.gui;

import  java.util.*;
import  de.botsnscouts.util.*;
import  de.botsnscouts.autobot.*;
import  de.botsnscouts.board.*;
import  de.botsnscouts.server.*;

/**
 * Wisenheimer
 * 
 */
public class Wisenheimer{

    private SpielfeldKS aiBoard;
    private Permu wirbel;
    private Roboter simRob = Roboter.getNewInstance("dummy");
    // damit wir nicht immer f�r jede Simulation einen neuen Robby brauchen

    public Wisenheimer (SpielfeldKS aiB) {
	aiBoard=aiB;
	wirbel = new Permu(aiBoard,0);
    }

    protected int getPrediction(ArrayList registers, ArrayList cards, Roboter robi){
	simRob.copyRob(robi);

	Karte[] simCards=new HumanCard[9];
	// cards in simCards einlesen, gesperrte Karten werden nicht ber�cksichtigt
	int j=0;
	for (int i = 0; i < cards.size(); i++) {
	    if(((HumanCard)cards.get(i)).getState() == HumanCard.FREE) {
		simCards[j] = new HumanCard((HumanCard)cards.get(i));
		j++;
	    }
	}
	// gelegte Karten in das enstprechende gesperrte Register des Robis packen
	for (int l = 0; l<registers.size();l++) {
	    if (!((HumanCard)registers.get(l)).free()) {
		simRob.sperreRegister(l,  KartenStapel.get(((HumanCard)registers.get(l)).getprio(),((HumanCard)registers.get(l)).getaktion()));
	    }
	}
	// gesperrte Register in simRob.zug schreiben
	for (int i = 0; i < simRob.getGesperrteRegister().length; i++){
	    simRob.setZug(i, simRob.getGesperrteRegister(i));
	}
	Karte[] vonPermut = wirbel.permutiere(simCards, simRob);

	// in vonPermut steht die vorgeschlagene Registerprogrammierung, so wie sie ggf. z.T.schon
	// in den Registern steht
	int nextprio =-1;
	for (int su=0; su<5;su++){
	    if (((HumanCard)registers.get(su)).getprio() != (vonPermut[su].getprio())){
                nextprio = vonPermut[su].getprio();
                break;
            }
	}
	int ind=0;
	for (int i=0; i<cards.size();i++){
            if (((HumanCard)cards.get(i)).getprio() == nextprio) {
		ind=i;
		break;
	    }
	}
	return ind;
    }
    
}
