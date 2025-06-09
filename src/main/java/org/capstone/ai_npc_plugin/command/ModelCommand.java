package org.capstone.ai_npc_plugin.command;

import com.google.gson.Gson;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.controller.ModelController;
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelCommand
 *
 * 모델 서버 제어 명령어 /model 을 처리하는 CommandExecutor 클래스
 * 새 구조 (action 기반 프로토콜 적용)
 */

public class ModelCommand implements CommandExecutor {

    private final AI_NPC_Plugin plugin;
    private final ModelController controller = new ModelController();
    private static final String MODEL_HOST = "127.0.0.1";
    private static final int MODEL_PORT = 12345;
    private static final Gson gson = new Gson();

    public ModelCommand(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
    }

    public static void shutdownModel() {
        ModelController.stopModel();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sender.sendMessage("사용법: /model on | off | status | reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                controller.startModel();
                sender.sendMessage(ChatColor.GREEN + "모델 서버를 실행했습니다.");
                return true;

            case "off":
                controller.stopModel();
                sender.sendMessage(ChatColor.RED + "모델 서버를 종료했습니다.");
                return true;

            case "status":
                boolean running = ModelController.isModelRunning();
                sender.sendMessage("모델 상태: " + (running ? ChatColor.GREEN + "실행 중" : ChatColor.RED + "정지됨"));
                return true;

            case "reload":
                List<PromptData> npcs = plugin.getNpcManager().getAllData();
                if (npcs.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "프롬프트에 NPC 데이터가 없습니다.");
                    return true;
                }

                boolean sent = sendReloadRequest(npcs);

                if (sent) {
                    sender.sendMessage(ChatColor.GREEN + "프롬프트 데이터를 모델 서버에 적용했습니다. (NPC " + npcs.size() + "명)");
                } else {
                    sender.sendMessage(ChatColor.RED + "모델 서버와 통신에 실패했습니다.");
                }

                return true;

            default:
                sender.sendMessage(ChatColor.YELLOW + "사용법: /model on | off | status | reload");
                return true;
        }
    }

    // 모델 서버로 reload 요청 전송
    private boolean sendReloadRequest(List<PromptData> npcs) {
        try (Socket socket = new Socket(MODEL_HOST, MODEL_PORT);
             OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {

            Map<String, Object> request = new HashMap<>();
            request.put("action", "reload");
            request.put("npcs", npcs);  // Gson 으로 자동 직렬화됨

            String json = gson.toJson(request);

            writer.write(json);
            writer.write("\n");
            writer.flush();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
