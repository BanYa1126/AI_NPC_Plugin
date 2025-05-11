package org.capstone.ai_npc_plugin.network;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ModelSocketClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 12345;
    private static final Gson gson = new Gson();

    public static String getNPCResponse(String playerName, String message) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. JSON 요청 만들기
            Map<String, String> request = new HashMap<>();
            request.put("player_name", playerName);
            request.put("player_message", message);

            String jsonRequest = gson.toJson(request);
            writer.write(jsonRequest);
            writer.newLine();
            writer.flush();

            // 2. 응답 수신
            String jsonResponse = reader.readLine();
            Map<?, ?> responseMap = gson.fromJson(jsonResponse, Map.class);
            return (String) responseMap.get("npc_response");

        } catch (IOException e) {
            e.printStackTrace();
            return "⚠️ 모델 서버와 연결할 수 없습니다.";
        }
    }
}
