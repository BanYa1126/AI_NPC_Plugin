package org.capstone.ai_npc_plugin.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class PromptEditorManager {

    private final Plugin plugin;
    private final File promptDataFolder;
    private final NpcFileSelector promptSelector;

    private PromptData currentData;
    private File currentDataFile;

    /**
     * @param plugin     플러그인 인스턴스
     * @param folderPath config.yml에서 읽어온 JSON 폴더 경로
     */
    public PromptEditorManager(Plugin plugin, String folderPath) {
        this.plugin = plugin;

        // 절대/상대 경로 처리
        File folder = new File(folderPath);
        if (!folder.isAbsolute()) {
            folder = new File(plugin.getDataFolder(), folderPath);
        }
        this.promptDataFolder = folder;
        if (!promptDataFolder.exists() && !promptDataFolder.mkdirs()) {
            plugin.getLogger().severe("프롬프트 폴더 생성 실패: " + promptDataFolder.getPath());
        }

        this.promptSelector = new NpcFileSelector(plugin, promptDataFolder);
    }

    /** 현재 로드된 PromptData 반환 */
    public PromptData getCurrentData() {
        return currentData;
    }

    /** JSON 파일에서 name 필드로 데이터 로드 */
    public boolean loadPromptDataByName(String name) {
        File[] files = promptDataFolder.listFiles((dir, f) -> f.endsWith(".json"));
        if (files == null) return false;

        Gson gson = new Gson();
        for (File f : files) {
            try (Reader r = new FileReader(f)) {
                PromptData d = gson.fromJson(r, PromptData.class);
                if (d != null && name.equals(d.name)) {
                    this.currentData = d;
                    this.currentDataFile = f;
                    return true;
                }
            } catch (IOException ex) {
                plugin.getLogger().severe("PromptData 로드 실패: " + f.getName());
            }
        }
        return false;
    }

    /** currentData를 JSON으로 덮어쓰기 */
    public void saveNpcData() {
        if (currentData == null || currentDataFile == null) return;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer w = new FileWriter(currentDataFile)) {
            gson.toJson(currentData, w);
        } catch (IOException ex) {
            plugin.getLogger().severe("NPC 데이터 저장 실패: " + ex.getMessage());
        }
    }

    /** 편집 GUI 열기 */
    public void openNpcEditGUI(Player player) {
        if (currentData == null) {
            player.sendMessage("§c불러온 NPC 데이터가 없습니다.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "NPC 편집 - " + currentData.name);
        // 머리 아이콘
        gui.setItem(10, new ItemStack(Material.PLAYER_HEAD));
        // 필드
        setField(gui, 12, "이름", currentData.name);
        setField(gui, 13, "나이", currentData.age);
        setField(gui, 14, "성별", currentData.gender);
        setField(gui, 15, "직업", currentData.job);
        setField(gui, 16, "성격", String.join(", ", currentData.personality));
        setField(gui, 21, "배경", currentData.background);
        // 저장/취소
        setButton(gui, 23, Material.LIME_CONCRETE, "✔ 저장");
        setButton(gui, 24, Material.RED_CONCRETE, "✖ 닫기");

        player.openInventory(gui);
    }

    private void setField(Inventory gui, int slot, String label, String value) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(label + ": " + value);
        item.setItemMeta(m);
        gui.setItem(slot, item);
    }

    private void setButton(Inventory gui, int slot, Material mat, String name) {
        ItemStack b = new ItemStack(mat);
        ItemMeta m = b.getItemMeta();
        m.setDisplayName(name);
        b.setItemMeta(m);
        gui.setItem(slot, b);
    }

    /** 프롬프트 선택 GUI 열기 */
    public void openPromptSelectGUI(Player player, Villager npc) {
        promptSelector.openGUI(player, npc);
    }
}