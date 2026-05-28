import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class PlaceholderPasswordField extends JPasswordField {
    private String placeholder;

    public PlaceholderPasswordField(String placeholder) {
        this.placeholder = placeholder;

        setFont(new Font("SansSerif", Font.BOLD, 16));
        setForeground(new Color(255, 255, 255, 0));
        setCaretColor(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 22));
        setOpaque(false);
        setEchoChar('•');

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

    public String getRealPassword() {
        return new String(getPassword()).trim();
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(255, 255, 255, 150));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);

        g2.setColor(new Color(255, 255, 255, 230));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);

        super.paintComponent(g);

        String pw = new String(getPassword());

        if (pw.isEmpty() && !isFocusOwner()) {
            UIUtil.drawOutlinedText(g2, placeholder, 22, 35);
        } else if (!pw.isEmpty()) {
            String dots = UIUtil.repeatText("•", pw.length());
            UIUtil.drawOutlinedText(g2, dots, 22, 35);
        }
    }
}