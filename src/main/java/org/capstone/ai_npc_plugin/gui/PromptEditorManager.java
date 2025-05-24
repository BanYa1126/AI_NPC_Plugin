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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class PromptEditorManager {
    private final Plugin plugin;
    private final File promptDataFolder;
    private final NpcFileSelector fileSelector;
    private final NpcGUIListener guiListener;
    private List<PromptData> allData;
    private PromptData currentData;
    private File currentDataFile;

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
        this.guiListener = new NpcGUIListener(plugin, this);
        this.fileSelector = new NpcFileSelector(
                plugin,
                promptDataFolder,
                this,           // PromptEditorManager
                guiListener     // NpcGUIListener
        );
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);
    }

    public void openPromptSelectGUI(Player player) {
        fileSelector.openGUI(player, null, NpcFileSelector.Mode.PROMPT_SET);
    }

    public void openDataSelectGUI(Player player, Villager npc) {
        guiListener.openCreateSelector(player, npc);
    }

    public boolean loadPromptFile(String fileName) {
        String base = fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
        return loadNpcData(base);
    }

    public boolean loadNpcData(String baseName) {
        File file = new File(promptDataFolder, baseName + ".json");
        if (!file.exists()) {
            plugin.getLogger().warning("NPC 데이터 파일이 없습니다: " + file);
            return false;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file),
                StandardCharsets.UTF_8
        )) {
            JsonElement je = JsonParser.parseReader(reader);

            if (je.isJsonArray()) {
                Type listType = new TypeToken<List<PromptData>>() {}.getType();
                allData = gson.fromJson(je, listType);
            } else if (je.isJsonObject()) {
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

    public PromptData getCurrentData() {
        return currentData;
    }

    public boolean loadPromptDataByName(String name) {
        File[] files = promptDataFolder.listFiles((dir, f) -> f.endsWith(".json"));
        if (files == null) return false;

        Gson gson = new Gson();
        for (File f : files) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(f),
                    StandardCharsets.UTF_8
            )) {
                PromptData d = gson.fromJson(reader, PromptData.class);
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

    public void saveNpcData() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(currentDataFile),
                StandardCharsets.UTF_8
        )) {
            gson.toJson(allData, w);
        } catch (IOException ex) {
            plugin.getLogger().severe("NPC 데이터 저장 실패: " + ex.getMessage());
        }
    }
    public void openPromptFixGUI(Player player) {
        // 두 번째 인자(npc)에는 아직 관련 정보가 없으므로 null
        fileSelector.openGUI(player, null, NpcFileSelector.Mode.PROMPT_FIX);
    }

    public void openNpcEditGUI(Player player) {
        guiListener.openFixSelector(player);
    }

    public List<PromptData> getAllData() {
        if (allData == null) return Collections.emptyList();
        return allData;
    }

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