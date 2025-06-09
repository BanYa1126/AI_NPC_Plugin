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
 */
public class NpcGUIListener implements Listener {

    private enum DataMode { CREATE, FIX }
    private final Map<UUID, DataMode> playerDataMode = new HashMap<>();
    private final Map<UUID, Villager> playerNpcForCreate = new HashMap<>();
    private final Plugin plugin;
    private final PromptEditorManager manager;

    private static final int GUI_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, String> playerSelectedCode = new HashMap<>();

    private static class EditState {
        PromptData data;
        int step;
        String selectedField;
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

    public void openCreateSelector(Player player, Villager npc) {
        playerDataMode.put(player.getUniqueId(), DataMode.CREATE);
        playerNpcForCreate.put(player.getUniqueId(), npc);
        showDataGui(player, "📋 NPC 생성용 데이터 선택");
    }

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

    private void showDataGui(Player player, String title) {
        List<PromptData> dataList = manager.getAllData();
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, dataList.size());

        DataSelectorHolder holder = new DataSelectorHolder(
                playerDataMode.get(player.getUniqueId()) == DataMode.CREATE
                        ? DataSelectorHolder.DataMode.CREATE
                        : DataSelectorHolder.DataMode.FIX,
                null
        );
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);
        String selCode = playerSelectedCode.get(player.getUniqueId());

        for (int i = start; i < end; i++) {
            PromptData data = dataList.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            String disp = ChatColor.WHITE + data.code;
            if (selCode != null && selCode.equals(data.code)) {
                disp = ChatColor.YELLOW + "✔ " + data.code;
            }
            meta.setDisplayName(disp);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Name: " + data.name);
            lore.add(ChatColor.GRAY + "Job: " + data.job);
            lore.add(ChatColor.GRAY + "Relation: " + data.relation);
            lore.add(ChatColor.GRAY + "City: " + data.city);
            lore.add(ChatColor.GRAY + "Description: " + data.description);
            if (selCode != null && selCode.equals(data.code)) {
                lore.add(ChatColor.GOLD + "[선택됨]");
            }
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "npc_code"),
                    PersistentDataType.STRING,
                    data.code
            );
            item.setItemMeta(meta);

            int slot = i - start;
            if (slot >= ITEMS_PER_PAGE) break;
            gui.setItem(slot, item);
        }

        if (page > 0) gui.setItem(49, control(Material.LEVER, "이전 페이지"));
        if (end < dataList.size()) gui.setItem(50, control(Material.LEVER, "다음 페이지"));

        DataMode mode = playerDataMode.get(player.getUniqueId());
        String confirmText = (mode == DataMode.CREATE) ? "✔ 선택" : "✔ 변경";
        gui.setItem(52, control(Material.LIME_CONCRETE, confirmText));

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
                String selCode = playerSelectedCode.get(id);
                if (selCode == null) {
                    p.sendMessage(ChatColor.RED + "먼저 항목을 선택하세요.");
                    return;
                }

                manager.setCurrentDataByCode(selCode);
                PromptData d = manager.getCurrentData();

                if (dataMode == DataSelectorHolder.DataMode.CREATE) {
                    Villager npc = playerNpcForCreate.remove(id);
                    npc.setCustomName(d.name);
                    p.sendMessage(ChatColor.GREEN + "NPC 생성 및 이름 설정: " + d.name);
                    p.closeInventory();
                } else {
                    p.closeInventory();

                    EditState st = new EditState();
                    st.data = d;
                    st.step = 0;
                    editing.put(id, st);

                    p.sendMessage(ChatColor.YELLOW + "수정 가능한 항목과 현재 값:");
                    int idx = 1;
                    for (String field : getEditableFields()) {
                        String value = getFieldValue(d, field);
                        p.sendMessage(
                                " " + idx + ") " + ChatColor.AQUA + field
                                        + ChatColor.GOLD + " : " + value
                        );
                        idx++;
                    }
                    p.sendMessage(ChatColor.YELLOW + "수정할 항목 번호(1~" + getEditableFields().length + ")를 채팅으로 입력하세요.");
                }
            }
            case "✘ 취소" -> p.closeInventory();

            default -> {
                String code = clicked.getItemMeta()
                        .getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "npc_code"),
                                PersistentDataType.STRING);
                if (code != null) {
                    playerSelectedCode.put(id, code);
                    p.sendMessage(ChatColor.GOLD + "📌 선택됨: NPC [" + code + "]");
                    openSelector(p);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().startsWith("📋 NPC")) {
            e.setCancelled(true);
        }
    }

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
                if (idx >= 1 && idx <= getEditableFields().length) {
                    st.selectedField = getEditableFields()[idx - 1];
                    st.step = 1;
                    String value = getFieldValue(st.data, st.selectedField);

                    p.sendMessage(ChatColor.GREEN + "선택됨: " + st.selectedField + " → 현재 값: " + value);
                    p.sendMessage(ChatColor.YELLOW + "새 값을 입력하세요:");
                } else {
                    p.sendMessage(ChatColor.RED + "올바른 번호를 입력하세요.");
                }
            } catch (NumberFormatException ex) {
                p.sendMessage(ChatColor.RED + "숫자를 입력해주세요.");
            }
        } else {
            setFieldValue(st.data, st.selectedField, msg);
            Bukkit.getScheduler().runTask(plugin, manager::saveNpcData);
            p.sendMessage(ChatColor.GREEN + "✔ 수정 완료: " + st.selectedField + " → " + msg);
            editing.remove(p.getUniqueId());
        }
    }

    private String[] getEditableFields() {
        return new String[] {
                "name", "era", "job", "social_status", "gender", "relation", "city", "description", "background_code"
        };
    }

    private String getFieldValue(PromptData d, String field) {
        return switch (field) {
            case "name" -> d.name;
            case "era" -> d.era;
            case "job" -> d.job;
            case "social_status" -> d.social_status;
            case "gender" -> d.gender;
            case "relation" -> d.relation;
            case "city" -> d.city;
            case "description" -> d.description;
            case "background_code" -> d.background_code;
            default -> "";
        };
    }

    private void setFieldValue(PromptData d, String field, String value) {
        switch (field) {
            case "name" -> d.name = value;
            case "era" -> d.era = value;
            case "job" -> d.job = value;
            case "social_status" -> d.social_status = value;
            case "gender" -> d.gender = value;
            case "relation" -> d.relation = value;
            case "city" -> d.city = value;
            case "description" -> d.description = value;
            case "background_code" -> d.background_code = value;
        }
    }

    private ItemStack control(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }
}