package net.capybarasmp.scoreboard;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /afk, /ameraldshop, /scoreboard, /amerald */
public final class AmeraldCommands implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final AmeraldManager amerald;
    private final AmeraldGui shopGui;
    private final ScoreboardGui boardGui;

    public AmeraldCommands(AmeraldManager amerald, AmeraldGui shopGui, ScoreboardGui boardGui) {
        this.amerald = amerald;
        this.shopGui = shopGui;
        this.boardGui = boardGui;
    }

    private void send(CommandSender s, String mini) {
        s.sendMessage(MM.deserialize(mini));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "afk" -> {
                if (sender instanceof Player p) amerald.toggleAfk(p);
                else send(sender, "<red>Players only.");
            }
            case "ameraldshop" -> {
                if (sender instanceof Player p) shopGui.open(p);
                else send(sender, "<red>Players only.");
            }
            case "scoreboard" -> {
                if (sender instanceof Player p) boardGui.open(p);
                else send(sender, "<red>Players only.");
            }
            case "amerald" -> amerald(sender, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void amerald(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                send(sender, "<red>Usage: /amerald give <player> <amount>");
                return;
            }
            send(sender, "<#17dd62><c>: <white><n>".replace("<c>", amerald.currency())
                    .replace("<n>", MoneyUtil.format(java.math.BigInteger.valueOf(amerald.balance(p.getUniqueId())))));
            return;
        }
        if (!CapybaraScoreboard.isAdmin(sender)) {
            send(sender, "<red>You don't have permission.");
            return;
        }
        if (args.length < 3 || !(args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            send(sender, "<red>Usage: /amerald <give|take> <player> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            send(sender, "<red>Unknown player (they must have joined before).");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            send(sender, "<red>Invalid amount.");
            return;
        }
        if (amount <= 0) {
            send(sender, "<red>Amount must be positive.");
            return;
        }
        boolean give = args[0].equalsIgnoreCase("give");
        if (give) amerald.add(target.getUniqueId(), amount);
        else amerald.take(target.getUniqueId(), amount);
        sender.sendMessage(MM.deserialize("<green>Done. <white><p> <green>now has <white><n> <c><green>.",
                Placeholder.unparsed("p", String.valueOf(target.getName())),
                Placeholder.unparsed("n", String.valueOf(amerald.balance(target.getUniqueId()))),
                Placeholder.unparsed("c", amerald.currency())));
    }
}
