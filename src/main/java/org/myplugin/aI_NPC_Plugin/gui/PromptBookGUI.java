package org.myplugin.aI_NPC_Plugin.gui;

import org.myplugin.aI_NPC_Plugin.npc.AINPC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class PromptBookGUI {

    public static void open(Player player, AINPC npc) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("AI 프롬프트");
        meta.setAuthor("AI_NPC");

        String prompt = npc.getPrompt();
        List<String> pages = splitIntoPages(prompt, 255);

        for (String page : pages) {
            meta.addPage(page);
        }

        book.setItemMeta(meta);
        player.openBook(book);
    }

    private static List<String> splitIntoPages(String text, int maxLength) {
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < text.length(); i += maxLength) {
            pages.add(text.substring(i, Math.min(i + maxLength, text.length())));
        }
        return pages;
    }
}