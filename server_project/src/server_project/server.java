/*
 * 자바 서버 구현
 * 클라이언트 접속을 확인하고 접속 했을 시 접속했다는 출력을 내보냅니다
 * 오류가 나면 종료 메시지를 출력하고 프로그램이 종료됩니다
 * 이 서버의 포트 넘버는 임시 지정이므로 코드를 합칠 때 정하는 것도 좋을거 같습니다
 * 추가적인 내용이 필요하거나 전체적인 로직이 다르다면 말씀해주세요!
 * */

package server_project;

import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int START_PORT = 12345;
    private static Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        int port = START_PORT;
        ServerSocket serverSocket = null;

        while (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("서버 시작: 포트 " + port);
            } catch (IOException e) {
                System.out.println("포트 " + port + " 사용 중, 다음 포트 시도...");
                port++;
            }
        }

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("새 클라이언트 접속: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("수신: " + message);
                    broadcast(message, socket);
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 종료: " + socket.getInetAddress());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientSockets.remove(socket);
            }
        }
    }

    private static void broadcast(String message, Socket sender) {
        synchronized (clientSockets) {
            for (Socket client : clientSockets) {
                if (client != sender) {
                    try {
                        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                        out.println(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
