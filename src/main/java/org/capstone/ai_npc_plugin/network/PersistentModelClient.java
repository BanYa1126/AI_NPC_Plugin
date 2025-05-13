package org.capstone.ai_npc_plugin.network;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PersistentModelClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 12345;

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final Gson gson = new Gson();

    private boolean connected = false;

    // 연결 초기화
    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            connected = true;
            System.out.println("[모델 클라이언트] 연결됨.");
            return true;
        } catch (IOException e) {
            System.err.println("[모델 클라이언트] 연결 실패: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    // 연결 해제
    public void disconnect() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
            connected = false;
            System.out.println("[모델 클라이언트] 연결 종료됨.");
        } catch (IOException e) {
            System.err.println("[모델 클라이언트] 종료 중 오류: " + e.getMessage());
        }
    }

    // 연결 상태 확인
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // 메시지 전송 및 응답 수신
    public synchronized String sendMessage(String playerName, String message) {
        if (!isConnected()) {
            return "⚠️ 모델 서버와 연결되어 있지 않습니다.";
        }

        try {
            Map<String, String> request = new HashMap<>();
            request.put("player_name", playerName);
            request.put("player_message", message);

            String jsonRequest = gson.toJson(request);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();

            String jsonResponse = reader.readLine();
            if (jsonResponse == null) return "⚠️ 모델 응답 없음.";

            Map<String, Object> response = gson.fromJson(jsonResponse, Map.class);
            return (String) response.getOrDefault("npc_response", "⚠️ 응답 파싱 실패");

        } catch (IOException e) {
            connected = false;
            return "❌ 오류 발생: " + e.getMessage();
        }
    }
}
