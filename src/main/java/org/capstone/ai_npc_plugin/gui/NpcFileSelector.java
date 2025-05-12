package org.capstone.ai_npc_plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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

    private final Map<UUID, Integer> playerScroll = new HashMap<>();
    private final Map<UUID, String> playerSelected = new HashMap<>();
    private final Map<UUID, Villager> playerNpc = new HashMap<>();

    public NpcFileSelector(Plugin plugin, File jsonFolder) {
        this.plugin = plugin;
        this.jsonFolder = jsonFolder;
        if (!jsonFolder.exists()) jsonFolder.mkdirs();
    }

    public void openGUI(Player player, Villager npc) {
        List<File> files = getSortedJsonFiles();
        int idx = playerScroll.getOrDefault(player.getUniqueId(), 0);
        int end = Math.min(idx + FILES_PER_PAGE, files.size());

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "📁 NPC 프롬프트 선택");

        for (int i = idx; i < end; i++) {
            File f = files.get(i);
            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(ChatColor.WHITE + f.getName());
            m.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "filename"), PersistentDataType.STRING, f.getName());
            it.setItemMeta(m);
            gui.setItem(i - idx, it);
        }

        if (idx > 0)          gui.setItem(45, control(Material.LEVER, "▲ 위로"));
        if (end < files.size()) gui.setItem(46, control(Material.LEVER, "▼ 아래로"));

        gui.setItem(52, control(Material.LIME_CONCRETE, "✔ 적용"));
        gui.setItem(53, control(Material.RED_CONCRETE, "✘ 취소"));

        playerNpc.put(player.getUniqueId(), npc);
        player.openInventory(gui);
    }

    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getCurrentItem() == null) return;
        if (!e.getView().getTitle().equals("📁 NPC 프롬프트 선택")) return;
        e.setCancelled(true);

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null) return;
        String name = ChatColor.stripColor(meta.getDisplayName());
        String fn = meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "filename"), PersistentDataType.STRING);

        switch (name) {
            case "▲ 위로" -> { scroll(p, -5); }
            case "▼ 아래로" -> { scroll(p, +5); }
            case "✔ 적용" -> apply(p);
            case "✘ 취소" -> p.closeInventory();
            default -> { if (fn != null) {
                playerSelected.put(p.getUniqueId(), fn);
                p.sendMessage("📌 선택됨: " + fn);
            }
            }
        }
    }

    private void scroll(Player p, int delta) {
        UUID id = p.getUniqueId();
        int cur = playerScroll.getOrDefault(id, 0);
        playerScroll.put(id, Math.max(0, cur + delta));
        openGUI(p, playerNpc.get(id));
    }

    private void apply(Player p) {
        String fn = playerSelected.get(p.getUniqueId());
        Villager npc = playerNpc.get(p.getUniqueId());
        if (fn != null && npc != null) {
            npc.setCustomName("📜 " + fn.replace(".json", ""));
            p.sendMessage("✅ 적용됨: " + fn);
        } else {
            p.sendMessage("⚠ 먼저 파일을 선택하세요.");
        }
        p.closeInventory();
    }

    private ItemStack control(Material m, String title) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(title);
        it.setItemMeta(meta);
        return it;
    }

    private List<File> getSortedJsonFiles() {
        File[] arr = jsonFolder.listFiles((d, n) -> n.endsWith(".json"));
        if (arr == null) return Collections.emptyList();
        return Arrays.stream(arr).sorted().collect(Collectors.toList());
    }
}