/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobilsignclient;

import javax.swing.ImageIcon;

/**
 *
 * @author Michal
 */
public class GUI extends javax.swing.JFrame {
    
    private MobilSignClient client;
    /**
     * Creates new form GUI
     */
    public GUI() {
        initComponents();
        client = new MobilSignClient("localhost", 2002, taConsole);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        QRWindow = new javax.swing.JFrame();
        lblQR = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        taConsole = new javax.swing.JTextArea();
        btnSend = new javax.swing.JButton();
        tfSend = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        miExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        miGenerateQR = new javax.swing.JMenuItem();

        QRWindow.setMinimumSize(new java.awt.Dimension(755, 780));
        QRWindow.setResizable(false);

        javax.swing.GroupLayout QRWindowLayout = new javax.swing.GroupLayout(QRWindow.getContentPane());
        QRWindow.getContentPane().setLayout(QRWindowLayout);
        QRWindowLayout.setHorizontalGroup(
            QRWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(QRWindowLayout.createSequentialGroup()
                .addComponent(lblQR)
                .addGap(0, 400, Short.MAX_VALUE))
        );
        QRWindowLayout.setVerticalGroup(
            QRWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(QRWindowLayout.createSequentialGroup()
                .addComponent(lblQR)
                .addGap(0, 300, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        taConsole.setEditable(false);
        taConsole.setColumns(20);
        taConsole.setRows(5);
        jScrollPane1.setViewportView(taConsole);

        btnSend.setText("Send");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        jMenu1.setText("File");

        miExit.setText("Exit");
        miExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miExitActionPerformed(evt);
            }
        });
        jMenu1.add(miExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Generate");

        miGenerateQR.setText("QR code");
        miGenerateQR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miGenerateQRActionPerformed(evt);
            }
        });
        jMenu2.add(miGenerateQR);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 664, Short.MAX_VALUE)
                .addComponent(tfSend, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSend))
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSend)
                    .addComponent(tfSend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void miExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miExitActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_miExitActionPerformed

    private void miGenerateQRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miGenerateQRActionPerformed

        QRWindow.setVisible(true);
        lblQR.setIcon(new ImageIcon(client.getQrCode(client.getMobileKey().getModulus()))); // nastavi QR kod na GUI 
        
        client.pairRequest();
    }//GEN-LAST:event_miGenerateQRActionPerformed
    
    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed

        client.sendMessage(tfSend.getText());
        tfSend.setText("");
    }//GEN-LAST:event_btnSendActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GUI().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFrame QRWindow;
    private javax.swing.JButton btnSend;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblQR;
    private javax.swing.JMenuItem miExit;
    private javax.swing.JMenuItem miGenerateQR;
    private javax.swing.JTextArea taConsole;
    private javax.swing.JTextField tfSend;
    // End of variables declaration//GEN-END:variables
}
