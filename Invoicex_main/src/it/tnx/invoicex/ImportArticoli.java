package it.tnx.invoicex;

import gestioneFatture.InvoicexEvent;
import it.tnx.Db;
import gestioneFatture.JDialogInsert;
import gestioneFatture.main;
import it.tnx.commons.CastUtils;
import it.tnx.commons.DbUtils;
import it.tnx.commons.RunnableWithArgs;
import it.tnx.commons.SwingUtils;
import it.tnx.commons.cu;
import it.tnx.commons.dbu;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.eventusermodel.AbortableHSSFListener;

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.EventWorkbookBuilder.SheetRecordCollectingListener;
import org.apache.poi.hssf.eventusermodel.HSSFUserException;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ImportArticoli implements HSSFListener {

    private POIFSFileSystem fs;
    private int lastRowNumber;
    /** Should we output the formula, or the value it has? */
    private boolean outputFormulaValues = true;
    /** For parsing Formulas */
    private SheetRecordCollectingListener workbookBuildingListener;
    // Records we pick up as we process
    private SSTRecord sstRecord;
    private FormatTrackingHSSFListener formatListener;
    private ArrayList boundSheetRecords = new ArrayList();
    // For handling formulas with string results
    private int nextRow;
    private int nextColumn;
    private boolean outputNextStringRecord;
    private ArrayList chiaviDb;
    private HashMap valorim = new HashMap(10);
    private String sql = "";
    private String sqlpre = "";
    private String listino = "";
    public JDialogInsert insert;
    private int lastRow;
    NumberFormat nf1 = new DecimalFormat("0");
    DecimalFormat df = new DecimalFormat("0.#####");
    boolean interrompi = false;
    Integer deposito = null;
    
    int elab_ok = 0;
    int elab_ko = 0;
    private String filename;
    
    static public enum TipoImport {
        NO,
        GIACENZA_TOTALE,
        GIACENZA_DEPOSITO
    }
    
    TipoImport giacenze = TipoImport.NO;
    
//!!!    RunnableWithArgs elabora_post = null;

    public ImportArticoli(POIFSFileSystem fs) {
        this.fs = fs;
    }

    public ImportArticoli(String filename, String listino, int lastRow, TipoImport giacenze, Integer deposito) throws IOException, FileNotFoundException {
        this(new POIFSFileSystem(new FileInputStream(filename)));
        this.lastRow = lastRow;
        this.listino = listino;
        this.giacenze = giacenze;
        this.filename = filename;
        this.deposito = deposito;
        
        chiaviDb = new ArrayList();
        chiaviDb.add("codice");
        chiaviDb.add("descrizione");
        chiaviDb.add("prezzo");
        chiaviDb.add("iva");
        chiaviDb.add("um");
        chiaviDb.add("codice_a_barre");
        chiaviDb.add("codice_fornitore");
        chiaviDb.add("descrizione_en");
        chiaviDb.add("um_en");
        chiaviDb.add("gestione_lotti");
        chiaviDb.add("gestione_matricola");
        chiaviDb.add("fornitore");
        chiaviDb.add("categoria");
        chiaviDb.add("sottocategoria");
        chiaviDb.add("peso_kg");
        chiaviDb.add("disponibilita_reale");
    }

    public void process() throws IOException {
        MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(this);
        formatListener = new FormatTrackingHSSFListener(listener);

        HSSFEventFactory factory = new HSSFEventFactory();
        HSSFRequest request = new HSSFRequest();
        request.addListenerForAllRecords(new AbortableHSSFListener() {
            @Override
            public short abortableProcessRecord(Record record) throws HSSFUserException {                
                if (interrompi) {
                    System.out.println("aborto");
                    return 1;
                }
                return 0;
            }
        });

        if (outputFormulaValues) {
            request.addListenerForAllRecords(formatListener);
        } else {
            workbookBuildingListener = new SheetRecordCollectingListener(formatListener);
            request.addListenerForAllRecords(workbookBuildingListener);
        }

        //factory.processWorkbookEvents(request, fs);
        try {
            factory.abortableProcessWorkbookEvents(request, fs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processRecord(Record record) {
        int thisRow = -1;
        int thisColumn = -1;
        
        try {
            String temp = "";
            switch (record.getSid()) {
                case BoundSheetRecord.sid:
                    boundSheetRecords.add(record);
                    break;
                case BOFRecord.sid:
                    break;
                case SSTRecord.sid:
                    sstRecord = (SSTRecord) record;
                    break;
                case BlankRecord.sid:
                    break;
                case BoolErrRecord.sid:
                    break;
                case FormulaRecord.sid:
                    FormulaRecord frec = (FormulaRecord) record;
                    thisRow = frec.getRow();
                    thisColumn = frec.getColumn();
                    break;
                case StringRecord.sid:
                    StringRecord srec = (StringRecord) record;
                    temp = cu.s(srec.getString());
                    temp = temp.replaceAll("\\s+$", "");
                    
                        valorim.put(chiaviDb.get(thisColumn), temp);
//                    }
                    thisRow = nextRow;
                    thisColumn = nextColumn;
                    break;
                case LabelRecord.sid:
                    LabelRecord lrec = (LabelRecord) record;
                    thisRow = lrec.getRow();
                    thisColumn = lrec.getColumn();
                    temp = cu.s(lrec.getValue());
                    temp = temp.replaceAll("\\s+$", "");
                    valorim.put(chiaviDb.get(thisColumn), temp);
                    break;
                case LabelSSTRecord.sid:
                    LabelSSTRecord lsrec = (LabelSSTRecord) record;
                    thisRow = lsrec.getRow();
                    thisColumn = lsrec.getColumn();
                    if (sstRecord == null) {
                        valorim.put(chiaviDb.get(thisColumn), "");
                    } else {
                        temp = sstRecord.getString(lsrec.getSSTIndex()).toString();
                        temp = temp.replaceAll("\\s+$", "");
                        if (chiaviDb.get(lsrec.getColumn()).equals("prezzo")) {
                            //altrimenti se sono sul prezzo lo formatto con i decimali
                            Double prezzo = cu.toDoubleAll(temp);
                            if (prezzo != null) {
                                valorim.put(chiaviDb.get(thisColumn), prezzo);                    
                            }
                        } else {
                            valorim.put(chiaviDb.get(thisColumn), temp);
                        }
                    }
                    break;
                case NoteRecord.sid:
                    break;
                case NumberRecord.sid:
                    NumberRecord numrec = (NumberRecord) record;
                    thisRow = numrec.getRow();
                    thisColumn = numrec.getColumn();
                    if (!chiaviDb.get(numrec.getColumn()).equals("prezzo") 
                            && !chiaviDb.get(numrec.getColumn()).equals("peso_kg")
                            && !chiaviDb.get(numrec.getColumn()).equals("disponibilita_reale")) {
                        //se non sul prezzo tolgo tutti i decimali per codice
                        //e neppure sul peso
                        valorim.put(chiaviDb.get(thisColumn), nf1.format(numrec.getValue()));
                    } else if (chiaviDb.get(numrec.getColumn()).equals("prezzo")) {
                        //altrimenti se sono sul prezzo lo formatto con i decimali
                        Double prezzo = 0d;
                        try {
                            prezzo = numrec.getValue();
                        } catch (Exception e) {
                            System.err.println("!!! errore prezzo:" + formatListener.formatNumberDateCell(numrec) + " msg:" + e.getMessage());
                        }
                        valorim.put(chiaviDb.get(thisColumn), prezzo);
                    } else {
                        //peso
                        valorim.put(chiaviDb.get(thisColumn), numrec.getValue());
                    }
                    break;
                case RKRecord.sid:
                    break;
                default:
                    break;
            }

            // Handle missing column
            if (record instanceof MissingCellDummyRecord) {
                MissingCellDummyRecord mc = (MissingCellDummyRecord) record;
                thisRow = mc.getRow();
                thisColumn = mc.getColumn();
                valorim.put(chiaviDb.get(thisColumn), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!SwingUtils.showYesNoMessage(main.padre_panel.desktop.getSelectedFrame(), "Si è verificato questo problema:\n" + e.getClass().getSimpleName() + ":" + e.getMessage() + "\n\nVuoi continuare ?")) {
                interrompi = true;
            }            
        }

        // Update column and row count
        if (thisRow > -1) {
            lastRowNumber = thisRow;
        }

        // Handle end of row
        if (record instanceof LastCellOfRowDummyRecord) {            
            if (valorim.get("codice") == null || StringUtils.isBlank(cu.toString(valorim.get("codice")))) {
                //ignoro riga senza codice articolo
                insert.updateValue();
            } else {
                //tolgo valori nulli o vuoti
                for (Iterator it = valorim.entrySet().iterator(); it.hasNext();) {
                    Map.Entry entry = (Map.Entry) it.next();
                    Object v = entry.getValue();
                    if (v == null || StringUtils.isBlank(cu.toString(v))) {
                        it.remove();
                    }
                }                

                //controllo iva, se inserita ma vuota metto 21
                if (valorim.containsKey("iva") && (valorim.get("iva") == null || valorim.get("iva").toString().length() == 0)) {
                    valorim.put("iva", InvoicexUtil.getIvaDefaultPassaggio());
                }

                if (valorim.get("peso_kg") != null) {
                    System.out.println("peso:" + valorim.get("peso_kg"));
                }
                
                Double prezzo = 0d;
                try {
                    prezzo = (Double) valorim.get("prezzo");
                } catch (Exception e) {
                }

                valorim.remove("prezzo");
                Double giac = cu.d(valorim.get("disponibilita_reale"));
                valorim.remove("disponibilita_reale");

                /*
                sql = "REPLACE articoli SET " + DbUtils.prepareSqlFromMap(valorim);
                sqlpre = "REPLACE articoli_prezzi SET articolo = '" + valorim.get("codice") + "', prezzo = (" + prezzo + "), listino = '" + listino + "'";
                if (!sql.equals("") && !sqlpre.equals("")) {
                    try {
                        DbUtils.tryExecQuery(Db.getConn(), sql);
                    } catch (Exception e) {
                        System.err.println("!!! Errore:" + e.getMessage());
                        System.err.println("!!! query:" + sql);
                    }
                    try {
                        DbUtils.tryExecQuery(Db.getConn(), sqlpre);
                    } catch (Exception e) {
                        System.err.println("!!! Errore:" + e.getMessage());
                        System.err.println("!!! query:" + sqlpre);
                    }
                }
                */
                
                int rowsupdated = 0;
                int rowsupdatedprezzi = 0;
                sql = "insert into articoli set " + DbUtils.prepareSqlFromMap(valorim);
                try {
                    DbUtils.tryExecQuery(Db.getConn(), sql);
                    rowsupdated = 1;
                    elab_ok++;
                } catch (Exception e) {
                    if (e instanceof SQLException && ((SQLException)e).getErrorCode() == 1062) {
                        sql = "update articoli set " + DbUtils.prepareSqlFromMap(valorim);
                        sql += " where codice = '" + Db.aa(CastUtils.toString(valorim.get("codice"))) + "'";
                        try {
                            rowsupdated = DbUtils.tryExecQueryWithResult(Db.getConn(), sql);
                            elab_ok++;
                        } catch (Exception ex) {
                            System.err.println("sql di errore articoli: " + sql);
                            elab_ko++;
                            ex.printStackTrace();
                        }
                    } else {
                        e.printStackTrace();
                        if (!SwingUtils.showYesNoMessage(main.padre_panel.desktop.getSelectedFrame(), "Si è verificato questo problema:\n" + e.getClass().getSimpleName() + ":" + e.getMessage() + "\n\nVuoi continuare ?")) {
                            interrompi = true;
                        }
                    }
                }

                if (!interrompi) {
                    sqlpre = "insert into articoli_prezzi SET articolo = '" + Db.aa(CastUtils.toString(valorim.get("codice"))) + "', prezzo = (" + prezzo + "), listino = '" + Db.aa(listino) + "'";
                    try {
                        DbUtils.tryExecQuery(Db.getConn(), sqlpre);
                        rowsupdatedprezzi = 1;
                        elab_ok++;
                    } catch (Exception e) {
                        if (e instanceof SQLException && ((SQLException)e).getErrorCode() == 1062) {
                            try {
                                Integer id_ap = cu.i(dbu.getObject(Db.getConn(), "select id from articoli_prezzi where articolo = '" + Db.aa(CastUtils.toString(valorim.get("codice"))) + "' and listino = '" + Db.aa(listino) + "'"));
                                sqlpre = "update articoli_prezzi SET prezzo = (" + prezzo + ") where id = " + id_ap;
                                rowsupdatedprezzi = DbUtils.tryExecQueryWithResult(Db.getConn(), sqlpre);
                                elab_ok++;
                            } catch (Exception ex) {
                                System.err.println("sql di errore articoli prezzi: " + sqlpre);
                                elab_ko++;
                                ex.printStackTrace();
                            }
                        }
                    }

                    if (giacenze != TipoImport.NO && giac != null) {
                        Map map = new HashMap();
                        map.put("classe", this);
                        map.put("articolo", valorim.get("codice"));
                        map.put("tipoimport", giacenze);
                        map.put("giacenza", giac);
                        map.put("deposito", deposito);
                        map.put("rowsupdated", rowsupdated);
                        map.put("rowsupdatedprezzi", rowsupdatedprezzi);
                        map.put("nomefile", (new File(filename)).getName());
                        try {
                            main.events.fireInvoicexEventExc(new InvoicexEvent(map, InvoicexEvent.TYPE_IMPORT_ARTICOLO));
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (!SwingUtils.showYesNoMessage(main.padre_panel.desktop.getSelectedFrame(), "Si è verificato questo problema:\n" + e.getClass().getSimpleName() + ":" + e.getMessage() + "\n\nVuoi continuare ?")) {
                                interrompi = true;
                            }
                        }
                    }
                }

                sql = "";
                sqlpre = "";

                valorim.clear();
                insert.updateValue();
                System.out.println("elab_ok:" + elab_ok + " elab_ko:" + elab_ko);
            }
        }
    }
}
