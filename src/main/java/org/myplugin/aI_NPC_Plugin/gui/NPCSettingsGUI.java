package org.myplugin.aI_NPC_Plugin.gui;

import org.myplugin.aI_NPC_Plugin.npc.AINPC;
import org.myplugin.aI_NPC_Plugin.registry.AINPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class NPCSettingsGUI {

    public static void open(Player player, AINPC npc) {
        Inventory gui = Bukkit.createInventory(null, 27, "§9NPC 설정");

        gui.setItem(11, createItem(Material.NAME_TAG, "§a이름 설정", "현재 이름: " + npc.getName()));
        gui.setItem(15, createItem(Material.BOOK, "§e프롬프트 설정", "현재 프롬프트: 클릭하여 수정"));

        player.openInventory(gui);
        AINPCRegistry.setSelectedNPC(player.getUniqueId(), npc);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
