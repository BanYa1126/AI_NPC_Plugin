package org.capstone.ai_npc_plugin.controller;

import java.io.File;
import java.io.IOException;

public class ModelController {

    private static Process modelProcess;

    public void startModel() {
        if (modelProcess != null && modelProcess.isAlive()) {
            System.out.println("[AI_NPC_Plugin] 모델 서버는 이미 실행 중입니다.");
            return;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder("python", "model/model_server.py");
            builder.directory(new File(System.getProperty("user.dir")));
            builder.redirectErrorStream(true);
            modelProcess = builder.start();

            System.out.println("[AI_NPC_Plugin] 모델 서버 실행됨");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stopModel() {
        if (modelProcess != null && modelProcess.isAlive()) {
            modelProcess.destroy();
            System.out.println("[AI_NPC_Plugin] 모델 서버 종료됨");
        }
    }

    public static boolean isModelRunning() {
        return modelProcess != null && modelProcess.isAlive();
    }
}
