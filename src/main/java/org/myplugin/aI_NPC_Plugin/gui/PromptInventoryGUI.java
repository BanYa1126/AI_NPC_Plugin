package org.myplugin.aI_NPC_Plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.myplugin.aI_NPC_Plugin.npc.AINPC;

public class PromptInventoryGUI {

    public static void open(Player player, AINPC npc) {
        Inventory gui = Bukkit.createInventory(null, 9, "§8프롬프트 관리");

        ItemStack viewPrompt = new ItemStack(Material.BOOK);
        ItemMeta viewMeta = viewPrompt.getItemMeta();
        viewMeta.setDisplayName("§a📖 프롬프트 확인");
        viewPrompt.setItemMeta(viewMeta);

        ItemStack editPrompt = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta editMeta = editPrompt.getItemMeta();
        editMeta.setDisplayName("§e✏️ 프롬프트 수정");
        editPrompt.setItemMeta(editMeta);

        gui.setItem(3, viewPrompt);
        gui.setItem(5, editPrompt);

        player.openInventory(gui);

        // NPC 정보를 기억하거나 메모리 캐시에서 불러오는 구조 필요 (예: UUID → AINPC 맵)
    }

    public static void handleClick(InventoryClickEvent event, AINPC npc) {
        if (!event.getView().getTitle().equals("§8프롬프트 관리")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        Player player = (Player) event.getWhoClicked();

        if (name.equals("§a📖 프롬프트 확인")) {
            PromptBookGUI.open(player, npc, false);
            player.closeInventory();
        } else if (name.equals("§e✏️ 프롬프트 수정")) {
            PromptBookGUI.open(player, npc, true);
            player.closeInventory();
        }
    }
}
