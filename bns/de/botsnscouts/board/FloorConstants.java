package de.botsnscouts.board;


public interface FloorConstants {
  /** Boden Grube */
  public static final int BDGRUBE   = -1;
  /** Boden normaler Boden */
  public static final int BDNORMAL  =  0;
  /** Boden Reparaturfeld */
  public static final int BDREPA    =  1;
  /** Boden DrehElement */
  public static final int BDDREHEL  = 10;

  /** Drehelementrichtung im Uhrzeigersinn */
  public static final int DUHRZ   = 0;
  /** Drehelementrichtung gegen den Uhrzeigersinn */
  public static final int DGGUHRZ = 1;

  /* Fliessbaenderdesign : Endziffern stehen fuer: */
  /** Fliessband Richtung Norden */
  public static final int FNORD = 0;
  /** Fliessband Richtung Osten */
  public static final int FOST  = 1;
  /** Fliessband Richtung S�den */
  public static final int FSUED = 2;
  /** Fliessband Richtung Westen */
  public static final int FWEST = 3;

  // Fliessb�nder - geradeaus
  // ========================
  /** Fliessband Richtung Norden, Geschwindigkeit 1 */
  public static final int FN1 = 100;
  /** Fliessband Richtung Osten,  Geschwindigkeit 1 */
  public static final int FO1 = 101;
  /** Fliessband Richtung Sueden, Geschwindigkeit 1 */
  public static final int FS1 = 102;
  /** Fliessband Richtung Westen, Geschwindigkeit 1 */
  public static final int FW1 = 103;

  /** Fliessband Richtung Norden, Geschwindigkeit 2 */
  public static final int FN2 = 200; //
  /** Fliessband Richtung Osten,  Geschwindigkeit 2 */
  public static final int FO2 = 201; //
  /** Fliessband Richtung Sueden, Geschwindigkeit 2 */
  public static final int FS2 = 202; //
  /** Fliessband Richtung Westen, Geschwindigkeit 2 */
  public static final int FW2 = 203; //

    // Fliessbaender - abbiegen -  normal

  /** Flie�band Abbiegen (Richtung) Norden von Westen*/
  public static final int NVW1 = 120;
  /** Flie�band Abbiegen (Richtung) Norden von Osten */
  public static final int NVO1 = 130;
  /** Flie�band Abbiegen (Richtung) Osten von Norden */
  public static final int OVN1 = 121;
  /** Flie�band Abbiegen (Richtung) Osten von Sueden */
  public static final int OVS1 = 131;
  /** Flie�band Abbiegen (Richtung) S�den von Westen */
  public static final int SVW1 = 132;
  /** Flie�band Abbiegen (Richtung) S�den von Osten */
  public static final int SVO1 = 122;
  /** Flie�band Abbiegen (Richtung) Westen von Norden */
  public static final int WVN1 = 133;
  /** Flie�band Abbiegen (Richtung) Westen von S�den */
  public static final int WVS1 = 123;
  /** Flie�band Abbiegen (Richtung)  Norden von  Westen oder Osten */
  public static final int NVWO1 = 150;
  /** Flie�band Abbiegen (Richtung)  Osten von   Norden oder S�den */
  public static final int OVNS1 = 151;
  /** Flie�band Abbiegen (Richtung)  S�den von  Westen oder Osten */
  public static final int SVWO1 = 152;
  /** Flie�band Abbiegen (Richtung)  Westen von  Nord oder S�den */
  public static final int WVNS1 = 153;

  // Fliessbaender - abbiegen - express
  //
  /** Express-Flie�band Abbiegen (Richtung) Norden von Westen (kommend) */
  public static final int NVW2 = 220;
  /** Express-Flie�band Abbiegen (Richtung) Norden von Osten */
  public static final int NVO2 = 230;
  /** Express-Flie�band Abbiegen (Richtung) Osten von Norden */
  public static final int OVN2 = 221;
  /** Express-Flie�band Abbiegen (Richtung) Osten von Sueden */
  public static final int OVS2 = 231;
  /** Express-Flie�band Abbiegen (Richtung) S�den von Westen */
  public static final int SVW2 = 232;
  /** Express-Flie�band Abbiegen (Richtung) S�den von Osten */
  public static final int SVO2 = 222;
  /** Express-Flie�band Abbiegen (Richtung) Westen von Norden */
  public static final int WVN2 = 233;
  /** Express-Flie�band Abbiegen (Richtung) Westen von S�den */
  public static final int WVS2 = 223;
  /** Express-Flie�band Abbiegen (Richtung)  Norden von  Westen oder Osten */
  public static final int NVWO2 = 250;
  /** Express-Flie�band Abbiegen (Richtung)  Osten von   Norden oder S�den */
  public static final int OVNS2 = 251;
  /** Express-Flie�band Abbiegen (Richtung)  S�den von  Westen oder Osten */
  public static final int SVWO2 = 252;
  /** Express-Flie�band Abbiegen (Richtung)  Westen von  Nord oder S�den */
  public static final int WVNS2 = 253;
}