package org.capstone.ai_npc_plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * DataSelectorHolder
 *
 * NPC 데이터 선택 GUI 의 InventoryHolder 구현 클래스
 *
 * 구분 용도:
 * - CREATE 모드 : NPC 생성 시 데이터 선택
 * - FIX 모드 : NPC 수정 시 데이터 선택
 *
 * 주요 역할:
 * - Inventory GUI 에서 어떤 모드로 열렸는지 구분
 * - NpcGUIListener 에서 GUI 구분 시 사용
 */

public class DataSelectorHolder implements InventoryHolder {

    // GUI가 어떤 용도로 열렸는지 구분하기 위한 열거형(enum)
    // CREATE: NPC 생성 시 데이터 선택용 GUI
    // FIX: 기존 NPC 수정 시 데이터 선택용 GUI
    public enum DataMode { CREATE, FIX }

    // 현재 Holder가 어떤 모드로 사용되는지 저장
    private final DataMode mode;

    // InventoryHolder 인터페이스 구현을 위해 Inventory 객체 저장
    // 실제로는 GUI 판별용으로만 사용되고, 이 inventory 필드는 내부적으로 직접 사용되지 않음
    private final Inventory inventory;

    // 생성자 - Holder 생성 시 모드와 Inventory를 전달받아 초기화
    public DataSelectorHolder(DataMode mode, Inventory inventory) {
        this.mode = mode;
        this.inventory = inventory;
    }

    // InventoryHolder 인터페이스 구현 메서드
    // Inventory 객체를 반환
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // 현재 Holder의 모드를 반환 (CREATE 또는 FIX)
    public DataMode getMode() {
        return mode;
    }
}
