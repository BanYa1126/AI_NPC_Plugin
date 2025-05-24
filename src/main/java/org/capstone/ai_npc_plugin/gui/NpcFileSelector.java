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
import org.capstone.ai_npc_plugin.npc.PromptData;
import org.capstone.ai_npc_plugin.gui.NpcGUIListener;
import org.capstone.ai_npc_plugin.gui.FileSelectorHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NpcFileSelector implements Listener {
    public enum Mode { PROMPT_SET, PROMPT_FIX }
    private final PromptEditorManager manager;
    private final Plugin plugin;
    private final File jsonFolder;
    private final NpcGUIListener fixListener;

    private static final int GUI_SIZE = 54;
    private static final int FILES_PER_PAGE = 45;

    private final Map<UUID,String> selectedForSet = new HashMap<>();
    private final Map<UUID,String> selectedForFix = new HashMap<>();
    private final Map<UUID, Integer> playerScroll   = new HashMap<>();
    private final Map<UUID, Villager>playerNpc      = new HashMap<>();

    public NpcFileSelector(Plugin plugin,
                           File jsonFolder,
                           PromptEditorManager manager,
                           NpcGUIListener fixListener) {
        this.plugin      = plugin;
        this.jsonFolder  = jsonFolder;
        this.manager     = manager;
        this.fixListener = fixListener;
        if (!jsonFolder.exists()) jsonFolder.mkdirs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** mode 에 따라 title 과 apply 버튼을 다르게 띄워 줍니다 */
    public void openGUI(Player player, Villager npc, Mode mode) {
        UUID id = player.getUniqueId();
        playerScroll.remove(id);
        playerNpc.put(id, npc);

        FileSelectorHolder holder = new FileSelectorHolder(mode);

        List<File> files = getSortedJsonFiles();
        int idx = playerScroll.getOrDefault(id, 0);
        int end = Math.min(idx + FILES_PER_PAGE, files.size());

        String title = (mode == Mode.PROMPT_SET
                ? "📁 Prompt Set 선택" : "📁 Prompt Fix 선택"
        );
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);

        String already = (mode == Mode.PROMPT_SET
                ? selectedForSet.get(id)
                : selectedForFix.get(id)
        );

        for (int i = idx; i < end; i++) {
            File f = files.get(i);
            String jsonName = "";
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8)) {

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

            ItemStack it = new ItemStack(Material.PAPER);
            ItemMeta m = it.getItemMeta();
            boolean isSel = f.getName().equals(already);

            m.setDisplayName(
                    isSel
                            ? ChatColor.YELLOW + "✔ " + f.getName()
                            : ChatColor.WHITE  + f.getName()
            );
            m.setLore(Collections.singletonList(ChatColor.GRAY + parseJsonNames(f)));
            m.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "filename"),
                            PersistentDataType.STRING,
                            f.getName());
            it.setItemMeta(m);

            gui.setItem(i - idx, it);
        }

        // 페이징
        if (idx > 0)            gui.setItem(49, control(Material.LEVER, "이전 페이지"));
        if (end < files.size()) gui.setItem(50, control(Material.LEVER, "다음 페이지"));

        // apply/cancel 버튼
        String applyText = mode == Mode.PROMPT_SET ? "✔ 적용" : "✔ 선택";
        gui.setItem(52, control(Material.LIME_CONCRETE, applyText));
        gui.setItem(53, control(Material.RED_CONCRETE, "✘ 취소"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getInventory().getHolder() instanceof FileSelectorHolder holder)) return;
        e.setCancelled(true);

        UUID id = p.getUniqueId();
        Mode mode = holder.getMode();

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String label = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        String fn    = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "filename"),
                        PersistentDataType.STRING);

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
                // (C) 모드별 맵에서 꺼내 적용
                String sel = (mode == Mode.PROMPT_SET
                        ? selectedForSet.get(id)
                        : selectedForFix.get(id)
                );
                if (sel == null) {
                    p.sendMessage(ChatColor.RED + "먼저 파일을 선택하세요.");
                    return;
                }

                if (!manager.loadPromptFile(sel)) {
                    p.sendMessage(ChatColor.RED + "파일 로드에 실패했습니다: " + sel);
                    p.closeInventory();
                    return;
                }
                if (mode == Mode.PROMPT_SET) {
                    p.sendMessage(ChatColor.GREEN + "프롬프트 파일 적용 완료: " + sel);
                    p.closeInventory();
                } else {
                    p.closeInventory();
                    manager.openNpcEditGUI(p);
                }
            }

            default -> {
                // (D) 파일 클릭 시 모드별로 선택 저장 후 리프레시
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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().startsWith("📁")) {
            e.setCancelled(true);
        }
    }

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

    private ItemStack control(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }

    private List<File> getSortedJsonFiles() {
        File[] arr = jsonFolder.listFiles((d,n)->n.toLowerCase().endsWith(".json"));
        return arr==null
                ? Collections.emptyList()
                : Arrays.stream(arr).sorted().toList();
    }
}