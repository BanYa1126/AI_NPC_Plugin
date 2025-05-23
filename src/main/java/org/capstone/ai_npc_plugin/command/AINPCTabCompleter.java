package org.capstone.ai_npc_plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

public class AINPCTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "prompt_set", "prompt_fix", "create", "remove", "reset", "chatlog", "chatmode", "disengage"
    );

    private static final List<String> CHATMODE_ARGS = Arrays.asList("on", "off");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 1단계: /ainpc <subcommand>
            return SUB_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("chatmode")) {
            // 2단계: /ainpc chatmode <on|off>
            return CHATMODE_ARGS.stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
