package org.capstone.ai_npc_plugin.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class NpcFileSelector implements Listener {

    private final Plugin plugin;
    private final File jsonFolder;
    private static final int GUI_SIZE = 54;
    private static final int FILES_PER_PAGE = 45;

    private final Map<UUID, Integer> playerScroll   = new HashMap<>();
    private final Map<UUID, String>  playerSelected = new HashMap<>();
    private final Map<UUID, Villager> playerNpc     = new HashMap<>();

    public NpcFileSelector(Plugin plugin, File jsonFolder) {
        this.plugin = plugin;
        this.jsonFolder = jsonFolder;
        if (!jsonFolder.exists()) jsonFolder.mkdirs();
        // 이벤트 리스너 등록
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player, Villager npc) {
        List<File> files = getSortedJsonFiles();
        int idx = playerScroll.getOrDefault(player.getUniqueId(), 0);
        int end = Math.min(idx + FILES_PER_PAGE, files.size());

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "📁 NPC 프롬프트 선택");

        for (int i = idx; i < end; i++) {
            File f = files.get(i);
            String jsonName = "";
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                if (obj.has("name")) jsonName = obj.get("name").getAsString();
            } catch (IOException e) {
                plugin.getLogger().warning("프롬프트 파싱 실패: " + f.getName());
            }

            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();

            // 파일명 displayName
            m.setDisplayName(ChatColor.WHITE + f.getName());
            // lore: JSON 내부 name
            m.setLore(Collections.singletonList(ChatColor.GRAY + jsonName));

            // 선택 강조
            String sel = playerSelected.get(player.getUniqueId());
            if (f.getName().equals(sel)) {
                m.setDisplayName(ChatColor.YELLOW + "✔ " + f.getName());
            }

            m.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "filename"), PersistentDataType.STRING, f.getName());
            it.setItemMeta(m);

            int slot = i - idx;
            if (slot >= FILES_PER_PAGE) break;
            gui.setItem(slot, it);
        }

        // 이전/다음 버튼 중앙 하단
        if (idx > 0)            gui.setItem(49, control(Material.LEVER, "이전"));
        if (end < files.size()) gui.setItem(50, control(Material.LEVER, "다음"));

        // 적용/취소 버튼 우측 하단
        gui.setItem(52, control(Material.LIME_CONCRETE, "✔ 적용"));
        gui.setItem(53, control(Material.RED_CONCRETE, "✘ 취소"));

        playerNpc.put(player.getUniqueId(), npc);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getCurrentItem() == null) return;
        if (!e.getView().getTitle().equals("📁 NPC 프롬프트 선택")) return;
        e.setCancelled(true);

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null) return;
        String label = ChatColor.stripColor(meta.getDisplayName());
        String fn = meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "filename"), PersistentDataType.STRING);

        switch (label) {
            case "이전" -> scroll(p, -FILES_PER_PAGE);
            case "다음" -> scroll(p, +FILES_PER_PAGE);
            case "✔ 적용" -> apply(p);
            case "✘ 취소" -> p.closeInventory();
            default -> {
                if (fn != null) {
                    playerSelected.put(p.getUniqueId(), fn);
                    p.sendMessage(ChatColor.GOLD + "📌 선택됨: " + ChatColor.WHITE + fn);
                    openGUI(p, playerNpc.get(p.getUniqueId()));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!"📁 NPC 프롬프트 선택".equals(e.getView().getTitle())) return;
        e.setCancelled(true);
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
            p.sendMessage(ChatColor.GREEN + "✅ 적용됨: " + fn);
        } else {
            p.sendMessage(ChatColor.RED + "⚠ 먼저 파일을 선택하세요.");
        }
        p.closeInventory();
    }

    private ItemStack control(Material mat, String title) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(title);
        it.setItemMeta(m);
        return it;
    }

    private List<File> getSortedJsonFiles() {
        File[] arr = jsonFolder.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (arr == null) return Collections.emptyList();
        return Arrays.stream(arr).sorted().collect(Collectors.toList());
    }
}