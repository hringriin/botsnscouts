package de.botsnscouts.gui.hotkey;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import de.botsnscouts.util.Conf;

import org.apache.log4j.Category;

public class HotKeyMan {
  static Category CAT = Category.getInstance(HotKeyMan.class);

  private HashMap hotkeys;
  private HashMap keysByName;

  public HotKeyMan() {
    hotkeys = new HashMap();
    keysByName = new HashMap();
  }


  public synchronized void setHotKey (String keyName, HotKeyAction action){
    CAT.debug("setting HotKey for: "+keyName);
    setHotKey(new HotKey(keyName, action));
  }


  public synchronized void setHotKey(HotKey key) {
    CAT.debug("setting hotkey: "+key.toString());
    hotkeys.put(key.getKeyCodeI(), key);
    keysByName.put(key.getName(), key);
    HotKeyConf.setKeyCode(key.getName(), key.getKeyCodeI());
  }


  public synchronized void invoke (int keyCode) {
    CAT.debug("invoking code: "+keyCode);
    invoke ( new Integer(keyCode));
  }

  public synchronized void invoke (Integer keyCode) {
    HotKey k = (HotKey) hotkeys.get(keyCode);
    CAT.debug("invoking code: "+keyCode==null?null:keyCode.intValue()+"("+keyCode.byteValue()+")");

    if ( k != null )
      k.executeAction();
  }

  public synchronized HotKey [] getHotKeys() {
     Set keys = hotkeys.keySet();
     int size = keys.size();
     HotKey [] back = new HotKey[size];
     Iterator it = keys.iterator();
     for (int i=0; i<size; i++)
        back [i] = (HotKey) hotkeys.get(it.next());

     return back;
  }

  public synchronized HotKey getHotKey(int keyCode) {
    return (HotKey) hotkeys.get(new Integer(keyCode));
  }

  public synchronized HotKey getHotKey(Integer keyCode) {
    return (HotKey) hotkeys.get(keyCode);
  }

  public synchronized HotKey getHotKeyByName(String keyName) {
    return (HotKey) keysByName.get(keyName);

  }


  public synchronized String dump () {
    HotKey [] all = getHotKeys();
    StringBuffer sb = new StringBuffer("HOTKEYS:\n");
    for (int i=0;i<all.length;i++)
      sb.append(all[i].toString()).append("\n");
    return sb.toString();
  }



}