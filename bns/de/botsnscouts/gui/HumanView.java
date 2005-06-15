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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.log4j.Category;

import de.botsnscouts.util.Bot;
import de.botsnscouts.util.Card;
import de.botsnscouts.util.Global;
import de.botsnscouts.util.Message;
import de.botsnscouts.widgets.OptionPane;
import de.botsnscouts.widgets.PaintPanel;
import de.botsnscouts.widgets.TJPanel;

/**
 * view for the human player
 * 
 * @author Lukasz Pekacki
 */

public class HumanView extends JPanel  {

    static Category CAT = Category.getInstance(HumanView.class);

    private HumanPlayer human;

    private CardLayout panelSwitcher = new CardLayout();

    private RepairRegisters repairRegisters;

   private UserInfo userInfo;

    private PhaseEvaluationPanel phaseInfo; 
    
    private CardArray cards;

    private RegisterArray registers;

    ScoutVertiefung scoutView;

    KlugscheisserLatte wisenheimerView;

    private JPanel switcherPanel;

    private JPanel wiseAndScout;

    private ZielfahneErreicht gameOverPanel = null;
    
    public HumanView(HumanPlayer hp) {
        human = hp;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        switcherPanel = new PaintPanel(OptionPane.getBackgroundPaint(this), true); 
        JPanel regsAndCards = new TJPanel();
        wiseAndScout = new TJPanel();

        switcherPanel.setLayout(panelSwitcher);

        GetDirection getDir = new GetDirection(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int direct = Integer.parseInt(ae.getActionCommand());
                Global.debug(this, "I have choosen direction: " + direct);
                sendDirection(direct);
                setDialogInSidebarActive(false);
            }
        });
        AgainPowerDown againPowerDown = new AgainPowerDown(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                CAT.debug("The user clicked: " + ae.getActionCommand());
                sendAgainPowerDown(ae.getActionCommand().equals("powerdownagain"));
                setDialogInSidebarActive(false);
            }
        });
        gameOverPanel = new ZielfahneErreicht();
        //ZielfahneErreicht reachedEndDead = new ZielfahneErreicht(Message.say("SpielerMensch", "mkilled"), true);
        //ZielfahneErreicht reachedEndWinner = new ZielfahneErreicht(Message.say("SpielerMensch", "mflagreached"), false);

        wisenheimerView = new KlugscheisserLatte(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (((KlugscheisserLatte) e.getSource()).selected()) {
                    //			d(this,"wisenheimer, wake up!");
                    if (human.mode == HumanPlayer.MODE_PROGRAM) {
                        klugscheisserClicked(true);
                        human.sendWisenheimerMsg();
                    }
                    else
                        CAT.debug("asked wisenheimer but it did not make sense at this moment");
                }
                else {
                    //			d(this,"wisenheimer, go home!");
                    klugscheisserClicked(false);
                }
            }
        });

        scoutView = new ScoutVertiefung(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (((ScoutVertiefung) e.getSource()).isSelected()) {
                    // d("Scout, wake up!");
                    scoutClicked(true);
                }
                else {
                    // d("Scout, go home!");
                    scoutClicked(false);
                }
            }
        });

        repairRegisters = new RepairRegisters(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendRepairRegisters();
                setDialogInSidebarActive(false);
            }
        });

        cards = new CardArray(new ActionListener() {
            public void actionPerformed(ActionEvent cardKlick) {
                treatCardKlick((CardView) cardKlick.getSource());
            }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent sendKlick) {
                treatSendCards();
                setDialogInSidebarActive(false);
            }
        });

        registers = new RegisterArray(new ActionListener() {
            public void actionPerformed(ActionEvent registerKlick) {
                treatRegisterKlick((RegisterView) registerKlick.getSource());
            }
        }

        );
        /*
        JButton emergencyButton = new JButton(Message.say("SpielerMensch","forceCardDisplayButton"));
		emergencyButton.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
		        ArrayList oldCards = cards.getCards();
		        
	            int cartCount = oldCards==null?0:oldCards.size();
	            for (int i=0;i<cartCount;i++){
	                if (oldCards.get(i)==null) { // got no cards
	                    return; // otherwise NullPointerException in "showCards"
	                }
	            }
		        
		        showCards(oldCards);
		    }
		});*/
        userInfo = new UserInfo(/*emergencyButton*/);

        phaseInfo = new PhaseEvaluationPanel();
		wiseAndScout.add(wisenheimerView);
        wiseAndScout.add(scoutView);

        regsAndCards.add(registers);
        regsAndCards.add(cards);
        switcherPanel.add(userInfo, "userInfo");
        switcherPanel.add(phaseInfo, "phaseInfo");
        
        switcherPanel.add(getDir, "getDirection");
        switcherPanel.add(againPowerDown, "againPowerDown");
        switcherPanel.add(repairRegisters, "repairRegisters");
        switcherPanel.add(regsAndCards, "regsAndCards");
        //switcherPanel.add(reachedEndDead, "reachedEndDead");
       // switcherPanel.add(reachedEndWinner, "reachedEndWinner");
        switcherPanel.add(gameOverPanel, "reachedEnd");
        //add(wiseAndScout);
        add(switcherPanel);
        
       
    }

    protected JPanel getWiseAndScoutPanel() {
        return wiseAndScout;

    }

    /**
     * display a message that is shown only to this player
     */
    public synchronized void showMessageToPlayer(String s) {
        if (isDialogInSidebarActive()) { /* don't switch :-) */
            return;
        }
        userInfo.setInfo(s);
        panelSwitcher.show(switcherPanel,nonDialogPanel);
        this.requestFocus();
    }

    protected void fillPhaseInfoPanel(Bot [] robs, ScalableRegisterRow[]registerRows){
        phaseInfo.setContents(robs, registerRows);
        
    }
    
    /** Tell PhaseInfoPanel to cover all cards (show their backsides)     
     */
    protected void hidePhaseInfoCards() {
        phaseInfo.hideAll(true);
        
    }
    
    
    /**
     * display the card
     */
    public synchronized void showCards(ArrayList humanCards) {
        CAT.debug("Show cards");
        cards.setCards(humanCards);
        panelSwitcher.show(switcherPanel, "regsAndCards");
        if (registers.allLocked()) {
            CAT.debug("All Registes locked!");
            cards.activateButton();
        }
       this.requestFocus();
        setDialogInSidebarActive(true);
    }

    /**
     * display info that the robot is power down int this turn
     */
    public void showPowerDown() {
        showMessageToPlayer(Message.say("SpielerMensch", "istPowerDown"));
         this.requestFocus();
    }

    private boolean dialogInSidebarActive = false;

    /**
     * display the get direction request
     */
    public synchronized void showGetDirection() {
        panelSwitcher.show(switcherPanel, "getDirection");
        this.requestFocus();
        setDialogInSidebarActive(true);
    }

    /**
     * display the power down again request f
     */

    public synchronized void showRePowerDown() {
        panelSwitcher.show(switcherPanel, "againPowerDown");
        this.requestFocus();
        setDialogInSidebarActive(true);
    }

    /**
     * display the register repair request
     */
    public synchronized void showRegisterRepair(Card[] robRegs, int repairNumber) {
        CAT.debug("Show Register Repair");
        registers.updateRegisters(robRegs);
        repairRegisters.setChoises(registers.getRegisterViewArray(), repairNumber);
        panelSwitcher.show(switcherPanel, "repairRegisters");
        this.requestFocus();
        setDialogInSidebarActive(true);
    }

    /**
     * update the position of the scout
     */
    public void showUpdatedScout() {
        if (scoutView.selected()) {
            Bot[] doPhaseRob = new Bot[1];
            doPhaseRob[0] = Bot.getCopy(human.getRob());
            d("got a copy of my robot: " + doPhaseRob[0]);
            ArrayList alreadyChosen = registers.getAlreadyChosen();
            d("already chosen registers:" + alreadyChosen);
            for (int m = 0; m < alreadyChosen.size(); m++) {
                doPhaseRob[0].setMove(m, ((HumanCard) alreadyChosen.get(m)));
            }
            doPhaseRob[0].debug();
            human.passUpdatedScout(alreadyChosen.size(), doPhaseRob);
        }
    }

    /**
     * activate the scout
     */
    public void activateScout() {
    }

    /**
     * remove the scout
     */
    public void removeScout() {
        Bot[] doPhaseRob = new Bot[1];
        doPhaseRob[0] = Bot.getCopy(human.getRob());
        doPhaseRob[0].debug();
        human.passUpdatedScout(0, doPhaseRob);
    }

    /**
     * update the position of the wisenheimer
     */
    public void showUpdatedWisenheimer(boolean predicted) {
        int predCard = -1;
        if ((wisenheimerView.selected()) && (human.mode == HumanPlayer.MODE_PROGRAM)) {
            if (predicted) {
                predCard = human.getNextPrediction(registers.getWisenheimerCards(), cards.getCards());
            }
            else {
                predCard = human.getPrediction(registers.getWisenheimerCards(), cards.getCards());
            }
            if (predCard == -1) {
                wisenheimerView.setSelected(false);
            }

        }
        cards.setWisenheimer(predCard);
    }


    /**
     * Remove the Wisenheimer from the cards he might be sitting on
     */
    public void removeWisenheimer(){
        cards.removeWisenheimer();
    }

    /**
     * show game over two types: a) winner + winner no. b) dead
     * @param lockPanel force the switcherPanel to not leave the dialog mode 
     *               (==showing the winnerpanel until the player quits, as he was removed and
     *                 won't get another dialog) 
     */
    public synchronized void showGameOver(boolean dead, int winnerNumber, String removalReason, boolean lockPanel) {        
        String bigVerticalMessage=dead?Message.say("SpielerMensch", "mkilled"):Message.say("SpielerMensch", "mflagreached");       
        gameOverPanel.setMessage(bigVerticalMessage,dead,removalReason);
        panelSwitcher.show(switcherPanel, "reachedEnd");      
        // little not-so-nice hack:
        // we are not a dialog but pretend to be one, so nobody immediately replaces the reachedEnd-Panel        
        //  The Runnable/Thread only ensures that we are show at least 8 seconds 
        setDialogInSidebarActive(true);
        if (!lockPanel) { 
	         Runnable foo = new Runnable(){
	             public void run(){                 
	                 synchronized (this){
	                     try {
	                         wait (8000);
	                     }
	                     catch (InterruptedException ie){
	                         CAT.error(ie.getMessage(), ie);
	                     }
	                     setDialogInSidebarActive(false);
	                 }
	             }
	         };
	         Thread t = new Thread(foo);
	         t.start();
        }
        //}
    }

    
    protected void updateRegisters(Card[] robRegs) {
        registers.updateRegisters(robRegs);
        CAT.debug("Updating Registers...");
        registers.resetAll();
    }

    protected void sendDirection(int d) {
        human.sendDirection(d);
        showMessageToPlayer(Message.say("SpielerMensch", "mkartenMisch"));
    }

    protected void sendAgainPowerDown(boolean again) {
        if (!again) {
            showMessageToPlayer(Message.say("SpielerMensch", "mkartenMisch"));
        }
        else {
            showPowerDown();
        }
        human.sendAgainPowerDown(again);
    }

    private void sendRepairRegisters(){
        sendRepairRegisters(repairRegisters.getSelection());
    }
    
    /**
     * 
     * @param regsRep A list of Integers, containing the number(s) of the register(s)
     *              to repair; register numbers start with 1 (not 0)
     */
    protected void sendRepairRegisters(ArrayList regsRep) {
        CAT.debug("sendRepairRegisters");
        showMessageToPlayer(Message.say("SpielerMensch", "mkartenMisch"));
        unlockRegisters(regsRep);
        human.sendRepair(regsRep);
    }

    private void unlockRegisters(ArrayList repairRegs) {
        CAT.debug("Die Register vor dem unlock: " + registers.toString());
        for (int i = 0; i < repairRegs.size(); i++) {
            int regNum = ((Integer) repairRegs.get(i)).intValue();
            CAT.debug("Entsperre Register: " +regNum );
            registers.unlockRegister(regNum - 1);
        }

        CAT.debug("Die Register nach dem unlock: " + registers.toString());
        registers.resetAll();

    }

    private void treatCardKlick(CardView cv) {
        boolean predicted = false;
        if ((cv.getCard() != null) && (!registers.allOcupied())) {
            registers.addCard(cv.getCard());
            predicted = cv.isWisenheimer();
            cv.reset();
            if (registers.allOcupied()) {
                cards.activateButton();
            }
        }
        showUpdatedScout();
        showUpdatedWisenheimer(predicted);
    }

    private void treatSendCards() {
        if (registers.allOcupied()) {
            showMessageToPlayer(Message.say("SpielerMensch", "mkartenMisch"));
            d("send to Server: " + registers.getCards() + " powerDown: " + cards.wishesPowerDown());
            human.sendCards(registers.getCards(), cards.wishesPowerDown());
            if (cards.wishesPowerDown()) {
                showPowerDown();
            }
            setNonDialogToPhaseInfo();
            cards.resetAll();
        }
        wisenheimerView.setSelected(false);
    }

    private void treatRegisterKlick(RegisterView rv) {
        if ((rv.getCard() != null) && (!rv.locked())) {
            rv.getCard().setState(HumanCard.FREE);
            cards.addCard(rv.getCard());
            rv.reset();
        }
        if (!registers.allOcupied()) {
            cards.deActivateButton();
        }
        showUpdatedScout();
        showUpdatedWisenheimer(false);
    }

    private void klugscheisserClicked(boolean awake) {
        //if ((awake) && ( human.mode == HumanPlayer.MODE_PROGRAM)){
        if (human.mode == HumanPlayer.MODE_PROGRAM) {
            showUpdatedWisenheimer(false);
        }
    }

    private void scoutClicked(boolean awake) {
        if (awake) {
            showUpdatedScout();
        }
        else {
            removeScout();
        }
    }

    public void sendChatMessage(String msg) {
        if (msg != null && msg.trim().length() != 0) {
            human.sendChat(msg);
        }
    }

    protected void quitHumanPlayer(boolean joinHPThread) {
        CAT.debug("HumanView asks the HumanPLayer to quit..");        
        human.shutdown();
        if (joinHPThread){
            try {
                human.join(3000);
            }
            catch (InterruptedException dontCare){
                CAT.warn(dontCare);
            }
        }
    }

    private void d(String s) {
        Global.debug(this, s);
    }

    public HumanPlayer getHumanPlayer() {
        return human;
    }

    private synchronized boolean isDialogInSidebarActive() {
        return dialogInSidebarActive;
    }
    
    private String nonDialogPanel="userInfo";
    
    protected void setNonDialogToUserInfo(){
        nonDialogPanel = "userInfo";
    }
    
    protected void setNonDialogToPhaseInfo(){
        nonDialogPanel ="phaseInfo";
    }

    protected synchronized void setDialogInSidebarActive(boolean dialogInSidebarActive) {
        this.dialogInSidebarActive = dialogInSidebarActive;
        if (!dialogInSidebarActive) {
            userInfo.setInfo(Message.say("SpielerMensch", "mrelax"));
            panelSwitcher.show(switcherPanel, nonDialogPanel);
        }
    }
   public Dimension getPreferredSize () {
        return new Dimension(150,550);
    }/*
   public Dimension getMinimumSize () {
       return new Dimension(150,550);
   }
   */
}

