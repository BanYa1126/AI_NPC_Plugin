package org.myplugin.aI_NPC_Plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class AI_NPC_Plugin extends JavaPlugin {
    private final Map<UUID, String> editingField = new HashMap<>();
    private NpcManager npcManager;

    @Override
    public void onEnable() {
        npcManager = new NpcManager(this);
        getCommand("npcgui").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                npcManager.openNpcEditGUI(player);
            }
            return true;
        });
        Bukkit.getPluginManager().registerEvents(new NpcGUIListener(this, npcManager, editingField), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Map<UUID, String> getEditingField() {
        return editingField;
    }
}