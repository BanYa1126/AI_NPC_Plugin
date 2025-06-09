package org.capstone.ai_npc_plugin.npc;
/**
 * AINPC
 * - AI NPC의 논리적 데이터 구조를 담당하는 클래스
 * - 현재 상호작용 중인 글로벌 NPC 객체로 사용됨 (AI_NPC_Plugin.globalNpc)
 *
 * 주요 기능:
 * - 이름 / 프롬프트 관리
 * - 대화 로그 관리
 * - 우호도 점수 / 우호도 레벨 관리
 */
public class AINPC {
    /**
     * 우호도 레벨 (AffinityLevel)
     * - HOSTILE   (적대)
     * - NEUTRAL   (중립)
     * - FRIENDLY  (우호)
     * - ALLY      (동료)
     */
    public enum AffinityLevel{
        HOSTILE, NEUTRAL, FRIENDLY, ALLY
    }
    // NPC 이름
    private String name = "기본 이름";
    // 현재 프롬프트 내용
    private String prompt = "기본 프롬프트";
    // 플레이어와의 대화 로그 (누적)
    private StringBuilder chatLog = new StringBuilder();
    // 우호도 점수 (0~100)
    private int affinityScore = 50; // 0~100
    // 우호도 레벨 (자동 계산됨)
    private AffinityLevel affinityLevel = AffinityLevel.NEUTRAL;

    // --- Getter/Setter ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    // --- 대화 로그 관리 ---

    /**
     * 대화 로그에 한 줄 추가
     * @param line 대화 내용 (ex: 플레이어 입력 or NPC 응답)
     */
    public void addToChatLog(String line) {
        chatLog.append(line).append("\n");
    }
    // 현재 대화 로그 전체 반환
    public String getChatLog() {
        return chatLog.toString();
    }
    // 대화 로그 초기화 (비우기)
    public void resetChatLog() {
        chatLog.setLength(0);
    }
    // --- 우호도 관리 ---
    //현재 우호도 점수 반환 (0~100)
    public int getAffinityScore() {return affinityScore;}
    /**
     * 우호도 점수 설정
     * - 자동으로 0~100 사이로 클램프됨
     * - 점수 변경 시 우호도 레벨도 업데이트됨
     */
    public void setAffinityScore(int score) {
        this.affinityScore = Math.max(0, Math.min(100, score));
        updateAffinityLevel();
    }
    // 현재 우호도 레벨 반환
    public AffinityLevel getAffinityLevel() {return affinityLevel;}
    // 우호도 점수에 따른 우호도 레벨 자동 계산
    private void updateAffinityLevel() {
        if (affinityScore <= 20) {
            affinityLevel = AffinityLevel.HOSTILE;
        } else if (affinityScore <= 50) {
            affinityLevel = AffinityLevel.NEUTRAL;
        } else if (affinityScore <= 80) {
            affinityLevel = AffinityLevel.FRIENDLY;
        } else {
            affinityLevel = AffinityLevel.ALLY;
        }
    }
}
