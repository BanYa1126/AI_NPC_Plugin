package org.capstone.ai_npc_plugin.controller;

import java.io.File;
import java.io.IOException;

/**
 * ModelController
 *
 * 외부 모델 서버 (ex: Python 기반 model_server.py) 를 관리하는 컨트롤러 클래스
 *
 * 주요 기능:
 * - startModel() : 모델 서버 실행 (새 프로세스 실행)
 * - stopModel() : 모델 서버 프로세스 종료
 * - isModelRunning() : 모델 서버 프로세스 실행 여부 확인
 *
 * 프로세스 실행 방식:
 * - model_server.py 를 현재 작업 디렉토리에서 실행
 * - Java 프로세스에서 별도 Process 로 관리
 *
 * 사용 위치:
 * - /model on, off, status, reload 명령어 처리 시 사용됨 (ModelCommand 에서 호출)
 */

public class ModelController {

    // 모델 서버 프로세스를 나타내는 변수 (전역 static 변수로 사용)
    private static Process modelProcess;

    // 모델 서버 실행 메서드
    public void startModel() {
        // 이미 실행 중인지 확인
        if (modelProcess != null && modelProcess.isAlive()) {
            System.out.println("[AI_NPC_Plugin] 모델 서버는 이미 실행 중입니다.");
            return;
        }

        try {
            // 프로세스 빌더 생성
            // 명령어: python model/model_server.py 실행
            ProcessBuilder builder = new ProcessBuilder("python", "model/model_server.py");

            // 현재 작업 디렉토리를 서버의 user.dir 로 설정
            builder.directory(new File(System.getProperty("user.dir")));

            // 표준 오류 출력을 표준 출력으로 리디렉트 (에러 메시지를 콘솔에 출력하도록 설정)
            builder.redirectErrorStream(true);

            // 프로세스 시작 후 modelProcess 변수에 저장
            modelProcess = builder.start();

            System.out.println("[AI_NPC_Plugin] 모델 서버 실행됨");
        } catch (IOException e) {
            // 실행 중 오류 발생 시 스택 트레이스 출력
            e.printStackTrace();
        }
    }

    // 모델 서버 종료 메서드 (정적 메서드로 사용 가능)
    public static void stopModel() {
        // 프로세스가 존재하고 실행 중일 경우 종료
        if (modelProcess != null && modelProcess.isAlive()) {
            modelProcess.destroy();
            System.out.println("[AI_NPC_Plugin] 모델 서버 종료됨");
        }
    }

    // 모델 서버 실행 상태 확인 메서드 (정적 메서드)
    public static boolean isModelRunning() {
        // modelProcess 가 null 이 아니고 실행 중인지 여부 반환
        return modelProcess != null && modelProcess.isAlive();
    }
}