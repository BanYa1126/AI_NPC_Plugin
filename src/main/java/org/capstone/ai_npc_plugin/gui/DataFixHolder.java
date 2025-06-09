package org.capstone.ai_npc_plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.capstone.ai_npc_plugin.manager.PromptEditorManager.DataCategory;

public class DataFixHolder implements InventoryHolder {
    private final DataCategory category;

    public DataFixHolder(DataCategory category) {
        this.category = category;
    }

    public DataCategory getCategory() {
        return category;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}