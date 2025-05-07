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
    private String currentPromptId = "npc";

    public NpcManager(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
        loadNpcData(this.currentPromptId);
    }

    public void openNpcEditGUI(Player player) {
        if (currentData == null) {
            player.sendMessage(ChatColor.RED + "해당 프롬프트 데이터를 불러올 수 없습니다.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "🛠 NPC 편집 - " + currentData.name);
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
        File dataFile = new File(plugin.getDataFolder(), "NPCData/" + currentPromptId + ".json");

        // 디렉토리 없으면 생성
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }

        try (Writer writer = new FileWriter(dataFile)) {
            gson.toJson(currentData, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("JSON 저장 실패: " + e.getMessage());
        }
    }

    public void loadNpcData(String promptId) {
        this.currentPromptId = promptId;
        File dataFile = new File(plugin.getDataFolder(), "NPCData/" + promptId + ".json");

        if (!dataFile.exists()) {
            plugin.getLogger().warning("프롬프트 파일이 존재하지 않습니다: " + dataFile.getAbsolutePath());
            currentData = null;
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            currentData = gson.fromJson(reader, NpcPromptData.class);
            plugin.getLogger().info("프롬프트 로드됨: " + promptId);
        } catch (IOException e) {
            plugin.getLogger().warning("프롬프트 파일 읽기 실패: " + e.getMessage());
            currentData = null;
        }
    }
}