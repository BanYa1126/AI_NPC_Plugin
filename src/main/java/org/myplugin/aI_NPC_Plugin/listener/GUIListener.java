package org.myplugin.aI_NPC_Plugin.listener;

import org.myplugin.aI_NPC_Plugin.gui.NPCSettingsGUI;
import org.myplugin.aI_NPC_Plugin.npc.AINPC;
import org.myplugin.aI_NPC_Plugin.registry.AINPCRegistry;
import net.wesjd.anvilgui.AnvilGUI; // Anvil GUI 라이브러리 필요
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§9NPC 설정")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String title = clicked.getItemMeta().getDisplayName();
        UUID uuid = player.getUniqueId();
        AINPC npc = AINPCRegistry.getSelectedNPC(uuid);
        if (npc == null) return;

        switch (title) {
            case "§a이름 설정" -> {
                new AnvilGUI.Builder()
                        .onComplete((p, text) -> {
                            npc.setName(text);
                            player.sendMessage("§aNPC 이름이 '" + text + "' 으로 변경되었습니다.");
                            return AnvilGUI.Response.close();
                        })
                        .title("NPC 이름 입력")
                        .text(npc.getName())
                        .plugin(/* your plugin instance */)
                        .open(player);
            }

            case "§e프롬프트 설정" -> {
                new AnvilGUI.Builder()
                        .onComplete((p, text) -> {
                            npc.setPrompt(text);
                            player.sendMessage("§e프롬프트가 수정되었습니다.");
                            return AnvilGUI.Response.close();
                        })
                        .title("AI 프롬프트 입력")
                        .text(npc.getPrompt())
                        .plugin(/* your plugin instance */)
                        .open(player);
            }
        }
    }
}
