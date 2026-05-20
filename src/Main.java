import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


import components.LiquidButton;
import components.UserDAO;
import components.LiquidPanel;
import components.GlassSmallButton;
import components.SystemMessage;
import components.ChatBubble;
import components.PlaceholderTextField;
import components.PlaceholderPasswordField;

public class Main extends JFrame {

    
    private DefaultListModel<String> friendModel = new DefaultListModel<>();
    private DefaultListModel<String> roomModel = new DefaultListModel<>();
    private WhisperHandler whisperHandler = new WhisperHandler();
    private UserDAO userDAO = new UserDAO();

    public Main() {
        roomModel.addElement("자바 팀프로젝트 방");
        roomModel.addElement("과제 질문방");
        roomModel.addElement("B조 회의방");
        roomModel.addElement("공지방");
        showLoginScreen();
    }

    private Font titleFont(int size) {
        return new Font("Arial Rounded MT Bold", Font.BOLD, size);
    }

    private Font mainFont(int style, int size) {
        return new Font("Apple SD Gothic Neo", style, size);
    }
    
    private JLabel outlinedLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(titleFont(size));
        label.setForeground(new Color(35, 35, 45));
        return label;
    }
   

    private void showLoginScreen() {
        setTitle("Messenger Login");
        setSize(900, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = createLiquidBackground();
        
        
        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 15));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);
        card.setFocusable(true);

        JLabel smallTitle = outlinedLabel("Welcome back", 16);
        smallTitle.setBounds(160, 70, 300, 35);

        JLabel title = outlinedLabel("Messenger", 52);
        title.setBounds(160, 105, 460, 75);

        JLabel subTitle = outlinedLabel("Sign up first, then log in.", 16);
        subTitle.setFont(mainFont(Font.BOLD, 16));
        subTitle.setBounds(160, 178, 400, 35);

        PlaceholderTextField idField = new PlaceholderTextField("User ID");
        idField.setBounds(160, 240, 360, 55);

        PlaceholderPasswordField pwField = new PlaceholderPasswordField("Password");
        pwField.setBounds(160, 315, 360, 55);

        LiquidButton loginButton = new LiquidButton("Login");
        loginButton.setBounds(160, 390, 360, 55);

        JLabel signupText = outlinedLabel("First time here?", 15);
        signupText.setFont(mainFont(Font.BOLD, 15));
        signupText.setBounds(185, 445, 190, 35);

        GlassSmallButton signupButton = new GlassSmallButton("Sign Up");
        signupButton.setBounds(360, 444, 140, 34);

        loginButton.addActionListener(e -> {
            String id = idField.getRealText();
            String pw = pwField.getRealPassword();

            if (id.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 입력하세요.");
                return;
            }

            if (!userDAO.exists(id)) {
                JOptionPane.showMessageDialog(this, "가입되어 있지 않은 유저입니다. 회원가입을 먼저 해주세요.");
                return;
            }

            if (!userDAO.login(id, pw)) {
                JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
                return;
            }

            fadeToScreen(() -> showMainScreen(id));
        });

        signupButton.addActionListener(e -> fadeToScreen(() -> showSignupScreen()));

        card.add(smallTitle);
        card.add(title);
        card.add(subTitle);
        card.add(idField);
        card.add(pwField);
        card.add(loginButton);
        card.add(signupText);
        card.add(signupButton);

        background.add(card);
        setContentPane(background);
        setVisible(true);

        SwingUtilities.invokeLater(() -> card.requestFocusInWindow());
    }

    private void showSignupScreen() {
        JPanel background = createLiquidBackground();

        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 150));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);
        card.setFocusable(true);

        JButton backButton = new JButton("← Login");
        backButton.setBounds(145, 65, 100, 30);
        backButton.setFont(mainFont(Font.BOLD, 14));
        backButton.setForeground(Color.WHITE);
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel title = outlinedLabel("Create Account", 44);
        title.setBounds(160, 115, 460, 70);

        JLabel subTitle = outlinedLabel("Create your User ID to start chatting.", 16);
        subTitle.setFont(mainFont(Font.BOLD, 16));
        subTitle.setBounds(160, 185, 460, 35);

        PlaceholderTextField idField = new PlaceholderTextField("New User ID");
        idField.setBounds(160, 250, 360, 55);

        PlaceholderPasswordField pwField = new PlaceholderPasswordField("New Password");
        pwField.setBounds(160, 325, 360, 55);

        LiquidButton signupButton = new LiquidButton("Sign Up");
        signupButton.setBounds(160, 405, 360, 58);

        signupButton.addActionListener(e -> {
            String id = idField.getRealText();
            String pw = pwField.getRealPassword();

            if (id.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 모두 입력하세요.");
                return;
            }

            if (userDAO.exists(id)) {
                JOptionPane.showMessageDialog(this, "이미 가입된 아이디입니다.");
                return;
            }

            userDAO.register(id, pw);
            JOptionPane.showMessageDialog(this, "회원가입 완료! 이제 로그인해주세요.");
            fadeToScreen(() -> showLoginScreen());
        });

        backButton.addActionListener(e -> fadeToScreen(() -> showLoginScreen()));

        card.add(backButton);
        card.add(title);
        card.add(subTitle);
        card.add(idField);
        card.add(pwField);
        card.add(signupButton);

        background.add(card);
        setContentPane(background);
        revalidate();
        repaint();

        SwingUtilities.invokeLater(() -> card.requestFocusInWindow());
    }

    private void showMainScreen(String userId) {
        JPanel background = createLiquidBackground();

        LiquidPanel mainCard = new LiquidPanel(40, new Color(255, 255, 255, 105));
        mainCard.setBounds(60, 45, 780, 500);
        mainCard.setLayout(null);

        JLabel title = outlinedLabel("Messenger", 34);
        title.setBounds(40, 25, 300, 55);

        JLabel userLabel = outlinedLabel(userId + "님, 안녕하세요", 15);
        userLabel.setFont(mainFont(Font.BOLD, 15));
        userLabel.setBounds(42, 75, 300, 35);

        GlassSmallButton logoutButton = new GlassSmallButton("Logout");
        logoutButton.setBounds(650, 35, 95, 36);
        logoutButton.addActionListener(e -> fadeToScreen(() -> showLoginScreen()));

        LiquidPanel friendPanel = new LiquidPanel(28, new Color(255, 255, 255, 235));
        friendPanel.setBounds(40, 125, 300, 330);
        friendPanel.setLayout(null);

        JLabel friendTitle = outlinedLabel("Friends", 22);
        friendTitle.setBounds(25, 15, 200, 45);

        JList<String> friendList = new JList<>(friendModel);
        friendList.setFont(mainFont(Font.PLAIN, 16));
        friendList.setFixedCellHeight(45);
        friendList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        friendList.setBackground(new Color(255, 255, 255, 185));
        friendList.setForeground(new Color(35, 30, 70));

        JScrollPane friendScroll = new JScrollPane(friendList);
        friendScroll.setBounds(20, 70, 260, 180);
        friendScroll.setBorder(BorderFactory.createEmptyBorder());

        LiquidButton inviteButton = new LiquidButton("Invite Friend");
        inviteButton.setBounds(20, 270, 260, 45);

        inviteButton.addActionListener(e -> {
            String friendName = JOptionPane.showInputDialog(this, "초대할 친구 이름 또는 ID를 입력하세요.");
            if (friendName == null || friendName.trim().isEmpty()) return;

            friendModel.addElement(friendName.trim());
            JOptionPane.showMessageDialog(this, friendName + "님을 친구 목록에 추가했습니다.");
        });

        friendPanel.add(friendTitle);
        friendPanel.add(friendScroll);
        friendPanel.add(inviteButton);

        LiquidPanel roomPanel = new LiquidPanel(28, new Color(255, 255, 255, 235));
        roomPanel.setBounds(380, 125, 360, 330);
        roomPanel.setLayout(null);

        JLabel roomTitle = outlinedLabel("Chat Rooms", 22);
        roomTitle.setBounds(25, 15, 230, 45);

        JList<String> roomList = new JList<>(roomModel);
        roomList.setFont(mainFont(Font.PLAIN, 16));
        roomList.setFixedCellHeight(50);
        roomList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        roomList.setBackground(new Color(255, 255, 255, 185));
        roomList.setForeground(new Color(35, 30, 70));

        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setBounds(20, 70, 320, 150);
        roomScroll.setBorder(BorderFactory.createEmptyBorder());

        LiquidButton createRoomButton = new LiquidButton("Create Room");
        createRoomButton.setBounds(20, 230, 320, 40);

        createRoomButton.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(this, "생성할 채팅방 이름을 입력하세요.");
            if (roomName == null || roomName.trim().isEmpty()) return;

            roomModel.addElement(roomName.trim());
            JOptionPane.showMessageDialog(this, roomName + " 채팅방이 생성되었습니다.");
        });

        LiquidButton enterButton = new LiquidButton("Enter Chat");
        enterButton.setBounds(20, 280, 320, 40);

        enterButton.addActionListener(e -> {
            String selectedRoom = roomList.getSelectedValue();

            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "채팅방을 선택하세요.");
                return;
            }

            fadeToScreen(() -> showChatScreen(userId, selectedRoom));
        });

        roomPanel.add(roomTitle);
        roomPanel.add(roomScroll);
        roomPanel.add(createRoomButton);
        roomPanel.add(enterButton);

        mainCard.add(title);
        mainCard.add(userLabel);
        mainCard.add(logoutButton);
        mainCard.add(friendPanel);
        mainCard.add(roomPanel);

        background.add(mainCard);
        setContentPane(background);
        revalidate();
        repaint();
    }

    

    private JPanel createLiquidBackground() {
        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();

                GradientPaint base = new GradientPaint(
                        0, 0, new Color(255, 241, 247),
                        w, h, new Color(226, 240, 255)
                );
                g2.setPaint(base);
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(255, 185, 215, 95));
                g2.fillOval(-160, -120, 520, 360);

                g2.setColor(new Color(178, 214, 255, 105));
                g2.fillOval(w - 330, -90, 450, 330);

                g2.setColor(new Color(255, 228, 236, 120));
                g2.fillOval(80, h - 230, 720, 280);

                g2.setColor(new Color(210, 230, 255, 90));
                g2.fillOval(360, 180, 620, 310);

                g2.setColor(new Color(255, 255, 255, 90));
                g2.fillRoundRect(25, 28, w - 50, h - 56, 45, 45);
            }
        };

        background.setLayout(null);
        return background;
    }

    private void fadeToScreen(Runnable nextScreen) {
        Timer timer = new Timer(120, e -> {
            nextScreen.run();
            revalidate();
            repaint();
        });

        timer.setRepeats(false);
        timer.start();
    }
    
    private void showChatScreen(String userId, String roomName) {
        JPanel background = createLiquidBackground();

        LiquidPanel appCard = new LiquidPanel(38, new Color(255, 255, 255, 145));
        appCard.setBounds(25, 35, 840, 535);
        appCard.setLayout(null);

        // 왼쪽 친구 목록 영역
        JPanel friendSide = new JPanel(null);
        friendSide.setOpaque(false);
        friendSide.setBounds(0, 0, 170, 535);

        JLabel userName = new JLabel(userId);
        userName.setFont(mainFont(Font.BOLD, 20));
        userName.setForeground(new Color(35, 35, 45));
        userName.setBounds(28, 30, 110, 25);

        JLabel online = new JLabel("● 온라인");
        online.setFont(mainFont(Font.BOLD, 12));
        online.setForeground(new Color(45, 200, 105));
        online.setBounds(30, 55, 100, 20);

        JLabel friendTitle = new JLabel("친구 목록");
        friendTitle.setFont(mainFont(Font.BOLD, 17));
        friendTitle.setForeground(new Color(40, 40, 50));
        friendTitle.setBounds(28, 115, 100, 25);

        String[] friends = {"Alice   ● 온라인", "Bob     ◐ 자리 비움", "Charlie ○ 오프라인", "David   ● 온라인"};
        JList<String> friendList = new JList<>(friends);
        friendList.setFont(mainFont(Font.PLAIN, 13));
        friendList.setFixedCellHeight(48);
        friendList.setBackground(new Color(255, 255, 255, 0));
        friendList.setForeground(new Color(55, 55, 70));
        friendList.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

        JScrollPane friendScroll = new JScrollPane(friendList);
        friendScroll.setBounds(10, 150, 150, 300);
        friendScroll.setOpaque(false);
        friendScroll.getViewport().setOpaque(false);
        friendScroll.setBorder(BorderFactory.createEmptyBorder());

        friendSide.add(userName);
        friendSide.add(online);
        friendSide.add(friendTitle);
        friendSide.add(friendScroll);

        // 가운데 채팅방 목록 영역
        JPanel roomSide = new JPanel(null);
        roomSide.setOpaque(false);
        roomSide.setBounds(170, 0, 200, 535);

        JLabel roomListTitle = new JLabel("채팅방");
        roomListTitle.setFont(mainFont(Font.BOLD, 18));
        roomListTitle.setForeground(new Color(35, 35, 45));
        roomListTitle.setBounds(25, 35, 100, 30);

        JButton plusRoom = new JButton("+");
        plusRoom.setBounds(150, 32, 38, 38);
        plusRoom.setFont(mainFont(Font.BOLD, 20));
        plusRoom.setForeground(new Color(80, 80, 95));
        plusRoom.setContentAreaFilled(false);
        plusRoom.setBorderPainted(false);
        plusRoom.setFocusPainted(false);
        plusRoom.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JList<String> roomList = new JList<>(roomModel);
        roomList.setFont(mainFont(Font.BOLD, 14));
        roomList.setFixedCellHeight(58);
        roomList.setBackground(new Color(255, 255, 255, 0));
        roomList.setForeground(new Color(50, 50, 65));
        roomList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        roomList.setSelectedValue(roomName, true);

        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setBounds(18, 90, 165, 340);
        roomScroll.setOpaque(false);
        roomScroll.getViewport().setOpaque(false);
        roomScroll.setBorder(BorderFactory.createEmptyBorder());

        plusRoom.addActionListener(e -> {
            String newRoom = JOptionPane.showInputDialog(this, "생성할 채팅방 이름을 입력하세요.");
            if (newRoom == null || newRoom.trim().isEmpty()) return;
            roomModel.addElement(newRoom.trim());
        });

        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && roomList.getSelectedValue() != null) {
                fadeToScreen(() -> showChatScreen(userId, roomList.getSelectedValue()));
            }
        });

        roomSide.add(roomListTitle);
        roomSide.add(plusRoom);
        roomSide.add(roomScroll);

        // 오른쪽 채팅 영역
        JPanel chatArea = new JPanel(null) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(255, 224, 240, 155),
                        getWidth(), getHeight(), new Color(220, 235, 255, 170)
                );

                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
            }
        };
        chatArea.setOpaque(false);
        chatArea.setBounds(370, 0, 470, 535);

        JLabel roomTitle = new JLabel(roomName);
        roomTitle.setFont(mainFont(Font.BOLD, 24));
        roomTitle.setForeground(new Color(30, 30, 42));
        roomTitle.setBounds(30, 25, 270, 35);

        JLabel memberText = new JLabel("참여자 5명");
        memberText.setFont(mainFont(Font.PLAIN, 13));
        memberText.setForeground(new Color(100, 100, 115));
        memberText.setBounds(32, 58, 120, 22);

        GlassSmallButton logoutButton = new GlassSmallButton("Logout");
        logoutButton.setBounds(325, 28, 115, 34);
        logoutButton.addActionListener(e -> fadeToScreen(() -> showLoginScreen()));

        JPanel messagePanel = new JPanel();
        messagePanel.setOpaque(false);
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));

        JScrollPane chatScroll = new JScrollPane(messagePanel);
        chatScroll.setBounds(25, 90, 420, 330);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);

        LiquidPanel inputBar = new LiquidPanel(35, new Color(255, 255, 255, 235));
        inputBar.setBounds(25, 445, 420, 62);
        inputBar.setLayout(null);

        JButton plusButton = new JButton("+");
        plusButton.setBounds(14, 13, 38, 38);
        plusButton.setFont(mainFont(Font.BOLD, 20));
        plusButton.setForeground(new Color(70, 70, 85));
        plusButton.setContentAreaFilled(false);
        plusButton.setBorderPainted(false);
        plusButton.setFocusPainted(false);

        PlaceholderTextField messageField = new PlaceholderTextField("메시지를 입력하세요...");
        messageField.setBounds(65, 10, 240, 42);

        LiquidButton sendButton = new LiquidButton("Send");
        sendButton.setBounds(300, 10, 105, 42);

        inputBar.add(plusButton);
        inputBar.add(messageField);
        inputBar.add(sendButton);

        sendButton.addActionListener(e -> {
            String message = messageField.getRealText();
            if (message.isEmpty()) return;

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            if (message.startsWith("/귓속말 ")) {
                String[] parts = message.split(" ", 3);

                if (parts.length < 3) {
                    JOptionPane.showMessageDialog(this, "귓속말 형식: /귓속말 상대ID 메시지");
                    return;
                }

                String targetId = parts[1];
                String content = parts[2];

                whisperHandler.sendWhisper(userId, targetId, content);
                messagePanel.add(new ChatBubble(userId + " → " + targetId, "[귓속말] " + content, time, true));
            } else {
                messagePanel.add(new ChatBubble(userId, message, time, true));
            }

            messagePanel.revalidate();
            messagePanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScroll.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

            messageField.clearAfterSend();
        });

        messageField.addActionListener(e -> sendButton.doClick());

        chatArea.add(roomTitle);
        chatArea.add(memberText);
        chatArea.add(logoutButton);
        chatArea.add(chatScroll);
        chatArea.add(inputBar);

        appCard.add(friendSide);
        appCard.add(roomSide);
        appCard.add(chatArea);

        background.add(appCard);
        setContentPane(background);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        new Main();
    }
    static class WhisperHandler {
        public void sendWhisper(String fromUserId, String targetId, String content) {
            System.out.println("WHISPER/" + fromUserId + "/" + targetId + "/" + content);

            // 나중에 서버 담당자 코드와 연결할 부분
            // 예: out.println("WHISPER/" + fromUserId + "/" + targetId + "/" + content);
        }
    }
}