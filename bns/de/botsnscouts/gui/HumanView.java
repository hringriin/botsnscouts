package de.botsnscouts.gui;

import de.botsnscouts.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * view for the human player
 * @author Lukasz Pekacki
 */

public class HumanView extends JPanel implements HumanViewInterface {
    
    private HumanPlayer human;
    private CardLayout panelSwitcher = new CardLayout();
    private JPanel scoutNFriends = new JPanel();
    private RepairRegisters repairRegisters;
    private CardArray cards;
    private RegisterArray registers;

    public HumanView() {
	setLayout(panelSwitcher);
	GetDirection getDir = new GetDirection(new ActionListener(){
		public void actionPerformed(ActionEvent ae) {
		    sendDirection(Integer.parseInt(ae.getActionCommand()));
		}
	    }
					       );
	AgainPowerDown againPowerDown = new AgainPowerDown(new ActionListener(){
		public void actionPerformed(ActionEvent ae) {
		    sendAgainPowerDown(ae.getActionCommand().equals("againpowerdown"));
		}
	    }
							   );
	repairRegisters = new RepairRegisters(new ActionListener(){
		public void actionPerformed(ActionEvent ae) {
		    sendRepairRegisters();
		}
	    });
	
	cards = new CardArray(new ActionListener(){
		public void actionPerformed(ActionEvent cardKlick) {
		    treatCardKlick((CardView) cardKlick.getSource());
		}
	    },
			      new ActionListener(){
				      public void actionPerformed(ActionEvent sendKlick) {
					  treatSendCards();
				      }
				  }
			      );
	
	registers = new RegisterArray(new ActionListener(){
		public void actionPerformed(ActionEvent registerKlick) {
		    treatRegisterKlick((RegisterView) registerKlick.getSource());
		}
	    }

				     );
	UserInfo userInfo = new UserInfo();

	JPanel regsAndCards = new JPanel();

	regsAndCards.add(registers);
	regsAndCards.add(cards);
	add(userInfo,"userInfo");
	add(getDir,"getDirection");
	add(againPowerDown,"againPowerDown");
	add(repairRegisters,"repairRegisters");
	add(regsAndCards,"regsAndCards");
    }

    /**
     * display a message that is shown only to this player
     */
    public void showMessageToPlayer(String s) {}

    /**
     * display info that the robot is power down int this turn
     */
    public void showPowerDown() {}


    /**
     * display the get direction request
     */
    public void showGetDirection() {

    }

    /**
     * display the power down again request
     */
    public void showRePowerDown() {}

    /**
     * display the register repair request
     */
    public void showRegisterRepair() {}

    /**
     * update the position of the scout
     */
    public void showUpdatedScout() {}

    /**
     * activate the scout
     */
    public void activateScout() {}


    /**
     * remove the scout
     */
    public void removeScout() {}

    /**
     * update the position of the knowitalll
     */
    public void showUpdatedKlugScheisser() {}

    /**
     * activate the knowitall
     */
    public void activateKlugScheisser() {}


    /**
     * remove the knowitall
     */
    public void removeKlugScheisser() {}


    /**
     * show game over
     * two types: a) winner + winner no.
     *            b) dead
     */
    public void showGameOver(int typeOfGameOver, int winnerNumber) {}


    /**
     *  exit the programm
     *  eihter by game over or by user request
     */
    public void shutup() {}


    private void sendDirection (int d) {
	human.sendDirection(d);
    }

    private void sendAgainPowerDown (boolean again) {
	// TODO
    }

    private void sendRepairRegisters () {
	ArrayList a = repairRegisters.getSelection();
	// TODO
    }


    private void treatCardKlick (CardView cv) {
	if ( ! registers.allOcupied() ) {
	    registers.addCard(cv.getCard());
	    cv.reset();
	}
    }


    private void treatSendCards () {
	if ( registers.allOcupied()) {
	    // SEND Cards
	}
    }

    private void treatRegisterKlick (RegisterView rv) {
	if ( ! rv.locked()) {
	    cards.addCard(rv.getCard());
	    rv.reset();
	}
    }

}




