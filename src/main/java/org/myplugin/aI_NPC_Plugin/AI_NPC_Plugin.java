package org.myplugin.aI_NPC_Plugin;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.myplugin.aI_NPC_Plugin.gui.NPCSettingsGUI;
import org.myplugin.aI_NPC_Plugin.listener.GUIListener;
import org.myplugin.aI_NPC_Plugin.npc.AINPC;
import org.myplugin.aI_NPC_Plugin.listener.PromptEditListener;
import org.myplugin.aI_NPC_Plugin.listener.InventoryClickListener;

public final class AI_NPC_Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PromptEditListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this); // GUI 클릭 핸들러
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getCommand("npcsettings").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                AINPC npc = new AINPC(); // 임시 NPC
                NPCSettingsGUI.open(player, npc);
            }
            return true;
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
