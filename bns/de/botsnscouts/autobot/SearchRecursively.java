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

package de.botsnscouts.autobot;

/**
 * SearchRecursively implements the algorithm to recursively look for
 * the best move with a given set of cards.
 *
 * @author Dirk, Lukasz
 * Id: $Id$
 */

import de.botsnscouts.board.BoardRoboter;
import de.botsnscouts.board.SpielfeldKS;
import de.botsnscouts.server.KartenStapel;
import de.botsnscouts.util.Karte;
import de.botsnscouts.util.Roboter;

import java.util.Arrays;

public class SearchRecursively {
    static org.apache.log4j.Category CAT = org.apache.log4j.Category.getInstance(SearchRecursively.class);

    private SpielfeldKS sf;
    private Karte[] bestCards;
    private int bestScore;
    private int malus;

    private static final Karte[] malusCards = {KartenStapel.getRefCard("RL"),
                                               KartenStapel.getRefCard("M1"),
                                               KartenStapel.getRefCard("M2"),
                                               KartenStapel.getRefCard("M3"),
                                               KartenStapel.getRefCard("BU")};
    private static final int[] mali = {25, 15, 15, 15, 10};
    private static final int maliSum = 80;

    public SearchRecursively(SpielfeldKS s, int m) {
        sf = s;
        malus = m;
    }

    public Karte[] findBestMove(Karte[] ka, final Roboter r) {
        int j = 0;
        bestCards = new Karte[5];
        for (int i = 0; i < 5; i++) {
            if (r.getZug(i) == null) {
                bestCards[i] = ka[j++];
            } else {
                bestCards[i] = r.getZug(i);
            }
        }
        if (j == 0) {
            return bestCards;
        }
        bestScore = 1000;
        j = 0;
        while (r.getZug(j) != null) {
            Roboter[] ra = new Roboter[1];
            ra[0] = r;
            sf.doPhase(j + 1, ra);
            j++;
        }

        int len = 0;
        while ((len < 9) && (ka[len] != null))
            len++;
        Arrays.sort(ka, 0, len, Karte.INVERSE_PRIORITY_COMPARATOR);

        recurse((BoardRoboter) r, ka, 0);
        return bestCards;
    }

    /** We need one temp per level of recursion, however we don't want to
     create a new one on each call. */
    private BoardRoboter[] tmp = {new BoardRoboter(), new BoardRoboter(),
                                  new BoardRoboter(), new BoardRoboter(),
                                  new BoardRoboter(), new BoardRoboter()};

    private void recurse(final BoardRoboter r, Karte[] ka, int recursionLevel) {
        if (r.getSchaden() == 10) return;
        int anzahl = 0;
        for (int i = 0; i < 5; i++)
            if (r.getZug(i) != null) anzahl++;
        if (anzahl == 5) {   // end of recursion reached, 5 cards selected
            int diemalus = 0;

            // If we are standing on a conveyor belt, check what cards we need
            // to not die next phase
            if (sf.bo(r.getX(), r.getY()).isBelt()) { // Belt
                for (int i = 0; i < malusCards.length; i++) {
                    tmp[recursionLevel].initFrom(r);
                    tmp[recursionLevel].setZug(0, malusCards[i]);
                    sf.doPhase(1, tmp[recursionLevel]);
                    if (tmp[recursionLevel].getSchaden() == 10)
                        diemalus += mali[i];
                }
                if (diemalus == maliSum)
                    return; // we die surely, discard this choice
            }

            int score = sf.getBewertung(r, malus) + diemalus;
            if (score <= bestScore) {
                bestScore = score;
                for (int i = 0; i < 5; i++)
                    bestCards[i] = r.getZug(i);
            }
            return;
        }

        for (int i = 0; i < 9; i++) {
            if (ka[i] == null)
                continue;
            Karte cardTemp = ka[i];
            ka[i] = null; // play that card
            tmp[recursionLevel].initFrom(r);

            int j = 0;
            while (tmp[recursionLevel].getZug(j) != null) j++;
            tmp[recursionLevel].setZug(j, cardTemp);
            while ((j < 5) && (tmp[recursionLevel].getZug(j) != null)) {
                sf.doPhase(j + 1, tmp[recursionLevel]);
                j++;
            }
            recurse(tmp[recursionLevel], ka, recursionLevel + 1);
            ka[i] = cardTemp;
            // Skip cards with identical action
            while ((i < 8) && ((ka[i + 1] == null) || (ka[i + 1].getaktion().equals(cardTemp.getaktion()))))
                i++;

        }
    }
}
