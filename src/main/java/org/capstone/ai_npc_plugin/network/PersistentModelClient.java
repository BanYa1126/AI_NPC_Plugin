package org.capstone.ai_npc_plugin.network;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * PersistentModelClient
 *
 * 모델 서버와 지속 연결을 유지하며 메시지 요청을 보내는 클라이언트 클래스
 * 새 구조:
 * - action 기반 요청 지원 ("chat", "reload" 등)
 * - chat 요청 시 npc_code 포함
 */

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

    // 메시지 전송 및 응답 수신 (NPC 코드 포함)
    // 메시지 전송 및 응답 수신
    public synchronized String sendMessage(String playerName, String npcCode, String message) {
        if (!isConnected()) {
            return "모델 서버와 연결되어 있지 않습니다.";
        }

        try {
            // 요청 구성
            Map<String, String> request = new HashMap<>();
            request.put("action", "chat");
            request.put("player_name", playerName);
            request.put("npc_code", npcCode);
            request.put("player_message", message);

            // JSON 직렬화 후 전송
            String jsonRequest = gson.toJson(request);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();

            // 응답 수신
            String jsonResponse = reader.readLine();
            if (jsonResponse == null) return "모델 응답 없음.";

            Map<String, Object> response = gson.fromJson(jsonResponse, Map.class);
            return (String) response.getOrDefault("npc_response", "⚠️ 응답 파싱 실패");

        } catch (IOException e) {
            connected = false;
            return "오류 발생: " + e.getMessage();
        }
    }
    public synchronized String sendReload(Map<String, Object> packet) {
        if (!isConnected()) {
            return "⚠️ 모델 서버와 연결되어 있지 않습니다.";
        }

        try {
            String jsonRequest = gson.toJson(packet);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();

            String jsonResponse = reader.readLine();
            if (jsonResponse == null) return "⚠️ 모델 응답 없음.";

            return jsonResponse;

        } catch (IOException e) {
            connected = false;
            return "❌ 오류 발생: " + e.getMessage();
        }
    }
    // 프롬프트 리로드 요청
    public synchronized String sendReloadPrompt(String jsonData) {
        if (!isConnected()) {
            return "⚠️ 모델 서버와 연결되어 있지 않습니다.";
        }

        try {
            // 요청 JSON 보내기
            writer.write(jsonData);
            writer.newLine();
            writer.flush();

            // 응답 수신
            String jsonResponse = reader.readLine();
            if (jsonResponse == null) return "⚠️ 모델 응답 없음.";

            return jsonResponse;

        } catch (IOException e) {
            connected = false;
            return "❌ 오류 발생: " + e.getMessage();
        }
    }
}