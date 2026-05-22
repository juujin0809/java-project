import javax.swing.*;
import java.awt.*;

public class ChatBubble extends JPanel {

    public ChatBubble(String sender, String message, String time, boolean isMine) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Font bubbleFont = new Font("Apple SD Gothic Neo", Font.BOLD, 14);
        FontMetrics fm = getFontMetrics(bubbleFont);

        int maxTextWidth = 270;
        int textWidth = fm.stringWidth(message);
        int bubbleWidth = Math.min(Math.max(textWidth + 44, 95), maxTextWidth + 44);

        JLabel nameLabel = new JLabel(sender);
        nameLabel.setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 12));
        nameLabel.setForeground(new Color(85, 85, 100));

        JPanel nameLine = new JPanel();
        nameLine.setOpaque(false);
        nameLine.setLayout(new BoxLayout(nameLine, BoxLayout.X_AXIS));
        nameLine.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        nameLine.setMaximumSize(new Dimension(410, 20));

        if (isMine) {
            nameLine.add(Box.createHorizontalGlue());
            nameLine.add(nameLabel);
        } else {
            nameLine.add(nameLabel);
            nameLine.add(Box.createHorizontalGlue());
        }

        JTextArea messageArea = new JTextArea(message);
        messageArea.setFont(bubbleFont);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
        messageArea.setForeground(new Color(75, 75, 90));
        messageArea.setSize(new Dimension(bubbleWidth, Short.MAX_VALUE));

        Dimension textSize = messageArea.getPreferredSize();

        LiquidPanel bubble = new LiquidPanel(24, new Color(255, 255, 255, 235));
        bubble.setLayout(new BorderLayout());
        bubble.add(messageArea, BorderLayout.CENTER);
        bubble.setPreferredSize(new Dimension(bubbleWidth, textSize.height + 10));
        bubble.setMaximumSize(new Dimension(bubbleWidth, textSize.height + 10));

        JPanel bubbleLine = new JPanel();
        bubbleLine.setOpaque(false);
        bubbleLine.setLayout(new BoxLayout(bubbleLine, BoxLayout.X_AXIS));
        bubbleLine.setBorder(BorderFactory.createEmptyBorder(3, 10, 2, 10));
        bubbleLine.setMaximumSize(new Dimension(410, bubble.getPreferredSize().height + 12));

        if (isMine) {
            bubbleLine.add(Box.createHorizontalGlue());
            bubbleLine.add(bubble);
        } else {
            bubbleLine.add(bubble);
            bubbleLine.add(Box.createHorizontalGlue());
        }

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Apple SD Gothic Neo", Font.PLAIN, 11));
        timeLabel.setForeground(new Color(120, 120, 135));

        JPanel timeLine = new JPanel();
        timeLine.setOpaque(false);
        timeLine.setLayout(new BoxLayout(timeLine, BoxLayout.X_AXIS));
        timeLine.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));
        timeLine.setMaximumSize(new Dimension(410, 22));

        if (isMine) {
            timeLine.add(Box.createHorizontalGlue());
            timeLine.add(timeLabel);
        } else {
            timeLine.add(timeLabel);
            timeLine.add(Box.createHorizontalGlue());
        }

        add(nameLine);
        add(bubbleLine);
        add(timeLine);

        setMaximumSize(new Dimension(420, bubble.getPreferredSize().height + 65));
    }
}