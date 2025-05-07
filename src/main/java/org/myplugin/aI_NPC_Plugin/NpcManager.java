package org.myplugin.aI_NPC_Plugin;

import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.util.*;

public class NpcManager {
    private final Gson gson = new Gson();
    private final AI_NPC_Plugin plugin;
    private NpcPromptData currentData;

    public NpcManager(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
        loadNpcData();
    }

    public void openNpcEditGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "\uD83D\uDEE0 NPC 편집 - " + currentData.name);

        gui.setItem(10, new ItemStack(Material.PLAYER_HEAD));
        gui.setItem(12, createEditableItem("이름", currentData.name));
        gui.setItem(13, createEditableItem("나이", currentData.age));
        gui.setItem(14, createEditableItem("성별", currentData.gender));
        gui.setItem(15, createEditableItem("직업", currentData.job));
        gui.setItem(16, createEditableItem("성격", String.join(", ", currentData.personality)));
        gui.setItem(21, createEditableItem("배경", wrapText(currentData.background, 30)));
        gui.setItem(23, createActionButton(Material.LIME_DYE, "✔ 저장"));
        gui.setItem(24, createActionButton(Material.BARRIER, "❌ 닫기"));

        player.openInventory(gui);
    }

    private ItemStack createEditableItem(String title, String value) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + title);
        meta.setLore(Collections.singletonList(ChatColor.WHITE + value));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + name);
        item.setItemMeta(meta);
        return item;
    }

    private String wrapText(String text, int maxLineLength) {
        return text.replaceAll("(.{1," + maxLineLength + "})(\\s+|$)", "$1\n");
    }

    public NpcPromptData getCurrentData() {
        return currentData;
    }

    public void saveNpcData() {
        try (Writer writer = new FileWriter("plugins/NPCPlugin/NPCData/npc.json")) {
            gson.toJson(currentData, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("JSON 저장 실패: " + e.getMessage());
        }
    }

    private void loadNpcData() {
        try (Reader reader = new FileReader("plugins/NPCPlugin/NPCData/npc.json")) {
            currentData = gson.fromJson(reader, NpcPromptData.class);
        } catch (IOException e) {
            plugin.getLogger().warning("JSON 파일 로드 실패. 기본값 사용.");
            currentData = new NpcPromptData();
        }
    }
}