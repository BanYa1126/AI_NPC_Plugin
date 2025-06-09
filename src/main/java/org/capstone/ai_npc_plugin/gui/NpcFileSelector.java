package org.capstone.ai_npc_plugin.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.capstone.ai_npc_plugin.listener.NpcGUIListener;
import org.capstone.ai_npc_plugin.manager.PromptEditorManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * NpcFileSelector
 *
 * 프롬프트 파일 선택 GUI 를 구성/처리하는 Listener 클래스
 *
 * 주요 기능:
 * - PROMPT_SET : 프롬프트 적용용 GUI 표시
 * - PROMPT_FIX : 프롬프트 수정용 GUI 표시
 *
 * 기능 설명:
 * - Json 파일 목록 표시 (페이지네이션)
 * - 선택/적용/취소 버튼 지원
 * - 플레이어가 선택한 프롬프트 파일을 PromptEditorManager 에 적용
 *
 * 이벤트 처리:
 * - InventoryClickEvent
 * - InventoryDragEvent
 */

public class NpcFileSelector implements Listener {

    // 프롬프트 파일 선택 GUI의 모드 구분용 열거형
    public enum Mode { PROMPT_SET, PROMPT_FIX }

    // 의존성 필드
    private final PromptEditorManager manager; // 프롬프트 매니저
    private final Plugin plugin; // 플러그인 인스턴스
    private final File jsonFolder; // JSON 파일 폴더
    private final NpcGUIListener fixListener; // 수정용 GUI 리스너 (FIX 모드에서 사용 가능)

    // GUI 구성 상수
    private static final int GUI_SIZE = 54; // 6행 * 9열
    private static final int FILES_PER_PAGE = 45; // 5행 * 9열 (파일 표시 영역)

    // 플레이어별 상태 저장용 맵
    private final Map<UUID,String> selectedForSet = new HashMap<>(); // PROMPT_SET 모드에서 선택한 파일명
    private final Map<UUID,String> selectedForFix = new HashMap<>(); // PROMPT_FIX 모드에서 선택한 파일명
    private final Map<UUID, Integer> playerScroll = new HashMap<>(); // 플레이어별 현재 페이지 인덱스
    private final Map<UUID, Villager> playerNpc = new HashMap<>(); // 플레이어별 연결된 Villager 객체 (CREATE 시 필요)

    // 생성자 - 필드 초기화 및 이벤트 리스너 등록
    public NpcFileSelector(Plugin plugin,
                           File jsonFolder,
                           PromptEditorManager manager,
                           NpcGUIListener fixListener) {
        this.plugin = plugin;
        this.jsonFolder = jsonFolder;
        this.manager = manager;
        this.fixListener = fixListener;

        // JSON 폴더가 없으면 생성
        if (!jsonFolder.exists()) jsonFolder.mkdirs();

        // 이벤트 리스너 등록
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // GUI 열기 메서드
    // mode 에 따라 제목과 적용 버튼 텍스트가 달라짐
    public void openGUI(Player player, Villager npc, Mode mode) {
        UUID id = player.getUniqueId();
        playerScroll.remove(id); // 페이지 인덱스 초기화
        playerNpc.put(id, npc); // 연결된 NPC 저장

        // Holder 설정 (구분용)
        FileSelectorHolder holder = new FileSelectorHolder(mode);

        // JSON 파일 목록 정렬
        List<File> files = getSortedJsonFiles();
        int idx = playerScroll.getOrDefault(id, 0); // 현재 페이지 시작 인덱스
        int end = Math.min(idx + FILES_PER_PAGE, files.size()); // 페이지 끝 인덱스

        // GUI 제목 설정
        String title = (mode == Mode.PROMPT_SET
                ? "📁 Prompt Set 선택"
                : "📁 Prompt Fix 선택"
        );

        // GUI 생성
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);

        // 현재 플레이어가 이미 선택한 파일명 (있으면 체크 표시용)
        String already = (mode == Mode.PROMPT_SET
                ? selectedForSet.get(id)
                : selectedForFix.get(id)
        );

        // 파일 목록을 GUI에 표시
        for (int i = idx; i < end; i++) {
            File f = files.get(i);
            String jsonName = "";
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8)) {

                // JSON 파일에서 name 필드 읽기
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    if (obj.has("name")) jsonName = obj.get("name").getAsString();
                } else if (root.isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray();
                    List<String> names = new ArrayList<>();
                    for (JsonElement el : arr) {
                        if (el.isJsonObject()) {
                            JsonObject o = el.getAsJsonObject();
                            if (o.has("name")) names.add(o.get("name").getAsString());
                        }
                    }
                    jsonName = String.join(", ", names);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("프롬프트 파싱 실패: " + f.getName());
            }

            // GUI 항목 생성
            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();
            boolean isSel = f.getName().equals(already);

            // 파일명 + 선택 표시
            m.setDisplayName(
                    isSel
                            ? ChatColor.YELLOW + "✔ " + f.getName()
                            : ChatColor.WHITE  + f.getName()
            );

            // Lore 설정 (JSON 내부 name 정보 표시)
            m.setLore(Collections.singletonList(ChatColor.GRAY + parseJsonNames(f)));

            // filename 태그 저장
            m.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "filename"),
                            PersistentDataType.STRING,
                            f.getName());
            it.setItemMeta(m);

            // GUI 슬롯에 추가
            gui.setItem(i - idx, it);
        }

        // 페이징 버튼
        if (idx > 0) gui.setItem(49, control(Material.LEVER, "이전 페이지"));
        if (end < files.size()) gui.setItem(50, control(Material.LEVER, "다음 페이지"));

        // 적용/취소 버튼
        String applyText = mode == Mode.PROMPT_SET ? "✔ 적용" : "✔ 선택";
        gui.setItem(52, control(Material.LIME_CONCRETE, applyText));
        gui.setItem(53, control(Material.RED_CONCRETE, "✘ 취소"));

        // GUI 열기
        player.openInventory(gui);
    }

    // GUI 클릭 이벤트 처리
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getInventory().getHolder() instanceof FileSelectorHolder holder)) return;

        e.setCancelled(true); // GUI 내 클릭 동작 취소

        UUID id = p.getUniqueId();
        Mode mode = holder.getMode();

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String label = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        String fn = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "filename"),
                        PersistentDataType.STRING);

        // 버튼 처리
        switch (label) {
            case "이전 페이지" -> {
                playerScroll.put(id, Math.max(0, playerScroll.getOrDefault(id, 0) - FILES_PER_PAGE));
                openGUI(p, playerNpc.get(id), mode);
            }
            case "다음 페이지" -> {
                playerScroll.put(id, playerScroll.getOrDefault(id, 0) + FILES_PER_PAGE);
                openGUI(p, playerNpc.get(id), mode);
            }
            case "✘ 취소" -> p.closeInventory();

            case "✔ 적용", "✔ 선택" -> {
                // 현재 선택한 파일 확인
                String sel = (mode == Mode.PROMPT_SET
                        ? selectedForSet.get(id)
                        : selectedForFix.get(id)
                );
                if (sel == null) {
                    p.sendMessage(ChatColor.RED + "먼저 파일을 선택하세요.");
                    return;
                }

                // 프롬프트 파일 로드 시도
                if (!manager.loadPromptFile(sel)) {
                    p.sendMessage(ChatColor.RED + "파일 로드에 실패했습니다: " + sel);
                    p.closeInventory();
                    return;
                }

                // 적용 후 처리
                if (mode == Mode.PROMPT_SET) {
                    p.sendMessage(ChatColor.GREEN + "프롬프트 파일 적용 완료: " + sel);
                    p.closeInventory();
                } else {
                    p.closeInventory();
                    manager.openNpcEditGUI(p);
                }
            }

            default -> {
                // 일반 파일 클릭 시 → 선택 상태 저장 후 리프레시
                if (fn != null) {
                    if (mode == Mode.PROMPT_SET) {
                        selectedForSet.put(id, fn);
                    } else {
                        selectedForFix.put(id, fn);
                    }
                    p.sendMessage(ChatColor.GOLD + "[선택됨] " + fn);
                    openGUI(p, playerNpc.get(id), mode);
                }
            }
        }
    }

    // GUI 드래그 방지
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().startsWith("📁")) {
            e.setCancelled(true);
        }
    }

    // JSON 파일에서 name 필드 추출
    private String parseJsonNames(File f) {
        try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                return obj.has("name") ? obj.get("name").getAsString() : "";
            } else if (root.isJsonArray()) {
                List<String> names = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray()) {
                    if (el.isJsonObject()) {
                        JsonObject o = el.getAsJsonObject();
                        if (o.has("name")) names.add(o.get("name").getAsString());
                    }
                }
                return String.join(", ", names);
            }
        } catch (IOException ignored) {}
        return "";
    }

    // GUI 버튼 생성 헬퍼
    private ItemStack control(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }

    // JSON 파일 목록 정렬 반환
    private List<File> getSortedJsonFiles() {
        File[] arr = jsonFolder.listFiles((d,n)->n.toLowerCase().endsWith(".json"));
        return arr == null
                ? Collections.emptyList()
                : Arrays.stream(arr).sorted().toList();
    }
}