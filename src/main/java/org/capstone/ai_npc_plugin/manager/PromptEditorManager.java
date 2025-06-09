package org.capstone.ai_npc_plugin.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;
import org.capstone.ai_npc_plugin.AI_NPC_Plugin;
import org.capstone.ai_npc_plugin.gui.NpcFileSelector;
import org.capstone.ai_npc_plugin.listener.NpcGUIListener;
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * PromptEditorManager
 *
 * 프롬프트 데이터 (PromptData) 를 관리하는 매니저 클래스
 *
 * 주요 기능:
 * - 프롬프트 JSON 파일 로드 및 저장
 * - 현재 선택된 PromptData 관리
 * - 프롬프트 선택 GUI (NpcFileSelector) 관리
 * - NPC 데이터 선택/수정 GUI (NpcGUIListener) 관리
 *
 * 연동 위치:
 * - /ainpc prompt_set, prompt_fix 명령어 처리 시 사용
 * - NpcFileSelector, NpcGUIListener 와 직접 연결
 *
 * 데이터 저장 위치:
 * - config.yml 에 지정된 promptDataFolder 내부
 */

public class PromptEditorManager {

    // 플러그인 인스턴스
    private final Plugin plugin;

    // 프롬프트 JSON 데이터 저장 폴더
    private final File promptDataFolder;

    // 프롬프트 파일 선택 GUI
    private final NpcFileSelector fileSelector;

    // NPC 데이터 선택/수정 GUI
    private final NpcGUIListener guiListener;

    // 전체 프롬프트 데이터 목록 (현재 로드된 것)
    private List<PromptData> allData;

    // 현재 선택된 PromptData
    private PromptData currentData;

    // 현재 로드된 파일
    private File currentDataFile;

    // 생성자 - 폴더 경로 설정, GUI 리스너 초기화
    public PromptEditorManager(Plugin plugin, String folderPath) {
        this.plugin = plugin;

        // 폴더 절대경로 처리
        File folder = new File(folderPath);
        if (!folder.isAbsolute()) {
            folder = new File(plugin.getDataFolder(), folderPath);
        }

        this.promptDataFolder = folder;

        // 폴더 없으면 생성 시도
        if (!promptDataFolder.exists() && !promptDataFolder.mkdirs()) {
            plugin.getLogger().severe("프롬프트 폴더 생성 실패: " + promptDataFolder.getPath());
        }

        // GUI 리스너 초기화 및 등록
        this.guiListener = new NpcGUIListener(plugin, this);
        this.fileSelector = new NpcFileSelector(
                plugin,
                promptDataFolder,
                this, // PromptEditorManager 참조 전달
                guiListener
        );

        // NpcGUIListener 리스너 등록
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);
    }

    // 프롬프트 파일 선택 GUI 열기
    public void openPromptSelectGUI(Player player) {
        fileSelector.openGUI(player, null, NpcFileSelector.Mode.PROMPT_SET);
    }

    // NPC 생성용 데이터 선택 GUI 열기
    public void openDataSelectGUI(Player player, Villager npc) {
        guiListener.openCreateSelector(player, npc);
    }

    // 프롬프트 파일 로드 (확장자 .json 자동 처리)
    public boolean loadPromptFile(String fileName) {
        String base = fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
        return loadNpcData(base);
    }

    // NPC 데이터 로드
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
            // JSON 파싱
            JsonElement je = JsonParser.parseReader(reader);

            if (je.isJsonArray()) {
                // 배열 형태 처리
                Type listType = new TypeToken<List<PromptData>>() {}.getType();
                allData = gson.fromJson(je, listType);
            } else if (je.isJsonObject()) {
                // 단일 객체 처리
                PromptData single = gson.fromJson(je, PromptData.class);
                allData = new ArrayList<>();
                allData.add(single);
            } else {
                plugin.getLogger().severe("지원하지 않는 JSON 형식입니다: root is " + je);
                return false;
            }

            // 현재 데이터 파일 기록
            currentDataFile = file;
            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("NPC 데이터 로드 실패: " + e.getMessage());
            return false;
        }
    }

    // 현재 선택된 데이터 반환
    public PromptData getCurrentData() {
        return currentData;
    }

    // 이름으로 프롬프트 데이터 로드 (플러그인 내부에서 /model reload 등에 사용)
    public boolean loadPromptDataByCode(String code) {
        File[] files = promptDataFolder.listFiles((dir, f) -> f.endsWith(".json"));
        if (files == null) return false;

        Gson gson = new Gson();

        for (File f : files) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(f),
                    StandardCharsets.UTF_8
            )) {
                JsonElement je = JsonParser.parseReader(reader);
                List<PromptData> fileData;

                if (je.isJsonArray()) {
                    Type listType = new TypeToken<List<PromptData>>() {}.getType();
                    fileData = gson.fromJson(je, listType);
                } else if (je.isJsonObject()) {
                    PromptData single = gson.fromJson(je, PromptData.class);
                    fileData = new ArrayList<>();
                    fileData.add(single);
                } else {
                    continue;
                }

                for (PromptData d : fileData) {
                    if (d.code != null && d.code.equals(code)) {
                        this.allData = fileData;
                        this.currentData = d;
                        this.currentDataFile = f;
                        return true;
                    }
                }

            } catch (IOException ex) {
                plugin.getLogger().severe("PromptData 로드 실패: " + f.getName());
            }
        }

        return false;
    }

    // 현재 NPC 데이터 저장
    public void saveNpcData() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(currentDataFile),
                StandardCharsets.UTF_8
        )) {
            // 현재 전체 데이터(allData)를 저장
            gson.toJson(allData, w);

        } catch (IOException ex) {
            plugin.getLogger().severe("NPC 데이터 저장 실패: " + ex.getMessage());
        }
    }

    // 프롬프트 수정용 GUI 열기
    public void openPromptFixGUI(Player player) {
        // npc 인자는 현재 사용 안 함 (null)
        fileSelector.openGUI(player, null, NpcFileSelector.Mode.PROMPT_FIX);
    }

    // NPC 데이터 수정용 GUI 열기 (프롬프트 FIX 단계에서 호출)
    public void openNpcEditGUI(Player player) {
        guiListener.openFixSelector(player);
    }

    // 현재 전체 데이터 목록 반환
    public List<PromptData> getAllData() {
        if (allData == null) return Collections.emptyList();
        return allData;
    }

    // 현재 데이터 중 특정 number 값과 일치하는 데이터 선택
    public boolean setCurrentDataByCode(String code) {
        for (PromptData d : allData) {
            if (d.code != null && d.code.equals(code)) {
                currentData = d;
                return true;
            }
        }
        return false;
    }
    // 현재 allData 를 모델 서버로 전송
    public void sendReloadPromptToModel() {
        AI_NPC_Plugin main = (AI_NPC_Plugin) plugin;

        if (main.getPersistentModelClient() == null || !main.getPersistentModelClient().isConnected()) {
            main.getLogger().warning("모델 서버와 연결되어 있지 않아 프롬프트를 전송할 수 없습니다.");
            return;
        }

        if (allData == null || allData.isEmpty()) {
            main.getLogger().warning("현재 로드된 프롬프트 데이터가 없습니다.");
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // 프롬프트 형식에 맞게 "npcs" 필드로 보내기
        Map<String, Object> request = new HashMap<>();
        request.put("npcs", allData);

        String jsonData = gson.toJson(request);

        // 모델 서버로 전송
        String response = main.getPersistentModelClient().sendReloadPrompt(jsonData);
        main.getLogger().info("모델 서버 응답 (reload): " + response);
    }
}