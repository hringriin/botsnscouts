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

package de.botsnscouts.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Category;
import org.apache.regexp.RE;

import de.botsnscouts.comm.KommException;
import de.botsnscouts.comm.KommServerAusgabe;
import de.botsnscouts.comm.KommServerRoboter;
import de.botsnscouts.start.RegistrationStartListener;
import de.botsnscouts.util.BNSThread;
import de.botsnscouts.util.Bot;
import de.botsnscouts.util.Encoder;
import de.botsnscouts.util.Message;
import de.botsnscouts.util.ShutdownListener;
import de.botsnscouts.util.Shutdownable;
import de.botsnscouts.util.ShutdownableSupport;

/**
 * 
 * Allows the concurrent registration of robots and outputs. Starts a new ServerAnmeldeThread for each registration attempt
 * 
 */
class RegistrationManager implements Runnable, Shutdownable {
    private static final Category CAT = Category.getInstance(RegistrationManager.class);

    static final String REGISTER_PATTERN = "(RGS|RGA|RS2|RA2)\\((([:alnum:]|\\+|\\%|\\_|\\-|\\.|\\*)+)(,([1-8]))?\\)";

    Server server;

    ServerSocket seso;

    private Set<String> names = new HashSet<String>();

    int anzSpieler = 0;

    private boolean isShutDown = false;

    private ShutdownableSupport shutdownSupport = new ShutdownableSupport(this);

    BNSThread workingThread = new BNSThread(this, "RegMan") {
        public void doShutdown() {
            this.shutdown();
        }
    };

    public RegistrationManager(Server s) {
        // super("RegistrationManager");
        server = s;
        CAT.debug("creating new registration manaegr");
    }

    public void beginRegistration() {
        workingThread.start();
    }

    public void endRegistration() {
        CAT.debug("endRegistration() called!");
        workingThread.interrupt();
    }

    public void shutdown(boolean notifyListeners) {
        endRegistration();
        if (seso != null) {
            try {
                seso.close();
            }
            catch (Exception e) {
                CAT.warn(e);
            }
        }
        isShutDown = true;
    }

    public boolean isShutDown() {
        return isShutDown;
    }

    public void addShutdownListener(ShutdownListener l) {
        shutdownSupport.addShutdownListener(l);
    }

    public boolean removeShutdownListener(ShutdownListener l) {
        return shutdownSupport.removeShutdownListener(l);
    }

    public void run() {
        Socket clientSocket = null;
        try {
            CAT.debug("creating server socket");
            seso = new ServerSocket(server.getRegistrationPort());
            seso.setSoTimeout(0);

            // we need to wait a little, so that "seso.accept()" below
            // is up and working; otherwise a quick (local) client still tries
            // to connect too early (yes, that does happen)
            // => notifying from this "dummy" Thread that will wait a few ms
            // before notifying the listeners

            Runnable notifier = new Runnable() {
                Object foo = new Object();

                public void run() {
                    try {
                        synchronized (foo) {
                            foo.wait(200);
                        }
                    }
                    catch (InterruptedException ie) {
                        CAT.warn(ie);
                    }
                    notifyStartListeners();
                }
            };
            Thread dummyNotify = new Thread(notifier);
            dummyNotify.start();

            while (!Thread.interrupted()) {
                clientSocket = seso.accept();
                if (CAT.isDebugEnabled())
                    CAT.debug("new registration from " + clientSocket);
                register(clientSocket);
            }
        }
        catch (IOException io) {
            CAT.warn(io);
        }
        finally {
            try {
                if (seso != null)
                    seso.close();
            }
            catch (IOException io) {
                CAT.warn(io);
            }
            try {
                if (clientSocket != null)
                    clientSocket.close();
            }
            catch (IOException io) {
                CAT.warn(io);
            }
        }
    }

    /** Registriert Namen als benutzt - soll mit isLegalName() benutzt werden */
    private void addName(String s) throws RegistrationException {
        synchronized (names) {
            if (CAT.isDebugEnabled()) {
                CAT.debug("want to add " + s + ", right now we have:");
                for (String ns : names) {
                    CAT.debug(ns);
                }
            }
            if (names.contains(s)) {
                throw new RegistrationException("Name already registered");
            }
            else {
                names.add(s);
            }
        }
    }

    boolean isNameAvailable(String name) {
        synchronized (names) {
            return !names.contains(name);
        }
    }

    /**
     * Wartet server.anmeldeto auf eine Aktion, kreiert ggf. neue ServerRoboterThread- bzw ServerAusgabeThread-Objekte und haengt diese in die
     * richtigen Vektoren ein.
     */

    private synchronized void register(Socket socket) {
        PrintWriter out = null;
        BufferedReader in = null;
        String clientName = "";
        int farbe = -1;

        try {
            CAT.debug("out = new PrintWriter    ...");
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            CAT.debug("in  = new BufferedReader ...");
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            CAT.debug("... we are connected, now check if client sent s.th.");

            socket.setSoTimeout(server.getSignUpTimeout());
            String erhalten = in.readLine();
            if (CAT.isDebugEnabled())
                CAT.debug("erhalten = " + erhalten);

            socket.setSoTimeout(0);

            RE registerRE = new RE(REGISTER_PATTERN);

            if (!registerRE.match(erhalten))
                throw new RegistrationException("Register string not in the right format");

            String type = registerRE.getParen(1);
            clientName = Encoder.commDecode(registerRE.getParen(2));

            if (registerRE.getParenCount() >= 5) {
                // for (int i=0; i<=5;i++)
                // System.out.println(i+"="+registerRE.getParen(i));
                farbe = Integer.parseInt(registerRE.getParen(5));
            }

            if (type.equals("RGS"))
                registerPlayer(clientName, farbe, in, out, 1.0f);
            else
                if (type.equals("RGA"))
                    registerOutput(clientName, in, out, 1.0f);
                else
                    if (type.equals("RS2"))
                        registerPlayer(clientName, farbe, in, out, 2.0f);
                    else
                        if (type.equals("RA2"))
                            registerOutput(clientName, in, out, 2.0f);
                        else {
                            CAT.fatal("should never be here");
                            throw new Error("should never be here");
                        }
        }
        catch (Exception ee) {
            CAT.warn(ee);
            try {
                in.close();
            }
            catch (IOException e) {
                CAT.warn(e);
            }
            out.close();
        }
    }

    private void registerPlayer(String clientName, int farbe, BufferedReader in, PrintWriter out,
                    @SuppressWarnings("unused") float version) throws RegistrationException {
        // darf ein Spieler sich anmelden?
        CAT.debug("A player tries to connect");
        // einfacher?: if(server.gameRunning())
        if (server.isGameStarted()) {
            CAT.debug("No more bot registrations at this point . Killing connection");
            String msg = Message.say("Server", "registrationFailureGameRunning");
            out.println("Error: " + msg);
            // out.println("error ");
            // out.println("REN(SO(SpielLaeuftSchon))");
            throw new RegistrationException("No robot registrations allowed now.");
        }
        CAT.debug("He may - as far as the server is concerned..");

        if (!isNameAvailable(clientName)) {
            String msg = Message.say("Server", "registrationFailureName");
            out.println("Error: " + msg);
            // out.println("error");
            // out.println("REN(SO(nameAlreadyInUse)");
            throw new RegistrationException("Name already registered: " + clientName);
        }
        CAT.debug("The name isn't in use.");
        addName(clientName);

        if (anzSpieler == server.getMaxPlayers()) {
            CAT.debug("Too many players. Killing connection");
            String msg = Message.say("Server", "registrationFailureGameFull");
            out.println("Error: " + msg);
            // out.println("error");
            // out.println("REN(ZS)");
            throw new RegistrationException("Game is full!");
        } // Zuviele Spieler
        CAT.debug("Not yet too many players..");

        farbe = server.allocateColor(farbe, clientName);
        CAT.debug("Assigned color: " + farbe);

        Bot h = Bot.getNewInstance(clientName);
        h.setBotVis(farbe);
        KommServerRoboter komm = new KommServerRoboter(in, out, "ServerComm_" + clientName);
        try {
            komm.anmeldeBestaetigung(true);
        }
        catch (KommException ke) {
            CAT.debug("ok konnte nicht an roboter gesendet werden");
            throw new RegistrationException("Couldn't send OK to robot");
        }
        CAT.debug("ok an Spieler geschickt.");

        anzSpieler++;
        if (CAT.isDebugEnabled())
            CAT.debug("" + anzSpieler + ". Bot mit Name " + clientName + " erzeugt.");

        ServerRoboterThread neu = new ServerRoboterThread(h, server.getOKListener(), server.getInfoRequestAnswerer(),
                        server.getRobThreadMaintainer(), komm);
        server.addRobotThread(neu);
        CAT.debug("ServerRoboterThread erzeugt und einsortiert.");
        server.updateNewBot(clientName, farbe);

        if (anzSpieler >= server.getMaxPlayers()) { // alle
            // da
            try {
                Thread.sleep(5000);
            }
            catch (InterruptedException ex) {
                CAT.debug("InterruptedException " + ex);
            }
            CAT.debug("server: sending start command");
            server.startGame();
        } // if maxspieler angemeldet
    }

    private void registerOutput(String clientName, BufferedReader in, PrintWriter out, float version)
                    throws RegistrationException {
        KommServerAusgabe ksa = new KommServerAusgabe(in, out, "ServerComm_" + clientName);
        // sende 'ok' zur anmeldebestaetigung
        try {
            ksa.anmeldeBestaetigung(true);
        }
        catch (KommException ke) {
            CAT.debug("ok konnte nicht an Ausgabekanal gesendet werden");
            throw new RegistrationException(ke);
        }

        try {
            addName("(" + clientName + ")");
            ServerAusgabeThread neu = new ServerAusgabeThread(ksa, server.getOKListener(), server.getMOKListener(),
                            server.getInfoRequestAnswerer(), server.getOutputThreadMaintainer());
            neu.setVersion(version);
            server.addOutput(neu);
            CAT.debug("neuen Ausgabethread erzeugt");
        }
        catch (RegistrationException re) {
            out.println("REN(SO(SchonAngemeldeterName))");
            throw re;
        }
    }

    private Collection<RegistrationStartListener> startListeners = new LinkedList<RegistrationStartListener>();

    public void addRegStartListener(RegistrationStartListener l) {
        synchronized (startListeners) {
            startListeners.add(l);
        }
    }

    private void notifyStartListeners() {
        synchronized (startListeners) {
            for (RegistrationStartListener listener : startListeners) {
                listener.registrationStarted();
            }
        }
    }
}
