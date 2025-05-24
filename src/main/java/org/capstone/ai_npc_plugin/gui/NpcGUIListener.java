package org.capstone.ai_npc_plugin.gui;

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
import org.capstone.ai_npc_plugin.npc.PromptData;

import java.util.*;

public class NpcGUIListener implements Listener {
    private enum DataMode { CREATE, FIX }
    private final Map<UUID,DataMode> playerDataMode = new HashMap<>();
    private final Map<UUID, Villager> playerNpcForCreate = new HashMap<>();
    private final Plugin plugin;
    private final PromptEditorManager manager;
    private static final int GUI_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, Integer> playerSelected = new HashMap<>();

    private static class EditState {
        PromptData data;
        int step;          // 0 = 필드선택, 1 = 새값입력
        int selectedField;
    }
    private final Map<UUID, EditState> editing = new HashMap<>();

    public NpcGUIListener(Plugin plugin, PromptEditorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void openFixSelector(Player player) {
        playerDataMode.put(player.getUniqueId(), DataMode.FIX);
        showDataGui(player, "📋 NPC 수정용 데이터 선택");
    }

    // 3) openCreateSelector: create 명령어에서 호출
    public void openCreateSelector(Player player, Villager npc) {
        playerDataMode.put(player.getUniqueId(), DataMode.CREATE);
        playerNpcForCreate.put(player.getUniqueId(), npc);
        showDataGui(player, "📋 NPC 생성용 데이터 선택");
    }

    private void openSelector(Player player) {
        UUID id = player.getUniqueId();
        DataMode mode = playerDataMode.get(id);
        if (mode == DataMode.CREATE) {
            // create 모드 → Villager 객체도 꺼내서 전달
            Villager npc = playerNpcForCreate.get(id);
            openCreateSelector(player, npc);
        } else {
            // fix 모드 → 단순히 수정용 선택 GUI
            openFixSelector(player);
        }
    }

    // 4) 공통 GUI 표시 헬퍼 (이 안에 기존 openSelector 코드를 통째로 이동)
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

                } else {
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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals("📋 NPC 선택")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!editing.containsKey(p.getUniqueId())) return;
        e.setCancelled(true);

        EditState st = editing.get(p.getUniqueId());
        String msg = e.getMessage().trim();

        if (st.step == 0) {
            try {
                int idx = Integer.parseInt(msg);
                if (idx >= 1 && idx <= 6) {
                    st.selectedField = idx;
                    st.step = 1;
                    p.sendMessage(ChatColor.YELLOW + "새 값을 입력하세요:");
                } else {
                    p.sendMessage(ChatColor.RED + "1~6 사이의 숫자를 입력하세요.");
                }
            } catch (NumberFormatException ex) {
                p.sendMessage(ChatColor.RED + "숫자를 입력해주세요.");
            }
        } else {
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