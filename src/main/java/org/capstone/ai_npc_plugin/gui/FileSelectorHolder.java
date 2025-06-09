package org.capstone.ai_npc_plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * FileSelectorHolder
 *
 * 프롬프트 파일 선택 GUI 의 InventoryHolder 구현 클래스
 *
 * 구분 용도:
 * - PROMPT_SET 모드 : 프롬프트 적용
 * - PROMPT_FIX 모드 : 프롬프트 수정
 *
 * 주요 역할:
 * - Inventory GUI 에서 어떤 모드로 열렸는지 구분
 * - NpcFileSelector 에서 GUI 구분 시 사용
 */


public class FileSelectorHolder implements InventoryHolder {

    // 현재 Holder가 어떤 모드로 사용되는지 저장
    // NpcFileSelector.Mode enum 값을 사용
    // 예: PROMPT_SET (프롬프트 파일 선택용), PROMPT_FIX (프롬프트 파일 수정용)
    private final NpcFileSelector.Mode mode;

    // 생성자 - Holder 생성 시 모드를 전달받아 초기화
    public FileSelectorHolder(NpcFileSelector.Mode mode) {
        this.mode = mode;
    }

    // InventoryHolder 인터페이스 구현 메서드
    // 실제 Inventory는 필요 없기 때문에 null 반환
    // 이벤트 핸들러에서는 Holder 객체 자체만으로 GUI를 구분하기 때문에 Inventory는 사용되지 않음
    @Override
    public Inventory getInventory() {
        return null; // 클릭 이벤트에서 holder만 확인하면 충분합니다.
    }

    // 현재 Holder의 모드를 반환
    // 이벤트 처리 시 어떤 용도의 GUI인지 판별하는 데 사용
    public NpcFileSelector.Mode getMode() {
        return mode;
    }
}
