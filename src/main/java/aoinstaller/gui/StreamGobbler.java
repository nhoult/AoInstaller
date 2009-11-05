/*
 * Credit goes to:
 * http://www.coderanch.com/t/458147/Swing-AWT-SWT-JFace/java/Redirect-output-from-stderr-stdout#2043387
 * Rob Prime
 *
 */


package aoinstaller.gui;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.JTextArea;



public class StreamGobbler extends Thread
{
    private final InputStream is;
    private final JTextArea text;

    public StreamGobbler(InputStream is, JTextArea text)
    {
        this.is = is;
        this.text = text;
    }

    @Override
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
            {
                text.append(line + "\n"); // JTextArea.append is thread safe
            }
        }
        catch (IOException ioe)
        {
            text.append(ioe.toString()); // note below
            ioe.printStackTrace();
        }
    }
}
