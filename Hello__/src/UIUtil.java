import java.awt.*;

public class UIUtil {

	public static void drawOutlinedText(Graphics2D g2, String text, int x, int y) {
	    g2.setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 16));
	    g2.setColor(new Color(70, 70, 85));
	    g2.drawString(text, x, y);
	}
    
    public static String repeatText(String text, int count) {
        String result = "";
        for (int i = 0; i < count; i++) {
            result += text;
        }
        return result;
    }
}
