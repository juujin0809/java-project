import javax.swing.*;
import java.awt.*;

public class SystemMessage extends JPanel {

    public SystemMessage(String message) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.CENTER));
        setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel label = new JLabel(message);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        //수정 사항 !!
        label.setForeground(new Color(85, 85, 100));

        add(label);
        setMaximumSize(new Dimension(690, 38));
    }
}