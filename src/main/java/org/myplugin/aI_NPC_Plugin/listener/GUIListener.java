package org.myplugin.aI_NPC_Plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.myplugin.aI_NPC_Plugin.gui.NPCSettingsGUI;
import org.myplugin.aI_NPC_Plugin.npc.AINPC;

import java.util.HashMap;
import java.util.UUID;

public class GUIListener implements Listener {

    public static final HashMap<UUID, AINPC> openedNPCMap = new HashMap<>();
    public static final HashMap<UUID, AINPC> waitingNameInput = new HashMap<>();
    public static final HashMap<UUID, AINPC> waitingPromptInput = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("NPC 설정")) return;

        event.setCancelled(true);

        UUID uuid = event.getWhoClicked().getUniqueId();
        if (openedNPCMap.containsKey(uuid)) {
            NPCSettingsGUI.handleClick(event, openedNPCMap.get(uuid));
        }
    }

    @EventHandler
    public void onChatInput(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String msg = event.getMessage();

        if (waitingNameInput.containsKey(uuid)) {
            AINPC npc = waitingNameInput.remove(uuid);
            npc.setName(msg);
            event.getPlayer().sendMessage("✅ 이름이 설정되었습니다: " + msg);
            event.setCancelled(true);
        }

        if (waitingPromptInput.containsKey(uuid)) {
            AINPC npc = waitingPromptInput.remove(uuid);
            npc.setPrompt(msg);
            event.getPlayer().sendMessage("✅ 프롬프트가 설정되었습니다.");
            event.setCancelled(true);
        }
    }
}