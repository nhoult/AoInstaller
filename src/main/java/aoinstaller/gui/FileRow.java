/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.gui;

import aoinstaller.backend.FileInfo;
import aoinstaller.backend.FileInfoCallback;
import javax.swing.JProgressBar;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nhoult
 */
public class FileRow implements FileInfoCallback {
    private static int PROGRESS_POS = 2;

    private String fileName = null;
//    private JProgressBar progress = new JProgressBar(0, 100);
    private int progress = 0;

//    private FileInfo fi = null;
    private DefaultTableModel dtm = null;
    private Object data[] = new Object[PROGRESS_POS+1];
    private int myRowNum = 0;

    public FileRow(FileInfo fi, DefaultTableModel dtm){
//        this.fi = fi;
        this.setFileName(fi.getLocalFile());
        fi.setCallback(this);
        this.dtm = dtm;

        
        data[0] = fi.getLocalFile();
        data[1] = fi.getRemoteSize()/1024+"kb";
        data[PROGRESS_POS] = new Integer(0);
        dtm.addRow(data);
        myRowNum = dtm.getRowCount() - 1;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void downloadProgress(float percent) {
        //throw new UnsupportedOperationException("Not supported yet.");
        //progress.setValue((int) (percent * 100));
       Integer progress = new Integer((int) (percent * 100));
       // data[1] = progress
        //System.out.println(progress);
        dtm.setValueAt(progress, myRowNum, PROGRESS_POS);
        //dtm.fireTableDataChanged();
    }

    public void downloadComplete() {
        dtm.setValueAt(100, myRowNum, PROGRESS_POS);
    }

    public void downloadCanceled() {
        dtm.setValueAt(-1, myRowNum, PROGRESS_POS);
    }

    
}
