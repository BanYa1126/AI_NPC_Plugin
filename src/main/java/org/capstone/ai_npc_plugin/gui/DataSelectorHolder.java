package org.capstone.ai_npc_plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class DataSelectorHolder implements InventoryHolder {
    public enum DataMode { CREATE, FIX }

    private final DataMode mode;
    private final Inventory inventory;  // 사용할 필요는 없지만 인터페이스 구현용으로 보관

    public DataSelectorHolder(DataMode mode, Inventory inventory) {
        this.mode = mode;
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public DataMode getMode() {
        return mode;
    }
}