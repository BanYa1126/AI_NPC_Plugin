package org.capstone.ai_npc_plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
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

    public void openSelector(Player player) {
        List<PromptData> dataList = manager.getAllData();
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, dataList.size());

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "📋 NPC 선택");
        Integer selectedNumber = playerSelected.get(player.getUniqueId());

        for (int i = start; i < end; i++) {
            PromptData data = dataList.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            // 제목과 선택 강조
            String title = ChatColor.WHITE + String.valueOf(data.number);
            if (selectedNumber != null && selectedNumber.equals(data.number)) {
                title = ChatColor.YELLOW + "✔ " + data.number;
            }
            meta.setDisplayName(title);

            // lore 설정
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Name: " + data.name);
            lore.add(ChatColor.GRAY + "Age: " + data.age);
            lore.add(ChatColor.GRAY + "Gender: " + data.gender);
            lore.add(ChatColor.GRAY + "Job: " + data.job);
            lore.add(ChatColor.GRAY + "Personality: " + String.join(", ", data.personality));
            lore.add(ChatColor.GRAY + "Background: " + data.background);
            if (selectedNumber != null && selectedNumber.equals(data.number)) {
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

        // 페이징 및 컨트롤 버튼
        if (page > 0) gui.setItem(49, control(Material.LEVER, "이전"));
        if (end < manager.getAllData().size()) gui.setItem(50, control(Material.LEVER, "다음"));
        gui.setItem(52, control(Material.LIME_CONCRETE, "✔ 변경"));
        gui.setItem(53, control(Material.RED_CONCRETE, "✘ 취소"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals("📋 수정할 NPC 데이터 선택")) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String label = ChatColor.stripColor(meta.getDisplayName());

        switch (label) {
            case "이전" -> {
                int pg = playerPage.getOrDefault(p.getUniqueId(), 0);
                playerPage.put(p.getUniqueId(), Math.max(0, pg - 1));
                openSelector(p);
            }
            case "다음" -> {
                int pg2 = playerPage.getOrDefault(p.getUniqueId(), 0);
                playerPage.put(p.getUniqueId(), pg2 + 1);
                openSelector(p);
            }
            case "✔ 변경" -> {
                Integer sel = playerSelected.get(p.getUniqueId());
                if (sel != null && manager.setCurrentData(sel)) {
                    EditState st = new EditState();
                    st.data = manager.getCurrentData();
                    st.step = 0;
                    editing.put(p.getUniqueId(), st);
                    p.closeInventory();
                    p.sendMessage(ChatColor.YELLOW + "수정할 항목 번호를 입력하세요:");
                    String[] fields = {"name","age","gender","job","personality","background"};
                    for (int i = 0; i < fields.length; i++) {
                        p.sendMessage(ChatColor.GOLD + "" + (i+1) + ". " + fields[i] + " : " + getFieldValue(st.data, fields[i]));
                    }
                } else {
                    p.sendMessage(ChatColor.RED + "먼저 NPC를 선택하세요.");
                }
            }
            case "✘ 취소" -> p.closeInventory();
            default -> {
                Integer num = meta.getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "npc_number"), PersistentDataType.INTEGER);
                if (num != null) {
                    playerSelected.put(p.getUniqueId(), num);
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