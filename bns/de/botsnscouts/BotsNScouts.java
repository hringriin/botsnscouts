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

package de.botsnscouts;

import org.apache.log4j.*;
import de.botsnscouts.gui.Splash;

public class BotsNScouts {
    public static Category CAT;

    public static void main(String[] args) throws Throwable {
        try {
            Splash splash = new Splash();
            splash.showSplash();

            CAT = Category.getInstance(BotsNScouts.class);

            PropertyConfigurator.configure(BotsNScouts.class.getResource("conf/log4j.conf"));
            CAT.debug("Starting app");
            CAT.debug("User.dir: " + System.getProperty("user.dir"));
            de.botsnscouts.start.Start.main(args, splash);
        } catch (Throwable t) {
            CAT.fatal("Exception:", t);
            throw t;
        }

    }
}
