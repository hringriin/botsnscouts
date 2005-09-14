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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Category;

/** Wait for some roboters or views for some reason.
 *  Constructing a WaitingForSet blocks the current Thread until all
 *  elements of the set tell they are ready (by calling remove())
 */
 class WaitingForSet {
      static Category CAT = Category.getInstance(WaitingForSet.class);
     private Vector waitingFor;

     /** Constructs the set of elements to wait for.
      */
     WaitingForSet(Collection c){
	 waitingFor = new Vector(c);
     }

     /** Blocks until the thing we waited for is done.
      *  @param timeout Timeout in ms.
      *  @return Iterator to the ones who did not make it within the timeout.
      */
     synchronized Iterator waitFor(int timeout){
	 if (waitingFor.isEmpty()){
	     return waitingFor.iterator();
	 }
	 try{
	     wait(timeout);
	 }catch (InterruptedException e){
		 d("InterruptedException, shouldn't happen: "+e.getMessage());
	 }
	 return waitingFor.iterator();
     }


     /** Removes one object we waited for from the set.
      * @throws RuntimeException if we didn't wait for that element.
      */
     synchronized void removeAndNotify(Waitable w){
        CAT.debug("remove and notify!");
		 if (waitingFor.remove(w)) {
		     CAT.debug("fire removal event");
	         fireRemovalEvent(w);	
	     }
		 CAT.debug("before empty check");
		 if (waitingFor.isEmpty()){
	            CAT.debug("waitingFor is empty/notify");
		     notify();
		 }
		 CAT.debug("leaving remove and notify");
     }

     boolean isEmpty(){ return waitingFor.isEmpty(); }
     Iterator iterator(){ return waitingFor.iterator(); }
     synchronized void remove(Waitable w){ waitingFor.remove(w); }
     int size() { return waitingFor.size(); }
     synchronized Waitable getElement(){
	 if (waitingFor.size()!=1)
	     throw new RuntimeException("Precondition: only one element left violated.");
	 return (Waitable)waitingFor.get(0);
     }

     // Event-throwing
     Vector listeners=new Vector();
     public void addRemovalListener(RemovalListener l){
	 synchronized (listeners){
	     listeners.add(l);
	 }
     }
     public void removeRemovalListener(RemovalListener l){
	 synchronized (listeners){
	     listeners.remove(l);
	 }
     }
     private void fireRemovalEvent(Waitable removed){
	 synchronized (listeners){
	     for (Iterator it=listeners.iterator();it.hasNext();)
		 ((RemovalListener)it.next()).waitableRemoved(removed);
	 }
     }

     private void d(String s){
	 de.botsnscouts.util.Global.debug(this,s);
     }
 }
