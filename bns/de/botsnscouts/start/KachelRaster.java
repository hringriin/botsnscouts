package de.spline.rr;

public class KachelRaster{
    private int KX=3, KY=3, FL=6 ;
    private Kachel kacheln[][]=new Kachel[KX][KY];
    private Ort[] flaggen = new Ort[FL];
    private int flaggenN=0;
    private int[][] kachind = new int[KX][KY];
    private KachelFactory kachFactory;

    public KachelRaster(KachelFactory kachF){
	kachFactory=kachF;
    }

    //setzt �bergebene Kachel an Stelle (x,y)
    public void setKachel(int x, int y, String name) throws FlaggenVorhandenException{
	if (sindFlaggen(x,y))
	    throw new FlaggenVorhandenException();
	kacheln[x][y]=kachFactory.getKachel(name,0);
    }

    //dreht die Kachel um 90� nach links
    public void rotKachel(int x, int y){
	if (kacheln[x][y]==null) return;
	//hole rotierte Kachel
	kacheln[x][y]=kachFactory.getKachel(kacheln[x][y].getName(), (kacheln[x][y].getDrehung()+1)%4);
	//rotiere Flaggen, falls vorhanden
	int kx,ky,fx,fy;
	for (int j=0;j<flaggenN;j++){
	    kx=(flaggen[j].x-1)/12;//kachel x
	    ky=(flaggen[j].y-1)/12;//kachel y
	    if (kx==x&&ky==y){//die Flagge ist auf der zu drehenden Kachel
		fx=(flaggen[j].x-1)%12;//Flaggenposition
		fy=(flaggen[j].y-1)%12;//im Kachel (-1)
		//y->11-y + kachelX*12 +1 (x)
		int nx=11-fy+kx*12+1;
		//x->11-x + kachelY*12 +1 (y)
		int ny=fx+ky*12+1;
		flaggen[j].x=nx;
		flaggen[j].y=ny;
	    }
	}
    }
    
    //l�scht die Kachel at (x,y)
    public void delKachel(int x, int y){
	//entferne Flaggen falls vorhanden
	for (int i=flaggenN-1;i>=0;i--){
	    if ((flaggen[i].x-1)/12==x&&(flaggen[i].y-1)/12==y){
		//Global.debug(this,"Entferne Flagge nr "+i);
		delFlagge(i);
	    }
	}
	//enferne Kachel
	kacheln[x][y]=null;
    }

    //pr�ft ob auf der Kachel at (x,y) Flaggen stehen
    public boolean sindFlaggen(int x, int y){
	for (int i=0;i<flaggenN;i++){
	    if ((flaggen[i].x-1)/12==x&&(flaggen[i].y-1)/12==y){
		return true;
	    }
	}
	return false;
    }

    // gibt ein String zur�ck falls die FlaggenPosition ung�nstig ist
    // "" sonst
    public String getFlaggeKomment(int x,int y){
	int kx=(x-1)/12;
	int ky=(y-1)/12;
	Ort[] flag= new Ort[1];
	flag[0]=new Ort((x-1)%12+1,(y-1)%12+1);
	String komment="";
	try{
	    Spielfeld tmpSpf=new Spielfeld(12,12,kacheln[kx][ky].getComputedString(),flag);
	    komment=tmpSpf.getFlaggenProbleme();
	}catch(FlaggenException e){
	    System.err.println(e);
	}catch (FormatException e){
	    System.err.println(e);
	}
	//Global.debug(this,komment);
	return komment;
    }

    // pr�ft ob eine Flagge hinzugef�gt werden kann
    public boolean checkFlaggePos(int x,int y){
	//falls schon alle Flaggen da sind
	if (flaggenN==FL){
	    return false;
	}
	return checkFlaggeMovePos(x,y);
    }

    // pr�ft ob eine Flagge hinzugef�gt werden kann
    public boolean checkFlaggeMovePos(int x,int y){
	int kx=(x-1)/12;
	int ky=(y-1)/12;
	//fals keine Kachel drunter
	//Global.debug(this, "kx: "+kx+" ky: "+ky+" x: "+x+" y: "+y);
	if (kacheln[kx][ky]==null){
	    return false;
	}

	//pr�fe ob an der Stelle schon eine Flagge Steht
	if (istFlagge(x,y)){
	    return false;
	}
	//pr�fe Kachelelement
	Ort[] flag=new Ort[1];
	flag[0]=new Ort((x-1)%12+1,(y-1)%12+1);

	boolean testFl=kacheln[kx][ky].testFlagge(flag);
	//Global.debug(this, "testFlagge "+kx+","+ky+" "+testFl+" Flagge:"+flag[0].x+","+flag[0].y);
	return testFl;
    }

    // f�gt eine Flagge hinzu 
    public void addFlagge(int x,int y) throws FlaggenException{
	if (!checkFlaggePos(x,y))
	    throw new FlaggenException();
	flaggen[flaggenN++]=new Ort(x,y);
    }

    // l�scht eine Flagge
    public void delFlagge(int nr){
	if (nr>=flaggenN) return;
	for (int i=nr+1;i<flaggenN;i++){
	    flaggen[i-1]=flaggen[i];
	}
	flaggen[--flaggenN]=null;
    }

    // l�scht eine Flagge an der Position
    public void delFlagge(int ax,int ay){
	for (int i=0;i<flaggenN;i++){
	    if (flaggen[i].x==ax&&flaggen[i].y==ay){
		delFlagge(i);
		return;
	    }
	}
    }

    // pr�ft ob eine Flagge an der Position vorhanden ist
    public boolean istFlagge(int ax,int ay){
	for (int i=0;i<flaggenN;i++){
	    if (flaggen[i].x==ax&&flaggen[i].y==ay){
		return true;
	    }
	}
	return false;
    }

    // versetzt eine Flagge 
    public void moveFlagge(int nr, int x,int y) throws FlaggenException{
	if (!checkFlaggeMovePos(x,y))
	    throw new FlaggenException();
	flaggen[nr]=new Ort(x,y);
    }

    // versetzt eine Flagge an der Position
    public void moveFlagge(int ax,int ay, int x,int y) throws FlaggenException{
	for (int i=0;i<flaggenN;i++){
	    if (flaggen[i].x==ax&&flaggen[i].y==ay){
		moveFlagge(i,x,y);
		return;
	    }
	}
    }

    //gibt die Flaggen zur�ck
    public Ort[] getFlaggen(){
	return flaggen;
    }

    //gibt maximale Anzahl der Flaggen zur�ck
    public int getMaxFlag(){
	return FL;
    }

    //gibt Kacheln als 2-dim Array von Spielfeld zur�ck
    public Kachel[][] getKacheln(){
	return kacheln;
    }

    //gibt eine Kachel an der gegebenen Position
    public Kachel getKachelAt(int x, int y){
	return kacheln[x][y];
    }

    //setzt Spielfeldgr��e
    public void setSpielfeldDim(int x, int y){
	KX=x;
	KY=y;
    }

    //gibt Spielfeldgr��e zur�ck
    public Ort getSpielfeldDim(){
	return new Ort(KX,KY);
    }

    //gibt den "Clone" zur�ck
    public KachelRaster getClone(){
	KachelRaster tmpRaster = new KachelRaster(kachFactory);
	tmpRaster.flaggenN=flaggenN;
	for (int i=0;i<flaggenN;i++){
	    tmpRaster.flaggen[i]=flaggen[i];
	}
	for (int i=0;i<KX;i++){
	    for (int j=0;j<KY;j++){
		tmpRaster.kacheln[i][j]=kacheln[i][j];
		tmpRaster.kachind[i][j]=kachind[i][j];
	    }
	}

	return tmpRaster;
    }

    //* nicht f�r Fassade *//

    //gibt Flaggen mit evtl versetzten Koordinaten zur�ck
    public int[][] getRFlaggen(){
	Ort[] bounds=findBounds();
	int[][] flags=new int[2][flaggenN];
	for (int i=0;i<flaggenN;i++){
	    flags[0][i]=flaggen[i].x-bounds[0].x*12;
	    flags[1][i]=flaggen[i].y-bounds[0].y*12;
	}
	return flags;
    }

    //gibt das Spielfeld als ein String zur�ck
    public String getSpielfeld() throws OneFlagException, NichtZusSpfException{
	checkSpielfeld();//teste
	Ort[] bounds= findBounds();
	String GRUBENZWR="____________";
	String GRUBENFLD="_G_G_G_G_G_G_G_G_G_G_G_G_";
	StringBuffer out=new StringBuffer();//hier wird das Spielfeld aufgebaut
	String rechts=new String();
	boolean links =false;
	StringBuffer oben = new StringBuffer();
	StringBuffer unten =new StringBuffer();
	for (int i=0;i<KX;i++)
	    for (int j=0;j<KY;kachind[i][j++]=0);
	for(int j=bounds[1].y;j>=bounds[0].y;j--){
	    for (int k=0;k<25;k++){
		for (int i=bounds[0].x;i<=bounds[1].x;i++){
		    if (k==0){
			if (kacheln[i][j]==null)
			    unten.append(GRUBENZWR);
			else unten.append(liesZeile(kacheln[i][j].getComputedString(),i,j));
		    }
		    else if (k==24){
			if (kacheln[i][j]==null)
			    oben.append(GRUBENZWR);
			else oben.append(liesZeile(kacheln[i][j].getComputedString(),i,j));
		    }
		    else{
			if (k%2==0){
			    if (kacheln[i][j]==null)
				out.append(GRUBENZWR);
			    else
				out.append(liesZeile(kacheln[i][j].getComputedString(),i,j));
			}
			else{
			    if (kacheln[i][j]==null)
				rechts=GRUBENFLD;
			    else rechts=new String(liesZeile(kacheln[i][j].getComputedString(),i,j));
			    if (links) mergez(out,rechts);
			    else{
				out.append(rechts);
				links=true;
			    }
			}
		    }
		} //for i
		links=false;
		if (k==0){
		    out.append(merger(oben,unten));
		    oben = new StringBuffer();
		    unten =new StringBuffer();
		} //endif
		out.append("\n");
	    } //for k
	} //for j
	out.append(oben);
	out.append(".\n");
	/*	if(!checkit(out.toString(),(xm+1)*12,(ym+1)*12))
		System.err.println("Ooops!!!!");*/
	return out.toString();
    }

    public static void mergez(StringBuffer l, String r){
	char lc,fc;
	lc=l.charAt(l.length()-1);
	if (lc=='#') fc=lc;
	else fc=r.charAt(0);
	int x=l.length()-1;
	l.setLength(l.length()-1);
	l.append(r);
	l.setCharAt(x,fc);
    }

    public static String merger(StringBuffer o, StringBuffer u){
	if (o.length()==0)
	    return u.toString();
	StringBuffer zwr=new StringBuffer();
	char oc,uc;
	int oi=0, ui=0;
	oc = o.charAt(oi++);
	uc = u.charAt(ui++);
	while (oi<o.length()&&ui<u.length()){
	    while (oc != '_' && oc != '#'&&oi<o.length()){
		zwr.append(oc);
		oc=o.charAt(oi++);
	    }
	    if (oc=='_') zwr.append(uc);
	    else zwr.append(oc);
	    uc = u.charAt(ui++);
	    if (oi<o.length()) oc = o.charAt(oi++);
	    while (uc != '_' && uc != '#'&&ui<u.length()){
		zwr.append(uc);
		uc=u.charAt(ui++);
	    }
	    if (ui==u.length()&&uc!='_'&&uc!='#') zwr.append(uc);
	}
	if (oi<o.length()){
	    while (oc != '_' && oc != '#'&&oi<o.length()){
		zwr.append(oc);
		oc=o.charAt(oi++);
	    }
	}
	if ((oc == '_'||oc=='#')&&(uc =='_'||uc =='#'))
	    if (oc=='_') zwr.append(uc);
	    else zwr.append(oc);
	if (ui<u.length()){
	    uc = u.charAt(ui++);
	    while (uc != '_' && uc != '#'&&ui<u.length()){
		zwr.append(uc);
		uc=u.charAt(ui++);
	    }
	    zwr.append(uc);
	}
	
	return (zwr.toString()).trim();
    }

    //liest eine Zeile aus Kachelstring
    public String liesZeile(String fil,int i, int j){
	StringBuffer str=new StringBuffer();
	try{
	    char x=fil.charAt(kachind[i][j]++);
	    while(x=='\10'||x=='\13'||x=='\32'||x=='\t'||x=='\n'||x==' ')
		x=fil.charAt(kachind[i][j]++);
	    while (x!='\10'&&x!='\13'&&x!='\32'&&x!='\t'&&x!='\n'&&x!=' '){
		str.append(x);
		x=fil.charAt(kachind[i][j]++);
	    }
	}catch(Exception ex){
	    System.err.println(ex);
	}
	return (str.toString()).trim();
    }
    
    //findet Grenzen des Spielfeldes
    Ort[] findBounds(){
	Ort[] bounds=new Ort[2];
	bounds[0]=new Ort(0,0);
	bounds[1]=new Ort(0,0);
	x0: for (int i=0;i<KX;i++){
	    for (int j=0;j<KY;j++){
		if (kacheln[i][j]!=null){
		    bounds[0].x=i;
		    break x0;
		}
	    }
	}
	y0: for (int j=0;j<KY;j++){
	    for (int i=0;i<KX;i++){
		if (kacheln[i][j]!=null){
		    bounds[0].y=j;
		    break y0;
		}
	    }
	}
	x1: for (int i=KX-1;i>=0;i--){
	    for (int j=KY-1;j>=0;j--){
		if (kacheln[i][j]!=null){
		    bounds[1].x=i;
		    break x1;
		}
	    }
	}
	y1: for (int j=KY-1;j>=0;j--){
	    for (int i=KX-1;i>=0;i--){
		if (kacheln[i][j]!=null){
		    bounds[1].y=j;
		    break y1;
		}
	    }
	}	    
	return bounds;
    }

    //gibt Gr��e des Spielfeldes zur�ck
    public Ort getSpielfeldSize(){
	Ort[] bounds=findBounds();
	return new Ort((bounds[1].x-bounds[0].x+1)*12,(bounds[1].y-bounds[0].y+1)*12);
    }
    
    //pr�ft ob Spielfeld g�ltig ist (plausibilit�tstests)
    public boolean checkSpielfeld() throws OneFlagException, NichtZusSpfException{
	//zu wenig Flaggen
	if (flaggenN<2)
	    throw new OneFlagException();
	//das Spielfeld ist nicht zusammenh�ngend
	Ort fkachel=findFirstKachel();
	boolean[][] mark=new boolean[KX][KY];
	for (int i=0;i<KX;i++)
	    for (int j=0;j<KY;mark[i][j]=false,j++);
	mark[fkachel.x][fkachel.y]=true;
	markNachbarn(fkachel,mark);
	for (int i=0;i<KX;i++){
	    for (int j=0;j<KY;j++){
		if (kacheln[i][j]!=null&&!mark[i][j]){
		    throw new NichtZusSpfException();
		}
	    }
	}
	return true;
    }

    //findet die erste belegte Kachel
    Ort findFirstKachel(){
	for (int i=0;i<KX;i++){
	    for (int j=0;j<KY;j++){
		if (kacheln[i][j]!=null)
		    return new Ort(i,j);
	    }
	}
	return null;
    }

    //markiert alle (mit der ersten) zusammenh�ngende Kacheln
    void markNachbarn(Ort kach, boolean[][] mark){
	//shau nach links
	if (kach.x-1>=0&&kacheln[kach.x-1][kach.y]!=null&&!mark[kach.x-1][kach.y]){
	    mark[kach.x-1][kach.y]=true;
	    markNachbarn(new Ort(kach.x-1,kach.y),mark);
	}
	//rechts
	if (kach.x+1<KX&&kacheln[kach.x+1][kach.y]!=null&&!mark[kach.x+1][kach.y]){
	    mark[kach.x+1][kach.y]=true;
	    markNachbarn(new Ort(kach.x+1,kach.y),mark);
	}
	//unten
	if (kach.y-1>=0&&kacheln[kach.x][kach.y-1]!=null&&!mark[kach.x][kach.y-1]){
	    mark[kach.x][kach.y-1]=true;
	    markNachbarn(new Ort(kach.x,kach.y-1),mark);
	}
	//oben
	if (kach.y+1<KY&&kacheln[kach.x][kach.y+1]!=null&&!mark[kach.x][kach.y+1]){
	    mark[kach.x][kach.y+1]=true;
	    markNachbarn(new Ort(kach.x,kach.y+1),mark);
	}
    }

}

