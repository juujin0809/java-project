import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class PlaceholderTextField extends JTextField {

    private String placeholder;

    public PlaceholderTextField(String placeholder) {
        this.placeholder = placeholder;

        setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 15));

        // 실제 입력 글씨 색
        setForeground(new Color(70, 70, 85));

        // 커서 색
        setCaretColor(new Color(255, 90, 180));

        // 내부 여백
        setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        setOpaque(false);

        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                repaint();
            }

            public void focusLost(FocusEvent e) {
                repaint();
            }
        });

        getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                repaint();
            }

            public void removeUpdate(DocumentEvent e) {
                repaint();
            }

            public void changedUpdate(DocumentEvent e) {
                repaint();
            }
        });
    }

    public String getRealText() {
        return getText().trim();
    }

    public void clearAfterSend() {
        setText("");
        repaint();
    }

    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // 은은한 그림자
        g2.setColor(new Color(0, 0, 0, 10));
        g2.fillRoundRect(4, 5, getWidth() - 8, getHeight() - 8, 36, 36);

        // 메인 배경
        GradientPaint gp = new GradientPaint(
                0, 0,
                new Color(255, 255, 255, 235),

                getWidth(), getHeight(),
                new Color(245, 248, 255, 225)
        );

        g2.setPaint(gp);

        g2.fillRoundRect(
                0,
                0,
                getWidth() - 4,
                getHeight() - 4,
                36,
                36
        );

        // 외곽선
        g2.setColor(new Color(255, 255, 255, 170));

        g2.drawRoundRect(
                0,
                0,
                getWidth() - 5,
                getHeight() - 5,
                36,
                36
        );

        super.paintComponent(g);

        // placeholder
        if (getText().isEmpty()) {

            g2.setFont(new Font("Apple SD Gothic Neo", Font.PLAIN, 14));

            g2.setColor(new Color(150, 150, 165));

            g2.drawString(
                    placeholder,
                    24,
                    getHeight() / 2 + 5
            );
        }
    }
} 