/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.backend;

/**
 *
 * @author nhoult
 */
public interface FileInfoCallback {
    public void downloadProgress(float percent);
    public void downloadComplete();
    public void downloadCanceled();
}
