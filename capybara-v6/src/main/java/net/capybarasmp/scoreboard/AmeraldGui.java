package net.capybarasmp.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** /ameraldshop - spend Amerald on rare Skelly Spawners and the Amerald Pickaxe. */
public final class AmeraldGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SPAWNER_SLOT = 11;
    private static final int BALANCE_SLOT = 13;
    private static final int PICKAXE_SLOT = 15;

    private final CapybaraScoreboard plugin;
    private final AmeraldManager amerald;
    private final SpawnerManager spawners;
    private final DataManager data;

    public AmeraldGui(CapybaraScoreboard plugin, AmeraldManager amerald, SpawnerManager spawners, DataManager data) {
        this.plugin = plugin;
        this.amerald = amerald;
        this.spawners = spawners;
        this.data = data;
    }

    private static Component l(String s) {
        return MM.deserialize("<!italic>" + s);
    }

    private long spawnerPrice() {
        return Math.max(1L, plugin.getConfig().getLong("amerald.shop.spawner-price", 1500L));
    }

    private long spawnerCooldownMs() {
        return Math.max(0L, plugin.getConfig().getLong("amerald.shop.spawner-cooldown-hours", 24L)) * 3_600_000L;
    }

    private long pickaxePrice() {
        return Math.max(1L, plugin.getConfig().getLong("amerald.shop.pickaxe-price", 100L));
    }

    private long pickaxeHours() {
        return Math.max(1L, plugin.getConfig().getLong("amerald.shop.pickaxe-hours", 24L));
    }

    public void open(Player p) {
        AmeraldHolder h = new AmeraldHolder();
        Inventory inv = Bukkit.createInventory(h, 27, MM.deserialize("<dark_gray><c> Shop",
                Placeholder.unparsed("c", amerald.currency())));
        h.setInventory(inv);
        render(inv, p);
        p.openInventory(inv);
    }

    private void render(Inventory inv, Player p) {
        inv.clear();
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(m -> m.displayName(Component.empty()));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        String cur = amerald.currency();
        long bal = amerald.balance(p.getUniqueId());

        // Skelly Spawner (rare: expensive + limited per player)
        ItemStack sp = spawners.createItem(1);
        List<Component> spLore = new ArrayList<>();
        var existing = sp.getItemMeta().lore();
        if (existing != null) spLore.addAll(existing);
        spLore.add(Component.empty());
        spLore.add(l("<light_purple><bold>RARE"));
        spLore.add(l("<gray>Price: <#17dd62>" + MoneyUtil.format(java.math.BigInteger.valueOf(spawnerPrice())) + " " + cur));
        long cd = spawnerCooldownMs();
        PlayerData d = data.get(p.getUniqueId());
        long wait = d.lastSpawnerBuy + cd - System.currentTimeMillis();
        if (cd > 0) {
            spLore.add(l("<gray>Limit: <white>1 every " + (cd / 3_600_000L) + " hours"));
            if (wait > 0) spLore.add(l("<red>Available again in <white>" + AmeraldManager.duration(wait)));
        }
        spLore.add(Component.empty());
        spLore.add(l("<yellow>Click <gray>» Buy"));
        sp.editMeta(m -> m.lore(spLore));
        inv.setItem(SPAWNER_SLOT, sp);

        // balance
        ItemStack balItem = new ItemStack(Material.EMERALD);
        balItem.editMeta(m -> {
            m.displayName(l("<#17dd62><bold>Your " + cur));
            m.lore(List.of(l("<white>" + MoneyUtil.format(java.math.BigInteger.valueOf(bal))),
                    Component.empty(),
                    l("<gray>Earn it by typing <white>/afk <gray>and standing still.")));
        });
        inv.setItem(BALANCE_SLOT, balItem);

        // Amerald Pickaxe (temporary)
        ItemStack pick = new ItemStack(Material.NETHERITE_PICKAXE);
        pick.editMeta(m -> {
            m.displayName(l("<#17dd62><bold>AMERALD PICKAXE"));
            m.lore(List.of(
                    l("<gray>Mines <white>6 blocks <gray>at once <dark_gray>(3x2)"),
                    l("<gray>Unbreakable"),
                    Component.empty(),
                    l("<red>Disappears <white>" + pickaxeHours() + " hours <red>after you buy it"),
                    Component.empty(),
                    l("<gray>Price: <#17dd62>" + MoneyUtil.format(java.math.BigInteger.valueOf(pickaxePrice())) + " " + cur),
                    Component.empty(),
                    l("<yellow>Click <gray>» Buy")));
        });
        inv.setItem(PICKAXE_SLOT, pick);
    }

    public void handleClick(Player p, Inventory inv, int slot) {
        if (slot == SPAWNER_SLOT) {
            buySpawner(p);
            render(inv, p);
        } else if (slot == PICKAXE_SLOT) {
            buyPickaxe(p);
            render(inv, p);
        }
    }

    private void deny(Player p, String mini) {
        p.sendMessage(MM.deserialize(mini));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private boolean give(Player p, ItemStack item) {
        return p.getInventory().addItem(item).isEmpty();
    }

    private void buySpawner(Player p) {
        PlayerData d = data.get(p.getUniqueId());
        long wait = d.lastSpawnerBuy + spawnerCooldownMs() - System.currentTimeMillis();
        if (wait > 0) {
            deny(p, "<red>Skelly Spawners are rare! You can buy another in <white>" + AmeraldManager.duration(wait) + "<red>.");
            return;
        }
        long price = spawnerPrice();
        if (amerald.balance(p.getUniqueId()) < price) {
            deny(p, "<red>Not enough " + amerald.currency() + "! You need <#17dd62>" + price + "<red>.");
            return;
        }
        if (p.getInventory().firstEmpty() == -1) {
            deny(p, "<red>Your inventory is full!");
            return;
        }
        amerald.take(p.getUniqueId(), price);
        give(p, spawners.createItem(1));
        d.lastSpawnerBuy = System.currentTimeMillis();
        p.sendMessage(MM.deserialize("<green>You bought a <white>Skelly Spawner <green>for <#17dd62><n> <c><green>!",
                Placeholder.unparsed("n", String.valueOf(price)),
                Placeholder.unparsed("c", amerald.currency())));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    private void buyPickaxe(Player p) {
        long price = pickaxePrice();
        if (amerald.balance(p.getUniqueId()) < price) {
            deny(p, "<red>Not enough " + amerald.currency() + "! You need <#17dd62>" + price + "<red>.");
            return;
        }
        if (p.getInventory().firstEmpty() == -1) {
            deny(p, "<red>Your inventory is full!");
            return;
        }
        amerald.take(p.getUniqueId(), price);
        give(p, amerald.createPickaxe());
        p.sendMessage(MM.deserialize("<green>You bought the <#17dd62>Amerald Pickaxe<green>! It disappears in <white><h> hours<green>.",
                Placeholder.unparsed("h", String.valueOf(pickaxeHours()))));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }
}
