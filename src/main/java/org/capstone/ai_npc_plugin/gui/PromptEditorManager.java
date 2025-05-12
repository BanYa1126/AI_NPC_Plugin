package org.capstone.ai_npc_plugin.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

public class PromptEditorManager {

    private final AI_NPC_Plugin plugin;
    private final NpcFileSelector promptSelector;
    private final File promptDataFolder;

    private PromptData currentData;
    private File currentDataFile;

    public PromptEditorManager(AI_NPC_Plugin plugin) {
        this.plugin = plugin;
        this.promptSelector = new NpcFileSelector(plugin);
        this.promptDataFolder = new File(plugin.getDataFolder(), "promptData");
        if (!promptDataFolder.exists()) {
            promptDataFolder.mkdirs();
        }
    }

    /**
     * NPC 편집 GUI를 생성 및 표시합니다.
     */
    public void openNpcEditGUI(Player player) {
        if (currentData == null) {
            plugin.getLogger().warning("저장된 NPC 데이터를 불러오지 못했습니다.");
            return;
        }
        // 27칸 GUI 생성
        Inventory gui = Bukkit.createInventory(null, 27, "NPC 편집 - " + currentData.name);

        // 1) NPC 머리 아이콘 (슬롯 10)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        gui.setItem(10, head);

        // 2) 속성 필드: 이름, 나이, 성별, 직업, 성격, 배경
        setFieldItem(gui, 12, "이름", currentData.name);
        setFieldItem(gui, 13, "나이", currentData.age);
        setFieldItem(gui, 14, "성별", currentData.gender);
        setFieldItem(gui, 15, "직업", currentData.job);
        setFieldItem(gui, 16, "성격", String.join(", ", currentData.personality));
        setFieldItem(gui, 21, "배경", currentData.background);

        // 3) 저장 / 닫기 버튼
        ItemStack saveBtn = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta saveMeta = saveBtn.getItemMeta();
        saveMeta.setDisplayName("✔ 저장");
        saveBtn.setItemMeta(saveMeta);
        gui.setItem(23, saveBtn);

        ItemStack cancelBtn = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelBtn.getItemMeta();
        cancelMeta.setDisplayName("✖ 닫기");
        cancelBtn.setItemMeta(cancelMeta);
        gui.setItem(24, cancelBtn);

        // 플레이어에게 GUI 오픈
        player.openInventory(gui);
    }

    // 편의 메소드: 인벤토리에 필드 항목 세팅
    private void setFieldItem(Inventory gui, int slot, String label, String value) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(label + ": " + value);
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }

    /**
     * JSON 프롬프트 선택 GUI 열기 (NPC 생성 시 호출)
     */
    public void openPromptSelectGUI(Player player, Villager npc) {
        promptSelector.openGUI(player, npc);
    }

    /**
     * GUI 이벤트 핸들러에서 접근할 수 있도록 셀렉터를 외부로 노출
     */
    public NpcFileSelector getPromptSelector() {
        return promptSelector;
    }

    /**
     * 현재 로드된 PromptData를 반환합니다.
     */
    public PromptData getCurrentData() {
        return currentData;
    }

    /**
     * 현재 로드된 PromptData를 JSON 파일로 저장합니다.
     */
    public void saveNpcData() {
        if (currentData == null || currentDataFile == null) {
            plugin.getLogger().warning("저장할 NPC 데이터가 없습니다.");
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(currentDataFile)) {
            gson.toJson(currentData, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("NPC 데이터 저장 실패: " + e.getMessage());
        }
    }

    /**
     * name 필드가 주어진 값과 일치하는 JSON을 로드하여 currentData로 설정합니다.
     * @param name 찾을 NPC 이름
     * @return 성공적으로 로드되면 true, 그렇지 않으면 false
     */
    public boolean loadPromptDataByName(String name) {
        File[] files = promptDataFolder.listFiles((dir, fname) -> fname.endsWith(".json"));
        if (files == null) {
            plugin.getLogger().warning("프롬프트 데이터 폴더를 찾을 수 없습니다.");
            return false;
        }
        Gson gson = new Gson();
        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                PromptData data = gson.fromJson(reader, PromptData.class);
                if (data != null && data.name != null && data.name.equals(name)) {
                    this.currentData = data;
                    this.currentDataFile = file;
                    return true;
                }
            } catch (IOException e) {
                plugin.getLogger().severe("PromptData 파일 읽기 실패: " + file.getName() + " - " + e.getMessage());
            }
        }
        return false;
    }
}