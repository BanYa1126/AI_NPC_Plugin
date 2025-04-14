package org.myplugin.aI_NPC_Plugin.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PromptFileManager {

    private static final File PROMPT_DIR = new File("plugins/ai_npc_plugin/prompts");

    public static String loadPrompt(String fileName) {
        File file = new File(PROMPT_DIR, fileName + ".txt");
        if (!file.exists()) return "(프롬프트 없음)";
        try {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "(불러오기 실패)";
        }
    }

    public static boolean savePrompt(String fileName, String content) {
        try {
            if (!PROMPT_DIR.exists()) PROMPT_DIR.mkdirs();
            File file = new File(PROMPT_DIR, fileName + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                writer.write(content);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}