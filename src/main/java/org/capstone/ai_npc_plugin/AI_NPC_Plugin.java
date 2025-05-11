package org.capstone.ai_npc_plugin;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.capstone.ai_npc_plugin.command.ModelCommand;
import org.capstone.ai_npc_plugin.command.AINPCCommand;
import org.capstone.ai_npc_plugin.listener.ChatListener;
import org.capstone.ai_npc_plugin.gui.NpcGUIListener;
import org.capstone.ai_npc_plugin.gui.PromptEditorManager;
import org.capstone.ai_npc_plugin.npc.AINPC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AI_NPC_Plugin extends JavaPlugin {

    // 단일 AI NPC 상태
    public static AINPC globalNpc = new AINPC();

    // GUI 관련 데이터
    private final Map<UUID, String> editingField = new HashMap<>();
    private PromptEditorManager promptEditorManager;

    @Override
    public void onEnable() {
        // 명령어 등록
        getCommand("model").setExecutor(new ModelCommand(this));
        getCommand("ainpc").setExecutor(new AINPCCommand(this));
        getCommand("npcgui").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                promptEditorManager.openNpcEditGUI(player);
            }
            return true;
        });

        // NPC 프롬프트 관리 GUI 초기화
        promptEditorManager = new PromptEditorManager(this);

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new ChatListener(), this); // 대화 감지용
        getServer().getPluginManager().registerEvents(new NpcGUIListener(this, promptEditorManager, editingField), this);

        getLogger().info("AI_NPC_Plugin 활성화됨");
    }

    @Override
    public void onDisable() {
        ModelCommand.shutdownModel();  // 모델 종료
        getLogger().info("AI_NPC_Plugin 비활성화됨");
    }

    public Map<UUID, String> getEditingField() {
        return editingField;
    }

    public PromptEditorManager getNpcManager() {
        return promptEditorManager;
    }
}

