/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.backend;

import aoinstaller.gui.TextGUI;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author nhoult
 */
public class FilesToDiff {
    private List<FileInfo> files = new ArrayList<FileInfo>();
    private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private Document dom = null;
    private String installPath = null;

    public FilesToDiff(String url, String installPath)
            throws ParserConfigurationException,
            SAXException,
            IOException,
            NoSuchAlgorithmException
    {
        this.installPath = installPath;
        //Using factory get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();
        //parse using builder to get DOM representation of the XML file
        InputStream is = new URL(url).openStream();
        dom = db.parse(is);
        is.close();

        populateFilesToComp();
    }

    public List<FileInfo> getFilesToUpdate()
            throws NoSuchAlgorithmException,
            FileNotFoundException,
            IOException
    {
        List<FileInfo> filesToUpdate = new ArrayList<FileInfo>();
        for(FileInfo fi: files){
           if(fi.isSizeDiff() || fi.isMd5Diff()){
               filesToUpdate.add(fi);
           }
        }
        return filesToUpdate;
    }

    private void populateFilesToComp() throws IOException{
        NodeList nl = dom.getElementsByTagName("File");
	if(nl != null && nl.getLength() > 0) {
            for(int i = 0 ; i < nl.getLength();i++) {
		//get the file element
		Element el = (Element)nl.item(i);

                String localFile = installPath + System.getProperty("file.separator") + el.getAttribute("local");
                String url = el.getAttribute("remote");
                if(el.hasAttribute("MD5")){
                    String MD5 = el.getAttribute("MD5");
                    files.add(new FileInfo(localFile, url, MD5));
                } else {
//                  System.out.println("local[" + localFile + "] remote[" + url +"]");
                    files.add(new FileInfo(localFile, url));
                }
            }
	}
    }

    public static void main(String[] args)
            throws ParserConfigurationException,
            SAXException,
            IOException,
            NoSuchAlgorithmException
    {
        FilesToDiff ftd = new FilesToDiff("http://hoult.selfip.com/~nhoult/AoSpaceInvaders.xml", "/tmp/AoInstall");
        List<FileInfo> files = ftd.getFilesToUpdate();
        
        for(FileInfo fi: files){
            TextGUI gui = new TextGUI(fi);
            fi.downloadToFile();
        }
    }
}
