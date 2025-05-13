package org.capstone.ai_npc_plugin.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

public class PromptEditorManager {
    private final Plugin plugin;
    private final File promptDataFolder;
    private final NpcFileSelector fileSelector;
    private final NpcGUIListener guiListener;
    private List<PromptData> allData;

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
            plugin.getLogger().warning("NPC 데이터 파일이 없습니다: " + file);
            return false;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Reader reader = new FileReader(file)) {
            JsonElement je = JsonParser.parseReader(reader);

            if (je.isJsonArray()) {
                // 배열인 경우 기존 로직
                Type listType = new TypeToken<List<PromptData>>(){}.getType();
                allData = gson.fromJson(je, listType);

            } else if (je.isJsonObject()) {
                // 단일 객체인 경우에도 처리
                PromptData single = gson.fromJson(je, PromptData.class);
                allData = new ArrayList<>();
                allData.add(single);

            } else {
                plugin.getLogger().severe("지원하지 않는 JSON 형식입니다: root is " + je);
                return false;
            }

            currentDataFile = file;
            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("NPC 데이터 로드 실패: " + e.getMessage());
            return false;
        }
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
        if (allData == null || currentDataFile == null) return;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer w = new FileWriter(currentDataFile)) {
            gson.toJson(allData, w);
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
        if (allData == null) return Collections.emptyList();
        return allData;
    }

    /**
     * 번호로 지정된 PromptData를 currentData로 설정
     */
    public boolean setCurrentData(int number) {
        for (PromptData d : allData) {
            if (d.number == number) {
                currentData = d;
                return true;
            }
        }
        return false;
    }
}