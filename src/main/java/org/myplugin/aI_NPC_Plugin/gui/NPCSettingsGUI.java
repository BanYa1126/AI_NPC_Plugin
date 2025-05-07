package org.myplugin.aI_NPC_Plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.aI_NPC_Plugin.listener.GUIListener;
import org.myplugin.aI_NPC_Plugin.npc.AINPC;

public class NPCSettingsGUI {
//test
    public static void open(Player player, AINPC npc) {
        Inventory gui = Bukkit.createInventory(null, 9, "NPC 설정");

        gui.setItem(2, createItem(Material.NAME_TAG, "§a이름 설정"));
        gui.setItem(4, createItem(Material.BOOK, "§프롬프트 설정"));
        gui.setItem(8, createItem(Material.LIME_WOOL, "§a설정 완료"));

        player.openInventory(gui);
        GUIListener.openedNPCMap.put(player.getUniqueId(), npc); // 현재 설정 중인 NPC 저장
    }

    private static ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void handleClick(InventoryClickEvent event, AINPC npc) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        player.closeInventory();

        switch (slot) {
            case 2 -> {
                player.sendMessage(ChatColor.YELLOW + "변경할 이름을 채팅에 입력하세요.");
                GUIListener.waitingNameInput.put(player.getUniqueId(), npc);
            }
            case 4 -> {
                player.sendMessage(ChatColor.YELLOW + "프롬프트 내용을 채팅에 입력하세요.");
                GUIListener.waitingPromptInput.put(player.getUniqueId(), npc);
            }
            case 8 -> {
                player.sendMessage(ChatColor.GREEN + "✅ 설정 완료!");
                // 저장 로직 추가 가능
            }
        }
    }
}