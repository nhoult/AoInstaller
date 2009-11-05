/*
 * Credit goes to:
 * http://www.coderanch.com/t/458147/Swing-AWT-SWT-JFace/java/Redirect-output-from-stderr-stdout#2043387
 * Rob Prime
 *
 */

package aoinstaller.gui;


import java.io.Writer;
import javax.swing.JTextArea;





public class TextAreaWriter extends Writer
{
    protected final JTextArea text;

    public TextAreaWriter(JTextArea text)
    {
        if (text == null)
        {
            throw new NullPointerException();
        }
        this.text = text;
    }

    @Override
    public void write(char[] c, int off, int len)
    {
        text.append(new String(c, off, len));
    }

    @Override
    public void flush()
    {
        // does nothing
    }

    @Override
    public void close()
    {
        // does nothing
    }
}