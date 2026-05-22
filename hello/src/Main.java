import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.net.*;

public class Main extends JFrame {

    private DefaultListModel<String> roomModel = new DefaultListModel<>();
    private UserDAO userDAO = new UserDAO();
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private JPanel currentMessagePanel;
    private JScrollPane currentChatScroll;

    public Main() {
        // 🌟 고정방 딱 2개만 선언하고 시작!
        roomModel.addElement("B팀 방");
        roomModel.addElement("실습 게임 방");

        showLoginScreen();
    }

    private void connectToServer(String userId) {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(userId);

            Thread receiveThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("수신: " + msg);
                        
                        if (currentMessagePanel != null) {
                            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                            final String finalMsg = msg;
                            
                            SwingUtilities.invokeLater(() -> {
                                if (finalMsg.startsWith("[알림]")) {
                                    currentMessagePanel.add(new SystemMessage(finalMsg));
                                } else if (finalMsg.startsWith("[귓속말]")) {
                                    currentMessagePanel.add(new ChatBubble("귓속말", finalMsg, time, false));
                                } else if (finalMsg.contains(": ")) {
                                    String[] parts = finalMsg.split(": ", 2);
                                    currentMessagePanel.add(new ChatBubble(parts[0], parts[1], time, false));
                                }

                                currentMessagePanel.revalidate();
                                currentMessagePanel.repaint();
                                
                                if (currentChatScroll != null) {
                                    JScrollBar vertical = currentChatScroll.getVerticalScrollBar();
                                    vertical.setValue(vertical.getMaximum());
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    System.out.println("서버 연결 종료");
                }
            });
            receiveThread.start();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "서버 연결 실패! 서버를 먼저 켜주세요.");
        }
    }

    private Font titleFont(int size) { return new Font("Arial Rounded MT Bold", Font.BOLD, size); }
    private Font mainFont(int style, int size) { return new Font("Apple SD Gothic Neo", style, size); }
    private JLabel makeLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(titleFont(size));
        label.setForeground(new Color(35, 35, 45));
        return label;
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL == null) return new ImageIcon();
        ImageIcon icon = new ImageIcon(imgURL);
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void showLoginScreen() {
        setTitle("Messenger Login");
        setSize(900, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = createLiquidBackground();
        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 130));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);

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
            if (id.isEmpty() || pw.isEmpty()) return;

            if (!userDAO.exists(id)) {
                JOptionPane.showMessageDialog(this, "가입되지 않은 유저입니다.");
                return;
            }
            if (!userDAO.login(id, pw)) {
                JOptionPane.showMessageDialog(this, "비밀번호가 틀렸습니다.");
                return;
            }

            connectToServer(id);
            fadeToScreen(() -> showMainScreen(id));
        });

        signupButton.addActionListener(e -> fadeToScreen(() -> showSignupScreen()));

        card.add(smallTitle); card.add(title); card.add(subTitle);
        card.add(idField); card.add(pwField); card.add(loginButton);
        card.add(signupText); card.add(signupButton);
        background.add(card);
        setContentPane(background);
        setVisible(true);
        SwingUtilities.invokeLater(() -> card.requestFocusInWindow());
    }

    private void showSignupScreen() {
        JPanel background = createLiquidBackground();
        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 140));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);

        JButton backButton = new JButton("← Login");
        backButton.setBounds(145, 65, 100, 30);
        backButton.setFont(mainFont(Font.BOLD, 14));
        backButton.setContentAreaFilled(false); backButton.setBorderPainted(false);

        JLabel title = makeLabel("Create Account", 44);
        title.setBounds(160, 115, 460, 70);
        PlaceholderTextField idField = new PlaceholderTextField("New User ID");
        idField.setBounds(160, 250, 360, 55);
        PlaceholderPasswordField pwField = new PlaceholderPasswordField("New Password");
        pwField.setBounds(160, 325, 360, 55);
        LiquidButton signupButton = new LiquidButton("Sign Up");
        signupButton.setBounds(160, 405, 360, 58);

        signupButton.addActionListener(e -> {
            String id = idField.getRealText();
            String pw = pwField.getRealPassword();
            if (id.isEmpty() || pw.isEmpty()) return;
            if (userDAO.exists(id)) {
                JOptionPane.showMessageDialog(this, "이미 존재하는 ID입니다.");
                return;
            }
            userDAO.register(id, pw);
            JOptionPane.showMessageDialog(this, "회원가입 완료!");
            fadeToScreen(() -> showLoginScreen());
        });

        backButton.addActionListener(e -> fadeToScreen(() -> showLoginScreen()));
        card.add(backButton); card.add(title); card.add(idField); card.add(pwField); card.add(signupButton);
        background.add(card);
        setContentPane(background);
    }

    private void showMainScreen(String userId) {
        JPanel background = createLiquidBackground();
        LiquidPanel mainCard = new LiquidPanel(40, new Color(255, 255, 255, 120));
        mainCard.setBounds(60, 45, 780, 500);
        mainCard.setLayout(null);

        JLabel title = makeLabel("Messenger", 34);
        title.setBounds(40, 25, 300, 55);
        JLabel userLabel = new JLabel(userId + "님, 안녕하세요");
        userLabel.setFont(mainFont(Font.BOLD, 15));
        userLabel.setBounds(42, 75, 300, 35);

        // 🌟 [UI 변경] 가짜 데이터 없는 청정 친구 목록 구현
        LiquidPanel friendPanel = new LiquidPanel(28, new Color(255, 255, 255, 225));
        friendPanel.setBounds(40, 125, 300, 330);
        friendPanel.setLayout(null);

        JLabel friendTitle = makeLabel("Friends", 22);
        friendTitle.setBounds(25, 15, 200, 45);
        
        DefaultListModel<String> friendModel = new DefaultListModel<>();
        JList<String> friendList = new JList<>(friendModel);
        friendList.setFont(mainFont(Font.PLAIN, 16));
        friendList.setFixedCellHeight(45);
        JScrollPane friendScroll = new JScrollPane(friendList);
        friendScroll.setBounds(20, 70, 260, 180);
        friendScroll.setBorder(BorderFactory.createEmptyBorder());

        LiquidButton inviteButton = new LiquidButton("Invite Friend");
        inviteButton.setBounds(20, 270, 260, 45);
        inviteButton.addActionListener(e -> {
            String friendName = JOptionPane.showInputDialog(this, "초대할 친구 이름 또는 ID를 입력하세요.");
            if (friendName == null || friendName.trim().isEmpty()) return;
            friendModel.addElement(friendName.trim() + "   ● 온라인");
            JOptionPane.showMessageDialog(this, friendName + "님을 친구 목록에 추가했습니다.");
        });
        friendPanel.add(friendTitle); friendPanel.add(friendScroll); friendPanel.add(inviteButton);

        // 🌟 [UI 변경] 방 만들기 버튼 없애고 배치 위로 당김
        LiquidPanel roomPanel = new LiquidPanel(28, new Color(255, 255, 255, 225));
        roomPanel.setBounds(380, 125, 360, 330);
        roomPanel.setLayout(null);

        JLabel roomTitle = makeLabel("Chat Rooms", 22);
        roomTitle.setBounds(25, 15, 230, 45);

        JList<String> roomList = new JList<>(roomModel);
        roomList.setFont(mainFont(Font.BOLD, 16));
        roomList.setFixedCellHeight(55);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setIcon(loadIcon("/images/room.png", 28, 28));
                label.setIconTextGap(12);
                label.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                if (isSelected) {
                    label.setBackground(new Color(255, 115, 190)); label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(255, 255, 255, 150)); label.setForeground(new Color(35, 35, 45));
                }
                return label;
            }
        });

        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setBounds(20, 70, 320, 160);
        roomScroll.setBorder(BorderFactory.createEmptyBorder());

        // 입장 버튼 위치 조절 (방 만들기 버튼 빈자리 채움)
        LiquidButton enterButton = new LiquidButton("Enter Chat");
        enterButton.setBounds(20, 255, 320, 50);
        enterButton.addActionListener(e -> {
            int index = roomList.getSelectedIndex();
            if (index < 0) index = 0;
            String roomToOpen = roomModel.getElementAt(index);
            fadeToScreen(() -> showChatScreen(userId, roomToOpen));
        });

        roomPanel.add(roomTitle); roomPanel.add(roomScroll); roomPanel.add(enterButton);
        mainCard.add(title); mainCard.add(userLabel); mainCard.add(friendPanel); mainCard.add(roomPanel);
        background.add(mainCard);
        setContentPane(background);
        revalidate(); repaint();
    }

    private void showChatScreen(String userId, String roomName) {
        JPanel background = createLiquidBackground();
        LiquidPanel appCard = new LiquidPanel(38, new Color(255, 255, 255, 145));
        appCard.setBounds(25, 35, 840, 535);
        appCard.setLayout(null);

        // 🌟 가짜 친구 목록 UI 완전히 삭제된 깨끗한 사이드바
        JPanel friendSide = new JPanel(null);
        friendSide.setOpaque(false); friendSide.setBounds(0, 0, 170, 535);

        JLabel profileImg = new JLabel(loadIcon("/images/myProfile.png", 42, 42));
        profileImg.setBounds(25, 25, 42, 42);
        JLabel userName = new JLabel(userId);
        userName.setFont(mainFont(Font.BOLD, 20));
        userName.setBounds(78, 28, 120, 28);
        JLabel online = new JLabel("● 온라인");
        online.setFont(mainFont(Font.BOLD, 12)); online.setForeground(new Color(45, 200, 105));
        online.setBounds(80, 55, 100, 20);
        
        friendSide.add(profileImg); friendSide.add(userName); friendSide.add(online);
        
        // 중앙 고정방 이동 탭 
        JPanel roomSide = new JPanel(null);
        roomSide.setOpaque(false); roomSide.setBounds(170, 0, 200, 535);

        JLabel roomListTitle = new JLabel("채팅방");
        roomListTitle.setFont(mainFont(Font.BOLD, 18));
        roomListTitle.setBounds(25, 35, 100, 30);

        JPanel roomBox = new JPanel(null);
        roomBox.setOpaque(false); roomBox.setBounds(18, 90, 165, 340);

        for (int i = 0; i < roomModel.size(); i++) {
            String rName = roomModel.getElementAt(i);
            JLabel roomItem = new JLabel(rName);
            roomItem.setIcon(loadIcon("/images/room.png", 24, 24));
            roomItem.setIconTextGap(10);
            roomItem.setFont(mainFont(Font.BOLD, 13));
            roomItem.setOpaque(true);
            roomItem.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            roomItem.setBounds(0, i * 58, 165, 50);

            if (rName.equals(roomName)) {
                roomItem.setBackground(new Color(255, 115, 190)); roomItem.setForeground(Color.WHITE);
            } else {
                roomItem.setBackground(new Color(255, 255, 255, 130)); roomItem.setForeground(new Color(50, 50, 65));
            }

            roomItem.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!rName.equals(roomName)) showChatScreen(userId, rName);
                }
            });
            roomBox.add(roomItem);
        }
        roomSide.add(roomListTitle); roomSide.add(roomBox);

        // 실제 채팅 공간
        JPanel chatArea = new JPanel(null) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 224, 240, 155), getWidth(), getHeight(), new Color(220, 235, 255, 170));
                g2.setPaint(gp); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
            }
        };
        chatArea.setBounds(370, 0, 470, 535); chatArea.setOpaque(false);

        JLabel roomIcon = new JLabel(loadIcon("/images/room.png", 36, 36));
        roomIcon.setBounds(28, 24, 36, 36);
        JLabel roomTitle = new JLabel(roomName);
        roomTitle.setFont(mainFont(Font.BOLD, 24));
        roomTitle.setBounds(75, 25, 240, 35);

        // 🌟 나가기 버튼 클릭 시 다시 로비(대기실) 신분으로 원복 신고
        GlassSmallButton logoutButton = new GlassSmallButton("Exit");
        logoutButton.setBounds(325, 28, 115, 34);
        logoutButton.addActionListener(e -> {
            if (out != null) out.println("JOIN/대기실/" + userId);
            fadeToScreen(() -> showMainScreen(userId));
        });

        JPanel messagePanel = new JPanel();
        messagePanel.setOpaque(false); messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane chatScroll = new JScrollPane(messagePanel);
        chatScroll.setBounds(25, 90, 420, 330);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.setOpaque(false); chatScroll.getViewport().setOpaque(false);

        currentMessagePanel = messagePanel;
        currentChatScroll = chatScroll;

        // 🌟 [방 변경 신고] 채팅창이 열리자마자 서버에 "나 이 방에 들어왔어!" 라고 통보
        if (out != null) {
            out.println("JOIN/" + roomName + "/" + userId);
        }

        LiquidPanel inputBar = new LiquidPanel(35, new Color(255, 255, 255, 235));
        inputBar.setBounds(25, 445, 420, 62); inputBar.setLayout(null);

        PlaceholderTextField messageField = new PlaceholderTextField("메시지를 입력하세요...");
        messageField.setBounds(25, 10, 280, 42); // 왼쪽 여백 정렬 조절
        LiquidButton sendButton = new LiquidButton("Send");
        sendButton.setBounds(310, 10, 95, 42);

        // 🌟 메시지를 보낼 때 어떤 방인지 "방 이름(roomName)"을 패킷에 심어서 보냄
        sendButton.addActionListener(e -> {
            String input = messageField.getRealText();
            if (input.isEmpty()) return;

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            if (input.startsWith("/w ") || input.startsWith("/귓속말 ")) {
                String[] parts = input.split(" ", 3);
                if (parts.length >= 3) {
                    String targetNickname = parts[1];
                    String content = parts[2];
                    if (out != null) {
                        out.println("WHISPER/" + roomName + "/" + userId + "/" + targetNickname + "/" + content);
                    }
                    messagePanel.add(new ChatBubble(userId + " → " + targetNickname, "[귓속말] " + content, time, true));
                }
            } else {
                // 일반 대화 패킷 규격 업그레이드: CHAT/방이름/내ID/ALL/메시지
                if (out != null) {
                    out.println("CHAT/" + roomName + "/" + userId + "/ALL/" + input);
                }
                messagePanel.add(new ChatBubble(userId, input, time, true));
            }

            messagePanel.revalidate(); messagePanel.repaint();
            SwingUtilities.invokeLater(() -> chatScroll.getVerticalScrollBar().setValue(chatScroll.getVerticalScrollBar().getMaximum()));
            messageField.clearAfterSend(); messageField.requestFocusInWindow();
        });

        messageField.addActionListener(e -> sendButton.doClick());
        inputBar.add(messageField); inputBar.add(sendButton);
        
        chatArea.add(roomIcon); chatArea.add(roomTitle); chatArea.add(logoutButton);
        chatArea.add(chatScroll); chatArea.add(inputBar);
        appCard.add(friendSide); appCard.add(roomSide); appCard.add(chatArea);
        background.add(appCard);
        setContentPane(background);
        revalidate(); repaint();
        SwingUtilities.invokeLater(() -> messageField.requestFocusInWindow());
    }

    private JPanel createLiquidBackground() {
        JPanel background = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint base = new GradientPaint(0, 0, new Color(255, 241, 247), getWidth(), getHeight(), new Color(226, 240, 255));
                g2.setPaint(base); g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        // 🌟 이 핵심 코드가 있어야 컴포넌트들이 화면에 나타납니다!
        background.setLayout(null); 
        
        return background;
    }

    private void fadeToScreen(Runnable nextScreen) {
        Timer timer = new Timer(120, e -> { nextScreen.run(); revalidate(); repaint(); });
        timer.setRepeats(false); timer.start();
    }

    public static void main(String[] args) { new Main(); }
}
