/**
 * Invoicex Copyright (c) 2005-2016 Marco Ceccarelli, Tnx srl
 *
 * Questo software è soggetto, e deve essere distribuito con la licenza GNU
 * General Public License, Version 2. La licenza accompagna il software o potete
 * trovarne una copia alla Free Software Foundation http://www.fsf.org .
 *
 * This software is subject to, and may be distributed under, the GNU General
 * Public License, Version 2. The license should have accompanied the software
 * or you may obtain a copy of the license from the Free Software Foundation at
 * http://www.fsf.org .
 *
 * -- Marco Ceccarelli (m.ceccarelli@tnx.it) Tnx snc (http://www.tnx.it)
 *
 */
/*
 * frmElenPrev.java
 *
 * Created on 23 novembre 2001, 14.54
 */
package gestioneFatture;

import gestioneFatture.logic.documenti.Documento;
import it.tnx.accessoUtenti.Permesso;
import it.tnx.commons.AutoCompletionEditable;
import it.tnx.commons.CastUtils;

import it.tnx.commons.DbUtils;
import it.tnx.commons.FormatUtils;
import it.tnx.commons.FxUtils;
import it.tnx.commons.SwingUtils;
import it.tnx.commons.cu;
import it.tnx.commons.dbu;
import it.tnx.commons.fu;
import it.tnx.commons.ju;
import it.tnx.commons.table.EditorUtils;
import it.tnx.gui.MyOsxFrameBorder;

import it.tnx.invoicex.InvoicexUtil;
import it.tnx.invoicex.MyAbstractListIntelliHints;
import it.tnx.invoicex.iu;
import it.tnx.invoicex.sync.Sync;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.beans.PropertyVetoException;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import tnxbeans.LimitedTextPlainDocument;
import tnxbeans.SeparatorComboBoxRenderer;
import tnxbeans.tnxComboField;
import tnxbeans.tnxDbGrid;
import tnxbeans.tnxDbPanel;
import tnxbeans.tnxTextField;

/**
 *
 *
 *
 * @author marco
 *
 */
public class frmTestDocu
        extends javax.swing.JInternalFrame
        implements GenericFrmTest {

    public dbDocumento dbdoc = new dbDocumento();
    private Documento doc = new Documento();
    public frmElenDDT from;
    private Db db = Db.INSTANCE;
    int pWidth;
    int pHeight;
    public String dbStato = "L";
    public static String DB_INSERIMENTO = "I";
    public static String DB_MODIFICA = "M";
    public static String DB_LETTURA = "L";
    private String sql = "";
    private String old_id = "";
    private boolean id_modificato = false;
    private String old_anno = "";
    private String old_data = "";
    private boolean anno_modificato = false;
    private int comClieSel_old = -1;
    private int comClieDest_old = -1;
    private double totaleIniziale;
    private String serie_originale = null;
    private Integer numero_originale = null;
    private Integer anno_originale = null;
    public boolean acquisto = false;
    public String suff = "";
    private String ccliente = "cliente";
    private boolean loading = true;
    public Integer id = null;
    private boolean block_aggiornareProvvigioni;

    MyAbstractListIntelliHints al_clifor = null;
    AtomicReference<ClienteHint> clifor_selezionato_ref = new AtomicReference(null);

    public tnxbeans.tnxComboField deposito;
    public tnxbeans.tnxComboField deposito_arrivo;
    public javax.swing.JLabel labDepositoDestinazione;
    public javax.swing.JLabel labDepositoPartenza;
    public tnxDbPanel pan_deposito = null;

    boolean chiudere = true;

    public String table_righe_temp = null;
    public String table_righe_lotti_temp = null;
    public String table_righe_matricole_temp = null;

    public frmTestDocu(String dbStato, String dbSerie, int dbNumero, String prevStato, int dbAnno) {
        this(dbStato, dbSerie, dbNumero, prevStato, dbAnno, -1);
    }

    public frmTestDocu(String dbStato, String dbSerie, int dbNumero, String prevStato, int dbAnno, int dbIdDocu) {
        this(dbStato, dbSerie, dbNumero, prevStato, dbAnno, dbIdDocu, false);
    }

    public frmTestDocu(String dbStato, String dbSerie, int dbNumero, String prevStato, int dbAnno, int dbIdDocu, boolean acquisto) {

        loading = true;
        this.id = dbIdDocu;
        if (!dbStato.equals(tnxDbPanel.DB_INSERIMENTO)) {
            dbdoc.setId(dbIdDocu);
        } else {
            dbdoc.setId(-1);
        }
        this.acquisto = acquisto;

//        int permesso = Permesso.PERMESSO_DDT_VENDITA;
        if (acquisto) {
            suff = "_acquisto";
            ccliente = "fornitore";
            dbdoc.acquisto = true;
//            permesso = Permesso.PERMESSO_DDT_VENDITA;
        } else {
        }

        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        this.dbStato = dbStato;

        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setDismissDelay(30000);

        initComponents();
        
        if (main.versione.equalsIgnoreCase("base")) {
            menColAggNote.setEnabled(false);
        } else {
            menColAggNote.setIcon(null);
            menColAggNote.setEnabled(true);
            menColAggNote.setToolTipText(null);
        }

        //campi liberi
        InvoicexUtil.initCampiLiberiTestate(this);

        LimitedTextPlainDocument limit = new LimitedTextPlainDocument(1, true);
        texSeri.setDocument(limit);

        if (!main.getPersonalContain("cirri")) {
            butImportXlsCirri.setVisible(false);
        }

        if (!main.getPersonalContain("consegna_e_scarico")) {
            labModConsegna.setVisible(false);
            labModScarico.setVisible(false);
            comConsegna.setVisible(false);
            comScarico.setVisible(false);

            labNoteConsegna.setVisible(false);
            texNoteConsegna.setVisible(false);
        }

        AutoCompletionEditable.enable(comAspettoEsterioreBeni);
        AutoCompletionEditable.enable(comCausaleTrasporto);
        AutoCompletionEditable.enable(comVettori);
        AutoCompletionEditable.enable(comMezzoTrasporto);
        AutoCompletionEditable.enable(comPorto);

//        if(!main.utente.getPermesso(permesso, Permesso.PERMESSO_TIPO_SCRITTURA)){
//            SwingUtils.showErrorMessage(main.getPadrePanel(), "Non hai i permessi per accedere a questa funzionalità", "Impossibile accedere", true);
//            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//            this.setVisible(false);
//            this.dispose();
//            return;
//        }
        InvoicexUtil.macButtonSmall(butPrezziPrec);

        prezzi_ivati.setVisible(false);

        comClie.putClientProperty("JComponent.sizeVariant", "small");
        comClieDest.putClientProperty("JComponent.sizeVariant", "small");
        comAgente.putClientProperty("JComponent.sizeVariant", "small");
        comPaga.putClientProperty("JComponent.sizeVariant", "small");
        stato_evasione.putClientProperty("JComponent.sizeVariant", "small");

        comCausaleTrasporto.putClientProperty("JComponent.sizeVariant", "mini");
        comAspettoEsterioreBeni.putClientProperty("JComponent.sizeVariant", "mini");
        comVettori.putClientProperty("JComponent.sizeVariant", "mini");
        comMezzoTrasporto.putClientProperty("JComponent.sizeVariant", "mini");
        comPorto.putClientProperty("JComponent.sizeVariant", "mini");
        comPaese.putClientProperty("JComponent.sizeVariant", "mini");

        if (main.getPersonalContain("litri")) {
            butInserisciPeso.setText("Inserisci Tot. Litri");
        }

        if (SystemUtils.IS_OS_MAC_OSX) {
            panDati.setBorder(new MyOsxFrameBorder());

            Border b1 = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.lightGray), BorderFactory.createEmptyBorder(2, 2, 2, 2));
            ArrayList<JComponent> compsb = new ArrayList();
            compsb.add(texSeri);
            compsb.add(texNume);
            compsb.add(texData);
            compsb.add(texCliente);
            compsb.add(texScon1);
            compsb.add(texScon2);
            compsb.add(texScon3);
            compsb.add(texSpeseTrasporto);
            compsb.add(texSpeseIncasso);
            compsb.add(texNote);
            compsb.add(texDataOra);
            compsb.add(texNumeroColli);
            compsb.add((JComponent) comPaga.getEditor().getEditorComponent());
            compsb.add(texPesoLordo);
            compsb.add(texPesoNetto);
            compsb.add((JComponent) comAgente.getEditor().getEditorComponent());
            compsb.add(texProvvigione);
            compsb.add((JComponent) comConsegna.getEditor().getEditorComponent());
            compsb.add((JComponent) comScarico.getEditor().getEditorComponent());

            for (JComponent c : compsb) {
                c.setBorder(b1);
            }

        }

//        texNote.getJTextArea().getDocument().addDocumentListener(new DocumentListener() {
//
//            public void insertUpdate(DocumentEvent e) {
//                Thread.dumpStack();
//                System.err.println("!!! rinsert");
//            }
//
//            public void removeUpdate(DocumentEvent e) {
//                Thread.dumpStack();
//                System.err.println("!!! remove");
//            }
//
//            public void changedUpdate(DocumentEvent e) {
//                Thread.dumpStack();
//                System.err.println("!!! change");
//            }
//        });
        texNote.setFont(texSeri.getFont());

        texCliente.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                texCliente.selectAll();
            }
        });

        boolean filtraclifor = main.fileIni.getValueBoolean("pref", "filtraCliFor", false);
        InvoicexUtil.CliforTipo tipo = InvoicexUtil.CliforTipo.Tutti;
        if (filtraclifor) {
            tipo = InvoicexUtil.CliforTipo.Solo_Clienti_Entrambi_Provvisori;
            if (acquisto) {
                tipo = InvoicexUtil.CliforTipo.Solo_Fornitori_Entrambi_Provvisori;
            }
        }
        al_clifor = InvoicexUtil.getCliforIntelliHints(texCliente, this, clifor_selezionato_ref, null, comClieDest, tipo);
        al_clifor.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("selezionato")) {
                    ClienteHint hint = (ClienteHint) clifor_selezionato_ref.get();
                    if (hint != null) {
                        texClie.setText(hint.codice);
                    } else {
                        texClie.setText("");
                    }
                    comClie.dbTrovaKey(texClie.getText());
                    selezionaCliente();
                }
            }
        });

        if (main.getPersonalContain("carburante")) {
            jLabel114.setText("spese carburante");
        }

//        griglia.setNoTnxResize(true);
        
        griglia.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        if (acquisto) {
            stato_evasione.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"Fatturato", "Fatturato Parzialmente", "Non Fatturato"}));
        } else {
            stato_evasione.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"Fatturato", "Fatturato Parzialmente", "Non Fatturato"}));
        }
        stato_evasione.setSelectedIndex(2);

        dati.aggiungiDbPanelCollegato(dati_dest_diversa);

        if (acquisto) {
            setTitle("DDT di Acquisto");
            jLabel151.setText("fornitore");
            texClie.setDbNomeCampo(ccliente);
            texClieDest.setDbNomeCampo("id_" + ccliente + "_destinazione");
            labRiferimento.setText("Rif. Forn.");

//            stampa_prezzi.setVisible(false);
            labAgente.setVisible(false);
            comAgente.setVisible(false);

            labProvvigione.setVisible(false);
            labPercentoProvvigione.setVisible(false);
            texProvvigione.setVisible(false);

            sepDestMerce.setVisible(false);

            jLabel15.setVisible(false);
            comClieDest.setVisible(false);

            labScon10.setVisible(false);
            labScon11.setVisible(false);
            labScon12.setVisible(false);
            labScon13.setVisible(false);
            labScon14.setVisible(false);
            labScon16.setVisible(false);
            labScon15.setVisible(false);
            labScon17.setVisible(false);
            texDestRagioneSociale.setVisible(false);
            texDestIndirizzo.setVisible(false);
            texDestCap.setVisible(false);
            texDestLocalita.setVisible(false);
            texDestProvincia.setVisible(false);
            texDestTelefono.setVisible(false);
            texDestCellulare.setVisible(false);
            comPaese.setVisible(false);
//            jPanel6.setVisible(false);

//            dati_altri2.remove(texForni);
//            dati_altri2.remove(comForni);
//
//            dati_altri1.setVisible(false);
//            dati_altri2.setVisible(false);
//
//            jLabel17.setText("Rif. Forn.");
//            jLabel17.setToolTipText("Inserire il numero dell'ordine assegnato dal fornitore");
//            jLabel151.setPreferredSize(new Dimension((int)jLabel151.getPreferredSize().getWidth()+150, (int)jLabel151.getPreferredSize().getHeight()));
//            jSplitPane1.setDividerLocation(170);
        } else {
            setTitle("DDT di Vendita");
        }

        //init campi particolari
        this.texData.setDbDefault(texData.DEFAULT_CURRENT);

        try {
            InvoicexEvent event = new InvoicexEvent(this);
            event.type = InvoicexEvent.TYPE_FRMTESTDDT_CONSTR_POST_INIT_COMPS;
            main.events.fireInvoicexEvent(event);
        } catch (Exception err) {
            err.printStackTrace();
        }

        //oggetto preventivo
        this.dbdoc.dbStato = dbStato;
        this.dbdoc.serie = dbSerie;
        this.dbdoc.numero = dbNumero;
        this.dbdoc.stato = prevStato;
        this.dbdoc.anno = dbAnno;
        this.dbdoc.texTota = this.texTota;
        this.dbdoc.texTotaImpo = this.texTotaImpo;
        this.dbdoc.texTotaIva = this.texTotaIva;
        this.dbdoc.tipoDocumento = getTipoDoc();
//        this.setClosable(false);

        //faccio copia in caso di annulla deve rimettere le righe giuste
        if (!main.edit_doc_in_temp) {
            if (dbStato == this.DB_MODIFICA) {
                porto_in_temp();
                //memorizzo il numero doc originale
                serie_originale = dbSerie;
                numero_originale = dbNumero;
                anno_originale = dbAnno;
            }
        } else {
            //porto righe in tabella temporanea e modifica quella temporanea, se poi si conferma le porto nelle righe definitive
            table_righe_temp = InvoicexUtil.getTempTableName("righ_ddt" + suff);
            table_righe_lotti_temp = InvoicexUtil.getTempTableName("righ_ddt" + suff + "_lotti");
            table_righe_matricole_temp = InvoicexUtil.getTempTableName("righ_ddt" + suff + "_matricole");
            try {
                InvoicexUtil.createTempTable(dbStato, table_righe_temp, "righ_ddt" + suff, id);
                InvoicexUtil.createTempTable(dbStato, table_righe_lotti_temp, "righ_ddt" + suff + "_lotti", id);
                InvoicexUtil.createTempTable(dbStato, table_righe_matricole_temp, "righ_ddt" + suff + "_matricole", id);
                dbdoc.table_righe_temp = table_righe_temp;
                if (dbStato.equalsIgnoreCase(this.DB_INSERIMENTO)) {
                    dbdoc.setId(-1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtils.showExceptionMessage(main.getPadreFrame(), e);
            }
        }

        //this.texSeri.setVisible(false);
        //associo il panel ai dati
        this.dati.dbNomeTabella = "test_ddt" + suff;

        dati.dbChiaveAutoInc = true;

        dati.messaggio_nuovo_manuale = true;

//        Vector chiave = new Vector();
//        chiave.add("serie");
//        chiave.add("numero");
//        chiave.add("anno");
//        this.dati.dbChiave = chiave;
        Vector chiave = new Vector();
        chiave.add("id");
        dati.dbChiave = chiave;

        dati.aggiungiDbPanelCollegato(datiAltro);

        //apro la combo pagamenti
        this.comPaga.dbAddElement("", "");
        this.comPaga.dbOpenList(db.getConn(), "select codice, codice from pagamenti order by codice", null, false);

        comConsegna.dbOpenList(db.getConn(), "select nome, id from tipi_consegna", null, false);
        comScarico.dbOpenList(db.getConn(), "select nome, id from tipi_scarico", null, false);

        comPaese.dbAddElement("", "");
        comPaese.dbOpenList(Db.getConn(), "select nome, codice1 from stati", null, false);

        comPorto.dbAddElement("", "");
        comPorto.dbOpenList(Db.getConn(), "select porto, id from tipi_porto group by porto");

        //mezzo di trasporto
        comMezzoTrasporto.dbAddElement("");
        comMezzoTrasporto.dbOpenList(Db.getConn(), "select nome,id from tipi_consegna group by nome");

        //carico causali trasporto
        comCausaleTrasporto.dbAddElement("");
//        comCausaleTrasporto.dbOpenList(Db.getConn(), "select nome, id from tipi_causali_trasporto order by nome");
        comCausaleTrasporto.dbOpenList(Db.getConn(), "select nome, id from tipi_causali_trasporto group by nome", null, false);

        //105 carico aspetti esteriori beni per gianni
//        comAspettoEsterioreBeni.dbAddElement("SCATOLA");
//        comAspettoEsterioreBeni.dbAddElement("A VISTA");
//        comAspettoEsterioreBeni.dbAddElement("SCATOLA IN PANCALE");
//        if (main.getPersonal().equals(main.PERSONAL_CUCINAIN)) {
//            comAspettoEsterioreBeni.dbAddElement("BUSTA");
//            if (dbStato.equals(this.DB_INSERIMENTO)) {
//                comAspettoEsterioreBeni.setText("BUSTA");
//            }
//        } else {
//            comAspettoEsterioreBeni.dbAddElement("");
//        }
        comAspettoEsterioreBeni.dbAddElement("");
        comAspettoEsterioreBeni.dbOpenList(Db.getConn(), "select nome, id from tipi_aspetto_esteriore_beni group by nome", null, false);

        comVettori.dbAddElement("");
        comVettori.dbOpenList(db.getConn(), "select nome,nome from vettori order by nome", null, false);

        if (main.getPersonalContain("medcomp")) {
            //selezionare gli agenti in base a quelli collegati al cliente fornitore
            comAgente.setRenderer(new SeparatorComboBoxRenderer());
            Integer cod_cliente = null;
            try {
                if (dbIdDocu != -1) {
                    cod_cliente = cu.toInteger(dbu.getObject(Db.getConn(), "select " + ccliente + " from test_ddt" + suff + " where id = " + dbIdDocu));
                }
            } catch (Exception e) {
            }
            InvoicexUtil.caricaComboAgentiCliFor(comAgente, cod_cliente);
        } else {
            comAgente.dbOpenList(Db.getConn(), "select nome, id from agenti where id != 0 and IFNULL(nome,'') != '' order by nome", null, false);
        }

        listino_consigliato.dbAddElement("", "");
        listino_consigliato.dbOpenList(Db.getConn(), "select CONCAT(descrizione, ' [', codice, ']'), codice from tipi_listino order by descrizione");

        //this.dati.butSave = this.butSave;
        //this.dati.butUndo = this.butUndo;
        //controllo se inserimento o modifica
        if (dbStato.equals(this.DB_INSERIMENTO)) {
            this.dati.dbOpen(db.getConn(), "select * from test_ddt" + suff + " limit 0");
        } else {
            sql = "select * from test_ddt" + suff;
//            sql += " where serie = " + db.pc(dbSerie, "VARCHAR");
//            sql += " and numero = " + dbNumero;
//            //sql += " and stato = " + db.pc(prevStato, "VARCHAR");
//            sql += " and anno = " + dbAnno;
            sql += " where id = " + id;
            this.dati.dbOpen(db.getConn(), sql);
        }

        this.dati.dbRefresh();
        this.dbdoc.dbRefresh();

        //apro la combo clienti
        this.comClie.setDbTextAbbinato(this.texClie);
        this.texClie.setDbComboAbbinata(this.comClie);

//        if (Db.nz(this.comCausaleTrasporto.getSelectedItem(), "").toString().equalsIgnoreCase("TENTATA VENDITA")) {
//            this.comClie.dbOpenList(db.getConn(), "select ragione_sociale,codice from clie_forn order by ragione_sociale", null, false);
//            this.comClie.setEnabled(false);
//        } else {
        if (this.texClie.getText().equalsIgnoreCase("0")) {
            this.comClie.dbOpenList(db.getConn(), "select ragione_sociale,codice from clie_forn where ragione_sociale != '' order by ragione_sociale", null, true);
        } else {
            this.comClie.dbOpenList(db.getConn(), "select ragione_sociale,codice from clie_forn where ragione_sociale != '' order by ragione_sociale", this.texClie.getText(), true);
        }
//        }

        //apro combo destinazione cliente
        comClieDest.dbTrovaMentreScrive = false;
        sql = "select ragione_sociale, id from clie_forn_dest";
        sql += " where codice_cliente = " + Db.pc(this.texClie.getText(), "NUMERIC");
        sql += " order by ragione_sociale";
        riempiDestDiversa(sql);

        //righe
        //apro la griglia
        griglia.dbNomeTabella = getNomeTabRighe();

        java.util.Hashtable colsWidthPerc = new java.util.Hashtable();
        colsWidthPerc.put("serie", new Double(0));
        colsWidthPerc.put("numero", new Double(0));
        colsWidthPerc.put("anno", new Double(0));
        colsWidthPerc.put("stato", new Double(0));
        colsWidthPerc.put("riga", new Double(5));
        colsWidthPerc.put("articolo", new Double(15));
        colsWidthPerc.put("descrizione", new Double(45));
        colsWidthPerc.put("um", new Double(5));
        colsWidthPerc.put("quantita", new Double(10));
        colsWidthPerc.put(getCampoQtaEvasa(), new Double(10));
        colsWidthPerc.put("prezzo", new Double(12));
        colsWidthPerc.put("sconto1", new Double(0));
        colsWidthPerc.put("sconto2", new Double(0));
        colsWidthPerc.put("iva", new Double(5));
        colsWidthPerc.put("Totale", new Double(10));
        colsWidthPerc.put("Ivato", new Double(10));
        colsWidthPerc.put("Sconti", new Double(10));
        colsWidthPerc.put("id", 0d);
        colsWidthPerc.put("id_padre", 0d);
        if (main.isPluginContabilitaAttivo()) {
            colsWidthPerc.put("conto", new Double(10));
        }
        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            colsWidthPerc.put("note", 15d);
        }                

        this.griglia.columnsSizePerc = colsWidthPerc;

        java.util.Hashtable colsAlign = new java.util.Hashtable();
        colsAlign.put("quantita", "RIGHT_CURRENCY");
        colsAlign.put("prezzo", "RIGHT_CURRENCY");
        this.griglia.columnsAlign = colsAlign;
        this.griglia.flagUsaOrdinamento = false;

        //        Vector chiave2 = new Vector();
        //        chiave2.add("serie");
        //        chiave2.add("numero");
        //        chiave2.add("anno");
        //        chiave2.add("riga");
        Vector chiave2 = new Vector();
        chiave2.add("id");
        this.griglia.dbChiave = chiave2;

        //this.griglia.dbPanel=this.dati;
        //controllo come devo aprire
        if (dbStato.equals(frmTestDocu.DB_INSERIMENTO)) {
            inserimento();
            SimpleDateFormat f1 = new SimpleDateFormat("dd/MM/yy HH:mm");
            texDataOra.setText(f1.format(new java.util.Date()));
            texData.setEditable(true);

            InvoicexUtil.fireEvent(this, InvoicexEvent.TYPE_FRMTESTDDT_INIT_INSERIMENTO);

        } else {

            //disabilito la data perch??? non tornerebbe pi??? la chiave per numero e anno
            this.texData.setEditable(false);
            this.dbdoc.sconto1 = Db.getDouble(this.texScon1.getText());
            this.dbdoc.sconto2 = Db.getDouble(this.texScon2.getText());
            this.dbdoc.sconto3 = Db.getDouble(this.texScon3.getText());

            //this.prev.speseVarie = Db.getDouble(this.texSpesVari.getText());
            this.dbdoc.speseTrasportoIva = Db.getDouble(this.texSpeseTrasporto.getText());
            this.dbdoc.speseIncassoIva = Db.getDouble(this.texSpeseIncasso.getText());
            dopoInserimento();

        }

//        java.util.prefs.Preferences preferences = java.util.prefs.Preferences.userNodeForPackage(main.class);
//        boolean azioniPericolose = preferences.getBoolean("azioniPericolose", false);
        boolean azioniPericolose = main.fileIni.getValueBoolean("pref", "azioniPericolose", true);

        if (azioniPericolose) {
            texNume.setEditable(true);
            texData.setEditable(true);
        }

        if (!dbStato.equals(DB_INSERIMENTO)) {
            if ("S".equalsIgnoreCase(String.valueOf(dati.dbGetField("evaso")))) {
                stato_evasione.setSelectedIndex(0);
            } else if ("P".equalsIgnoreCase(String.valueOf(dati.dbGetField("evaso")))) {
                stato_evasione.setSelectedIndex(1);
            } else {
                stato_evasione.setSelectedIndex(2);
            }
        } else {
            stato_evasione.setSelectedIndex(2);
        }

        texCliente.requestFocus();

        prezzi_ivati_virtual.setSelected(prezzi_ivati.isSelected());

        dati.dbCheckModificatiReset();

        if (dbStato.equalsIgnoreCase(DB_INSERIMENTO)) {
            texSconto.setText("0");
            texAcconto.setText("0");
        } else {
            texSconto.setText(FormatUtils.formatEuroIta(CastUtils.toDouble0(dati.dbGetField("sconto"))));
            texAcconto.setText(FormatUtils.formatEuroIta(CastUtils.toDouble0(dati.dbGetField("acconto"))));
            ricalcolaTotali();
        }

        //allegati
        InvoicexEvent evt = new InvoicexEvent(frmTestDocu.this, InvoicexEvent.TYPE_AllegatiInit);
        evt.args = new Object[]{tab};
        main.events.fireInvoicexEvent(evt);
        InvoicexUtil.fireEvent(frmTestDocu.this, InvoicexEvent.TYPE_AllegatiCarica, dati.dbNomeTabella, id);

        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        loading = false;
    }

    private void inserimento() {

        //oggetto preventivo
        this.dati.dbNew();

        //prendo base da impostazioni
        boolean prezzi_ivati_b = false;
        try {
            String prezzi_ivati_s = CastUtils.toString(DbUtils.getObject(Db.getConn(), "select l.prezzi_ivati from dati_azienda a join tipi_listino l on a.listino_base = l.codice"));
            if (prezzi_ivati_s.equalsIgnoreCase("S")) {
                prezzi_ivati_b = true;
            }
            prezzi_ivati.setSelected(prezzi_ivati_b);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (main.iniSerie == false) {
            assegnaNumero();
            dopoInserimento();
            listino_consigliato.dbTrovaKey(main.fileIni.getValue("pref", "listinoConsigliatoDdt", ""));
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    texCliente.grabFocus();
                }
            });
        } else {

            //disabilitare tutto prima
            Component[] cs = this.dati.getComponents();
            for (int i = 0; i < cs.length; i++) {
                cs[i].setEnabled(false);
                if (cs[i] instanceof tnxbeans.tnxComboField) {
                    tnxbeans.tnxComboField combo = (tnxbeans.tnxComboField) cs[i];
                    combo.setEditable(false);
                    combo.setLocked(true);
                }
            }
            cs = dati_dest_diversa.getComponents();
            for (int i = 0; i < cs.length; i++) {
                cs[i].setEnabled(false);
                if (cs[i] instanceof tnxbeans.tnxComboField) {
                    tnxbeans.tnxComboField combo = (tnxbeans.tnxComboField) cs[i];
                    combo.setEditable(false);
                    combo.setLocked(true);
                }
            }
            listino_consigliato.setSelectedItem(main.fileIni.getValue("pref", "listinoConsigliatoDdt", ""));
            this.texSeri.setToolTipText("Inserisci la serie e premi Invio per confermarla ed assegnare un numero al documento");
            this.texSeri.setEnabled(true);
            this.texSeri.setEditable(true);
            this.texSeri.setBackground(java.awt.Color.RED);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    texSeri.requestFocus();
                }
            });
        }
    }

    private String getAnno() {
        try {
            SimpleDateFormat datef = null;
            if (texData.getText().length() == 8 || texData.getText().length() == 7) {
                datef = new SimpleDateFormat("dd/MM/yy");
            } else if (texData.getText().length() == 10 || texData.getText().length() == 9) {
                datef = new SimpleDateFormat("dd/MM/yyyy");
            } else {
                return "";
            }
            Calendar cal = Calendar.getInstance();
            datef.setLenient(true);
            cal.setTime(datef.parse(texData.getText()));
            return String.valueOf(cal.get(Calendar.YEAR));
        } catch (Exception err) {
            return "";
        }
    }

    private void dopoInserimento() {

        controllaPermessiAnagCliFor();

        dbAssociaGrigliaRighe();
        doc.table_righe_temp = table_righe_temp;
        doc.load(Db.INSTANCE, this.dbdoc.numero, this.dbdoc.serie, this.dbdoc.anno, getTipoDoc(), id);
        dbdoc.setId(id);

        if (comClie.getText().trim().length() == 0) {
            try {
                String cliente = (String) DbUtils.getObject(Db.getConn(), "select ragione_sociale from clie_forn where codice = " + Db.pc(texClie.getText(), Types.INTEGER));
                texCliente.setText(cliente);
            } catch (Exception e) {
            }
        } else {
            texCliente.setText(comClie.getText());
        }

        //provo a fare timer per aggiornare prezzo totale
        //        tim = new java.util.Timer();
        //        timerRefreshPreventivo timTest = new timerRefreshPreventivo(this, doc);
        //        tim.schedule(timTest,1000,500);
        ricalcolaTotali();

        dbdoc.dbRefresh();
        totaleIniziale = dbdoc.totale;
//        totaleIniziale = doc.getTotale();

        //nascondo la targa all'avvio che serve solo per i ddt di tentata venmdita
        visualizzaTarga();

        //debu cliente
        System.out.println("cliente4:" + this.texClie.getText());

    }
    
    private void assegnaNumero() {

        //metto ultimo numero preventivo + 1
        //apre il resultset per ultimo +1
        java.sql.Statement stat;
        ResultSet resu;

        try {
            stat = db.getConn().createStatement();

            String sql = "select numero from test_ddt" + suff;

            sql += " where anno = " + java.util.Calendar.getInstance().get(Calendar.YEAR);
            sql += " and serie = " + Db.pc(this.texSeri.getText(), Types.VARCHAR);
            sql += " order by numero desc limit 1";
            resu = stat.executeQuery(sql);

            if (resu.next() == true) {
                this.texNume.setText(String.valueOf(resu.getInt(1) + 1));
            } else {
                this.texNume.setText("1");
            }

            dati.setCampiAggiuntivi(new Hashtable());
            dati.getCampiAggiuntivi().put("sconto", Db.pc(doc.getSconto(), Types.DOUBLE));
            dati.getCampiAggiuntivi().put("totale_imponibile_pre_sconto", Db.pc(doc.totaleImponibilePreSconto, Types.DOUBLE));
            dati.getCampiAggiuntivi().put("totale_ivato_pre_sconto", Db.pc(doc.totaleIvatoPreSconto, Types.DOUBLE));
            dati.getCampiAggiuntivi().put("evaso", Db.pc(getStatoEvaso(), Types.VARCHAR));
            dati.getCampiAggiuntivi().put("acconto", Db.pc(Db.getDouble(texAcconto.getText()), Types.DOUBLE));

            this.texAnno.setText(String.valueOf(java.util.Calendar.getInstance().get(Calendar.YEAR)));

            //-----------------------------------------------------------------
            //se apre in inserimento gli faccio subito salvare la testa
            //se poi la annulla vado ad eliminare
            //appoggio totali
            this.texTota1.setText(this.texTota.getText());
            this.texTotaIva1.setText(this.texTotaIva.getText());
            this.texTotaImpo1.setText(this.texTotaImpo.getText());

//            texClie.setText("0");
            if (this.dati.dbStato.equals(DB_INSERIMENTO)) {
                try {
                    String tmpSerie = this.texSeri.getText();
                    Integer numero = Integer.parseInt(texNume.getText());
                    Integer anno = Integer.parseInt(texAnno.getText());

                    String tmpSql = "select * from test_ddt" + suff + " where serie = '" + tmpSerie + "' and anno = " + anno + " and numero = " + numero;
                    ResultSet tmpControl = Db.openResultSet(tmpSql);

                    if (tmpControl.next()) {
                        JOptionPane.showMessageDialog(this, "Un' altro documento con lo stesso gruppo numero - serie - anno è già stato inserito!", "Impossibile inserire dati", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!main.edit_doc_in_temp) {
                if (this.dati.dbSave() == true) {
                    //richiamo il refresh della maschera che lo ha lanciato
                    if (from != null) {
                        frmElenDDT temp = (frmElenDDT) from;
                        temp.dbRefresh();
                    }
                }

                this.id = (Integer) dati.last_inserted_id;
                this.dbdoc.setId(id);
                if (dbStato.equalsIgnoreCase(tnxDbPanel.DB_INSERIMENTO)) {
                    InvoicexUtil.checkLock(dati.dbNomeTabella, id, false, null);
                }
                InvoicexUtil.checkLockAddFrame(this, dati.dbNomeTabella, id);

            }

            this.dbdoc.serie = this.texSeri.getText();
            this.dbdoc.stato = "P";
            this.dbdoc.numero = new Integer(this.texNume.getText()).intValue();
            this.dbdoc.anno = java.util.Calendar.getInstance().get(Calendar.YEAR);

            if (!main.edit_doc_in_temp) {
                this.dati.dbCambiaStato(this.dati.DB_LETTURA);
            }
            try {
                String chiaveNote = acquisto ? "noteStandardDdtAcquisto" : "noteStandardDdt";
                texNote.setText(main.fileIni.getValue("pref", chiaveNote));
            } catch (Exception err) {
                err.printStackTrace();
            }
            this.texSeri.setEditable(false);
            this.texSeri.setBackground(this.texNume.getBackground());

            //-----------------------------------------------------------------
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
    
    
    private boolean saveDocumento() {    
        SwingUtils.mouse_wait(this);

        //provo a non ricalcolare
        //prev.dbRicalcolaProgressivo(dbStato, this.texData.getText(), this.texNumePrev, this.texAnno);
        //sposto i totali di modo che li salvi
        this.texTota1.setText(this.texTota.getText());
        this.texTotaImpo1.setText(this.texTotaImpo.getText());
        this.texTotaIva1.setText(this.texTotaIva.getText());
        
        //storico
        Storico.scrivi("Salva Documento", "Documento = " + (acquisto ? Db.TIPO_DOCUMENTO_DDT_ACQUISTO : Db.TIPO_DOCUMENTO_DDT) + "/" + this.texSeri.getText() + "/" + this.dbdoc.numero + "/" + this.dbdoc.anno + ", Pagamento = " + this.comPaga.getText() + ", Importo documento = " + this.texTota1.getText());

        dati.setCampiAggiuntivi(new Hashtable());
        dati.getCampiAggiuntivi().put("evaso", Db.pc(getStatoEvaso(), Types.VARCHAR));
        dati.getCampiAggiuntivi().put("sconto", Db.pc(doc.getSconto(), Types.DOUBLE));
        dati.getCampiAggiuntivi().put("totale_imponibile_pre_sconto", Db.pc(doc.totaleImponibilePreSconto, Types.DOUBLE));
        dati.getCampiAggiuntivi().put("totale_ivato_pre_sconto", Db.pc(doc.totaleIvatoPreSconto, Types.DOUBLE));
        dati.getCampiAggiuntivi().put("acconto", Db.pc(Db.getDouble(texAcconto.getText()), Types.DOUBLE));

        //salvo altrimenti genera le scadenze sull'importo vuoto
        if (!main.edit_doc_in_temp) {
            this.dati.dbSave();
        } else {
            Sync.saveDoc(suff, texSeri.getText(), texNume.getText(), texAnno.getText(), id, this, dati, table_righe_temp, table_righe_lotti_temp, table_righe_matricole_temp);
            if (dati.id != null) {  //in inserimento viene avvalorato con il nuovo id di testata
                id = dati.id;
            }
        }

        //forzo anno in base alla data
        if (!main.edit_doc_in_temp) {
            InvoicexUtil.aggiornaAnnoDaData(acquisto ? Db.TIPO_DOCUMENTO_DDT_ACQUISTO : Db.TIPO_DOCUMENTO_DDT, id);
        }

//        //forzo gli id padre
//        String serie = this.texSeri.getText();
//        Integer numero = Integer.parseInt(texNume.getText());
//        Integer anno = Integer.parseInt(texAnno.getText());
//        String sql = "UPDATE righ_ddt r left join test_ddt t on r.serie = t.serie and r.anno = t.anno and r.numero = t.numero set r.id_padre = t.id where t.serie = " + Db.pc(serie, Types.VARCHAR) + " and t.numero = " + numero + " and t.anno = " + anno;
//        try {
//            DbUtils.tryExecQuery(Db.getConn(), sql);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        System.out.println("************** id per generamovimenti = " + id);
        dbdoc.setId(id);
        if (this.dbdoc.generaMovimentiMagazzino() == false) {
            SwingUtils.mouse_def(this);
            javax.swing.JOptionPane.showMessageDialog(this, "Errore nella generazione dei movimenti di magazzino", "Errore", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        InvoicexEvent event = new InvoicexEvent(this);
        event.type = InvoicexEvent.TYPE_AllegatiSalva;
        event.args = new Object[]{dati.dbNomeTabella, id};
        try {
            Object ret = main.events.fireInvoicexEventWResult(event);
            if (ret != null && ret instanceof Boolean && ((Boolean) ret)) {
                chiudere = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //aggiorno eventuali documenti collegati (ordini, ddt)
        InvoicexUtil.aggiornaRiferimentoDocumenti(getTipoDoc(), id);        
        
        return true;
    }

    private void caricaDestinazioneDiversa() {

        String sql = "select * from clie_forn_dest";
        sql += " where codice_cliente = " + Db.pc(this.texClie.getText(), Types.INTEGER);
        sql += " and id = " + Db.pc(this.texClieDest.getText(), Types.INTEGER);

        ResultSet dest = Db.openResultSet(sql);

        try {
            if (dest.next()) {
                texDestRagioneSociale.setText(dest.getString("ragione_sociale"));
                texDestIndirizzo.setText(dest.getString("indirizzo"));
                texDestCap.setText(dest.getString("cap"));
                texDestLocalita.setText(dest.getString("localita"));
                texDestProvincia.setText(dest.getString("provincia"));
                texDestTelefono.setText(dest.getString("telefono"));
                texDestCellulare.setText(dest.getString("cellulare"));
                comPaese.dbTrovaKey(dest.getString("paese"));
            } else {
                texDestRagioneSociale.setText("");
                texDestIndirizzo.setText("");
                texDestCap.setText("");
                texDestLocalita.setText("");
                texDestProvincia.setText("");
                texDestTelefono.setText("");
                texDestCellulare.setText("");
                comPaese.setSelectedIndex(-1);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to
     *
     * initialize the form.
     *
     * WARNING: Do NOT modify this code. The content of this method is
     *
     * always regenerated by the Form Editor.
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popGrig = new javax.swing.JPopupMenu();
        popGrigModi = new javax.swing.JMenuItem();
        popGrigElim = new javax.swing.JMenuItem();
        popGridAdd = new javax.swing.JMenuItem();
        popDuplicaRighe = new javax.swing.JMenuItem();
        menColAgg = new javax.swing.JMenu();
        menColAggNote = new javax.swing.JCheckBoxMenuItem();
        labScon7 = new javax.swing.JLabel();
        texScon7 = new tnxbeans.tnxTextField();
        labScon6 = new javax.swing.JLabel();
        labScon4 = new javax.swing.JLabel();
        texScon9 = new tnxbeans.tnxTextField();
        labScon5 = new javax.swing.JLabel();
        texScon10 = new tnxbeans.tnxTextField();
        jPanel5 = new javax.swing.JPanel();
        texScon6 = new tnxbeans.tnxTextField();
        labScon8 = new javax.swing.JLabel();
        texScon8 = new tnxbeans.tnxTextField();
        labScon3 = new javax.swing.JLabel();
        labScon9 = new javax.swing.JLabel();
        texScon5 = new tnxbeans.tnxTextField();
        texScon4 = new tnxbeans.tnxTextField();
        menClientePopup = new javax.swing.JPopupMenu();
        menClienteNuovo = new javax.swing.JMenuItem();
        menClienteModifica = new javax.swing.JMenuItem();
        panDati = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        datiRighe = new tnxbeans.tnxDbPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        griglia = getGrigliaInitComp();
        jPanel1 = new javax.swing.JPanel();
        prezzi_ivati_virtual = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        butNuovArti = new javax.swing.JButton();
        butInserisciPeso = new javax.swing.JButton();
        butImportRighe = new javax.swing.JButton();
        butImportXlsCirri = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        butPdf = new javax.swing.JButton();
        butStampa = new javax.swing.JButton();
        butUndo = new javax.swing.JButton();
        butSave = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        stato_evasione = new javax.swing.JComboBox();
        jSeparator4 = new javax.swing.JSeparator();
        jPanel4 = new javax.swing.JPanel();
        texTotaImpo = new tnxbeans.tnxTextField();
        texTotaIva = new tnxbeans.tnxTextField();
        texTota = new tnxbeans.tnxTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        texSconto = new tnxbeans.tnxTextField();
        jLabel27 = new javax.swing.JLabel();
        texAcconto = new tnxbeans.tnxTextField();
        texTotaDaPagareFinale = new tnxbeans.tnxTextField();
        jLabel3 = new javax.swing.JLabel();
        tab = new javax.swing.JTabbedPane();
        scrollDati = new javax.swing.JScrollPane();
        dati = new tnxbeans.tnxDbPanel();
        texNume = new tnxbeans.tnxTextField();
        texClie = new tnxbeans.tnxTextField();
        texClie.setVisible(false);
        texSpeseIncasso = new tnxbeans.tnxTextField();
        texScon2 = new tnxbeans.tnxTextField();
        texScon1 = new tnxbeans.tnxTextField();
        comClie = new tnxbeans.tnxComboField();
        comClie.setVisible(false);
        texTotaImpo1 = new tnxbeans.tnxTextField();
        texTotaImpo1.setVisible(false);
        texTotaIva1 = new tnxbeans.tnxTextField();
        texTotaIva1.setVisible(false);
        texTota1 = new tnxbeans.tnxTextField();
        texTota1.setVisible(false);
        texNote = new tnxbeans.tnxMemoField();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        labScon1 = new javax.swing.JLabel();
        labScon2 = new javax.swing.JLabel();
        jLabel113 = new javax.swing.JLabel();
        texData = new tnxbeans.tnxTextField();
        jLabel11 = new javax.swing.JLabel();
        texStat = new tnxbeans.tnxTextField();
        texStat.setVisible(false);
        jLabel12 = new javax.swing.JLabel();
        texScon3 = new tnxbeans.tnxTextField();
        labScon21 = new javax.swing.JLabel();
        jLabel151 = new javax.swing.JLabel();
        texSeri = new tnxbeans.tnxTextField();
        texAnno = new tnxbeans.tnxTextField();
        texAnno.setVisible(false);
        texClieDest = new tnxbeans.tnxTextField();
        texClieDest.setVisible(false);
        jLabel18 = new javax.swing.JLabel();
        texNumeroColli = new tnxbeans.tnxTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        comPorto = new tnxbeans.tnxComboField();
        jLabel4 = new javax.swing.JLabel();
        comCausaleTrasporto = new tnxbeans.tnxComboField();
        texSpeseTrasporto = new tnxbeans.tnxTextField();
        jLabel114 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        comMezzoTrasporto = new tnxbeans.tnxComboField();
        texRiferimento = new tnxbeans.tnxTextField();
        butPrezziPrec = new javax.swing.JButton();
        comAspettoEsterioreBeni = new tnxbeans.tnxComboField();
        jLabel19 = new javax.swing.JLabel();
        comPaga = new tnxbeans.tnxComboField();
        comVettori = new tnxbeans.tnxComboField();
        texDataOra = new tnxbeans.tnxTextField();
        jLabel6 = new javax.swing.JLabel();
        stampa_prezzi = new tnxbeans.tnxCheckBox();
        labAgente = new javax.swing.JLabel();
        comAgente = new tnxbeans.tnxComboField();
        labProvvigione = new javax.swing.JLabel();
        texProvvigione = new tnxbeans.tnxTextField();
        labPercentoProvvigione = new javax.swing.JLabel();
        labRiferimento = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        texPesoLordo = new tnxbeans.tnxTextField();
        texPesoNetto = new tnxbeans.tnxTextField();
        jLabel24 = new javax.swing.JLabel();
        prezzi_ivati = new tnxbeans.tnxCheckBox();
        texCliente = new javax.swing.JTextField();
        apriclienti = new BasicArrowButton(BasicArrowButton.SOUTH, UIManager.getColor("ComboBox.buttonBackground"), UIManager.getColor("ComboBox.buttonShadow"), UIManager.getColor("ComboBox.buttonDarkShadow"), UIManager.getColor("ComboBox.buttonHighlight"));
        butAddClie = new javax.swing.JButton();
        labModConsegna = new javax.swing.JLabel();
        comConsegna = new tnxbeans.tnxComboField();
        labModScarico = new javax.swing.JLabel();
        comScarico = new tnxbeans.tnxComboField();
        sepDestMerce = new javax.swing.JSeparator();
        dati_dest_diversa = new tnxbeans.tnxDbPanel();
        comClieDest = new tnxbeans.tnxComboField();
        labScon10 = new javax.swing.JLabel();
        texDestRagioneSociale = new tnxbeans.tnxTextField();
        labScon11 = new javax.swing.JLabel();
        texDestIndirizzo = new tnxbeans.tnxTextField();
        labScon12 = new javax.swing.JLabel();
        texDestCap = new tnxbeans.tnxTextField();
        labScon13 = new javax.swing.JLabel();
        texDestLocalita = new tnxbeans.tnxTextField();
        labScon14 = new javax.swing.JLabel();
        texDestProvincia = new tnxbeans.tnxTextField();
        labScon16 = new javax.swing.JLabel();
        texDestTelefono = new tnxbeans.tnxTextField();
        labScon15 = new javax.swing.JLabel();
        texDestCellulare = new tnxbeans.tnxTextField();
        labScon17 = new javax.swing.JLabel();
        comPaese = new tnxbeans.tnxComboField();
        jSeparator2 = new javax.swing.JSeparator();
        datiAltro = new tnxbeans.tnxDbPanel();
        jLabel8 = new javax.swing.JLabel();
        listino_consigliato = new tnxbeans.tnxComboField();
        jSeparator3 = new javax.swing.JSeparator();
        pan_segnaposto_depositi = new javax.swing.JPanel();
        labNoteConsegna = new javax.swing.JLabel();
        texNoteConsegna = new tnxbeans.tnxMemoField();
        labCampoLibero1 = new javax.swing.JLabel();
        comCampoLibero1 = new tnxbeans.tnxComboField();

        popGrigModi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/apps/accessories-text-editor.png"))); // NOI18N
        popGrigModi.setText("modifica riga");
        popGrigModi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popGrigModiActionPerformed(evt);
            }
        });
        popGrig.add(popGrigModi);

        popGrigElim.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/places/user-trash.png"))); // NOI18N
        popGrigElim.setText("elimina");
        popGrigElim.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popGrigElimActionPerformed(evt);
            }
        });
        popGrig.add(popGrigElim);

        popGridAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/list-add.png"))); // NOI18N
        popGridAdd.setLabel("Inserisci nuova riga fra");
        popGridAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popGridAddActionPerformed(evt);
            }
        });
        popGrig.add(popGridAdd);

        popDuplicaRighe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/edit-copy.png"))); // NOI18N
        popDuplicaRighe.setText("Duplica");
        popDuplicaRighe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popDuplicaRigheActionPerformed(evt);
            }
        });
        popGrig.add(popDuplicaRighe);

        menColAgg.setText("Colonne Aggiuntive");

        menColAggNote.setText("Note");
        menColAggNote.setToolTipText("Abilitato solo per versioni PRO o superiore");
        menColAggNote.setIcon(new javax.swing.ImageIcon(getClass().getResource("/it/tnx/invoicex/res/ico_pro.png"))); // NOI18N
        menColAggNote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menColAggNoteActionPerformed(evt);
            }
        });
        menColAgg.add(menColAggNote);

        popGrig.add(menColAgg);

        labScon7.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        labScon7.setText("prov.");
        labScon7.setToolTipText("");

        texScon7.setToolTipText("");
        texScon7.setDbDescCampo("");
        texScon7.setDbNomeCampo("dest_localita");
        texScon7.setDbTipoCampo("");
        texScon7.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        labScon6.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        labScon6.setText("loc.");
        labScon6.setToolTipText("");

        labScon4.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        labScon4.setText("indirizzo");
        labScon4.setToolTipText("");

        texScon9.setToolTipText("");
        texScon9.setDbDescCampo("");
        texScon9.setDbNomeCampo("dest_telefono");
        texScon9.setDbTipoCampo("");
        texScon9.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        labScon5.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        labScon5.setText("cap");
        labScon5.setToolTipText("");

        texScon10.setToolTipText("");
        texScon10.setDbDescCampo("");
        texScon10.setDbNomeCampo("dest_cellulare");
        texScon10.setDbTipoCampo("");
        texScon10.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        jPanel5.setBackground(new java.awt.Color(204, 204, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "destinazione diversa (manuale)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 10))); // NOI18N
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        texScon6.setToolTipText("");
        texScon6.setDbDescCampo("");
        texScon6.setDbNomeCampo("dest_cap");
        texScon6.setDbTipoCampo("");
        texScon6.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        labScon8.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        labScon8.setText("telefono");
        labScon8.setToolTipText("");

        texScon8.setToolTipText("");
        texScon8.setDbDescCampo("");
        texScon8.setDbNomeCampo("dest_provincia");
        texScon8.setDbTipoCampo("");
        texScon8.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        labScon3.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        labScon3.setText("ragione sociale");
        labScon3.setToolTipText("");

        labScon9.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        labScon9.setText("cellulare");
        labScon9.setToolTipText("");

        texScon5.setToolTipText("");
        texScon5.setDbDescCampo("");
        texScon5.setDbNomeCampo("dest_indirizzo");
        texScon5.setDbTipoCampo("");
        texScon5.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        texScon4.setToolTipText("");
        texScon4.setDbDescCampo("");
        texScon4.setDbNomeCampo("dest_ragione_sociale");
        texScon4.setDbTipoCampo("");
        texScon4.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        menClienteNuovo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/contact-new.png"))); // NOI18N
        menClienteNuovo.setText("Nuova Anagrafica");
        menClienteNuovo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menClienteNuovoActionPerformed(evt);
            }
        });
        menClientePopup.add(menClienteNuovo);

        menClienteModifica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/apps/accessories-text-editor.png"))); // NOI18N
        menClienteModifica.setText("Modifica Anagrafica");
        menClienteModifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menClienteModificaActionPerformed(evt);
            }
        });
        menClientePopup.add(menClienteModifica);

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Documento");
        addInternalFrameListener(new javax.swing.event.InternalFrameListener() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameClosed(evt);
            }
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameOpened(evt);
            }
        });
        addVetoableChangeListener(new java.beans.VetoableChangeListener() {
            public void vetoableChange(java.beans.PropertyChangeEvent evt)throws java.beans.PropertyVetoException {
                formVetoableChange(evt);
            }
        });

        panDati.setLayout(new java.awt.BorderLayout());

        jSplitPane1.setBorder(null);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        datiRighe.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(null);

        griglia.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                grigliaMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                grigliaMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                grigliaMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(griglia);

        datiRighe.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        prezzi_ivati_virtual.setText("Prezzi IVA inclusa");
        prezzi_ivati_virtual.setToolTipText("<html>\nSelezionando questa opzione verrà effettuato lo scorporo IVA soltanto a fine documento e non riga per riga, inoltre<br>\nverranno presentati in stampa gli importi di riga già ivati invece che gli imponibili.<br>\n<br>\nL'esempio più lampante è questo:<br>\n<br>\nArticolo di prezzo <b>10,00</b> € (iva inclusa del 21%)<br>\n- Senza la scelta 'Prezzi IVA inclusa' il totale fattura verrà <b>9,99</b> € perchè:<br>\nlo scorporo di 10,00 € genera un imponibile di 8,26 il quale applicando l'iva 21% (1,73 €) genererà un totale di 9,99 €<br>\n- Con la scelta 'Prezzi IVA inclusa' il totale fattura verrà direttamente <b>10,00</b> € e verrà calcolato l'imponibile facendo la<br>\nsottrazione tra il totale e l'iva derivante dallo scorporo del totale già ivato.<br>\n</html>");
        prezzi_ivati_virtual.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        prezzi_ivati_virtual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prezzi_ivati_virtualActionPerformed(evt);
            }
        });
        jPanel1.add(prezzi_ivati_virtual);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setPreferredSize(new java.awt.Dimension(6, 20));
        jPanel1.add(jSeparator1);

        butNuovArti.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/document-new.png"))); // NOI18N
        butNuovArti.setText("Inserisci nuova riga");
        butNuovArti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butNuovArtiActionPerformed(evt);
            }
        });
        jPanel1.add(butNuovArti);

        butInserisciPeso.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/document-new.png"))); // NOI18N
        butInserisciPeso.setText("Inserisci Peso");
        butInserisciPeso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butInserisciPesoActionPerformed(evt);
            }
        });
        jPanel1.add(butInserisciPeso);

        butImportRighe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/document-new.png"))); // NOI18N
        butImportRighe.setText("Importa Righe Da CSV");
        butImportRighe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butImportRigheActionPerformed(evt);
            }
        });
        jPanel1.add(butImportRighe);

        butImportXlsCirri.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/document-new.png"))); // NOI18N
        butImportXlsCirri.setText("Import xls");
        butImportXlsCirri.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butImportXlsCirriActionPerformed(evt);
            }
        });
        jPanel1.add(butImportXlsCirri);

        datiRighe.add(jPanel1, java.awt.BorderLayout.NORTH);

        jPanel2.setLayout(new java.awt.GridLayout(1, 0));

        jPanel3.setLayout(new java.awt.GridLayout(0, 1));

        butPdf.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/pdf-icon-16.png"))); // NOI18N
        butPdf.setText("Crea PDF");
        butPdf.setMargin(new java.awt.Insets(2, 4, 2, 4));
        butPdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butPdfActionPerformed(evt);
            }
        });

        butStampa.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/document-print.png"))); // NOI18N
        butStampa.setText("Stampa");
        butStampa.setMargin(new java.awt.Insets(2, 4, 2, 4));
        butStampa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butStampaActionPerformed(evt);
            }
        });

        butUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/edit-undo.png"))); // NOI18N
        butUndo.setText("Annulla");
        butUndo.setMargin(new java.awt.Insets(2, 4, 2, 4));
        butUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butUndoActionPerformed(evt);
            }
        });

        butSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/devices/media-floppy.png"))); // NOI18N
        butSave.setText("Salva");
        butSave.setMargin(new java.awt.Insets(2, 4, 2, 4));
        butSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveActionPerformed(evt);
            }
        });

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Stato");

        stato_evasione.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Evaso", "Evaso Parziale", "Non Evaso" }));
        stato_evasione.setSelectedIndex(2);

        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(butPdf, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, butStampa, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(butUndo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 97, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(butSave, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 97, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(stato_evasione, 0, 132, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(5, 5, 5)
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 101, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(butUndo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(butSave, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(butStampa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jLabel7)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(stato_evasione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(butPdf)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.add(jPanel7);

        jPanel2.add(jPanel3);

        texTotaImpo.setEditable(false);
        texTotaImpo.setBorder(null);
        texTotaImpo.setColumns(10);
        texTotaImpo.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        texTotaImpo.setText("0");
        texTotaImpo.setDbTipoCampo("valuta");
        texTotaImpo.setFont(texTotaImpo.getFont());

        texTotaIva.setEditable(false);
        texTotaIva.setBorder(null);
        texTotaIva.setColumns(10);
        texTotaIva.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        texTotaIva.setText("0");
        texTotaIva.setDbTipoCampo("valuta");
        texTotaIva.setFont(texTotaIva.getFont());

        texTota.setEditable(false);
        texTota.setBorder(null);
        texTota.setColumns(10);
        texTota.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        texTota.setText("0");
        texTota.setDbTipoCampo("valuta");
        texTota.setFont(texTota.getFont().deriveFont(texTota.getFont().getStyle() | java.awt.Font.BOLD, texTota.getFont().getSize()+1));

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD, jLabel2.getFont().getSize()+1));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Totale");

        jLabel21.setFont(jLabel21.getFont());
        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel21.setText("Totale Iva");

        jLabel22.setFont(jLabel22.getFont());
        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel22.setText("Totale Imponibile");

        jLabel26.setFont(jLabel26.getFont());
        jLabel26.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel26.setText("Sconto");

        texSconto.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texSconto.setColumns(10);
        texSconto.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        texSconto.setText("0");
        texSconto.setDbTipoCampo("valuta");
        texSconto.setFont(texSconto.getFont());
        texSconto.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texScontoKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                texScontoKeyReleased(evt);
            }
        });

        jLabel27.setFont(jLabel27.getFont());
        jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel27.setText("Acconto");

        texAcconto.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texAcconto.setColumns(10);
        texAcconto.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        texAcconto.setText("0");
        texAcconto.setDbTipoCampo("valuta");
        texAcconto.setFont(texAcconto.getFont());
        texAcconto.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texAccontoKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                texAccontoKeyReleased(evt);
            }
        });

        texTotaDaPagareFinale.setEditable(false);
        texTotaDaPagareFinale.setBorder(null);
        texTotaDaPagareFinale.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        texTotaDaPagareFinale.setText("0");
        texTotaDaPagareFinale.setDbTipoCampo("valuta");
        texTotaDaPagareFinale.setFont(texTotaDaPagareFinale.getFont().deriveFont(texTotaDaPagareFinale.getFont().getStyle() | java.awt.Font.BOLD, texTotaDaPagareFinale.getFont().getSize()+1));

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD, jLabel3.getFont().getSize()+1));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Tot. da Pagare");

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(0, 143, Short.MAX_VALUE)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(5, 5, 5)
                        .add(texTotaDaPagareFinale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(5, 5, 5)
                        .add(texAcconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(5, 5, 5)
                        .add(texTota, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(5, 5, 5)
                        .add(texTotaIva, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(5, 5, 5)
                        .add(texTotaImpo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(5, 5, 5)
                        .add(texSconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(new java.awt.Component[] {texAcconto, texSconto, texTota, texTotaDaPagareFinale, texTotaImpo, texTotaIva}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(2, 2, 2)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texSconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(1, 1, 1)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texTotaImpo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(1, 1, 1)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texTotaIva, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(1, 1, 1)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texTota, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(1, 1, 1)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texAcconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texTotaDaPagareFinale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2))
        );

        jPanel2.add(jPanel4);

        datiRighe.add(jPanel2, java.awt.BorderLayout.SOUTH);

        jSplitPane1.setBottomComponent(datiRighe);

        scrollDati.setBorder(null);
        scrollDati.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        texNume.setEditable(false);
        texNume.setColumns(4);
        texNume.setText("numero");
        texNume.setDbDescCampo("");
        texNume.setDbNomeCampo("numero");
        texNume.setDbTipoCampo("testo");
        texNume.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                texNumeFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                texNumeFocusLost(evt);
            }
        });

        texClie.setText("cliente");
        texClie.setDbComboAbbinata(comClie);
        texClie.setDbDefault("vuoto");
        texClie.setDbDescCampo("");
        texClie.setDbNomeCampo("cliente");
        texClie.setDbNullSeVuoto(true);
        texClie.setDbTipoCampo("");
        texClie.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texClieFocusLost(evt);
            }
        });
        texClie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texClieActionPerformed(evt);
            }
        });
        texClie.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texClieKeyPressed(evt);
            }
        });

        texSpeseIncasso.setColumns(6);
        texSpeseIncasso.setText("spese_incasso");
        texSpeseIncasso.setDbDescCampo("");
        texSpeseIncasso.setDbNomeCampo("spese_incasso");
        texSpeseIncasso.setDbTipoCampo("valuta");
        texSpeseIncasso.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texSpeseIncassoFocusLost(evt);
            }
        });
        texSpeseIncasso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texSpeseIncassoActionPerformed(evt);
            }
        });
        texSpeseIncasso.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                texSpeseIncassoKeyReleased(evt);
            }
        });

        texScon2.setColumns(4);
        texScon2.setText("sconto2");
        texScon2.setToolTipText("secondo sconto");
        texScon2.setDbDescCampo("");
        texScon2.setDbNomeCampo("sconto2");
        texScon2.setDbTipoCampo("numerico");
        texScon2.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texScon2FocusLost(evt);
            }
        });
        texScon2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texScon2ActionPerformed(evt);
            }
        });
        texScon2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texScon2KeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                texScon2KeyReleased(evt);
            }
        });

        texScon1.setColumns(4);
        texScon1.setText("sconto1");
        texScon1.setToolTipText("primo sconto");
        texScon1.setDbDescCampo("");
        texScon1.setDbNomeCampo("sconto1");
        texScon1.setDbTipoCampo("numerico");
        texScon1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texScon1FocusLost(evt);
            }
        });
        texScon1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texScon1ActionPerformed(evt);
            }
        });
        texScon1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texScon1KeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                texScon1KeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                texScon1KeyTyped(evt);
            }
        });

        comClie.setDbNomeCampo("");
        comClie.setDbRiempire(false);
        comClie.setDbSalvare(false);
        comClie.setDbTextAbbinato(texClie);
        comClie.setDbTipoCampo("");
        comClie.setDbTrovaMentreScrive(true);
        comClie.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comClieItemStateChanged(evt);
            }
        });
        comClie.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                comClieFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                comClieFocusLost(evt);
            }
        });
        comClie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comClieActionPerformed(evt);
            }
        });
        comClie.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                comClieKeyPressed(evt);
            }
        });

        texTotaImpo1.setBackground(new java.awt.Color(255, 200, 200));
        texTotaImpo1.setText("0");
        texTotaImpo1.setDbDescCampo("");
        texTotaImpo1.setDbNomeCampo("totale_imponibile");
        texTotaImpo1.setDbTipoCampo("valuta");

        texTotaIva1.setBackground(new java.awt.Color(255, 200, 200));
        texTotaIva1.setText("0");
        texTotaIva1.setDbDescCampo("");
        texTotaIva1.setDbNomeCampo("totale_iva");
        texTotaIva1.setDbTipoCampo("valuta");

        texTota1.setBackground(new java.awt.Color(255, 200, 200));
        texTota1.setText("0");
        texTota1.setDbDescCampo("");
        texTota1.setDbNomeCampo("totale");
        texTota1.setDbTipoCampo("valuta");

        texNote.setDbNomeCampo("note");
        texNote.setFont(texNote.getFont());
        texNote.setText("note");

        jLabel13.setText("Numero");

        jLabel14.setText("Serie");

        jLabel15.setText("Destinazione merce");

        jLabel16.setText("Data");

        labScon1.setText("Sc. 1");
        labScon1.setToolTipText("primo sconto");

        labScon2.setText("Sc. 3");
        labScon2.setToolTipText("sconto3");

        jLabel113.setText("Sp. incasso");

        texData.setEditable(false);
        texData.setColumns(9);
        texData.setText("data");
        texData.setDbDescCampo("");
        texData.setDbNomeCampo("data");
        texData.setDbTipoCampo("data");
        texData.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                texDataFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                texDataFocusLost(evt);
            }
        });

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Note");

        texStat.setBackground(new java.awt.Color(255, 200, 200));
        texStat.setText("P");
        texStat.setDbDescCampo("");
        texStat.setDbNomeCampo("stato");
        texStat.setDbRiempire(false);
        texStat.setDbTipoCampo("");

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Aspetto esteriore beni");

        texScon3.setColumns(4);
        texScon3.setText("sconto3");
        texScon3.setToolTipText("terzo sconto");
        texScon3.setDbDescCampo("");
        texScon3.setDbNomeCampo("sconto3");
        texScon3.setDbTipoCampo("numerico");
        texScon3.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texScon3FocusLost(evt);
            }
        });
        texScon3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texScon3ActionPerformed(evt);
            }
        });
        texScon3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                texScon3KeyReleased(evt);
            }
        });

        labScon21.setText("Sc. 2");
        labScon21.setToolTipText("secondo sconto");

        jLabel151.setText("Cliente");

        texSeri.setEditable(false);
        texSeri.setBackground(new java.awt.Color(204, 204, 204));
        texSeri.setColumns(2);
        texSeri.setText("serie");
        texSeri.setDbDescCampo("");
        texSeri.setDbNomeCampo("serie");
        texSeri.setDbTipoCampo("");
        texSeri.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texSeriKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                texSeriKeyTyped(evt);
            }
        });

        texAnno.setBackground(new java.awt.Color(255, 200, 200));
        texAnno.setText("anno");
        texAnno.setDbDescCampo("");
        texAnno.setDbNomeCampo("anno");
        texAnno.setDbTipoCampo("");
        texAnno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texAnnoActionPerformed(evt);
            }
        });

        texClieDest.setBackground(new java.awt.Color(255, 200, 200));
        texClieDest.setText("id_cliente_destinazione");
        texClieDest.setDbComboAbbinata(comClieDest);
        texClieDest.setDbDescCampo("");
        texClieDest.setDbNomeCampo("id_cliente_destinazione");
        texClieDest.setDbTipoCampo("numerico");

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel18.setText("Peso netto");

        texNumeroColli.setColumns(6);
        texNumeroColli.setText("numero_colli");
        texNumeroColli.setDbDescCampo("");
        texNumeroColli.setDbNomeCampo("numero_colli");
        texNumeroColli.setDbTipoCampo("");
        texNumeroColli.setmaxChars(255);

        jLabel20.setText("1° Vettore");

        jLabel1.setText("Porto");

        comPorto.setDbNomeCampo("porto");
        comPorto.setDbRiempireForceText(true);
        comPorto.setDbSalvaKey(false);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("Causale del trasporto");

        comCausaleTrasporto.setDbNomeCampo("causale_trasporto");
        comCausaleTrasporto.setDbRiempireForceText(true);
        comCausaleTrasporto.setDbSalvaKey(false);
        comCausaleTrasporto.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comCausaleTrasportoItemStateChanged(evt);
            }
        });
        comCausaleTrasporto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comCausaleTrasportoActionPerformed(evt);
            }
        });

        texSpeseTrasporto.setColumns(6);
        texSpeseTrasporto.setText("spese_trasporto");
        texSpeseTrasporto.setDbDescCampo("");
        texSpeseTrasporto.setDbNomeCampo("spese_trasporto");
        texSpeseTrasporto.setDbTipoCampo("valuta");
        texSpeseTrasporto.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texSpeseTrasportoFocusLost(evt);
            }
        });
        texSpeseTrasporto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texSpeseTrasportoActionPerformed(evt);
            }
        });
        texSpeseTrasporto.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texSpeseTrasportoKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                texSpeseTrasportoKeyReleased(evt);
            }
        });

        jLabel114.setText("Sp. trasporto");

        jLabel5.setText("Consegna o inizio trasporto a mezzo");

        comMezzoTrasporto.setDbNomeCampo("mezzo_consegna");
        comMezzoTrasporto.setDbRiempireForceText(true);
        comMezzoTrasporto.setDbSalvaKey(false);

        texRiferimento.setText("riferimento");
        texRiferimento.setDbDescCampo("");
        texRiferimento.setDbNomeCampo("riferimento");
        texRiferimento.setDbTipoCampo("");
        texRiferimento.setmaxChars(255);

        butPrezziPrec.setFont(butPrezziPrec.getFont().deriveFont(butPrezziPrec.getFont().getSize()-1f));
        butPrezziPrec.setText("Prezzi precedenti");
        butPrezziPrec.setMargin(new java.awt.Insets(1, 4, 1, 4));
        butPrezziPrec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butPrezziPrecActionPerformed(evt);
            }
        });

        comAspettoEsterioreBeni.setDbNomeCampo("aspetto_esteriore_beni");
        comAspettoEsterioreBeni.setDbRiempireForceText(true);
        comAspettoEsterioreBeni.setDbSalvaKey(false);

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel19.setText("Pagamento");

        comPaga.setDbDescCampo("");
        comPaga.setDbNomeCampo("pagamento");
        comPaga.setDbTextAbbinato(null);
        comPaga.setDbTipoCampo("VARCHAR");
        comPaga.setDbTrovaMentreScrive(true);

        comVettori.setDbNomeCampo("vettore1");
        comVettori.setDbRiempireForceText(true);
        comVettori.setDbSalvaKey(false);

        texDataOra.setColumns(10);
        texDataOra.setText("dataoraddt");
        texDataOra.setDbDescCampo("");
        texDataOra.setDbNomeCampo("dataoraddt");
        texDataOra.setDbTipoCampo("");
        texDataOra.setmaxChars(255);
        texDataOra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texDataOraActionPerformed(evt);
            }
        });

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Data e ora");

        stampa_prezzi.setText("stampa prezzi");
        stampa_prezzi.setToolTipText("Spuntare per avere i prezzi in stampa del documento");
        stampa_prezzi.setDbDescCampo("Stampa prezzi in DDT");
        stampa_prezzi.setDbNomeCampo("opzione_prezzi_ddt");
        stampa_prezzi.setDbTipoCampo("");
        stampa_prezzi.setFont(stampa_prezzi.getFont().deriveFont(stampa_prezzi.getFont().getSize()-1f));
        stampa_prezzi.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        stampa_prezzi.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        stampa_prezzi.setIconTextGap(2);
        stampa_prezzi.setMaximumSize(new java.awt.Dimension(230, 25));

        labAgente.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labAgente.setText("Agente");

        comAgente.setToolTipText("");
        comAgente.setDbDescCampo("");
        comAgente.setDbNomeCampo("agente_codice");
        comAgente.setDbTipoCampo("numerico");
        comAgente.setDbTrovaMentreScrive(true);
        comAgente.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                comAgenteFocusLost(evt);
            }
        });
        comAgente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comAgenteActionPerformed(evt);
            }
        });

        labProvvigione.setText("Provvigione");

        texProvvigione.setColumns(5);
        texProvvigione.setToolTipText("");
        texProvvigione.setDbDescCampo("");
        texProvvigione.setDbNomeCampo("agente_percentuale");
        texProvvigione.setDbTipoCampo("numerico");
        texProvvigione.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                texProvvigioneFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                texProvvigioneFocusLost(evt);
            }
        });

        labPercentoProvvigione.setText("%");

        labRiferimento.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        labRiferimento.setText("Riferimento");

        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel23.setText("Num. colli");

        texPesoLordo.setColumns(5);
        texPesoLordo.setText("peso_lordo");
        texPesoLordo.setDbDescCampo("");
        texPesoLordo.setDbNomeCampo("peso_lordo");
        texPesoLordo.setDbTipoCampo("");
        texPesoLordo.setmaxChars(255);

        texPesoNetto.setColumns(5);
        texPesoNetto.setText("peso_netto");
        texPesoNetto.setDbDescCampo("");
        texPesoNetto.setDbNomeCampo("peso_netto");
        texPesoNetto.setDbTipoCampo("");
        texPesoNetto.setmaxChars(255);

        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel24.setText("Peso lordo");

        prezzi_ivati.setBackground(new java.awt.Color(255, 204, 204));
        prezzi_ivati.setText("prezzi ivati");
        prezzi_ivati.setToolTipText("Selezionando questa opzione stampa la Destinazione diversa nella Distinta delle RIBA");
        prezzi_ivati.setDbDescCampo("Prezzi Ivati");
        prezzi_ivati.setDbNomeCampo("prezzi_ivati");
        prezzi_ivati.setDbTipoCampo("");
        prezzi_ivati.setFont(new java.awt.Font("Dialog", 0, 9)); // NOI18N
        prezzi_ivati.setIconTextGap(1);

        texCliente.setColumns(24);
        texCliente.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                texClienteMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                texClienteMouseReleased(evt);
            }
        });

        apriclienti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apriclientiActionPerformed(evt);
            }
        });

        butAddClie.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/Actions-contact-new-icon-16.png"))); // NOI18N
        butAddClie.setToolTipText("Crea nuova anagrafica");
        butAddClie.setMargin(new java.awt.Insets(1, 1, 1, 1));
        butAddClie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butAddClieActionPerformed(evt);
            }
        });

        labModConsegna.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labModConsegna.setText("Modalità di consegna");

        comConsegna.setDbDescCampo("Modalità di consegna");
        comConsegna.setDbNomeCampo("modalita_consegna");
        comConsegna.setDbTipoCampo("");
        comConsegna.setDbTrovaMentreScrive(true);
        comConsegna.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comConsegnaActionPerformed(evt);
            }
        });

        labModScarico.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labModScarico.setText("Scarico");

        comScarico.setDbDescCampo("Modalità di scarico");
        comScarico.setDbNomeCampo("modalita_scarico");
        comScarico.setDbTipoCampo("");
        comScarico.setDbTrovaMentreScrive(true);
        comScarico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comScaricoActionPerformed(evt);
            }
        });

        comClieDest.setDbNomeCampo("");
        comClieDest.setDbRiempire(false);
        comClieDest.setDbSalvare(false);
        comClieDest.setDbTextAbbinato(texClieDest);
        comClieDest.setDbTipoCampo("numerico");
        comClieDest.setDbTrovaMentreScrive(true);
        comClieDest.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                comClieDestFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                comClieDestFocusLost(evt);
            }
        });
        comClieDest.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                comClieDestKeyPressed(evt);
            }
        });

        labScon10.setFont(labScon10.getFont().deriveFont(labScon10.getFont().getSize()-1f));
        labScon10.setText("ragione sociale");
        labScon10.setToolTipText("");

        texDestRagioneSociale.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texDestRagioneSociale.setColumns(20);
        texDestRagioneSociale.setToolTipText("");
        texDestRagioneSociale.setDbDescCampo("");
        texDestRagioneSociale.setDbNomeCampo("dest_ragione_sociale");
        texDestRagioneSociale.setDbTipoCampo("");
        texDestRagioneSociale.setFont(texDestRagioneSociale.getFont().deriveFont(texDestRagioneSociale.getFont().getSize()-1f));

        labScon11.setFont(labScon11.getFont().deriveFont(labScon11.getFont().getSize()-1f));
        labScon11.setText("indirizzo");
        labScon11.setToolTipText("");

        texDestIndirizzo.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texDestIndirizzo.setToolTipText("");
        texDestIndirizzo.setDbDescCampo("");
        texDestIndirizzo.setDbNomeCampo("dest_indirizzo");
        texDestIndirizzo.setDbTipoCampo("");
        texDestIndirizzo.setFont(texDestIndirizzo.getFont().deriveFont(texDestIndirizzo.getFont().getSize()-1f));

        labScon12.setFont(labScon12.getFont().deriveFont(labScon12.getFont().getSize()-1f));
        labScon12.setText("cap");
        labScon12.setToolTipText("");

        texDestCap.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texDestCap.setColumns(5);
        texDestCap.setToolTipText("");
        texDestCap.setDbDescCampo("");
        texDestCap.setDbNomeCampo("dest_cap");
        texDestCap.setDbTipoCampo("");
        texDestCap.setFont(texDestCap.getFont().deriveFont(texDestCap.getFont().getSize()-1f));

        labScon13.setFont(labScon13.getFont().deriveFont(labScon13.getFont().getSize()-1f));
        labScon13.setText("loc.");
        labScon13.setToolTipText("");

        texDestLocalita.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texDestLocalita.setToolTipText("");
        texDestLocalita.setDbDescCampo("");
        texDestLocalita.setDbNomeCampo("dest_localita");
        texDestLocalita.setDbTipoCampo("");
        texDestLocalita.setFont(texDestLocalita.getFont().deriveFont(texDestLocalita.getFont().getSize()-1f));

        labScon14.setFont(labScon14.getFont().deriveFont(labScon14.getFont().getSize()-1f));
        labScon14.setText("prov.");
        labScon14.setToolTipText("");

        texDestProvincia.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texDestProvincia.setColumns(3);
        texDestProvincia.setToolTipText("");
        texDestProvincia.setDbDescCampo("");
        texDestProvincia.setDbNomeCampo("dest_provincia");
        texDestProvincia.setDbTipoCampo("");
        texDestProvincia.setFont(texDestProvincia.getFont().deriveFont(texDestProvincia.getFont().getSize()-1f));

        labScon16.setFont(labScon16.getFont().deriveFont(labScon16.getFont().getSize()-1f));
        labScon16.setText("telefono");
        labScon16.setToolTipText("");

        texDestTelefono.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texDestTelefono.setToolTipText("");
        texDestTelefono.setDbDescCampo("");
        texDestTelefono.setDbNomeCampo("dest_telefono");
        texDestTelefono.setDbTipoCampo("");
        texDestTelefono.setFont(texDestTelefono.getFont().deriveFont(texDestTelefono.getFont().getSize()-1f));

        labScon15.setFont(labScon15.getFont().deriveFont(labScon15.getFont().getSize()-1f));
        labScon15.setText("cellulare");
        labScon15.setToolTipText("");

        texDestCellulare.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        texDestCellulare.setColumns(10);
        texDestCellulare.setToolTipText("");
        texDestCellulare.setDbDescCampo("");
        texDestCellulare.setDbNomeCampo("dest_cellulare");
        texDestCellulare.setDbTipoCampo("");
        texDestCellulare.setFont(texDestCellulare.getFont().deriveFont(texDestCellulare.getFont().getSize()-1f));

        labScon17.setFont(labScon17.getFont().deriveFont(labScon17.getFont().getSize()-1f));
        labScon17.setText("paese");
        labScon17.setToolTipText("");

        comPaese.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        comPaese.setDbNomeCampo("dest_paese");
        comPaese.setDbTipoCampo("");
        comPaese.setDbTrovaMentreScrive(true);
        comPaese.setFont(comPaese.getFont().deriveFont(comPaese.getFont().getSize()-1f));

        org.jdesktop.layout.GroupLayout dati_dest_diversaLayout = new org.jdesktop.layout.GroupLayout(dati_dest_diversa);
        dati_dest_diversa.setLayout(dati_dest_diversaLayout);
        dati_dest_diversaLayout.setHorizontalGroup(
            dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(dati_dest_diversaLayout.createSequentialGroup()
                .add(dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(dati_dest_diversaLayout.createSequentialGroup()
                        .add(labScon10)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestRagioneSociale, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_dest_diversaLayout.createSequentialGroup()
                        .add(labScon11)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestIndirizzo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_dest_diversaLayout.createSequentialGroup()
                        .add(labScon12)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestCap, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(labScon13)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestLocalita, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(labScon14)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestProvincia, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(dati_dest_diversaLayout.createSequentialGroup()
                        .add(labScon16)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestTelefono, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(labScon15)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestCellulare, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(dati_dest_diversaLayout.createSequentialGroup()
                        .add(labScon17)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comPaese, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(comClieDest, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(0, 0, 0))
        );
        dati_dest_diversaLayout.setVerticalGroup(
            dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(dati_dest_diversaLayout.createSequentialGroup()
                .add(comClieDest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(2, 2, 2)
                .add(dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon10)
                    .add(texDestRagioneSociale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon11)
                    .add(texDestIndirizzo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon12)
                    .add(texDestCap, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labScon13)
                    .add(texDestLocalita, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labScon14)
                    .add(texDestProvincia, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon16)
                    .add(texDestTelefono, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labScon15)
                    .add(texDestCellulare, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_dest_diversaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon17)
                    .add(comPaese, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(0, 0, Short.MAX_VALUE))
        );

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        org.jdesktop.layout.GroupLayout datiLayout = new org.jdesktop.layout.GroupLayout(dati);
        dati.setLayout(datiLayout);
        datiLayout.setHorizontalGroup(
            datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiLayout.createSequentialGroup()
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(datiLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                    .add(jLabel20)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(comVettori, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                    .add(jLabel5)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(comMezzoTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 214, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                    .add(jLabel1)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(comPorto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 187, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                            .add(datiLayout.createSequentialGroup()
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                        .add(labRiferimento)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(texRiferimento, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(datiLayout.createSequentialGroup()
                                                .add(texSeri, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(texNume, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(texData, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                            .add(datiLayout.createSequentialGroup()
                                                .add(jLabel14)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jLabel13)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jLabel16)))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(datiLayout.createSequentialGroup()
                                                .add(jLabel151)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(butPrezziPrec))
                                            .add(datiLayout.createSequentialGroup()
                                                .add(texCliente, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .add(0, 0, 0)
                                                .add(apriclienti, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(butAddClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jLabel12)
                                            .add(jLabel4))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(comCausaleTrasporto, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .add(comAspettoEsterioreBeni, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(dati_dest_diversa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(datiLayout.createSequentialGroup()
                                        .add(jLabel15)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(sepDestMerce))))))
                    .add(datiLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(texScon1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texScon2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texScon3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texSpeseTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texSpeseIncasso, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(datiLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(labScon1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labScon21)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labScon2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel114)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel113))
                    .add(datiLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(datiLayout.createSequentialGroup()
                                .add(texStat, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texTota1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(prezzi_ivati, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiLayout.createSequentialGroup()
                                .add(texTotaImpo1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texClieDest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texAnno, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texTotaIva1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                    .add(datiLayout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                .add(jLabel24)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texPesoLordo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel18)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texPesoNetto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(labAgente)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comAgente, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(labProvvigione)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texProvvigione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(labPercentoProvvigione))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                .add(jLabel11)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texNote, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 563, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                                .add(jLabel6)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texDataOra, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel23)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texNumeroColli, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jLabel19)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comPaga, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 202, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(stampa_prezzi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                    .add(datiLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(labModConsegna)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comConsegna, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labModScarico)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comScarico, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        datiLayout.linkSize(new java.awt.Component[] {jLabel14, texSeri}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel13, texNume}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel16, texData}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {labScon1, texScon1}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {labScon21, texScon2}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {labScon2, texScon3}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel114, texSpeseTrasporto}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel113, texSpeseIncasso}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel12, jLabel4}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel11, jLabel20, jLabel24, jLabel6}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {texDataOra, texPesoLordo}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel18, jLabel23}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {texNumeroColli, texPesoNetto}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel19, labAgente}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.setVerticalGroup(
            datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiLayout.createSequentialGroup()
                .add(2, 2, 2)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(datiLayout.createSequentialGroup()
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(sepDestMerce, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel15))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(dati_dest_diversa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                        .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 141, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, datiLayout.createSequentialGroup()
                            .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                .add(datiLayout.createSequentialGroup()
                                    .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel14)
                                        .add(jLabel13)
                                        .add(jLabel16)
                                        .add(jLabel151)
                                        .add(butPrezziPrec))
                                    .add(0, 0, 0)
                                    .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(texSeri, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(texNume, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(texData, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(texCliente, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .add(apriclienti, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(butAddClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(1, 1, 1)
                            .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(labScon1)
                                .add(labScon21)
                                .add(labScon2)
                                .add(jLabel114)
                                .add(jLabel113))
                            .add(1, 1, 1)
                            .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(texScon1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(texScon2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(texScon3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(texSpeseTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(texSpeseIncasso, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(2, 2, 2)
                            .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(labRiferimento)
                                .add(texRiferimento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(2, 2, 2)
                            .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(comCausaleTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(jLabel4))
                            .add(2, 2, 2)
                            .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(jLabel12)
                                .add(comAspettoEsterioreBeni, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                .add(2, 2, 2)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel20)
                    .add(comVettori, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(comMezzoTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(comPorto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel11)
                    .add(texNote, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(stampa_prezzi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6)
                    .add(texDataOra, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel23)
                    .add(texNumeroColli, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel19)
                    .add(comPaga, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labPercentoProvvigione)
                    .add(jLabel24)
                    .add(texPesoLordo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel18)
                    .add(texPesoNetto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labAgente)
                    .add(comAgente, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texProvvigione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labProvvigione))
                .add(2, 2, 2)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labModConsegna)
                    .add(comConsegna, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labModScarico)
                    .add(comScarico, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(texTotaImpo1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texClieDest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texAnno, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(comClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texTotaIva1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(texStat, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texTota1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(prezzi_ivati, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        datiLayout.linkSize(new java.awt.Component[] {apriclienti, butAddClie, texCliente}, org.jdesktop.layout.GroupLayout.VERTICAL);

        datiLayout.linkSize(new java.awt.Component[] {comCausaleTrasporto, jLabel4}, org.jdesktop.layout.GroupLayout.VERTICAL);

        datiLayout.linkSize(new java.awt.Component[] {comAspettoEsterioreBeni, jLabel12}, org.jdesktop.layout.GroupLayout.VERTICAL);

        scrollDati.setViewportView(dati);

        tab.addTab("Dati", scrollDati);

        jLabel8.setText("Colonna listino consigliato");

        listino_consigliato.setDbNomeCampo("listino_consigliato");
        listino_consigliato.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listino_consigliatoActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pan_segnaposto_depositiLayout = new org.jdesktop.layout.GroupLayout(pan_segnaposto_depositi);
        pan_segnaposto_depositi.setLayout(pan_segnaposto_depositiLayout);
        pan_segnaposto_depositiLayout.setHorizontalGroup(
            pan_segnaposto_depositiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );
        pan_segnaposto_depositiLayout.setVerticalGroup(
            pan_segnaposto_depositiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        labNoteConsegna.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labNoteConsegna.setText("Note consegna");

        texNoteConsegna.setDbNomeCampo("note_consegna");
        texNoteConsegna.setRows(5);

        labCampoLibero1.setText("campo libero 1");
        labCampoLibero1.setToolTipText("");

        comCampoLibero1.setDbNomeCampo("campo_libero_1");
        comCampoLibero1.setDbRiempireForceText(true);
        comCampoLibero1.setDbSalvaKey(false);
        comCampoLibero1.setDbTrovaMentreScrive(true);
        comCampoLibero1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                comCampoLibero1FocusGained(evt);
            }
        });
        comCampoLibero1.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                comCampoLibero1PopupMenuWillBecomeVisible(evt);
            }
        });

        org.jdesktop.layout.GroupLayout datiAltroLayout = new org.jdesktop.layout.GroupLayout(datiAltro);
        datiAltro.setLayout(datiAltroLayout);
        datiAltroLayout.setHorizontalGroup(
            datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiAltroLayout.createSequentialGroup()
                .addContainerGap()
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(datiAltroLayout.createSequentialGroup()
                        .add(labNoteConsegna)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texNoteConsegna, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(datiAltroLayout.createSequentialGroup()
                        .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(datiAltroLayout.createSequentialGroup()
                                .add(jLabel8)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(listino_consigliato, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 265, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(pan_segnaposto_depositi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(datiAltroLayout.createSequentialGroup()
                                .add(labCampoLibero1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comCampoLibero1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 250, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        datiAltroLayout.setVerticalGroup(
            datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiAltroLayout.createSequentialGroup()
                .addContainerGap()
                .add(pan_segnaposto_depositi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(listino_consigliato, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(labNoteConsegna)
                    .add(texNoteConsegna, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labCampoLibero1)
                    .add(comCampoLibero1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tab.addTab("Altro", datiAltro);

        jSplitPane1.setTopComponent(tab);

        panDati.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(panDati, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void texDataFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texDataFocusGained

        old_anno = getAnno();
        old_data = texData.getText();
        old_id = texNume.getText();
        anno_modificato = false;

    }//GEN-LAST:event_texDataFocusGained

    private void texNumeFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texNumeFocusGained
        old_id = texNume.getText();
        id_modificato = false;
    }//GEN-LAST:event_texNumeFocusGained

    private void texNumeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texNumeFocusLost
        texNume.setText(texNume.getText().replaceAll("[^\\d.]", ""));
        if (!old_id.equals(texNume.getText())) {
            //controllo che se è un numero già presente non glielo facci ofare percè altrimenti sovrascrive una altra fattura
            sql = "select numero from test_ddt" + suff;
            sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
            sql += " and numero " + Db.pcW(texNume.getText(), "NUMBER");
            sql += " and anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
            ResultSet r = Db.openResultSet(sql);
            try {
                if (r.next()) {
                    texNume.setText(old_id);
                    JOptionPane.showMessageDialog(this, "Non puoi mettere il numero di un documento già presente !", "Attenzione", JOptionPane.INFORMATION_MESSAGE);
                    return;
                } else {
                    //associo al nuovo numero
                    dbdoc.numero = new Integer(this.texNume.getText()).intValue();

                    if (!main.edit_doc_in_temp) {
                        sql = "update righ_ddt" + suff + "";
                        sql += " set numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                        sql += " and numero " + Db.pcW(old_id, "NUMBER");
                        sql += " and anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
                        Db.executeSql(sql);

                        sql = "update test_ddt" + suff + "";
                        sql += " set numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                        sql += " and numero " + Db.pcW(old_id, "NUMBER");
                        sql += " and anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
                        Db.executeSql(sql);

    //                    dati.dbChiaveValori.clear();
    //                    dati.dbChiaveValori.put("serie", prev.serie);
    //                    dati.dbChiaveValori.put("numero", prev.numero);
    //                    dati.dbChiaveValori.put("anno", prev.anno);
                        //riassocio
                        dbAssociaGrigliaRighe();
                        id_modificato = true;

                        dbdoc.numero = Integer.parseInt(texNume.getText());
                        doc.load(Db.INSTANCE, this.dbdoc.numero, this.dbdoc.serie, this.dbdoc.anno, getTipoDoc(), id);
                        ricalcolaTotali();

                        //vado ad aggiornare eventuali ddt o ordini legati
                        sql = "update test_ordi";
                        sql += " set doc_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where doc_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                        sql += " and doc_numero " + Db.pcW(old_id, "NUMBER");
                        sql += " and doc_anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
                        sql += " and doc_tipo " + Db.pcW(String.valueOf(this.dbdoc.tipoDocumento), "VARCHAR");
                        Db.executeSql(sql);

                        //vado ad aggiornare eventuali movimenti generati
                        sql = "update movimenti_magazzino";
                        sql += " set da_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where da_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                        sql += " and da_numero " + Db.pcW(old_id, "NUMBER");
                        sql += " and da_anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
                        sql += " and da_tabella = 'test_ddt" + suff + "'";
                        Db.executeSql(sql);
                    }

                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }//GEN-LAST:event_texNumeFocusLost

    private void comClieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comClieActionPerformed
        sql = "select obsoleto from clie_forn";
        sql += " where codice = " + Db.pc(this.texClie.getText(), "NUMERIC");
        sql += " order by ragione_sociale";

        ResultSet rs = Db.openResultSet(sql);
        try {
            if (rs.next()) {
                int obsoleto = rs.getInt("obsoleto");
                if (obsoleto == 1) {
                    JOptionPane.showMessageDialog(this, "Attenzione, il cliente selezionato è segnato come obsoleto.", "Cliente obsoleto", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //apro combo destinazione cliente
        sql = "select ragione_sociale, id from clie_forn_dest";
        sql += " where codice_cliente = " + Db.pc(this.texClie.getText(), "NUMERIC");

        riempiDestDiversa(sql);
    }//GEN-LAST:event_comClieActionPerformed

    private void texClieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texClieActionPerformed
        ricalcolaTotali();
    }//GEN-LAST:event_texClieActionPerformed

    private void texSpeseTrasportoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texSpeseTrasportoActionPerformed
        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseTrasportoActionPerformed

    private void texScon3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texScon3ActionPerformed
        ricalcolaTotali();
    }//GEN-LAST:event_texScon3ActionPerformed

    private void texScon2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texScon2ActionPerformed
        ricalcolaTotali();
    }//GEN-LAST:event_texScon2ActionPerformed

    private void texSpeseIncassoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texSpeseIncassoFocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseIncassoFocusLost

    private void texSpeseTrasportoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texSpeseTrasportoFocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseTrasportoFocusLost

    private void texScon3FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texScon3FocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texScon3FocusLost

    private void texSeriKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSeriKeyTyped
            }//GEN-LAST:event_texSeriKeyTyped

    private void texSeriKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSeriKeyPressed

        if (evt.getKeyCode() == evt.VK_TAB || evt.getKeyCode() == evt.VK_ENTER) {
            texSeri.setText(texSeri.getText().toUpperCase());
            assegnaNumero();

            //riabilito
            Component[] cs = this.dati.getComponents();

            for (int i = 0; i < cs.length; i++) {
                cs[i].setEnabled(true);
                if (cs[i] instanceof tnxbeans.tnxComboField) {
                    tnxbeans.tnxComboField combo = (tnxbeans.tnxComboField) cs[i];
                    combo.setEditable(true);
                    combo.setLocked(false);
                }
            }
            cs = dati_dest_diversa.getComponents();
            for (int i = 0; i < cs.length; i++) {
                cs[i].setEnabled(true);
                if (cs[i] instanceof tnxbeans.tnxComboField) {
                    tnxbeans.tnxComboField combo = (tnxbeans.tnxComboField) cs[i];
                    combo.setEditable(true);
                    combo.setLocked(false);
                }
            }

            dopoInserimento();
            texCliente.requestFocus();
        }
    }//GEN-LAST:event_texSeriKeyPressed

    private void comCausaleTrasportoItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comCausaleTrasportoItemStateChanged
        if (evt.getStateChange() == evt.SELECTED) {
            visualizzaTarga();
        }
    }//GEN-LAST:event_comCausaleTrasportoItemStateChanged

    private void comCausaleTrasportoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comCausaleTrasportoActionPerformed
            }//GEN-LAST:event_comCausaleTrasportoActionPerformed

    private void comClieDestFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieDestFocusLost
        if (comClieDest.getSelectedIndex() != comClieDest_old) {
            caricaDestinazioneDiversa();
        }
    }//GEN-LAST:event_comClieDestFocusLost

    private void comClieDestKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_comClieDestKeyPressed

        if (evt.getKeyCode() == evt.VK_ENTER) {
            caricaDestinazioneDiversa();
        }
    }//GEN-LAST:event_comClieDestKeyPressed

    private void butPrezziPrecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butPrezziPrecActionPerformed
        showPrezziFatture();
    }//GEN-LAST:event_butPrezziPrecActionPerformed

    private void texSpeseTrasportoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSpeseTrasportoKeyReleased

        try {
            dbdoc.speseTrasportoIva = Db.getDouble(this.texSpeseTrasporto.getText());
        } catch (Exception err) {
            dbdoc.speseTrasportoIva = 0;
        }

        dbdoc.dbRefresh();
    }//GEN-LAST:event_texSpeseTrasportoKeyReleased

    private void texSpeseTrasportoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSpeseTrasportoKeyPressed
            }//GEN-LAST:event_texSpeseTrasportoKeyPressed

    private void texSpeseIncassoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texSpeseIncassoActionPerformed
        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseIncassoActionPerformed

    private void butStampaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butStampaActionPerformed
        if (block_aggiornareProvvigioni) {
            return;
        }

        if (SwingUtils.showYesNoMessage(this, "Prima di stampare è necessario salvare il documento, proseguire ?")) {
            if (controlloCampi()) {
                
                String dbSerie = this.dbdoc.serie;
                int dbNumero = this.dbdoc.numero;
                int dbAnno = this.dbdoc.anno;

                if (saveDocumento()) {

                    try {
                        if (from != null) {
                            this.from.dbRefresh();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    main.events.fireInvoicexEvent(new InvoicexEvent(this, InvoicexEvent.TYPE_SAVE));

                    //STAMPA
                    if (evt.getActionCommand().equalsIgnoreCase("pdf")) {
                        try {
                            InvoicexUtil.creaPdf(getTipoDoc(), new Integer[]{id}, true, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                            SwingUtils.showExceptionMessage(this, e);
                        }
                    } else {
                        frmElenDDT.stampa("", dbSerie, dbNumero, dbAnno, acquisto, id);
                    }

                    //MODIFICA dopo
                    String nuova_serie = texSeri.getText();
                    Integer nuovo_numero = cu.toInteger(texNume.getText());
                    Integer nuovo_anno = cu.toInteger(texAnno.getText());

                    //aggiorno le righe temp
                    if (!main.edit_doc_in_temp) {
                        sql = "update " + getNomeTabRighe();
                        sql += " set serie = '" + nuova_serie + "'";
                        sql += " , numero = " + nuovo_numero + "";
                        sql += " , anno = " + nuovo_anno + "";
                        sql += " where serie = " + db.pc(serie_originale, "VARCHAR");
                        sql += " and numero = " + numero_originale;
                        sql += " and anno = " + anno_originale;
                        Db.executeSqlDialogExc(sql, true);
                    }

                    serie_originale = texSeri.getText();
                    numero_originale = cu.toInteger(texNume.getText());
                    anno_originale = cu.toInteger(texAnno.getText());

                    totaleIniziale = doc.getTotale();

                    //una volta salvatao e stampato entro in modalitaà modifica se ero in inserimento
                    if (!main.edit_doc_in_temp) {
                        if (dbStato.equals(frmTestDocu.DB_INSERIMENTO)) {
                            dbStato = frmTestDocu.DB_MODIFICA;
                            //e riporto le righe in _temp
                            sql = "insert into righ_ddt" + suff + "_temp";
                            sql += " select *, '" + main.login + "' as username";
                            sql += " from righ_ddt" + suff;
                            sql += " where serie = " + db.pc(nuova_serie, "VARCHAR");
                            sql += " and numero = " + nuovo_numero;
                            sql += " and anno = " + nuovo_anno;
                            try {
                                DbUtils.tryExecQuery(Db.getConn(), sql);
                                System.out.println("sql ok:" + sql);
                            } catch (Exception e) {
                                System.err.println("sql errore:" + sql);
                                e.printStackTrace();
                            }
                        } else {
                            porto_in_temp();
                        }
                    }
                    dati.dbCheckModificatiReset();
                }
            }
        }

        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

    }//GEN-LAST:event_butStampaActionPerformed

    private void texDataFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texDataFocusLost
        if (!ju.isValidDateIta(texData.getText())) {
            SwingUtils.showFlashMessage2Comp("Data non valida", 3, texData, Color.red);
            return;
        }

        if (!old_anno.equals(getAnno())) {
            if (dbStato.equals(DB_INSERIMENTO)) {
                dbdoc.dbRicalcolaProgressivo(dbStato, this.texData.getText(), this.texNume, texAnno, texSeri.getText(), id);
                dbdoc.numero = new Integer(this.texNume.getText()).intValue();
                id_modificato = true;
            } else {
                //controllo che se è un numero già presente non glielo faccio fare percè altrimenti sovrascrive una altra fattura
                sql = "select numero from test_ddt" + suff + "";
                sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                sql += " and numero " + Db.pcW(texNume.getText(), "NUMBER");
                sql += " and anno " + Db.pcW(getAnno(), "VARCHAR");
                ResultSet r = Db.openResultSet(sql);
                try {
                    if (r.next()) {
                        texData.setText(old_data);
                        JOptionPane.showMessageDialog(this, "Non puoi mettere questo numero e data, si sovrappongono ad un documento già presente !", "Attenzione", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            try {
                texAnno.setText(getAnno());
                dbdoc.anno = Integer.parseInt(getAnno());
                dbdoc.numero = Integer.parseInt(texNume.getText());

                
                if (!main.edit_doc_in_temp) {
                    sql = "update righ_ddt" + suff + "";
                    sql += " set anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " , numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                    sql += " and numero " + Db.pcW(old_id, "NUMBER");
                    sql += " and anno " + Db.pcW(old_anno, "VARCHAR");
                    Db.executeSql(sql);

                    sql = "update test_ddt" + suff + "";
                    sql += " set anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " , numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                    sql += " and numero " + Db.pcW(old_id, "NUMBER");
                    sql += " and anno " + Db.pcW(old_anno, "VARCHAR");
                    Db.executeSql(sql);

    //                dati.dbChiaveValori.clear();
    //                dati.dbChiaveValori.put("serie", prev.serie);
    //                dati.dbChiaveValori.put("numero", prev.numero);
    //                dati.dbChiaveValori.put("anno", prev.anno);
                    //riassocio
                    dbAssociaGrigliaRighe();

                    doc.load(Db.INSTANCE, dbdoc.numero, dbdoc.serie, dbdoc.anno, getTipoDoc(), id);
                    ricalcolaTotali();

                    anno_modificato = true;

                    //vado ad aggiornare eventuali ddt o ordini legati
                    sql = "update test_ordi";
                    sql += " set doc_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += " , anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " where doc_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                    sql += " and doc_numero " + Db.pcW(old_id, "NUMBER");
                    sql += " and doc_anno " + Db.pcW(String.valueOf(old_anno), "VARCHAR");
                    sql += " and doc_tipo " + Db.pcW(String.valueOf(this.dbdoc.tipoDocumento), "VARCHAR");
                    Db.executeSql(sql);

                    //vado ad aggiornare eventuali movimenti generati
                    sql = "update movimenti_magazzino";
                    sql += " set da_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += ", da_anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " where da_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                    sql += " and da_numero " + Db.pcW(old_id, "NUMBER");
                    sql += " and da_anno " + Db.pcW(String.valueOf(old_anno), "VARCHAR");
                    sql += " and da_tabella = 'test_ddt" + suff + "'";
                    Db.executeSql(sql);
                }

            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }//GEN-LAST:event_texDataFocusLost

    private void comClieItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comClieItemStateChanged
//        if (evt.getStateChange() == ItemEvent.SELECTED && !loading) {
//            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//            String sqlTmp = "SELECT note, note_automatiche as auto FROM clie_forn where codice = " + Db.pc(String.valueOf(comClie.getSelectedKey()), "NUMERIC");
//            ResultSet noteauto = Db.openResultSet(sqlTmp);
//            try {
//                if (noteauto.next()) {
//                    String auto = noteauto.getString("auto");
//                    String nota = noteauto.getString("note");
//                    if (auto != null && auto.equals("S")) {
//                        if (!texNote.getText().equals("") && !texNote.getText().equals(nota)) {
//                            this.texNote.setText(nota);
//                        } else {
//                            this.texNote.setText(noteauto.getString("note"));
//                        }
//                    }
//                }
//            } catch (SQLException ex) {
//                ex.printStackTrace();
//            }
//            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
//        }
    }//GEN-LAST:event_comClieItemStateChanged

    private void texScon3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon3KeyReleased

        try {
            dbdoc.sconto3 = Db.getDouble(this.texScon3.getText());
        } catch (Exception err) {

            //err.printStackTrace();
            dbdoc.sconto3 = 0;
        }

        dbdoc.dbRefresh();
    }//GEN-LAST:event_texScon3KeyReleased

    private void comClieKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_comClieKeyPressed

        // Add your handling code here:
        if (evt.getKeyCode() == evt.VK_ENTER) {
            this.recuperaDatiCliente();
        }

        //ricerca con F4
        if (evt.getKeyCode() == evt.VK_F4) {

            java.util.Hashtable colsWidthPerc = new java.util.Hashtable();
            colsWidthPerc.put("id", new Double(20));
            colsWidthPerc.put("ragione", new Double(40));
            colsWidthPerc.put("indi", new Double(40));

            String sql
                    = "select clie_forn.codice as id, clie_forn.ragione_sociale as ragione, clie_forn.indirizzo as indi from clie_forn " + "where clie_forn.ragione_sociale like '%" + Db.aa(this.comClie.getText()) + "%'" + " order by clie_forn.ragione_sociale";
            ResultSet resTemp = db.openResultSet(sql);

            try {

                if (resTemp.next() == true) {

                    frmDbListSmall temp = new frmDbListSmall(main.getPadre(), true, sql, this.texClie, 0, colsWidthPerc, 50, 50, 400, 300);
                    this.recuperaDatiCliente();
                    this.comClie.dbTrovaKey(texClie.getText());
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "Nessun cliente trovato");
                }
            } catch (Exception err) {
                err.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(this, "Errore nella ricerca cliente: " + err.toString());
            }
        }
    }//GEN-LAST:event_comClieKeyPressed

    private void texClieKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texClieKeyPressed

        // Add your handling code here:
        if (evt.getKeyCode() == evt.VK_ENTER) {
            this.recuperaDatiCliente();
        }
    }//GEN-LAST:event_texClieKeyPressed

    private void comClieFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieFocusLost
        if (comClie.getSelectedIndex() != comClieSel_old) {
            this.recuperaDatiCliente();
            ricalcolaTotali();
        }
    }//GEN-LAST:event_comClieFocusLost

    private void texClieFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texClieFocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texClieFocusLost

    private void texSpeseIncassoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSpeseIncassoKeyReleased

        try {

            //prev.speseVarie = Db.getDouble(this.texSpesVari.getText());
            dbdoc.speseIncassoIva = Db.getDouble(this.texSpeseIncasso.getText());
        } catch (Exception err) {

            //err.printStackTrace();
            dbdoc.speseIncassoIva = 0;
        }

        dbdoc.dbRefresh();
    }//GEN-LAST:event_texSpeseIncassoKeyReleased

    private void texScon2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon2KeyReleased

        try {
            dbdoc.sconto2 = Db.getDouble(this.texScon2.getText());
        } catch (Exception err) {

            //err.printStackTrace();
            dbdoc.sconto2 = 0;
        }

        dbdoc.dbRefresh();
    }//GEN-LAST:event_texScon2KeyReleased

    private void texScon1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon1KeyReleased

        try {
            dbdoc.sconto1 = Db.getDouble(this.texScon1.getText());
        } catch (Exception err) {

            //err.printStackTrace();
            dbdoc.sconto1 = 0;
        }

        //debug
        //System.out.println("sconto1:" + prev.sconto1 + " testo:" + this.texScon1.getText());
        dbdoc.dbRefresh();
    }//GEN-LAST:event_texScon1KeyReleased

    private void texScon2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon2KeyPressed
            }//GEN-LAST:event_texScon2KeyPressed

    private void texScon1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon1KeyPressed
            }//GEN-LAST:event_texScon1KeyPressed

    private void texScon1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon1KeyTyped
            }//GEN-LAST:event_texScon1KeyTyped

    private void texScon2FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texScon2FocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texScon2FocusLost

    private void texScon1FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texScon1FocusLost
        ricalcolaTotali();

        if (main.fileIni.getValueBoolean("pref", "provvigioniAutomatiche", false)) {
            this.comAgenteFocusLost(null);
        }
    }//GEN-LAST:event_texScon1FocusLost

    private void texScon1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texScon1ActionPerformed
        ricalcolaTotali();
    }//GEN-LAST:event_texScon1ActionPerformed

    private void formInternalFrameOpened(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_formInternalFrameOpened
        this.griglia.resizeColumnsPerc(true);
        Point p = new Point(0, 0);
        Point p2 = SwingUtilities.convertPoint(dati, p, jSplitPane1);

        InvoicexUtil.aggiornaSplit(dati, jSplitPane1);
    }//GEN-LAST:event_formInternalFrameOpened

    private void formInternalFrameClosed(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_formInternalFrameClosed
        InvoicexUtil.removeLock(dati.dbNomeTabella, id, this);

        main.getPadre().closeFrame(this);
    }//GEN-LAST:event_formInternalFrameClosed

    private void grigliaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_grigliaMouseClicked

        // Add your handling code here:
        try {

            if (evt.getClickCount() == 2) {

                //modifico o la riga o la finestra
                popGrigModiActionPerformed(null);
            }
        } catch (Exception err) {
            javax.swing.JOptionPane.showMessageDialog(null, err.toString());
            err.printStackTrace();
        }

    }//GEN-LAST:event_grigliaMouseClicked

    private void popGrigElimActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popGrigElimActionPerformed

        //elimino la/le righe
        try {
            if (griglia.getSelectedRowCount() >= 1) {
                //elimino gli le righe per id
                for (int i = 0; i < griglia.getSelectedRowCount(); i++) {
                    sql = "delete from " + griglia.dbNomeTabella + " where id = " + griglia.getValueAt(griglia.getSelectedRows()[i], griglia.getColumnByName("id"));
                    System.out.println("i = " + i);
                    dbu.tryExecQuery(Db.getConn(), sql);
                }
            }
        } catch (Exception e) {
            SwingUtils.showExceptionMessage(this, e);
        }

        griglia.dbRefresh();
        dbdoc.dbRefresh();
        ricalcolaTotali();

    }//GEN-LAST:event_popGrigElimActionPerformed

    private void popGrigModiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popGrigModiActionPerformed
        Integer id_riga = cu.i(griglia.getValueAt(griglia.getSelectedRow(), griglia.getColumnByName("id")));
        Integer id_padre = cu.i(griglia.getValueAt(griglia.getSelectedRow(), griglia.getColumnByName("id_padre")));

        //modifico la riga
        String codiceListino = "1";

        try {
            codiceListino = Db.lookUp(this.texClie.getText(), "codice", "clie_forn").getString("codice_listino");
        } catch (Exception err) {
            err.printStackTrace();
        }

        int codiceCliente = -1;

        if (this.texClie.getText().length() > 0) {

            try {
                codiceCliente = Integer.parseInt(texClie.getText());
            } catch (Exception err) {
                err.printStackTrace();
            }
        }

//        java.util.prefs.Preferences preferences = java.util.prefs.Preferences.userNodeForPackage(main.class);
//        boolean multiriga = preferences.getBoolean("multiriga", false);
        boolean multiriga = main.fileIni.getValueBoolean("pref", "multiriga", false);

//        if (multiriga == false) {
//
//            frmNuovRiga frm = new frmNuovRiga(this, tnxDbPanel.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNumePrev.getText()).intValue(), "P", Integer.valueOf(this.griglia.getValueAt(griglia.getSelectedRow(), 3).toString()).intValue(), Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente);
//            temp.openFrame(frm, 600, 350, 100, 100);
//            frm.setStato();
//        } else {
        try {
            JInternalFrame frm = null;
            int w = 650;
            int h = 400;
            int top = 100;
            int left = 100;
            if (main.getPersonalContain("frajor")) {
                if (!Sync.isActive()) {
                    frmNuovRigaDescrizioneMultiRigaNewFrajor temp_form = new frmNuovRigaDescrizioneMultiRigaNewFrajor(this, tnxDbPanel.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", Integer.valueOf(this.griglia.getValueAt(griglia.getSelectedRow(), 3).toString()), Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre);
                    temp_form.setStato();
                    w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_w", "760"));
                    h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_h", "660"));
                    top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_top", "100"));
                    left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_left", "100"));
                    frm = temp_form;
                } else {
                    SwingUtils.showErrorMessage(this, "SYNC: form non gestita");
                }
            } else {
                frmNuovRigaDescrizioneMultiRigaNew temp_form = new frmNuovRigaDescrizioneMultiRigaNew(this, tnxDbPanel.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", Integer.valueOf(this.griglia.getValueAt(griglia.getSelectedRow(), 3).toString()), Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre, getNomeTabRighe());
                temp_form.setStato();
                w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_w", "980"));
                h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_h", "460"));
                top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_top", "100"));
                left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_left", "100"));
                frm = temp_form;
            }

            main.getPadre().openFrame(frm, w, h, top, left);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        }
    }//GEN-LAST:event_popGrigModiActionPerformed

    private void butNuovArtiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butNuovArtiActionPerformed
        String codiceListino = "1";

        try {
            codiceListino = Db.lookUp(this.texClie.getText(), "codice", "clie_forn").getString("codice_listino");
        } catch (Exception err) {
            System.out.println("butNuovArtiActionPerformed:" + err.toString());
        }

        int codiceCliente = -1;

        if (this.texClie.getText().length() > 0) {

            try {
                codiceCliente = Integer.parseInt(texClie.getText());
            } catch (Exception err) {
                err.printStackTrace();
            }
        }

//        java.util.prefs.Preferences preferences = java.util.prefs.Preferences.userNodeForPackage(main.class);
//        boolean multiriga = preferences.getBoolean("multiriga", false);
        boolean multiriga = main.fileIni.getValueBoolean("pref", "multiriga", false);

//        if (multiriga == false) {
//            frmNuovRiga frm = new frmNuovRiga(this, this.dati.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNumePrev.getText()).intValue(), "P", 0, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente);
//            temp.openFrame(frm, 600, 350, 100, 100);
//            frm.setStato();
//        } else {
        try {
            JInternalFrame frm = null;
            int w = 650;
            int h = 400;
            int top = 100;
            int left = 100;
            if (main.getPersonalContain("frajor")) {
                if (!Sync.isActive()) {
                    frmNuovRigaDescrizioneMultiRigaNewFrajor temp_form = new frmNuovRigaDescrizioneMultiRigaNewFrajor(this, this.dati.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", 0, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, null, this.id);
                    w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_w", "700"));
                    h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_h", "660"));
                    top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_top", "100"));
                    left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_left", "100"));
                    temp_form.setStato();
                    frm = temp_form;
                } else {
                    SwingUtils.showErrorMessage(this, "SYNC: form non gestita");
                }
            } else {
                frmNuovRigaDescrizioneMultiRigaNew temp_form = new frmNuovRigaDescrizioneMultiRigaNew(this, tnxDbPanel.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", 0, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, null, this.id, getNomeTabRighe());
                w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_w", "980"));
                h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_h", "460"));
                top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_top", "100"));
                left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_left", "100"));
                temp_form.setStato();
                frm = temp_form;
                temp_form.texProvvigione.setText(texProvvigione.getText());
            }

            main.getPadre().openFrame(frm, w, h, top, left);

        } catch (Exception e) {
            e.printStackTrace();
        }
//        }

        /*      //fisso serie e numero
         this.serieRigh.setText(this.prev.serie);
         this.numeroRigh.setText(String.valueOf(this.prev.numero));
         this.codice_articolo.requestFocus();
         java.sql.Statement stat;
         ResultSet resu;
         //apre il resultset per ultimo +1
         try {
         stat = db.conn.createStatement();
         String sql = "select riga from righ_ddt" +
         " where serie = " + db.pc(this.prev.serie,"VARCHAR") +
         " and numero = " + db.pc(String.valueOf(this.prev.numero),"INTEGER") +
         " order by riga desc limit 1";
         resu = stat.executeQuery(sql);
         if(resu.next()==true) {
         this.riga.setText(String.valueOf(resu.getInt(1)+1));
         } else {
         this.riga.setText("1");
         }
         } catch (Exception err) {
         err.printStackTrace();
         javax.swing.JOptionPane.showMessageDialog(null,err.toString());
         }
         */
    }//GEN-LAST:event_butNuovArtiActionPerformed

    private void butUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butUndoActionPerformed
        if (block_aggiornareProvvigioni) {
            return;
        }

        if (evt != null) {
            if (!SwingUtils.showYesNoMessage(main.getPadreWindow(), "Sicuro di annullare le modifiche ?")) {
                return;
            }
        }

        if (!main.edit_doc_in_temp) {
            if (dbStato.equals(this.DB_INSERIMENTO)) {

                //elimino la testata inserita e poi annullata
                String sql = "delete from test_ddt" + suff + "";
                sql += " where serie = " + Db.pc(String.valueOf(this.dbdoc.serie), "VARCHAR");
                sql += " and numero = " + Db.pc(String.valueOf(this.dbdoc.numero), "NUMBER");
                //sql += " and stato = " + Db.pc(String.valueOf(this.prev.stato), "VARCHAR");
                sql += " and anno = " + Db.pc(String.valueOf(this.dbdoc.anno), "INTEGER");
                Db.executeSql(sql);
                sql = "delete from righ_ddt" + suff + "";
                sql += " where serie = " + Db.pc(String.valueOf(this.dbdoc.serie), "VARCHAR");
                sql += " and numero = " + Db.pc(String.valueOf(this.dbdoc.numero), "NUMBER");
                //sql += " and stato = " + Db.pc(String.valueOf(this.prev.stato), "VARCHAR");
                sql += " and anno = " + Db.pc(String.valueOf(this.dbdoc.anno), "INTEGER");
                Db.executeSql(sql);
            } else if (dbStato.equals(this.DB_MODIFICA)) {

                System.out.println("annulla da modifica, elimino " + dbdoc.serie + "/" + dbdoc.numero + "/" + dbdoc.anno + " e rimetto da temp " + serie_originale + "/" + numero_originale + "/" + anno_originale);

                //rimetto numero originale
                sql = "update test_ddt" + suff + "";
                sql += " set numero = " + Db.pc(numero_originale, "NUMBER");
                sql += " , anno = " + Db.pc(anno_originale, "NUMBER");
                sql += " where id = " + this.id;
                Db.executeSql(sql);

                //elimino le righe inserite
                sql = "delete from righ_ddt" + suff + "";
                sql += " where id_padre = " + this.id;
                Db.executeSql(sql);

                //e rimetto quelle da temp
                /* ATTENZIONE, NON RIMETTERE COME QUI SOTTO, OVVERO SENZA GLI ID ALTRIMENTI SI PERDE IL COLLEGAMENTO CON LE INFO SU LOTTI E MATRICOLE */
    //            sql = "insert into righ_ddt" + suff + " (" + Db.getFieldList("righ_ddt" + suff + "", false, Arrays.asList("id")) + ")";
    //            sql += " select " + Db.getFieldList("righ_ddt" + suff + "_temp", true, Arrays.asList("id"));
                sql = "insert into righ_ddt" + suff + " (" + Db.getFieldList("righ_ddt" + suff + "", false) + ")";
                sql += " select " + Db.getFieldList("righ_ddt" + suff + "_temp", true);

                sql += " from righ_ddt" + suff + "_temp";
                sql += " where id_padre = " + this.id;
                sql += " and username = '" + main.login + "'";
                Db.executeSqlDialogExc(sql, true);

                //rimetto numero originale su eventuali movimenti
                sql = "update movimenti_magazzino";
                sql += " set da_numero = " + Db.pc(numero_originale, "NUMBER");
                sql += ", da_anno = " + Db.pc(anno_originale, "NUMBER");
                sql += " where da_serie " + Db.pcW(dbdoc.serie, "VARCHAR");
                sql += " and da_numero " + Db.pcW(dbdoc.numero, "NUMBER");
                sql += " and da_anno " + Db.pcW(dbdoc.anno, "VARCHAR");
                sql += " and da_tabella = 'test_ddt" + suff + "'";
                Db.executeSql(sql);
            }
        } else {
            //non faccio niente, se si annulla rimane tutto come prima
        }
        
        if (from != null) {
            this.from.dbRefresh();
        }

        this.dispose();
    }//GEN-LAST:event_butUndoActionPerformed

    public void annulla() {
        butUndoActionPerformed(null);
    }

    public void focusSuButNuovArti() {
        butNuovArti.requestFocusInWindow();
    }

    private void butSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveActionPerformed
        if (block_aggiornareProvvigioni) {
            return;
        }

        if (controlloCampi()) {
            if (saveDocumento()) {
                try {
                    if (from != null) {
                        this.from.dbRefresh();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                main.events.fireInvoicexEvent(new InvoicexEvent(this, InvoicexEvent.TYPE_SAVE));

                if (chiudere) {
                    SwingUtils.mouse_def(this);
                    this.dispose();
                } else {
                    //ci pensa allegati util a chiudere
                    System.out.println("aspetto il salvataggio degli allegati");
                }
            }
        }
    }//GEN-LAST:event_butSaveActionPerformed

    private boolean controlloCampi() {

        //controllo data
        if (!ju.isValidDateIta(texData.getText())) {
            texData.requestFocus();
            javax.swing.JOptionPane.showMessageDialog(this, "Data del documento non valida");
            return false;
        }


        if (main.getPersonalContain("erika") && !acquisto) {
            if (comAgente.getSelectedIndex() < 0 || comAgente.getSelectedItem() == null) {
                FxUtils.fadeBackground(comAgente, Color.red);
                SwingUtils.showErrorMessage(this, "Selezionare obbligatoriamente l'agente", "Attenzione", true);
                return false;
            }
        }

        //controllo cliente
        //li recupero dal cliente
        ResultSet tempClie;
        String sql = "select * from clie_forn";
        sql += " where codice = " + Db.pc(this.texClie.getText(), "NUMERIC");
        tempClie = Db.openResultSet(sql);

        try {
            if (tempClie.next() != true) {
                texCliente.requestFocus();
                javax.swing.JOptionPane.showMessageDialog(this, "Il codice cliente specificato non esiste in anagrafica !");
                return false;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        if (deposito_arrivo != null && deposito != null) {
            if (deposito_arrivo.getSelectedItem() != null && deposito.getSelectedItem() != null && deposito_arrivo.getSelectedItem().equals(deposito.getSelectedItem())) {
                tab.setSelectedComponent(datiAltro);
                SwingUtils.showErrorMessage(this, "Non puoi selezionare il deposito di arrivo uguale al deposito di partenza !");
                FxUtils.fadeForeground(labDepositoPartenza, Color.red);
                FxUtils.fadeForeground(labDepositoDestinazione, Color.red);
                FxUtils.fadeForeground(deposito, Color.red);
                FxUtils.fadeForeground(deposito_arrivo, Color.red);
                return false;
            }
        }

        return true;
    }

private void butInserisciPesoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butInserisciPesoActionPerformed
    doc.setPrezziIvati(prezzi_ivati.isSelected());
    doc.setSconto(Db.getDouble(texSconto.getText()));
    doc.calcolaTotali();
    System.out.println("peso:" + doc.totalePeso);

    String dbSerie = this.dbdoc.serie;
    int dbNumero = this.dbdoc.numero;
    int dbAnno = this.dbdoc.anno;
    int riga = 0;

    //apre il resultset per ultimo +1
    Statement stat = null;
    ResultSet resu = null;
    try {
        stat = Db.getConn().createStatement();
        String sql = "select riga from " + getNomeTabRighe();
        sql += " where serie = " + Db.pc(dbSerie, "VARCHAR");
        sql += " and numero = " + dbNumero;
        sql += " and anno = " + dbAnno;
        sql += " order by riga desc limit 1";
        resu = stat.executeQuery(sql);
        if (resu.next() == true) {
            riga = resu.getInt(1) + iu.getRigaInc();
        } else {
            riga = iu.getRigaInc();
        }
    } catch (Exception err) {
        err.printStackTrace();
    } finally {
        try {
            stat.close();
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
    }
    sql = "insert into " + getNomeTabRighe() + " (serie, numero, anno, riga, codice_articolo, descrizione, id_padre) values (";
    sql += db.pc(dbSerie, "VARCHAR");
    sql += ", " + db.pc(dbNumero, "NUMBER");
    sql += ", " + db.pc(dbAnno, "NUMBER");
    sql += ", " + db.pc(riga, "NUMBER");
    sql += ", ''";
    if (main.getPersonalContain("litri")) {
        sql += ", '" + it.tnx.Util.format2Decimali(doc.totalePeso) + " Litri Totali'";
    } else {
        sql += ", 'Peso totale Kg. " + it.tnx.Util.format2Decimali(doc.totalePeso) + "'";
    }
    sql += ", " + Db.pc(id, Types.INTEGER);
    sql += ")";
    Db.executeSql(sql);

    griglia.dbRefresh();
}//GEN-LAST:event_butInserisciPesoActionPerformed

private void comClieFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieFocusGained
    comClieSel_old = comClie.getSelectedIndex();
}//GEN-LAST:event_comClieFocusGained

private void comClieDestFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieDestFocusGained
    comClieDest_old = comClieDest.getSelectedIndex();
}//GEN-LAST:event_comClieDestFocusGained

private void popGridAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popGridAddActionPerformed
    int numCol = this.griglia.getColumnByName("riga");
    int numRiga = this.griglia.getSelectedRow();
    int value = (Integer) this.griglia.getValueAt(numRiga, numCol);
    Integer id_riga = cu.i(griglia.getValueAt(numRiga, griglia.getColumnByName("id")));
    Integer id_padre = cu.i(griglia.getValueAt(numRiga, griglia.getColumnByName("id_padre")));

    String codiceListino = "1";

    try {
        codiceListino = Db.lookUp(this.texClie.getText(), "codice", "clie_forn").getString("codice_listino");
    } catch (Exception err) {
        System.out.println("butNuovArtiActionPerformed:" + err.toString());
    }

    int codiceCliente = -1;

    if (this.texClie.getText().length() > 0) {

        try {
            codiceCliente = Integer.parseInt(texClie.getText());
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

//        java.util.prefs.Preferences preferences = java.util.prefs.Preferences.userNodeForPackage(main.class);
//        boolean multiriga = preferences.getBoolean("multiriga", false);
    boolean multiriga = main.fileIni.getValueBoolean("pref", "multiriga", false);

//    if (multiriga == false) {
//        frmNuovRiga frm = new frmNuovRiga(this, this.dati.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNumePrev.getText()).intValue(), "P", value, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente);
//        temp.openFrame(frm, 600, 350, 100, 100);
//        frm.setStato();
//    } else {
    try {
        JInternalFrame frm = null;
        int w = 650;
        int h = 400;
        int top = 100;
        int left = 100;
        if (main.getPersonalContain("frajor")) {
            if (!Sync.isActive()) {
                frmNuovRigaDescrizioneMultiRigaNewFrajor temp_form = new frmNuovRigaDescrizioneMultiRigaNewFrajor(this, this.dati.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", value, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre);
                temp_form.setStato();
                w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_w", "700"));
                h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_h", "660"));
                top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_top", "100"));
                left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_left", "100"));
                frm = temp_form;
            } else {
                SwingUtils.showErrorMessage(this, "SYNC: form non gestita");
            }
        } else {
            frmNuovRigaDescrizioneMultiRigaNew temp_form = new frmNuovRigaDescrizioneMultiRigaNew(this, this.dati.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", value, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre, getNomeTabRighe());
            temp_form.setStato();
            w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_w", "980"));
            h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_h", "460"));
            top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_top", "100"));
            left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_left", "100"));
            frm = temp_form;
            temp_form.texProvvigione.setText(texProvvigione.getText());
        }

        main.getPadre().openFrame(frm, w, h, top, left);
    } catch (Exception e) {
        e.printStackTrace();
    }

//    }
}//GEN-LAST:event_popGridAddActionPerformed

private void formVetoableChange(java.beans.PropertyChangeEvent evt)throws java.beans.PropertyVetoException {//GEN-FIRST:event_formVetoableChange
    if (evt.getPropertyName().equals(IS_CLOSED_PROPERTY)) {
        boolean changed = ((Boolean) evt.getNewValue()).booleanValue();
        if (changed) {
            if (dati.dbCheckModificati() || (doc.getTotale() != this.totaleIniziale)) {
                FxUtils.fadeBackground(butSave, Color.RED);
                int confirm = JOptionPane.showOptionDialog(this,
                        "<html><b>Chiudi " + getTitle() + "?</b><br>Hai fatto delle modifiche e così verranno <b>perse</b> !<br>Per salvarle devi cliccare sul pulsante <b>Salva</b> in basso a sinistra<br>",
                        "Conferma chiusura",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, null, null);
                if (confirm == 0) {
                } else {
                    throw new PropertyVetoException("Cancelled", null);
                }
            }
            if (dbStato.equalsIgnoreCase(this.DB_INSERIMENTO)) {
                butUndoActionPerformed(null);
            }
        }

    }
}//GEN-LAST:event_formVetoableChange

private void texDataOraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texDataOraActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_texDataOraActionPerformed

private void butImportRigheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butImportRigheActionPerformed
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    JFileChooser fileChoose = new JFileChooser(new File(System.getProperty("user.home") + File.separator + ".invoicex" + File.separator + "Export" + File.separator));
    FileFilter filter1 = new FileFilter() {

        public boolean accept(File pathname) {
            if (pathname.getAbsolutePath().endsWith(".csv")) {
                return true;
            } else if (pathname.isDirectory()) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getDescription() {
            return "File CSV (*.csv)";
        }
    };

    fileChoose.addChoosableFileFilter(filter1);
    fileChoose.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

    int ret = fileChoose.showOpenDialog(this);

    if (ret == javax.swing.JFileChooser.APPROVE_OPTION) {
        ret = JOptionPane.showConfirmDialog(this, "Vuoi selezionare un listino prezzi esistente?", "Import CSV", JOptionPane.YES_NO_CANCEL_OPTION);
        String nomeListino = "";
        if (ret == JOptionPane.CANCEL_OPTION) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        } else if (ret == JOptionPane.YES_OPTION) {
            JDialogChooseListino dialog = new JDialogChooseListino(main.getPadre(), true);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            nomeListino = dialog.listinoChoose;

            if (nomeListino.equals("")) {
                nomeListino = "FromFile";
                JOptionPane.showMessageDialog(this, "Non hai scelto nessun listino. Il file verrà caricato con i prezzi interni al file stesso", "Errore Selezione", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            nomeListino = "FromFile";
        }
        try {
            //apro il file
            File f = fileChoose.getSelectedFile();
            String serie = texSeri.getText();
            int numero = Integer.valueOf(this.texNume.getText()).intValue();
            int anno = Integer.valueOf(this.texAnno.getText()).intValue();
            int idPadre = this.id;
            InvoicexUtil.importCSV(getTipoDoc(), f, serie, numero, anno, idPadre, nomeListino);
            InvoicexUtil.aggiornaTotaliRighe(getTipoDoc(), idPadre, prezzi_ivati_virtual.isSelected());
            griglia.dbRefresh();
            ricalcolaTotali();
            JOptionPane.showMessageDialog(this, "Righe caricate correttamente", "Esecuzione terminata", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_butImportRigheActionPerformed

private void grigliaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_grigliaMousePressed
    if (evt.isPopupTrigger()) {
        iu.impostaRigaSopraSotto(griglia, popGridAdd, popGrig, evt);
    }
}//GEN-LAST:event_grigliaMousePressed

private void grigliaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_grigliaMouseReleased
    if (evt.isPopupTrigger()) {
        iu.impostaRigaSopraSotto(griglia, popGridAdd, popGrig, evt);
    }
}//GEN-LAST:event_grigliaMouseReleased

private void comAgenteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comAgenteActionPerformed
}//GEN-LAST:event_comAgenteActionPerformed

private void comAgenteFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comAgenteFocusLost
    InvoicexUtil.controlloProvvigioniAutomatiche(comAgente, texProvvigione, texScon1, this, acquisto ? null : cu.toInteger(texClie.getText()));
}//GEN-LAST:event_comAgenteFocusLost

private void texProvvigioneFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texProvvigioneFocusLost
    if (!StringUtils.equals(texProvvigione.getName(), texProvvigione.getText()) && griglia.getRowCount() > 0 && !acquisto) {
        aggiornareProvvigioni();
    }
}//GEN-LAST:event_texProvvigioneFocusLost

private void texProvvigioneFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texProvvigioneFocusGained
    texProvvigione.setName(texProvvigione.getText());
}//GEN-LAST:event_texProvvigioneFocusGained

private void texAnnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texAnnoActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_texAnnoActionPerformed

private void listino_consigliatoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listino_consigliatoActionPerformed
}//GEN-LAST:event_listino_consigliatoActionPerformed

private void popDuplicaRigheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popDuplicaRigheActionPerformed
    if (griglia.getRowCount() <= 0) {
        SwingUtils.showErrorMessage(this, "Seleziona una riga prima!");
        return;
    }

    String sql;
    String sqlC = "";
    String sqlV = "";

    int numDup = griglia.getSelectedRows().length;
    int res;
    //chiedo conferma per eliminare il documento
    if (numDup > 1) {
        String msg = "Sicuro di voler duplicare " + numDup + " Righe ?";
        res = JOptionPane.showConfirmDialog(this, msg);
    } else {
        res = JOptionPane.OK_OPTION;
    }

    if (res == JOptionPane.OK_OPTION) {
        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        for (int sel : griglia.getSelectedRows()) {

            int dbId = Integer.parseInt(String.valueOf(griglia.getValueAt(sel, griglia.getColumnByName("id"))));

            //cerco ultimo numero ordine
            int newNumero = 1;
            sqlC = "";
            sqlV = "";

            try {
                int dbIdPadre = (Integer) DbUtils.getObject(Db.getConn(), "SELECT id_padre FROM " + getNomeTabRighe() + " WHERE id = " + Db.pc(dbId, Types.INTEGER));
                sql = "SELECT MAX(riga) as maxnum FROM " + getNomeTabRighe() + " WHERE id_padre = " + Db.pc(dbIdPadre, Types.INTEGER);
                ResultSet tempUltimo = Db.openResultSet(sql);
                if (tempUltimo.next() == true) {
                    newNumero = tempUltimo.getInt("maxnum") + InvoicexUtil.getRigaInc();
                }
            } catch (Exception err) {
                err.printStackTrace();
            }

            sql = "select * from " + getNomeTabRighe() + " where id = " + Db.pc(dbId, Types.INTEGER);
            ResultSet tempPrev2 = Db.openResultSet(sql);
            try {
                ResultSetMetaData metaPrev2 = tempPrev2.getMetaData();

                while (tempPrev2.next() == true) {
                    sqlC = "";
                    sqlV = "";
                    for (int i = 1; i <= metaPrev2.getColumnCount(); i++) {
                        if (!metaPrev2.getColumnName(i).equalsIgnoreCase("id")) {
                            if (metaPrev2.getColumnName(i).equalsIgnoreCase("riga")) {
                                sqlC += "riga";
                                sqlV += Db.pc(newNumero, metaPrev2.getColumnType(i));
                            } else {
                                sqlC += metaPrev2.getColumnName(i);
                                sqlV += Db.pc(tempPrev2.getObject(i), metaPrev2.getColumnType(i));
                            }
                            if (i != metaPrev2.getColumnCount()) {
                                sqlC += ",";
                                sqlV += ",";
                            }
                        }
                    }
                    sql = "insert into " + getNomeTabRighe();
                    sql += "(" + sqlC + ") values (" + sqlV + ")";
                    System.out.println("duplica righe:" + sql);
                    Db.executeSql(sql);
                }
            } catch (Exception err) {
                err.printStackTrace();
            }

        }
        griglia.dbRefresh();
        this.ricalcolaTotali();

        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
}//GEN-LAST:event_popDuplicaRigheActionPerformed

private void prezzi_ivati_virtualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prezzi_ivati_virtualActionPerformed
    prezzi_ivati.setSelected(prezzi_ivati_virtual.isSelected());
    InvoicexUtil.aggiornaTotaliRighe(Db.TIPO_DOCUMENTO_FATTURA, this.id, prezzi_ivati_virtual.isSelected());
    dbAssociaGrigliaRighe();
    ricalcolaTotali();
}//GEN-LAST:event_prezzi_ivati_virtualActionPerformed

private void texScontoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScontoKeyPressed

}//GEN-LAST:event_texScontoKeyPressed

private void texScontoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScontoKeyReleased
    double valore = CastUtils.toDouble0(texSconto.getText());
    if (valore < 0) {
        valore = Math.abs(valore);
        texSconto.setText(FormatUtils.formatEuroIta(valore));
    }
    ricalcolaTotali();
}//GEN-LAST:event_texScontoKeyReleased

    private void apriclientiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apriclientiActionPerformed
        //    alRicercaCliente.showHints();
        if (texCliente.getText().trim().length() == 0) {
            al_clifor.showHints2();
            al_clifor.updateHints(null);
            al_clifor.showHints2();
        } else {
            al_clifor.showHints();
        }
        //    al_clifor.showHints();
    }//GEN-LAST:event_apriclientiActionPerformed

    private void butAddClieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butAddClieActionPerformed
        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        InvoicexUtil.genericFormAddCliente(this);
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_butAddClieActionPerformed

    private void butPdfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butPdfActionPerformed
        butStampaActionPerformed(new ActionEvent(this, 0, "pdf"));
    }//GEN-LAST:event_butPdfActionPerformed

    private void butImportXlsCirriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butImportXlsCirriActionPerformed
        InvoicexUtil.importRigheXlsCirri(this);
    }//GEN-LAST:event_butImportXlsCirriActionPerformed

    private void comConsegnaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comConsegnaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comConsegnaActionPerformed

    private void comScaricoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comScaricoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comScaricoActionPerformed

    private void texAccontoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texAccontoKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_texAccontoKeyPressed

    private void texAccontoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texAccontoKeyReleased
        double valore = CastUtils.toDouble0(texAcconto.getText());
        if (valore < 0) {
            valore = Math.abs(valore);
            texAcconto.setText(FormatUtils.formatEuroIta(valore));
        }
        ricalcolaTotali();
    }//GEN-LAST:event_texAccontoKeyReleased

    private void menClienteNuovoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menClienteNuovoActionPerformed
        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        InvoicexUtil.genericFormAddCliente(this);
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_menClienteNuovoActionPerformed

    private void menClienteModificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menClienteModificaActionPerformed
        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        InvoicexUtil.genericFormEditCliente(this, texClie.getText());
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_menClienteModificaActionPerformed

    private void texClienteMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_texClienteMousePressed
        if (evt.isPopupTrigger()) {
            menClientePopup.show(texCliente, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_texClienteMousePressed

    private void texClienteMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_texClienteMouseReleased
        if (evt.isPopupTrigger()) {
            menClientePopup.show(texCliente, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_texClienteMouseReleased

    private void comCampoLibero1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comCampoLibero1FocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_comCampoLibero1FocusGained

    private void comCampoLibero1PopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_comCampoLibero1PopupMenuWillBecomeVisible
        if (comCampoLibero1.getItemCount() == 0) {
            InvoicexUtil.caricaComboTestateCampoLibero1(comCampoLibero1);
        }
    }//GEN-LAST:event_comCampoLibero1PopupMenuWillBecomeVisible

    private void menColAggNoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menColAggNoteActionPerformed
        main.fileIni.setValue("pref", "ColAgg_righe_note", menColAggNote.isSelected());
        java.util.Hashtable colsWidthPerc = griglia.columnsSizePerc;
        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            colsWidthPerc.put("note", 15d);
        }
        griglia.columnsSizePercOrig = null;
        dbAssociaGrigliaRighe();
    }//GEN-LAST:event_menColAggNoteActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apriclienti;
    private javax.swing.JButton butAddClie;
    private javax.swing.JButton butImportRighe;
    private javax.swing.JButton butImportXlsCirri;
    private javax.swing.JButton butInserisciPeso;
    public javax.swing.JButton butNuovArti;
    private javax.swing.JButton butPdf;
    private javax.swing.JButton butPrezziPrec;
    private javax.swing.JButton butSave;
    private javax.swing.JButton butStampa;
    public javax.swing.JButton butUndo;
    private tnxbeans.tnxComboField comAgente;
    private tnxbeans.tnxComboField comAspettoEsterioreBeni;
    private tnxbeans.tnxComboField comCampoLibero1;
    private tnxbeans.tnxComboField comCausaleTrasporto;
    public tnxbeans.tnxComboField comClie;
    private tnxbeans.tnxComboField comClieDest;
    private tnxbeans.tnxComboField comConsegna;
    private tnxbeans.tnxComboField comMezzoTrasporto;
    private tnxbeans.tnxComboField comPaese;
    private tnxbeans.tnxComboField comPaga;
    private tnxbeans.tnxComboField comPorto;
    private tnxbeans.tnxComboField comScarico;
    private tnxbeans.tnxComboField comVettori;
    public tnxbeans.tnxDbPanel dati;
    public tnxbeans.tnxDbPanel datiAltro;
    private tnxbeans.tnxDbPanel datiRighe;
    private tnxbeans.tnxDbPanel dati_dest_diversa;
    public tnxbeans.tnxDbGrid griglia;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel113;
    private javax.swing.JLabel jLabel114;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel151;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel labAgente;
    private javax.swing.JLabel labCampoLibero1;
    private javax.swing.JLabel labModConsegna;
    private javax.swing.JLabel labModScarico;
    private javax.swing.JLabel labNoteConsegna;
    private javax.swing.JLabel labPercentoProvvigione;
    private javax.swing.JLabel labProvvigione;
    private javax.swing.JLabel labRiferimento;
    private javax.swing.JLabel labScon1;
    private javax.swing.JLabel labScon10;
    private javax.swing.JLabel labScon11;
    private javax.swing.JLabel labScon12;
    private javax.swing.JLabel labScon13;
    private javax.swing.JLabel labScon14;
    private javax.swing.JLabel labScon15;
    private javax.swing.JLabel labScon16;
    private javax.swing.JLabel labScon17;
    private javax.swing.JLabel labScon2;
    private javax.swing.JLabel labScon21;
    private javax.swing.JLabel labScon3;
    private javax.swing.JLabel labScon4;
    private javax.swing.JLabel labScon5;
    private javax.swing.JLabel labScon6;
    private javax.swing.JLabel labScon7;
    private javax.swing.JLabel labScon8;
    private javax.swing.JLabel labScon9;
    private tnxbeans.tnxComboField listino_consigliato;
    private javax.swing.JMenuItem menClienteModifica;
    private javax.swing.JMenuItem menClienteNuovo;
    private javax.swing.JPopupMenu menClientePopup;
    private javax.swing.JMenu menColAgg;
    private javax.swing.JCheckBoxMenuItem menColAggNote;
    private javax.swing.JPanel panDati;
    public javax.swing.JPanel pan_segnaposto_depositi;
    private javax.swing.JMenuItem popDuplicaRighe;
    private javax.swing.JMenuItem popGridAdd;
    private javax.swing.JPopupMenu popGrig;
    private javax.swing.JMenuItem popGrigElim;
    private javax.swing.JMenuItem popGrigModi;
    public tnxbeans.tnxCheckBox prezzi_ivati;
    private javax.swing.JCheckBox prezzi_ivati_virtual;
    private javax.swing.JScrollPane scrollDati;
    private javax.swing.JSeparator sepDestMerce;
    private tnxbeans.tnxCheckBox stampa_prezzi;
    private javax.swing.JComboBox stato_evasione;
    private javax.swing.JTabbedPane tab;
    public tnxbeans.tnxTextField texAcconto;
    private tnxbeans.tnxTextField texAnno;
    public tnxbeans.tnxTextField texClie;
    private tnxbeans.tnxTextField texClieDest;
    public javax.swing.JTextField texCliente;
    private tnxbeans.tnxTextField texData;
    private tnxbeans.tnxTextField texDataOra;
    private tnxbeans.tnxTextField texDestCap;
    private tnxbeans.tnxTextField texDestCellulare;
    private tnxbeans.tnxTextField texDestIndirizzo;
    private tnxbeans.tnxTextField texDestLocalita;
    private tnxbeans.tnxTextField texDestProvincia;
    private tnxbeans.tnxTextField texDestRagioneSociale;
    private tnxbeans.tnxTextField texDestTelefono;
    private tnxbeans.tnxMemoField texNote;
    private tnxbeans.tnxMemoField texNoteConsegna;
    private tnxbeans.tnxTextField texNume;
    private tnxbeans.tnxTextField texNumeroColli;
    private tnxbeans.tnxTextField texPesoLordo;
    private tnxbeans.tnxTextField texPesoNetto;
    private tnxbeans.tnxTextField texProvvigione;
    private tnxbeans.tnxTextField texRiferimento;
    public tnxbeans.tnxTextField texScon1;
    private tnxbeans.tnxTextField texScon10;
    public tnxbeans.tnxTextField texScon2;
    public tnxbeans.tnxTextField texScon3;
    private tnxbeans.tnxTextField texScon4;
    private tnxbeans.tnxTextField texScon5;
    private tnxbeans.tnxTextField texScon6;
    private tnxbeans.tnxTextField texScon7;
    private tnxbeans.tnxTextField texScon8;
    private tnxbeans.tnxTextField texScon9;
    public tnxbeans.tnxTextField texSconto;
    private tnxbeans.tnxTextField texSeri;
    public tnxbeans.tnxTextField texSpeseIncasso;
    public tnxbeans.tnxTextField texSpeseTrasporto;
    private tnxbeans.tnxTextField texStat;
    public tnxbeans.tnxTextField texTota;
    private tnxbeans.tnxTextField texTota1;
    public tnxbeans.tnxTextField texTotaDaPagareFinale;
    public tnxbeans.tnxTextField texTotaImpo;
    private tnxbeans.tnxTextField texTotaImpo1;
    public tnxbeans.tnxTextField texTotaIva;
    private tnxbeans.tnxTextField texTotaIva1;
    // End of variables declaration//GEN-END:variables

    void dbAssociaGrigliaRighe() {
        
        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            menColAggNote.setSelected(true);
        }
        

        String campi = "serie,";
        campi += "numero,";
        campi += "anno,";
        campi += "riga,";
        campi += "stato,";
        campi += "codice_articolo as articolo,";
        campi += "descrizione,";
        campi += "um,";

        campi += "quantita,";
        campi += "quantita_evasa AS '" + getCampoQtaEvasa() + "',";

        campi += "prezzo, ";
        campi += "sconto1 as Sconti, ";
        campi += "sconto2, ";
        campi += "(totale_imponibile) as Totale ";
        campi += ", iva ";
        campi += ", (totale_ivato) as Ivato ";
        campi += ",r.id";
        campi += ",r.id_padre";

        if (main.isPluginContabilitaAttivo()) {
            campi += ", conto";
        }
        
        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            campi += ", n.note";
        }        

        String sql = "select " + campi + " from " + getNomeTabRighe() + " r ";

        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            sql += " left join note n on n.tabella = '" + getNomeTabRighe() + "' and n.id_tab = r.id";
        }        

        sql += " where id_padre = " + id;
        sql += " order by riga";

        //debug
        //javax.swing.JOptionPane.showMessageDialog(null,sql);
        System.out.println("sql associa griglia:" + sql);

        griglia.colonneEditabiliByName = new String[]{getCampoQtaEvasa()};
        griglia.dbEditabile = true;

        this.griglia.dbOpen(db.getConn(), sql);
        griglia.getColumn("quantita").setCellRenderer(InvoicexUtil.getNumber0_5Renderer());
        griglia.getColumn(getCampoQtaEvasa()).setCellRenderer(InvoicexUtil.getNumber0_5Renderer());

        griglia.getColumn("Sconti").setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lab = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Double sconto1 = cu.d(table.getValueAt(row, column));
                String ssconto1 = fu.formatNum0_5Dec(sconto1);
                Double sconto2 = cu.d(table.getValueAt(row, column + 1));
                String ssconto2 = fu.formatNum0_5Dec(sconto2);
                lab.setText("");
                lab.setHorizontalAlignment(SwingConstants.CENTER);
                if (sconto1 != null && sconto1 != 0) {
                    lab.setText(ssconto1);
                }
                if (sconto2 != null && sconto2 != 0) {
                    lab.setText(lab.getText() + " + " + ssconto2);
                }
                return lab;
            }

        });

        DecimalFormat df1 = new DecimalFormat("0.#####");
        griglia.getColumn(getCampoQtaEvasa()).setCellEditor(new EditorUtils.NumberEditor(new JTextField(), df1) {
            public Object getCellEditorValue() {
                String text = ((JTextField) editorComponent).getText();
                Double qta_evasa = CastUtils.toDouble0All(text);
                System.out.println("text:" + text + " qta_evasa:" + qta_evasa);
                return qta_evasa;
            }
        });
    }
    
    private String getNomeTabRighe() {
        if (table_righe_temp != null) {
            return table_righe_temp;
        }
        return "righ_ddt" + suff;
    }
    
    public String getNomeTabRigheLotti() {
        if (table_righe_lotti_temp != null) {
            return table_righe_lotti_temp;
        }
        return "righ_ddt" + suff + "_lotti";
    }

    public String getNomeTabRigheMatricole() {
        if (table_righe_matricole_temp != null) {
            return table_righe_matricole_temp;
        }
        return "righ_ddt" + suff + "_matricole";
    }

    public void recuperaDatiCliente() {

        //li recupero dal cliente
        ResultSet tempClie;
        String sql = "select * from clie_forn";
        sql += " where codice = " + Db.pc(this.texClie.getText(), "NUMERIC");
        tempClie = Db.openResultSet(sql);

        try {
            if (tempClie.next() == true) {

                //tecnospurghi preferisce che rimanga con il codice fra parentesi quadre dopo la selezione
//                texCliente.setText(cu.s(tempClie.getString("ragione_sociale")));
                if (Db.nz(tempClie.getString("pagamento"), "").length() > 0) {
                    this.comPaga.dbTrovaRiga(Db.nz(tempClie.getString("pagamento"), ""));
                }

                if (tempClie.getInt("agente") >= 0) {
                    if (main.getPersonalContain("medcomp")) {
                        //selezionare gli agenti in base a quelli collegati al cliente fornitore
                        Integer cod_cliente = null;
                        try {
                            cod_cliente = cu.toInteger(texClie.getText());
                        } catch (Exception e) {
                        }
                        InvoicexUtil.caricaComboAgentiCliFor(comAgente, cod_cliente);
                    } else {
                        comAgente.dbTrovaKey(tempClie.getString("agente"));
                    }
                    comAgenteFocusLost(null);
                }

                if (tempClie.getInt("tipo_causale_trasporto") >= 0) {
                    comCausaleTrasporto.dbTrovaKey(tempClie.getInt("tipo_causale_trasporto"));
                }
                if (tempClie.getInt("tipo_consegna") >= 0) {
                    comMezzoTrasporto.dbTrovaKey(tempClie.getInt("tipo_consegna"));
//                    int id_tipo_consegna = tempClie.getInt("tipo_consegna");
//                    String sql_tipo_consegna = "select nome from tipi_consegna where id=" + id_tipo_consegna;
//                    ArrayList<Map> list_tipo_consegna = DbUtils.getListMap(Db.getConn(), sql_tipo_consegna);
//                    if (list_tipo_consegna.size() > 0) {
//                        comMezzoTrasporto.setSelectedItem(list_tipo_consegna.get(0).get("nome"));
//                    }
                } else {
                    //TODO Workaround, riseleziono il valore di default.
                    comMezzoTrasporto.setSelectedItem("");
                }

                if (Db.nz(tempClie.getString("opzione_prezzi_ddt"), "").equalsIgnoreCase("S")) {
                    stampa_prezzi.setSelected(true);
                } else {
                    stampa_prezzi.setSelected(false);
                }

                //carico sconti
                texScon1.setText(FormatUtils.formatPerc(tempClie.getObject("sconto1t"), true));
                texScon2.setText(FormatUtils.formatPerc(tempClie.getObject("sconto2t"), true));
                texScon3.setText(FormatUtils.formatPerc(tempClie.getObject("sconto3t"), true));

                //leggere listino del cliente per prezzi_ivati o meno
                boolean prezzi_ivati_b = false;
                try {
                    String prezzi_ivati_s = CastUtils.toString(DbUtils.getObject(Db.getConn(), "select l.prezzi_ivati from clie_forn c join tipi_listino l on c.codice_listino = l.codice where c.codice = " + Db.pc(this.texClie.getText(), "NUMERIC")));
                    if (prezzi_ivati_s.equalsIgnoreCase("S")) {
                        prezzi_ivati_b = true;
                    }
                } catch (Exception e) {
                    //prendo base da impostazioni
                    String prezzi_ivati_s = CastUtils.toString(DbUtils.getObject(Db.getConn(), "select l.prezzi_ivati from dati_azienda a join tipi_listino l on a.listino_base = l.codice"));
                    if (prezzi_ivati_s.equalsIgnoreCase("S")) {
                        prezzi_ivati_b = true;
                    }
                }
                if (prezzi_ivati_virtual.isSelected() != prezzi_ivati_b) {
                    prezzi_ivati_virtual.setSelected(prezzi_ivati_b);
                    prezzi_ivati.setSelected(prezzi_ivati_b);
                    prezzi_ivati_virtualActionPerformed(null);
                }

                aggiornaNote();

                InvoicexUtil.fireEvent(this, InvoicexEvent.TYPE_FRMTESTDDT_CARICA_DATI_CLIENTE, tempClie);

            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "Il codice cliente specificato non esiste in anagrafica !");
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    private void aggiornaNote() {
        if (loading) {
            return;
        }
        //note automatiche
        String sqlTmp = "SELECT note_docu, note_docu_acq, modalita_consegna, modalita_scarico, note_consegna FROM clie_forn where codice = " + Db.pc(String.valueOf(comClie.getSelectedKey()), "NUMERIC");
        ResultSet res = Db.openResultSet(sqlTmp);
        try {
            if (res.next()) {
                String key_standard = acquisto ? "noteStandardDdtAcquisto" : "noteStandardDdt";
                String key_cliente = acquisto ? "note_docu_acq" : "note_docu";

                String note_standard = main.fileIni.getValue("pref", key_standard, "");
                String note_cliente = cu.s(res.getString(key_cliente));

                String nuove_note = note_standard + (StringUtils.isNotBlank(texNote.getText()) ? "\n" : "") + note_cliente;

                Map map_diversi = new HashMap();
                if (!texNote.getText().equalsIgnoreCase(nuove_note)) {
                    map_diversi.put("Note", nuove_note);
                }
                if (!comConsegna.getText().equalsIgnoreCase(cu.s(res.getObject("modalita_consegna")))) {
                    map_diversi.put("Modalità di consegna", cu.s(res.getObject("modalita_consegna")));
                }
                if (!comScarico.getText().equalsIgnoreCase(cu.s(res.getObject("modalita_scarico")))) {
                    map_diversi.put("Modalità di scarico", cu.s(res.getObject("modalita_scarico")));
                }
                if (texNoteConsegna.isVisible() && !texNoteConsegna.getText().equalsIgnoreCase(cu.s(res.getObject("note_consegna")))) {
                    map_diversi.put("Note di consegna", res.getString("note_consegna"));
                }
                boolean sovrascrivere = true;
                if (map_diversi.size() > 0 && dbStato.equals(tnxDbPanel.DB_MODIFICA)) {
                    if (!SwingUtils.showYesNoMessage(this, "Sovrascrivere i seguenti dati:" + map_diversi.keySet() + "\nCon i dati dall'anagrafica cliente/fornitore o dalle note predefinite ?", "Attenzione")) {
                        sovrascrivere = false;
                    }
                }
                if (sovrascrivere) {
                    texNote.setText(nuove_note);
                    //consegna e scarico
                    try {
                        comConsegna.dbTrovaKey(res.getObject("modalita_consegna"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        comScarico.dbTrovaKey(res.getObject("modalita_scarico"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        texNoteConsegna.setText(res.getString("note_consegna"));
                    } catch (Exception e) {
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            dbu.close(res);
        }
    }

    private void showPrezziFatture() {
        frmPrezziFatturePrecedenti form = new frmPrezziFatturePrecedenti(Integer.parseInt(this.texClie.getText().toString()), null, getTipoDoc());
        main.getPadre().openFrame(form, 450, 500, this.getY() + 50, this.getX() + this.getWidth() - 200);
    }

    private void visualizzaTarga() {

//        if (Db.nz(this.comCausaleTrasporto.getSelectedItem(), "").toString().equalsIgnoreCase("TENTATA VENDITA")) {
//            this.texClie.setText("");
//            this.texClie.setEnabled(false);
//            this.comClie.setSelectedItem(null);
//            this.comClie.setEnabled(false);
//            this.comClie.setLocked(true);
//
//            if (this.texTarga.getText().trim().length() == 0) {
//                this.texTarga.setText(main.getTargaStandard());
//            }
//
//            this.labTarga.setVisible(true);
//            this.texTarga.setVisible(true);
//            this.labRiferimento.setVisible(false);
//            this.texRiferimento.setVisible(false);
//        } else {
        this.comClie.setEnabled(true);
        this.comClie.setLocked(false);
        this.texClie.setEnabled(true);
        //this.labTarga.setVisible(false);
        //this.texTarga.setVisible(false);
        this.labRiferimento.setVisible(true);
        this.texRiferimento.setVisible(true);
//        }

    }

    public void ricalcolaTotali() {
        try {
            if (texClie.getText() != null && texClie.getText().length() > 0) {
                doc.setCodiceCliente(Long.parseLong(texClie.getText()));
            }

            doc.setAcconto(Db.getDouble(texAcconto.getText()));
            doc.setScontoTestata1(Db.getDouble(texScon1.getText()));
            doc.setScontoTestata2(Db.getDouble(texScon2.getText()));
            doc.setScontoTestata3(Db.getDouble(texScon3.getText()));
            doc.setSpeseIncasso(Db.getDouble(texSpeseIncasso.getText()));
            doc.setSpeseTrasporto(Db.getDouble(texSpeseTrasporto.getText()));
            doc.setPrezziIvati(prezzi_ivati.isSelected());
            doc.setSconto(Db.getDouble(texSconto.getText()));
            doc.calcolaTotali();
            texTota.setText(it.tnx.Util.formatValutaEuro(doc.getTotale()));
            texTotaImpo.setText(it.tnx.Util.formatValutaEuro(doc.getTotaleImponibile()));
            texTotaIva.setText(it.tnx.Util.formatValutaEuro(doc.getTotaleIva()));
            texTotaDaPagareFinale.setText(it.tnx.Util.formatValutaEuro(doc.getTotale_da_pagare_finale()));
        } catch (Exception err) {
            err.printStackTrace();
        }

        //calcolo totale numero colli 
        if (!loading) {
            InvoicexUtil.calcolaColli(griglia, texNumeroColli);
        }
    }

    synchronized private void riempiDestDiversa(String sql) {
        boolean oldrefresh = dati.isRefreshing;
        dati.isRefreshing = true;
        comClieDest.setRinominaDuplicati(true);
        comClieDest.dbClearList();
        comClieDest.dbAddElement("", "");
        System.out.println("*** riempiDestDiversa *** sql:" + sql);
        comClieDest.dbOpenList(Db.getConn(), sql, this.texClieDest.getText(), false);
        dati.isRefreshing = oldrefresh;
    }

    public void aggiornareProvvigioni() {
        block_aggiornareProvvigioni = true;
        if (SwingUtils.showYesNoMessage(this, "Vuoi aggiornare le provvigioni delle righe già inserite alla nuova provvigione ?")) {
            int id = InvoicexUtil.getIdDdt(dbdoc.serie, dbdoc.numero, dbdoc.anno);
            String sql = "update " + getNomeTabRighe() + " set provvigione = " + Db.pc(cu.toDouble0(texProvvigione.getText()), Types.DOUBLE) + " where id_padre = " + id;
            try {
                DbUtils.tryExecQuery(Db.getConn(), sql);
                griglia.dbRefresh();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        block_aggiornareProvvigioni = false;
    }

    public JTable getGrid() {
        return griglia;
    }

    public tnxDbGrid getGrigliaInitComp() {
        return new tnxDbGrid() {
            //ovveride del save

            @Override
            public void saveDataEntry(int row) {
                //non faccio niente e salvo solo cosa voglio io
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                double qta_evasa = CastUtils.toDouble0(aValue);
                qta_evasa = FormatUtils.round(qta_evasa, 5);
                aValue = qta_evasa;
                super.setValueAt(aValue, row, column);
                //salvo in tabella riga ddt la qta evasa
                String tabr = getNomeTabRighe();
                try {
                    Integer idriga = CastUtils.toInteger(getValueAt(row, getColumnByName("id")));
                    String sql = "update " + tabr + " set quantita_evasa = " + Db.pc(CastUtils.toDouble(aValue), Types.DOUBLE) + " where id = " + idriga;
                    System.out.println("sql = " + sql);
                    DbUtils.tryExecQuery(Db.getConn(), sql);
                    aggiornaStatoEvasione();
                } catch (Exception e) {
                    SwingUtils.showExceptionMessage(this, e);
                }
            }

            public void changeSelection(final int row, final int column, boolean toggle, boolean extend) {
                super.changeSelection(row, column, toggle, extend);
                System.out.println("changeSel:" + row + " " + column);
                if (editCellAt(row, column)) {
                    Component comp = getEditorComponent();
                    comp.requestFocusInWindow();
                    if (comp instanceof JTextField) {
                        JTextField textComp = (JTextField) comp;
                        textComp.selectAll();
                    }
                }
            }
        };
    }

    private void aggiornaStatoEvasione() {
        String evaso = InvoicexUtil.getStatoEvasione(griglia, "quantita", getCampoQtaEvasa());
        if ("S".equalsIgnoreCase(evaso)) {
            stato_evasione.setSelectedIndex(0);
        } else if ("P".equalsIgnoreCase(evaso)) {
            stato_evasione.setSelectedIndex(1);
        } else {
            stato_evasione.setSelectedIndex(2);
        }

    }

    private String getTipoDoc() {
        return acquisto ? Db.TIPO_DOCUMENTO_DDT_ACQUISTO : Db.TIPO_DOCUMENTO_DDT;
    }

    private String getStatoEvaso() {
        if (stato_evasione.getSelectedIndex() == 0) {
            return "S";
        }
        if (stato_evasione.getSelectedIndex() == 1) {
            return "P";
        } else {
            return "";
        }
    }

    private String getCampoQtaEvasa() {
//        return acquisto ? "qta arrivata" : "qta evasa";
        return "qta fatturata";
    }

    public tnxDbPanel getDatiPanel() {
        return dati;
    }

    public JTabbedPane getTab() {
        return tab;
    }

    public boolean isAcquisto() {
        return acquisto;
    }

    public void selezionaCliente() {
        //apro combo destinazione cliente
        sql = "select ragione_sociale, id from clie_forn_dest";
        sql += " where codice_cliente = " + Db.pc(this.texClie.getText(), "NUMERIC");
        sql += " order by ragione_sociale";
        riempiDestDiversa(sql);

        recuperaDatiCliente();

        //salto perchè lo fa già dentro recuperaDatiCliente
        //aggiornaNote();
        ricalcolaTotali();

    }

    public Integer getId() {
        return this.id;
    }

    public boolean isPrezziIvati() {
        return prezzi_ivati_virtual.isSelected();
    }

    private void porto_in_temp() {
        if (!main.edit_doc_in_temp) {
            //controllo tabella temp
            String sql = "check table righ_ddt" + suff + "_temp";
            try {
                DbUtils.dumpResultSet(Db.getConn(), sql);
                ResultSet r = Db.openResultSet(sql);
                if (r.next()) {
                    if (!r.getString("Msg_text").equalsIgnoreCase("OK")) {
                        SwingUtils.showErrorMessage(main.getPadre(), "Errore durante il controllo della tabella temporanea [1]");
                    }
                } else {
                    SwingUtils.showErrorMessage(main.getPadre(), "Errore durante il controllo della tabella temporanea [2]");
                }
            } catch (Exception e) {
                SwingUtils.showErrorMessage(main.getPadre(), "Errore durante il controllo della tabella temporanea\n" + e.toString());
            }

            //tolgo le righe da temp che tanto non serbono +
            sql = "delete from righ_ddt" + suff + "_temp";
            sql += " where id_padre = " + frmTestDocu.this.id;
            sql += " and username = '" + main.login + "'";
            Db.executeSqlDialogExc(sql, true);

            sql = "delete te.* from righ_ddt" + suff + "_temp te join righ_ddt" + suff + " ri on te.id = ri.id";
            sql += " and ri.serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
            sql += " and ri.numero " + Db.pcW(String.valueOf(this.dbdoc.numero), "NUMBER");
            sql += " and ri.anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
            sql += " and te.username = '" + main.login + "'";
            Db.executeSqlDialogExc(sql, true);

            //e inserisco
            sql = "insert into righ_ddt" + suff + "_temp";
            sql += " select *, '" + main.login + "' as username";
            sql += " from righ_ddt" + suff + "";
            sql += " where id_padre = " + frmTestDocu.this.id;
            Db.executeSqlDialogExc(sql, true);
        } else {
            System.out.println("porto_in_temp, NO per edit in mem");            
        }
    }

    public JLabel getCampoLibero1Label() {
        return labCampoLibero1;
    }

    public tnxComboField getCampoLibero1Combo() {
        return comCampoLibero1;
    }

    private void controllaPermessiAnagCliFor() {
        butAddClie.setEnabled(false);
        menClienteNuovo.setEnabled(false);
        menClienteModifica.setEnabled(false);
        if (main.utente.getPermesso(Permesso.PERMESSO_ANAGRAFICA_CLIENTI, Permesso.PERMESSO_TIPO_SCRITTURA)) {
            butAddClie.setEnabled(true);
            menClienteNuovo.setEnabled(true);
            menClienteModifica.setEnabled(true);
        }
    }
    
    public void selezionaCliente(String codice) {
        texClie.setText(codice);
        try {
            String cliente = (String) DbUtils.getObject(Db.getConn(), "select ragione_sociale from clie_forn where codice = " + Db.pc(texClie.getText(), Types.INTEGER));
            texCliente.setText(cliente);
        } catch (Exception e) {
            e.printStackTrace();
        }
        recuperaDatiCliente();
    }
    
}
