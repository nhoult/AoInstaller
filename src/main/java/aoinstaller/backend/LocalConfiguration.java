/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.backend;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;



/**
 *
 * @author nhoult
 */
public class LocalConfiguration {

    private String saveFile = null;
    private String saveDir = ".";
    private File file = null;
    private Properties prop = new Properties();

    private boolean webStart = false;
//    private String localDir;
//    private String remoteURL;

    public LocalConfiguration() throws IOException{

        if(System.getProperty("AoInstaller.sourceURL") != null &&
                System.getProperty("AoInstaller.appName") != null)
        {
            System.out.println("Webstart!");
            webStart = true;
        }
        
        if(webStart){
            saveDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".AoInstaller";
            saveFile = saveDir + System.getProperty("file.separator") + System.getProperty("AoInstaller.appName") + ".conf";
            // make the directory
            ( new File(saveDir) ).mkdirs();
        } else {
            // directory is assumed to be user.dir
            saveFile = System.getProperty("user.dir") + System.getProperty("file.separator") + ".AoInstaller.conf";
        }

        // read in savefile
        file = new File(saveFile);
        file.createNewFile();
        FileReader fr = new FileReader(file);
        prop.load(fr);
        fr.close();

        // override what exsists
        if(webStart){ // if it is webstart then we tell it where to fetch from
            setRemoteURL(System.getProperty("AoInstaller.sourceURL"));
        } else { // if it is local then if they moved the directory we still want local
            setLocalDir(System.getProperty("user.dir"));
        }

    }

 

    public String getRemoteURL(){
        return prop.getProperty("RemoteURL");
    }

    public String getLocalDir(){
        return prop.getProperty("LocalDir");
    }

    public void setRemoteURL(String remoteURL){
        prop.setProperty("RemoteURL", remoteURL);
    }

    public void setLocalDir(String localDir){
        prop.setProperty("LocalDir", localDir);
    }

    public void save() throws IOException{
        FileWriter fw = new FileWriter(file);
        prop.store(fw, "");
        fw.flush();
        fw.close();

    }
}
