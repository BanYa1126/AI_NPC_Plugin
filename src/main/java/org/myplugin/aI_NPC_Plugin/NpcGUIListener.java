package org.myplugin.aI_NPC_Plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class NpcGUIListener implements Listener {
    private final AI_NPC_Plugin plugin;
    private final NpcManager manager;
    private final Map<UUID, String> editingField;

    public NpcGUIListener(AI_NPC_Plugin plugin, NpcManager manager, Map<UUID, String> editingField) {
        this.plugin = plugin;
        this.manager = manager;
        this.editingField = editingField;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("NPC 편집")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case 12 -> editingField.put(player.getUniqueId(), "name");
            case 13 -> editingField.put(player.getUniqueId(), "age");
            case 14 -> editingField.put(player.getUniqueId(), "gender");
            case 15 -> editingField.put(player.getUniqueId(), "job");
            case 16 -> editingField.put(player.getUniqueId(), "personality");
            case 21 -> editingField.put(player.getUniqueId(), "background");
            case 23 -> {
                manager.saveNpcData();
                player.sendMessage(ChatColor.GREEN + "NPC 정보가 저장되었습니다.");
                player.closeInventory();
                return;
            }
            case 24 -> {
                player.sendMessage(ChatColor.RED + "수정을 취소했습니다.");
                player.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "채팅으로 새 값을 입력해주세요.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!editingField.containsKey(player.getUniqueId())) return;

        String field = editingField.remove(player.getUniqueId());
        String input = event.getMessage();
        NpcPromptData data = manager.getCurrentData();

        switch (field) {
            case "name" -> data.name = input;
            case "age" -> data.age = input;
            case "gender" -> data.gender = input;
            case "job" -> data.job = input;
            case "personality" -> data.personality = Arrays.asList(input.split(",\\s*"));
            case "background" -> data.background = input;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> manager.openNpcEditGUI(player));
    }
}