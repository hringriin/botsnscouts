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

package de.botsnscouts.widgets;

import javax.swing.*;
import java.awt.*;

/** Transparent JLabel */
public class TJLabel extends JLabel {

    private static Font font = new Font("Sans", Font.BOLD, 12);


    public TJLabel() {
        setOpaque(false);
        setFont(font);
    }

    public TJLabel(String text) {
        super(text);
        setOpaque(false);
        setFont(font);
    }

    public TJLabel(String text, int align) {
        super(text, align);
        setOpaque(false);
        setFont(font);

    }

    public TJLabel(Icon icon) {
        super(icon);
        setOpaque(false);
        setFont(font);

    }

    public TJLabel(Icon icon, int align) {
        super(icon, align);
        setOpaque(false);
        setFont(font);
    }
}