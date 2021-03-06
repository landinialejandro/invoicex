/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * JDialogImportaReport2.java
 *
 * Created on 18-dic-2009, 10.35.20
 */

package gestioneFatture;

import java.awt.Cursor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.JOptionPane;

/**
 *
 * @author Toce Alessio
 */
public class JDialogImportaReport extends javax.swing.JDialog {
    JDialogImpostazioni padre;
    File report;
    /** Creates new form JDialogImportaReport2 */
    public JDialogImportaReport(javax.swing.JDialog parent, boolean modal, JDialogImpostazioni padre, File report) {
        super(parent, modal);
        initComponents();

        this.padre = padre;
        this.report = report;

        String nomeFile = report.getName();
        if(nomeFile.startsWith("fattura")){
            nomeFile = nomeFile.replace("fattura_", "").replace(".jrxml", "");
        } else if (nomeFile.startsWith("ddt")){
            nomeFile = nomeFile.replace("ddt_", "").replace(".jrxml", "");;
        } else if (nomeFile.startsWith("ordine")){
            nomeFile = nomeFile.replace("ordine_", "").replace(".jrxml", "");;
        } else if (nomeFile.startsWith("fattura_acc")){
            nomeFile = nomeFile.replace("fattura_acc_", "").replace(".jrxml", "");;
        }

        texNome.setText(nomeFile);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        comTipoReport = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        texNome = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Importa Report");

        jLabel1.setText("Tipo di Report: ");

        comTipoReport.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Fattura", "DDT", "Ordine", "Fattura Accompagnatoria" }));

        jLabel2.setText("Nome:");

        texNome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                texNomeActionPerformed(evt);
            }
        });

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/it/tnx/invoicex/res/Delete Sign-16.png"))); // NOI18N
        jButton1.setText("Annulla");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/it/tnx/invoicex/res/Checkmark-16.png"))); // NOI18N
        jButton2.setText("Importa");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .add(6, 6, 6)
                        .add(comTipoReport, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 230, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(43, 43, 43)
                        .add(jLabel2)
                        .add(6, 6, 6)
                        .add(texNome, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 230, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(134, 134, 134)
                        .add(jButton1)
                        .add(3, 3, 3)
                        .add(jButton2)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(3, 3, 3)
                        .add(jLabel1))
                    .add(comTipoReport, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(3, 3, 3)
                        .add(jLabel2))
                    .add(texNome, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(11, 11, 11)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jButton1)
                    .add(jButton2))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void texNomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_texNomeActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_texNomeActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            String tipo = "";

            String selected = String.valueOf(comTipoReport.getSelectedItem());
            if(selected.equals("Fattura")){
                tipo = "fattura";
            } else if (selected.equals("DDT")){
                tipo = "ddt";
            } else if (selected.equals("Ordine")){
                tipo = "ordine";
            } else if (selected.equals("Fattura Accompagnatoria")){
                tipo = "fattura_acc";
            }

            tipo += "_" + texNome.getText() + ".jrxml";
            String nomeFile = tipo;
            System.out.println("nomeFile: " + nomeFile);

            File toSave = new File(Reports.DIR_REPORTS + Reports.DIR_FATTURE + nomeFile);

            if (toSave.exists()) {
                System.out.println("elimino: " + toSave);
                toSave.delete();
            }

            if (!toSave.exists()) {
                toSave.createNewFile();
            }

            FileInputStream fis = new FileInputStream(report);
            FileOutputStream fos = new FileOutputStream(toSave);

            int readByte = fis.read();
            while (readByte > 0) {
                fos.write(readByte);
                fos.flush();
                readByte = fis.read();
            }

            if(selected.equals("Fattura")){
                main.fileIni.setValue("pref", "tipoStampa", nomeFile);
                padre.preparaTipoStampa(padre.comTipoStampa1, 0);
                padre.comTipoStampa1.setSelectedItem(main.fileIni.getValue("pref", "tipoStampa", null));
            } else if (selected.equals("DDT")){
                main.fileIni.setValue("pref", "tipoStampaDDT", nomeFile);
                padre.preparaTipoStampa(padre.comTipoStampaDdt, 2);
                padre.comTipoStampaDdt.setSelectedItem(main.fileIni.getValue("pref", "tipoStampaDDT", null));
            } else if (selected.equals("Ordine")){
                main.fileIni.setValue("pref", "tipoStampaOrdine", nomeFile);
                padre.preparaTipoStampa(padre.comTipoStampaOrdine, 3);
                padre.comTipoStampaOrdine.setSelectedItem(main.fileIni.getValue("pref", "tipoStampaOrdine", null));
            } else if (selected.equals("Fattura Accompagnatoria")){
                main.fileIni.setValue("pref", "tipoStampaFA", nomeFile);
                padre.preparaTipoStampa(padre.comTipoStampa, 1);
                padre.comTipoStampa.setSelectedItem(main.fileIni.getValue("pref", "tipoStampaFA", null));
            }

            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            JOptionPane.showMessageDialog(this, "Report importato correttamente", "Esecuzione terminata", JOptionPane.INFORMATION_MESSAGE);
            
            this.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
}//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comTipoReport;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField texNome;
    // End of variables declaration//GEN-END:variables

}
