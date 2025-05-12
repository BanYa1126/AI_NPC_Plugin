package org.capstone.ai_npc_plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.capstone.ai_npc_plugin.command.AINPCCommand;
import org.capstone.ai_npc_plugin.command.ModelCommand;
import org.capstone.ai_npc_plugin.listener.ChatListener;
import org.capstone.ai_npc_plugin.gui.NpcGUIListener;
import org.capstone.ai_npc_plugin.gui.PromptEditorManager;
import org.capstone.ai_npc_plugin.npc.AINPC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AI_NPC_Plugin extends JavaPlugin {

    public static AINPC globalNpc = new AINPC();

    private final Map<UUID, String> editingField = new HashMap<>();
    private PromptEditorManager promptEditorManager;

    @Override
    public void onEnable() {
        promptEditorManager = new PromptEditorManager(this);

        getCommand("model").setExecutor(new ModelCommand(this));
        getCommand("ainpc").setExecutor(new AINPCCommand(this));

        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new NpcGUIListener(this, promptEditorManager, editingField), this);

        getLogger().info("AI_NPC_Plugin 활성화됨");
    }

    @Override
    public void onDisable() {
        ModelCommand.shutdownModel();
        getLogger().info("AI_NPC_Plugin 비활성화됨");
    }

    public Map<UUID, String> getEditingField() {
        return editingField;
    }

    public PromptEditorManager getNpcManager() {
        return promptEditorManager;
    }
}