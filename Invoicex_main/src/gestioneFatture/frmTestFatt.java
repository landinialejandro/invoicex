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
package gestioneFatture;

import gestioneFatture.logic.clienti.Cliente;
import gestioneFatture.logic.documenti.Documento;
import it.tnx.accessoUtenti.Permesso;
import it.tnx.commons.AutoCompletionEditable;
import it.tnx.commons.CastUtils;
import it.tnx.commons.DbUtils;
import it.tnx.commons.DebugUtils;
import it.tnx.commons.FormatUtils;
import it.tnx.commons.FxUtils;
import it.tnx.commons.MicroBench;
import it.tnx.commons.SwingUtils;
import it.tnx.commons.cu;
import it.tnx.commons.dbu;
import it.tnx.commons.fu;
import it.tnx.commons.ju;
import it.tnx.commons.table.EditorUtils;
import it.tnx.commons.table.RendererUtils;
import it.tnx.gui.JTableSs;
import it.tnx.gui.MyBasicArrowButton;
import it.tnx.gui.MyOsxFrameBorder;

import it.tnx.invoicex.InvoicexUtil;
import it.tnx.invoicex.MyAbstractListIntelliHints;
import it.tnx.invoicex.gui.utils.CellEditorFoglio;
import it.tnx.invoicex.gui.utils.NumeroRigaCellEditor;
import it.tnx.invoicex.iu;
import it.tnx.invoicex.sync.Sync;
import it.tnx.proto.LockableCircularBusyPainterUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.JInternalFrame;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.TimingUtils;
import net.java.balloontip.utils.ToolTipUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.jdesktop.jxlayer_old.JXLayer;
import tnxbeans.LimitedTextPlainDocument;
import tnxbeans.SeparatorComboBoxRenderer;
import tnxbeans.tnxCheckBox;
import tnxbeans.tnxComboField;
import tnxbeans.tnxDbPanel;
import tnxbeans.tnxTextField;

public class frmTestFatt extends javax.swing.JInternalFrame implements GenericFrmTest {

    public dbFattura dbdoc = new dbFattura();
    public Documento doc = new Documento();
    public frmElenFatt from;
    private Db db = Db.INSTANCE;
    int pWidth;
    int pHeight;
    public String dbStato = "L";
    public static String DB_INSERIMENTO = "I";
    public static String DB_MODIFICA = "M";
    public static String DB_LETTURA = "L";
    private String sql = "";
    private double totaleDaPagareFinaleIniziale;
    private String pagamentoIniziale;
    private String pagamentoInizialeGiorno;
    //private int tempTipoFatt = 0;
    //per controllare le provvigioni
    private double provvigioniIniziale;
    private String provvigioniInizialeScadenze;
    private int codiceAgenteIniziale;
    private double provvigioniTotaleIniziale;
    //per foglio righe
    private DataModelFoglio foglioData;
    public boolean loadingFoglio = false;
    public boolean editingFoglio = false;
    private String sqlGriglia;
    java.util.Timer tim;
    FoglioSelectionListener foglioSelList;
    javax.swing.JInternalFrame zoom;
    private String old_id = "";
    private boolean id_modificato = false;
    private String old_anno = "";
    private String old_data = "";
    private boolean anno_modificato = false;
    private int comClieSel_old = -1;
    private int comClieDest_old = -1;
    private String serie_originale = null;
    private Integer numero_originale = null;
    private Integer anno_originale = null;
    private String data_originale = null;

    public Integer id = null;
    public boolean in_apertura = false;
//    LockableUI lockableUI = new LockableBusyPainterUI();
    public LockableCircularBusyPainterUI lockableUI = new LockableCircularBusyPainterUI();
    ArrayList<Runnable> toRun = new ArrayList<Runnable>();
    org.jdesktop.swingworker.SwingWorker worker = null;
    public Throwable trow = null;
    private boolean block_aggiornareProvvigioni;
//    AbstractListIntelliHints alRicercaCliente = null;
    MyAbstractListIntelliHints al_clifor = null;
    AtomicReference<ClienteHint> clifor_selezionato_ref = new AtomicReference(null);

    public tnxbeans.tnxComboField deposito;
    public javax.swing.JLabel labDeposito;
    public tnxDbPanel pan_deposito = null;

    boolean chiudere = true;
    private Integer foglio_last_sel_row = null;
    private boolean foglio_ins_art_in_corso = false;

    private int contaopen = 0;
    private String old_bollo;

    BalloonTip balloon_bollo;
    
    String suff = "";   //qui sempre di vendita
    
    public String table_righe_temp = null;
    public String table_righe_lotti_temp = null;
    public String table_righe_matricole_temp = null;
    

    public frmTestFatt() {
        initComponents();
        
        if (Sync.isActive()) {
            tabDocumento.remove(panFoglioRighe);
        }

        //campi liberi
        InvoicexUtil.initCampiLiberiTestate(this);

        LimitedTextPlainDocument limit = new LimitedTextPlainDocument(1, true);
        texSeri.setDocument(limit);
                
        if (main.versione.equalsIgnoreCase("base")) {
            menColAggNote.setEnabled(false);
        } else {
            menColAggNote.setIcon(null);
            menColAggNote.setEnabled(true);
            menColAggNote.setToolTipText(null);
        }
    }
    

    public void init(final String dbStato, final String dbSerie, final int dbNumero, String prevStato, final int dbAnno, int tipoFattura, int dbIdFattura) {
        in_apertura = true;

        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        this.dbStato = dbStato;
        this.id = dbIdFattura;

        //this.tempTipoFatt = tipoFattura;
        System.out.println("SwingUtilities.isEventDispatchThread(): = " + SwingUtilities.isEventDispatchThread());

        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setDismissDelay(30000);

        try {
            scrollDatiPa.getVerticalScrollBar().setUnitIncrement(16);
        } catch (Exception e) {
        }

        comValuta.setVisible(false);
        labvaluta.setVisible(false);

        if (!main.getPersonalContain("cirri")) {
            butImportXlsCirri.setVisible(false);
        }

        if (main.getPersonalContain("consegna_e_scarico")) {
            split.setDividerLocation(324);
        } else {
            split.setDividerLocation(300);
            labModConsegna.setVisible(false);
            labModScarico.setVisible(false);
            comConsegna.setVisible(false);
            comScarico.setVisible(false);
            labNoteConsegna.setVisible(false);
            texNoteConsegna.setVisible(false);
        }

//        if(!main.utente.getPermesso(Permesso.PERMESSO_FATTURE_VENDITA, Permesso.PERMESSO_TIPO_SCRITTURA)){
//            SwingUtils.showErrorMessage(main.getPadrePanel(), "Non hai i permessi per accedere a questa funzionalità", "Impossibile accedere", true);
//            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
//            this.setVisible(false);
//            this.dispose();
//            return;
//        }
        controllaPermessiAnagCliFor();

        InvoicexUtil.macButtonSmall(butPrezziPrec);

        try {
            AutoCompletionEditable.enable(comCausaleTrasporto);
            AutoCompletionEditable.enable(comAspettoEsterioreBeni);
            AutoCompletionEditable.enable(comVettori);
            AutoCompletionEditable.enable(comMezzoTrasporto);
            AutoCompletionEditable.enable(comPorto);
        } catch (Exception e) {
            e.printStackTrace();
        }

        prezzi_ivati.setVisible(false);

        comClie.putClientProperty("JComponent.sizeVariant", "small");
        comClieDest.putClientProperty("JComponent.sizeVariant", "small");
        comAgente.putClientProperty("JComponent.sizeVariant", "small");
        comPaga.putClientProperty("JComponent.sizeVariant", "small");

        comCausaleTrasporto.putClientProperty("JComponent.sizeVariant", "mini");
        comAspettoEsterioreBeni.putClientProperty("JComponent.sizeVariant", "mini");
        comVettori.putClientProperty("JComponent.sizeVariant", "mini");
        comMezzoTrasporto.putClientProperty("JComponent.sizeVariant", "mini");
        comMezzoTrasporto.putClientProperty("JComponent.sizeVariant", "mini");
        comPorto.putClientProperty("JComponent.sizeVariant", "mini");
        comForni.putClientProperty("JComponent.sizeVariant", "mini");
        comPaese.putClientProperty("JComponent.sizeVariant", "mini");
        comValuta.putClientProperty("JComponent.sizeVariant", "mini");

        butPrezziPrec.putClientProperty("JComponent.sizeVariant", "mini");

        if (SystemUtils.IS_OS_MAC_OSX) {
            tutto.setBorder(new MyOsxFrameBorder());

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
            compsb.add(bollo_importo);
            compsb.add(texNote);
            compsb.add(texPaga2);
            compsb.add((JComponent) comPaga.getEditor().getEditorComponent());
            compsb.add(texGiornoPagamento);
            compsb.add(texNotePagamento);
            compsb.add(texBancAbi);
            compsb.add(texBancCab);
            compsb.add(texBancIban);
            compsb.add((JComponent) comAgente.getEditor().getEditorComponent());
            compsb.add(texProvvigione);
            compsb.add((JComponent) comConsegna.getEditor().getEditorComponent());
            compsb.add((JComponent) comScarico.getEditor().getEditorComponent());

            for (JComponent c : compsb) {
                c.setBorder(b1);
            }

        }

        texNote.setFont(texSeri.getFont());

        if (main.getPersonalContain("carburante")) {
            jLabel114.setText("spese carburante");
        }
        if (main.getPersonalContain("litri")) {
            butInserisciPeso.setText("Inserisci Tot. Litri");
        }
        System.out.println("SwingUtilities.isEventDispatchThread(): = " + SwingUtilities.isEventDispatchThread());

        tutto.remove(tabDocumento);
        tutto.remove(jPanel5);

        JXLayer<JComponent> l = new JXLayer<JComponent>(tutto, lockableUI);
        lockableUI.setLocked(true);
        add(l);

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

        texDestRagioneSociale.setMargin(new Insets(0, 0, 0, 0));
        texDestIndirizzo.setMargin(new Insets(0, 0, 0, 0));
        texDestCap.setMargin(new Insets(0, 0, 0, 0));
        texDestLocalita.setMargin(new Insets(0, 0, 0, 0));
        texDestProvincia.setMargin(new Insets(0, 0, 0, 0));
        texDestTelefono.setMargin(new Insets(0, 0, 0, 0));
        texDestCellulare.setMargin(new Insets(0, 0, 0, 0));
//        texVettore1.setMargin(new Insets(0, 0, 0, 0));
        ((JTextComponent) comVettori.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
        texForni.setMargin(new Insets(0, 0, 0, 0));
        texNumeroColli.setMargin(new Insets(0, 0, 0, 0));

        try {
            ((JTextComponent) comPaese.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
            ((JTextComponent) comCausaleTrasporto.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
            ((JTextComponent) comAspettoEsterioreBeni.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
            ((JTextComponent) comMezzoTrasporto.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
            ((JTextComponent) comPorto.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
            ((JTextComponent) comForni.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
            ((JTextComponent) comValuta.getEditor().getEditorComponent()).setMargin(new Insets(0, 0, 0, 0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            InvoicexEvent event = new InvoicexEvent(this);
            event.type = InvoicexEvent.TYPE_FRMTESTFATT_CONSTR_POST_INIT_COMPS;
            main.events.fireInvoicexEvent(event);
        } catch (Exception err) {
            err.printStackTrace();
        }

        foglioData = new DataModelFoglio(1000, 12, this);
        foglio.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        foglio.setModel(foglioData);
        foglio_last_sel_row = 0;
        foglio.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                System.out.println("foglio change sel PRE " + e.getValueIsAdjusting() + " - " + e.getFirstIndex() + " " + e.getLastIndex() + " " + foglio.getSelectedRow());
//                if (!e.getValueIsAdjusting() && (e.getLastIndex() - e.getFirstIndex() <= 1)) {
//                    System.out.println("foglio change sel POST " + e.getFirstIndex() + " " + e.getLastIndex() + " " + foglio.getSelectedRow());

                //controllo se articolo della riga di prima è presente in anagrafica se non presente propongo inserimento
                foglioCheckInsArt(foglio_last_sel_row);

                foglio_last_sel_row = foglio.getSelectedRow();
//                }
            }

            private void foglioCheckInsArt(Integer row) {
                String codice = cu.s(foglio.getValueAt(row, 1));

                String descr = cu.s(foglio.getValueAt(row, 2));
                String um = cu.s(foglio.getValueAt(row, 3));
                Double prezzo = cu.d(foglio.getValueAt(row, 5));

                System.out.println("foglioCheckInsArt row:" + row + "(" + foglio.getValueAt(row, 0) + ") codice = " + codice + " descr = " + descr + " prezzo = " + prezzo);

                if (codice.trim().equals("")) {
                    return;
                }

                foglio_ins_art_in_corso = true;

                try {
                    String codiceListino = cu.s(dbu.getObject(Db.getConn(), "select listino_base from dati_azienda", false));
                    if (texClie.getText().length() > 0) {
                        try {
                            String codiceListinoClie = cu.s(Db.lookUp(texClie.getText(), "codice", "clie_forn").getString("codice_listino"));
                            if (codiceListinoClie.trim() != "") {
                                codiceListino = codiceListinoClie;
                            }
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                    System.out.println("codiceListino = " + codiceListino);

                    Object dbcodice = dbu.getObject(Db.getConn(), "select codice from articoli where codice = " + dbu.sql(codice), false);
                    if (dbcodice == null) {
                        //non esiste articolo
                        if (SwingUtils.showYesNoMessage(frmTestFatt.this, "L'articolo " + codice + " non esiste in anagrafica, vuoi inserirlo ?")) {
                            if (foglio.isEditing()) {
                                foglio.getCellEditor().stopCellEditing();
                            }

                            setCursor(new Cursor(Cursor.WAIT_CURSOR));
                            frmArtiConListino frm = new frmArtiConListino();
                            main.getPadrePanel().openFrame(frm, 850, InvoicexUtil.getHeightIntFrame(750));
                            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                            frm.butNew();
                            frm.texCodi.setText(codice);
                            frm.texDescrizione.setText(descr);
                            frm.texUm.setText(um);

                            //frm.tabListino.setValueAt(prezzo, 1, 1);
                            String listino_std = null;
                            try {
                                String sql = "select codice_listino from clie_forn cf "
                                        + " join tipi_listino l on cf.codice_listino = l.codice"
                                        + " where cf.codice = " + dbu.sql(frmTestFatt.this.texClie.getText());
                                listino_std = cu.s(dbu.getObject(Db.getConn(), sql));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                listino_std = cu.s(dbu.getObject(Db.getConn(), "select listino_base from dati_azienda"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            int rowtoedit = 0;
                            try {
                                if (listino_std != null) {
                                    for (int rowl = 0; rowl < frm.tabListino.getRowCount(); rowl++) {
                                        if (frm.tabListino.getValueAt(rowl, frm.tabListino.getColumnModel().getColumnIndex("codice")).equals(listino_std)) {
                                            rowtoedit = rowl;
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                frm.tabListino.setValueAt(prezzo, rowtoedit, frm.tabListino.getColumnModel().getColumnIndex("prezzo"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            frm.requestFocus();
                            frm.texCodi.requestFocus();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                foglio_ins_art_in_corso = false;

            }

        });

        //foglio.getColumn(foglio.getColumnName(0)).setHeaderValue("riga");
        //foglio.getColumn(foglio.getColumnName(0)).setPreferredWidth(30);
        javax.swing.table.TableColumnModel columns = foglio.getColumnModel();
        javax.swing.table.TableColumn col = columns.getColumn(0);

        EditorUtils.NumberEditor numedit = new EditorUtils.NumberEditor();
        numedit.returnNull = true;
        EditorUtils.CurrencyEditor curedit = new EditorUtils.CurrencyEditor();
        curedit.returnNull = true;
        EditorUtils.CurrencyEditor curedit25 = new EditorUtils.CurrencyEditor(2, 5);
        curedit25.returnNull = true;
        RendererUtils.NumberRenderer numrend05 = new RendererUtils.NumberRenderer(0, 5);
        RendererUtils.NumberRenderer numrend02 = new RendererUtils.NumberRenderer(0, 2);
        RendererUtils.CurrencyRenderer currend = new RendererUtils.CurrencyRenderer();
        RendererUtils.CurrencyRenderer currend25 = new RendererUtils.CurrencyRenderer(2, 5);

        col.setHeaderValue("riga");
        col.setPreferredWidth(30);
        col = columns.getColumn(1);
        col.setHeaderValue("codice art.");
        col.setPreferredWidth(50);
        col = columns.getColumn(2);
        col.setHeaderValue("descrizione");
        col.setPreferredWidth(200);
        col = columns.getColumn(3);
        col.setHeaderValue("um");
        col.setPreferredWidth(20);
        col = columns.getColumn(4);
        col.setHeaderValue("qta");
        col.setPreferredWidth(40);
        col.setCellEditor(numedit);
        col.setCellRenderer(numrend05);

        col = columns.getColumn(5);
        col.setHeaderValue("prezzo");
        col.setPreferredWidth(80);
        col.setCellEditor(curedit25);
        col.setCellRenderer(currend25);

        col = columns.getColumn(6);
        col.setHeaderValue("sc.1");
        col.setPreferredWidth(30);
        col.setCellEditor(numedit);
        col.setCellRenderer(numrend02);

        col = columns.getColumn(7);
        col.setHeaderValue("sc.2");
        col.setCellEditor(numedit);
        col.setCellRenderer(numrend02);
        col.setMaxWidth(0);
        col.setMinWidth(0);
        col.setPreferredWidth(0);
        col.setWidth(0);
        col.setResizable(false);
        col = columns.getColumn(8);
        col.setHeaderValue("importo");
        col.setPreferredWidth(80);
        col.setCellEditor(curedit);
        col.setCellRenderer(currend);

        col = columns.getColumn(9);
        col.setHeaderValue("iva");
        col.setPreferredWidth(30);

        col = columns.getColumn(10);
        col.setHeaderValue("id");
        col.setMaxWidth(0);
        col.setMinWidth(0);
        col.setPreferredWidth(0);
        col.setWidth(0);

        col = columns.getColumn(11);
        col.setHeaderValue("note");
        if (main.versione.equalsIgnoreCase("Base")) {
            col.setMaxWidth(0);
            col.setMinWidth(0);
            col.setPreferredWidth(0);
            col.setWidth(0);
        } else {
            col.setPreferredWidth(100);
        }

        JTextField textEdit = new javax.swing.JTextField() {
        };

        CellEditorFoglio edit = new CellEditorFoglio(textEdit);

        //it.tnx.gui.KeyableCellEditor edit = new it.tnx.gui.KeyableCellEditor();
        //edit.setClickCountToStart(0);
        edit.setClickCountToStart(2);
        foglio.setDefaultEditor(Object.class, edit);

        NumeroRigaCellEditor editriga = new NumeroRigaCellEditor(textEdit);
        editriga.colonna = 0;
        foglio.getColumnModel().getColumn(0).setCellEditor(editriga);

        loadingFoglio = true;
        for (int i = 0; i < foglioData.getRowCount(); i++) {
            foglioData.setValueAt(new Integer((i + 1) * iu.getRigaInc()), i, 0);
        }
        loadingFoglio = false;

        FoglioSelectionListener foglioSelList = new FoglioSelectionListener(foglio);
        foglio.getSelectionModel().addListSelectionListener(foglioSelList);

        //--- fine foglio ---------
        this.griglia.dbEditabile = false;

        //init campi particolari
        this.texData.setDbDefault(tnxTextField.DEFAULT_CURRENT);

        //oggetto preventivo
        this.dbdoc.dbStato = dbStato;
        this.dbdoc.serie = dbSerie;
        this.dbdoc.numero = dbNumero;
        this.dbdoc.stato = prevStato;
        this.dbdoc.anno = dbAnno;
        this.dbdoc.setId(this.id);

        //105
        this.dbdoc.tipoFattura = tipoFattura;
        this.dbdoc.texTota = this.texTota;
        this.dbdoc.texTotaImpo = this.texTotaImpo;
        this.dbdoc.texTotaIva = this.texTotaIva;
//        this.setClosable(false);

        //faccio copia in caso di annulla deve rimettere le righe giuste
        if (!main.edit_doc_in_temp) {
            if (dbStato.equals(frmTestFatt.DB_MODIFICA)) {
                porto_in_temp();
                //memorizzo il numero doc originale
                serie_originale = dbSerie;
                numero_originale = dbNumero;
                anno_originale = dbAnno;
            }
        } else {
            //porto righe in tabella temporanea e modifica quella temporanea, se poi si conferma le porto nelle righe definitive
            table_righe_temp = InvoicexUtil.getTempTableName("righ_fatt" + suff);
            table_righe_lotti_temp = InvoicexUtil.getTempTableName("righ_fatt" + suff + "_lotti");
            table_righe_matricole_temp = InvoicexUtil.getTempTableName("righ_fatt" + suff + "_matricole");
            try {
                InvoicexUtil.createTempTable(dbStato, table_righe_temp, "righ_fatt" + suff, id);
                InvoicexUtil.createTempTable(dbStato, table_righe_lotti_temp, "righ_fatt" + suff + "_lotti", id);
                InvoicexUtil.createTempTable(dbStato, table_righe_matricole_temp, "righ_fatt" + suff + "_matricole", id);
                dbdoc.table_righe_temp = table_righe_temp;
                doc.table_righe_temp = table_righe_temp;
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
        frmTestFatt.this.dati.dbNomeTabella = "test_fatt";

        dati.messaggio_nuovo_manuale = true;

        dati.dbChiaveAutoInc = true;

//        Vector chiave = new Vector();
//        chiave.add("serie");
//        chiave.add("numero");
//        chiave.add("anno");
        Vector chiave = new Vector();
        chiave.add("id");
        frmTestFatt.this.dati.dbChiave = chiave;

        dati.aggiungiDbPanelCollegato(datiAltro);

//        //aggiungo il verifier alla casella dell' agente e della percentuale
//        class AgenteInputVerifier extends InputVerifier implements ActionListener {
//
//            @Override
//            public boolean verify(JComponent input) {
//                System.out.println("input = " + input);
//                return false;
//            }
//
//            public void actionPerformed(ActionEvent e) {
//                System.out.println("AgenteInputVerifier e:" + e);
//            }
//        
//        }
//        AgenteInputVerifier verifier = new AgenteInputVerifier();
//        comAgente.setInputVerifier(verifier);
//        texProvvigione.setInputVerifier(verifier);
        //rimesso qui perchè c'è rischio che il lookup che fa dopo non abbia ancora i pagamenti caricati
        comPaga.dbOpenList(db.getConn(), "select codice, codice from pagamenti order by codice", null, false);

        comConsegna.dbOpenList(db.getConn(), "select nome, id from tipi_consegna", null, false);
        comScarico.dbOpenList(db.getConn(), "select nome, id from tipi_scarico", null, false);

        //xmlpa
        //ritenuta
        dg_dr_tipo_ritenuta.dbAddElement("", "");
        dg_dr_tipo_ritenuta.dbAddElement("RT01 - Ritenuta persone fisiche", "RT01");
        dg_dr_tipo_ritenuta.dbAddElement("RT02 - Ritenuta persone giuridiche", "RT02");
        dg_dr_causale_pagamento.dbAddElement("", "");
        dg_dr_causale_pagamento.dbAddElement("A - Prestazioni di lavoro autonomo rientranti nell’esercizio di arte o professione abituale.", "A");
        dg_dr_causale_pagamento.dbAddElement("B - Utilizzazione economica, da parte dell’autore o dell’inventore, di opere dell’ingegno ...", "B");
        dg_dr_causale_pagamento.dbAddElement("C - Utili derivanti da contratti di associazione in partecipazione e da contratti di cointeressenza ...", "C");
        dg_dr_causale_pagamento.dbAddElement("D - Utili spettanti ai soci promotori e ai soci fondatori delle società di capitali.", "D");
        dg_dr_causale_pagamento.dbAddElement("E - Levata di protesti cambiari da parte dei segretari comunali.", "E");
        dg_dr_causale_pagamento.dbAddElement("G - Indennità corrisposte per la cessazione di attività sportiva professionale.", "G");
        dg_dr_causale_pagamento.dbAddElement("H - Indennità corrisposte per la cessazione dei rapporti di agenzia delle p. fisiche e delle soc. di persone ...", "H");
        dg_dr_causale_pagamento.dbAddElement("I - Indennità corrisposte per la cessazione da funzioni notarili.", "I");
        dg_dr_causale_pagamento.dbAddElement("L - Utilizzazione economica, da parte di soggetto diverso dall’autore o dall’inventore, di opere dell’ingegno ...", "L");
        dg_dr_causale_pagamento.dbAddElement("L1 - redditi derivanti dall’utilizzazione economica di opere dell’ingegno, di brevetti industriali e ...", "L1");
        dg_dr_causale_pagamento.dbAddElement("M - Prestazioni di lavoro aut. non esercitate abitualmente, obblighi di fare, di non fare o permettere.", "M");
        dg_dr_causale_pagamento.dbAddElement("M1 - redditi derivanti dall’assunzione di obblighi di fare, di non fare o permettere", "M1");
        dg_dr_causale_pagamento.dbAddElement("N - Indennità di trasferta, rimborso forfetario di spese, premi e compensi erogati ...", "N");
        dg_dr_causale_pagamento.dbAddElement("O - Prestazioni di lavoro aut. non esercitate abitualmente, senza obbligo di iscrizione alla gest. separata ...", "O");
        dg_dr_causale_pagamento.dbAddElement("O1 - redditi derivanti dall’assunzione di obblighi di fare, di non fare o permettere, per le quali non sussiste ...", "O1");
        dg_dr_causale_pagamento.dbAddElement("P - Compensi corrisposti a soggetti non residenti privi di stabile organizzazione per l’uso ...", "P");
        dg_dr_causale_pagamento.dbAddElement("Q - Provvigioni corrisposte ad agente o rappresentante di commercio monomandatario.", "Q");
        dg_dr_causale_pagamento.dbAddElement("R - Provvigioni corrisposte ad agente o rappresentante di commercio plurimandatario.", "R");
        dg_dr_causale_pagamento.dbAddElement("S - Provvigioni corrisposte a commissionario.", "S");
        dg_dr_causale_pagamento.dbAddElement("T - Provvigioni corrisposte a mediatore.", "T");
        dg_dr_causale_pagamento.dbAddElement("U - Provvigioni corrisposte a procacciatore di affari.", "U");
        dg_dr_causale_pagamento.dbAddElement("V - Provvigioni corrisposte per le vendite a domicilio / ambulante di giornali quotidiani e periodici ...", "V");
        dg_dr_causale_pagamento.dbAddElement("V1 - redditi derivanti da attività commerciali non esercitate abitualmente (ad esempio, provvigioni corrisposte ...", "V1");
        dg_dr_causale_pagamento.dbAddElement("W - Corrispettivi erogati nel 2013 per prestazioni relative a contratti d’appalto ...", "W");
        dg_dr_causale_pagamento.dbAddElement("X - Canoni corrisposti nel 2004 da società o enti residenti, ovvero da stabili organizzazioni di società estere ...", "X");
        dg_dr_causale_pagamento.dbAddElement("Y - Canoni corrisposti dal 1.01.2005 al 26.07.2005 da soggetti di cui al punto precedente.", "Y");
        dg_dr_causale_pagamento.dbAddElement("Z - Titolo diverso dai precedenti.", "Z");
        dg_dr_totale_da_esportare.dbAddElement("Totale lordo (Predefinito)", "lordo");
        dg_dr_totale_da_esportare.dbAddElement("Totale da pagare", "netto");

        //rivalsa
        dg_dcp_tipo_cassa.dbAddElement("", "");
        dg_dcp_tipo_cassa.dbAddElement("TC01 - Cassa nazionale previdenza e assistenza avvocati e procuratori legali", "TC01");
        dg_dcp_tipo_cassa.dbAddElement("TC02 - Cassa previdenza dottori commercialisti", "TC02");
        dg_dcp_tipo_cassa.dbAddElement("TC03 - Cassa previdenza e assistenza geometri", "TC03");
        dg_dcp_tipo_cassa.dbAddElement("TC04 - Cassa nazionale previdenza e assistenza ingegneri e architetti liberi professionisti", "TC04");
        dg_dcp_tipo_cassa.dbAddElement("TC05 - Cassa nazionale del notariato", "TC05");
        dg_dcp_tipo_cassa.dbAddElement("TC06 - Cassa nazionale previdenza e assistenza ragionieri e periti commerciali", "TC06");
        dg_dcp_tipo_cassa.dbAddElement("TC07 - Ente nazionale assistenza agenti e rappresentanti di commercio (ENASARCO)", "TC07");
        dg_dcp_tipo_cassa.dbAddElement("TC08 - Ente nazionale previdenza e assistenza consulenti del lavoro (ENPACL)", "TC08");
        dg_dcp_tipo_cassa.dbAddElement("TC09 - Ente nazionale previdenza e assistenza medici (ENPAM)", "TC09");
        dg_dcp_tipo_cassa.dbAddElement("TC10 - Ente nazionale previdenza e assistenza farmacisti (ENPAF)", "TC10");
        dg_dcp_tipo_cassa.dbAddElement("TC11 - Ente nazionale previdenza e assistenza veterinari (ENPAV)", "TC11");
        dg_dcp_tipo_cassa.dbAddElement("TC12 - Ente nazionale previdenza e assistenza impiegati dell'agricoltura (ENPAIA)", "TC12");
        dg_dcp_tipo_cassa.dbAddElement("TC13 - Fondo previdenza impiegati imprese di spedizione e agenzie marittime", "TC13");
        dg_dcp_tipo_cassa.dbAddElement("TC14 - Istituto nazionale previdenza giornalisti italiani (INPGI)", "TC14");
        dg_dcp_tipo_cassa.dbAddElement("TC15 - Opera nazionale assistenza orfani sanitari italiani (ONAOSI)", "TC15");
        dg_dcp_tipo_cassa.dbAddElement("TC16 - Cassa autonoma assistenza integrativa giornalisti italiani (CASAGIT)", "TC16");
        dg_dcp_tipo_cassa.dbAddElement("TC17 - Ente previdenza periti industriali e periti industriali laureati (EPPI)", "TC17");
        dg_dcp_tipo_cassa.dbAddElement("TC18 - Ente previdenza e assistenza pluricategoriale (EPAP)", "TC18");
        dg_dcp_tipo_cassa.dbAddElement("TC19 - Ente nazionale previdenza e assistenza biologi (ENPAB)", "TC19");
        dg_dcp_tipo_cassa.dbAddElement("TC20 - Ente nazionale previdenza e assistenza professione infermieristica (ENPAPI)", "TC20");
        dg_dcp_tipo_cassa.dbAddElement("TC21 - Ente nazionale previdenza e assistenza psicologi (ENPAP)", "TC21");
        dg_dcp_tipo_cassa.dbAddElement("TC22 - INPS", "TC22");

        worker = new org.jdesktop.swingworker.SwingWorker<Object, Object>() {
            ArrayList<Object[]> paesi = null;
            ArrayList<Object[]> agenti = null;
            ArrayList<Object[]> clienti = null;
            ArrayList<Object[]> fornitori = null;
            ArrayList<Object[]> valute = null;

            MicroBench mb = new MicroBench(true);

            @Override
            protected Object doInBackground() throws Exception {
                //apertura delle combo                
                try {
                    paesi = DbUtils.getListArray(Db.getConn(), "select nome, codice1 from stati");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    agenti = DbUtils.getListArray(Db.getConn(), "select nome, id from agenti where id != 0 and IFNULL(nome,'') != '' order by nome");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    clienti = DbUtils.getListArray(Db.getConn(), "select ragione_sociale,codice from clie_forn order by ragione_sociale");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    fornitori = DbUtils.getListArray(Db.getConn(), "select ragione_sociale,codice from clie_forn where codice != '0' and IFNULL(ragione_sociale,'') != '' order by ragione_sociale");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mb.out("apertura testfatt background");

                return null;
            }

            @Override
            protected void done() {

                comPaese.dbAddElement("", "");
                comPaese.dbOpenListArray(paesi, null);

                mb.out("comPaese");

                comClie.dbOpenListArray(clienti, frmTestFatt.this.texClie.getText());

                mb.out("comClie");

                comForni.dbOpenListArray(fornitori, frmTestFatt.this.texForni.getText());

                mb.out("comForni");

//                comValuta.dbOpenListArray(valute, cu.s(frmTestFatt.this.dati.dbGetFieldOnlyNotClosed("valuta")));
                //105 metto titolo finestra per sapere se fattura o altro
                if (dbdoc.tipoFattura == dbFattura.TIPO_FATTURA_NON_IDENTIFICATA) {
                    //prev.tipoFattura = Integer.valueOf(frmTestFatt.this.texTipoFattura.getText()).intValue();
                    //leggo da db
                    sql = "select tipo_fattura from test_fatt";
                    sql += " where id = " + frmTestFatt.this.id;
                    System.err.println("dbopen tipo_fattura:" + sql);
                    try {
                        dbdoc.tipoFattura = CastUtils.toInteger(DbUtils.getObject(Db.getConn(), sql));
                    } catch (Exception e) {
                    }
                }

                setTipoFattura(dbdoc.tipoFattura);

                mb.out("setTipoFattura");

                texForni1.setVisible(false);

                comClie.setDbTextAbbinato(frmTestFatt.this.texClie);
                texClie.setDbComboAbbinata(frmTestFatt.this.comClie);
                comForni.setDbTextAbbinato(frmTestFatt.this.texForni);
                texForni.setDbComboAbbinata(frmTestFatt.this.comForni);

//this.dati.butSave = this.butSave;
                //this.dati.butUndo = this.butUndo;
                //controllo se inserimento o modifica
                System.out.println("*** *** *** APERTURA DATI");

                if (main.getPersonalContain("medcomp")) {
                    //selezionare gli agenti in base a quelli collegati al cliente fornitore
                    comAgente.setRenderer(new SeparatorComboBoxRenderer());
                    Integer cod_cliente = null;
                    try {
                        if (frmTestFatt.this.id != null) {
                            cod_cliente = cu.toInteger(DbUtils.getObject(Db.getConn(), "select cliente from test_fatt where id = " + dbu.sql(frmTestFatt.this.id)));
                        }
                    } catch (Exception e) {
                    }
                    InvoicexUtil.caricaComboAgentiCliFor(comAgente, cod_cliente);
                    it.tnx.commons.AutoCompletionEditable.enable(comAgente);
                } else {
                    comAgente.dbAddElement("", "");
                    comAgente.dbOpenListArray(agenti, null);
                }

                mb.out("comAgente");

                System.out.println("*** agente pre : " + comAgente.getSelectedItem() + " : " + comAgente.getSelectedIndex());

                if (dbStato.equalsIgnoreCase(frmTestFatt.DB_INSERIMENTO)) {
                    frmTestFatt.this.dati.dbOpen(Db.getConn(), "select * from test_fatt limit 0");
                } else {
                    boolean bollo_presente = false;
                    try {
                        bollo_presente = cu.toBoolean(dbu.getObject(Db.getConn(), "select bollo_presente from test_fatt where id = " + frmTestFatt.this.id));
                    } catch (Exception e) {
                    }
                    if (bollo_presente) {
                        bollo_si_no.setSelected(true);
                        bollo_importo.setEnabled(true);
                        doc.speseBolloSiNo = true;
                    }

                    sql = "select * from test_fatt";
                    sql += " where id = " + frmTestFatt.this.id;

                    contaopen++;
                    System.err.println("dbopen conta:" + contaopen);

                    frmTestFatt.this.dati.dbOpen(Db.getConn(), sql);
                }

                mb.out("apertura dati");

                System.out.println("*** agente post: " + comAgente.getSelectedItem() + " : " + comAgente.getSelectedIndex());

                //righe
                //apro la griglia
                frmTestFatt.this.griglia.dbNomeTabella = getNomeTabRighe();

                java.util.Hashtable colsWidthPerc = new java.util.Hashtable();
                colsWidthPerc.put("serie", new Double(0));
                colsWidthPerc.put("numero", new Double(0));
                colsWidthPerc.put("anno", new Double(0));
                colsWidthPerc.put("stato", new Double(0));
                colsWidthPerc.put("riga", new Double(5));
                colsWidthPerc.put("articolo", new Double(10));
                colsWidthPerc.put("descrizione", new Double(35));
                colsWidthPerc.put("um", new Double(5));
                colsWidthPerc.put("quantita", new Double(10));
                colsWidthPerc.put("prezzo", new Double(15));
                colsWidthPerc.put("sconto1", new Double(0));
                colsWidthPerc.put("sconto2", new Double(0));
                colsWidthPerc.put("iva", new Double(5));
                colsWidthPerc.put("Totale", new Double(10));
                colsWidthPerc.put("Ivato", new Double(10));
                colsWidthPerc.put("Sconti", new Double(10));
                colsWidthPerc.put("provvigione", new Double(7));
                colsWidthPerc.put("id", new Double(0));
                if (main.isPluginContabilitaAttivo()) {
                    colsWidthPerc.put("conto", new Double(10));
                }
                if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
                    colsWidthPerc.put("note", 15d);
                }                
                frmTestFatt.this.griglia.columnsSizePerc = colsWidthPerc;

                java.util.Hashtable colsAlign = new java.util.Hashtable();
                colsAlign.put("quantita", "RIGHT_CURRENCY");
                colsAlign.put("prezzo", "RIGHT_CURRENCY");
                frmTestFatt.this.griglia.columnsAlign = colsAlign;
                frmTestFatt.this.griglia.flagUsaOrdinamento = false;

                //        Vector chiave2 = new Vector();
                //        chiave2.add("serie");
                //        chiave2.add("numero");
                //        chiave2.add("anno");
                //        chiave2.add("riga");
                Vector chiave2 = new Vector();
                chiave2.add("id");
                frmTestFatt.this.griglia.dbChiave = chiave2;
                if (dbStato.equalsIgnoreCase(frmTestFatt.DB_INSERIMENTO)) {
                } else {
                    //disabilito la data perch??? non tornerebbe pi??? la chiave per numero e anno
                    //siccome tutti vogliono modificarsi la data che se la modifichino...
                    //this.texData.setEditable(false);
                    frmTestFatt.this.dbdoc.sconto1 = Db.getDouble(frmTestFatt.this.texScon1.getText());
                    frmTestFatt.this.dbdoc.sconto2 = Db.getDouble(frmTestFatt.this.texScon2.getText());
                    frmTestFatt.this.dbdoc.sconto3 = Db.getDouble(frmTestFatt.this.texScon3.getText());

                    //this.prev.speseVarie = Db.getDouble(this.texSpesVari.getText());
                    frmTestFatt.this.dbdoc.speseTrasportoIva = Db.getDouble(frmTestFatt.this.texSpeseTrasporto.getText());
                    frmTestFatt.this.dbdoc.speseIncassoIva = Db.getDouble(frmTestFatt.this.texSpeseIncasso.getText());
                }

                texForni.setText(texForni1.getText());
                comForni.setSelectedIndex(-1);
                if (!frmTestFatt.this.texForni1.getText().equals("")) {
                    boolean continua = true;
                    for (int i = 0; i < comForni.getItemCount() && continua; i++) {
                        Integer tempchiave = Integer.parseInt(String.valueOf(comForni.getKey(i)));
                        if (Integer.parseInt(texForni.getText()) == tempchiave) {
                            comForni.setSelectedIndex(i);
                            continua = false;
                        }
                    }
                }

                mb.out("apertura dati 2");

                if (dbStato.equalsIgnoreCase(frmTestFatt.DB_INSERIMENTO)) {
                    inserimento();
                    texSconto.setText("0");
                    texAcconto.setText("0");
                    prezzi_ivati_virtual.setSelected(prezzi_ivati.isSelected());
                    bollo_importo.setText("");
                    InvoicexUtil.fireEvent(frmTestFatt.this, InvoicexEvent.TYPE_FRMTESTFATT_INIT_INSERIMENTO);
                } else {
                    texSconto.setText(FormatUtils.formatEuroIta(CastUtils.toDouble0(dati.dbGetField("sconto"))));
                    texAcconto.setText(FormatUtils.formatEuroIta(CastUtils.toDouble0(dati.dbGetField("acconto"))));
                    prezzi_ivati_virtual.setSelected(prezzi_ivati.isSelected());

                    mb.out("dopoInserimento 1");
                    dopoInserimento();
                    mb.out("dopoInserimento 2");
                    //xmlpa
                    initXmlPa();
                }

                mb.out("init xmlpa");

                //apro combo destinazione cliente
                comClieDest.dbTrovaMentreScrive = false;
                sql = "select ragione_sociale, id from clie_forn_dest";
                sql += " where codice_cliente = " + Db.pc(frmTestFatt.this.texClie.getText(), "NUMERIC");
                sql += " order by ragione_sociale";
                riempiDestDiversa(sql);

                mb.out("riempidest diversa");

                boolean azioniPericolose = main.fileIni.getValueBoolean("pref", "azioniPericolose", true);
                if (azioniPericolose) {
                    texNume.setEditable(true);
                    texData.setEditable(true);
                }

                //impostazioni griglia foglio
                frmTestFatt.this.foglio.setRowHeight(20);
                zoom = new frmZoomDesc();

                frmZoomDesc frmZoom = (frmZoomDesc) zoom;
                frmZoom.selectList = frmTestFatt.this.foglioSelList;
                frmZoom.setGriglia(foglio);
                zoom.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
                zoom.setResizable(true);
                zoom.setIconifiable(true);
                zoom.setClosable(true);
                zoom.setBounds((int) frmTestFatt.this.getLocation().getX() + 430, (int) frmTestFatt.this.getLocation().getY() + 350, 300, 150);

                Menu m = (Menu) main.getPadre();
                comPagaItemStateChanged(null);
                dati.dbCheckModificatiReset();
                data_originale = texData.getText();

                if (dbStato.equalsIgnoreCase(DB_INSERIMENTO)) {
                    SimpleDateFormat f1 = new SimpleDateFormat("dd/MM/yy HH:mm");
                    texDataOra.setText(f1.format(new java.util.Date()));
                    texData.setEditable(true);
                } else {
                }

                frmTestFatt.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                tutto.add(tabDocumento, BorderLayout.CENTER);
                tutto.add(jPanel5, BorderLayout.SOUTH);
                lockableUI.setLocked(false);

                //allegati
                InvoicexEvent evt = new InvoicexEvent(frmTestFatt.this, InvoicexEvent.TYPE_AllegatiInit);
                evt.args = new Object[]{tabDocumento};
                main.events.fireInvoicexEvent(evt);

                InvoicexUtil.fireEvent(frmTestFatt.this, InvoicexEvent.TYPE_AllegatiCarica, dati.dbNomeTabella, id);

                in_apertura = false;

                if (toRun != null) {
                    System.err.println("eseguo toRun in worker done");
                    for (Runnable run : toRun) {
                        System.err.println("eseguo run in worker done:" + run);
                        run.run();
                    }
                }

                griglia.resizeColumnsPerc(true);

                //if (main.iniSerie == false || (prev.tipoFattura == dbFattura.TIPO_FATTURA_NOTA_DI_CREDITO || prev.tipoFattura == dbFattura.TIPO_FATTURA_PROFORMA)) {
                if (main.iniSerie == false || dbdoc.tipoFattura == dbFattura.TIPO_FATTURA_PROFORMA) {
                    texCliente.requestFocus();
                } else {
                    texSeri.requestFocus();
                }

                //balloon bollo
                BalloonTipStyle edgedLook = new EdgedBalloonStyle((Color) UIManager.get("ToolTip.background"), Color.RED);
                balloon_bollo = new BalloonTip(bollo_importo, "<html>In caso di fattura elettronica alla P.A. e bollo presente devi comunque lasciare importo 0 o vuoto,<br>verrà automaticamente impostato a 2 € in export</html>", edgedLook, false);
                ToolTipUtils.balloonToToolTip(balloon_bollo, 500, 5000);

                InvoicexUtil.aggiornaSplit(dati, split);

                mb.out("apertura testfatt done");
            }

        };
        worker.execute();

    }

    private void initXmlPa() {
        Vector chiavexmlpa = new Vector();
        chiavexmlpa.add("id_fattura");
        frmTestFatt.this.datiPa.dbChiave = chiavexmlpa;
        frmTestFatt.this.datiPa.dbNomeTabella = "test_fatt_xmlpa";
        frmTestFatt.this.datiPa.messaggio_nuovo_manuale = true;
        if (!Sync.isActive()) {
            try {
                DbUtils.tryExecQuery(Db.getConn(), "insert into test_fatt_xmlpa set id_fattura = " + id);
            } catch (Exception e) {
            }
            frmTestFatt.this.datiPa.dbOpen(Db.getConn(), "select * from test_fatt_xmlpa where id_fattura = " + id);
        } else {
            if (dbStato.equals(DB_INSERIMENTO)) {
                frmTestFatt.this.datiPa.dbOpen(Db.getConn(), "select * from test_fatt_xmlpa limit 0");
            } else {
                frmTestFatt.this.datiPa.dbOpen(Db.getConn(), "select * from test_fatt_xmlpa where id_fattura = " + id);
            }
            frmTestFatt.this.datiPa.dbCambiaStato(tnxDbPanel.DB_MODIFICA);
        }

        //controllo bollo
        System.out.println("bollo: " + bollo_importo.getText() + " " + bollo_si_no.isSelected());
    }

    public void eseguiDopo(Runnable run) {
        System.err.println("eseguiDopo aggiungo run:" + run);
        toRun.add(run);
//        System.err.println("eseguo toRun subito run:" + run + " worker:" + worker);
//        run.run();
    }

    public void selezionaCliente() {
        //apro combo destinazione cliente
        sql = "select ragione_sociale, id from clie_forn_dest";
        sql += " where codice_cliente = " + Db.pc(this.texClie.getText(), "NUMERIC");
        sql += " order by ragione_sociale";
        riempiDestDiversa(sql);

        recuperaDatiCliente();

        //lo fa già in recuperaDatiCliente
        //texNote.setText("");
        //aggiornaNote();
        ricalcolaTotali();

    }

    private String getAnnoDaForm() {
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

    private void inserimento() {
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

        //controllo serie default
        if (Db.getSerieDefault().length() > 0) {
            texSeri.setText(Db.getSerieDefault());
        } else {
            texSeri.setText(dbdoc.serie);
        }

        //if (main.iniSerie == false || (prev.tipoFattura == dbFattura.TIPO_FATTURA_NOTA_DI_CREDITO || prev.tipoFattura == dbFattura.TIPO_FATTURA_PROFORMA)) {
        if (main.iniSerie == false || dbdoc.tipoFattura == dbFattura.TIPO_FATTURA_PROFORMA) {
            assegnaNumero();
            dopoInserimento();
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

            this.texSeri.setToolTipText("Inserisci la serie e premi Invio per confermarla ed assegnare un numero al documento");
            this.texSeri.setEnabled(true);
            this.texSeri.setEditable(true);
            this.texSeri.setBackground(java.awt.Color.RED);
        }
    }

    private void assegnaSerie() {
        this.texSeri.setText(texSeri.getText().toUpperCase());
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

        bollo_importo.setEnabled(false);

        dopoInserimento();
        texCliente.requestFocus();
    }

    private void assegnaNumero() {

        //metto ultimo numero preventivo + 1
        //apre il resultset per ultimo +1
        java.sql.Statement stat;
        ResultSet resu;

        try {
            stat = db.getConn().createStatement();

            int myanno = java.util.Calendar.getInstance().get(Calendar.YEAR);
            String sql = "select numero from test_fatt";
            if (InvoicexUtil.getTipoNumerazione() == InvoicexUtil.TIPO_NUMERAZIONE_ANNO_INFINITA && myanno >= 2013) {
                sql += " where anno >= 2013";
            } else {
                sql += " where anno = " + myanno;
            }
            sql += " and serie = " + Db.pc(texSeri.getText(), Types.VARCHAR);
            sql += " and tipo_fattura != 7";
            sql += " order by numero desc limit 1";
            resu = stat.executeQuery(sql);

            if (resu.next() == true) {
                this.texNume.setText(String.valueOf(resu.getInt(1) + 1));
            } else {
                this.texNume.setText("1");
            }

            if (dati.getCampiAggiuntivi() == null) {
                dati.setCampiAggiuntivi(new Hashtable());
            }
            dati.getCampiAggiuntivi().put("sconto", Db.pc(doc.getSconto(), Types.DOUBLE));
            dati.getCampiAggiuntivi().put("totale_imponibile_pre_sconto", Db.pc(doc.totaleImponibilePreSconto, Types.DOUBLE));
            dati.getCampiAggiuntivi().put("totale_ivato_pre_sconto", Db.pc(doc.totaleIvatoPreSconto, Types.DOUBLE));
            dati.getCampiAggiuntivi().put("acconto", Db.pc(Db.getDouble(texAcconto.getText()), Types.DOUBLE));
            dati.getCampiAggiuntivi().put("totale_da_pagare_finale", Db.pc(Db.getDouble(texTotaDaPagareFinale.getText()), Types.DOUBLE));

            this.texTipoFattura.setText(String.valueOf(this.dbdoc.tipoFattura));
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

                    String tmpSql = "select * from test_fatt where serie = '" + tmpSerie + "' and anno = " + anno + " and numero = " + numero + " and tipo_fattura != 7";
                    System.out.println("tmpSql:" + tmpSql);
                    ResultSet tmpControl = Db.openResultSet(tmpSql);

                    if (tmpControl.next()) {
                        JOptionPane.showMessageDialog(this, "Un' altra fattura con lo stesso gruppo numero - serie - anno è già stata inserita!", "Impossibile inserire dati", JOptionPane.ERROR_MESSAGE);
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
                        frmElenFatt temp = (frmElenFatt) from;
                        temp.dbRefresh();
                    }
                }
                texClie.setText("");

                this.id = (Integer) dati.last_inserted_id;
                this.dbdoc.setId(id);
                if (dbStato.equalsIgnoreCase(tnxDbPanel.DB_INSERIMENTO)) {
                    InvoicexUtil.checkLock(dati.dbNomeTabella, id, false, null);
                }
                InvoicexUtil.checkLockAddFrame(frmTestFatt.this, dati.dbNomeTabella, id);
            }
            
            this.dbdoc.serie = this.texSeri.getText();
            this.dbdoc.stato = "P";
            this.dbdoc.numero = new Integer(this.texNume.getText()).intValue();
            this.dbdoc.anno = java.util.Calendar.getInstance().get(Calendar.YEAR);

            initXmlPa();
            
            if (!main.edit_doc_in_temp) {
                this.dati.dbCambiaStato(this.dati.DB_LETTURA);
            }

            try {
                texNote.setText(main.fileIni.getValue("pref", "noteStandard"));
            } catch (Exception err) {
                err.printStackTrace();
            }

            this.texSeri.setEditable(false);
            this.texSeri.setBackground(this.texNume.getBackground());

            //Fine
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private void dopoInserimento() {
        controllaPermessiAnagCliFor();
        
        dbAssociaGrigliaRighe();

        doc.load(Db.INSTANCE, this.dbdoc.numero, this.dbdoc.serie, this.dbdoc.anno, Db.TIPO_DOCUMENTO_FATTURA, id);
        dbdoc.setId(id);

//        SwingUtils.showInfoMessage(this, "comClie:" + comClie.getText() + " texClie:" + texClie.getText());
        if (comClie.getText().trim().length() == 0) {
            try {
                String cliente = (String) DbUtils.getObject(Db.getConn(), "select ragione_sociale from clie_forn where codice = " + Db.pc(texClie.getText(), Types.INTEGER));
                texCliente.setText(cliente);
            } catch (Exception e) {
            }
        } else {
            texCliente.setText(comClie.getText());
        }

        //apro combo banche
        trovaAbi();
        trovaCab();

        //provo a fare timer per aggiornare prezzo totale
        tim = new java.util.Timer();

//        timerRefreshFattura timTest = new timerRefreshFattura(this, doc);
//        tim.schedule(timTest, 1000, 500);
        //rinfresco il discorso extra cee
        try {
            if (this.texClie.getText().length() > 0) {
                this.dbdoc.forceCliente(Long.parseLong(this.texClie.getText()));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        //memorizzo totale iniziale, se cambia rigenreo le scadenze
        dbdoc.dbRefresh();

        try {
            InvoicexEvent event = new InvoicexEvent(this);
            event.type = InvoicexEvent.TYPE_FRMTESTFATT_DOPO_INSERIMENTO;
            main.events.fireInvoicexEvent(event);
        } catch (Exception err) {
            err.printStackTrace();
        }

        comPagaItemStateChanged(null);

        ricalcolaTotali();

        totaleDaPagareFinaleIniziale = doc.getTotale_da_pagare_finale();
        pagamentoIniziale = this.comPaga.getText();
        pagamentoInizialeGiorno = this.texGiornoPagamento.getText();
        provvigioniIniziale = Db.getDouble(this.texProvvigione.getText());
        provvigioniInizialeScadenze = dumpScadenze();
        provvigioniTotaleIniziale = dumpProvvigioni();
        codiceAgenteIniziale = it.tnx.Util.getInt(this.comAgente.getSelectedKey().toString());
        data_originale = texData.getText();

        //debug
        System.out.println("provvigioni iniziale scadenze = " + provvigioniInizialeScadenze);
        System.out.println("provvigioni iniziale = " + provvigioniIniziale);
        System.out.println("codice agente iniziale = " + codiceAgenteIniziale);

    }

    synchronized private void riempiDestDiversa(String sql) {
        boolean oldrefresh = dati.isRefreshing;
        dati.isRefreshing = true;
        comClieDest.setRinominaDuplicati(true);
        comClieDest.dbClearList();
        comClieDest.dbAddElement("", "");
        comClieDest.dbOpenList(Db.getConn(), sql, this.texClieDest.getText(), false);
        dati.isRefreshing = oldrefresh;
    }

    private void setTipoFattura(int tipoFattura) {

        //imposto il titolo
        if (tipoFattura == dbFattura.TIPO_FATTURA_IMMEDIATA) {
            this.setTitle("FATTURA IMMEDIATA");
        } else if (tipoFattura == dbFattura.TIPO_FATTURA_ACCOMPAGNATORIA) {
            this.setTitle("FATTURA ACCOMPAGNATORIA");
        } else if (tipoFattura == dbFattura.TIPO_FATTURA_NOTA_DI_CREDITO) {
            this.setTitle("NOTA DI CREDITO");
        } else if (tipoFattura == dbFattura.TIPO_FATTURA_PROFORMA) {
            this.setTitle("FATTURA PRO-FORMA");
        } else {
            this.setTitle("FATTURA");
        }

        if (tipoFattura == dbFattura.TIPO_FATTURA_ACCOMPAGNATORIA) {

            //carico porti
            comPorto.dbAddElement("");
            comPorto.dbOpenList(Db.getConn(), "select porto, id from tipi_porto group by porto");

            //mezzo di trasporto
            comMezzoTrasporto.dbAddElement("");
            comMezzoTrasporto.dbOpenList(Db.getConn(), "select nome from tipi_consegna group by nome");

            comVettori.dbAddElement("");
            comVettori.dbOpenList(db.getConn(), "select nome,nome from vettori order by nome", null, false);

            //carico causali trasporto
            comCausaleTrasporto.dbAddElement("");
            comCausaleTrasporto.dbOpenList(Db.getConn(), "select nome, id from tipi_causali_trasporto group by nome");

//            comAspettoEsterioreBeni.dbAddElement("");
//            comAspettoEsterioreBeni.dbAddElement("SCATOLA");
//            comAspettoEsterioreBeni.dbAddElement("A VISTA");
//            comAspettoEsterioreBeni.dbAddElement("SCATOLA IN PANCALE");
            comAspettoEsterioreBeni.dbAddElement("");
            comAspettoEsterioreBeni.dbOpenList(Db.getConn(), "select nome, id from tipi_aspetto_esteriore_beni group by nome", null, false);
        }

        //visualizzo i componenti per la fattura accompagnatoria
        boolean come = false;

        dati.aggiungiDbPanelCollegato(dati_altri2);

        if (tipoFattura == dbFattura.TIPO_FATTURA_ACCOMPAGNATORIA) {
            come = true;
        } else {
            come = false;
        }

//        sepFaSeparatore.setVisible(come);
        labFaTitolo.setVisible(come);
        labFa1.setVisible(come);
        labFa2.setVisible(come);
        labFa3.setVisible(come);
        labFa4.setVisible(come);
        labFa5.setVisible(come);
        labFa6.setVisible(come);
        labFa7.setVisible(come);
        comCausaleTrasporto.setVisible(come);
        comAspettoEsterioreBeni.setVisible(come);
        texNumeroColli.setVisible(come);

        //texVettore1.setVisible(come);
        comVettori.setVisible(come);
        comMezzoTrasporto.setVisible(come);
        comPorto.setVisible(come);
        texDataOra.setVisible(come);
        labPesoLordo.setVisible(come);
        labPesoNetto.setVisible(come);
        texPesoLordo.setVisible(come);
        texPesoNetto.setVisible(come);
    }

    private void saveDocumento() {
        saveDocumento(true);
    }

    private void saveDocumento(boolean avvisi) {

        //sposto i totali di modo che li salvi
        this.texTota1.setText(this.texTota.getText());
        this.texTotaImpo1.setText(this.texTotaImpo.getText());
        this.texTotaIva1.setText(this.texTotaIva.getText());

        //aggiorno totali
        try {

            if (texClie.getText() != null && texClie.getText().length() > 0) {
                doc.setCodiceCliente(Long.parseLong(texClie.getText()));
            }

            doc.setScontoTestata1(Db.getDouble(texScon1.getText()));
            doc.setScontoTestata2(Db.getDouble(texScon2.getText()));
            doc.setScontoTestata3(Db.getDouble(texScon3.getText()));
            doc.setSpeseIncasso(Db.getDouble(texSpeseIncasso.getText()));
            doc.setSpeseTrasporto(Db.getDouble(texSpeseTrasporto.getText()));
            doc.setPrezziIvati(prezzi_ivati.isSelected());
            doc.setSconto(Db.getDouble(texSconto.getText()));
            doc.setAcconto(Db.getDouble(texAcconto.getText()));
            doc.calcolaTotali();
        } catch (Exception err) {
            err.printStackTrace();
        }

        texTotaRitenuta.setText(Db.formatDecimalNoGroup(doc.getTotale_ritenuta()));
        texRivalsa.setText(Db.formatDecimalNoGroup(doc.getTotale_rivalsa()));
        texTotaDaPagare.setText(Db.formatDecimalNoGroup(doc.getTotale_da_pagare()));

        if (dati.getCampiAggiuntivi() == null) {
            dati.setCampiAggiuntivi(new Hashtable());
        }
        dati.getCampiAggiuntivi().put("sconto", Db.pc(doc.getSconto(), Types.DOUBLE));
        dati.getCampiAggiuntivi().put("totale_imponibile_pre_sconto", Db.pc(doc.totaleImponibilePreSconto, Types.DOUBLE));
        dati.getCampiAggiuntivi().put("totale_ivato_pre_sconto", Db.pc(doc.totaleIvatoPreSconto, Types.DOUBLE));
        dati.getCampiAggiuntivi().put("acconto", Db.pc(Db.getDouble(texAcconto.getText()), Types.DOUBLE));
        dati.getCampiAggiuntivi().put("totale_da_pagare_finale", Db.pc(doc.getTotale_da_pagare_finale(), Types.DOUBLE));

        //storico
        Storico.scrivi("Salva Documento", "Documento = " + this.texSeri.getText() + "/" + this.dbdoc.numero + "/" + this.dbdoc.anno + ", Pagamento = " + this.comPaga.getText() + ", Importo documento = " + this.texTota1.getText());

        //salvo altrimenti genera le scadenze sull'importo vuoto
        if (!main.edit_doc_in_temp) {
            this.dati.dbSave();
            InvoicexUtil.aggiornaAnnoDaData(Db.TIPO_DOCUMENTO_FATTURA, id);
        } else {
            Sync.saveDoc(suff, texSeri.getText(), texNume.getText(), texAnno.getText(), id, this, dati, table_righe_temp, table_righe_lotti_temp, table_righe_matricole_temp);
            if (dati.id != null) {  //in inserimento viene avvalorato con il nuovo id di testata
                id = dati.id;
            }
        }

        dbdoc.setId(id);
        
        //genero le scadenze
        Scadenze tempScad = new Scadenze(Db.TIPO_DOCUMENTO_FATTURA, id, this.comPaga.getText());
        boolean scadenzeRigenerate = false;

        //20090730
//        if (doc.getTotale() != this.totaleIniziale || !this.pagamentoInizialeGiorno.equals(this.texGiornoPagamento.getText()) || !this.pagamentoIniziale.equals(this.comPaga.getText()) || doc.getTotale_da_pagare() != this.totaleDaPagareIniziale) {
        System.err.println("totaliDiversi = " + tempScad.totaliDiversi());

        List<String> motivi = new ArrayList();
        if (tempScad.totaliDiversi()) {
            motivi.add("Totale scadenze diverso (totale doc. " + fu.formatEuroIta(tempScad.totale_doc_2d) + " / totale sca. " + fu.formatEuroIta(tempScad.totale_sca_2d) + ")");
        }
        if (!pagamentoInizialeGiorno.equals(texGiornoPagamento.getText())) {
            motivi.add("Giorno del mese di pagamento diverso (" + texGiornoPagamento.getText() + "/" + pagamentoInizialeGiorno + ")");
        }
        if (!pagamentoIniziale.equals(comPaga.getText())) {
            motivi.add("Tipo di pagamento diverso (" + comPaga.getText() + " - " + pagamentoIniziale + ")");
        }
        if (doc.getTotale_da_pagare_finale() != this.totaleDaPagareFinaleIniziale) {
            motivi.add("Totale da pagare diverso (" + fu.formatEuroIta(doc.getTotale_da_pagare_finale()) + " - " + fu.formatEuroIta(totaleDaPagareFinaleIniziale) + ")");
        }
        if (!data_originale.equalsIgnoreCase(texData.getText())) {
            motivi.add("Data del documento diversa (" + texData.getText() + " - " + data_originale + ")");
        }

        if (motivi.size() >= 1) {
            tempScad.generaScadenze(null, avvisi);
            try {
                Storico.scrivi("Genera scadenze", Db.TIPO_DOCUMENTO_FATTURA + " " + this.texSeri.getText() + " " + this.dbdoc.numero + " " + this.dbdoc.anno + " " + this.comPaga.getText());
            } catch (Exception e) {
            }
            scadenzeRigenerate = true;

            //rimetto i totali iniziali almeno in caso di inserimento e modifica delle date non vengono rigenerate.
            totaleDaPagareFinaleIniziale = doc.getTotale_da_pagare_finale();
            pagamentoIniziale = this.comPaga.getText();
            pagamentoInizialeGiorno = this.texGiornoPagamento.getText();

            if (!dbStato.equals(this.DB_INSERIMENTO)) {
                if (avvisi) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Sono state rigenerate le scadenze per " + (motivi.size() > 1 ? "i seguenti motivi" : "il seguente motivo") + ":\n-" + StringUtils.join(motivi, "\n-"), "Attenzione", javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        //rigenero le provvigioni se ancora non sono state pagate
        //Scadenze tempScad = new Scadenze(Db.TIPO_DOCUMENTO_FATTURA, this.texSeri.getText(), this.prev.numero, this.prev.anno, this.comPaga.getText());
        double nuovoImportoTeoricoProvvigioni = 0;
        gestioneFatture.logic.provvigioni.ProvvigioniFattura provvigioni = new gestioneFatture.logic.provvigioni.ProvvigioniFattura(id, it.tnx.Util.getInt(this.comAgente.getSelectedKey().toString()), it.tnx.Util.getDouble(this.texProvvigione.getText()));
        try {
            nuovoImportoTeoricoProvvigioni = provvigioni.getTotaleProvvigioni();
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtils.showExceptionMessage(this, e);
        }
        if (scadenzeRigenerate == true
                || doc.getTotale_da_pagare_finale() != this.totaleDaPagareFinaleIniziale
                || this.provvigioniIniziale != Db.getDouble(this.texProvvigione.getText())
                || !this.provvigioniInizialeScadenze.equalsIgnoreCase(dumpScadenze())
                || this.codiceAgenteIniziale != it.tnx.Util.getInt(this.comAgente.getSelectedKey().toString())
                || !data_originale.equalsIgnoreCase(texData.getText())
                || nuovoImportoTeoricoProvvigioni != provvigioniTotaleIniziale) {
            System.out.println("rigenero provvigioni:" + doc.getTotale_da_pagare_finale() + " != " + totaleDaPagareFinaleIniziale + " || scadenzeRigenerate:" + scadenzeRigenerate + " || " + provvigioniIniziale + " != " + Db.getDouble(this.texProvvigione.getText()) + " || " + provvigioniInizialeScadenze + " != " + dumpScadenze() + " || " + codiceAgenteIniziale + " != " + it.tnx.Util.getInt(this.comAgente.getSelectedKey().toString()) + " || " + data_originale + " != " + texData.getText() + " || " + nuovoImportoTeoricoProvvigioni + " != " + provvigioniTotaleIniziale);
            boolean ret = provvigioni.generaProvvigioni();
            try {
                Storico.scrivi("Genera provvigioni", Db.TIPO_DOCUMENTO_FATTURA + " " + this.texSeri.getText() + " " + this.dbdoc.numero + " " + this.dbdoc.anno + " " + it.tnx.Util.getInt(this.comAgente.getSelectedKey().toString()) + " " + it.tnx.Util.getDouble(this.texProvvigione.getText()));
            } catch (Exception e) {
            }
            System.out.println("esito genera provvigioni:" + ret + " : " + provvigioni.ret);
            if (!dbStato.equals(this.DB_INSERIMENTO)) {
                if (ret) {
                    if (avvisi) {
                        javax.swing.JOptionPane.showMessageDialog(this, "Sono state rigenerate le provvigioni", "Attenzione", javax.swing.JOptionPane.WARNING_MESSAGE);
                    }
                }
            }

            this.provvigioniInizialeScadenze = dumpScadenze();
            this.provvigioniIniziale = Db.getDouble(this.texProvvigione.getText());
            this.codiceAgenteIniziale = it.tnx.Util.getInt(this.comAgente.getSelectedKey().toString());
            this.provvigioniTotaleIniziale = nuovoImportoTeoricoProvvigioni;
        }

        //vado a cercare nei ddt se ce ne è almeno uno attaccato a questa fattura
        int conta = 0;
        if (id != null) {
            try {
                String sql = "select count(*) from righ_ddt "
                        + " where in_fatt = " + id;
                ResultSet r = Db.openResultSet(sql);
                if (r.next()) {
                    conta = r.getInt(1);
                }
            } catch (SQLException sqlerr) {
                sqlerr.printStackTrace();
            }
        }

        if (conta == 0) {
            dbDocumento tempPrev = new dbDocumento();
            tempPrev.serie = dbdoc.serie;
            tempPrev.numero = dbdoc.numero;
            tempPrev.stato = dbdoc.stato;
            tempPrev.anno = dbdoc.anno;
            tempPrev.setId(dbdoc.getId());
            tempPrev.tipoDocumento = Db.TIPO_DOCUMENTO_FATTURA;

            int tipo_fattura = Integer.parseInt(texTipoFattura.getText());
            boolean genera = InvoicexUtil.generareMovimenti(tipo_fattura, this);

            if (genera) {
                if (tempPrev.generaMovimentiMagazzino() == false) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Errore nella generazione dei movimenti di magazzino", "Errore", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (gestioneFatture.main.pluginClientManager) {
            JOptionPane.showMessageDialog(this, "La fattura proviene da uno o più ddt e non verranno creati o rigenerati i movimenti");
        }

        //aggiorno eventuali documenti collegati (ordini, ddt)
        InvoicexUtil.aggiornaRiferimentoDocumenti(Db.TIPO_DOCUMENTO_FATTURA, dbdoc.getId());

        //xmlpa
        try {
            String test = "";
            for (int i = 0; i < datiPa.getComponentCount(); i++) {
                Component c = datiPa.getComponent(i);
                if (c instanceof tnxTextField) {
                    test += ((tnxTextField) c).getText();
                } else if (c instanceof tnxComboField) {
                    if (c == dg_dr_totale_da_esportare) {
                        if (dg_dr_totale_da_esportare.getSelectedIndex() >= 0) {
                            test += cu.s(((tnxComboField) c).getSelectedKey());
                        }
                    } else {
                        test += cu.s(((tnxComboField) c).getSelectedKey());
                    }
                } else if (c instanceof tnxCheckBox) {
                    test += cu.s((((tnxCheckBox) c).isSelected()) ? "S" : "");
                }
            }
            if (StringUtils.isBlank(test)) {
                DbUtils.tryExecQuery(Db.getConn(), "delete from test_fatt_xmlpa where id_fattura = " + id);
            } else {
                if (Sync.isActive() && dbStato.equals(this.DB_INSERIMENTO)) {    
                    try {
                        DbUtils.tryExecQuery(Db.getConn(), "insert into test_fatt_xmlpa set id_fattura = " + id);
                    } catch (Exception e) {
                    }
                    if (datiPa.dbChiaveValori == null) {
                        datiPa.dbChiaveValori = new Hashtable();
                    }                    
                }
                datiPa.dbChiaveValori.put("id_fattura", id);
                datiPa.dbSave();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
     *
     * initialize the form.
     *
     *
     * WARNING: Do NOT modify this code. The content of this method is
     *
     *
     * always regenerated by the Form Editor.
     *
     *
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popGrig = new javax.swing.JPopupMenu();
        popGrigModi = new javax.swing.JMenuItem();
        popGrigElim = new javax.swing.JMenuItem();
        popGrigAdd = new javax.swing.JMenuItem();
        popDuplicaRiga = new javax.swing.JMenuItem();
        menColAgg = new javax.swing.JMenu();
        menColAggNote = new javax.swing.JCheckBoxMenuItem();
        popFoglio = new javax.swing.JPopupMenu();
        popFoglioElimina = new javax.swing.JMenuItem();
        popFoglioModifica = new javax.swing.JMenuItem();
        foglio3 = new javax.swing.JTable();
        menClientePopup = new javax.swing.JPopupMenu();
        menClienteNuovo = new javax.swing.JMenuItem();
        menClienteModifica = new javax.swing.JMenuItem();
        tutto = new javax.swing.JPanel();
        tabDocumento = new javax.swing.JTabbedPane();
        panDati = new javax.swing.JPanel();
        split = new javax.swing.JSplitPane();
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
        jLabel16 = new javax.swing.JLabel();
        labScon1 = new javax.swing.JLabel();
        labScon2 = new javax.swing.JLabel();
        jLabel113 = new javax.swing.JLabel();
        texData = new tnxbeans.tnxTextField();
        jLabel11 = new javax.swing.JLabel();
        texScon3 = new tnxbeans.tnxTextField();
        labScon21 = new javax.swing.JLabel();
        jLabel151 = new javax.swing.JLabel();
        texSeri = new tnxbeans.tnxTextField();
        texAnno = new tnxbeans.tnxTextField();
        texAnno.setVisible(false);
        texClieDest = new tnxbeans.tnxTextField();
        texClieDest.setVisible(false);
        jLabel17 = new javax.swing.JLabel();
        texPaga2 = new tnxbeans.tnxTextField();
        texSpeseTrasporto = new tnxbeans.tnxTextField();
        jLabel114 = new javax.swing.JLabel();
        butPrezziPrec = new javax.swing.JButton();
        jLabel18 = new javax.swing.JLabel();
        comPaga = new tnxbeans.tnxComboField();
        butScad = new javax.swing.JButton();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        texBancAbi = new tnxbeans.tnxTextField();
        texBancIban = new tnxbeans.tnxTextField();
        butCoor = new javax.swing.JButton();
        labBancAbi = new javax.swing.JLabel();
        labBancCab = new javax.swing.JLabel();
        texTipoFattura = new tnxbeans.tnxTextField();
        texTipoFattura.setVisible(false);
        sepFaSeparatore = new javax.swing.JSeparator();
        butAddClie = new javax.swing.JButton();
        cheOpzioneRibaDestDiversa = new tnxbeans.tnxCheckBox();
        jLabel24 = new javax.swing.JLabel();
        texNotePagamento = new tnxbeans.tnxTextField();
        labAgente = new javax.swing.JLabel();
        comAgente = new tnxbeans.tnxComboField();
        labProvvigione = new javax.swing.JLabel();
        texProvvigione = new tnxbeans.tnxTextField();
        labPercentoProvvigione = new javax.swing.JLabel();
        texGiornoPagamento = new tnxbeans.tnxTextField();
        labGiornoPagamento = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        texBancCab = new tnxbeans.tnxTextField();
        texRitenuta = new tnxbeans.tnxTextField();
        texRitenuta.setVisible(false);
        texTotaRitenuta = new tnxbeans.tnxTextField();
        texTotaRitenuta.setVisible(false);
        texTotaDaPagare = new tnxbeans.tnxTextField();
        texTotaDaPagare.setVisible(false);
        texRivalsa = new tnxbeans.tnxTextField();
        texRivalsa.setVisible(false);
        texForni1 = new tnxbeans.tnxTextField();
        labMarcaBollo = new javax.swing.JLabel();
        bollo_importo = new tnxbeans.tnxTextField();
        texCliente = new javax.swing.JTextField();
        apriclienti = new MyBasicArrowButton(BasicArrowButton.SOUTH);
        prezzi_ivati = new tnxbeans.tnxCheckBox();
        labModConsegna = new javax.swing.JLabel();
        comConsegna = new tnxbeans.tnxComboField();
        labModScarico = new javax.swing.JLabel();
        comScarico = new tnxbeans.tnxComboField();
        jLabel54 = new javax.swing.JLabel();
        labMarcaBollo1 = new javax.swing.JLabel();
        bollo_si_no = new tnxbeans.tnxCheckBox();
        dati_altri2 = new tnxbeans.tnxDbPanel();
        jLabel15 = new javax.swing.JLabel();
        sepDestMerce = new javax.swing.JSeparator();
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
        labFaTitolo = new javax.swing.JLabel();
        sepFattAcc = new javax.swing.JSeparator();
        labFa1 = new javax.swing.JLabel();
        comCausaleTrasporto = new tnxbeans.tnxComboField();
        labFa2 = new javax.swing.JLabel();
        comAspettoEsterioreBeni = new tnxbeans.tnxComboField();
        labFa4 = new javax.swing.JLabel();
        comVettori = new tnxbeans.tnxComboField();
        labFa5 = new javax.swing.JLabel();
        comMezzoTrasporto = new tnxbeans.tnxComboField();
        labFa6 = new javax.swing.JLabel();
        comPorto = new tnxbeans.tnxComboField();
        labFa7 = new javax.swing.JLabel();
        texNumeroColli = new tnxbeans.tnxTextField();
        labFa3 = new javax.swing.JLabel();
        texDataOra = new tnxbeans.tnxTextField();
        labPesoLordo = new javax.swing.JLabel();
        texPesoLordo = new tnxbeans.tnxTextField();
        labPesoNetto = new javax.swing.JLabel();
        texPesoNetto = new tnxbeans.tnxTextField();
        jLabel61 = new javax.swing.JLabel();
        datiRighe = new tnxbeans.tnxDbPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        griglia = new tnxbeans.tnxDbGrid();
        jPanel1 = new javax.swing.JPanel();
        prezzi_ivati_virtual = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        butNuovArti = new javax.swing.JButton();
        butInserisciPeso = new javax.swing.JButton();
        butImportRighe = new javax.swing.JButton();
        butImportXlsCirri = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        panFoglioRighe = new javax.swing.JPanel();
        panGriglia = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        foglio = new JTableSs(){
            @Override
            public void setValueAt(Object value,int row,int column){
                super.setValueAt(value, row, column);
                ricalcolaTotali();
            }
        };
        panTotale = new javax.swing.JPanel();
        butSalvaFoglioRighe = new javax.swing.JButton();
        labStatus = new javax.swing.JLabel();
        datiAltro = new tnxbeans.tnxDbPanel();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        texForni = new tnxbeans.tnxTextField();
        comForni = new tnxbeans.tnxComboField();
        jLabel4 = new javax.swing.JLabel();
        pan_segnaposto_deposito = new javax.swing.JPanel();
        labNoteConsegna = new javax.swing.JLabel();
        texNoteConsegna = new tnxbeans.tnxMemoField();
        comValuta = new tnxbeans.tnxComboField();
        labvaluta = new javax.swing.JLabel();
        labCampoLibero1 = new javax.swing.JLabel();
        comCampoLibero1 = new tnxbeans.tnxComboField();
        scrollDatiPa = new javax.swing.JScrollPane();
        datiPa = new tnxbeans.tnxDbPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        linkcodiceufficio = new org.jdesktop.swingx.JXHyperlink();
        dg_doa_riferimentonumerolinea = new tnxbeans.tnxTextField();
        dg_doa_iddocumento = new tnxbeans.tnxTextField();
        dg_doa_data = new tnxbeans.tnxTextField();
        dg_doa_numitem = new tnxbeans.tnxTextField();
        dg_doa_codicecommessaconvenzione = new tnxbeans.tnxTextField();
        dg_doa_codicecup = new tnxbeans.tnxTextField();
        dg_doa_codicecig = new tnxbeans.tnxTextField();
        dg_dc_riferimentonumerolinea = new tnxbeans.tnxTextField();
        dg_dc_iddocumento = new tnxbeans.tnxTextField();
        dg_dc_data = new tnxbeans.tnxTextField();
        dg_dc_numitem = new tnxbeans.tnxTextField();
        dg_dc_codicecommessaconvenzione = new tnxbeans.tnxTextField();
        dg_dc_codicecup = new tnxbeans.tnxTextField();
        dg_dc_codicecig = new tnxbeans.tnxTextField();
        jLabel37 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        dp_banca = new tnxbeans.tnxTextField();
        jLabel53 = new javax.swing.JLabel();
        dp_iban = new tnxbeans.tnxTextField();
        dp_iban_lab = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel55 = new javax.swing.JLabel();
        lregfis = new javax.swing.JLabel();
        lregfis1 = new javax.swing.JLabel();
        linkcodiceufficio1 = new org.jdesktop.swingx.JXHyperlink();
        jLabel56 = new javax.swing.JLabel();
        lregfis2 = new javax.swing.JLabel();
        dg_dr_tipo_ritenuta = new tnxbeans.tnxComboField();
        dg_dr_causale_pagamento = new tnxbeans.tnxComboField();
        dg_dcp_tipo_cassa = new tnxbeans.tnxComboField();
        dg_dr_totale_da_esportare = new tnxbeans.tnxComboField();
        lregfis3 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        dg_causale = new tnxbeans.tnxTextField();
        jLabel59 = new javax.swing.JLabel();
        split_payment = new tnxbeans.tnxCheckBox();
        jLabel60 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        panSalva = new javax.swing.JPanel();
        butStampa = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        butUndo = new javax.swing.JButton();
        butSave = new javax.swing.JButton();
        butPdf = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        texTotaImpo = new tnxbeans.tnxTextField();
        texTotaIva = new tnxbeans.tnxTextField();
        texTota = new tnxbeans.tnxTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        texSconto = new tnxbeans.tnxTextField();
        jLabel57 = new javax.swing.JLabel();
        texAcconto = new tnxbeans.tnxTextField();
        jLabel6 = new javax.swing.JLabel();
        texTotaDaPagareFinale = new tnxbeans.tnxTextField();
        panRitenute = new javax.swing.JPanel();

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

        popGrigAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/list-add.png"))); // NOI18N
        popGrigAdd.setLabel("Aggiungi Riga");
        popGrigAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popGrigAddActionPerformed(evt);
            }
        });
        popGrig.add(popGrigAdd);

        popDuplicaRiga.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/edit-copy.png"))); // NOI18N
        popDuplicaRiga.setText("Duplica");
        popDuplicaRiga.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popDuplicaRigaActionPerformed(evt);
            }
        });
        popGrig.add(popDuplicaRiga);

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

        popFoglioElimina.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/places/user-trash.png"))); // NOI18N
        popFoglioElimina.setText("Elimina");
        popFoglioElimina.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popFoglioEliminaActionPerformed(evt);
            }
        });
        popFoglio.add(popFoglioElimina);

        popFoglioModifica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/document-open.png"))); // NOI18N
        popFoglioModifica.setText("Modifica dettagli");
        popFoglioModifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popFoglioModificaActionPerformed(evt);
            }
        });
        popFoglio.add(popFoglioModifica);

        foglio3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        foglio3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                foglio3MouseClicked(evt);
            }
        });

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
        setTitle("Fattura");
        addInternalFrameListener(new javax.swing.event.InternalFrameListener() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameClosed(evt);
            }
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameClosing(evt);
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
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });
        addVetoableChangeListener(new java.beans.VetoableChangeListener() {
            public void vetoableChange(java.beans.PropertyChangeEvent evt)throws java.beans.PropertyVetoException {
                formVetoableChange(evt);
            }
        });

        tutto.setLayout(new java.awt.BorderLayout());

        tabDocumento.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabDocumentoStateChanged(evt);
            }
        });

        panDati.setLayout(new java.awt.BorderLayout());

        split.setBorder(null);
        split.setDividerLocation(500);
        split.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        scrollDati.setBorder(null);
        scrollDati.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollDati.setHorizontalScrollBar(null);

        dati.setName("datiFattura"); // NOI18N

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
        texNume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texNumeActionPerformed(evt);
            }
        });

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
        texClie.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                texCliePropertyChange(evt);
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
        texScon2.addKeyListener(new java.awt.event.KeyAdapter() {
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
        });

        comClie.setDbNomeCampo("");
        comClie.setDbRiempire(false);
        comClie.setDbSalvare(false);
        comClie.setDbTextAbbinato(texClie);
        comClie.setDbTipoCampo("");
        comClie.setDbTrovaMentreScrive(true);
        comClie.setName("comClie"); // NOI18N
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

        texNote.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        texNote.setDbNomeCampo("note");
        texNote.setFont(texNote.getFont());
        texNote.setText("note");

        jLabel13.setFont(jLabel13.getFont());
        jLabel13.setText("Numero");

        jLabel14.setFont(jLabel14.getFont());
        jLabel14.setText("Serie");

        jLabel16.setFont(jLabel16.getFont());
        jLabel16.setText("Data");

        labScon1.setFont(labScon1.getFont());
        labScon1.setText("Sc. 1");
        labScon1.setToolTipText("primo sconto o ricarico");

        labScon2.setFont(labScon2.getFont());
        labScon2.setText("Sc. 3");
        labScon2.setToolTipText("terzo sconto o ricarico");

        jLabel113.setFont(jLabel113.getFont());
        jLabel113.setText("Sp. incasso");

        texData.setEditable(false);
        texData.setColumns(9);
        texData.setText("data");
        texData.setDbDescCampo("");
        texData.setDbNomeCampo("data");
        texData.setDbTipoCampo("data");
        texData.setmaxChars(10);
        texData.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                texDataFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                texDataFocusLost(evt);
            }
        });
        texData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texDataActionPerformed(evt);
            }
        });

        jLabel11.setFont(jLabel11.getFont());
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Note");

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

        labScon21.setFont(labScon21.getFont());
        labScon21.setText("Sc. 2");
        labScon21.setToolTipText("secondo sconto o ricarico");

        jLabel151.setFont(jLabel151.getFont());
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
        });

        texAnno.setBackground(new java.awt.Color(255, 200, 200));
        texAnno.setText("anno");
        texAnno.setDbDescCampo("");
        texAnno.setDbNomeCampo("anno");
        texAnno.setDbTipoCampo("");

        texClieDest.setBackground(new java.awt.Color(255, 200, 200));
        texClieDest.setText("id_cliente_destinazione");
        texClieDest.setDbComboAbbinata(comClieDest);
        texClieDest.setDbDescCampo("");
        texClieDest.setDbNomeCampo("id_cliente_destinazione");
        texClieDest.setDbTipoCampo("numerico");

        jLabel17.setFont(jLabel17.getFont());
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel17.setText("Riferimento");
        jLabel17.setToolTipText("Riferimento vostro ordine o altro documento");

        texPaga2.setText("riferimento");
        texPaga2.setDbDescCampo("");
        texPaga2.setDbNomeCampo("riferimento");
        texPaga2.setDbTipoCampo("");
        texPaga2.setmaxChars(255);

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

        jLabel114.setFont(jLabel114.getFont());
        jLabel114.setText("Sp. trasporto");

        butPrezziPrec.setFont(butPrezziPrec.getFont().deriveFont(butPrezziPrec.getFont().getSize()-1f));
        butPrezziPrec.setText("Prezzi precedenti");
        butPrezziPrec.setMargin(new java.awt.Insets(1, 4, 1, 4));
        butPrezziPrec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butPrezziPrecActionPerformed(evt);
            }
        });

        jLabel18.setFont(jLabel18.getFont());
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel18.setText("Banca ABI");

        comPaga.setDbDescCampo("");
        comPaga.setDbNomeCampo("pagamento");
        comPaga.setDbTipoCampo("VARCHAR");
        comPaga.setDbTrovaMentreScrive(true);
        comPaga.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comPagaItemStateChanged(evt);
            }
        });
        comPaga.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                comPagaFocusLost(evt);
            }
        });

        butScad.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        butScad.setText("...");
        butScad.setToolTipText("Gestione Scadenze");
        butScad.setMargin(new java.awt.Insets(1, 1, 1, 1));
        butScad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butScadActionPerformed(evt);
            }
        });

        jLabel19.setFont(jLabel19.getFont());
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel19.setText("Pagamento");

        jLabel20.setFont(jLabel20.getFont());
        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel20.setText("IBAN");

        texBancAbi.setColumns(6);
        texBancAbi.setToolTipText("");
        texBancAbi.setDbDescCampo("");
        texBancAbi.setDbNomeCampo("banca_abi");
        texBancAbi.setDbTipoCampo("");
        texBancAbi.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texBancAbiFocusLost(evt);
            }
        });
        texBancAbi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texBancAbiActionPerformed(evt);
            }
        });

        texBancIban.setColumns(20);
        texBancIban.setToolTipText("");
        texBancIban.setDbDescCampo("");
        texBancIban.setDbNomeCampo("banca_iban");
        texBancIban.setDbTipoCampo("");
        texBancIban.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texBancIbanFocusLost(evt);
            }
        });
        texBancIban.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texBancIbanActionPerformed(evt);
            }
        });

        butCoor.setFont(butCoor.getFont().deriveFont(butCoor.getFont().getSize()-1f));
        butCoor.setText("cerca");
        butCoor.setMargin(new java.awt.Insets(1, 2, 1, 2));
        butCoor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butCoorActionPerformed(evt);
            }
        });

        labBancAbi.setFont(labBancAbi.getFont().deriveFont(labBancAbi.getFont().getSize()-1f));
        labBancAbi.setText("...");

        labBancCab.setFont(labBancCab.getFont().deriveFont(labBancCab.getFont().getSize()-1f));
        labBancCab.setText("...");

        texTipoFattura.setBackground(new java.awt.Color(255, 200, 200));
        texTipoFattura.setText("tipoFattura");
        texTipoFattura.setDbDescCampo("");
        texTipoFattura.setDbNomeCampo("tipo_fattura");
        texTipoFattura.setDbTipoCampo("numerico");

        sepFaSeparatore.setOrientation(javax.swing.SwingConstants.VERTICAL);

        butAddClie.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        butAddClie.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/Actions-contact-new-icon-16.png"))); // NOI18N
        butAddClie.setToolTipText("Crea una nuova anagrafica");
        butAddClie.setMargin(new java.awt.Insets(1, 1, 1, 1));
        butAddClie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butAddClieActionPerformed(evt);
            }
        });

        cheOpzioneRibaDestDiversa.setText("stampa Dest. Diversa su distinta ");
        cheOpzioneRibaDestDiversa.setToolTipText("Selezionando questa opzione stampa la Destinazione diversa nella Distinta delle RIBA");
        cheOpzioneRibaDestDiversa.setDbDescCampo("Opzione Dest. Diversa Riba");
        cheOpzioneRibaDestDiversa.setDbNomeCampo("opzione_riba_dest_diversa");
        cheOpzioneRibaDestDiversa.setDbTipoCampo("");
        cheOpzioneRibaDestDiversa.setFont(cheOpzioneRibaDestDiversa.getFont().deriveFont(cheOpzioneRibaDestDiversa.getFont().getSize()-1f));
        cheOpzioneRibaDestDiversa.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        cheOpzioneRibaDestDiversa.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        cheOpzioneRibaDestDiversa.setIconTextGap(1);

        jLabel24.setFont(jLabel24.getFont());
        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel24.setText("Note pagamento");

        texNotePagamento.setColumns(40);
        texNotePagamento.setText("note pagamento");
        texNotePagamento.setDbDescCampo("");
        texNotePagamento.setDbNomeCampo("note_pagamento");
        texNotePagamento.setDbTipoCampo("");
        texNotePagamento.setmaxChars(255);
        texNotePagamento.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texNotePagamentoFocusLost(evt);
            }
        });

        labAgente.setFont(labAgente.getFont());
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

        labProvvigione.setFont(labProvvigione.getFont());
        labProvvigione.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
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

        labPercentoProvvigione.setFont(labPercentoProvvigione.getFont());
        labPercentoProvvigione.setText("%");

        texGiornoPagamento.setToolTipText("Giorno del mese per le scadenze");
        texGiornoPagamento.setDbDescCampo("");
        texGiornoPagamento.setDbNomeCampo("giorno_pagamento");
        texGiornoPagamento.setDbTipoCampo("numerico");
        texGiornoPagamento.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texGiornoPagamentoFocusLost(evt);
            }
        });

        labGiornoPagamento.setText("giorno");

        jLabel23.setFont(jLabel23.getFont());
        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel23.setText("Banca CAB");

        texBancCab.setColumns(6);
        texBancCab.setToolTipText("");
        texBancCab.setDbDescCampo("");
        texBancCab.setDbNomeCampo("banca_cab");
        texBancCab.setDbTipoCampo("");
        texBancCab.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texBancCabFocusLost(evt);
            }
        });
        texBancCab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texBancCabActionPerformed(evt);
            }
        });

        texRitenuta.setBackground(new java.awt.Color(255, 200, 200));
        texRitenuta.setDbDescCampo("");
        texRitenuta.setDbNomeCampo("ritenuta");
        texRitenuta.setDbTipoCampo("numerico");

        texTotaRitenuta.setBackground(new java.awt.Color(255, 200, 200));
        texTotaRitenuta.setText("0");
        texTotaRitenuta.setDbDescCampo("");
        texTotaRitenuta.setDbNomeCampo("totale_ritenuta");
        texTotaRitenuta.setDbTipoCampo("valuta");

        texTotaDaPagare.setBackground(new java.awt.Color(255, 200, 200));
        texTotaDaPagare.setText("0");
        texTotaDaPagare.setDbDescCampo("");
        texTotaDaPagare.setDbNomeCampo("totale_da_pagare");
        texTotaDaPagare.setDbTipoCampo("valuta");

        texRivalsa.setBackground(new java.awt.Color(255, 200, 200));
        texRivalsa.setDbDescCampo("");
        texRivalsa.setDbNomeCampo("totaleRivalsa");
        texRivalsa.setDbTipoCampo("numerico");

        texForni1.setDbNomeCampo("fornitore");
        texForni1.setDbNullSeVuoto(true);
        texForni1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                texForni1KeyPressed(evt);
            }
        });

        labMarcaBollo.setFont(labMarcaBollo.getFont());
        labMarcaBollo.setText("€");

        bollo_importo.setColumns(4);
        bollo_importo.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        bollo_importo.setText("marche_da_bollo");
        bollo_importo.setDbNomeCampo("marca_da_bollo");
        bollo_importo.setDbNullSeVuoto(true);
        bollo_importo.setDbTipoCampo("valuta");
        bollo_importo.setEnabled(false);
        bollo_importo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                bollo_importoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                bollo_importoFocusLost(evt);
            }
        });
        bollo_importo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                bollo_importoKeyReleased(evt);
            }
        });

        texCliente.setColumns(18);
        texCliente.setName("cliente"); // NOI18N
        texCliente.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                texClienteMouseClicked(evt);
            }
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

        prezzi_ivati.setBackground(new java.awt.Color(255, 204, 204));
        prezzi_ivati.setText("prezzi ivati");
        prezzi_ivati.setToolTipText("Selezionando questa opzione stampa la Destinazione diversa nella Distinta delle RIBA");
        prezzi_ivati.setDbDescCampo("Prezzi Ivati");
        prezzi_ivati.setDbNomeCampo("prezzi_ivati");
        prezzi_ivati.setDbTipoCampo("");
        prezzi_ivati.setFont(new java.awt.Font("Dialog", 0, 9)); // NOI18N
        prezzi_ivati.setIconTextGap(1);

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

        jLabel54.setFont(jLabel54.getFont().deriveFont((jLabel54.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel54.setText("Coordinate bancarie del Cliente (RIBA) ");

        labMarcaBollo1.setFont(labMarcaBollo1.getFont());
        labMarcaBollo1.setText("Bollo in fattura");

        bollo_si_no.setDbDescCampo("Bollo presente");
        bollo_si_no.setDbNomeCampo("bollo_presente");
        bollo_si_no.setDbRiempire(false);
        bollo_si_no.setDbTipoCampo("");
        bollo_si_no.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        bollo_si_no.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        bollo_si_no.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bollo_si_noActionPerformed(evt);
            }
        });

        jLabel15.setFont(jLabel15.getFont());
        jLabel15.setText("Destinazione merce");

        comClieDest.setToolTipText("Premere invio per selezionarlo");
        comClieDest.setDbNomeCampo("");
        comClieDest.setDbRiempire(false);
        comClieDest.setDbSalvare(false);
        comClieDest.setDbTextAbbinato(texClieDest);
        comClieDest.setDbTipoCampo("");
        comClieDest.setDbTrovaMentreScrive(true);
        comClieDest.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                comClieDestFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                comClieDestFocusLost(evt);
            }
        });
        comClieDest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comClieDestActionPerformed(evt);
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

        texDestRagioneSociale.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        texDestRagioneSociale.setColumns(20);
        texDestRagioneSociale.setToolTipText("");
        texDestRagioneSociale.setDbDescCampo("");
        texDestRagioneSociale.setDbNomeCampo("dest_ragione_sociale");
        texDestRagioneSociale.setDbTipoCampo("");
        texDestRagioneSociale.setFont(texDestRagioneSociale.getFont().deriveFont(texDestRagioneSociale.getFont().getSize()-1f));

        labScon11.setFont(labScon11.getFont().deriveFont(labScon11.getFont().getSize()-1f));
        labScon11.setText("indirizzo");
        labScon11.setToolTipText("");

        texDestIndirizzo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        texDestIndirizzo.setColumns(20);
        texDestIndirizzo.setToolTipText("");
        texDestIndirizzo.setDbDescCampo("");
        texDestIndirizzo.setDbNomeCampo("dest_indirizzo");
        texDestIndirizzo.setDbTipoCampo("");
        texDestIndirizzo.setFont(texDestIndirizzo.getFont().deriveFont(texDestIndirizzo.getFont().getSize()-1f));

        labScon12.setFont(labScon12.getFont().deriveFont(labScon12.getFont().getSize()-1f));
        labScon12.setText("cap");
        labScon12.setToolTipText("");

        texDestCap.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        texDestCap.setColumns(5);
        texDestCap.setToolTipText("");
        texDestCap.setDbDescCampo("");
        texDestCap.setDbNomeCampo("dest_cap");
        texDestCap.setDbTipoCampo("");
        texDestCap.setFont(texDestCap.getFont().deriveFont(texDestCap.getFont().getSize()-1f));

        labScon13.setFont(labScon13.getFont().deriveFont(labScon13.getFont().getSize()-1f));
        labScon13.setText("loc.");
        labScon13.setToolTipText("");

        texDestLocalita.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        texDestLocalita.setColumns(10);
        texDestLocalita.setToolTipText("");
        texDestLocalita.setDbDescCampo("");
        texDestLocalita.setDbNomeCampo("dest_localita");
        texDestLocalita.setDbTipoCampo("");
        texDestLocalita.setFont(texDestLocalita.getFont().deriveFont(texDestLocalita.getFont().getSize()-1f));

        labScon14.setFont(labScon14.getFont().deriveFont(labScon14.getFont().getSize()-1f));
        labScon14.setText("prov.");
        labScon14.setToolTipText("");

        texDestProvincia.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        texDestProvincia.setColumns(3);
        texDestProvincia.setToolTipText("");
        texDestProvincia.setDbDescCampo("");
        texDestProvincia.setDbNomeCampo("dest_provincia");
        texDestProvincia.setDbTipoCampo("");
        texDestProvincia.setFont(texDestProvincia.getFont().deriveFont(texDestProvincia.getFont().getSize()-1f));

        labScon16.setFont(labScon16.getFont().deriveFont(labScon16.getFont().getSize()-1f));
        labScon16.setText("telefono");
        labScon16.setToolTipText("");

        texDestTelefono.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        texDestTelefono.setColumns(6);
        texDestTelefono.setToolTipText("");
        texDestTelefono.setDbDescCampo("");
        texDestTelefono.setDbNomeCampo("dest_telefono");
        texDestTelefono.setDbTipoCampo("");
        texDestTelefono.setFont(texDestTelefono.getFont().deriveFont(texDestTelefono.getFont().getSize()-1f));

        labScon15.setFont(labScon15.getFont().deriveFont(labScon15.getFont().getSize()-1f));
        labScon15.setText("cellulare");
        labScon15.setToolTipText("");

        texDestCellulare.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        texDestCellulare.setColumns(10);
        texDestCellulare.setToolTipText("");
        texDestCellulare.setDbDescCampo("");
        texDestCellulare.setDbNomeCampo("dest_cellulare");
        texDestCellulare.setDbTipoCampo("");
        texDestCellulare.setFont(texDestCellulare.getFont().deriveFont(texDestCellulare.getFont().getSize()-1f));

        labScon17.setFont(labScon17.getFont().deriveFont(labScon17.getFont().getSize()-1f));
        labScon17.setText("paese");
        labScon17.setToolTipText("");

        comPaese.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        comPaese.setDbNomeCampo("dest_paese");
        comPaese.setDbTipoCampo("");
        comPaese.setDbTrovaMentreScrive(true);
        comPaese.setFont(comPaese.getFont().deriveFont(comPaese.getFont().getSize()-1f));

        labFaTitolo.setFont(labFaTitolo.getFont());
        labFaTitolo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labFaTitolo.setText("Dati fattura accompagnatoria");

        labFa1.setFont(labFa1.getFont().deriveFont(labFa1.getFont().getSize()-1f));
        labFa1.setText("Causale del trasporto");

        comCausaleTrasporto.setDbNomeCampo("causale_trasporto");
        comCausaleTrasporto.setDbRiempireForceText(true);
        comCausaleTrasporto.setDbSalvaKey(false);
        comCausaleTrasporto.setFont(comCausaleTrasporto.getFont().deriveFont(comCausaleTrasporto.getFont().getSize()-1f));

        labFa2.setFont(labFa2.getFont().deriveFont(labFa2.getFont().getSize()-1f));
        labFa2.setText("Aspetto esteriore beni");

        comAspettoEsterioreBeni.setDbNomeCampo("aspetto_esteriore_beni");
        comAspettoEsterioreBeni.setDbRiempireForceText(true);
        comAspettoEsterioreBeni.setDbSalvaKey(false);
        comAspettoEsterioreBeni.setFont(comAspettoEsterioreBeni.getFont().deriveFont(comAspettoEsterioreBeni.getFont().getSize()-1f));

        labFa4.setFont(labFa4.getFont().deriveFont(labFa4.getFont().getSize()-1f));
        labFa4.setText("1° Vettore");

        comVettori.setDbNomeCampo("vettore1");
        comVettori.setDbRiempireForceText(true);
        comVettori.setDbSalvaKey(false);
        comVettori.setFont(comVettori.getFont().deriveFont(comVettori.getFont().getSize()-1f));

        labFa5.setFont(labFa5.getFont().deriveFont(labFa5.getFont().getSize()-1f));
        labFa5.setText("Cons. o inizio trasp. a mezzo");

        comMezzoTrasporto.setDbNomeCampo("mezzo_consegna");
        comMezzoTrasporto.setDbRiempireForceText(true);
        comMezzoTrasporto.setDbSalvaKey(false);
        comMezzoTrasporto.setFont(comMezzoTrasporto.getFont().deriveFont(comMezzoTrasporto.getFont().getSize()-1f));

        labFa6.setFont(labFa6.getFont().deriveFont(labFa6.getFont().getSize()-1f));
        labFa6.setText("Porto");

        comPorto.setDbNomeCampo("porto");
        comPorto.setDbRiempireForceText(true);
        comPorto.setDbSalvaKey(false);
        comPorto.setFont(comPorto.getFont().deriveFont(comPorto.getFont().getSize()-1f));

        labFa7.setFont(labFa7.getFont().deriveFont(labFa7.getFont().getSize()-1f));
        labFa7.setText("Num. colli");

        texNumeroColli.setText("numero_colli");
        texNumeroColli.setDbDescCampo("");
        texNumeroColli.setDbNomeCampo("numero_colli");
        texNumeroColli.setDbTipoCampo("");
        texNumeroColli.setFont(texNumeroColli.getFont().deriveFont(texNumeroColli.getFont().getSize()-1f));
        texNumeroColli.setmaxChars(255);

        labFa3.setFont(labFa3.getFont().deriveFont(labFa3.getFont().getSize()-1f));
        labFa3.setText("Data / ora");

        texDataOra.setText("dataoraddt");
        texDataOra.setDbDescCampo("");
        texDataOra.setDbNomeCampo("dataoraddt");
        texDataOra.setDbTipoCampo("");
        texDataOra.setFont(texDataOra.getFont().deriveFont(texDataOra.getFont().getSize()-1f));
        texDataOra.setmaxChars(255);

        labPesoLordo.setFont(labPesoLordo.getFont().deriveFont(labPesoLordo.getFont().getSize()-1f));
        labPesoLordo.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labPesoLordo.setText("Peso lordo");

        texPesoLordo.setText("peso_lordo");
        texPesoLordo.setDbDescCampo("");
        texPesoLordo.setDbNomeCampo("peso_lordo");
        texPesoLordo.setDbTipoCampo("");
        texPesoLordo.setFont(texPesoLordo.getFont().deriveFont(texPesoLordo.getFont().getSize()-1f));
        texPesoLordo.setmaxChars(255);

        labPesoNetto.setFont(labPesoNetto.getFont().deriveFont(labPesoNetto.getFont().getSize()-1f));
        labPesoNetto.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labPesoNetto.setText("Peso netto");

        texPesoNetto.setText("peso_netto");
        texPesoNetto.setDbDescCampo("");
        texPesoNetto.setDbNomeCampo("peso_netto");
        texPesoNetto.setDbTipoCampo("");
        texPesoNetto.setFont(texPesoNetto.getFont().deriveFont(texPesoNetto.getFont().getSize()-1f));
        texPesoNetto.setmaxChars(255);

        org.jdesktop.layout.GroupLayout dati_altri2Layout = new org.jdesktop.layout.GroupLayout(dati_altri2);
        dati_altri2.setLayout(dati_altri2Layout);
        dati_altri2Layout.setHorizontalGroup(
            dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(dati_altri2Layout.createSequentialGroup()
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(comClieDest, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(jLabel15)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sepDestMerce))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labScon11)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestIndirizzo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labScon10)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestRagioneSociale, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labScon12)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestCap, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labScon13)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestLocalita, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labScon14)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestProvincia, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labScon17)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comPaese, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labScon16)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestTelefono, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 83, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labScon15)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDestCellulare, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFaTitolo)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sepFattAcc)
                        .add(1, 1, 1))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFa1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comCausaleTrasporto, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFa2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comAspettoEsterioreBeni, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFa4)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comVettori, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFa3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texDataOra, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labPesoNetto)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texPesoNetto, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFa5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comMezzoTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFa6)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(comPorto, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(dati_altri2Layout.createSequentialGroup()
                        .add(labFa7)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texNumeroColli, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(labPesoLordo)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texPesoLordo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)))
                .add(15, 15, 15))
        );

        dati_altri2Layout.linkSize(new java.awt.Component[] {labFa1, labFa2}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        dati_altri2Layout.setVerticalGroup(
            dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(dati_altri2Layout.createSequentialGroup()
                .addContainerGap()
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(sepDestMerce, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel15))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(comClieDest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon10)
                    .add(texDestRagioneSociale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon11)
                    .add(texDestIndirizzo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon12)
                    .add(texDestCap, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labScon13)
                    .add(labScon14)
                    .add(texDestProvincia, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texDestLocalita, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon16)
                    .add(labScon15)
                    .add(texDestCellulare, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(texDestTelefono, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labScon17)
                    .add(comPaese, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(labFaTitolo)
                    .add(sepFattAcc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labFa1)
                    .add(comCausaleTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labFa2)
                    .add(comAspettoEsterioreBeni, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labFa4)
                    .add(comVettori, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labFa5)
                    .add(comMezzoTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labFa6)
                    .add(comPorto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labFa7)
                    .add(texNumeroColli, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labPesoLordo)
                    .add(texPesoLordo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(dati_altri2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labFa3)
                    .add(texDataOra, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labPesoNetto)
                    .add(texPesoNetto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel61.setText(" ");

        org.jdesktop.layout.GroupLayout datiLayout = new org.jdesktop.layout.GroupLayout(dati);
        dati.setLayout(datiLayout);
        datiLayout.setHorizontalGroup(
            datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiLayout.createSequentialGroup()
                .addContainerGap()
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(datiLayout.createSequentialGroup()
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(datiLayout.createSequentialGroup()
                                .add(labModConsegna)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comConsegna, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(labModScarico)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comScarico, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(datiLayout.createSequentialGroup()
                                .add(labAgente)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comAgente, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(labProvvigione)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texProvvigione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(labPercentoProvvigione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiLayout.createSequentialGroup()
                                .add(labScon1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(labScon21)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(labScon2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel114)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel113)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(labMarcaBollo1))
                            .add(datiLayout.createSequentialGroup()
                                .add(texScon1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texScon2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texScon3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texSpeseTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texSpeseIncasso, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(bollo_si_no, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(bollo_importo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(labMarcaBollo))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel54)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cheOpzioneRibaDestDiversa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel18)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texBancAbi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(1, 1, 1)
                                .add(butCoor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(labBancAbi))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel23)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texBancCab, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(5, 5, 5)
                                .add(labBancCab))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel20)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texBancIban, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel24)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texNotePagamento, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel19)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comPaga, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 251, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(butScad, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(labGiornoPagamento)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texGiornoPagamento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel17)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texPaga2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(datiLayout.createSequentialGroup()
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(datiLayout.createSequentialGroup()
                                        .add(jLabel14)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jLabel13)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jLabel16))
                                    .add(datiLayout.createSequentialGroup()
                                        .add(texSeri, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(texNume, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(texData, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(datiLayout.createSequentialGroup()
                                        .add(jLabel151)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(butPrezziPrec))
                                    .add(datiLayout.createSequentialGroup()
                                        .add(texCliente)
                                        .add(0, 0, 0)
                                        .add(apriclienti, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(butAddClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                            .add(datiLayout.createSequentialGroup()
                                .add(jLabel11)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texNote, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 420, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(datiLayout.createSequentialGroup()
                        .add(texTotaImpo1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTotaIva1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTota1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texClieDest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTotaRitenuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTotaDaPagare, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(datiLayout.createSequentialGroup()
                        .add(texAnno, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTipoFattura, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(comClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(prezzi_ivati, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(datiLayout.createSequentialGroup()
                        .add(texRitenuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texRivalsa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texForni1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sepFaSeparatore, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(1, 1, 1)
                .add(dati_altri2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
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

        datiLayout.linkSize(new java.awt.Component[] {jLabel18, jLabel20, jLabel23}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.linkSize(new java.awt.Component[] {jLabel17, jLabel19}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiLayout.setVerticalGroup(
            datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiLayout.createSequentialGroup()
                .add(1, 1, 1)
                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sepFaSeparatore)
                    .add(datiLayout.createSequentialGroup()
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(datiLayout.createSequentialGroup()
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel14)
                                    .add(jLabel13)
                                    .add(jLabel16)
                                    .add(jLabel151)
                                    .add(butPrezziPrec))
                                .add(0, 0, 0)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                            .add(texSeri, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(texNume, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(texData, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(texCliente, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                        .add(apriclienti, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(butAddClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(jLabel61))
                                .add(1, 1, 1)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(labScon1)
                                    .add(labScon21)
                                    .add(labScon2)
                                    .add(jLabel114)
                                    .add(jLabel113)
                                    .add(labMarcaBollo1))
                                .add(0, 0, 0)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(texScon1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(texScon2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(texScon3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(texSpeseTrasporto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(texSpeseIncasso, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(bollo_si_no, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(bollo_importo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(labMarcaBollo)))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel11)
                                    .add(texNote, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel17)
                                    .add(texPaga2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel19)
                                    .add(comPaga, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(butScad, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(labGiornoPagamento)
                                    .add(texGiornoPagamento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel24)
                                    .add(texNotePagamento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel54)
                                    .add(cheOpzioneRibaDestDiversa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel18)
                                    .add(texBancAbi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(butCoor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(labBancAbi))
                                .add(0, 0, 0)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel23)
                                    .add(texBancCab, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(labBancCab))
                                .add(0, 0, 0)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel20)
                                    .add(texBancIban, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(labAgente)
                                    .add(comAgente, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(labProvvigione)
                                    .add(texProvvigione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(labPercentoProvvigione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(2, 2, 2)
                                .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(labModConsegna)
                                    .add(comConsegna, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(labModScarico)
                                    .add(comScarico, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                            .add(dati_altri2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(0, 0, 0)
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(texAnno, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texTipoFattura, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(comClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texClie, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(prezzi_ivati, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(texTotaImpo1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texTotaIva1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texTota1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texClieDest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texTotaRitenuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texTotaDaPagare, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(datiLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(texRitenuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texRivalsa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(texForni1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .add(0, 0, 0))
        );

        datiLayout.linkSize(new java.awt.Component[] {apriclienti, butAddClie, texCliente}, org.jdesktop.layout.GroupLayout.VERTICAL);

        scrollDati.setViewportView(dati);

        split.setTopComponent(scrollDati);

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
        butNuovArti.setMnemonic('r');
        butNuovArti.setText("Inserisci nuova riga");
        butNuovArti.setName("nuova_riga"); // NOI18N
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
        datiRighe.add(jPanel2, java.awt.BorderLayout.SOUTH);

        split.setBottomComponent(datiRighe);

        panDati.add(split, java.awt.BorderLayout.CENTER);

        tabDocumento.addTab("Dati Fattura", panDati);

        panFoglioRighe.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                panFoglioRigheFocusGained(evt);
            }
        });
        panFoglioRighe.setLayout(new java.awt.BorderLayout());

        panGriglia.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                panGrigliaFocusGained(evt);
            }
        });
        panGriglia.setLayout(new java.awt.BorderLayout());

        foglio.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        foglio.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                foglioMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                foglioMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                foglioMouseReleased(evt);
            }
        });
        jScrollPane2.setViewportView(foglio);

        panGriglia.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        panFoglioRighe.add(panGriglia, java.awt.BorderLayout.CENTER);

        butSalvaFoglioRighe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/devices/media-floppy.png"))); // NOI18N
        butSalvaFoglioRighe.setText("Salva e rimani");
        butSalvaFoglioRighe.setName("salva"); // NOI18N
        butSalvaFoglioRighe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSalvaFoglioRigheActionPerformed(evt);
            }
        });
        panTotale.add(butSalvaFoglioRighe);

        labStatus.setText("...");
        panTotale.add(labStatus);

        panFoglioRighe.add(panTotale, java.awt.BorderLayout.SOUTH);

        tabDocumento.addTab("Foglio Righe", panFoglioRighe);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Fornitore");

        texForni.setDbNomeCampo("");
        texForni.setDbRiempire(false);
        texForni.setDbSalvare(false);
        texForni.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                texForniFocusLost(evt);
            }
        });

        comForni.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comForni.setDbRiempire(false);
        comForni.setDbSalvaKey(false);
        comForni.setDbSalvare(false);
        comForni.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comForniItemStateChanged(evt);
            }
        });

        jLabel4.setFont(jLabel4.getFont());
        jLabel4.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel4.setText("Selezionando un fornitore potrai utilizzare la sua intestazione in fase di stampa del documento");

        org.jdesktop.layout.GroupLayout pan_segnaposto_depositoLayout = new org.jdesktop.layout.GroupLayout(pan_segnaposto_deposito);
        pan_segnaposto_deposito.setLayout(pan_segnaposto_depositoLayout);
        pan_segnaposto_depositoLayout.setHorizontalGroup(
            pan_segnaposto_depositoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );
        pan_segnaposto_depositoLayout.setVerticalGroup(
            pan_segnaposto_depositoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        labNoteConsegna.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labNoteConsegna.setText("Note consegna");

        texNoteConsegna.setDbNomeCampo("note_consegna");
        texNoteConsegna.setRows(5);

        comValuta.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comValuta.setDbNomeCampo("valuta");
        comValuta.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                comValutaItemStateChanged(evt);
            }
        });

        labvaluta.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labvaluta.setText("Valuta");

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
                            .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 265, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(datiAltroLayout.createSequentialGroup()
                                .add(jLabel1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(texForni, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 48, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(comForni, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 275, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jLabel4)
                            .add(pan_segnaposto_deposito, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(datiAltroLayout.createSequentialGroup()
                                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(labCampoLibero1)
                                    .add(labvaluta))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(comValuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 275, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(comCampoLibero1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 250, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        datiAltroLayout.linkSize(new java.awt.Component[] {jLabel1, labNoteConsegna, labvaluta}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiAltroLayout.setVerticalGroup(
            datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiAltroLayout.createSequentialGroup()
                .addContainerGap()
                .add(pan_segnaposto_deposito, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(9, 9, 9)
                .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(texForni, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(comForni, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(jLabel4)
                .add(18, 18, 18)
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(labNoteConsegna)
                    .add(texNoteConsegna, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(7, 7, 7)
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labCampoLibero1)
                    .add(comCampoLibero1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiAltroLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(labvaluta)
                    .add(comValuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(510, Short.MAX_VALUE))
        );

        datiAltroLayout.linkSize(new java.awt.Component[] {comForni, texForni}, org.jdesktop.layout.GroupLayout.VERTICAL);

        datiAltroLayout.linkSize(new java.awt.Component[] {comValuta, labvaluta}, org.jdesktop.layout.GroupLayout.VERTICAL);

        tabDocumento.addTab("Altro", datiAltro);

        scrollDatiPa.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        datiPa.setName("datiPa"); // NOI18N

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getSize()+1f));
        jLabel5.setText("Dati aggiuntivi per l'export della fattura elettronica per la Pubblica Amministrazione -");

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel7.setText("Dati Ordine Acquisto");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Riferimento Numero Linea");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Id Documento");

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Data");

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Num Item");

        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel22.setText("Codice Commessa Convenzione");

        jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel27.setText("Codice CUP");

        jLabel28.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel28.setText("Codice CIG");

        jLabel29.setFont(jLabel29.getFont().deriveFont(jLabel29.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel29.setText("Dati Contratto");

        jLabel30.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel30.setText("Riferimento Numero Linea");

        jLabel31.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel31.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel31.setText("Id Documento");

        jLabel32.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel32.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel32.setText("Data");

        jLabel33.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel33.setText("Num Item");

        jLabel34.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel34.setText("Codice Commessa Convenzione");

        jLabel35.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel35.setText("Codice CUP");

        jLabel36.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel36.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel36.setText("Codice CIG");

        linkcodiceufficio.setText("Documentazione sui dati");
        linkcodiceufficio.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        linkcodiceufficio.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        linkcodiceufficio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linkcodiceufficioActionPerformed(evt);
            }
        });

        dg_doa_riferimentonumerolinea.setColumns(6);
        dg_doa_riferimentonumerolinea.setDbNomeCampo("dg_doa_riferimentonumerolinea");

        dg_doa_iddocumento.setColumns(20);
        dg_doa_iddocumento.setDbNomeCampo("dg_doa_iddocumento");

        dg_doa_data.setColumns(12);
        dg_doa_data.setDbDescCampo("");
        dg_doa_data.setDbNomeCampo("dg_doa_data");
        dg_doa_data.setDbTipoCampo("data");

        dg_doa_numitem.setColumns(20);
        dg_doa_numitem.setDbNomeCampo("dg_doa_numitem");

        dg_doa_codicecommessaconvenzione.setColumns(20);
        dg_doa_codicecommessaconvenzione.setDbNomeCampo("dg_doa_codicecommessaconvenzione");

        dg_doa_codicecup.setColumns(20);
        dg_doa_codicecup.setDbNomeCampo("dg_doa_codicecup");

        dg_doa_codicecig.setColumns(20);
        dg_doa_codicecig.setDbNomeCampo("dg_doa_codicecig");

        dg_dc_riferimentonumerolinea.setColumns(6);
        dg_dc_riferimentonumerolinea.setDbNomeCampo("dg_dc_riferimentonumerolinea");

        dg_dc_iddocumento.setColumns(20);
        dg_dc_iddocumento.setDbNomeCampo("dg_dc_iddocumento");

        dg_dc_data.setColumns(12);
        dg_dc_data.setDbNomeCampo("dg_dc_data");
        dg_dc_data.setDbTipoCampo("data");

        dg_dc_numitem.setColumns(20);
        dg_dc_numitem.setDbNomeCampo("dg_dc_numitem");

        dg_dc_codicecommessaconvenzione.setColumns(20);
        dg_dc_codicecommessaconvenzione.setDbNomeCampo("dg_dc_codicecommessaconvenzione");

        dg_dc_codicecup.setColumns(20);
        dg_dc_codicecup.setDbNomeCampo("dg_dc_codicecup");

        dg_dc_codicecig.setColumns(20);
        dg_dc_codicecig.setDbNomeCampo("dg_dc_codicecig");

        jLabel37.setFont(jLabel37.getFont().deriveFont(jLabel37.getFont().getSize()-1f));
        jLabel37.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel37.setText("linea di dettaglio della fattura a cui si fa riferimento (se il riferimento è all'intera fattura, non viene valorizzato)");

        jLabel38.setFont(jLabel38.getFont().deriveFont(jLabel38.getFont().getSize()-1f));
        jLabel38.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel38.setText("numero del documento ");

        jLabel39.setFont(jLabel39.getFont().deriveFont(jLabel39.getFont().getSize()-1f));
        jLabel39.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel39.setText("data del documento (formato italiano gg/mm/aaaa)");

        jLabel40.setFont(jLabel40.getFont().deriveFont(jLabel40.getFont().getSize()-1f));
        jLabel40.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel40.setText("identificativo della singola voce all'interno del documento");

        jLabel41.setFont(jLabel41.getFont().deriveFont(jLabel41.getFont().getSize()-1f));
        jLabel41.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel41.setText("codice della commessa o della convenzione ");

        jLabel42.setFont(jLabel42.getFont().deriveFont(jLabel42.getFont().getSize()-1f));
        jLabel42.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel42.setText("codice gestito dal CIPE che caratterizza ogni progetto di investimento pubblico (Codice Unitario Progetto)");

        jLabel43.setFont(jLabel43.getFont().deriveFont(jLabel43.getFont().getSize()-1f));
        jLabel43.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel43.setText("codice Identificativo della Gara ");

        jLabel44.setFont(jLabel44.getFont().deriveFont(jLabel44.getFont().getSize()-1f));
        jLabel44.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel44.setText("linea di dettaglio della fattura a cui si fa riferimento (se il riferimento è all'intera fattura, non viene valorizzato)");

        jLabel45.setFont(jLabel45.getFont().deriveFont(jLabel45.getFont().getSize()-1f));
        jLabel45.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel45.setText("numero del documento ");

        jLabel46.setFont(jLabel46.getFont().deriveFont(jLabel46.getFont().getSize()-1f));
        jLabel46.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel46.setText("data del documento (formato italiano gg/mm/aaaa)");

        jLabel47.setFont(jLabel47.getFont().deriveFont(jLabel47.getFont().getSize()-1f));
        jLabel47.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel47.setText("identificativo della singola voce all'interno del documento");

        jLabel48.setFont(jLabel48.getFont().deriveFont(jLabel48.getFont().getSize()-1f));
        jLabel48.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel48.setText("codice della commessa o della convenzione ");

        jLabel49.setFont(jLabel49.getFont().deriveFont(jLabel49.getFont().getSize()-1f));
        jLabel49.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel49.setText("codice gestito dal CIPE che caratterizza ogni progetto di investimento pubblico (Codice Unitario Progetto)");

        jLabel50.setFont(jLabel50.getFont().deriveFont(jLabel50.getFont().getSize()-1f));
        jLabel50.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel50.setText("codice Identificativo della Gara ");

        jLabel51.setFont(jLabel51.getFont().deriveFont(jLabel51.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel51.setText("Dati Pagamento");

        jLabel52.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel52.setText("Istituto Finanziario");

        dp_banca.setColumns(60);
        dp_banca.setDbNomeCampo("dp_istituto_finanziario");

        jLabel53.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel53.setText("IBAN");

        dp_iban.setColumns(40);
        dp_iban.setDbNomeCampo("dp_iban");

        dp_iban_lab.setText("...");

        jButton1.setText("Leggi da fattura");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel55.setFont(jLabel55.getFont().deriveFont(jLabel55.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel55.setText("Dati Ritenuta d'acconto");

        lregfis.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lregfis.setText("Tipo ritenuta");

        lregfis1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lregfis1.setText("Causale pagamento");

        linkcodiceufficio1.setText("Documentazione");
        linkcodiceufficio1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        linkcodiceufficio1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        linkcodiceufficio1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linkcodiceufficio1ActionPerformed(evt);
            }
        });

        jLabel56.setFont(jLabel56.getFont().deriveFont(jLabel56.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel56.setText("Dati Rivalsa / Cassa previdenziale");

        lregfis2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lregfis2.setText("Tipo cassa");

        dg_dr_tipo_ritenuta.setEditable(false);
        dg_dr_tipo_ritenuta.setDbDescCampo("");
        dg_dr_tipo_ritenuta.setDbNomeCampo("dg_dr_tipo_ritenuta");
        dg_dr_tipo_ritenuta.setDbTipoCampo("VARCHAR");
        dg_dr_tipo_ritenuta.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dg_dr_tipo_ritenutaItemStateChanged(evt);
            }
        });
        dg_dr_tipo_ritenuta.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dg_dr_tipo_ritenutaFocusLost(evt);
            }
        });

        dg_dr_causale_pagamento.setEditable(false);
        dg_dr_causale_pagamento.setDbDescCampo("");
        dg_dr_causale_pagamento.setDbNomeCampo("dg_dr_causale_pagamento");
        dg_dr_causale_pagamento.setDbTipoCampo("VARCHAR");
        dg_dr_causale_pagamento.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dg_dr_causale_pagamentoItemStateChanged(evt);
            }
        });
        dg_dr_causale_pagamento.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dg_dr_causale_pagamentoFocusLost(evt);
            }
        });

        dg_dcp_tipo_cassa.setEditable(false);
        dg_dcp_tipo_cassa.setDbDescCampo("");
        dg_dcp_tipo_cassa.setDbNomeCampo("dg_dcp_tipo_cassa");
        dg_dcp_tipo_cassa.setDbTipoCampo("VARCHAR");
        dg_dcp_tipo_cassa.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dg_dcp_tipo_cassaItemStateChanged(evt);
            }
        });
        dg_dcp_tipo_cassa.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dg_dcp_tipo_cassaFocusLost(evt);
            }
        });

        dg_dr_totale_da_esportare.setEditable(false);
        dg_dr_totale_da_esportare.setDbDescCampo("");
        dg_dr_totale_da_esportare.setDbNomeCampo("dg_dr_totale_da_esportare");
        dg_dr_totale_da_esportare.setDbTipoCampo("VARCHAR");
        dg_dr_totale_da_esportare.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dg_dr_totale_da_esportareItemStateChanged(evt);
            }
        });
        dg_dr_totale_da_esportare.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dg_dr_totale_da_esportareFocusLost(evt);
            }
        });

        lregfis3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lregfis3.setText("Totale da esportare");

        jLabel58.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel58.setText("Causale");

        dg_causale.setColumns(60);
        dg_causale.setDbNomeCampo("dg_causale");

        jLabel59.setFont(jLabel59.getFont().deriveFont(jLabel59.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel59.setText("Dati generali documento");

        split_payment.setText("Split payment");
        split_payment.setDbNomeCampo("split_payment");
        split_payment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                split_paymentActionPerformed(evt);
            }
        });

        jLabel60.setFont(jLabel60.getFont().deriveFont(jLabel60.getFont().getSize()-1f));
        jLabel60.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.disabledForeground"));
        jLabel60.setText("art. 1, comma 629, lett. b) della L. n. 190/2014 (Legge di Stabilità 2015)");

        org.jdesktop.layout.GroupLayout datiPaLayout = new org.jdesktop.layout.GroupLayout(datiPa);
        datiPa.setLayout(datiPaLayout);
        datiPaLayout.setHorizontalGroup(
            datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiPaLayout.createSequentialGroup()
                .addContainerGap()
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(datiPaLayout.createSequentialGroup()
                        .add(split_payment, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jLabel60))
                    .add(datiPaLayout.createSequentialGroup()
                        .add(jLabel5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(linkcodiceufficio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jLabel7)
                    .add(jLabel29)
                    .add(jLabel51)
                    .add(jLabel55)
                    .add(jLabel56)
                    .add(jLabel59)
                    .add(datiPaLayout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(datiPaLayout.createSequentialGroup()
                                .add(lregfis2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dcp_tipo_cassa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(lregfis)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dr_tipo_ritenuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel9)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_doa_iddocumento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel38))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel8)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_doa_riferimentonumerolinea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel37))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel10)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_doa_data, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel39))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel12)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_doa_numitem, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel40))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel22)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_doa_codicecommessaconvenzione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel41))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel27)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_doa_codicecup, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel42))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel28)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_doa_codicecig, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel43))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel31)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dc_iddocumento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel45))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel30)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dc_riferimentonumerolinea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel44))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel32)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dc_data, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel46))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel33)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dc_numitem, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel47))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel34)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dc_codicecommessaconvenzione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel48))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel35)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dc_codicecup, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel49))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel36)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dc_codicecig, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel50))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel52)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dp_banca, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel53)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(dp_iban_lab)
                                    .add(datiPaLayout.createSequentialGroup()
                                        .add(dp_iban, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jButton1))))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(lregfis1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dr_causale_pagamento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(linkcodiceufficio1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(lregfis3)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_dr_totale_da_esportare, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(datiPaLayout.createSequentialGroup()
                                .add(jLabel58)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(dg_causale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        datiPaLayout.linkSize(new java.awt.Component[] {jLabel10, jLabel12, jLabel22, jLabel27, jLabel28, jLabel8, jLabel9}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiPaLayout.linkSize(new java.awt.Component[] {jLabel30, jLabel31, jLabel32, jLabel33, jLabel34, jLabel35, jLabel36}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiPaLayout.linkSize(new java.awt.Component[] {jLabel52, jLabel53}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiPaLayout.linkSize(new java.awt.Component[] {lregfis, lregfis1}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        datiPaLayout.setVerticalGroup(
            datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(datiPaLayout.createSequentialGroup()
                .addContainerGap()
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(linkcodiceufficio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(split_payment, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel60))
                .add(18, 18, 18)
                .add(jLabel51)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel53)
                    .add(dp_iban, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton1))
                .add(1, 1, 1)
                .add(dp_iban_lab)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel52)
                    .add(dp_banca, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel55)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lregfis)
                    .add(dg_dr_tipo_ritenuta, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lregfis1)
                    .add(linkcodiceufficio1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(dg_dr_causale_pagamento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(dg_dr_totale_da_esportare, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(lregfis3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel56)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lregfis2)
                    .add(dg_dcp_tipo_cassa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel59)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel58)
                    .add(dg_causale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel7)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(dg_doa_riferimentonumerolinea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel37))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(dg_doa_iddocumento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel38))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(dg_doa_data, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel39))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel12)
                    .add(dg_doa_numitem, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel40))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel22)
                    .add(dg_doa_codicecommessaconvenzione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel41))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel27)
                    .add(dg_doa_codicecup, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel42))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel28)
                    .add(dg_doa_codicecig, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel43))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel29)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel30)
                    .add(dg_dc_riferimentonumerolinea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel44))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel31)
                    .add(dg_dc_iddocumento, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel45))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel32)
                    .add(dg_dc_data, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel46))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel33)
                    .add(dg_dc_numitem, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel47))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel34)
                    .add(dg_dc_codicecommessaconvenzione, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel48))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel35)
                    .add(dg_dc_codicecup, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel49))
                .add(1, 1, 1)
                .add(datiPaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel36)
                    .add(dg_dc_codicecig, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel50))
                .addContainerGap(303, Short.MAX_VALUE))
        );

        scrollDatiPa.setViewportView(datiPa);

        tabDocumento.addTab("P.A.", scrollDatiPa);

        tutto.add(tabDocumento, java.awt.BorderLayout.CENTER);

        jPanel5.setLayout(new java.awt.BorderLayout());

        panSalva.setPreferredSize(new java.awt.Dimension(216, 140));

        butStampa.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/document-print.png"))); // NOI18N
        butStampa.setText("Stampa");
        butStampa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butStampaActionPerformed(evt);
            }
        });

        jLabel3.setForeground(javax.swing.UIManager.getDefaults().getColor("Label.background"));

        butUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/actions/edit-undo.png"))); // NOI18N
        butUndo.setText("Annulla");
        butUndo.setName("annulla"); // NOI18N
        butUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butUndoActionPerformed(evt);
            }
        });

        butSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/tango-icon-theme-080/16x16/devices/media-floppy.png"))); // NOI18N
        butSave.setText("Salva");
        butSave.setName("salva"); // NOI18N
        butSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveActionPerformed(evt);
            }
        });

        butPdf.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/pdf-icon-16.png"))); // NOI18N
        butPdf.setText("Crea PDF");
        butPdf.setMargin(new java.awt.Insets(2, 4, 2, 4));
        butPdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butPdfActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout panSalvaLayout = new org.jdesktop.layout.GroupLayout(panSalva);
        panSalva.setLayout(panSalvaLayout);
        panSalvaLayout.setHorizontalGroup(
            panSalvaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panSalvaLayout.createSequentialGroup()
                .add(5, 5, 5)
                .add(panSalvaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(panSalvaLayout.createSequentialGroup()
                        .add(butUndo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 97, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(4, 4, 4)
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(butSave, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 97, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(butStampa, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(butPdf, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panSalvaLayout.setVerticalGroup(
            panSalvaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panSalvaLayout.createSequentialGroup()
                .add(5, 5, 5)
                .add(panSalvaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(panSalvaLayout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(jLabel3))
                    .add(panSalvaLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(butSave, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(butUndo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(butStampa, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(butPdf, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        jPanel5.add(panSalva, java.awt.BorderLayout.WEST);

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

        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel21.setText("Totale Iva");

        jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel25.setText("Totale Imponibile");

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

        jLabel57.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel57.setText("Acconto");

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

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getStyle() | java.awt.Font.BOLD, jLabel6.getFont().getSize()+1));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Tot. da Pagare");

        texTotaDaPagareFinale.setEditable(false);
        texTotaDaPagareFinale.setBorder(null);
        texTotaDaPagareFinale.setColumns(10);
        texTotaDaPagareFinale.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        texTotaDaPagareFinale.setText("0");
        texTotaDaPagareFinale.setDbTipoCampo("valuta");
        texTotaDaPagareFinale.setFont(texTotaDaPagareFinale.getFont().deriveFont(texTotaDaPagareFinale.getFont().getStyle() | java.awt.Font.BOLD, texTotaDaPagareFinale.getFont().getSize()+1));

        org.jdesktop.layout.GroupLayout panRitenuteLayout = new org.jdesktop.layout.GroupLayout(panRitenute);
        panRitenute.setLayout(panRitenuteLayout);
        panRitenuteLayout.setHorizontalGroup(
            panRitenuteLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 395, Short.MAX_VALUE)
        );
        panRitenuteLayout.setVerticalGroup(
            panRitenuteLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .add(0, 0, 0)
                .add(panRitenute, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel26)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texSconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel25)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTotaImpo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel21)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTotaIva, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTota, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel57)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texAcconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                        .add(jLabel6)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(texTotaDaPagareFinale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(new java.awt.Component[] {texAcconto, texSconto, texTota, texTotaDaPagareFinale, texTotaImpo, texTotaIva}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(5, 5, 5)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel26)
                    .add(texSconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(2, 2, 2)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(texTotaImpo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel25))
                .add(2, 2, 2)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(texTotaIva, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel21))
                .add(2, 2, 2)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(texTota, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .add(2, 2, 2)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(texAcconto, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel57))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(texTotaDaPagareFinale, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6))
                .addContainerGap(31, Short.MAX_VALUE))
            .add(panRitenute, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jPanel5.add(jPanel4, java.awt.BorderLayout.CENTER);

        tutto.add(jPanel5, java.awt.BorderLayout.SOUTH);

        getContentPane().add(tutto, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void texNumeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texNumeFocusLost
        texNume.setText(texNume.getText().replaceAll("[^\\d.]", ""));
        final String old_id_final = old_id;
        if (!old_id.equals(texNume.getText())) {

            //controllo che se è un numero già presente non glielo faccio fare percè altrimenti sovrascrive una altra fattura
            sql = "select numero from test_fatt";
            sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
            sql += " and numero " + Db.pcW(texNume.getText(), "NUMBER");
            sql += " and anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
            sql += " and tipo_fattura != 7";
            ResultSet r = Db.openResultSet(sql);
            try {
                if (r.next()) {
                    texNume.setText(old_id_final);
                    JOptionPane.showMessageDialog(this, "Non puoi mettere il numero di una fattura già presente !", "Attenzione", JOptionPane.INFORMATION_MESSAGE);
                    return;
                } else {
                    //controllo se presente in distinta riba
                    sql = "select id, data_scadenza, importo, distinta from scadenze";
                    sql += " where documento_tipo = " + Db.pc(Db.TIPO_DOCUMENTO_FATTURA, Types.VARCHAR);
                    sql += " and id_doc = " + id;
                    sql += " and distinta is not null";
                    try {
                        ResultSet resu = DbUtils.tryOpenResultSet(Db.getConn(), sql);
                        if (resu.next() == true) {
                            String msg = "La fattura e' legata ad una o piu' scadenze gia' stampate in distinta\nProsegunedo nel cambio del numero la distinta presentata non corrisponderà con questa fattura\nProsegui ?";
                            int ret = javax.swing.JOptionPane.showConfirmDialog(null, msg, "Attenzione", javax.swing.JOptionPane.YES_NO_OPTION);
                            if (ret == javax.swing.JOptionPane.NO_OPTION) {
                                texNume.setText(old_id_final);
                                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                                return;
                            }
                        }
                        resu.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //associo al nuovo numero
                    dbdoc.numero = new Integer(this.texNume.getText()).intValue();
                    
                    if (!main.edit_doc_in_temp) {
                        sql = "update righ_fatt";
                        sql += " set numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where id_padre = " + this.id;
                        Db.executeSql(sql);

                        sql = "update test_fatt";
                        sql += " set numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where id = " + this.id;
                        Db.executeSql(sql);

    //                    dati.dbChiaveValori.clear();
    //                    dati.dbChiaveValori.put("serie", prev.serie);
    //                    dati.dbChiaveValori.put("numero", prev.numero);
    //                    dati.dbChiaveValori.put("anno", prev.anno);
                        //riassocio
                        dbAssociaGrigliaRighe();
                        id_modificato = true;

                        dbdoc.numero = Integer.parseInt(texNume.getText());
                        dbdoc.setId(id);
                        doc.load(Db.INSTANCE, this.dbdoc.numero, this.dbdoc.serie, this.dbdoc.anno, Db.TIPO_DOCUMENTO_FATTURA, id);
                        ricalcolaTotali();

                        //vado ad aggiornare eventuali ddt o ordini legati
                        sql = "update test_ddt";
                        sql += " set fattura_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where fattura_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                        sql += " and fattura_numero " + Db.pcW(old_id_final, "NUMBER");
                        sql += " and fattura_anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
                        Db.executeSql(sql);

                        //vado ad aggiornare eventuali ddt o ordini legati
                        sql = "update test_ordi";
                        sql += " set doc_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                        sql += " where doc_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                        sql += " and doc_numero " + Db.pcW(old_id_final, "NUMBER");
                        sql += " and doc_anno " + Db.pcW(String.valueOf(this.dbdoc.anno), "VARCHAR");
                        sql += " and doc_tipo " + Db.pcW(String.valueOf(this.dbdoc.tipoDocumento), "VARCHAR");
                        Db.executeSql(sql);

                        //aggiorno scadenze
                        //non serve più si va per id_doc
                        //aggiorno provvigioni
                        //non serve più si va per id_doc
                    }
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }//GEN-LAST:event_texNumeFocusLost

    private void texNumeFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texNumeFocusGained
        System.err.println("old_id = " + texNume.getText() + " da texNumePrevFocusGained");
        old_id = texNume.getText();
        id_modificato = false;
    }//GEN-LAST:event_texNumeFocusGained

    private void texSpeseTrasportoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSpeseTrasportoKeyPressed
// TODO add your handling code here:
    }//GEN-LAST:event_texSpeseTrasportoKeyPressed

    private void texSpeseIncassoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texSpeseIncassoFocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseIncassoFocusLost

    private void texSpeseTrasportoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texSpeseTrasportoFocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseTrasportoFocusLost

    private void texScon3FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texScon3FocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_texScon3FocusLost

    private void foglioMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_foglioMouseClicked
        }//GEN-LAST:event_foglioMouseClicked

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        System.out.println("evt = " + evt);
    }//GEN-LAST:event_formKeyPressed

    private void tabDocumentoStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabDocumentoStateChanged

        if (tabDocumento.getSelectedIndex() == 1) {
            aggiornaFoglioRighe();
        } else {
            try {
                zoom.setVisible(false);
            } catch (Exception err) {
            }
        }
    }//GEN-LAST:event_tabDocumentoStateChanged

    private void panGrigliaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_panGrigliaFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_panGrigliaFocusGained

    private void panFoglioRigheFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_panFoglioRigheFocusGained
                            }//GEN-LAST:event_panFoglioRigheFocusGained

    private void popFoglioEliminaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popFoglioEliminaActionPerformed
        DefaultTableModel tableModel = (DefaultTableModel) foglio.getModel();
        int selrowcount = foglio.getSelectedRowCount();
        int[] selrows = foglio.getSelectedRows();
        try {
            foglio.getCellEditor().cancelCellEditing();
        } catch (Exception e) {
        }
        try {
            foglio.getSelectionModel().clearSelection();
        } catch (Exception e) {
        }
        for (int i = selrowcount - 1; i >= 0; i--) {
            //tableModel.removeRow(foglio.getSelectedRow());
            System.out.println("rimuovo la riga (" + i + "):" + selrows[i]);
            tableModel.removeRow(selrows[i]);
        }
    }//GEN-LAST:event_popFoglioEliminaActionPerformed

    private void foglio3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_foglio3MouseClicked

        if (evt.getModifiers() == InputEvent.BUTTON3_MASK) {
            popFoglio.show(foglio, evt.getX(), evt.getY());
        }//GEN-LAST:event_foglio3MouseClicked
    }

    private void comPagaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comPagaItemStateChanged
        if (evt != null && evt.getStateChange() == ItemEvent.DESELECTED) {
            return;
        }

        try {
            texGiornoPagamento.setEnabled(false);
            labGiornoPagamento.setEnabled(false);

            if (comPaga.getSelectedIndex() >= 0) {
                ResultSet r = Db.lookUp(String.valueOf(comPaga.getSelectedKey()), "codice", "pagamenti");
                if (Db.nz(r.getString("flag_richiedi_giorno"), "N").equalsIgnoreCase("S")) {
                    texGiornoPagamento.setEnabled(true);
                    labGiornoPagamento.setEnabled(true);
                    if (!in_apertura) {
                        //carico il giorno dal cliente
                        texGiornoPagamento.setText("");
                        //li recupero dal cliente
                        ResultSet tempClie;
                        String sql = "select giorno_pagamento from clie_forn";
                        sql += " where codice = " + Db.pc(this.texClie.getText(), "NUMERIC");
                        tempClie = Db.openResultSet(sql);
                        try {
                            if (tempClie.next() == true) {
                                texGiornoPagamento.setText(tempClie.getString("giorno_pagamento"));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        if (texGiornoPagamento.getText().equals("0")) {
                            texGiornoPagamento.setText("");
                        }
                    }
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }//GEN-LAST:event_comPagaItemStateChanged

    private void texSeriKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSeriKeyPressed

        if (evt.getKeyCode() == evt.VK_TAB || evt.getKeyCode() == evt.VK_ENTER) {
            String serie = texSeri.getText();
//            if (serie.equals("#") || serie.equals("*")) {
//                JOptionPane.showMessageDialog(this, "Non si puo' usare '#' o '*' come serie del documento", "Attenzione", JOptionPane.WARNING_MESSAGE);
//                texSeri.setText("");
//                return;
//            }
            assegnaSerie();
        }
    }//GEN-LAST:event_texSeriKeyPressed

    private void comAgenteFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comAgenteFocusLost
        InvoicexUtil.controlloProvvigioniAutomatiche(comAgente, texProvvigione, texScon1, this, cu.toInteger(texClie.getText()));
    }//GEN-LAST:event_comAgenteFocusLost

    private void comAgenteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comAgenteActionPerformed
                            }//GEN-LAST:event_comAgenteActionPerformed

    private void comPagaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comPagaFocusLost
        //carico note su pagamento
        try {
            ResultSet p = Db.openResultSet("select * from pagamenti where codice = " + Db.pc(this.comPaga.getSelectedKey(), Types.VARCHAR));
            if (p.next()) {
                this.texNotePagamento.setText(p.getString("note_su_documenti"));
            }

            aggiornaPaIban();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }//GEN-LAST:event_comPagaFocusLost

    private void comClieDestKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_comClieDestKeyPressed

        if (evt.getKeyCode() == evt.VK_ENTER) {
            caricaDestinazioneDiversa();
        }
    }//GEN-LAST:event_comClieDestKeyPressed

    private void comClieDestFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieDestFocusLost
        if (comClieDest.getSelectedIndex() != comClieDest_old) {
            caricaDestinazioneDiversa();
        }
    }//GEN-LAST:event_comClieDestFocusLost

    private void comClieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comClieActionPerformed
//        sql = "select obsoleto from clie_forn";
//        sql += " where codice = " + Db.pc(this.texClie.getText(), "NUMERIC");
//
//        ResultSet rs = Db.openResultSet(sql);
//        try {
//            if (rs.next()) {
//                int obsoleto = rs.getInt("obsoleto");
//                if (obsoleto == 1) {
//                    JOptionPane.showMessageDialog(this, "Attenzione, il cliente selezionato è segnato come obsoleto.", "Cliente obsoleto", JOptionPane.INFORMATION_MESSAGE);
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }

        //debug
        System.out.println(">selected");

        //apro combo destinazione cliente
        sql = "select ragione_sociale, id from clie_forn_dest";
        sql += " where codice_cliente = " + Db.pc(this.texClie.getText(), "NUMERIC");
        sql += " order by ragione_sociale";

        riempiDestDiversa(sql);

//            //SAMUELE INNOCENTI
//            //costringo l'utente a selezionare la destinazione diversa (se vuole)
//            this.comClieDest.setSelectedItem(null);
//            //quando cambia l'utente pulisco la destinazione diversa
//            texDestRagioneSociale.setText(null);
//            texDestIndirizzo.setText(null);
//            texDestCap.setText(null);
//            texDestLocalita.setText(null);
//            texDestProvincia.setText(null);
//            texDestTelefono.setText(null);
//            texDestCellulare.setText(null);                

    }//GEN-LAST:event_comClieActionPerformed

    private void texDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texDataActionPerformed
        // Add your handling code here:
    }//GEN-LAST:event_texDataActionPerformed

    private void butAddClieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butAddClieActionPerformed
        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        InvoicexUtil.genericFormAddCliente(this);
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_butAddClieActionPerformed

    private void butStampaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butStampaActionPerformed
        if (block_aggiornareProvvigioni) {
            return;
        }
        
        if (SwingUtils.showYesNoMessage(this, "Prima di stampare è necessario salvare il documento, proseguire ?")) {
            if (controlloCampi() == true) {
                this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                try {
                    InvoicexEvent event = new InvoicexEvent(this);
                    event.type = InvoicexEvent.TYPE_FRMTESTFATT_PRIMA_DI_SAVE;
                    main.events.fireInvoicexEvent(event);
                } catch (Exception err) {
                    err.printStackTrace();
                }
                saveDocumento();

                //scateno evento salvataggio nuova fattura
                if (dbStato.equalsIgnoreCase(this.DB_INSERIMENTO)) {
                    try {
                        InvoicexEvent event = new InvoicexEvent(this);
                        event.type = InvoicexEvent.TYPE_SALVA_NUOVA_FATTURA;
                        event.serie = texSeri.getText();
                        event.numero = Integer.parseInt(texNume.getText());
                        event.anno = Integer.parseInt(texAnno.getText());
                        main.events.fireInvoicexEvent(event);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
                main.events.fireInvoicexEvent(new InvoicexEvent(this, InvoicexEvent.TYPE_SAVE));
                if (from != null) {
                    this.from.dbRefresh();
                }
                //rinizializzo le cose che iniz all'inizio
                String nuova_serie = texSeri.getText();
                Integer nuovo_numero = cu.toInteger(texNume.getText());
                Integer nuovo_anno = cu.toInteger(texAnno.getText());

                if (!main.edit_doc_in_temp) {
                    //aggiorno le righe temp
                    sql = "update righ_fatt_temp";
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

                totaleDaPagareFinaleIniziale = doc.getTotale_da_pagare_finale();
                pagamentoIniziale = comPaga.getText();
                pagamentoInizialeGiorno = texGiornoPagamento.getText();
                provvigioniInizialeScadenze = dumpScadenze();
                provvigioniIniziale = Db.getDouble(texProvvigione.getText());
                codiceAgenteIniziale = it.tnx.Util.getInt(comAgente.getSelectedKey().toString());
                //proseguo con la stampa
                String tf = "";

                if (dbdoc.tipoFattura == dbdoc.TIPO_FATTURA_ACCOMPAGNATORIA) {
                    tf = "FA";
                }
                if (dbdoc.tipoFattura == dbdoc.TIPO_FATTURA_NOTA_DI_CREDITO) {
                    tf = "NC";
                }
                String dbSerie = this.dbdoc.serie;
                int dbNumero = this.dbdoc.numero;
                int dbAnno = this.dbdoc.anno;
                this.dati.dbSave();
                if (evt.getActionCommand().equalsIgnoreCase("pdf")) {
                    try {
                        InvoicexUtil.creaPdf(Db.TIPO_DOCUMENTO_FATTURA, new Integer[]{dbdoc.getId()}, true, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        SwingUtils.showExceptionMessage(this, e);
                    }
                } else {
                    frmElenFatt.stampa(tf, dbSerie, dbNumero, dbAnno, id);
                }

                //una volta salvatao e stampato entro in modalitaà modifica
                if (!main.edit_doc_in_temp) {
                    if (dbStato.equals(frmTestFatt.DB_INSERIMENTO)) {
                        dbStato = frmTestFatt.DB_MODIFICA;
                        //e riporto le righe in _temp
                        sql = "insert into righ_fatt_temp";
                        sql += " select *, '" + main.login + "' as username";
                        sql += " from righ_fatt";
                        sql += " where id_padre = " + dbdoc.getId();
                        try {
                            DbUtils.tryExecQuery(Db.getConn(), sql);
                            System.out.println("sql ok:" + sql);
                        } catch (Exception e) {
                            System.err.println("sql errore:" + sql);
                            e.printStackTrace();
                            trow = e;
                        }
                    } else {
                        porto_in_temp();
                    }
                }
                dati.dbCheckModificatiReset();
            }
        }
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_butStampaActionPerformed

    private void texSpeseTrasportoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texSpeseTrasportoActionPerformed
        // Add your handling code here:
    }//GEN-LAST:event_texSpeseTrasportoActionPerformed

    private void butPrezziPrecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butPrezziPrecActionPerformed
        showPrezziFatture();
    }//GEN-LAST:event_butPrezziPrecActionPerformed

    private void texSpeseTrasportoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSpeseTrasportoKeyReleased

        try {
            dbdoc.speseTrasportoIva = Db.getDouble(this.texSpeseTrasporto.getText());
        } catch (Exception err) {
            dbdoc.speseTrasportoIva = 0;
        }

        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseTrasportoKeyReleased

    private void formInternalFrameClosing(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_formInternalFrameClosing
        System.out.println("closing");
                            }//GEN-LAST:event_formInternalFrameClosing

    private void texBancAbiFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texBancAbiFocusLost
        texBancAbiActionPerformed(null);
    }//GEN-LAST:event_texBancAbiFocusLost

    private void texBancIbanFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texBancIbanFocusLost
        aggiornaPaIban();
}//GEN-LAST:event_texBancIbanFocusLost

    private void texBancCCFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texBancCCFocusLost
        //MC271102 ho ridisegnato la form e non ricordo a cosa serve
        /*
         if (Util.getScreenResolution() < Util.SCREEN_RES_1024x768) {
         if (evt.getOppositeComponent().getName().equalsIgnoreCase("butNuovArti")) {
         this.jScrollPane3.getViewport().setViewPosition(new java.awt.Point(0, 150));
         } else {
         this.jScrollPane3.getViewport().setViewPosition(new java.awt.Point(0, 0));
         }
         }
         */
    }//GEN-LAST:event_texBancCCFocusLost

    private void texBancIbanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texBancIbanActionPerformed
}//GEN-LAST:event_texBancIbanActionPerformed

    private void texBancAbiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texBancAbiActionPerformed
        caricaBanca();
    }//GEN-LAST:event_texBancAbiActionPerformed

    public void caricaBanca() {
        try {
            String descAbi = Db.lookUp(texBancAbi.getText(), "abi", "banche_abi").getString(2);
            descAbi = StringUtils.abbreviate(descAbi, CoordinateBancarie.maxAbi);
            labBancAbi.setText(descAbi);
        } catch (Exception err) {
            labBancAbi.setText("");
        }
    }

    private void butCoorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butCoorActionPerformed

        CoordinateBancarie coords = new CoordinateBancarie();
        coords.setField_texBancAbi(this.texBancAbi);
        coords.setField_labBancAbi(this.labBancAbi);
        coords.setField_texBancCab(this.texBancCab);
        coords.setField_labBancCab(this.labBancCab);

        frmListCoorBanc frm = new frmListCoorBanc(coords);
        main.getPadre().openFrame(frm, 700, 500, 150, 50);
    }//GEN-LAST:event_butCoorActionPerformed

    private void comClieItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comClieItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            aggiornaNote();
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_comClieItemStateChanged

    private void butUndo1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butUndo1ActionPerformed
    }//GEN-LAST:event_butUndo1ActionPerformed

    private void butScadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butScadActionPerformed
        saveDocumento();

//        Scadenze tempScad = new Scadenze(Db.TIPO_DOCUMENTO_FATTURA, this.texSeri.getText(), this.prev.numero, this.prev.anno, this.comPaga.getText());
        Scadenze tempScad = new Scadenze(Db.TIPO_DOCUMENTO_FATTURA, id, this.comPaga.getText());
        frmPagaPart frm = new frmPagaPart(tempScad, null);
        main.getPadre().openFrame(frm, 650, 550, 300, 100);
    }//GEN-LAST:event_butScadActionPerformed

    private void texDataFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texDataFocusLost

        if (!ju.isValidDateIta(texData.getText())) {
            SwingUtils.showFlashMessage2Comp("Data non valida", 3, texData, Color.red);
            return;
        }
        
        if (!old_anno.equals(getAnnoDaForm())) {
            if (dbStato == DB_INSERIMENTO) {
                dbdoc.dbRicalcolaProgressivo(dbStato, this.texData.getText(), this.texNume, texAnno, texSeri.getText(), id);
                dbdoc.numero = new Integer(this.texNume.getText()).intValue();
                id_modificato = true;
            } else {
                //controllo che se è un numero già presente non glielo faccio fare percè altrimenti sovrascrive una altra fattura
                sql = "select numero from test_fatt";
                sql += " where serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                sql += " and numero " + Db.pcW(texNume.getText(), "NUMBER");
                sql += " and anno " + Db.pcW(getAnnoDaForm(), "VARCHAR");
                sql += " and tipo_fattura != 7";
                ResultSet r = Db.openResultSet(sql);
                try {
                    if (r.next()) {
                        texData.setText(old_data);
                        JOptionPane.showMessageDialog(this, "Non puoi mettere questo numero e data, si sovrappongono ad una fattura già presente !", "Attenzione", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            try {
                texAnno.setText(getAnnoDaForm());
                dbdoc.anno = Integer.parseInt(getAnnoDaForm());
                dbdoc.numero = Integer.parseInt(texNume.getText());

                if (!main.edit_doc_in_temp) {
                
                    sql = "update righ_fatt";
                    sql += " set anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " , numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += " where id_padre = " + this.id;
                    Db.executeSql(sql);

                    sql = "update test_fatt";
                    sql += " set anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " , numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += " where id = " + this.id;
                    Db.executeSql(sql);

                    //riassocio
                    dbAssociaGrigliaRighe();

                    doc.load(Db.INSTANCE, dbdoc.numero, dbdoc.serie, dbdoc.anno, Db.TIPO_DOCUMENTO_FATTURA, id);
                    ricalcolaTotali();

                    anno_modificato = true;

                    //vado ad aggiornare eventuali ddt o ordini legati
                    sql = "update test_ddt";
                    sql += " set fattura_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += " , anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " where fattura_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                    sql += " and fattura_numero " + Db.pcW(old_id, "NUMBER");
                    sql += " and fattura_anno " + Db.pcW(String.valueOf(old_anno), "VARCHAR");
                    Db.executeSql(sql);

                    //vado ad aggiornare eventuali ddt o ordini legati
                    sql = "update test_ordi";
                    sql += " set doc_numero = " + Db.pc(dbdoc.numero, "NUMBER");
                    sql += " , anno = " + Db.pc(dbdoc.anno, "NUMBER");
                    sql += " where doc_serie " + Db.pcW(this.dbdoc.serie, "VARCHAR");
                    sql += " and doc_numero " + Db.pcW(old_id, "NUMBER");
                    sql += " and doc_anno " + Db.pcW(String.valueOf(old_anno), "VARCHAR");
                    sql += " and doc_tipo " + Db.pcW(String.valueOf(this.dbdoc.tipoDocumento), "VARCHAR");
                    Db.executeSql(sql);
                }

            } catch (Exception err) {
                err.printStackTrace();
            }
        } else {
            ricalcolaTotali();
        }

    }//GEN-LAST:event_texDataFocusLost

    private void texScon3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon3KeyReleased

        try {
            dbdoc.sconto3 = Db.getDouble(this.texScon3.getText());
        } catch (Exception err) {

            //err.printStackTrace();
            dbdoc.sconto3 = 0;
        }

        ricalcolaTotali();
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

            String sql = "select clie_forn.codice as id, clie_forn.ragione_sociale as ragione, clie_forn.indirizzo as indi from clie_forn " + "where clie_forn.ragione_sociale like '%" + Db.aa(this.comClie.getText()) + "%'" + " order by clie_forn.ragione_sociale";
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
        }//GEN-LAST:event_texClieKeyPressed
    }

    private void comClieFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieFocusLost
        if (comClie.getSelectedIndex() != comClieSel_old) {
            this.recuperaDatiCliente();
            ricalcolaTotali();
        }
    }//GEN-LAST:event_comClieFocusLost

    private void texClieFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texClieFocusLost

        try {

            if (this.texClie.getText().length() > 0) {
                this.dbdoc.forceCliente(Long.parseLong(this.texClie.getText()));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        ricalcolaTotali();
    }//GEN-LAST:event_texClieFocusLost

    private void texSpeseIncassoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texSpeseIncassoKeyReleased

        try {
            dbdoc.speseIncassoIva = Db.getDouble(this.texSpeseIncasso.getText());
        } catch (Exception err) {
            dbdoc.speseIncassoIva = 0;
        }

        ricalcolaTotali();
    }//GEN-LAST:event_texSpeseIncassoKeyReleased

    private void texScon2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon2KeyReleased

        try {
            dbdoc.sconto2 = Db.getDouble(this.texScon2.getText());
        } catch (Exception err) {

            //err.printStackTrace();
            dbdoc.sconto2 = 0;
        }

        ricalcolaTotali();
    }//GEN-LAST:event_texScon2KeyReleased

    private void texScon1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texScon1KeyReleased

        try {
            dbdoc.sconto1 = Db.getDouble(this.texScon1.getText());
        } catch (Exception err) {
            dbdoc.sconto1 = 0;
        }

        ricalcolaTotali();
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
    }//GEN-LAST:event_texScon1ActionPerformed

    private void formInternalFrameOpened(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_formInternalFrameOpened
        texNotePagamento.setMaximumSize(new Dimension(texNotePagamento.getSize()));
    }//GEN-LAST:event_formInternalFrameOpened

    private void formInternalFrameClosed(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_formInternalFrameClosed

        // Add your handling code here:
        if (tim != null) {
            tim.cancel();
        }
        if (zoom != null) {
            zoom.dispose();
        }

        InvoicexUtil.removeLock("test_fatt", id, this);

        main.getPadre().closeFrame(this);

//        lockableUI.stop();
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

        SwingUtils.mouse_wait(this);

        //modifico la riga
        String codiceListino = "1";
        Integer id_riga = CastUtils.toInteger(griglia.getValueAt(griglia.getSelectedRow(), griglia.getColumnByName("id")));
        Integer id_padre = this.id;

        if (texClie.getText().length() > 0) {
            try {
                codiceListino = Db.lookUp(texClie.getText(), "codice", "clie_forn").getString("codice_listino");
            } catch (Exception err) {
                err.printStackTrace();
            }
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
//            frmNuovRiga frm = new frmNuovRiga(this, this.dati.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNumePrev.getText()).intValue(), "P", Integer.valueOf(this.griglia.getValueAt(griglia.getSelectedRow(), 3).toString()).intValue(), Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente);
//            temp.openFrame(frm, 600, 350, 100, 100);
//            frm.setStato();
//        } else {
//            frmNuovRigaDescrizioneMultiRiga frm = new frmNuovRigaDescrizioneMultiRiga(this, this.dati.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNumePrev.getText()).intValue(), "P", Integer.valueOf(this.griglia.getValueAt(griglia.getSelectedRow(), 3).toString()).intValue(), Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente);
        try {
            JInternalFrame frm = null;
            int w = 650;
            int h = 400;
            int top = 100;
            int left = 100;
            if (main.getPersonalContain("frajor")) {
                if (!Sync.isActive()) {
                    frmNuovRigaDescrizioneMultiRigaNewFrajor temp_form = new frmNuovRigaDescrizioneMultiRigaNewFrajor(this, this.dati.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", Integer.valueOf(this.griglia.getValueAt(griglia.getSelectedRow(), 3).toString()).intValue(), Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre);
                    temp_form.setStato();
                    w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_w", "770"));
                    h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_h", "660"));
                    top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_top", "100"));
                    left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRigaFrajor_left", "100"));
                    frm = temp_form;
                } else {
                    SwingUtils.showErrorMessage(this, "SYNC: form non gestita");
                }
            } else {
                frmNuovRigaDescrizioneMultiRigaNew temp_form = new frmNuovRigaDescrizioneMultiRigaNew(this, this.dati.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", Integer.valueOf(this.griglia.getValueAt(griglia.getSelectedRow(), 3).toString()).intValue(), Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre, getNomeTabRighe());
                temp_form.setStato();
                w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_w", "980"));
                h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_h", "460"));
                top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_top", "100"));
                left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_left", "100"));
                frm = temp_form;
            }
            main.getPadre().openFrame(frm, w, h, top, left);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtils.mouse_def(this);
//        }
    }//GEN-LAST:event_popGrigModiActionPerformed

    private void butNuovArtiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butNuovArtiActionPerformed
        SwingUtils.mouse_wait(this);
        MicroBench mb = new MicroBench(true);
        String codiceListino = "";

        try {
            codiceListino = cu.toString(DbUtils.getObject(Db.getConn(), "select codice_listino from clie_forn where codice = " + Db.pc(texClie.getText(), "VARCHAR")));
        } catch (NullPointerException nerr) {
            System.err.println(nerr.toString());
        } catch (Exception err) {
//            err.printStackTrace();
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
                frmNuovRigaDescrizioneMultiRigaNew temp_form = new frmNuovRigaDescrizioneMultiRigaNew(this, tnxDbPanel.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", 0, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, null, this.id, getNomeTabRighe());
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
            SwingUtils.showErrorMessage(main.getPadre(), e.toString());
        }

        SwingUtils.mouse_def(this);
        mb.out("fine nuova riga");
//        }
    }//GEN-LAST:event_butNuovArtiActionPerformed

    private void butUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butUndoActionPerformed
        if (block_aggiornareProvvigioni) {
            return;
        }

        if (evt != null && !main.debug) {
            if (!SwingUtils.showYesNoMessage(main.getPadreWindow(), "Sicuro di annullare le modifiche ?")) {
                return;
            }
        }

        if (!main.edit_doc_in_temp) {
            lockableUI.setLocked(true);
            org.jdesktop.swingworker.SwingWorker worker = new org.jdesktop.swingworker.SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    annulla2();
                    return null;
                }

                @Override
                protected void done() {
                    lockableUI.setLocked(false);
                    dispose();
                }
            };
            worker.execute();
        } else {
            dispose();
        }
    }//GEN-LAST:event_butUndoActionPerformed

    public void annulla() {
        butUndoActionPerformed(null);
    }

    private void butSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveActionPerformed
        if (block_aggiornareProvvigioni) {
            return;
        }

        if (controlloCampi() == true) {
            try {
                InvoicexEvent event = new InvoicexEvent(this);
                event.type = InvoicexEvent.TYPE_FRMTESTFATT_PRIMA_DI_SAVE;
                main.events.fireInvoicexEvent(event);
            } catch (Exception err) {
                err.printStackTrace();
            }

            SwingUtils.mouse_wait(this);
            butSave.setEnabled(false);
            lockableUI.setLocked(true);

            saveDocumento();

            //scateno evento salvataggio nuova fattura
            if (dbStato.equalsIgnoreCase(this.DB_INSERIMENTO)) {
                try {
                    InvoicexEvent event = new InvoicexEvent(this);
                    event.type = InvoicexEvent.TYPE_SALVA_NUOVA_FATTURA;
                    event.serie = texSeri.getText();
                    event.numero = Integer.parseInt(texNume.getText());
                    event.anno = Integer.parseInt(texAnno.getText());
                    main.events.fireInvoicexEvent(event);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

            main.events.fireInvoicexEvent(new InvoicexEvent(this, InvoicexEvent.TYPE_SAVE));

            if (from != null) {
                this.from.dbRefresh();
            }
            if (chiudere) {
                SwingUtils.mouse_def(this);
                this.dispose();
            } else {
                //ci pensa allegati util a chiudere
                System.out.println("aspetto il salvataggio degli allegati");
            }
        }
    }//GEN-LAST:event_butSaveActionPerformed

    private void texDataFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texDataFocusGained
        old_anno = getAnnoDaForm();
        old_data = texData.getText();
        System.err.println("old_id = " + texNume.getText() + " da texDataFocusGained");
        old_id = texNume.getText();
        anno_modificato = false;
    }//GEN-LAST:event_texDataFocusGained

    private void texBancCabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texBancCabActionPerformed
        trovaCab();
}//GEN-LAST:event_texBancCabActionPerformed

    private void texBancCabFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texBancCabFocusLost
        trovaCab();
}//GEN-LAST:event_texBancCabFocusLost

private void butInserisciPesoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butInserisciPesoActionPerformed
    doc.setPrezziIvati(prezzi_ivati.isSelected());
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
        String sql = "select riga from righ_fatt";
//        sql += " where serie = " + Db.pc(dbSerie, "VARCHAR");
//        sql += " and numero = " + dbNumero;
//        sql += " and anno = " + dbAnno;
        sql += " where id_padre = " + this.id;
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
        }
    }

    sql = "insert into righ_fatt (serie, numero, anno, riga, codice_articolo, descrizione, id_padre) values (";
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

private void comClieDestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comClieDestActionPerformed
}//GEN-LAST:event_comClieDestActionPerformed

private void comClieFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieFocusGained
    comClieSel_old = comClie.getSelectedIndex();
}//GEN-LAST:event_comClieFocusGained

private void comClieDestFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comClieDestFocusGained
    comClieDest_old = comClieDest.getSelectedIndex();
}//GEN-LAST:event_comClieDestFocusGained

private void popGrigAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popGrigAddActionPerformed
    int numCol = this.griglia.getColumnByName("riga");
    int numRiga = this.griglia.getSelectedRow();
    int value = (Integer) this.griglia.getValueAt(numRiga, numCol);
    System.out.println("value = " + griglia.getValueAt(numRiga, griglia.getColumnByName("id")));
    Integer id_riga = cu.i(griglia.getValueAt(numRiga, griglia.getColumnByName("id")));
    Integer id_padre = cu.i(griglia.getValueAt(numRiga, griglia.getColumnByName("id_padre")));

    try {

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
//            frmNuovRiga frm = new frmNuovRiga(this, this.dati.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNumePrev.getText()).intValue(), "P", value, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente);
//            temp.openFrame(frm, 600, 350, 100, 100);
//            frm.setStato();
//        } else {
//            frmNuovRigaDescrizioneMultiRiga frm = new frmNuovRigaDescrizioneMultiRiga(this, this.dati.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNumePrev.getText()).intValue(), "P", value, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente);
        try {
            JInternalFrame frm = null;
            int w = 650;
            int h = 400;
            int top = 100;
            int left = 100;
            if (main.getPersonalContain("frajor")) {
                if (!Sync.isActive()) {
                    frmNuovRigaDescrizioneMultiRigaNewFrajor temp_form = new frmNuovRigaDescrizioneMultiRigaNewFrajor(this, tnxDbPanel.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", value, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre);
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
                frmNuovRigaDescrizioneMultiRigaNew temp_form = new frmNuovRigaDescrizioneMultiRigaNew(this, tnxDbPanel.DB_INSERIMENTO, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", value, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre, getNomeTabRighe());
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
//        }
    } catch (Exception e) {
        e.printStackTrace();
    }


}//GEN-LAST:event_popGrigAddActionPerformed

private void texScon3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texScon3ActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_texScon3ActionPerformed

private void formVetoableChange(java.beans.PropertyChangeEvent evt)throws java.beans.PropertyVetoException {//GEN-FIRST:event_formVetoableChange

    boolean tr = false;
    if (evt.getPropertyName().equals(IS_CLOSED_PROPERTY)) {
        boolean changed = ((Boolean) evt.getNewValue()).booleanValue();
        if (changed) {
            try {
                if (dati.dbCheckModificati()
                        || (!this.pagamentoInizialeGiorno.equals(this.texGiornoPagamento.getText())
                        || !this.pagamentoIniziale.equals(this.comPaga.getText())
                        || doc.getTotale_da_pagare_finale() != this.totaleDaPagareFinaleIniziale)) {
                    FxUtils.fadeBackground(butSave, Color.RED);
                    int confirm = JOptionPane.showOptionDialog(this,
                            "<html><b>Chiudi " + getTitle() + "?</b><br>Hai fatto delle modifiche e così verranno <b>perse</b> !<br>Per salvarle devi cliccare sul pulsante <b>Salva</b> in basso a sinistra<br>",
                            "Conferma chiusura",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, null, null);
                    if (confirm == 0) {
                    } else {
                        tr = true;
                    }
                }
            } catch (Exception e) {
            }

            if (tr) {
                throw new PropertyVetoException("Cancelled", null);
            } else {
//                if (dbStato.equalsIgnoreCase(this.DB_INSERIMENTO)) {
                butUndoActionPerformed(null);
//                }
            }
        }
    }

}//GEN-LAST:event_formVetoableChange

private void texCliePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_texCliePropertyChange
    System.out.println("texCliePropertyChange:" + evt.getPropertyName());
    if (evt.getPropertyName().equalsIgnoreCase("text")) {
        System.out.println("stop");
    }
}//GEN-LAST:event_texCliePropertyChange

private void texNumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texNumeActionPerformed
    // TODO add your handling code here:
}//GEN-LAST:event_texNumeActionPerformed

private void texForni1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_texForni1KeyPressed
    // TODO add your handling code here:
}//GEN-LAST:event_texForni1KeyPressed

private void texForniFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texForniFocusLost
    this.texForni1.setText(texForni.getText());
}//GEN-LAST:event_texForniFocusLost

private void comForniItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comForniItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
        this.texForni.setText(String.valueOf(comForni.getSelectedKey()));
        this.texForni1.setText(String.valueOf(comForni.getSelectedKey()));
    }
}//GEN-LAST:event_comForniItemStateChanged

private void grigliaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_grigliaMousePressed
    if (evt.isPopupTrigger()) {
        iu.impostaRigaSopraSotto(griglia, popGrigAdd, popGrig, evt);
    }
}//GEN-LAST:event_grigliaMousePressed

private void grigliaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_grigliaMouseReleased
    if (evt.isPopupTrigger()) {
        iu.impostaRigaSopraSotto(griglia, popGrigAdd, popGrig, evt);
    }
}//GEN-LAST:event_grigliaMouseReleased

private void foglioMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_foglioMousePressed
    if (evt.isPopupTrigger()) {
        popFoglio.show(foglio, evt.getX(), evt.getY());
    }
}//GEN-LAST:event_foglioMousePressed

private void foglioMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_foglioMouseReleased
    if (evt.isPopupTrigger()) {
        popFoglio.show(foglio, evt.getX(), evt.getY());
    }
}//GEN-LAST:event_foglioMouseReleased

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
            InvoicexUtil.importCSV(Db.TIPO_DOCUMENTO_FATTURA, f, serie, numero, anno, idPadre, nomeListino);
            InvoicexUtil.aggiornaTotaliRighe(Db.TIPO_DOCUMENTO_FATTURA, this.id, prezzi_ivati_virtual.isSelected());
            griglia.dbRefresh();
            ricalcolaTotali();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_butImportRigheActionPerformed

private void texProvvigioneFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texProvvigioneFocusLost
    if (!StringUtils.equals(texProvvigione.getName(), texProvvigione.getText()) && griglia.getRowCount() > 0) {
        aggiornareProvvigioni();
    }
}//GEN-LAST:event_texProvvigioneFocusLost

private void texProvvigioneFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texProvvigioneFocusGained
    texProvvigione.setName(texProvvigione.getText());
}//GEN-LAST:event_texProvvigioneFocusGained

private void apriclientiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apriclientiActionPerformed
//    alRicercaCliente.showHints();
    if (texCliente.getText().trim().length() == 0) {
        al_clifor.showHints();
        al_clifor.updateHints(null);
        al_clifor.showHints();
    } else {
        al_clifor.showHints();
    }
//    al_clifor.showHints();
}//GEN-LAST:event_apriclientiActionPerformed

private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown
}//GEN-LAST:event_formComponentShown

private void popDuplicaRigaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popDuplicaRigaActionPerformed
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

            int dbIdPadre = this.id;
            int dbId = Integer.parseInt(String.valueOf(griglia.getValueAt(sel, griglia.getColumnByName("id"))));

            //cerco ultimo numero ordine
            int newNumero = 1;
            sqlC = "";
            sqlV = "";
            sql = "SELECT MAX(riga) as maxnum FROM righ_fatt WHERE id_padre = " + Db.pc(dbIdPadre, Types.INTEGER);

            try {
                ResultSet tempUltimo = Db.openResultSet(sql);
                if (tempUltimo.next() == true) {
                    newNumero = tempUltimo.getInt("maxnum") + InvoicexUtil.getRigaInc();
                }
            } catch (Exception err) {
                err.printStackTrace();
            }

            sql = "select * from righ_fatt where id = " + Db.pc(dbId, Types.INTEGER);
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
                    sql = "insert into righ_fatt ";
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
}//GEN-LAST:event_popDuplicaRigaActionPerformed

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

    private void comValutaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_comValutaItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_comValutaItemStateChanged

    private void linkcodiceufficioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linkcodiceufficioActionPerformed
        try {
            SwingUtils.openUrl(new URL("http://www.fatturapa.gov.it/export/fatturazione/sdi/fatturapa/v1.1/Formato_FatturaPA_tabellare_V1.1.pdf"));
        } catch (Exception err) {
            SwingUtils.showExceptionMessage(this, err);
        }
    }//GEN-LAST:event_linkcodiceufficioActionPerformed

    private void texNotePagamentoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texNotePagamentoFocusLost
        aggiornaPaIban();
    }//GEN-LAST:event_texNotePagamentoFocusLost

    private void texGiornoPagamentoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_texGiornoPagamentoFocusLost

    }//GEN-LAST:event_texGiornoPagamentoFocusLost

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        aggiornaPaIban();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void linkcodiceufficio1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linkcodiceufficio1ActionPerformed
        try {
            SwingUtils.openUrl(new URL("http://www.agenziaentrate.gov.it/wps/file/Nsilib/Nsi/Home/CosaDeviFare/Dichiarare/DichiarazioniSostitutiImposta/770S2014/Modello+770+2014+Semp/770+Semplificato+2014+istruzioni/770Semplificato_2014_istr_N.pdf"));
        } catch (Exception err) {
            SwingUtils.showExceptionMessage(this, err);
        }
    }//GEN-LAST:event_linkcodiceufficio1ActionPerformed

    private void dg_dr_tipo_ritenutaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dg_dr_tipo_ritenutaItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dr_tipo_ritenutaItemStateChanged

    private void dg_dr_tipo_ritenutaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dg_dr_tipo_ritenutaFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dr_tipo_ritenutaFocusLost

    private void dg_dr_causale_pagamentoItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dg_dr_causale_pagamentoItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dr_causale_pagamentoItemStateChanged

    private void dg_dr_causale_pagamentoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dg_dr_causale_pagamentoFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dr_causale_pagamentoFocusLost

    private void dg_dcp_tipo_cassaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dg_dcp_tipo_cassaFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dcp_tipo_cassaFocusLost

    private void dg_dcp_tipo_cassaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dg_dcp_tipo_cassaItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dcp_tipo_cassaItemStateChanged

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

    private void dg_dr_totale_da_esportareItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dg_dr_totale_da_esportareItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dr_totale_da_esportareItemStateChanged

    private void dg_dr_totale_da_esportareFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dg_dr_totale_da_esportareFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_dg_dr_totale_da_esportareFocusLost

    private void split_paymentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_split_paymentActionPerformed
        String come = split_payment.isSelected() ? "'S'" : "null";
        String sql = "update test_fatt_xmlpa set split_payment = " + come + " where id_fattura = " + id;
        try {
            dbu.tryExecQuery(Db.getConn(), sql);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (split_payment.isSelected()) {
            double bollo = cu.d0(bollo_importo.getText());
            if (bollo != 0) {
                bollo_importo.setText("");
                Point p = null;
                try {
                    p = getLocationOnScreen();
                    p.translate(getWidth() / 3, getHeight() / 2);
                } catch (Exception e) {
                }
                if (p == null) {
                    p = main.getPadreFrame().getLocation();
                    p.translate(main.getPadreFrame().getWidth() / 3, main.getPadreFrame().getHeight() / 2);
                }
                SwingUtils.showFlashMessage2("Ho azzerato l'importo del bollo in quanto nell'export della fattura elettronica verrà automaticamente impostato 2 €", 3, p, Color.red);
            }
        }

        ricalcolaTotali();
    }//GEN-LAST:event_split_paymentActionPerformed

    private void butSalvaFoglioRigheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSalvaFoglioRigheActionPerformed
        if (block_aggiornareProvvigioni) {
            return;
        }

        this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            InvoicexEvent event = new InvoicexEvent(this);
            event.type = InvoicexEvent.TYPE_FRMTESTFATT_PRIMA_DI_SAVE;
            main.events.fireInvoicexEvent(event);
        } catch (Exception err) {
            err.printStackTrace();
        }
        saveDocumento(false);

        //scateno evento salvataggio nuova fattura
        if (dbStato.equalsIgnoreCase(this.DB_INSERIMENTO)) {
            try {
                InvoicexEvent event = new InvoicexEvent(this);
                event.type = InvoicexEvent.TYPE_SALVA_NUOVA_FATTURA;
                event.serie = texSeri.getText();
                event.numero = Integer.parseInt(texNume.getText());
                event.anno = Integer.parseInt(texAnno.getText());
                main.events.fireInvoicexEvent(event);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        main.events.fireInvoicexEvent(new InvoicexEvent(this, InvoicexEvent.TYPE_SAVE));
        if (from != null) {
            this.from.dbRefresh();
        }
        //rinizializzo le cose che iniz all'inizio
        String nuova_serie = texSeri.getText();
        Integer nuovo_numero = cu.toInteger(texNume.getText());
        Integer nuovo_anno = cu.toInteger(texAnno.getText());

        //aggiorno le righe temp
        sql = "update righ_fatt_temp";
        sql += " set serie = '" + nuova_serie + "'";
        sql += " , numero = " + nuovo_numero + "";
        sql += " , anno = " + nuovo_anno + "";
        sql += " where serie = " + db.pc(serie_originale, "VARCHAR");
        sql += " and numero = " + numero_originale;
        sql += " and anno = " + anno_originale;
        Db.executeSqlDialogExc(sql, true);
        
        serie_originale = texSeri.getText();
        numero_originale = cu.toInteger(texNume.getText());
        anno_originale = cu.toInteger(texAnno.getText());

        totaleDaPagareFinaleIniziale = doc.getTotale_da_pagare_finale();
        pagamentoIniziale = comPaga.getText();
        pagamentoInizialeGiorno = texGiornoPagamento.getText();
        provvigioniInizialeScadenze = dumpScadenze();
        provvigioniIniziale = Db.getDouble(texProvvigione.getText());
        codiceAgenteIniziale = it.tnx.Util.getInt(comAgente.getSelectedKey().toString());

        //una volta salvato e stampato entro in modalitaà modifica
        if (dbStato.equals(frmTestFatt.DB_INSERIMENTO)) {
            dbStato = frmTestFatt.DB_MODIFICA;
            //e riporto le righe in _temp
            sql = "insert into righ_fatt_temp";
            sql += " select *, '" + main.login + "' as username";
            sql += " from righ_fatt";
            sql += " where id_padre = " + dbdoc.getId();
            try {
                DbUtils.tryExecQuery(Db.getConn(), sql);
                System.out.println("sql ok:" + sql);
            } catch (Exception e) {
                System.err.println("sql errore:" + sql);
                e.printStackTrace();
                trow = e;
            }
        } else {
            porto_in_temp();
        }
        dati.dbCheckModificatiReset();

        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_butSalvaFoglioRigheActionPerformed

    private void popFoglioModificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popFoglioModificaActionPerformed
        //modifico la riga
        String codiceListino = "1";
        Integer id_riga = cu.i(foglio.getValueAt(foglio.getSelectedRow(), 10));
        Integer riga = cu.i(foglio.getValueAt(foglio.getSelectedRow(), 0));
        Integer id_padre = this.id;

        if (texClie.getText().length() > 0) {
            try {
                codiceListino = Db.lookUp(texClie.getText(), "codice", "clie_forn").getString("codice_listino");
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        int codiceCliente = -1;
        if (this.texClie.getText().length() > 0) {
            try {
                codiceCliente = Integer.parseInt(texClie.getText());
            } catch (Exception err) {
                err.printStackTrace();
            }
        }

        boolean multiriga = main.fileIni.getValueBoolean("pref", "multiriga", false);
        try {
            JInternalFrame frm = null;
            int w = 650;
            int h = 400;
            int top = 100;
            int left = 100;
            frmNuovRigaDescrizioneMultiRigaNew temp_form = new frmNuovRigaDescrizioneMultiRigaNew(this, this.dati.DB_MODIFICA, this.texSeri.getText(), Integer.valueOf(this.texNume.getText()).intValue(), "P", riga, Integer.valueOf(this.texAnno.getText()).intValue(), codiceListino, codiceCliente, id_riga, id_padre, getNomeTabRighe());
            temp_form.setStato();
            w = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_w", "980"));
            h = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_h", "460"));
            top = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_top", "100"));
            left = Integer.parseInt(main.fileIni.getValue("dimensioni", "frmNuovRigaDescrizioneMultiRiga_left", "100"));
            frm = temp_form;
            main.getPadre().openFrame(frm, w, h, top, left);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtils.mouse_def(this);
    }//GEN-LAST:event_popFoglioModificaActionPerformed

    private void bollo_importoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_bollo_importoKeyReleased
        impostaSpeseBollo();
    }//GEN-LAST:event_bollo_importoKeyReleased

    private void bollo_importoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bollo_importoFocusLost
        ricalcolaTotali();
    }//GEN-LAST:event_bollo_importoFocusLost

    private void bollo_importoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bollo_importoFocusGained

        //visualizzo tooltip
//        balloon_bollo.setVisible(true);
        TimingUtils.showTimedBalloon(balloon_bollo, 5000);

    }//GEN-LAST:event_bollo_importoFocusGained

    private void bollo_si_noActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bollo_si_noActionPerformed
        if (!bollo_si_no.isSelected()) {
            old_bollo = bollo_importo.getText();
            bollo_importo.setText("");
            bollo_importo.setEnabled(false);
        } else {
            bollo_importo.setText(old_bollo);
            bollo_importo.setEnabled(true);
        }
        impostaSpeseBollo();
    }//GEN-LAST:event_bollo_si_noActionPerformed

    private void texClienteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_texClienteMouseClicked

    }//GEN-LAST:event_texClienteMouseClicked

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

    private void comCampoLibero1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comCampoLibero1FocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_comCampoLibero1FocusGained

    private void comCampoLibero1PopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_comCampoLibero1PopupMenuWillBecomeVisible
        if (comCampoLibero1.getItemCount() == 0) {
            InvoicexUtil.caricaComboTestateCampoLibero1(comCampoLibero1);
        }
    }//GEN-LAST:event_comCampoLibero1PopupMenuWillBecomeVisible

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        dati.setSize(this.getWidth(), dati.getHeight());
        scrollDati.getViewport().setSize(this.getWidth(), dati.getHeight());

        System.out.println("panSalva.getHeight() = " + panSalva.getHeight());
        if (panSalva.getHeight() < 150) {
            panSalva.setSize(panSalva.getWidth(), 150);
        }
        jPanel5.validate();

        dati.validate();
    }//GEN-LAST:event_formComponentResized

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
    private tnxbeans.tnxTextField bollo_importo;
    private tnxbeans.tnxCheckBox bollo_si_no;
    private javax.swing.JButton butAddClie;
    private javax.swing.JButton butCoor;
    private javax.swing.JButton butImportRighe;
    private javax.swing.JButton butImportXlsCirri;
    private javax.swing.JButton butInserisciPeso;
    public javax.swing.JButton butNuovArti;
    private javax.swing.JButton butPdf;
    private javax.swing.JButton butPrezziPrec;
    private javax.swing.JButton butSalvaFoglioRighe;
    private javax.swing.JButton butSave;
    private javax.swing.JButton butScad;
    private javax.swing.JButton butStampa;
    public javax.swing.JButton butUndo;
    private tnxbeans.tnxCheckBox cheOpzioneRibaDestDiversa;
    private tnxbeans.tnxComboField comAgente;
    private tnxbeans.tnxComboField comAspettoEsterioreBeni;
    private tnxbeans.tnxComboField comCampoLibero1;
    private tnxbeans.tnxComboField comCausaleTrasporto;
    public tnxbeans.tnxComboField comClie;
    private tnxbeans.tnxComboField comClieDest;
    private tnxbeans.tnxComboField comConsegna;
    private tnxbeans.tnxComboField comForni;
    private tnxbeans.tnxComboField comMezzoTrasporto;
    private tnxbeans.tnxComboField comPaese;
    private tnxbeans.tnxComboField comPaga;
    private tnxbeans.tnxComboField comPorto;
    private tnxbeans.tnxComboField comScarico;
    private tnxbeans.tnxComboField comValuta;
    private tnxbeans.tnxComboField comVettori;
    public tnxbeans.tnxDbPanel dati;
    public tnxbeans.tnxDbPanel datiAltro;
    public tnxbeans.tnxDbPanel datiPa;
    private tnxbeans.tnxDbPanel datiRighe;
    private tnxbeans.tnxDbPanel dati_altri2;
    private tnxbeans.tnxTextField dg_causale;
    private tnxbeans.tnxTextField dg_dc_codicecig;
    private tnxbeans.tnxTextField dg_dc_codicecommessaconvenzione;
    private tnxbeans.tnxTextField dg_dc_codicecup;
    private tnxbeans.tnxTextField dg_dc_data;
    private tnxbeans.tnxTextField dg_dc_iddocumento;
    private tnxbeans.tnxTextField dg_dc_numitem;
    private tnxbeans.tnxTextField dg_dc_riferimentonumerolinea;
    private tnxbeans.tnxComboField dg_dcp_tipo_cassa;
    private tnxbeans.tnxTextField dg_doa_codicecig;
    private tnxbeans.tnxTextField dg_doa_codicecommessaconvenzione;
    private tnxbeans.tnxTextField dg_doa_codicecup;
    private tnxbeans.tnxTextField dg_doa_data;
    private tnxbeans.tnxTextField dg_doa_iddocumento;
    private tnxbeans.tnxTextField dg_doa_numitem;
    private tnxbeans.tnxTextField dg_doa_riferimentonumerolinea;
    private tnxbeans.tnxComboField dg_dr_causale_pagamento;
    private tnxbeans.tnxComboField dg_dr_tipo_ritenuta;
    private tnxbeans.tnxComboField dg_dr_totale_da_esportare;
    private tnxbeans.tnxTextField dp_banca;
    private tnxbeans.tnxTextField dp_iban;
    private javax.swing.JLabel dp_iban_lab;
    public it.tnx.gui.JTableSs foglio;
    private javax.swing.JTable foglio3;
    public tnxbeans.tnxDbGrid griglia;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel113;
    private javax.swing.JLabel jLabel114;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel151;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    public javax.swing.JPanel jPanel4;
    public javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JLabel labAgente;
    private javax.swing.JLabel labBancAbi;
    private javax.swing.JLabel labBancCab;
    private javax.swing.JLabel labCampoLibero1;
    private javax.swing.JLabel labFa1;
    private javax.swing.JLabel labFa2;
    private javax.swing.JLabel labFa3;
    private javax.swing.JLabel labFa4;
    private javax.swing.JLabel labFa5;
    private javax.swing.JLabel labFa6;
    private javax.swing.JLabel labFa7;
    private javax.swing.JLabel labFaTitolo;
    private javax.swing.JLabel labGiornoPagamento;
    private javax.swing.JLabel labMarcaBollo;
    private javax.swing.JLabel labMarcaBollo1;
    private javax.swing.JLabel labModConsegna;
    private javax.swing.JLabel labModScarico;
    private javax.swing.JLabel labNoteConsegna;
    private javax.swing.JLabel labPercentoProvvigione;
    private javax.swing.JLabel labPesoLordo;
    private javax.swing.JLabel labPesoNetto;
    private javax.swing.JLabel labProvvigione;
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
    public javax.swing.JLabel labStatus;
    private javax.swing.JLabel labvaluta;
    private org.jdesktop.swingx.JXHyperlink linkcodiceufficio;
    private org.jdesktop.swingx.JXHyperlink linkcodiceufficio1;
    private javax.swing.JLabel lregfis;
    private javax.swing.JLabel lregfis1;
    private javax.swing.JLabel lregfis2;
    private javax.swing.JLabel lregfis3;
    private javax.swing.JMenuItem menClienteModifica;
    private javax.swing.JMenuItem menClienteNuovo;
    private javax.swing.JPopupMenu menClientePopup;
    private javax.swing.JMenu menColAgg;
    private javax.swing.JCheckBoxMenuItem menColAggNote;
    private javax.swing.JPanel panDati;
    private javax.swing.JPanel panFoglioRighe;
    private javax.swing.JPanel panGriglia;
    public javax.swing.JPanel panRitenute;
    private javax.swing.JPanel panSalva;
    private javax.swing.JPanel panTotale;
    public javax.swing.JPanel pan_segnaposto_deposito;
    private javax.swing.JMenuItem popDuplicaRiga;
    private javax.swing.JPopupMenu popFoglio;
    private javax.swing.JMenuItem popFoglioElimina;
    private javax.swing.JMenuItem popFoglioModifica;
    private javax.swing.JPopupMenu popGrig;
    private javax.swing.JMenuItem popGrigAdd;
    private javax.swing.JMenuItem popGrigElim;
    private javax.swing.JMenuItem popGrigModi;
    public tnxbeans.tnxCheckBox prezzi_ivati;
    private javax.swing.JCheckBox prezzi_ivati_virtual;
    private javax.swing.JScrollPane scrollDati;
    private javax.swing.JScrollPane scrollDatiPa;
    private javax.swing.JSeparator sepDestMerce;
    private javax.swing.JSeparator sepFaSeparatore;
    private javax.swing.JSeparator sepFattAcc;
    private javax.swing.JSplitPane split;
    public tnxbeans.tnxCheckBox split_payment;
    public javax.swing.JTabbedPane tabDocumento;
    public tnxbeans.tnxTextField texAcconto;
    private tnxbeans.tnxTextField texAnno;
    private tnxbeans.tnxTextField texBancAbi;
    private tnxbeans.tnxTextField texBancCab;
    private tnxbeans.tnxTextField texBancIban;
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
    private tnxbeans.tnxTextField texForni;
    private tnxbeans.tnxTextField texForni1;
    public tnxbeans.tnxTextField texGiornoPagamento;
    private tnxbeans.tnxMemoField texNote;
    private tnxbeans.tnxMemoField texNoteConsegna;
    private tnxbeans.tnxTextField texNotePagamento;
    private tnxbeans.tnxTextField texNume;
    private tnxbeans.tnxTextField texNumeroColli;
    private tnxbeans.tnxTextField texPaga2;
    private tnxbeans.tnxTextField texPesoLordo;
    private tnxbeans.tnxTextField texPesoNetto;
    private tnxbeans.tnxTextField texProvvigione;
    public tnxbeans.tnxTextField texRitenuta;
    public tnxbeans.tnxTextField texRivalsa;
    public tnxbeans.tnxTextField texScon1;
    public tnxbeans.tnxTextField texScon2;
    public tnxbeans.tnxTextField texScon3;
    public tnxbeans.tnxTextField texSconto;
    private tnxbeans.tnxTextField texSeri;
    public tnxbeans.tnxTextField texSpeseIncasso;
    public tnxbeans.tnxTextField texSpeseTrasporto;
    private tnxbeans.tnxTextField texTipoFattura;
    public tnxbeans.tnxTextField texTota;
    private tnxbeans.tnxTextField texTota1;
    public tnxbeans.tnxTextField texTotaDaPagare;
    public tnxbeans.tnxTextField texTotaDaPagareFinale;
    public tnxbeans.tnxTextField texTotaImpo;
    private tnxbeans.tnxTextField texTotaImpo1;
    public tnxbeans.tnxTextField texTotaIva;
    private tnxbeans.tnxTextField texTotaIva1;
    public tnxbeans.tnxTextField texTotaRitenuta;
    private javax.swing.JPanel tutto;
    // End of variables declaration//GEN-END:variables

    void dbAssociaGrigliaRighe() {
        
        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            menColAggNote.setSelected(true);
        }
        
        //!!! ATTENZIONE a cambiare i nomi dei campi, il caricamento del foglio righe potrebbe non funzionare !!!

        String campi = "serie,";
        campi += "numero,";
        campi += "anno,";
        campi += "riga,";
        campi += "stato,";
        campi += "codice_articolo as articolo,";
        campi += "descrizione,";
        campi += "um,";

        campi += "quantita,";

        campi += "prezzo, ";
        campi += "sconto1 as Sconti, ";
        campi += "sconto2, ";
        campi += "(totale_imponibile) as Totale ";
        campi += ", iva ";
        campi += ", (totale_ivato) as Ivato ";

        campi += ", r.id";
        campi += ", provvigione";

        if (main.isPluginContabilitaAttivo()) {
            campi += ", conto";
        }
        
        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            campi += ", n.note";
        }        
       
//        String sql = "select " + campi + " from righ_fatt" + " where serie = " + db.pc(this.prev.serie, "VARCHAR") + " and numero = " + this.prev.numero + " and anno = " + db.pc(this.prev.anno, "INTEGER");
        String sql = "select " + campi + " from " + getNomeTabRighe() + " r";
        
        if (main.fileIni.getValueBoolean("pref", "ColAgg_righe_note", false)) {
            sql += " left join note n on n.tabella = '" + getNomeTabRighe() + "' and n.id_tab = r.id";
        }        

        sql += " where r.id_padre = " + id;    //per non selezionare li scontrini!!                
        sql += " order by r.riga";

        //debug
        //javax.swing.JOptionPane.showMessageDialog(null,sql);
        System.out.println("sql associa griglia:" + sql);
        this.sqlGriglia = sql;
//        griglia.setNoTnxResize(true);
        System.err.println("this visible " + this.isVisible());
        griglia.dbOpen(db.getConn(), sql);
        griglia.getColumn("quantita").setCellRenderer(InvoicexUtil.getNumber0_5Renderer());

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

    }

    public void recuperaDatiFornitore() {

        //li recupero dal cliente
        ResultSet tempForni;
        String sql = "select * from clie_forn";
        sql += " where codice = " + Db.pc(this.texForni.getText(), "NUMERIC");
        tempForni = Db.openResultSet(sql);

        try {

            if (tempForni.next() == true) {
                int codice = tempForni.getInt("codice");

                boolean continua = true;
                for (int i = 0; i < comForni.getItemCount() && continua; i++) {
                    int codice_sel = (Integer) comForni.getKey(i);

                    if (codice_sel == codice) {
                        comForni.setSelectedIndex(i);
                        continua = false;
                    }
                }
                if (continua) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Il codice cliente specificato non esiste in anagrafica !");
                }
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "Il codice cliente specificato non esiste in anagrafica !");
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
    
    public String getNomeTabRighe() {
        if (table_righe_temp != null) {
            return table_righe_temp;
        }
        return "righ_fatt" + suff;
    }
    
    public String getNomeTabRigheLotti() {
        if (table_righe_lotti_temp != null) {
            return table_righe_lotti_temp;
        }
        return "righ_fatt" + suff + "_lotti";
    }

    public String getNomeTabRigheMatricole() {
        if (table_righe_matricole_temp != null) {
            return table_righe_matricole_temp;
        }
        return "righ_fatt" + suff + "_matricole";
    }

    

    public void recuperaDatiCliente() {

        try {
            if (this.texClie.getText().length() > 0) {
                this.dbdoc.forceCliente(Long.parseLong(this.texClie.getText()));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        //li recupero dal cliente
        ResultSet tempClie;
        String sql = "select * from clie_forn";
        sql += " where codice = " + Db.pc(this.texClie.getText(), "NUMERIC");
        tempClie = Db.openResultSet(sql);

        try {

            if (tempClie.next() == true) {

                //tecnospurghi preferisce che rimanga con il codice fra parentesi quadre dopo la selezione
//                texCliente.setText(cu.s(tempClie.getString("ragione_sociale")));
                //this.texPaga.setText(Db.nz(tempClie.getString("pagamento"),""));
                //if (Db.nz(tempClie.getString("pagamento"),"").length() > 0) this.comPaga.setText(Db.nz(tempClie.getString("pagamento"),""));
                if (Db.nz(tempClie.getString("pagamento"), "").length() > 0) {
                    this.comPaga.dbTrovaRiga(Db.nz(tempClie.getString("pagamento"), ""));

                }
                comPagaFocusLost(null);

//                if (Db.nz(tempClie.getString("banca_abi"), "").length() > 0) {
                this.texBancAbi.setText(Db.nz(tempClie.getString("banca_abi"), ""));
//                }
//                if (Db.nz(tempClie.getString("banca_cab"), "").length() > 0) {
                this.texBancCab.setText(Db.nz(tempClie.getString("banca_cab"), ""));
//                }
//                if (Db.nz(tempClie.getString("banca_cc_iban"), "").length() > 0) {
                this.texBancIban.setText(Db.nz(tempClie.getString("banca_cc_iban"), ""));
//                }

                //if (Db.nz(tempClie.getString("banca_cc"),"").length() > 0) this.texBancCC.setText(Db.nz(tempClie.getString("banca_cc"),""));
                //cerca lengthdescrizioni
                texBancAbiActionPerformed(null);
                trovaCab();

                //opzione dest diversa riba
                if (tempClie.getString("opzione_riba_dest_diversa") != null && tempClie.getString("opzione_riba_dest_diversa").equalsIgnoreCase("S")) {
                    this.cheOpzioneRibaDestDiversa.setSelected(true);
                } else {
                    this.cheOpzioneRibaDestDiversa.setSelected(false);
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

                InvoicexUtil.fireEvent(this, InvoicexEvent.TYPE_FRMTESTFATT_CARICA_DATI_CLIENTE, tempClie);
            } else {
                //javax.swing.JOptionPane.showMessageDialog(this,"Il codice cliente specificato non esiste in anagrafica !");
                //spostato il controllo su controllaCampi
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private void aggiornaNote() {
        if (in_apertura) {
            return;
        }

        //note automatiche e altro
        String sqlTmp = "SELECT note_fatt, modalita_consegna, modalita_scarico, note_consegna, split_payment FROM clie_forn where codice = " + Db.pc(String.valueOf(comClie.getSelectedKey()), "NUMERIC");
        ResultSet res = Db.openResultSet(sqlTmp);
        try {
            if (res.next()) {
                String key_standard = "noteStandard";
                String key_cliente = "note_fatt";

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

                try {
                    split_payment.setSelected(cu.toBoolean(res.getString("split_payment")));
                    split_paymentActionPerformed(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void trovaAbi() {
        caricaBanca();
    }

    private void trovaCab() {
        try {
            String sql = "";
            sql += "select banche_cab.cap,";
            sql += " banche_cab.indirizzo,";
            sql += " comuni.comune,";
            sql += " comuni.provincia";
            sql += " from banche_cab left join comuni on banche_cab.codice_comune = comuni.codice";
            sql += " where banche_cab.abi = " + Db.pc(this.texBancAbi.getText(), "VARCHAR");
            sql += " and banche_cab.cab = " + Db.pc(this.texBancCab.getText(), "VARCHAR");
            ResultSet temp = Db.openResultSet(sql);
            if (temp.next()) {
                String descCab = Db.nz(temp.getString(1), "") + " " + Db.nz(temp.getString(2), "") + ", " + Db.nz(temp.getString(3), "") + " (" + Db.nz(temp.getString(4), "") + ")";
                descCab = StringUtils.abbreviate(descCab, CoordinateBancarie.maxCab);
                this.labBancCab.setText(descCab);
            } else {
                this.labBancCab.setText("");
            }
        } catch (Exception err) {
            this.labBancCab.setText("");
        }
    }

    private boolean controlloCampi() {
        
        //controllo data
        if (!ju.isValidDateIta(texData.getText())) {
            texData.requestFocus();
            javax.swing.JOptionPane.showMessageDialog(this, "Data del documento non valida");
            return false;
        }
        

        //controllo cliente
        //li recupero dal cliente
        ResultSet tempClie;
        String sql = "select * from clie_forn";
        sql += " where codice = " + Db.pc(this.texClie.getText(), "NUMERIC");
        tempClie = Db.openResultSet(sql);

        if (dbdoc.totale < 0) {
            int res = javax.swing.JOptionPane.showConfirmDialog(this, "Il totale risulta negativo.\nSolitamente devono essere in positivo.\nVuoi continuare comunque?", "Conferma Dati", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.NO_OPTION) {
                return false;
            }
        }
        try {

            if (tempClie.next() != true) {
                tabDocumento.setSelectedIndex(0);
                texCliente.requestFocus();
                javax.swing.JOptionPane.showMessageDialog(this, "Il codice cliente specificato non esiste in anagrafica !");
                return false;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        //controllo pagamento
        ResultSet temp;
        boolean ok = true;
        boolean flagCoordinate = false;
        sql = "select * from pagamenti where codice = " + Db.pc(this.comPaga.getSelectedKey().toString(), "VARCHAR");
        temp = Db.openResultSet(sql);

        try {

            if (temp.next() == true && comPaga.getSelectedKey().toString().length() > 0) {

                if (temp.getString("coordinate_necessarie").equalsIgnoreCase("S")) {

                    //servono lengthcoordinate, cotnrollare che ci siano i 3 ccampi della banca
                    if (this.texBancAbi.getText().length() == 0 || this.texBancCab.getText().length() == 0) {
                        flagCoordinate = true;
                        ok = false;
                    }
                }
            } else {
                tabDocumento.setSelectedIndex(0);
                comPaga.requestFocus();
                javax.swing.JOptionPane.showMessageDialog(this, "Manca il tipo di pagamento (e' obbligatorio)", "Attenzione", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                ok = false;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        if (ok == false && flagCoordinate == true) {
            //uscire per mancanza coordinate
            int ret = javax.swing.JOptionPane.showConfirmDialog(this, "Mancano le coordinate bancarie per il tipo di pagamento scelto\nContinuare ugualmente?", "Attenzione", javax.swing.JOptionPane.YES_NO_OPTION);
            if (ret != javax.swing.JOptionPane.YES_OPTION) {
                return false;
            }
        } else if (ok == false) {
            return false;
        }

        //controllo tipo pagamento
        if (!InvoicexUtil.controlloGiornoPagamento(String.valueOf(comPaga.getSelectedKey()), texGiornoPagamento.getText(), this)) {
            return false;
        }

        return true;
    }

    private void showPrezziFatture() {
        try {
            frmPrezziFatturePrecedenti form = new frmPrezziFatturePrecedenti(Integer.parseInt(this.texClie.getText().toString()), null, Db.TIPO_DOCUMENTO_FATTURA);
            main.getPadre().openFrame(form, 450, 500, this.getY() + 50, this.getX() + this.getWidth() - 200);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getSerie() {

        return this.texSeri.getText();
    }

    public String getNumero() {

        return this.texNume.getText();
    }

    public String getAnno() {
        return this.texAnno.getText();
    }

    public void ricalcolaTotali() {
        if (loadingFoglio) {
            return;
        }

        try {

            //this.parent.prev.dbRefresh();
            //provo con nuova classe Documento
            if (texClie.getText() != null && texClie.getText().length() > 0) {
                try {
                    doc.setCodiceCliente(Long.parseLong(texClie.getText()));
                } catch (NumberFormatException ex0) {
                    return;
                }
            }

            doc.setAcconto(Db.getDouble(texAcconto.getText()));
            System.out.println("doc.getAcconto():" + doc.getAcconto());
            doc.setScontoTestata1(Db.getDouble(texScon1.getText()));
            doc.setScontoTestata2(Db.getDouble(texScon2.getText()));
            doc.setScontoTestata3(Db.getDouble(texScon3.getText()));
            doc.setSpeseIncasso(Db.getDouble(texSpeseIncasso.getText()));
            doc.setSpeseTrasporto(Db.getDouble(texSpeseTrasporto.getText()));
            doc.setRitenuta(0);
            try {
                InvoicexEvent event = new InvoicexEvent(this);
                event.type = InvoicexEvent.TYPE_FRMTESTFATT_RICALCOLA_TOTALI_1;
                main.events.fireInvoicexEvent(event);
            } catch (Exception err) {
                err.printStackTrace();
            }
            doc.setPrezziIvati(prezzi_ivati.isSelected());
            doc.setSconto(Db.getDouble(texSconto.getText()));

            doc.speseBolloSiNo = bollo_si_no.isSelected();
            doc.speseBollo = Db.getDouble(bollo_importo.getText());

            doc.setData(null);
            try {
                SimpleDateFormat datef = null;
                if (texData.getText().length() == 8 || texData.getText().length() == 7) {
                    datef = new SimpleDateFormat("dd/MM/yy");
                } else if (texData.getText().length() == 10 || texData.getText().length() == 9) {
                    datef = new SimpleDateFormat("dd/MM/yyyy");
                }
                if (datef != null) {
                    Calendar cal = Calendar.getInstance();
                    datef.setLenient(true);
                    cal.setTime(datef.parse(texData.getText()));
                    doc.setData(new java.sql.Date(cal.getTime().getTime()));
                }
            } catch (Exception err) {
                System.out.println("err:" + err);
            }

            doc.calcolaTotali();
            texTota.setText(it.tnx.Util.formatValutaEuro(doc.getTotale()));
            texTotaImpo.setText(it.tnx.Util.formatValutaEuro(doc.getTotaleImponibile()));
            texTotaIva.setText(it.tnx.Util.formatValutaEuro(doc.getTotaleIva()));
            texTotaDaPagareFinale.setText(it.tnx.Util.formatValutaEuro(doc.getTotale_da_pagare_finale()));

            //presconto.setText(it.tnx.Util.formatValutaEuro(doc.totaleImponibilePreSconto));
            //prescontoivato.setText(it.tnx.Util.formatValutaEuro(doc.totaleIvatoPreSconto));
            //test rivalsa inps
            Component comp = SwingUtils.getCompByName(jPanel4, "texRivalsaPerc");
            if (comp != null) {
//                labRivInps.setText("rivvvvvv");
                System.out.println("comp:" + comp);
            }

            try {
                InvoicexEvent event = new InvoicexEvent(this);
                event.type = InvoicexEvent.TYPE_FRMTESTFATT_RICALCOLA_TOTALI_2;
                main.events.fireInvoicexEvent(event);
            } catch (Exception err) {
                err.printStackTrace();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        //calcolo totale numero colli 
        if (!in_apertura) {
            InvoicexUtil.calcolaColli(griglia, texNumeroColli);
        }

    }

    private void annulla2() {
        // Add your handling code here:
        if (dbStato.equals(DB_INSERIMENTO)) {
            //elimino la testata inserita e poi annullata
            String sql = "delete from test_fatt";
            sql += " where id = " + this.id;
            Db.executeSql(sql);
            sql = "delete from righ_fatt";
            sql += " where id_padre = " + this.id;
            Db.executeSql(sql);
        } else if (dbStato.equals(frmTestFatt.DB_MODIFICA)) {
            System.out.println("annulla da modifica, elimino " + dbdoc.serie + "/" + dbdoc.numero + "/" + dbdoc.anno + " e rimetto da temp " + serie_originale + "/" + numero_originale + "/" + anno_originale);

            //rimetto numero originale
            sql = "update test_fatt";
            sql += " set numero = " + Db.pc(numero_originale, "NUMBER");
            sql += " , anno = " + Db.pc(anno_originale, "NUMBER");
            sql += " where id = " + this.id;
            Db.executeSql(sql);

            //elimino le righe inserite
            sql = "delete from righ_fatt";
            sql += " where id_padre = " + this.id;
            Db.executeSql(sql);

            //e rimetto quelle da temp
            /* ATTENZIONE, NON RIMETTERE COME QUI SOTTO, OVVERO SENZA GLI ID ALTRIMENTI SI PERDE IL COLLEGAMENTO CON LE INFO SU LOTTI E MATRICOLE */
//            sql = "insert into righ_fatt (" + Db.getFieldList("righ_fatt", false, Arrays.asList("id")) + ")";
//            sql += " select " + Db.getFieldList("righ_fatt_temp", true, Arrays.asList("id"));
            sql = "insert into righ_fatt (" + Db.getFieldList("righ_fatt", false) + ")";
            sql += " select " + Db.getFieldList("righ_fatt_temp", true);
            sql += " from righ_fatt_temp";
            sql += " where id_padre = " + this.id;
            sql += " and username = '" + main.login + "'";
            Db.executeSqlDialogExc(sql, true);

//            //vado ad aggiornare eventuali ddt o ordini legati
//            sql = "update test_ddt";
//            sql += " set fattura_numero = " + Db.pc(numero_originale, "NUMBER");
//            sql += " where fattura_serie " + Db.pcW(this.prev.serie, "VARCHAR");
//            sql += " and fattura_numero " + Db.pcW(prev.numero, "NUMBER");
//            sql += " and fattura_anno " + Db.pcW(String.valueOf(this.prev.anno), "VARCHAR");
//            Db.executeSql(sql);
//            //vado ad aggiornare eventuali ddt o ordini legati
//            sql = "update test_ordi";
//            sql += " set doc_numero = " + Db.pc(numero_originale, "NUMBER");
//            sql += " where doc_serie " + Db.pcW(this.prev.serie, "VARCHAR");
//            sql += " and doc_numero " + Db.pcW(prev.numero, "NUMBER");
//            sql += " and doc_anno " + Db.pcW(String.valueOf(this.prev.anno), "VARCHAR");
//            sql += " and doc_tipo " + Db.pcW(String.valueOf(this.prev.tipoDocumento), "VARCHAR");
//            Db.executeSql(sql);
            //aggiorno scadenze
            //non serve più si va per id_doc
            //aggiorno provvigioni
            //non serve più si va per id_doc
            //rimetto numero originale su eventuali movimenti
            sql = "update movimenti_magazzino";
            sql += " set da_numero = " + Db.pc(numero_originale, "NUMBER");
            sql += ", da_anno = " + Db.pc(anno_originale, "NUMBER");
            sql += " where da_serie " + Db.pcW(dbdoc.serie, "VARCHAR");
            sql += " and da_numero " + Db.pcW(dbdoc.numero, "NUMBER");
            sql += " and da_anno " + Db.pcW(dbdoc.anno, "VARCHAR");
            sql += " and da_tabella = 'test_fatt'";
            Db.executeSql(sql);

        }

        if (from != null) {
            this.from.dbRefresh();
        }
    }

    private String dumpScadenze() {
        try {
            return DebugUtils.dumpAsString(DbUtils.getListMap(Db.getConn(), "select data_scadenza, importo from scadenze where documento_tipo = 'FA' and id_doc = " + id));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private Double dumpProvvigioni() {
        try {
            String sql = "select sum(importo_provvigione) from provvigioni where id_doc = " + id;
            System.out.println("sql = " + sql);
            return it.tnx.Util.round(((BigDecimal) DbUtils.getObject(Db.getConn(), sql)).doubleValue(), 2);
        } catch (NullPointerException ne) {
            return 0d;
        } catch (Exception e) {
            e.printStackTrace();
            return 0d;
        }
    }

    public void aggiornareProvvigioni() {
        block_aggiornareProvvigioni = true;
        if (SwingUtils.showYesNoMessage(this, "Vuoi aggiornare le provvigioni delle righe già inserite alla nuova provvigione ?")) {
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

    private void checkSize(int w, int h, int top, int left) {
        w = 100;
        h = 100;
        top = 0;
        left = 0;
    }

    public tnxDbPanel getDatiPanel() {
        return dati;
    }

    public JTabbedPane getTab() {
        return tabDocumento;
    }

    public boolean isAcquisto() {
        return false;
    }

    public Integer getId() {
        return this.id;
    }

    public boolean isPrezziIvati() {
        return prezzi_ivati_virtual.isSelected();
    }

    private void porto_in_temp() {
        if (!main.edit_doc_in_temp) {
            String sql = "check table righ_fatt_temp";
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

            //tolgo le righe da temp che tanto non servono +
            sql = "delete from righ_fatt_temp";
            sql += " where id_padre = " + frmTestFatt.this.id;
            sql += " and username = '" + main.login + "'";
            try {
                DbUtils.tryExecQuery(Db.getConn(), sql);
                System.out.println("sql ok:" + sql);
            } catch (Exception e) {
                System.err.println("sql errore:" + sql);
                e.printStackTrace();
            }

            sql = "delete te.* from righ_fatt_temp te join righ_fatt ri on te.id = ri.id";
            sql += " and ri.serie " + Db.pcW(frmTestFatt.this.dbdoc.serie, "VARCHAR");
            sql += " and ri.numero " + Db.pcW(String.valueOf(frmTestFatt.this.dbdoc.numero), "NUMBER");
            sql += " and ri.anno " + Db.pcW(String.valueOf(frmTestFatt.this.dbdoc.anno), "VARCHAR");
            sql += " and te.username = '" + main.login + "'";
            try {
                DbUtils.tryExecQuery(Db.getConn(), sql);
                System.out.println("sql ok:" + sql);
            } catch (Exception e) {
                System.err.println("sql errore:" + sql);
                e.printStackTrace();
            }

            //e inserisco
            sql = "insert into righ_fatt_temp";
            sql += " select *, '" + main.login + "' as username";
            sql += " from righ_fatt";
            sql += " where id_padre = " + frmTestFatt.this.id;
            try {
                DbUtils.tryExecQuery(Db.getConn(), sql);
                System.out.println("sql ok:" + sql);
            } catch (Exception e) {
                System.err.println("sql errore:" + sql);
                e.printStackTrace();
                trow = e;
            }
        } else {
            System.out.println("porto_in_temp, NO per edit in mem");            
        }
    }

    private void aggiornaPaIban() {
        boolean trovato_da_note = false;
        boolean trovato_da_iban = false;

        String notepagamento = texNotePagamento.getText();
        String expression = "[a-zA-Z]{2}[0-9]{2}[a-zA-Z0-9]{4}[0-9]{7}([a-zA-Z0-9]?){0,16}";
        CharSequence inputStr = cu.s(notepagamento);
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.find()) {
            try {
                dp_iban.setText(matcher.group(0));
                trovato_da_note = true;
            } catch (Exception e) {
            }
        }

        //se trovato da note pagamento non provo da iban
        if (!trovato_da_note) {
            String iban = texBancIban.getText();
            inputStr = cu.s(iban);
            pattern = Pattern.compile(expression);
            matcher = pattern.matcher(inputStr);
            if (matcher.find()) {
                try {
                    dp_iban.setText(matcher.group(0));
                    trovato_da_iban = true;
                } catch (Exception e) {
                }
            }
        }

        if (trovato_da_note || trovato_da_iban) {
            dp_iban.setEnabled(false);
            dp_iban_lab.setText("se vuoi cambiare IBAN cambialo dalle note di pagamento della fattura");
            //cerco nome banca
            String abi = "";
            try {
                abi = dp_iban.getText().substring(5, 10);
                List<Map> list = DbUtils.getListMap(Db.getConn(), "select nome from banche_abi where abi = " + dbu.sql(abi));
                if (list.size() > 0) {
                    String banca = cu.s(list.get(0).get("nome"));
                    banca = StringUtils.substringBefore(banca, "(");
                    banca = banca.trim();
                    if (banca.length() > 80) {
                        banca = StringUtils.left(banca, 80);
                    }
                    dp_banca.setText(banca);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            dp_iban.setEnabled(true);
            dp_iban.setText("");
            dp_iban_lab.setText("se vuoi puoi inserire l'IBAN dalle note di pagamento della fattura e verrà riportato anche qui");
        }
    }

    public void aggiornaFoglioRighe() {
        //visualizzo il pannellino per lengthdescrizioni multiriga
        zoom.setVisible(true);

        //associo il nuovo foglio
        ResultSet resu = Db.openResultSet(sqlGriglia);
        loadingFoglio = true;

        int rowCount = 0;
        Integer maxNumeroRiga = null;

        try {

            while (resu.next()) {
                foglio.setValueAt(resu.getString("riga"), rowCount, 0);
                if (maxNumeroRiga == null || CastUtils.toInteger0(resu.getString("riga")) > maxNumeroRiga) {
                    maxNumeroRiga = CastUtils.toInteger(resu.getString("riga"));
                }
                foglio.setValueAt(resu.getString("articolo"), rowCount, 1);
                foglio.setValueAt(resu.getString("descrizione"), rowCount, 2);
                foglio.setValueAt(resu.getString("um"), rowCount, 3);

//                    foglio.setValueAt(FormatUtils.formatNum0_5Dec(resu.getDouble(9)), rowCount, 4);  //qta
//                    foglio.setValueAt(Db.formatValuta(resu.getDouble(10)), rowCount, 5);                    
                foglio.setValueAt(resu.getObject("quantita"), rowCount, 4);  //qta
                foglio.setValueAt(resu.getObject("prezzo"), rowCount, 5); //prezzo

                foglio.setValueAt(resu.getObject("Sconti"), rowCount, 6); //sc.1
                foglio.setValueAt(resu.getObject("sconto2"), rowCount, 7); //sc2
                foglio.setValueAt(resu.getString("iva"), rowCount, 9); //iva

                foglio.setValueAt(resu.getString("id"), rowCount, 10); //id
                
                foglio.setValueAt(resu.getString("note"), rowCount, 11); //note

                //calcolo importi riga
                String temp = "";
                double importo = 0;
                double sconto1 = 0;
                double sconto2 = 0;
                double quantita = 0;

                try {
//                        sconto1 = resu.getDouble("sconto1");
                    sconto1 = resu.getDouble("Sconti");
                    sconto2 = resu.getDouble("sconto2");
                    quantita = CastUtils.toDouble0(resu.getDouble("quantita"));
                    importo = resu.getDouble("prezzo");
                } catch (java.lang.NumberFormatException err4) {
                }

                importo = importo - (importo / 100 * sconto1);
                importo = importo - (importo / 100 * sconto2);
                importo = importo * quantita;

                if (importo != 0) {
//                        foglio.setValueAt(it.tnx.Util.format2Decimali(importo), rowCount, 8);
                    foglio.setValueAt(importo, rowCount, 8);
                }

                rowCount++;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        //ricalcolo progressivi riga successivi (problema Marchetti service che aveva numeri riga duplicati!)
        if (maxNumeroRiga != null) {
            maxNumeroRiga = maxNumeroRiga / 10;
            maxNumeroRiga = maxNumeroRiga * 10;
            for (int i = rowCount; i < foglioData.getRowCount(); i++) {
                maxNumeroRiga += 10;
                foglioData.setValueAt(maxNumeroRiga, i, 0);

                foglioData.setValueAt(null, i, 1);
                foglioData.setValueAt(null, i, 2);
                foglioData.setValueAt(null, i, 3);
                foglioData.setValueAt(null, i, 4);
                foglioData.setValueAt(null, i, 5);
                foglioData.setValueAt(null, i, 6);
                foglioData.setValueAt(null, i, 7);
                foglioData.setValueAt(null, i, 8);
                foglioData.setValueAt(null, i, 9);
                foglioData.setValueAt(null, i, 10);
            }
        }

        loadingFoglio = false;
        ricalcolaTotali();
    }

    private void impostaSpeseBollo() {
        try {
            doc.speseBolloSiNo = bollo_si_no.isSelected();
            if (StringUtils.isBlank(bollo_importo.getText())) {
                doc.speseBollo = null;
            } else {
                doc.speseBollo = Db.getDouble(bollo_importo.getText());
            }
        } catch (Exception err) {
            doc.speseBollo = null;
        }
        ricalcolaTotali();
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

class timerRefreshFattura extends java.util.TimerTask {

    frmTestFatt parent;
    gestioneFatture.logic.documenti.Documento doc;

    public timerRefreshFattura(frmTestFatt parent, gestioneFatture.logic.documenti.Documento doc) {
        this.parent = parent;
        this.doc = doc;
    }

    public void run() {
        parent.ricalcolaTotali();
    }
}

class DataModelFoglio extends javax.swing.table.DefaultTableModel {

    frmTestFatt form;
    int currentRow = -1;
    Cliente cliente = null;
    Integer old_cliente = null;
    String listino = null;
    boolean isItalian = true;
    double cliente_sconto1r = 0;
    double cliente_sconto2r = 0;

    public DataModelFoglio(int rowCount, int columnCount, frmTestFatt form) {
        super(rowCount, columnCount);
        this.form = form;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == 8) {
            return false;
        }
        return super.isCellEditable(row, column);
    }

    public void setValueAt(Object obj, int row, int col) {
//        System.out.println("frmTestFatt foglio setValueAt = " + obj + " row:" + row + " col:" + col);
        super.setValueAt(obj, row, col);

        if (form.loadingFoglio || form.editingFoglio) {
            return;
        }

        String sql;
        String sqlc;
        String sqlv;
        currentRow = row;

//        SwingUtils.showFlashMessage2(String.valueOf(getValueAt(row, 3)) + "|" + String.valueOf(getValueAt(row, 4)) + "|" + String.valueOf(getValueAt(row, 5)) + "|" + String.valueOf(getValueAt(row, 6)), 5);
        if ((getValueAt(row, 9) == null || String.valueOf(getValueAt(row, 9)).equals("0")) && (getValueAt(row, 1) != null || getValueAt(row, 2) != null) && form.loadingFoglio == false) {
            if (StringUtils.isNotBlank(cu.s(getValueAt(row, 4))) || StringUtils.isNotBlank(cu.s(getValueAt(row, 5)))) {
//                setValueAt("20", row, 9);
                form.editingFoglio = true;
                setValueAt(InvoicexUtil.getIvaDefaultPassaggio(), row, 9);
                form.editingFoglio = false;
            }
        }
        if ((getValueAt(row, 3) == null || String.valueOf(getValueAt(row, 3)).equals("0")) && (getValueAt(row, 1) != null || getValueAt(row, 2) != null) && form.loadingFoglio == false) {
            if (!Db.nz(main.fileIni.getValue("varie", "umpred"), "").equals("")) {
                form.editingFoglio = true;
                setValueAt(main.fileIni.getValue("varie", "umpred"), currentRow, 3);
                form.editingFoglio = false;
            }
        }

        //per codice articolo vado a riprendere i dati
        if ((col == 1) && form.loadingFoglio == false) {

            String codice = "";
            String desc = "";
            codice = String.valueOf(Db.nz(getValueAt(currentRow, 1), ""));
            desc = String.valueOf(Db.nz(getValueAt(currentRow, 2), ""));

            if (codice.trim().length() > 0) {
                recuperaDatiArticolo(String.valueOf(obj));
            }

            if (codice.trim().length() > 0 || desc.trim().length() > 0) {
//                if (Db.nz(getValueAt(row, 9), "").equals("")) {
//                    setValueAt("20", currentRow, 9);
//                }
            }
        }

        //ricalcolo importo riga
        if (col != 8) {

            String temp = "";
            double importo = 0;
            double sconto1 = 0;
            double sconto2 = 0;
            double quantita = 0;

            try {
                temp = Db.nz(getValueAt(row, 6), "");
                temp = temp.replace('.', ',');
                sconto1 = it.tnx.Util.getDouble(temp);
            } catch (java.lang.NumberFormatException err1) {
            }

            try {
                temp = Db.nz(getValueAt(row, 7), "");
                temp = temp.replace('.', ',');
                sconto2 = it.tnx.Util.getDouble(temp);
            } catch (java.lang.NumberFormatException err2) {
            }

            try {
                temp = Db.nz(getValueAt(row, 4), "");
                temp = temp.replace('.', ',');
                quantita = it.tnx.Util.getDouble(temp);
            } catch (java.lang.NumberFormatException err3) {
            }

            try {
                temp = Db.nz(getValueAt(row, 5), "");
                temp = temp.replace('.', ',');
                importo = it.tnx.Util.getDouble(temp);
//                System.out.println("importo:" + importo);
            } catch (java.lang.NumberFormatException err4) {
            }

            importo = importo - (importo / 100 * sconto1);
            importo = importo - (importo / 100 * sconto2);
            importo = importo * quantita;

            if (importo != 0) {
                form.editingFoglio = true;
                setValueAt(it.tnx.Util.format2Decimali(importo), row, 8);
                form.editingFoglio = false;
            }
        }

        //salvo modifica
        if (((col >= 0 && col <= 9) || (col == 11)) && form.loadingFoglio == false) {

            String codice = "";
            String desc = "";
            codice = String.valueOf(Db.nz(getValueAt(currentRow, 1), ""));
            desc = String.valueOf(Db.nz(getValueAt(currentRow, 2), ""));
            double qta = CastUtils.toDouble0(getValueAt(currentRow, 4));

            if (codice.trim().length() > 0 || desc.trim().length() > 0 || qta != 0d) {

                //totale_imponibile
                double totale_imponibile = 0;
                double totale_ivato = 0;
                double prezzo_ivato = 0;
                try {
                    double prezzo = CastUtils.toDouble0All(getValueAt(currentRow, 5));
                    double sconto1 = CastUtils.toDouble0All(getValueAt(currentRow, 6));
                    double sconto2 = CastUtils.toDouble0All(getValueAt(currentRow, 7));
                    double tot_senza_iva = prezzo * qta;
                    tot_senza_iva = tot_senza_iva - (tot_senza_iva / 100d * sconto1);
                    tot_senza_iva = tot_senza_iva - (tot_senza_iva / 100d * sconto2);
                    tot_senza_iva = FormatUtils.round(tot_senza_iva, 2);
                    if (main.fileIni.getValueBoolean("pref", "attivaArrotondamento", false)) {
                        double parametro = 0d;
                        boolean perDifetto = true;
                        tot_senza_iva = InvoicexUtil.calcolaPrezzoArrotondato(tot_senza_iva, parametro, perDifetto);
                    }
                    totale_imponibile = tot_senza_iva;
                    //totale ivato
                    String iva_codice = (String) getValueAt(currentRow, 9);
                    double iva_perc = 0;
                    try {
                        iva_perc = CastUtils.toDouble0(DbUtils.getObject(Db.getConn(), "select percentuale from codici_iva where codice = '" + iva_codice + "'"));
                    } catch (Exception e) {
                        System.err.println("frmTestFatt fogli orighe setValueAt non trovata iva:" + iva_codice);
                        iva_perc = 0;
                    }
                    double tot_con_iva = tot_senza_iva + (tot_senza_iva / 100d * iva_perc);
                    tot_con_iva = FormatUtils.round(tot_con_iva, 2);
                    totale_ivato = tot_con_iva;
                    prezzo_ivato = prezzo + (prezzo / 100d * iva_perc);
                    prezzo_ivato = FormatUtils.round(prezzo_ivato, 2);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (1 == 1) {
                    //provare l'inserimento dell riga, se errore di chiave duplicata andare in update
                    Map m = new HashMap();
                    Map tras = new HashMap();
                    tras.put("qta", "quantita");
                    tras.put("codice art.", "codice_articolo");
                    tras.put("sc.1", "sconto1");
                    tras.put("sc.2", "sconto2");
                    String campo = cu.s(form.foglio.getColumnModel().getColumn(col).getIdentifier());
                    if (tras.containsKey(campo)) {
                        campo = cu.s(tras.get(campo));
                    }
                    Object value = getValueAt(currentRow, col);
                    if (!campo.equalsIgnoreCase("note")) {
                        m.put(campo, value);
                    }
                    m.put("serie", form.getSerie());
                    m.put("numero", form.getNumero());
                    m.put("anno", form.getAnno());
                    m.put("riga", getValueAt(currentRow, 0));
                    m.put("id_padre", form.id);

                    m.put("totale_imponibile", totale_imponibile);
                    m.put("totale_ivato", totale_ivato);
                    m.put("prezzo_ivato", prezzo_ivato);
                    m.put("iva", getValueAt(currentRow, cu.i0(form.foglio.getColumn("iva").getModelIndex())));

                    Integer id = cu.i(getValueAt(row, 10));
                    boolean update = false;
                    if (id != null) {
                        try {
                            if (dbu.containRows(Db.getConn(), "select id from " + form.getNomeTabRighe() + " where id = " + dbu.sql(getValueAt(row, 10)))) {
                                update = true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            if (dbu.containRows(Db.getConn(), "select id from " + form.getNomeTabRighe() + " where id_padre = " + form.id + " and riga = " + dbu.sql(getValueAt(row, 0)))) {
                                update = true;
                            }
                        } catch (Exception e) {
                        }
                    }
                    if (!update) {
                        sql = "insert into " + form.getNomeTabRighe() + " set " + DbUtils.prepareSqlFromMap(m);
                    } else {
                        sql = "update " + form.getNomeTabRighe() + " set " + DbUtils.prepareSqlFromMap(m) + " where id = " + getValueAt(row, 10);
                    }
                    System.out.println("sql = " + sql);
                    try {
                        if (sql.startsWith("insert")) {
                            Integer lastid = Db.executeSqlRetIdDialogExc(Db.getConn(), sql, false, true);
                            setValueAt(lastid, row, 10);
                        } else {
                            dbu.tryExecQuery(Db.getConn(), sql);
                        }
                        //salvo eventuali note
                        if (col == 11) {
                            String note = cu.s(getValueAt(currentRow, 11));
                            Integer id_riga = cu.i(getValueAt(currentRow, 10));
                            if (id_riga != null) {
                                if (StringUtils.isNotBlank(note)) {
                                    //tento insert oppure update
                                    Integer id_nota = cu.i(dbu.getObject(Db.getConn(), "select id from note where tabella = 'righ_fatt' and id_tab = " + id_riga, false));
                                    String sqlnota = "update note ";
                                    if (id_nota == null) {
                                        sqlnota = "insert into note ";
                                    }
                                    Map key_nota = new HashMap();
                                    key_nota.put("tabella", "righ_fatt");
                                    key_nota.put("id_tab", id_riga);
                                    key_nota.put("id_utente", main.utente.getIdUtente());
                                    key_nota.put("note", note);
                                    sqlnota += " set " + dbu.prepareSqlFromMap(key_nota);
                                    if (key_nota.get("id") != null) {
                                        sqlnota += " where id = " + dbu.sql(id_nota);
                                    }
                                    System.out.println("sql note = " + sqlnota);
                                    try {
                                        dbu.tryExecQuery(it.tnx.Db.getConn(), sqlnota);
                                    } catch (Exception ex) {
                                        SwingUtils.showExceptionMessage(main.getPadreFrame(), ex);
                                    }
                                } else {
                                    //delete della nota
                                    dbu.tryExecQuery(Db.getConn(), "delete from note where tabella = 'righ_fatt' and id_tab = " + id_riga);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    //VECCHIA ROUTINE, NON PASSA DI QUI !!!!!!!

                    //ricalcolare i totali
                    //salvare solo le modifiche invece di elminiare e ricreare...
                    sql = "delete from " + form.getNomeTabRighe() + " where ";
//                    sql += "serie = " + Db.pc(form.getSerie(), Types.VARCHAR);
//                    sql += " and numero = " + form.getNumero();
//                    sql += " and anno = " + form.getAnno();
//                    sql += " id_padre = " + form.id;
//                    sql += " and riga = " + Db.pc(getValueAt(currentRow, 0), Types.INTEGER);
                    sql += " id = " + Db.pc(getValueAt(currentRow, 10), Types.INTEGER);
                    Db.executeSql(sql);

                    sql = "insert into " + form.getNomeTabRighe() + " (";
                    sqlc = "serie";
                    sqlv = Db.pc(form.getSerie(), Types.VARCHAR);
                    sqlc += ", numero";
                    sqlv += ", " + form.getNumero();
                    sqlc += ", id_padre";
                    sqlv += ", " + Db.pc(form.id, Types.INTEGER);
                    sqlc += ", anno";
                    sqlv += ", " + form.getAnno();
                    sqlc += ", riga";
                    sqlv += ", " + Db.pc(getValueAt(currentRow, 0), Types.INTEGER);
                    sqlc += ", codice_articolo";
                    sqlv += ", " + Db.pc(getValueAt(currentRow, 1), Types.VARCHAR);
                    sqlc += ", descrizione";
                    sqlv += ", " + Db.pc(getValueAt(currentRow, 2), Types.VARCHAR);
                    sqlc += ", um";
                    sqlv += ", " + Db.pc(getValueAt(currentRow, 3), Types.VARCHAR);
                    sqlc += ", quantita";
                    sqlv += ", " + getDouble(Db.nz(getValueAt(currentRow, 4), "").replace('.', ','));
                    sqlc += ", prezzo";
                    sqlv += ", " + getDouble(Db.nz(getValueAt(currentRow, 5), "").replace('.', ','));
                    sqlc += ", iva";
                    if (getValueAt(currentRow, 9) == null || getValueAt(currentRow, 9).toString().equals("")) {
                        sqlv += ", ''";
                    } else {
                        sqlv += ", " + Db.pc(Db.nz(getValueAt(currentRow, 9), InvoicexUtil.getIvaDefaultPassaggio()), Types.VARCHAR);
                    }
                    sqlc += ", sconto1";
                    sqlv += ", " + getDouble(Db.nz(getValueAt(currentRow, 6), "").replace('.', ','));
                    sqlc += ", sconto2";
                    sqlv += ", " + getDouble(Db.nz(getValueAt(currentRow, 7), "").replace('.', ','));
                    sqlc += ", stato";
                    sqlv += ", 'P'";
                }

            } else {
                System.out.println("elimino riga");

                if (form.getNumero().length() > 0 && form.getAnno().length() > 0 && getValueAt(currentRow, 0).toString().length() > 0) {
                    System.out.println("elimino riga 2");
                    sql = "delete from " + form.getNomeTabRighe() + " where ";
                    sql += " id = " + Db.pc(getValueAt(currentRow, 10), Types.INTEGER);
                    Db.executeSql(sql);
                }
            }

            form.dbAssociaGrigliaRighe();
        }
    }

    private String getDouble(Object valore) {

        if (valore == null) {

            return "0";
        }
        NumberFormat numFormat = NumberFormat.getInstance();

        try {

            return Db.pc(numFormat.parse(valore.toString()), Types.DOUBLE);
        } catch (Exception err) {

            return "0";
        }
    }

    private void recuperaDatiArticolo(String codArt) {
        String codicelistino = "0";
        boolean non_applicare_percentuale = false;

        if (codArt.length() > 0) {

            ResultSet temp;
            String sql = "select * from articoli";
            sql += " where codice = " + Db.pc(codArt, "VARCHAR");
//            temp = Db.openResultSet(sql);
            try {
                temp = DbUtils.tryOpenResultSet(Db.getConn(), sql);

                if (temp.next() == true) {
                    non_applicare_percentuale = CastUtils.toBoolean(temp.getString("non_applicare_percentuale"));

                    boolean eng = false;

                    if (form.texClie.getText().length() > 0) {

                        if (old_cliente == null || old_cliente != Integer.parseInt(form.texClie.getText())) {
                            cliente = new Cliente(Integer.parseInt(form.texClie.getText()));
                            old_cliente = Integer.parseInt(form.texClie.getText());
                            listino = cliente.getListinoCliente(false);
                            isItalian = cliente.isItalian();
                            cliente_sconto1r = CastUtils.toDouble0(cliente.getObject("sconto1r"));
                            cliente_sconto2r = CastUtils.toDouble0(cliente.getObject("sconto2r"));
                        }
                        if (cliente != null) {
//                                codicelistino = cliente.getListinoCliente(false);
                            codicelistino = listino;
//                                if (cliente.isItalian() == true) {
                            if (isItalian == true) {
                                eng = false;
                            } else {
                                eng = true;
                            }
                        }
                    }

                    if (eng == true) {
                        setValueAt(Db.nz(temp.getString("um_en"), ""), currentRow, 3);
                    } else {
                        setValueAt(Db.nz(temp.getString("um"), ""), currentRow, 3);
                    }

                    String iva = this.getIva(form.texClie.getText(), codArt);
                    setValueAt(iva, currentRow, 9);

//                    int clieIva = (Integer) DbUtils.getObject(Db.getConn(), "SELECT iva_standard FROM clie_forn WHERE id = " + Db.pc(Integer.parseInt(form.texClie.getText()), Types.INTEGER));
//                    try {
//                        Double iva = Double.parseDouble(temp.getString("iva"));
//                        setValueAt(Db.formatNumero(iva), currentRow, 9);
//                    } catch (Exception e1) {
//                        ResultSet ivaStandard = Db.openResultSet("SELECT iva_standard as iva FROM clie_forn WHERE codice = " + Db.pc(form.texClie.getText(), Types.INTEGER));
//                        try {
//                            if (ivaStandard.next()) {
//                                Double iva = Double.parseDouble(ivaStandard.getString("iva"));
//                                if (iva != -1) {
//                                    setValueAt(Db.formatNumero(iva), currentRow, 9);
//                                } else {
//                                    setValueAt(Db.formatNumero(20d), currentRow, 9);
//                                }
//                            } else {
//                                setValueAt(Db.formatNumero(20d), currentRow, 9);
//                            }
//                        } catch (Exception e2) {
//                            setValueAt(Db.formatNumero(20d), currentRow, 9);
//                        }
//
//                    }
                    setValueAt(Db.nz(temp.getString("descrizione"), ""), currentRow, 2);

                    sql = "select prezzo, tipi_listino.*, sconto1, sconto2 from articoli_prezzi left join tipi_listino on articoli_prezzi.listino = tipi_listino.codice";
                    sql += " where articolo = " + Db.pc(codArt, "VARCHAR");
                    sql += " and listino = " + Db.pc(codicelistino, java.sql.Types.VARCHAR);

                    ResultSet prezzi = Db.openResultSet(sql);

                    if (prezzi.next() == true) {
//                        setValueAt(Db.formatDecimal(Math.max(prezzi.getDouble("sconto1"), cliente_sconto1r)), currentRow, 6);
//                        setValueAt(Db.formatDecimal(Math.max(prezzi.getDouble("sconto2"), cliente_sconto2r)), currentRow, 7);
//                        setValueAt(Db.formatDecimal5(temp.getDouble("prezzo1")), currentRow, 5);
                        setValueAt(cu.d(Math.max(prezzi.getDouble("sconto1"), cliente_sconto1r)), currentRow, 6);
                        setValueAt(cu.d(Math.max(prezzi.getDouble("sconto2"), cliente_sconto2r)), currentRow, 7);
                        setValueAt(cu.d(temp.getDouble("prezzo1")), currentRow, 5);
                        //                        if (prezzi.getString("ricarico_flag") != null && prezzi.getString("ricarico_flag").equals("S") && !servizio) {
                        if (prezzi.getString("ricarico_flag") != null && prezzi.getString("ricarico_flag").equals("S")) {
                            double perc = prezzi.getDouble("ricarico_perc");
                            double nuovo_prezzo = 0;
                            sql = "select prezzo from articoli_prezzi";
                            sql += " where articolo = " + Db.pc(codArt, "VARCHAR");
                            sql += " and listino = " + Db.pc(prezzi.getString("ricarico_listino"), java.sql.Types.VARCHAR);
                            ResultSet prezzi2 = Db.openResultSet(sql);
                            prezzi2.next();
                            if (non_applicare_percentuale) {
                                nuovo_prezzo = prezzi2.getDouble("prezzo");
                            } else {
                                nuovo_prezzo = prezzi2.getDouble("prezzo") * ((perc + 100d) / 100d);
                            }
//                            setValueAt(Db.formatDecimal5(nuovo_prezzo), currentRow, 5);
                            setValueAt(cu.d(nuovo_prezzo), currentRow, 5);
                        } else {
//                            setValueAt(Db.formatDecimal5(prezzi.getDouble(1)), currentRow, 5);
                            setValueAt(cu.d(prezzi.getDouble(1)), currentRow, 5);
                        }
                    } else {
                        sql = "select prezzo, tipi_listino.*, sconto1, sconto2 from articoli_prezzi left join tipi_listino on articoli_prezzi.listino = tipi_listino.codice";
                        sql += " where articolo = " + Db.pc(codArt, "VARCHAR");
                        sql += " and listino = " + Db.pc(codicelistino, Types.VARCHAR);
                        prezzi = Db.openResultSet(sql);

                        if (prezzi.next() == true) {
//                            setValueAt(Db.formatDecimal(Math.max(prezzi.getDouble("sconto1"), cliente_sconto1r)), currentRow, 6);
//                            setValueAt(Db.formatDecimal(Math.max(prezzi.getDouble("sconto2"), cliente_sconto2r)), currentRow, 7);
//                            setValueAt(Db.formatDecimal5(temp.getDouble("prezzo1")), currentRow, 5);
                            setValueAt(cu.d(Math.max(prezzi.getDouble("sconto1"), cliente_sconto1r)), currentRow, 6);
                            setValueAt(cu.d(Math.max(prezzi.getDouble("sconto2"), cliente_sconto2r)), currentRow, 7);
                            setValueAt(cu.d(temp.getDouble("prezzo1")), currentRow, 5);
                            if (prezzi.getString("ricarico_flag") != null && prezzi.getString("ricarico_flag").equals("S")) {
                                double perc = prezzi.getDouble("ricarico_perc");
                                double nuovo_prezzo = 0;
                                sql = "select prezzo from articoli_prezzi";
                                sql += " where articolo = " + Db.pc(codArt, "VARCHAR");
                                sql += " and listino = " + Db.pc(prezzi.getString("ricarico_listino"), java.sql.Types.VARCHAR);
                                ResultSet prezzi2 = Db.openResultSet(sql);
                                prezzi2.next();
                                if (non_applicare_percentuale) {
                                    nuovo_prezzo = prezzi2.getDouble("prezzo");
                                } else {
                                    nuovo_prezzo = prezzi2.getDouble("prezzo") * ((perc + 100d) / 100d);
                                }
//                                setValueAt(Db.formatDecimal5(nuovo_prezzo), currentRow, 5);
                                setValueAt(cu.d(nuovo_prezzo), currentRow, 5);
                            } else {
//                                setValueAt(Db.formatDecimal5(prezzi.getDouble(1)), currentRow, 5);
                                setValueAt(cu.d(prezzi.getDouble(1)), currentRow, 5);
                            }
                        }
                    }
                } else {
                    form.labStatus.setText("Non trovo l'articolo:" + codArt);
                    //da cirri chiesto se si poteva rimuovere
//                    try {
//                        String sqlClie = "SELECT iva_standard FROM clie_forn WHERE codice = " + Db.pc(form.texClie.getText(), Types.VARCHAR);
//                        String ivaCliente = String.valueOf(DbUtils.getObject(Db.getConn(), sqlClie));
//                        if (ivaCliente.equals("")) {
//                            setValueAt(main.fileIni.getValue("iva", "codiceIvaDefault", InvoicexUtil.getIvaDefaultPassaggio()), currentRow, 9);
//                        } else {
//                            setValueAt(ivaCliente, currentRow, 9);
//                        }
//                    } catch (Exception e) {
//                        setValueAt(main.fileIni.getValue("iva", "codiceIvaDefault", InvoicexUtil.getIvaDefaultPassaggio()), currentRow, 9);
//                    }
                }
                DbUtils.close(temp);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    private String getIva(String codiceCliente, String codiceArticolo) {
        String ivaStandard = InvoicexUtil.getIvaDefaultPassaggio();
        String ivaCliente = "";
        String ivaArticolo = "";
        try {
            String sql = "SELECT iva_standard FROM clie_forn WHERE codice = " + Db.pc(codiceCliente, Types.VARCHAR);
            ivaCliente = cu.s(DbUtils.getObject(Db.getConn(), sql));
        } catch (Exception e) {
            ivaCliente = "";
        }
        try {
            String sql = "SELECT iva FROM articoli WHERE codice = " + Db.pc(codiceArticolo, Types.VARCHAR);
            ivaArticolo = cu.s(DbUtils.getObject(Db.getConn(), sql));
        } catch (Exception e) {
            ivaArticolo = "";
        }

//        return ivaCliente.equals("") ? ivaArticolo.equals("") ? ivaStandard : ivaArticolo : ivaCliente;
        System.out.println("frmTestFatt getIva cliente:" + ivaCliente + " articolo:" + ivaArticolo + " std:" + ivaStandard);
        if (!ivaCliente.equals("")) {
            return ivaCliente;
        }
        if (!ivaArticolo.equals("")) {
            return ivaArticolo;
        }
        return ivaStandard;
    }

    public void removeRow(int param) {

        String sql;
        String sqlc;
        String sqlv;

        //cancello la riga
        if (form.getNumero().length() > 0 && form.getAnno().length() > 0 && getValueAt(param, 0).toString().length() > 0) {
            sql = "delete from " + form.getNomeTabRighe() + " where ";
            sql += " id_padre = " + form.id;
            sql += " and riga = " + Db.pc(getValueAt(param, 0), Types.INTEGER);

            if (Db.executeSql(sql) == true) {
                System.out.println("row count:" + getRowCount() + " row to del:" + param);
                super.removeRow(param);
            }

            form.dbAssociaGrigliaRighe();
        }
    }
}
