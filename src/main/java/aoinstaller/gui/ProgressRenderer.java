/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package aoinstaller.gui;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author nhoult
 */
public class ProgressRenderer extends DefaultTableCellRenderer {
    private final JProgressBar b = new JProgressBar(0, 100);
    public ProgressRenderer() {
        super();
        setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Integer i = (Integer)value;
        String text = "Done";
        if(i<0) {
            text = "Canceled";
        }else if(i<100) {
            b.setValue(i);
            return b;
        }
        super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        return this;
    }
}
