import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.net.*;

public class Main extends JFrame {

    private static final int SERVER_PORT = 12345;
    // 발표 때 다른 PC에서 바로 접속시키려면 localhost 대신 서버 노트북 IP로 바꾸면 됩니다.
    // 예) private static final String DEFAULT_SERVER_IP = "192.168.0.15";
    private static final String DEFAULT_SERVER_IP = "localhost";
    private String serverHost = DEFAULT_SERVER_IP;

    private DefaultListModel<String> roomModel = new DefaultListModel<>();
    private DefaultListModel<String> friendModel = new DefaultListModel<>();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean receiveThreadStarted = false;
    private String loginUserId = "";

    private JPanel currentMessagePanel;
    private JScrollPane currentChatScroll;

    // 🌟 [추가] 내부 클래스 WhisperHandler 객체 선언
    private WhisperHandler whisperHandler;

    public Main() {
        // 🌟 고정방 딱 2개만 선언하고 시작!
        roomModel.addElement("B팀 방");
        roomModel.addElement("실습 게임 방");

        showLoginScreen();
    }

    private boolean connectToServer() {
        if (socket != null && socket.isConnected() && !socket.isClosed() && out != null && in != null) {
            return true;
        }
        try {
            socket = new Socket(serverHost, SERVER_PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void showLoginScreen() {
        setTitle("로그인");
        setSize(450, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel background = createLiquidBackground();

        JLabel titleLabel = new JLabel("Chat Login");
        titleLabel.setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 28));
        titleLabel.setForeground(new Color(85, 85, 100));
        titleLabel.setBounds(40, 60, 300, 40);
        background.add(titleLabel);

        PlaceholderTextField idField = new PlaceholderTextField("아이디를 입력하세요");
        idField.setBounds(40, 140, 354, 46);
        background.add(idField);

        PlaceholderPasswordField pwField = new PlaceholderPasswordField("비밀번호를 입력하세요");
        pwField.setBounds(40, 200, 354, 46);
        background.add(pwField);

        LiquidButton loginBtn = new LiquidButton("로그인");
        loginBtn.setBounds(40, 280, 354, 48);
        background.add(loginBtn);

        GlassSmallButton signupBtn = new GlassSmallButton("회원가입 하러가기");
        signupBtn.setBounds(40, 350, 354, 44);
        background.add(signupBtn);

        loginBtn.addActionListener(e -> {
            String id = idField.getRealText();
            String pw = pwField.getRealPassword();
            if (id.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 모두 입력하세요.");
                return;
            }
            sendAuthRequest("LOGIN", id, pw);
        });

        signupBtn.addActionListener(e -> {
            String id = idField.getRealText();
            String pw = pwField.getRealPassword();
            if (id.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "회원가입할 아이디와 비밀번호를 입력창에 적어주세요.");
                return;
            }
            sendAuthRequest("SIGNUP", id, pw);
        });

        setContentPane(background);
        setVisible(true);
    }

    private void sendAuthRequest(String type, String id, String pw) {
        if (!connectToServer()) return;

        out.println(type + Protocol.SEPARATOR + id + Protocol.SEPARATOR + pw);

        if (!receiveThreadStarted) {
            startReceiveThread();
            receiveThreadStarted = true;
        }
    }

    private void startReceiveThread() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> handleServerMessage(msg));
                }
            } catch (IOException e) {
                System.out.println("서버와 연결이 끊어졌습니다.");
            }
        }).start();
    }

    private void handleServerMessage(String rawMsg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        if (rawMsg.startsWith("LOGIN_SUCCESS/")) {
            String[] tokens = rawMsg.split("/", 2);
            this.loginUserId = tokens[1];
            
            // 🌟 [추가] 로그인 성공 시 귓속말 담당 핸들러 초기화 생성
            this.whisperHandler = new WhisperHandler(this.out, this.loginUserId);
            
            fadeToScreen("MAIN");
            return;
        } else if (rawMsg.startsWith("LOGIN_FAIL/")) {
            String[] tokens = rawMsg.split("/", 2);
            JOptionPane.showMessageDialog(this, "로그인 실패: " + tokens[1]);
            return;
        } else if (rawMsg.startsWith("SIGNUP_SUCCESS")) {
            JOptionPane.showMessageDialog(this, "회원가입 성공! 로그인해 주세요.");
            return;
        } else if (rawMsg.startsWith("SIGNUP_FAIL/")) {
            String[] tokens = rawMsg.split("/", 2);
            JOptionPane.showMessageDialog(this, "회원가입 실패: " + tokens[1]);
            return;
        } else if (rawMsg.startsWith("FRIEND_LIST/")) {
            String[] tokens = rawMsg.split("/", 2);
            friendModel.clear();
            if (tokens.length > 1 && !tokens[1].isEmpty()) {
                String[] friends = tokens[1].split(",");
                for (String f : friends) {
                    friendModel.addElement(f.trim());
                }
            }
            return;
        } else if (rawMsg.startsWith("FRIEND_ADD_FAIL/")) {
            String[] tokens = rawMsg.split("/", 2);
            JOptionPane.showMessageDialog(this, "친구 추가 실패: " + tokens[1]);
            return;
        }

        if (currentMessagePanel == null) return;

        // 🌟 [추가/수정] 서버로부터 온 메시지가 귓속말 패킷 포맷일 때 우선 처리
        if (whisperHandler != null && whisperHandler.isWhisper(rawMsg)) {
            String sender = whisperHandler.getSender(rawMsg);
            String content = whisperHandler.getContent(rawMsg);
            currentMessagePanel.add(new ChatBubble("[귓속말] " + sender, content, time, false));
        } 
        // 기존 일반 전체 대화 파싱 조건문 보존
        else if (rawMsg.contains(": ")) {
            String[] parts = rawMsg.split(": ", 2);
            currentMessagePanel.add(new ChatBubble(parts[0], parts[1], time, false));
        } 
        // 기존 시스템 공지 메시지 파싱 조건문 보존
        else {
            currentMessagePanel.add(new SystemMessage(rawMsg));
        }

        currentMessagePanel.revalidate();
        currentMessagePanel.repaint();

        // 자동 스크롤 다운 로직 보존
        SwingUtilities.invokeLater(() -> {
            if (currentChatScroll != null) {
                JScrollBar vertical = currentChatScroll.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }

    private void fadeToScreen(String screenType) {
        if (screenType.equals("MAIN")) {
            getContentPane().removeAll();
            showChatScreen(); // 🌟 원본 메서드 이름 100% 유지
        }
    }

    private void showChatScreen() {
        setTitle("메인 실시간 대화방 - " + loginUserId);
        setSize(960, 640);
        setLocationRelativeTo(null);

        JPanel background = createLiquidBackground();

        JPanel appCard = new JPanel();
        appCard.setLayout(null);
        appCard.setBackground(new Color(255, 255, 255, 110));
        appCard.setBounds(20, 20, 904, 562);

        // 1. 왼쪽 친구 영역 (디자인 및 스크롤 경계선 값 원본 유지)
        LiquidPanel friendSide = new LiquidPanel(24, new Color(240, 242, 255, 140));
        friendSide.setLayout(new BoxLayout(friendSide, BoxLayout.Y_AXIS));
        friendSide.setBounds(10, 10, 190, 542);
        friendSide.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel friendTitle = new JLabel(" 친구 목록");
        friendTitle.setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 15));
        friendTitle.setForeground(new Color(90, 90, 110));
        friendTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JList<String> friendList = new JList<>(friendModel);
        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendList.setFont(new Font("Apple SD Gothic Neo", Font.PLAIN, 13));
        JScrollPane friendScroll = new JScrollPane(friendList);
        friendScroll.setBorder(BorderFactory.createEmptyBorder());
        friendScroll.setOpaque(false);
        friendScroll.getViewport().setOpaque(false);

        PlaceholderTextField addFriendField = new PlaceholderTextField("친구 아이디 추가");
        addFriendField.setMaximumSize(new Dimension(170, 36));
        addFriendField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String target = addFriendField.getRealText();
                    if (!target.isEmpty()) {
                        out.println("ADD_FRIEND" + Protocol.SEPARATOR + loginUserId + Protocol.SEPARATOR + target);
                        addFriendField.setText("");
                    }
                }
            }
        });

        // 🌟 [추가] 이미지 기획 요구사항인 귓속말 버튼 인스턴스 생성 및 액션 매핑
        LiquidButton whisperButton = new LiquidButton("귓속말");
        whisperButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        whisperButton.setMaximumSize(new Dimension(170, 38));
        
        whisperButton.addActionListener(e -> {
            String selectedFriend = friendList.getSelectedValue();
            if (selectedFriend == null || selectedFriend.isEmpty()) {
                JOptionPane.showMessageDialog(Main.this, "귓속말을 보낼 친구를 목록에서 선택해주세요!", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // 타이핑 필요 없이 다이얼로그 팝업창으로 메시지 내용 입력받기
            String content = JOptionPane.showInputDialog(Main.this, selectedFriend + " 님에게 보낼 귓속말 내용:", "귓속말 보내기", JOptionPane.PLAIN_MESSAGE);
            
            if (content != null && !content.trim().isEmpty()) {
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                String activeRoom = (roomModel.size() > 0) ? roomModel.getElementAt(0) : "B팀 방"; 
                
                // 귓속말 패킷 서버 전송
                whisperHandler.sendWhisper(activeRoom, selectedFriend, content.trim());
                
                // 내 화면 대화 영역에 내가 보낸 귓속말 말풍선 추가
                currentMessagePanel.add(new ChatBubble(loginUserId + " → " + selectedFriend, "[귓속말] " + content.trim(), time, true));
                currentMessagePanel.revalidate();
                currentMessagePanel.repaint();
                
                SwingUtilities.invokeLater(() -> {
                    if (currentChatScroll != null) {
                        currentChatScroll.getVerticalScrollBar().setValue(currentChatScroll.getVerticalScrollBar().getMaximum());
                    }
                });
            }
        });

        // 친구 아이디 더블클릭 시에도 귓속말 버튼 액션이 자동 실행되도록 구현
        friendList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    whisperButton.doClick();
                }
            }
        });

        friendSide.add(friendTitle);
        friendSide.add(Box.createVerticalStrut(10));
        friendSide.add(friendScroll);
        friendSide.add(Box.createVerticalStrut(10));
        friendSide.add(addFriendField);
        friendSide.add(Box.createVerticalStrut(8)); // 입력필드와 버튼 사이 간격 추가
        friendSide.add(whisperButton);               // 🌟 친구 사이드바 맨 하단에 귓속말 버튼 안착

        // 2. 중간 방 목록 영역 (원본 소스 로직 및 수치 100% 보존)
        LiquidPanel roomSide = new LiquidPanel(24, new Color(245, 240, 255, 140));
        roomSide.setLayout(new BoxLayout(roomSide, BoxLayout.Y_AXIS));
        roomSide.setBounds(210, 10, 170, 542);
        roomSide.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel roomTitleLabel = new JLabel(" 대화방 목록");
        roomTitleLabel.setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 15));
        roomTitleLabel.setForeground(new Color(90, 90, 110));
        roomTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JList<String> roomList = new JList<>(roomModel);
        roomList.setFont(new Font("Apple SD Gothic Neo", Font.PLAIN, 13));
        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setBorder(BorderFactory.createEmptyBorder());
        roomScroll.setOpaque(false);
        roomScroll.getViewport().setOpaque(false);

        roomSide.add(roomTitleLabel);
        roomSide.add(Box.createVerticalStrut(10));
        roomSide.add(roomScroll);

        // 3. 오른쪽 채팅방 영역 (원본 컴포넌트 여백 및 바인딩 완벽 유지)
        LiquidPanel chatArea = new LiquidPanel(32, new Color(255, 255, 255, 180));
        chatArea.setLayout(null);
        chatArea.setBounds(390, 10, 504, 542);

        JLabel roomIcon = new JLabel("💬");
        roomIcon.setFont(new Font("SansSerif", Font.PLAIN, 22));
        roomIcon.setBounds(24, 18, 40, 30);

        JLabel roomTitle = new JLabel("B팀 방");
        roomTitle.setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 18));
        roomTitle.setForeground(new Color(70, 70, 85));
        roomTitle.setBounds(64, 18, 250, 30);

        GlassSmallButton logoutButton = new GlassSmallButton("로그아웃");
        logoutButton.setFont(new Font("Apple SD Gothic Neo", Font.BOLD, 12));
        logoutButton.setBounds(390, 16, 90, 34);
        logoutButton.addActionListener(e -> {
            try {
                if (socket != null) socket.close();
            } catch (Exception ex) {}
            System.exit(0);
        });

        currentMessagePanel = new JPanel();
        currentMessagePanel.setLayout(new BoxLayout(currentMessagePanel, BoxLayout.Y_AXIS));
        currentMessagePanel.setBackground(new Color(255, 255, 255, 0));

        JScrollPane chatScroll = new JScrollPane(currentMessagePanel);
        chatScroll.setBounds(14, 65, 476, 400);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        this.currentChatScroll = chatScroll;

        JPanel inputBar = new JPanel();
        inputBar.setLayout(null);
        inputBar.setOpaque(false);
        inputBar.setBounds(14, 478, 476, 50);

        PlaceholderTextField messageField = new PlaceholderTextField("메시지를 입력하세요...");
        messageField.setBounds(0, 0, 385, 46);

        LiquidButton sendButton = new LiquidButton("전송");
        sendButton.setBounds(395, 0, 81, 44);

        // 엔터키 및 전송 버튼 액션 리스너 바인딩 (안전한 Protocol.SEPARATOR 구조 반영)
        ActionListener sendAction = e -> {
            String input = messageField.getRealText();
            if (input.isEmpty()) return;

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            
            // 원본 CHAT 구조 전송 유지
            out.println("CHAT" + Protocol.SEPARATOR + "B팀 방" + Protocol.SEPARATOR + loginUserId + Protocol.SEPARATOR + "ALL" + Protocol.SEPARATOR + input);
            
            currentMessagePanel.add(new ChatBubble(loginUserId, input, time, true));
            messageField.setText("");
            
            currentMessagePanel.revalidate();
            currentMessagePanel.repaint();
            
            SwingUtilities.invokeLater(() -> chatScroll.getVerticalScrollBar().setValue(chatScroll.getVerticalScrollBar().getMaximum()));
        };

        messageField.addActionListener(sendAction);
        sendButton.addActionListener(sendAction);

        inputBar.add(messageField); 
        inputBar.add(sendButton);

        chatArea.add(roomIcon); 
        chatArea.add(roomTitle); 
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
        
        SwingUtilities.invokeLater(() -> messageField.requestFocusInWindow());
    }

    private JPanel createLiquidBackground() {
        JPanel background = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint base = new GradientPaint(0, 0, new Color(255, 241, 247), getWidth(), getHeight(), new Color(226, 240, 255));
                g2.setPaint(base); 
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // 🌟 이 핵심 코드가 있어야 컴포넌트들이 화면에 나타납니다! (원본 고스란히 보존)
        background.setLayout(null);

        return background;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main());
    }

    // ====================================================================
    // 🌟 [추가] WhisperHandler 클래스를 내부 스캔 구조(Inner Class)로 매핑하여 내장
    // ====================================================================
    private class WhisperHandler {
        private PrintWriter out;
        private String myNickname; 
        
        public WhisperHandler(PrintWriter out, String myNickname) {
            this.out = out;
            this.myNickname = myNickname;
        }
        
        public void sendWhisper(String currentRoom, String targetNickname, String message) {
            String packet = Protocol.CHAT 
                    + Protocol.SEPARATOR + currentRoom 
                    + Protocol.SEPARATOR + myNickname 
                    + Protocol.SEPARATOR + targetNickname 
                    + Protocol.SEPARATOR + message;
            out.println(packet);
        }
        
        public boolean isWhisper(String serverMessage) {
            return serverMessage != null && serverMessage.startsWith("[귓속말]");
        }
        
        public String getSender(String serverMessage) {
            try {
                int start = serverMessage.indexOf("] ") + 2;
                int end = serverMessage.indexOf(" →");
                if (start >= 0 && end > start) {
                    return serverMessage.substring(start, end).trim();
                }
            } catch (Exception e) {
                System.out.println("귓속말 발신자 추출 오류: " + e.getMessage());
            }
            return "Unknown";
        }
        
        public String getContent(String serverMessage) {
            try {
                int start = serverMessage.indexOf(": ");
                if (start >= 0) {
                    return serverMessage.substring(start + 2);
                }
            } catch (Exception e) {
                System.out.println("귓속말 본문 추출 오류: " + e.getMessage());
            }
            return serverMessage;
        }
    }
}
