package org.capstone.ai_npc_plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

/**
 * AINPCTabCompleter
 *
 * /ainpc 명령어의 Tab 자동완성 기능 제공
 *
 * 자동완성 지원:
 * - prompt_set
 * - prompt_fix
 * - create
 * - remove
 * - reset
 * - chatlog
 * - chatmode (하위 옵션: on, off)
 * - disengage
 */

public class AINPCTabCompleter implements TabCompleter {

    // /ainpc 명령어의 첫 번째 인자에 사용될 서브 커맨드 목록
    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "prompt_set", "prompt_fix", "create", "remove", "reset", "chatlog", "chatmode", "disengage"
    );

    // chatmode 명령어의 두 번째 인자 (on/off 옵션)
    private static final List<String> CHATMODE_ARGS = Arrays.asList("on", "off");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // 첫 번째 인자를 입력 중일 때 (ex: /ainpc <여기에서 tab>)
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    // 현재 입력한 문자열로 시작하는 명령어만 필터링하여 반환
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // 두 번째 인자를 입력 중일 때 (ex: /ainpc chatmode <여기에서 tab>)
        // 단, 첫 번째 인자가 chatmode 일 때만 적용
        if (args.length == 2 && args[0].equalsIgnoreCase("chatmode")) {
            return CHATMODE_ARGS.stream()
                    // 현재 입력한 문자열로 시작하는 옵션만 필터링하여 반환
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        // 그 외 경우에는 자동 완성 없음
        return List.of();
    }
}