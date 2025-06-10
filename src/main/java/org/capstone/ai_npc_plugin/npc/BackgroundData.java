package org.capstone.ai_npc_plugin.npc;

import java.util.List;

public class BackgroundData {
    public String code;
    public String type;
    public String era;
    public String description;
    public List<CityData> cities;

    public static class CityData {
        public String name;
        public String type;
        public List<String> traits;
        public String description;
    }
}
