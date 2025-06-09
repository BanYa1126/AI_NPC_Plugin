package org.capstone.ai_npc_plugin.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * PromptData
 *
 * - 1개의 AI NPC 프롬프트 정보를 담는 데이터 구조
 * - JSON → PromptData 로 역직렬화됨
 * - PromptEditorManager에서 관리
 * - NPC GUI (생성/수정)에서 표시됨
 * - JSON 파일 저장 시 그대로 저장됨
 *
 * 필드:
 * - number      : NPC 식별 번호 (int) → GUI 표시용으로 사용
 * - name        : 이름
 * - age         : 나이
 * - gender      : 성별
 * - job         : 직업
 * - personality : 성격 (여러 개 가능 → List<String>)
 * - background  : 배경 설명 (스토리)
 */

public class PromptData {
    // 고유 번호 (GUI에서 NPC 선택 시 사용됨)
    public int number;
    // NPC 이름
    public String name = "";
    // 나이 (문자열로 저장)
    public String age = "";
    // 성별
    public String gender = "";
    // 직업
    public String job = "";
    // 성격 (List<String> 형식 → personality: ["친절함", "용감함", ...])
    public List<String> personality = new ArrayList<>();
    // 배경 설명 (스토리, 세계관 설정 등)
    public String background = "";
}