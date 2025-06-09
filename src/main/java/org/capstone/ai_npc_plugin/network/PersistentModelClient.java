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
 * 지속 연결(keep-alive) 방식의 모델 서버 클라이언트 클래스
 *
 * 주요 기능:
 * - connect(): 모델 서버와 소켓 연결 초기화 (프로그램 시작 시)
 * - disconnect(): 연결 종료
 * - isConnected(): 현재 연결 상태 확인
 * - sendMessage(): 플레이어 메시지를 모델 서버에 전송하고 NPC 응답 반환 (thread-safe)
 *
 * 통신 방식:
 * - Persistent 연결 유지 → 여러 요청 시 재연결 필요 없음
 * - Thread-safe (sendMessage 에 synchronized 적용)
 *
 * 사용 위치:
 * - ChatListener 에서 모델 호출 시 사용됨
 * - 플러그인 시작 시 1회 연결 시도
 */

public class PersistentModelClient {
    // 서버 호스트 및 포트
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 12345;
    // 서버 호스트 및 포트
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    // JSON 변환용 Gson 인스턴스
    private final Gson gson = new Gson();
    // 연결 상태 플래그
    private boolean connected = false;

    // 연결 초기화
    public boolean connect() {
        try {
            // 소켓 연결
            socket = new Socket(HOST, PORT);
            // 스트림 초기화
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            // 상태 업데이트
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
        try {  // 스트림 및 소켓 정리
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
            // 상태 업데이트
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

        try { // 요청 생성
            Map<String, String> request = new HashMap<>();
            request.put("player_name", playerName);
            request.put("player_message", message);
            // 직렬화 후 전송
            String jsonRequest = gson.toJson(request);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();
            // 응답 수신
            String jsonResponse = reader.readLine();
            if (jsonResponse == null) return "⚠️ 모델 응답 없음.";
            // 역직렬화 후 응답 반환
            Map<String, Object> response = gson.fromJson(jsonResponse, Map.class);
            return (String) response.getOrDefault("npc_response", "⚠️ 응답 파싱 실패");

        } catch (IOException e) {
            // 오류 발생 시 연결 끊김으로 처리
            connected = false;
            return "❌ 오류 발생: " + e.getMessage();
        }
    }
}
