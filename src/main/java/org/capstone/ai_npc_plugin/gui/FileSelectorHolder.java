package org.capstone.ai_npc_plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class FileSelectorHolder implements InventoryHolder {
    private final NpcFileSelector.Mode mode;

    public FileSelectorHolder(NpcFileSelector.Mode mode) {
        this.mode = mode;
    }

    @Override
    public Inventory getInventory() {
        return null;  // 클릭 이벤트에서 holder만 확인하면 충분합니다.
    }

    public NpcFileSelector.Mode getMode() {
        return mode;
    }
}