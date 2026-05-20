package components;

import javax.swing.*;
import java.awt.*;

public class GlassSmallButton extends JButton {
    public GlassSmallButton(String text) {
        super(text);
        setFont(new Font("SansSerif", Font.BOLD, 15));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(255, 255, 255, 80));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);

        g2.setColor(new Color(255, 205, 245, 160));
        g2.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 18, 18);

        super.paintComponent(g);
    }
}