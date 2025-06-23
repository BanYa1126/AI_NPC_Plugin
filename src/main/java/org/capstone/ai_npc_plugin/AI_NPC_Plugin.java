package org.capstone.ai_npc_plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.capstone.ai_npc_plugin.command.*;
import org.capstone.ai_npc_plugin.listener.AffinityListener;
import org.capstone.ai_npc_plugin.listener.ChatListener;
import org.capstone.ai_npc_plugin.manager.PromptEditorManager;
import org.capstone.ai_npc_plugin.listener.CombatAssistListener;
import org.capstone.ai_npc_plugin.listener.NpcInteractListener;
import org.capstone.ai_npc_plugin.network.PersistentModelClient;
import org.capstone.ai_npc_plugin.npc.AINPC;
import org.capstone.ai_npc_plugin.manager.NPCStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI_NPC_Plugin 메인 클래스
 *
 * 전체 플러그인 라이프사이클 및 주요 매니저/리소스를 초기화
 *
 * - onEnable() → 활성화 시 호출 (명령어 등록, 이벤트 등록, 주기적 Task 등록)
 * - onDisable() → 비활성화 시 호출 (상태 저장 및 Task 해제)
 *
 * 주요 역할:
 * - globalNpc : 현재 PromptData 기반 글로벌 NPC 설정용
 * - PromptEditorManager : 프롬프트 GUI 및 파일 관리
 * - NPCStateManager : NPC 우호도 관리 (파일 저장 포함)
 * - followMap : 플레이어 → 따라오는 NPC Map
 * - assistMap : 플레이어 → 전투 지원 NPC Map
 * - npcTargetMap : NPC → 현재 타겟 UUID Map
 * - followTask / combatTask : 주기적 AI 동작 처리
 */
public final class AI_NPC_Plugin extends JavaPlugin {

    // 글로벌 NPC 인스턴스 (Prompt 적용 시 사용됨)
    public static AINPC globalNpc = new AINPC();

    // 프롬프트 관리
    private PromptEditorManager promptEditorManager;

    // NPC 우호도 관리
    private NPCStateManager npcStateManager;

    // 자동 저장 task ID
    private int autoSaveTaskId;

    // 플레이어 → 따라오는 NPC (follow 명령어)
    private final Map<UUID, UUID> followMap = new HashMap<>();

    private final Map<UUID, UUID> waitMap = new HashMap<>();

    // 플레이어 → 전투 지원 NPC (assist 명령어)
    private final Map<UUID, UUID> assistMap = new HashMap<>();

    // NPC → 현재 공격중인 타겟
    public Map<UUID, UUID> npcTargetMap = new HashMap<>();

    // 주기적 AI task ID
    private int followTaskId;
    private int combatTaskId;

    private PersistentModelClient persistentModelClient;

    // Getter 메서드
    public Map<UUID, UUID> getFollowMap() { return followMap; }
    public Map<UUID, UUID> getAssistMap() { return assistMap; }
    public Map<UUID, UUID> getNpcTargetMap() { return npcTargetMap; }
    public Map<UUID, UUID> getWaitMap() { return waitMap; }
    public PromptEditorManager getNpcManager() { return promptEditorManager; }
    public NPCStateManager getNpcStateManager() { return npcStateManager; }
    public PersistentModelClient getPersistentModelClient() { return persistentModelClient; }

    /**
     * 플러그인 활성화 시 호출
     */
    @Override
    public void onEnable() {

        // config.yml 기본 저장
        saveDefaultConfig();
        // 프롬프트 폴더 설정값 가져오기
        String folderPath = getConfig().getString("promptDataFolder", "promptData");

        // Prompt 및 NPC 상태 매니저 초기화
        promptEditorManager = new PromptEditorManager(this, folderPath);
        npcStateManager = new NPCStateManager(getDataFolder());
        persistentModelClient = new PersistentModelClient();

        // 모델 클라이언트 연결
        persistentModelClient.connect();

        // 명령어 등록
        getCommand("model").setExecutor(new ModelCommand(this));
        getCommand("ainpc").setExecutor(new AINPCCommand(this, promptEditorManager));
        getCommand("ainpc").setTabCompleter(new AINPCTabCompleter());
        getCommand("model").setTabCompleter(new ModelTabCompleter());
        getCommand("ainpc_action").setExecutor(new AINPCActionCommand(this));
        getCommand("ainpc_action").setTabCompleter(new AINPCActionTabCompleter());

        // 리스너 등록
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new AffinityListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatAssistListener(this), this);

        // 주기적 자동 저장 (5분 주기)
        autoSaveTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(
                this,
                () -> npcStateManager.save(),
                20L * 60L * 5L, // 5분 후 최초 실행
                20L * 60L * 5L  // 이후 주기
        );

        // 주기적 따라오기 AI 동작 Task 등록 (0.5초마다)
        followTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(
                this,
                this::runFollowTask,
                20L, // 1초 지연 후 시작
                10L  // 0.5초 간격
        );

        // 주기적 전투 AI 동작 Task 등록 (0.5초마다)
        combatTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(
                this,
                this::runCombatTask,
                20L,
                10L
        );

        getLogger().info("AI_NPC_Plugin 활성화됨");
    }

    /**
     * 플러그인 비활성화 시 호출
     */
    @Override
    public void onDisable() {
        // 우호도 상태 저장 (최종 저장)
        npcStateManager.save();
        // 주기적 Task 해제
        getServer().getScheduler().cancelTask(autoSaveTaskId);
        getServer().getScheduler().cancelTask(followTaskId);
        getServer().getScheduler().cancelTask(combatTaskId);

        // 모델 서버 종료
        ModelCommand.shutdownModel();

        // 모델 클라이언트 연결 해제
        if (persistentModelClient != null && persistentModelClient.isConnected()) {
            persistentModelClient.disconnect();
        }

        getLogger().info("AI_NPC_Plugin 비활성화됨");
    }

    /**
     * 주기적 따라오기 AI Task
     * 플레이어 → NPC 따라가기 처리
     */
    private void runFollowTask() {
        // 대기 상태 해제: NPC가 사라졌거나 죽었으면 waitMap에서 제거
        getWaitMap().entrySet().removeIf(entry -> {
            UUID npcId = entry.getValue();
            Villager npc = (Villager) Bukkit.getEntity(npcId);
            return (npc == null || npc.isDead());
        });

        for (Map.Entry<UUID, UUID> entry : followMap.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;

            UUID npcId = entry.getValue();
            Villager npc = (Villager) Bukkit.getEntity(npcId);
            if (npc == null || npc.isDead()) continue;

            // 대기 중인 NPC는 이동시키지 않음
            if (getWaitMap().containsKey(player.getUniqueId())) continue;

            npc.setAI(true); // AI 활성화

            Location playerLoc = player.getLocation();
            Location npcLoc = npc.getLocation();
            double distance = playerLoc.distance(npcLoc);

            if (distance > 15) {
                // 15m 이상: 순간이동
                npc.teleport(playerLoc.add(1, 0, 1));
            } else if (distance > 2.5) {
                // 2.5~15m: 이동 경로 설정
                Location behindPlayer = playerLoc.clone().add(playerLoc.getDirection().normalize().multiply(-2));
                behindPlayer.setY(playerLoc.getY()); // 높이 유지
                npc.getPathfinder().moveTo(behindPlayer); // Paper API 기반 이동
            }
            // 2.5 이하: 정지 (이동 명령 없음)
        }
    }
    /**
     * 주기적 전투 AI Task
     * NPC → 타겟 공격 동작
     */
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();

    private void runCombatTask() {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, UUID> entry : npcTargetMap.entrySet()) {
            UUID npcId = entry.getKey();
            UUID targetId = entry.getValue();

            Villager npc = (Villager) Bukkit.getEntity(npcId);
            LivingEntity target = (LivingEntity) Bukkit.getEntity(targetId);

            if (npc == null || npc.isDead() || target == null || target.isDead()) {
                npcTargetMap.remove(npcId);
                continue;
            }

            npc.setAI(true);

            double distance = npc.getLocation().distance(target.getLocation());

            if (distance > 15) {
                npc.teleport(target.getLocation().add(1, 0, 1));
            } else if (distance > 2.5) {
                Location approach = target.getLocation().clone().add(target.getLocation().getDirection().normalize().multiply(-1));
                npc.teleport(approach);
            } else {
                // 공격 쿨타임 확인 (예: 2초)
                long last = lastAttackTime.getOrDefault(npcId, 0L);
                if (now - last >= 2000) {
                    lastAttackTime.put(npcId, now);

                    // 피해 적용
                    target.damage(2.0, npc);

                    // 사운드 효과 (근접 타격 느낌)
                    npc.getWorld().playSound(npc.getLocation(), "entity.player.attack.crit", 1.0f, 1.0f);
                    npc.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation(), 10);
                }
            }
        }
    }
}