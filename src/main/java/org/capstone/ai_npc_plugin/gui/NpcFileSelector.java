package org.capstone.ai_npc_plugin.gui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class NpcFileSelector {

    private final Plugin plugin;
    private final File jsonFolder;
    private final int GUI_SIZE = 54;
    private final int FILES_PER_PAGE = 45;

    private final Map<UUID, Integer> playerScrollIndex = new HashMap<>();
    private final Map<UUID, String> playerSelectedFile = new HashMap<>();
    private final Map<UUID, Villager> playerNpc = new HashMap<>();

    public NpcFileSelector(Plugin plugin) {
        this.plugin = plugin;
        this.jsonFolder = new File(plugin.getDataFolder(), "promptData");
        if (!jsonFolder.exists()) jsonFolder.mkdirs();
    }

    public void openGUI(Player player, Villager npc) {
        List<File> jsonFiles = getSortedJsonFiles();
        int scrollIndex = playerScrollIndex.getOrDefault(player.getUniqueId(), 0);
        int endIndex = Math.min(scrollIndex + FILES_PER_PAGE, jsonFiles.size());

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "📁 NPC 프롬프트 선택");

        for (int i = scrollIndex; i < endIndex; i++) {
            File file = jsonFiles.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.WHITE + file.getName());
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "filename"), PersistentDataType.STRING, file.getName());
                item.setItemMeta(meta);
            }
            gui.setItem(i - scrollIndex, item);
        }
        if (scrollIndex > 0)
            gui.setItem(45, createControlItem(Material.LEVER, "▲ 위로"));
        if (scrollIndex + FILES_PER_PAGE < jsonFiles.size())
            gui.setItem(46, createControlItem(Material.LEVER, "▼ 아래로"));

        gui.setItem(52, createControlItem(Material.LIME_CONCRETE, "✔ 적용"));
        gui.setItem(53, createControlItem(Material.RED_CONCRETE, "✘ 취소"));

        playerNpc.put(player.getUniqueId(), npc);
        player.openInventory(gui);
    }

    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        if (!e.getView().getTitle().equals("📁 NPC 프롬프트 선택")) return;

        e.setCancelled(true);
        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null) return;

        String display = ChatColor.stripColor(meta.getDisplayName());
        NamespacedKey key = new NamespacedKey(plugin, "filename");
        String filename = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        switch (display) {
            case "▲ 위로" -> {
                int current = playerScrollIndex.getOrDefault(player.getUniqueId(), 0);
                playerScrollIndex.put(player.getUniqueId(), Math.max(0, current - 5));
                openGUI(player, playerNpc.get(player.getUniqueId()));
            }
            case "▼ 아래로" -> {
                int current = playerScrollIndex.getOrDefault(player.getUniqueId(), 0);
                playerScrollIndex.put(player.getUniqueId(), current + 5);
                openGUI(player, playerNpc.get(player.getUniqueId()));
            }
            case "✔ 적용" -> {
                String selected = playerSelectedFile.get(player.getUniqueId());
                if (selected != null) {
                    Villager npc = playerNpc.get(player.getUniqueId());
                    if (npc != null) {
                        npc.setCustomName("📜 " + selected.replace(".json", ""));
                        player.sendMessage("✅ 주민에 적용됨: " + selected);
                        // 실제 적용 로직은 이곳에 구현
                    }
                } else {
                    player.sendMessage("⚠ 파일을 먼저 선택하세요.");
                }
                player.closeInventory();
            }
            case "✘ 취소" -> {
                player.sendMessage("❌ 취소되었습니다.");
                player.closeInventory();
            }
            default -> {
                if (filename != null) {
                    playerSelectedFile.put(player.getUniqueId(), filename);
                    player.sendMessage("📌 선택됨: " + filename);
                }
            }
        }
    }

    private ItemStack createControlItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<File> getSortedJsonFiles() {
        File[] files = jsonFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return new ArrayList<>();
        return Arrays.stream(files).sorted().collect(Collectors.toList());
    }
}