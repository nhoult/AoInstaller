/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.gui;

import aoinstaller.backend.*;

/**
 *
 * @author nhoult
 */
public class TextGUI implements FileInfoCallback{

    private FileInfo fi;

    public TextGUI(FileInfo fi){
        this.fi = fi;
        fi.setCallback(this);
    }

    public void downloadProgress(float percent) {
        System.out.println(fi.getLocalFile() + ": " + percent);
    }

    public void downloadComplete() {
        System.out.println(fi.getLocalFile() + ": Finished");
    }

    public void downloadCanceled() {
        System.out.println(fi.getLocalFile() + ": Canceled");
    }

}
