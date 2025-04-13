package org.myplugin.aI_NPC_Plugin.npc;

public class AINPC {
    private String name;
    private String prompt;

    public AINPC(String name, String prompt) {
        this.name = name;
        this.prompt = prompt;
    }

    public String getName() {
        return name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}