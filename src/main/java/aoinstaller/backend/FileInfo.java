/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nhoult
 */
public class FileInfo extends Thread {
//    private float progress = 0;
    private int fileSize = 0;
    private int downloadedBytes = 0;
    private String localFile;
    private String remoteURL;
    private File file;
    private URLConnection conn;
    private URL url = null;
    private Boolean cancel = false;
    private final Object cancelMutex = new Object();
    // FILE SIZE
    private int remoteSize = -1;
    private long localSize = -1;
    //MD5
    private String localMD5 = null;
    private String remoteMD5 = null;

    // callback
    private FileInfoCallback callback = null;


    public FileInfo(String localFile, String remoteURL, String remoteMD5) throws IOException {
      this(localFile, remoteURL);
      this.remoteMD5 = remoteMD5;
    }

    public FileInfo(String localFile, String remoteURL) throws IOException {
        this.localFile = localFile;
        this.remoteURL = remoteURL;
        // make the directory
        boolean madeDir = (new File(localFile.substring(0, localFile.lastIndexOf(System.getProperty("file.separator"))))).mkdirs();
        file = new File(localFile); // try again

        url = new URL(remoteURL);
        conn = url.openConnection();
    }

//    private void startConnection() throws IOException{
        
//    }
    
//    private void closeConnection() throws IOException{
        
//    }
    
    public int getRemoteSize(){
        if(remoteSize < 0){
            remoteSize = conn.getContentLength();
        }
        return remoteSize;
    }

    public long getLocalSize(){
        if(localSize < 0){
            localSize = file.length();
        }
        return localSize;
    }

    public boolean isLocalOlder(){
        //long connDate = conn.getDate();
        long connDate = conn.getLastModified();
        long localDate = file.lastModified();
//        System.out.println("connDate["+connDate+"] localDate["+localDate+"]");
        return (connDate > localDate);
    }

    public boolean isSizeDiff(){
        long connFileSize = getRemoteSize();//conn.getContentLength();
        long localFileSize = getLocalSize();//file.length();
//        System.out.println("connFileSize["+connFileSize+"] localFileSize["+localFileSize+"]");
        return (connFileSize != localFileSize);
    }

    public boolean isMd5Diff() 
            throws NoSuchAlgorithmException,
            FileNotFoundException,
            IOException
    {
        if(remoteMD5 == null){
            return false; // I guess so...
        }
        if(localMD5 == null) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fr = new FileInputStream(file);
            byte buffer[] = new byte[8192];
            int read = 0;
            while((read = fr.read(buffer)) > 0){
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
	    BigInteger bigInt = new BigInteger(1, md5sum);
	    localMD5 = bigInt.toString(16);

            fr.close(); // cleanup
        }
        //System.out.println("Remote MD5 ["+remoteMD5+"] Local MD5 ["+localMD5+"]");
        return !remoteMD5.equals(localMD5);
    }

    @Override
    public void run() {
        try {
            downloadToFile();
        } catch (IOException ex) {
            Logger.getLogger(FileInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(callback != null){
             callback.downloadProgress(1.0f);
             callback.downloadComplete();
        }
    }

     public void downloadToFileBackground(){
         this.start();
     }

    public void downloadToFile() throws IOException
    {
        int myRemoteSize = getRemoteSize();
        if(myRemoteSize <= 0){
            myRemoteSize = 1; // possible divide by zero problem..
        }
        InputStream is = conn.getInputStream();
        
        FileOutputStream fw = new FileOutputStream(file);

        byte buffer[] = new byte[8192];
        int read = 0;
        downloadedBytes += read;
        while(((read = is.read(buffer)) > 0) && !getCanceled()){
            fw.write(buffer, 0 , read);
            downloadedBytes += read;
            // update the progress
            if(callback != null){
                float progress = (float)downloadedBytes / (float)myRemoteSize;
                if(progress > 1){
                    progress = 1;
                }
                callback.downloadProgress(progress);

            }
        }

        // cleanup
        fw.flush();
        fw.close();
        is.close();

        if(callback != null){
            if(getCanceled()){
                callback.downloadCanceled();
            } else {
                callback.downloadComplete();
            }
        }

        // reset it to it can be restarted
        synchronized(cancelMutex){
            cancel = false;
        }
    }

    private boolean getCanceled(){
        synchronized(cancelMutex){
            return cancel;// ABORT ABORT!
        }
    }

    public String getLocalFile() {
        return localFile;
    }

    public String getRemoteURL() {
        return remoteURL;
    }

    public void setCallback(FileInfoCallback callback){
        this.callback = callback;
    }

    public void cancel() {
        synchronized(cancelMutex){
            cancel = true;
        }
    }
}
