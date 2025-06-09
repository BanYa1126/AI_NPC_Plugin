package org.capstone.ai_npc_plugin.network;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * ModelSocketClient
 *
 * 단일 요청-응답 방식의 모델 서버 클라이언트 클래스
 *
 * 주요 기능:
 * - getNPCResponse(): 플레이어 메시지를 모델 서버에 전송하고, NPC 응답을 받아 반환
 *
 * 통신 방식:
 * - 요청 시마다 Socket 을 새로 연결 → 요청 → 응답 수신 → 종료
 *
 * 사용 위치:
 * - 기존 테스트용, 단발성 호출 시 유용
 * - 현재 실사용은 PersistentModelClient 가 주로 사용됨
 */

public class ModelSocketClient {
    // 모델 서버 호스트 (현재 로컬호스트)
    private static final String HOST = "127.0.0.1";
    // 모델 서버 포트 (예: 12345)
    private static final int PORT = 12345;
    // JSON 변환용 Gson 인스턴스
    private static final Gson gson = new Gson();

    // NPC 응답 요청 메서드 (단발성 소켓 사용)
    // playerName : 플레이어 이름
    // message    : 플레이어 입력 메시지
    // 반환값: 모델 서버가 돌려준 npc_response 문자열
    public static String getNPCResponse(String playerName, String message) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. JSON 요청 만들기
            Map<String, String> request = new HashMap<>();
            request.put("player_name", playerName);
            request.put("player_message", message);
            // 직렬화
            String jsonRequest = gson.toJson(request);
            // 전송
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();

            // 2. 응답 수신
            String jsonResponse = reader.readLine();
            // 역직렬화 (Map 형태로 받음)
            Map<?, ?> responseMap = gson.fromJson(jsonResponse, Map.class);
            // npc_response 키에서 응답 추출
            return (String) responseMap.get("npc_response");

        } catch (IOException e) { // 에러 발생 시 예외 출력 및 안내 메시지 반환
            e.printStackTrace();
            return "⚠️ 모델 서버와 연결할 수 없습니다.";
        }
    }
}
