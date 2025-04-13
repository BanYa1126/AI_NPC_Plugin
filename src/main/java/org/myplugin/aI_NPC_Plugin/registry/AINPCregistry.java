package org.myplugin.aI_NPC_Plugin.registry;

import org.myplugin.aI_NPC_Plugin.npc.AINPC;

import java.util.HashMap;
import java.util.UUID;

public class AINPCRegistry {
    private static final HashMap<UUID, AINPC> selectedNPCMap = new HashMap<>();

    public static void setSelectedNPC(UUID playerUUID, AINPC npc) {
        selectedNPCMap.put(playerUUID, npc);
    }

    public static AINPC getSelectedNPC(UUID playerUUID) {
        return selectedNPCMap.get(playerUUID);
    }
}
