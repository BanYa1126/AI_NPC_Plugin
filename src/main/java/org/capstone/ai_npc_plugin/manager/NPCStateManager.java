package org.capstone.ai_npc_plugin.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NPCStateManager
 *
 * AI NPC의 개별 상태(현재는 우호도)를 **파일로 저장/로드**하는 관리 클래스
 *
 * - 핵심 기능: Villager UUID → 우호도 점수(Map)
 * - 파일 저장 형식: JSON (npc_states.json)
 * - 서버 종료 시 save(), 서버 시작 시 load()
 *
 * 사용하는 곳:
 * - AffinityListener (Shift 우클릭, 공격 이벤트)
 * - CombatAssistListener (동료 상태 확인 시)
 * - AINPCActionCommand (요청 처리 시 우호도 기준 확인)
 */

public class NPCStateManager {
    // 저장할 파일
    private final File dataFile;
    // JSON 변환기
    private final Gson gson = new Gson();
    // 실제 우호도 데이터 (Villager UUID → 점수 0~100)
    private final Map<UUID, Integer> affinityMap = new HashMap<>();
    /**
     * 생성자
     * @param folder 저장할 폴더 (예: plugin.getDataFolder())
     */
    public NPCStateManager(File folder) {
        // 파일명은 npc_states.json 으로 고정
        this.dataFile = new File(folder, "npc_states.json");
        // 서버 시작 시 자동 로드 시도
        load();
    }
    /**
     * 우호도 설정
     * @param npcId Villager UUID
     * @param score 점수 (자동으로 0~100으로 클램프됨)
     */
    public void setAffinity(UUID npcId, int score) {
        affinityMap.put(npcId, Math.max(0, Math.min(100, score)));
    }
    /**
     * 우호도 조회
     * @param npcId Villager UUID
     * @return 현재 점수 (없으면 기본 50 반환)
     */
    public int getAffinity(UUID npcId) {
        return affinityMap.getOrDefault(npcId, 50); // 기본 50
    }
    /**
     * 우호도 상태 저장 (npc_states.json)
     * - JSON 직렬화
     * - save() 는 주로 서버 종료 시 호출됨
     */
    public void save() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
            gson.toJson(affinityMap, writer);
            Bukkit.getLogger().info("[AI_NPC_Plugin] NPC 상태 저장 완료 (" + affinityMap.size() + "개).");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 우호도 상태 로드 (npc_states.json)
     * - JSON 역직렬화
     * - 서버 시작 시 자동 호출됨
     */
    public void load() {
        if (!dataFile.exists()) return; // 파일 없으면 생략

        try (Reader reader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<UUID, Integer>>() {}.getType();
            Map<UUID, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                affinityMap.clear();
                affinityMap.putAll(loaded);
                Bukkit.getLogger().info("[AI_NPC_Plugin] NPC 상태 로드 완료 (" + affinityMap.size() + "개).");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

