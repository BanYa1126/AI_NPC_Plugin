# AI_NPC_Plugin
Minecraft에서 생성형 AI 모델을 기반으로 한 대화형 NPC를 생성하고 관리할 수 있는 플러그인입니다.  
플레이어와 NPC 간의 실시간 상호작용을 통해 몰입감 있는 게임 경험을 제공합니다.  
본 플러그인은 2025년 동의대학교 컴퓨터공학과 캡스톤디자인 과제로 개발되었습니다.

## 주요 기능
- 생성형 AI 기반 NPC 프롬프트 자동 생성 및 커스터마이징
- NPC 생성, 수정, 삭제, 대화모드 등 다양한 제어 명령어 제공
- 모델 서버와의 **지속적 TCP 소켓 통신**을 통한 빠른 대화 응답
- GUI 기반 프롬프트/배경 설정 기능
- 플레이어와 NPC 간의 우클릭 및 거리 기반 상호작용
- JSON 기반 프롬프트 데이터 관리 및 NPC 상태 저장
- 모델 서버 실행/중단/상태 확인 등의 명령어 제공

- ## 프로젝트 구조
![image](https://github.com/user-attachments/assets/14d22013-7082-409e-8077-0bacb58c389d)
![image](https://github.com/user-attachments/assets/14d22013-7082-409e-8077-0bacb58c389d)

## 생성형 AI 연동 구조
플레이어 입력 → 플러그인 내부 처리
→ PromptData 선택 → WorldPrompt 구조로 직렬화
→ TCP 소켓 통신으로 모델 서버 전달
→ 생성된 응답 수신 → NPC가 채팅 형식으로 응답 출력
- 모델 서버는 Python 기반이며, 파인튜닝된 Polyglot-Ko 기반 모델을 사용합니다.
- 응답 형식: JSON 문자열 (액션: `chat`, `prompt_load`, 등)

## 명령어
|          명령어          |          설명           |
|--------------------------|--------------------------|
| `/ainpc create`          | 새 NPC 생성 (GUI 포함)   |
| `/ainpc prompt_set`      | 프롬프트 설정 GUI 호출   |
| `/ainpc prompt_fix`      | 기존 NPC의 프롬프트 수정 |
| `/ainpc chatmode on/off` | 대화모드 활성/비활성     |
| `/ainpc disengage`       | 현재 NPC와의 대화 종료   |

## 데이터 구조 예시
json
{
  "npcs": [
    {
      "code": "N010",
      "name": "바르가",
      "job": "병사",
      "gender": "남성",
      "background_code": "B000",
      ...
    }
],
  "backgrounds": [...],
  "players": [...]
}

기술 스택
Minecraft API: Spigot 1.20
Java: 주요 플러그인 로직
Python: 모델 서버 및 프롬프트 생성기
TCP Socket: 실시간 통신 구현
JSON: 프롬프트 데이터 직렬화/역직렬화
