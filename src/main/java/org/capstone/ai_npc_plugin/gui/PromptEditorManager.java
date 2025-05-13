package org.capstone.ai_npc_plugin.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PromptEditorManager {
    private final Plugin plugin;
    private final File promptDataFolder;
    private final NpcFileSelector fileSelector;
    private final NpcGUIListener guiListener;

    private PromptData currentData;
    private File currentDataFile;

    /**
     * @param plugin     플러그인 인스턴스
     * @param folderPath config.yml에서 읽어온 JSON 폴더 경로
     */
    public PromptEditorManager(Plugin plugin, String folderPath) {
        this.plugin = plugin;
        File folder = new File(folderPath);
        if (!folder.isAbsolute()) {
            folder = new File(plugin.getDataFolder(), folderPath);
        }
        this.promptDataFolder = folder;
        if (!promptDataFolder.exists() && !promptDataFolder.mkdirs()) {
            plugin.getLogger().severe("프롬프트 폴더 생성 실패: " + promptDataFolder.getPath());
        }
        this.fileSelector = new NpcFileSelector(plugin, promptDataFolder);
        this.guiListener = new NpcGUIListener(plugin, this);
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);
    }

    /**
     * 기본 JSON 파일 이름(npc.json 등)로 로드
     */
    public boolean loadNpcData(String baseName) {
        File file = new File(promptDataFolder, baseName + ".json");
        if (!file.exists()) {
            plugin.getLogger().warning("프롬프트 파일이 존재하지 않습니다: " + file.getPath());
            return false;
        }
        Gson gson = new Gson();
        try (Reader reader = new FileReader(file)) {
            PromptData data = gson.fromJson(reader, PromptData.class);
            if (data != null) {
                this.currentData = data;
                this.currentDataFile = file;
                return true;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("NPC 데이터 로드 실패: " + e.getMessage());
        }
        return false;
    }

    /**
     * 현재 로드된 PromptData 반환
     */
    public PromptData getCurrentData() {
        return currentData;
    }

    /**
     * JSON 파일에서 name 필드로 데이터 로드
     */
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

    /**
     * currentData를 JSON으로 덮어쓰기
     */
    public void saveNpcData() {
        if (currentData == null || currentDataFile == null) return;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer w = new FileWriter(currentDataFile)) {
            gson.toJson(currentData, w);
        } catch (IOException ex) {
            plugin.getLogger().severe("NPC 데이터 저장 실패: " + ex.getMessage());
        }
    }

    /**
     * NPC 생성 시 프롬프트 선택 GUI 열기
     */
    public void openPromptSelectGUI(Player player, Villager npc) {
        fileSelector.openGUI(player, npc);
    }

    /**
     * 프롬프트 편집 GUI(필드 선택) 열기
     */
    public void openNpcEditGUI(Player player) {
        guiListener.openSelector(player);
    }

    /**
     * JSON 폴더 내 모든 PromptData 리스트로 반환
     */
    public List<PromptData> getAllData() {
        List<PromptData> list = new ArrayList<>();
        Gson gson = new Gson();
        File[] files = promptDataFolder.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files == null) return list;
        for (File f : files) {
            try (Reader r = new FileReader(f)) {
                PromptData d = gson.fromJson(r, PromptData.class);
                if (d != null) list.add(d);
            } catch (IOException e) {
                plugin.getLogger().warning("데이터 로드 실패: " + f.getName());
            }
        }
        list.sort((a, b) -> Integer.compare(a.number, b.number));
        return list;
    }

    /**
     * 번호로 지정된 PromptData를 currentData로 설정
     */
    public boolean setCurrentData(int number) {
        for (PromptData d : getAllData()) {
            if (d.number == number) {
                this.currentData = d;
                this.currentDataFile = new File(promptDataFolder, number + ".json");
                return true;
            }
        }
        return false;
    }
}