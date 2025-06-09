package org.capstone.ai_npc_plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

/**
 * AINPCActionTabCompleter
 *
 * /ainpc_action 명령어의 Tab 자동완성 기능 제공
 *
 * 자동완성 지원:
 * - follow
 * - wait
 * - assist
 */


public class AINPCActionTabCompleter implements TabCompleter {

    // 명령어 자동 완성에 사용될 서브 커맨드 목록
    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "follow", "wait", "assist"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 첫 번째 인자 입력 중일 때 자동 완성 제공
        if (args.length == 1) {
            // 현재 입력한 문자열로 시작하는 커맨드만 필터링하여 반환
            return SUB_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // 첫 번째 인자 외에는 자동 완성 없음
        return List.of();
    }
}