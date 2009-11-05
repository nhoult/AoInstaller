/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.gui;

import aoinstaller.backend.FileInfo;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

/**
 *
 * @author nhoult
 */
public class DownloadThread extends Thread {
    private Boolean cancel = new Boolean(false);
    private final Object cancelMutex = new Object();
    private List<FileInfo> files = null;
    private FileInfo currentFi = null;
    private final Object currentFiMutex = new Object();
    private JButton updateButton = null;
    private JButton downloadButton = null;
    private JButton cancelButton = null;

    public DownloadThread(List<FileInfo> files, JButton updateButton, JButton downloadButton, JButton cancelButton){
        this.files = files;
        this.updateButton = updateButton;
        this.downloadButton = downloadButton;
        this.cancelButton = cancelButton;
        // start on creation
        this.start();
    }

    @Override
    public void run(){
        if(files != null){
            updateButton.setEnabled(false);
            downloadButton.setEnabled(false);
            cancelButton.setEnabled(true);

            for(FileInfo fi: files){
                // so I can make cacneling faster
                synchronized(currentFiMutex){
                    currentFi = fi;
                }

                boolean myCancel = false;
                synchronized(cancelMutex){
                    myCancel = cancel;
                }
                if(!myCancel){
                    try {
                        fi.downloadToFile();
                    } catch (IOException ex) {
                        Logger.getLogger(DownloadThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            updateButton.setEnabled(true);
            downloadButton.setEnabled(true);
            cancelButton.setEnabled(false);
            synchronized(cancelMutex){
                cancel = false;
            }
        }
    }

    public void cancel(){
        // stop the loop of going to the next file
        synchronized(cancelMutex){
            cancel = true;
        }
        // stop the current d/l
        synchronized(currentFiMutex) {
            currentFi.cancel();
        }
    }
}
