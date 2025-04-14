package org.myplugin.aI_NPC_Plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.myplugin.aI_NPC_Plugin.gui.PromptInventoryGUI;
import org.myplugin.aI_NPC_Plugin.npc.AINPC;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // 선택된 NPC가 있다면 (예: Map<UUID, AINPC> 캐시)
        // 여기서는 테스트용으로 임시 NPC 사용
        AINPC dummyNPC = new AINPC();
        PromptInventoryGUI.handleClick(event, dummyNPC);
    }
}
