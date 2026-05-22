import javax.swing.*;
import java.awt.*;

public class LiquidButton extends JButton {

    public LiquidButton(String text) {
        super(text);
        setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 16));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 12, 0, 12));
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 버튼 그림자
        g2.setColor(new Color(255, 90, 160, 55));
        g2.fillRoundRect(4, 5, getWidth() - 8, getHeight() - 7, 28, 28);

        // 핑크 그라데이션 버튼
        GradientPaint gp = new GradientPaint(
                0, 0, new Color(255, 112, 195),
                getWidth(), getHeight(), new Color(255, 70, 135)
        );

        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, getWidth(), getHeight() - 3, 28, 28);

        // 위쪽 하이라이트
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 7, 26, 26);

        g2.dispose();
        super.paintComponent(g);
    }
}