package org.capstone.ai_npc_plugin.npc;

/**
 * PromptData 클래스 (최신 프롬프트 양식 대응)
 *
 * - 기존 구조를 최신 JSON 프롬프트 구조 (npc 객체) 에 맞추어 재구성
 * - 해당 클래스는 npc 1개 정보를 나타냄
 *
 * JSON 예시:
 * {
 *   "code": "N001",
 *   "type": "npc",
 *   "name": "아룬",
 *   "era": "근대",
 *   "job": "상인",
 *   "social_status": "중산층",
 *   "gender": "남성",
 *   "relation": "동맹",
 *   "city": "하이랄",
 *   "description": "하이랄 도시의 성실한 상인",
 *   "background_code": "B000"
 * }
 */

public class PromptData {
    public String code;
    public String type;
    public String name;
    public String era;
    public String job;
    public String social_status;
    public String gender;
    public String relation;
    public String city;
    public String description;
    public String background_code;
}