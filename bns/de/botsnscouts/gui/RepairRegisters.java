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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.apache.log4j.BasicConfigurator;

import de.botsnscouts.util.Message;
import de.botsnscouts.widgets.GreenTheme;
import de.botsnscouts.widgets.OptionPane;
import de.botsnscouts.widgets.TJLabel;
import de.botsnscouts.widgets.TJPanel;

/**
 * ask the user for the register reparation
 * 
 * @author Lukasz Pekacki
 */
@SuppressWarnings("serial")
public class RepairRegisters extends TJPanel implements ActionListener {
    private JButton done;

    private JLabel title;

    private int toAssign = 0;

    //
    // private int repairPoints = 0;

    private ArrayList<RegisterView> registers;

    private boolean[] locked;

    private Box left = new Box(BoxLayout.Y_AXIS);

    public RepairRegisters(ActionListener doneListener) {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        title = new TJLabel();
        done = OptionPane.getTransparentButton(Message.say("SpielerMensch", "ok"), 14);
        done.setEnabled(false);
        done.addActionListener(doneListener);
        Box right = new Box(BoxLayout.Y_AXIS);
        Box buttonBox = new Box(BoxLayout.X_AXIS);
        Box main = new Box(BoxLayout.X_AXIS);
        main.add(left);
        main.add(right);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(done);
        right.add(buttonBox);
        right.add(Box.createVerticalGlue());
        add(title);
        add(main);
    }

    public void setChoices(ArrayList<RegisterView> registers, int repairPoints) {
        this.toAssign = repairPoints;
        ArrayList<RegisterView> newRegisters = new ArrayList<RegisterView>(5);
        // resetAll();

        title.setText(Message.say("SpielerMensch", "mregwahl", repairPoints));
        locked = new boolean[registers.size()];

        left.removeAll();
        for (int i = 0; i < registers.size(); i++) {
            RegisterView rv = registers.get(i);
            boolean isLocked = rv.locked();
            String cardAction = rv.getCardAction();
            int cardPrio = rv.getPrio();
            rv = new RegisterView(this);
            rv.setCard(new HumanCard(cardPrio, cardAction));
            rv.setLocked(isLocked);
            newRegisters.add(rv);
            left.add(rv);
            rv.setActionCommand("" + i);
            locked[i] = isLocked;

        }
        this.registers = newRegisters;
    }

    /**
     * 
     * @return A list of Integers, containing the number(s) of the register(s)
     *         to repair; the first register has number 1 ( not 0)
     */
    public ArrayList<Integer> getSelection() {
        int cntr = 0;
        boolean[] torepair = new boolean[5];
        for (int i = 0; i < registers.size(); i++) {
            torepair[i] = locked[i] && (!(registers.get(i).locked()));
            if (torepair[i]) {
                cntr++;
            }
        }
        ArrayList<Integer> lst = new ArrayList<Integer>(cntr);
        for (int i = 0; i < registers.size(); i++) {
            if (torepair[i]) {
                lst.add(new Integer(i + 1));
            }
        }
        return lst;
    }

    public void actionPerformed(ActionEvent e) {
        int num = Integer.parseInt(e.getActionCommand());
        RegisterView rv = ((RegisterView) e.getSource());
        if (rv.locked() && (toAssign > 0)) {
            rv.setLocked(false);
            toAssign--;
            done.setEnabled(toAssign <= 0);
            title.setText(Message.say("SpielerMensch", "mregwahl", toAssign));
        }
        else
            if ((!rv.locked()) && locked[num]) {
                rv.setLocked(true);
                toAssign++;
                done.setEnabled(toAssign <= 0);
                title.setText(Message.say("SpielerMensch", "mregwahl", toAssign));
            }
    }

    public static void main(String args[]) {
        BasicConfigurator.configure();
        Message.setLanguage("deutsch");
        JFrame f = new JFrame("testing RepairRegisters");
        MetalLookAndFeel.setCurrentTheme(new GreenTheme());

        RepairRegisters re = new RepairRegisters(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Send.");

            }
        });

        ArrayList<RegisterView> ra = new ArrayList<RegisterView>(5);
        RegisterView rv;
        for (int i = 0; i < 5; i++) {
            rv = new RegisterView(re);
            rv.setCard(new HumanCard(100 + i, "M2"));
            ra.add(rv);
        }

        ra.get(2).setLocked(true);
        ra.get(0).setLocked(true);
        ra.get(4).setLocked(true);

        re.setChoices(ra, 2);
        f.getContentPane().add(re);
        f.pack();
        f.setLocation(100, 100);

        f.setVisible(true);
    }

}
