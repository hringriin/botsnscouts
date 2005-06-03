package de.botsnscouts.gui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Category;

import de.botsnscouts.comm.ClientAntwort;


public class MessageSequencer {
    private static Category CAT = Category.getInstance(MessageSequencer.class);

    private int nextMsg;
    private TreeSet messages = new TreeSet();
    private HashMap eventToActionMap = new HashMap();
    private static final int LATESTART = -1;

    public MessageSequencer(boolean lateStart) {
        nextMsg = lateStart ? LATESTART : 1;
    }

    public synchronized void clear(){
        messages.clear();
        eventToActionMap.clear();
    }
    
    public synchronized void addActionMapping(String messageEventID, AbstractMessageAction action) {
        eventToActionMap.put(messageEventID, action);
    }

    public synchronized void invoke(String useThisIDString, ClientAntwort messageData) {
        int msgNum = messageData.messageSequenceNumber;

        if (msgNum < 0) { // message not numbered, invoking immediately
            String actionType;
            if (useThisIDString == null)
                actionType = messageData.namen[0];
            else
                actionType = useThisIDString;
            AbstractMessageAction action = (AbstractMessageAction) eventToActionMap.get(actionType);
            action.invoke(messageData);
            // invokeAsLongAsPossible();
        } else {
            // store the special String for later use in our special container
            if (useThisIDString != null)
                messageData.specialMessageId = useThisIDString;
            if (msgNum < nextMsg) {
                // old message; can only happen on lateInit. Drop Message.
                CAT.debug("dropping earlier message");
                return;
            }
            messages.add(messageData);

            if (CAT.isDebugEnabled()) {
                CAT.debug("before invokeAsLongAsPossible():");
                dump();
                invokeAsLongAsPossible();
                CAT.debug("after invokeAsLongAsPossible():");
                dump();
            } else {
                invokeAsLongAsPossible();
            }
        }
    }

    public synchronized void invoke(ClientAntwort messageData) {
        invoke(null, messageData);
    }

    /**
     * Invokes actions (ordered by their sequence number) until there is no
     * uninvoked action/message left or we detect that the next message in
     * the sequence is missing
     */
    private void invokeAsLongAsPossible() {
        if (messages.isEmpty())
            return;

        ClientAntwort smallest = (ClientAntwort) messages.first();
        int currentId = smallest.messageSequenceNumber;
        if (nextMsg == LATESTART) {
            nextMsg = currentId;
        }

        while (currentId == nextMsg) {
            CAT.debug("INVOKING MESSAGEACTION #" + currentId);
            String actionType = smallest.specialMessageId;
            if (actionType == null)
                actionType = smallest.namen[0];

            AbstractMessageAction action = (AbstractMessageAction) eventToActionMap.get(actionType);
            if (action != null)
                action.invoke(smallest);
            ++nextMsg;
            messages.remove(smallest);
            if (!messages.isEmpty()) {
                smallest = (ClientAntwort) messages.first();
                currentId = smallest.messageSequenceNumber;
            }
            CAT.debug("actual=" + currentId + "\tnext=" + nextMsg);
            // else nextMsg!=currentId -> exiting loop
        }
    }

    private synchronized void dump() {
        CAT.debug("nextMesg=" + nextMsg);
        CAT.debug("ids: ");
        StringBuffer sb = new StringBuffer();
        Iterator it = messages.iterator();
        while (it.hasNext())
            sb.append(((ClientAntwort) it.next()).messageSequenceNumber).append(", ");
        CAT.debug(sb.toString());
    }
}