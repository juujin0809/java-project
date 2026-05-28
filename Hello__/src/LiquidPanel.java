import javax.swing.*;
import java.awt.*;

public class LiquidPanel extends JPanel {
    private int radius;
    private Color color;

    public LiquidPanel(int radius, Color color) {
        this.radius = radius;
        this.color = color;
        setOpaque(false);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 부드러운 그림자
        g2.setColor(new Color(185, 170, 200, 35));
        g2.fillRoundRect(10, 12, getWidth() - 20, getHeight() - 22, radius, radius);

        // 반투명 유리 카드
        g2.setColor(color);
        g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, radius, radius);

        // 밝은 외곽선
        g2.setColor(new Color(255, 255, 255, 210));
        g2.drawRoundRect(1, 1, getWidth() - 10, getHeight() - 10, radius, radius);

        // 안쪽 하이라이트
        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawRoundRect(5, 5, getWidth() - 18, getHeight() - 18, Math.max(radius - 6, 10), Math.max(radius - 6, 10));

        g2.dispose();
        super.paintComponent(g);
    }
}