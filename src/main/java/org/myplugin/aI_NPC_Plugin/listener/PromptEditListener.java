package org.myplugin.aI_NPC_Plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.myplugin.aI_NPC_Plugin.util.PromptFileManager;
import org.myplugin.aI_NPC_Plugin.npc.AINPC;

import java.util.HashMap;
import java.util.UUID;

public class PromptEditListener implements Listener {

    private static final HashMap<UUID, AINPC> editingMap = new HashMap<>();

    public static void waitForEdit(UUID uuid, AINPC npc) {
        editingMap.put(uuid, npc);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!editingMap.containsKey(uuid)) return;

        AINPC npc = editingMap.remove(uuid);
        String newPrompt = event.getMessage();

        npc.setPrompt(newPrompt);
        PromptFileManager.savePrompt(npc.getName(), newPrompt);

        event.getPlayer().sendMessage("§a[프롬프트] 저장 완료!");
        event.setCancelled(true);
    }
}