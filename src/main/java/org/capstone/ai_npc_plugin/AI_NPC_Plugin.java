package org.capstone.ai_npc_plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.capstone.ai_npc_plugin.command.AINPCCommand;
import org.capstone.ai_npc_plugin.command.AINPCTabCompleter;
import org.capstone.ai_npc_plugin.command.ModelCommand;
import org.capstone.ai_npc_plugin.listener.ChatListener;
import org.capstone.ai_npc_plugin.gui.PromptEditorManager;
import org.capstone.ai_npc_plugin.listener.NpcInteractListener;
import org.capstone.ai_npc_plugin.npc.AINPC;

public final class AI_NPC_Plugin extends JavaPlugin {

    public static AINPC globalNpc = new AINPC();

    private PromptEditorManager promptEditorManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String folderPath = getConfig().getString("promptDataFolder", "promptData");

        promptEditorManager = new PromptEditorManager(this, folderPath);

        // 3) 나머지 명령어 & 리스너 등록
        getCommand("model").setExecutor(new ModelCommand(this));
        getCommand("ainpc").setExecutor(new AINPCCommand(this, promptEditorManager));
        getCommand("ainpc").setTabCompleter(new AINPCTabCompleter());
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);

        getLogger().info("AI_NPC_Plugin 활성화됨");
    }

    @Override
    public void onDisable() {
        ModelCommand.shutdownModel();
        getLogger().info("AI_NPC_Plugin 비활성화됨");
    }

    public PromptEditorManager getNpcManager() {
        return promptEditorManager;
    }
}