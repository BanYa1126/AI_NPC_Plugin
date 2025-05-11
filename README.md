✅ 프로젝트 개요
프로젝트명: 생성형 AI 기반 단일 NPC 플러그인
목표: 단 하나의 AI NPC가 플레이어와 1:1로 상호작용하며, 프롬프트 기반 대화를 실시간으로 생성하고 출력
사용 기술:
- Java (IntelliJ, Paper 기반 Minecraft 플러그인)
- Python (모델 서버, TCP 소켓 통신)
- GSON (JSON 처리)

✅ 전체 아키텍처
[웹] → 프롬프트 JSON 작성
    ↓ 수동 저장
[모델 서버 (Python)] ←→ (TCP 소켓, JSON) ←→ [마인크래프트 플러그인 (Java)]
                               ↕
                     플레이어 채팅 & AI 응답 출력

✅ 주요 구성 요소
모듈	              기능
웹	              프롬프트 JSON (npc.json) 생성 후 서버 디렉토리에 수동 저장
모델 서버 (Python)  npc.json 기반으로 응답 생성, TCP 통해 플러그인과 통신
플러그인 (Java)	  모델 실행/중단/리로드, 채팅 감지, 응답 출력, GUI 편집 등 담당

✅ 명령어 체계
명령어	                        기능
/model on / off / status	    모델 서버 프로세스 실행 및 상태 확인
/model reload	                저장된 프롬프트 JSON을 불러와 NPC 상태 갱신
/ainpc create	                (예정) 프롬프트 선택 GUI 실행 + NPC 초기화
/ainpc prompt_fix	            프롬프트 GUI 열기 → 수정 → JSON 저장
/ainpc remove / reset / chatlog	단일 NPC 상태 초기화 또는 로그 확인

✅ 주요 클래스 구조
org.capstone.ai_npc_plugin/
├── AI_NPC_Plugin.java               // 메인 플러그인 클래스
├── command/
│   ├── ModelCommand.java           // /model 명령어 처리
│   └── AINPCCommand.java           // /ainpc 명령어 처리
├── controller/
│   └── ModelController.java        // 모델 프로세스 관리
├── gui/
│   ├── PromptEditorManager.java    // 프롬프트 GUI 제어 (구 NpcManager)
│   └── NpcGUIListener.java         // GUI 이벤트 처리 (구 NpcGUIListener)
├── npc/
│   ├── AINPC.java                  // 단일 NPC 상태 (이름, 프롬프트, 로그)
│   └── PromptData.java             // 프롬프트 JSON 데이터 (구 NpcPromptData)
├── listener/
│   └── ChatListener.java           // 플레이어 채팅 감지 및 모델 전송
├── network/
│   └── ModelSocketClient.java      // 모델 서버와 TCP 소켓 통신

✅ 주요 흐름 정리
/ainpc prompt_fix
1. GUI 열림 → 필드 수정
2. 수정 결과 → PromptData 객체에 반영
3. npc.json으로 저장됨
4. 사용자에게 /model reload 안내 출력

/model reload
1. npc.json 로드
2. PromptData 기반으로 AINPC 상태 갱신
3. 이후 채팅 입력 시 새로운 프롬프트 기반으로 모델 대화 진행

✅ 통신 흐름
1. 웹에서 prompts.json 생성
2. 사용자가 직접 모델 폴더에 해당 JSON 파일 저장
3. /model on 명령어로 모델 실행 (ProcessBuilder)
4. 모델은 localhost:12345에서 TCP 대기
5. 플레이어 채팅 발생 시 → ChatListener 감지
6. ModelSocketClient에서 채팅 JSON 전송 → 응답 JSON 수신
7. 플러그인이 응답을 게임 내 채팅으로 출력
8. 필요 시 /model off로 종료

✅ 테스트 완료 상태
- 모델 명령어 정상 작동 (on, off, status)
- 단일 AI NPC 상태 및 설정 조작 가능
- 채팅 감지 및 모델 연결 테스트 완료
- JSON 요청 및 응답 포맷 설계 완료
- 게임 내 응답 출력 확인 완료
- /model reload → JSON 기반 반영 구조 확정

✅ 향후 진행 사항
🔲 Prompt 선택 GUI (PromptSelectGUI)	/ainpc create 후 여러 프롬프트 JSON 중 선택
🔲 NPC 시각화 연동   NPC 실체화 작업, 우선 기본 마크 주민을 사용
