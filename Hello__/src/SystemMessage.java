import javax.swing.*;
import java.awt.*;

public class SystemMessage extends JPanel {

    public SystemMessage(String message) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.CENTER));
        setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel label = new JLabel(message);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(255, 255, 255, 245));

        add(label);
        setMaximumSize(new Dimension(690, 38));
    }
}