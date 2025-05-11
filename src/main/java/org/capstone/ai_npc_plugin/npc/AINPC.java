package org.capstone.ai_npc_plugin.npc;

public class AINPC {
    private String name = "기본 이름";
    private String prompt = "기본 프롬프트";
    private StringBuilder chatLog = new StringBuilder();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public void addToChatLog(String line) {
        chatLog.append(line).append("\n");
    }

    public String getChatLog() {
        return chatLog.toString();
    }

    public void resetChatLog() {
        chatLog.setLength(0);
    }
}
