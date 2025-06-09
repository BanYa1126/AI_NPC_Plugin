package org.capstone.ai_npc_plugin.listener;

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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.capstone.ai_npc_plugin.gui.DataSelectorHolder;
import org.capstone.ai_npc_plugin.manager.PromptEditorManager;
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.util.*;

/**
 * NpcGUIListener
 *
 * NPC 데이터 선택 및 수정 GUI 를 구성/처리하는 Listener 클래스
 *
 * 주요 기능:
 * - CREATE 모드: NPC 생성 시 PromptData 선택
 * - FIX 모드: NPC 수정 시 PromptData 선택 후 필드 수정 (채팅 입력 기반)
 *
 * 기능 설명:
 * - 페이지네이션 지원
 * - 필드별 수정 가능 (name, age, gender, job, personality, background)
 * - AsyncPlayerChatEvent 를 이용한 텍스트 입력 기반 수정
 *
 * 이벤트 처리:
 * - InventoryClickEvent
 * - InventoryDragEvent
 * - AsyncPlayerChatEvent
 */

public class NpcGUIListener implements Listener {
    // 현재 플레이어별 GUI 모드 (CREATE / FIX)
    private enum DataMode { CREATE, FIX }
    private final Map<UUID, DataMode> playerDataMode = new HashMap<>();
    // CREATE 모드 시, 플레이어가 선택한 NPC 객체 저장 (Villager)
    private final Map<UUID, Villager> playerNpcForCreate = new HashMap<>();
    // 플러그인 인스턴스
    private final Plugin plugin;
    // PromptEditorManager 참조
    private final PromptEditorManager manager;
    // GUI 설정 값
    private static final int GUI_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;
    // 플레이어별 현재 페이지
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    // 플레이어별 현재 선택된 데이터 번호
    private final Map<UUID, Integer> playerSelected = new HashMap<>();
    // 수정 모드에서 사용되는 플레이어별 편집 상태
    private static class EditState {
        PromptData data; // 현재 수정 중인 데이터
        int step;       // 단계 (0: 필드 선택, 1: 새 값 입력)
        int selectedField; // 선택한 필드 인덱스
    }
    private final Map<UUID, EditState> editing = new HashMap<>();

    // 생성자
    public NpcGUIListener(Plugin plugin, PromptEditorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // FIX 모드: 기존 데이터 수정용 GUI 열기
    public void openFixSelector(Player player) {
        playerDataMode.put(player.getUniqueId(), DataMode.FIX);
        showDataGui(player, "📋 NPC 수정용 데이터 선택");
    }

    // CREATE 모드: NPC 생성 시 데이터 선택 GUI 열기
    public void openCreateSelector(Player player, Villager npc) {
        playerDataMode.put(player.getUniqueId(), DataMode.CREATE);
        playerNpcForCreate.put(player.getUniqueId(), npc);
        showDataGui(player, "📋 NPC 생성용 데이터 선택");
    }

    // 내부 헬퍼: 현재 모드에 따라 GUI 다시 열기
    private void openSelector(Player player) {
        UUID id = player.getUniqueId();
        DataMode mode = playerDataMode.get(id);

        if (mode == DataMode.CREATE) {
            Villager npc = playerNpcForCreate.get(id);
            openCreateSelector(player, npc);
        } else {
            openFixSelector(player);
        }
    }

    // 공통 GUI 표시 헬퍼 (이 안에 기존 openSelector 코드를 통째로 이동)
    private void showDataGui(Player player, String title) {
        List<PromptData> dataList = manager.getAllData();
        int page    = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start   = page * ITEMS_PER_PAGE;
        int end     = Math.min(start + ITEMS_PER_PAGE, dataList.size());

        DataSelectorHolder holder = new DataSelectorHolder(
                playerDataMode.get(player.getUniqueId()) == DataMode.CREATE
                        ? DataSelectorHolder.DataMode.CREATE
                        : DataSelectorHolder.DataMode.FIX,
                null
        );
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);
        Integer selNum = playerSelected.get(player.getUniqueId());

        // — 기존 반복문: 아이템 세팅 그대로 —
        for (int i = start; i < end; i++) {
            PromptData data = dataList.get(i);
            ItemStack item  = new ItemStack(Material.PAPER);
            ItemMeta meta   = item.getItemMeta();

            // 제목 & 선택 강조
            String disp = ChatColor.WHITE + String.valueOf(data.number);
            if (selNum != null && selNum.equals(data.number)) {
                disp = ChatColor.YELLOW + "✔ " + data.number;
            }
            meta.setDisplayName(disp);

            // lore 설정
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Name: " + data.name);
            lore.add(ChatColor.GRAY + "Age: " + data.age);
            lore.add(ChatColor.GRAY + "Gender: " + data.gender);
            lore.add(ChatColor.GRAY + "Job: " + data.job);
            lore.add(ChatColor.GRAY + "Personality: " + String.join(", ", data.personality));
            lore.add(ChatColor.GRAY + "Background: " + data.background);
            if (selNum != null && selNum.equals(data.number)) {
                lore.add(ChatColor.GOLD + "[선택됨]");
            }
            meta.setLore(lore);

            // npc_number 저장
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "npc_number"),
                    PersistentDataType.INTEGER,
                    data.number
            );
            item.setItemMeta(meta);

            int slot = i - start;
            if (slot >= ITEMS_PER_PAGE) break;
            gui.setItem(slot, item);
        }

        // 페이징 버튼
        if (page > 0) gui.setItem(49, control(Material.LEVER, "이전 페이지"));
        if (end < dataList.size()) gui.setItem(50, control(Material.LEVER, "다음 페이지"));

        // ✔ 버튼 텍스트만 모드별로 바꿔 주기
        DataMode mode = playerDataMode.get(player.getUniqueId());
        String confirmText = (mode == DataMode.CREATE) ? "✔ 선택" : "✔ 변경";
        gui.setItem(52, control(Material.LIME_CONCRETE, confirmText));

        // 취소 버튼
        gui.setItem(53, control(Material.RED_CONCRETE, "✘ 취소"));

        player.openInventory(gui);
    }

    // GUI 클릭 이벤트 처리
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getInventory().getHolder() instanceof DataSelectorHolder holder)) return;

        e.setCancelled(true);
        UUID id = p.getUniqueId();
        DataSelectorHolder.DataMode dataMode = holder.getMode();

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String label = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (label) {
            case "이전 페이지" -> {
                int pg = playerPage.getOrDefault(id, 0);
                playerPage.put(id, Math.max(0, pg - 1));
                openSelector(p);
            }
            case "다음 페이지" -> {
                int pg2 = playerPage.getOrDefault(id, 0);
                playerPage.put(id, pg2 + 1);
                openSelector(p);
            }
            case "✔ 선택", "✔ 변경" -> {
                Integer sel = playerSelected.get(id);
                if (sel == null) {
                    p.sendMessage(ChatColor.RED + "먼저 항목을 선택하세요.");
                    return;
                }

                // 공통: 선택된 데이터 로드
                manager.setCurrentData(sel);
                PromptData d = manager.getCurrentData();

                if (dataMode == DataSelectorHolder.DataMode.CREATE) {
                    // ─ CREATE 모드: NPC 스폰 후 이름 설정
                    Villager npc = playerNpcForCreate.remove(id);
                    npc.setCustomName(d.name);
                    p.sendMessage(ChatColor.GREEN + "NPC 생성 및 이름 설정: " + d.name);
                    p.closeInventory();

                } else { // 수정 모드 진입
                    p.closeInventory();

                    EditState st = new EditState();
                    st.data = d;
                    st.step = 0;  // 0: 번호 입력 대기
                    editing.put(id, st);

                    // 현재 필드 & 값 출력
                    p.sendMessage(ChatColor.YELLOW + "수정 가능한 항목과 현재 값:");
                    for (int i = 1; i <= 6; i++) {
                        String fname = getFieldName(i);
                        String fval   = getFieldValue(d, fname);
                        p.sendMessage(
                                " " + i
                                        + ") " + ChatColor.AQUA + fname
                                        + ChatColor.GOLD  + " : " + fval
                        );
                    }
                    p.sendMessage(ChatColor.YELLOW + "수정할 항목 번호(1~6)를 채팅으로 입력하세요.");
                }
            }
            case "✘ 취소" -> p.closeInventory();

            default -> {
                // 데이터 아이콘 클릭: 번호 저장 & GUI 리프레시
                Integer num = clicked.getItemMeta()
                        .getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "npc_number"),
                                PersistentDataType.INTEGER);
                if (num != null) {
                    playerSelected.put(id, num);
                    p.sendMessage(ChatColor.GOLD + "📌 선택됨: NPC #" + num);
                    openSelector(p);
                }
            }
        }
    }

    // GUI에서 드래그 방지
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals("📋 NPC 선택")) {
            e.setCancelled(true);
        }
    }

    // 수정 단계: 채팅 입력 처리
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (!editing.containsKey(id)) return;
        e.setCancelled(true);

        EditState st = editing.get(id);
        String msg = e.getMessage().trim();

        if (st.step == 0) {
            try {
                int idx = Integer.parseInt(msg);
                if (idx >= 1 && idx <= 6) {
                    st.selectedField = idx;
                    st.step = 1;
                    String fieldName = getFieldName(idx);
                    String fieldVal  = getFieldValue(st.data, fieldName);

                    p.sendMessage(
                            ChatColor.GREEN.toString() + idx + "번 "
                                    + ChatColor.AQUA + fieldName
                                    + ChatColor.GOLD + " : " + fieldVal
                                    + ChatColor.GREEN + " 항목이 선택되었습니다."
                    );
                    p.sendMessage(ChatColor.YELLOW + "새 값을 입력하세요:");
                } else {
                    p.sendMessage(ChatColor.RED + "1~6 사이의 숫자를 입력하세요.");
                }
            } catch (NumberFormatException ex) {
                p.sendMessage(ChatColor.RED + "숫자를 입력해주세요.");
            }
        } else { // 값 입력 후 저장
            PromptData d = st.data;
            String val = msg;
            switch (st.selectedField) {
                case 1: d.name = val; break;
                case 2: d.age = val; break;
                case 3: d.gender = val; break;
                case 4: d.job = val; break;
                case 5: d.personality = Arrays.asList(val.split(",\\s*")); break;
                case 6: d.background = val; break;
            }
            Bukkit.getScheduler().runTask(plugin, manager::saveNpcData);
            p.sendMessage(ChatColor.GREEN + "✔ 수정 완료: " + getFieldName(st.selectedField) + " -> " + val);
            editing.remove(p.getUniqueId());
        }
    }

    private String getFieldValue(PromptData d, String field) {
        return switch(field) {
            case "name" -> d.name;
            case "age" -> d.age;
            case "gender" -> d.gender;
            case "job" -> d.job;
            case "personality" -> String.join(", ", d.personality);
            case "background" -> d.background;
            default -> "";
        };
    }

    private String getFieldName(int idx) {
        return switch(idx) {
            case 1 -> "name";
            case 2 -> "age";
            case 3 -> "gender";
            case 4 -> "job";
            case 5 -> "personality";
            case 6 -> "background";
            default -> "";
        };
    }

    private ItemStack control(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }
}