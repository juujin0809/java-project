import javax.swing.*;
import components.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;



public class Main extends JFrame {

    private DefaultListModel<String> roomModel = new DefaultListModel<>();
    private UserDAO userDAO = new UserDAO();
    private WhisperHandler whisperHandler = new WhisperHandler();

    private boolean aliceOnline = true;
    private Timer statusTimer;

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

    private JLabel makeLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(titleFont(size));
        label.setForeground(new Color(35, 35, 45));
        return label;
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        java.net.URL imgURL = getClass().getResource(path);

        if (imgURL == null) {
            System.out.println("이미지 못 찾음: " + path);
            return new ImageIcon();
        }

        ImageIcon icon = new ImageIcon(imgURL);
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void stopStatusTimer() {
        if (statusTimer != null) {
            statusTimer.stop();
            statusTimer = null;
        }
    }

    private void showLoginScreen() {
        stopStatusTimer();

        setTitle("Messenger Login");
        setSize(900, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = createLiquidBackground();

        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 130));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);
        card.setFocusable(true);

        JLabel smallTitle = makeLabel("Welcome back", 16);
        smallTitle.setBounds(160, 70, 300, 35);

        JLabel title = makeLabel("Messenger", 52);
        title.setBounds(160, 105, 460, 75);

        JLabel subTitle = new JLabel("Sign up first, then log in.");
        subTitle.setFont(mainFont(Font.BOLD, 16));
        subTitle.setForeground(new Color(55, 55, 70));
        subTitle.setBounds(160, 178, 400, 35);

        PlaceholderTextField idField = new PlaceholderTextField("User ID");
        idField.setBounds(160, 240, 360, 55);

        PlaceholderPasswordField pwField = new PlaceholderPasswordField("Password");
        pwField.setBounds(160, 315, 360, 55);

        LiquidButton loginButton = new LiquidButton("Login");
        loginButton.setBounds(160, 390, 360, 55);

        JLabel signupText = new JLabel("First time here?");
        signupText.setFont(mainFont(Font.BOLD, 15));
        signupText.setForeground(new Color(55, 55, 70));
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
        stopStatusTimer();

        JPanel background = createLiquidBackground();

        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 140));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);
        card.setFocusable(true);

        JButton backButton = new JButton("← Login");
        backButton.setBounds(145, 65, 100, 30);
        backButton.setFont(mainFont(Font.BOLD, 14));
        backButton.setForeground(new Color(60, 60, 70));
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel title = makeLabel("Create Account", 44);
        title.setBounds(160, 115, 460, 70);

        JLabel subTitle = new JLabel("Create your User ID to start chatting.");
        subTitle.setFont(mainFont(Font.BOLD, 16));
        subTitle.setForeground(new Color(55, 55, 70));
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
        stopStatusTimer();

        JPanel background = createLiquidBackground();

        LiquidPanel mainCard = new LiquidPanel(40, new Color(255, 255, 255, 120));
        mainCard.setBounds(60, 45, 780, 500);
        mainCard.setLayout(null);

        JLabel title = makeLabel("Messenger", 34);
        title.setBounds(40, 25, 300, 55);

        JLabel userLabel = new JLabel(userId + "님, 안녕하세요");
        userLabel.setFont(mainFont(Font.BOLD, 15));
        userLabel.setForeground(new Color(35, 35, 45));
        userLabel.setBounds(42, 75, 300, 35);

        GlassSmallButton logoButton = new GlassSmallButton("Logo...");
        logoButton.setBounds(650, 35, 95, 36);
        logoButton.addActionListener(e -> fadeToScreen(() -> showLoginScreen()));

        LiquidPanel friendPanel = new LiquidPanel(28, new Color(255, 255, 255, 225));
        friendPanel.setBounds(40, 125, 300, 330);
        friendPanel.setLayout(null);

        JLabel friendTitle = makeLabel("Friends", 22);
        friendTitle.setBounds(25, 15, 200, 45);

        JList<String> friendList = new JList<>(new String[]{
                "Alice   ● 온라인",
                "Bob     ◐ 자리 비움",
                "Charlie ○ 오프라인",
                "David   ● 온라인"
        });
        friendList.setFont(mainFont(Font.PLAIN, 16));
        friendList.setFixedCellHeight(45);
        friendList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        friendList.setBackground(new Color(255, 255, 255, 185));
        friendList.setForeground(new Color(35, 35, 45));
        friendList.setSelectionBackground(new Color(255, 222, 242));
        friendList.setSelectionForeground(new Color(35, 35, 45));

        JScrollPane friendScroll = new JScrollPane(friendList);
        friendScroll.setBounds(20, 70, 260, 180);
        friendScroll.setBorder(BorderFactory.createEmptyBorder());

        LiquidButton inviteButton = new LiquidButton("Invite Friend");
        inviteButton.setBounds(20, 270, 260, 45);

        inviteButton.addActionListener(e -> {
            String friendName = JOptionPane.showInputDialog(this, "초대할 친구 이름 또는 ID를 입력하세요.");
            if (friendName == null || friendName.trim().isEmpty()) return;
            JOptionPane.showMessageDialog(this, friendName + "님을 친구 목록에 추가했습니다.");
        });

        friendPanel.add(friendTitle);
        friendPanel.add(friendScroll);
        friendPanel.add(inviteButton);

        LiquidPanel roomPanel = new LiquidPanel(28, new Color(255, 255, 255, 225));
        roomPanel.setBounds(380, 125, 360, 330);
        roomPanel.setLayout(null);

        JLabel roomTitle = makeLabel("Chat Rooms", 22);
        roomTitle.setBounds(25, 15, 230, 45);

        JList<String> roomList = new JList<>(roomModel);
        roomList.setFont(mainFont(Font.BOLD, 16));
        roomList.setFixedCellHeight(55);
        roomList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        roomList.setBackground(new Color(255, 255, 255, 185));
        roomList.setForeground(new Color(35, 35, 45));
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setSelectionBackground(new Color(255, 222, 242));
        roomList.setSelectionForeground(new Color(35, 35, 45));

        roomList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                label.setIcon(loadIcon("/images/room.png", 28, 28));
                label.setIconTextGap(12);
                label.setFont(mainFont(Font.BOLD, 16));
                label.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                
                label.setOpaque(true);

                if (isSelected) {
                    label.setBackground(new Color(255, 115, 190));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(255, 255, 255, 150));
                    label.setForeground(new Color(35, 35, 45));
                }

                return label;
            }
        });

        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setBounds(20, 70, 320, 150);
        roomScroll.setBorder(BorderFactory.createEmptyBorder());
        
        roomScroll.setOpaque(false);
        roomScroll.getViewport().setOpaque(false);

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
            int index = roomList.getSelectedIndex();

            if (index < 0) {
                index = 0;
            }

            String roomToOpen = roomModel.getElementAt(index);
            fadeToScreen(() -> showChatScreen(userId, roomToOpen));
        });

        roomPanel.add(roomTitle);
        roomPanel.add(roomScroll);
        roomPanel.add(createRoomButton);
        roomPanel.add(enterButton);

        mainCard.add(title);
        mainCard.add(userLabel);
        mainCard.add(logoButton);
        mainCard.add(friendPanel);
        mainCard.add(roomPanel);

        background.add(mainCard);
        setContentPane(background);
        revalidate();
        repaint();
    }

    private void showChatScreen(String userId, String roomName) {
        stopStatusTimer();

        JPanel background = createLiquidBackground();

        LiquidPanel appCard = new LiquidPanel(38, new Color(255, 255, 255, 145));
        appCard.setBounds(25, 35, 840, 535);
        appCard.setLayout(null);

        JPanel friendSide = new JPanel(null);
        friendSide.setOpaque(false);
        friendSide.setBounds(0, 0, 170, 535);

        JLabel profileImg = new JLabel(loadIcon("/images/myProfile.png", 42, 42));
        profileImg.setBounds(25, 25, 42, 42);

        JLabel userName = new JLabel(userId);
        userName.setFont(mainFont(Font.BOLD, 20));
        userName.setForeground(new Color(35, 35, 45));
        userName.setBounds(78, 28, 120, 28);

        JLabel online = new JLabel("● 온라인");
        online.setFont(mainFont(Font.BOLD, 12));
        online.setForeground(new Color(45, 200, 105));
        online.setBounds(80, 55, 100, 20);

        JLabel friendTitle = new JLabel("친구 목록");
        friendTitle.setFont(mainFont(Font.BOLD, 17));
        friendTitle.setForeground(new Color(40, 40, 50));
        friendTitle.setBounds(28, 115, 100, 25);

        DefaultListModel<String> friendStatusModel = new DefaultListModel<>();
        friendStatusModel.addElement("Alice   " + (aliceOnline ? "● 온라인" : "○ 오프라인"));
        friendStatusModel.addElement("Bob     ◐ 자리 비움");
        friendStatusModel.addElement("Charlie ○ 오프라인");
        friendStatusModel.addElement("David   ● 온라인");

        JList<String> friendList = new JList<>(friendStatusModel);
        friendList.setFont(mainFont(Font.PLAIN, 13));
        friendList.setFixedCellHeight(48);
        friendList.setBackground(new Color(255, 255, 255, 0));
        friendList.setForeground(new Color(55, 55, 70));
        friendList.setSelectionBackground(new Color(255, 222, 242));
        friendList.setSelectionForeground(new Color(35, 35, 45));
        friendList.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

        friendList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                label.setIcon(loadIcon("/images/myProfile.png", 28, 28));
                label.setIconTextGap(10);
                label.setFont(mainFont(Font.BOLD, 13));
                label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

                if (isSelected) {
                    label.setBackground(new Color(255, 222, 242));
                    label.setForeground(new Color(35, 35, 45));
                } else {
                    label.setBackground(new Color(255, 255, 255, 0));
                    label.setForeground(new Color(55, 55, 70));
                }

                return label;
            }
        });

        JPanel friendBox = new JPanel(null);
        friendBox.setOpaque(false);
        friendBox.setBounds(10, 150, 150, 300);

        String[] friends = {
                "Alice   " + (aliceOnline ? "● 온라인" : "○ 오프라인"),
                "Bob     ◐ 자리 비움",
                "Charlie ○ 오프라인",
                "David   ● 온라인"
        };

        for (int i = 0; i < friends.length; i++) {
            JLabel friendItem = new JLabel(friends[i]);
            friendItem.setIcon(loadIcon("/images/myProfile.png", 28, 28));
            friendItem.setIconTextGap(8);
            friendItem.setFont(mainFont(Font.BOLD, 12));
            friendItem.setForeground(new Color(55, 55, 70));
            friendItem.setBounds(0, i * 55, 150, 45);
            friendBox.add(friendItem);
        }

        friendSide.add(profileImg);
        friendSide.add(userName);
        friendSide.add(online);
        friendSide.add(friendTitle);
        friendSide.add(friendBox);
        
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

        JLabel roomIcon = new JLabel(loadIcon("/images/room.png", 36, 36));
        roomIcon.setBounds(28, 24, 36, 36);

        JLabel roomTitle = new JLabel(roomName);
        roomTitle.setFont(mainFont(Font.BOLD, 24));
        roomTitle.setForeground(new Color(30, 30, 42));
        roomTitle.setBounds(75, 25, 240, 35);

        JLabel memberText = new JLabel("참여자 5명");
        memberText.setFont(mainFont(Font.PLAIN, 13));
        memberText.setForeground(new Color(100, 100, 115));
        memberText.setBounds(75, 58, 120, 22);

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
        
        JPopupMenu whisperMenu = new JPopupMenu();

        JMenuItem whisperItem = new JMenuItem("귓속말 사용법");

        whisperItem.setFont(mainFont(Font.BOLD, 13));

        whisperItem.addActionListener(ev -> {

            JOptionPane.showMessageDialog(
                    this,
                    "사용법:\n/귓속말 상대ID 메시지"
            );

        });

        whisperMenu.add(whisperItem);

        plusButton.addActionListener(ev -> {

            whisperMenu.show(
                    plusButton,
                    0,
                    plusButton.getHeight()
            );

        });
        
        JPanel roomBox = new JPanel(null);
        roomBox.setOpaque(false);
        roomBox.setBounds(18, 90, 165, 340);

        for (int i = 0; i < roomModel.size(); i++) {

            String rName = roomModel.getElementAt(i);

            JLabel roomItem = new JLabel(rName);

            roomItem.setIcon(loadIcon("/images/room.png", 24, 24));
            roomItem.setIconTextGap(10);

            roomItem.setFont(mainFont(Font.BOLD, 13));

            roomItem.setOpaque(true);

            roomItem.setBorder(
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
            );

            roomItem.setBounds(0, i * 58, 165, 50);

            roomItem.setCursor(
                    new Cursor(Cursor.HAND_CURSOR)
            );

            if (rName.equals(roomName)) {

                roomItem.setBackground(
                        new Color(255, 115, 190)
                );

                roomItem.setForeground(Color.WHITE);

            } else {

                roomItem.setBackground(
                        new Color(255, 255, 255, 130)
                );

                roomItem.setForeground(
                        new Color(50, 50, 65)
                );
            }

            roomItem.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {

                    if (!rName.equals(roomName)) {

                        showChatScreen(userId, rName);
                    }
                }

                public void mouseEntered(MouseEvent e) {

                    if (!rName.equals(roomName)) {

                        roomItem.setBackground(
                                new Color(255, 235, 248)
                        );
                    }
                }

                public void mouseExited(MouseEvent e) {

                    if (!rName.equals(roomName)) {

                        roomItem.setBackground(
                                new Color(255, 255, 255, 130)
                        );
                    }
                }
            });

            roomBox.add(roomItem);
        }

        JScrollPane roomScroll = new JScrollPane(roomBox);

        roomScroll.setBounds(18, 90, 165, 340);

        roomScroll.setBorder(
                BorderFactory.createEmptyBorder()
        );

        roomScroll.setOpaque(false);

        roomScroll.getViewport().setOpaque(false);

        roomScroll.getVerticalScrollBar().setUnitIncrement(14);

        plusRoom.addActionListener(e -> {

            String newRoom = JOptionPane.showInputDialog(
                    this,
                    "생성할 채팅방 이름을 입력하세요."
            );

            if (newRoom == null || newRoom.trim().isEmpty())
                return;

            roomModel.addElement(newRoom.trim());

            showChatScreen(userId, roomName);
        });

        roomSide.add(roomListTitle);
        roomSide.add(plusRoom);
        roomSide.add(roomScroll);

        PlaceholderTextField messageField = new PlaceholderTextField("메시지를 입력하세요...");
        messageField.setBounds(65, 10, 240, 42);

        LiquidButton sendButton = new LiquidButton("Send");
        sendButton.setBounds(300, 10, 105, 42);

        inputBar.add(plusButton);
        inputBar.add(messageField);
        inputBar.add(sendButton);

        sendButton.addActionListener(e -> {
            String input = messageField.getRealText();
            if (input.isEmpty()) return;

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            if (input.startsWith("/w ")) {
                String[] parts = input.split(" ", 3);

                if (parts.length < 3) {
                    JOptionPane.showMessageDialog(this, "귓속말 형식: /w 상대이름 메시지");
                    return;
                }

                String targetNickname = parts[1];
                String content = parts[2];

                whisperHandler.sendWhisper(targetNickname, content);

                messagePanel.add(new ChatBubble(
                        userId + " → " + targetNickname,
                        "[귓속말] " + content,
                        time,
                        true
                ));
            } else {
                messagePanel.add(new ChatBubble(userId, input, time, true));
            }

            messagePanel.revalidate();
            messagePanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScroll.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

            messageField.clearAfterSend();
            messageField.requestFocusInWindow();
        });

        messageField.addActionListener(e -> sendButton.doClick());

        chatArea.add(roomIcon);
        chatArea.add(roomTitle);
        chatArea.add(memberText);
        chatArea.add(logoutButton);
        chatArea.add(chatScroll);
        chatArea.add(inputBar);

        appCard.add(friendSide);
        appCard.add(roomSide);
        appCard.add(chatArea);

        background.add(appCard);

        statusTimer = new Timer(3000, e -> {
            aliceOnline = !aliceOnline;
            friendStatusModel.set(0, "Alice   " + (aliceOnline ? "● 온라인" : "○ 오프라인"));
        });
        statusTimer.start();

        setContentPane(background);
        revalidate();
        repaint();

        SwingUtilities.invokeLater(() -> messageField.requestFocusInWindow());
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

    public static void main(String[] args) {
        new Main();
    }

    static class WhisperHandler {
        public void sendWhisper(String targetNickname, String content) {
            System.out.println("WHISPER/" + targetNickname + "/" + content);
        }
    }
}
