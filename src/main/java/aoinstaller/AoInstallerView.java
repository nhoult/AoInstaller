/*
 * AoInstallerView.java
 */

package aoinstaller;

import aoinstaller.backend.FileInfo;
import aoinstaller.backend.FilesToDiff;
import aoinstaller.backend.LocalConfiguration;
import aoinstaller.gui.DownloadThread;
import aoinstaller.gui.FileRow;
import aoinstaller.gui.ProgressRenderer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.CookieHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.xml.sax.SAXException;

/**
 * The application's main frame.
 */
public class AoInstallerView extends FrameView {

    public AoInstallerView(SingleFrameApplication app) {
        super(app);

        initComponents();

        console = new Console();
        console.setVisible(false);

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

//        System.setProperty("javawebstart.version", "javaws-1.0.1-ea");
//        System.setProperty("AoInstaller.appName", "AoSpaceInvaders");
//        System.setProperty("AoInstaller.sourceURL", "http://hoult.selfip.com/~nhoult/AoSpaceInvaders.xml");

        try {
            config = new LocalConfiguration();
            saveDir(config.getLocalDir());
            remoteURL = config.getRemoteURL();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AoInstallerView.class.getName()).log(Level.WARNING, null, ex);
            showConsole();
            console.setError(true);
        } catch (IOException ex) {
            Logger.getLogger(AoInstallerView.class.getName()).log(Level.WARNING, null, ex);
            showConsole();
            console.setError(true);
        }

        DefaultTableModel dtm = (DefaultTableModel) updateProgressTable.getModel();
        TableColumn column = updateProgressTable.getColumnModel().getColumn(2);
        column.setCellRenderer(new ProgressRenderer());

        // get any system properties
//        if(System.getProperty("aoinstall.isWebStart") != null){
//            isWebStart = Boolean.parseBoolean(System.getProperty("aoinstall.isWebStart"));
//        }
//        saveDir(System.getProperty("user.dir"));
    }

    private void saveDir(String dir){
        if(dir != null){
             saveDir = dir;
            labelSaveDir.setText(saveDir);
            try {
                config.setLocalDir(saveDir);
                config.save();
            } catch (IOException ex) {
                Logger.getLogger(AoInstallerView.class.getName()).log(Level.WARNING, null, ex);
                showConsole();
                console.setError(true);
            }
        }
    }

    @Action
    public void showConsole(){
        console.setVisible(true);
        console.setAlwaysOnTop(true);
    }

    @Action
    public void chooseSaveDir(){
        JFileChooser chooser = new JFileChooser("");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog((java.awt.Component) null);

        if ( returnVal == chooser.APPROVE_OPTION ) {

          //  java.io.File inFile = chooser.getSelectedFile();
          //  processFile(inFile);
          saveDir( chooser.getSelectedFile().getAbsolutePath() );
          
        }
    }

    @Action
    public void cancelUpdates(){
        if(dt != null){
            dt.cancel();
        }
    }

    @Action
    public void downloadUpdates(){
        if(files == null) {
            checkForUpdates();
        }
        if(files != null && saveDir != null ){
            dt = new DownloadThread(files, buttonCheckForUpdates, buttonGetUpdates, buttonCancel);
        } else {
            // BAD!!!
        }
    }
    
    @Action
    public void checkForUpdates() {
        if(saveDir == null){
            chooseSaveDir();
            if(saveDir == null){
                return;
            }
        }

        DefaultTableModel dtm = (DefaultTableModel) updateProgressTable.getModel();
//        if(files != null){
            for(int i = dtm.getRowCount(); i >= 1; --i){
                dtm.removeRow(0);
            }
//        }
        FilesToDiff ftd;
        files = new ArrayList<FileInfo>();
        try {
            ftd = new FilesToDiff(remoteURL, saveDir);
            files = ftd.getFilesToUpdate();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(AoInstallerView.class.getName()).log(Level.WARNING, null, ex);
            showConsole();
            console.setError(true);
        } catch (SAXException ex) {
            Logger.getLogger(AoInstallerView.class.getName()).log(Level.WARNING, null, ex);
            showConsole();
            console.setError(true);
        } catch (IOException ex) {
            Logger.getLogger(AoInstallerView.class.getName()).log(Level.WARNING, null, ex);
            showConsole();
            console.setError(true);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AoInstallerView.class.getName()).log(Level.WARNING, null, ex);
            showConsole();
            console.setError(true);
        }
        
        
        for(FileInfo fi: files){
            // glue between backend and table
            new FileRow(fi, dtm);
        }
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = AoInstallerApp.getApplication().getMainFrame();
            aboutBox = new AoInstallerAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        AoInstallerApp.getApplication().show(aboutBox);
    }

    String retrieveCookie(URL url) throws IOException, URISyntaxException
    {
     String cookieValue = null;

     CookieHandler handler = CookieHandler.getDefault();
     if (handler != null)    {
          Map<String, List<String>> headers = handler.get(url.toURI(), new HashMap<String,                                                           List<String>>());
          List<String> values = headers.get("Cookie");
          for (Iterator<String> iter=values.iterator(); iter.hasNext();) {
               String v = iter.next();

                if (cookieValue == null)
                    cookieValue = v;
                else
                    cookieValue = cookieValue + ";" + v;
            }
        }
        return cookieValue;
    }

    void setCookie(URL url, String value) throws IOException, URISyntaxException
    {
        CookieHandler handler = CookieHandler.getDefault();
        if (handler != null)    {
              Map<String, List<String>> headers= new HashMap<String, List<String>>();
              List<String> values = new Vector<String>();
              values.add(value);
              headers.put("Cookie", values);

              handler.put(url.toURI(), headers);
        }
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        updateProgressPane = new javax.swing.JScrollPane();
        updateProgressTable = new javax.swing.JTable();
        buttonCheckForUpdates = new javax.swing.JButton();
        buttonGetUpdates = new javax.swing.JButton();
        labelSaveDir = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonSaveTo = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        checkForUpdates = new javax.swing.JMenuItem();
        downloadUpdates = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        consoleMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        updateProgressPane.setName("updateProgressPane"); // NOI18N

        updateProgressTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "File Size", "Progress"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        updateProgressTable.setName("updateProgressTable"); // NOI18N
        updateProgressPane.setViewportView(updateProgressTable);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(aoinstaller.AoInstallerApp.class).getContext().getResourceMap(AoInstallerView.class);
        updateProgressTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("updateProgressTable.columnModel.title0")); // NOI18N
        updateProgressTable.getColumnModel().getColumn(1).setResizable(false);
        updateProgressTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        updateProgressTable.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("updateProgressTable.columnModel.title2")); // NOI18N
        updateProgressTable.getColumnModel().getColumn(2).setResizable(false);
        updateProgressTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        updateProgressTable.getColumnModel().getColumn(2).setHeaderValue(resourceMap.getString("updateProgressTable.columnModel.title1")); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(aoinstaller.AoInstallerApp.class).getContext().getActionMap(AoInstallerView.class, this);
        buttonCheckForUpdates.setAction(actionMap.get("checkForUpdates")); // NOI18N
        buttonCheckForUpdates.setText(resourceMap.getString("buttonCheckForUpdates.text")); // NOI18N
        buttonCheckForUpdates.setName("buttonCheckForUpdates"); // NOI18N

        buttonGetUpdates.setAction(actionMap.get("downloadUpdates")); // NOI18N
        buttonGetUpdates.setText(resourceMap.getString("buttonGetUpdates.text")); // NOI18N
        buttonGetUpdates.setName("buttonGetUpdates"); // NOI18N

        labelSaveDir.setText(resourceMap.getString("labelSaveDir.text")); // NOI18N
        labelSaveDir.setName("labelSaveDir"); // NOI18N

        buttonCancel.setAction(actionMap.get("cancelUpdates")); // NOI18N
        buttonCancel.setText(resourceMap.getString("buttonCancel.text")); // NOI18N
        buttonCancel.setName("buttonCancel"); // NOI18N

        buttonSaveTo.setAction(actionMap.get("chooseSaveDir")); // NOI18N
        buttonSaveTo.setText(resourceMap.getString("buttonSaveTo.text")); // NOI18N
        buttonSaveTo.setName("buttonSaveTo"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                        .addComponent(buttonCheckForUpdates)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonGetUpdates)
                        .addGap(6, 6, 6)
                        .addComponent(buttonCancel))
                    .addComponent(buttonSaveTo, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap(202, Short.MAX_VALUE))
            .addComponent(updateProgressPane, javax.swing.GroupLayout.DEFAULT_SIZE, 529, Short.MAX_VALUE)
            .addComponent(labelSaveDir, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 529, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addComponent(updateProgressPane, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonSaveTo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelSaveDir, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCheckForUpdates)
                    .addComponent(buttonGetUpdates)
                    .addComponent(buttonCancel))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setBorder(null);
        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        checkForUpdates.setAction(actionMap.get("checkForUpdates")); // NOI18N
        checkForUpdates.setText(resourceMap.getString("checkForUpdates.text")); // NOI18N
        checkForUpdates.setName("checkForUpdates"); // NOI18N
        fileMenu.add(checkForUpdates);

        downloadUpdates.setAction(actionMap.get("downloadUpdates")); // NOI18N
        downloadUpdates.setText(resourceMap.getString("downloadUpdates.text")); // NOI18N
        downloadUpdates.setName("downloadUpdates"); // NOI18N
        fileMenu.add(downloadUpdates);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        consoleMenuItem.setAction(actionMap.get("showConsole")); // NOI18N
        consoleMenuItem.setText(resourceMap.getString("consoleMenuItem.text")); // NOI18N
        consoleMenuItem.setName("consoleMenuItem"); // NOI18N
        helpMenu.add(consoleMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setPreferredSize(new java.awt.Dimension(529, 30));

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(statusPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(statusMessageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 337, Short.MAX_VALUE)
                        .addComponent(statusAnimationLabel)
                        .addGap(30, 30, 30))
                    .addGroup(statusPanelLayout.createSequentialGroup()
                        .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonCheckForUpdates;
    private javax.swing.JButton buttonGetUpdates;
    private javax.swing.JButton buttonSaveTo;
    private javax.swing.JMenuItem checkForUpdates;
    private javax.swing.JMenuItem consoleMenuItem;
    private javax.swing.JMenuItem downloadUpdates;
    private javax.swing.JLabel labelSaveDir;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JScrollPane updateProgressPane;
    private javax.swing.JTable updateProgressTable;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;


    private List<FileInfo> files = null;
    private DownloadThread dt = null;
    private String saveDir = null;
    private String remoteURL = null;

    private boolean isWebStart = false;

    private LocalConfiguration config = null;
    private Console console = null;
}
