
package tw.moses.sleepvote;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SleepVote extends JavaPlugin
        implements CommandExecutor, Listener {

    private static final String BOT_UUID_PREFIX = "fb07";
    private static final long VOTE_SECONDS = 30L;

    private final Set<UUID> eligiblePlayers = new HashSet<>();
    private final Map<UUID, Boolean> votes = new HashMap<>();

    private UUID initiator;
    private BukkitTask timeoutTask;
    private boolean active = false;

    @Override
    public void onEnable() {
        if (getCommand("snvote") == null) {
            getLogger().severe(
                    "Command snvote is missing from plugin.yml!"
            );
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("snvote").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("SkipSleepVote enabled.");
    }

    @Override
    public void onDisable() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }

    private boolean isBot(Player player) {
        return player.getUniqueId()
                .toString()
                .toLowerCase()
                .startsWith(BOT_UUID_PREFIX);
    }

    private int yesCount() {
        return (int) votes.values().stream()
                .filter(Boolean::booleanValue)
                .count();
    }

    private int noCount() {
        return (int) votes.values().stream()
                .filter(v -> !v)
                .count();
    }

    // 嚴格過半，例如 5 名真人至少需要 3 票
    private int requiredVotes() {
        return eligiblePlayers.size() / 2 + 1;
    }

    private void startVote(Player player) {
        if (isBot(player)) {
            player.sendMessage(Component.text(
                    "機器人不能發起投票。",
                    NamedTextColor.RED
            ));
            return;
        }

        if (active) {
            player.sendMessage(Component.text(
                    "目前已有一場跳夜投票。",
                    NamedTextColor.RED
            ));
            return;
        }

        eligiblePlayers.clear();
        votes.clear();

        // 固定投票開始時的真人玩家名單
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!isBot(online)) {
                eligiblePlayers.add(online.getUniqueId());
            }
        }

        if (eligiblePlayers.isEmpty()) {
            player.sendMessage(Component.text(
                    "沒有符合資格的真人玩家。",
                    NamedTextColor.RED
            ));
            return;
        }

        active = true;
        initiator = player.getUniqueId();

        // 發起公告只執行一次
        broadcastVoteStart();

        timeoutTask = Bukkit.getScheduler().runTaskLater(
                this,
                this::finishVote,
                VOTE_SECONDS * 20L
        );
    }

    
private void castVote(Player player, boolean agree) {
    if (!active) {
        player.sendMessage(Component.text(
                "目前沒有進行中的跳夜投票。",
                NamedTextColor.RED
        ));
        return;
    }

    UUID uuid = player.getUniqueId();

    if (isBot(player)) {
        player.sendMessage(Component.text(
                "機器人不能參與投票。",
                NamedTextColor.RED
        ));
        return;
    }

    if (!eligiblePlayers.contains(uuid)) {
        player.sendMessage(Component.text(
                "你不在本次投票的合格玩家名單中。",
                NamedTextColor.RED
        ));
        return;
    }

    // 已經投過票就拒絕，不更新任何人的投票訊息
    if (votes.containsKey(uuid)) {
        player.sendMessage(Component.text(
                "你已經參與投票！",
                NamedTextColor.RED
        ));
        return;
    }

    votes.put(uuid, agree);

    // 只有首次投票成功才更新全體玩家看到的票數
    broadcastVoteStatus();

    player.sendMessage(Component.text(
            agree ? "你已投下同意票。" : "你已投下不同意票。",
            NamedTextColor.GREEN
    ));
}


    private void broadcastVoteStart() {
        Player starter = Bukkit.getPlayer(initiator);

        String starterName = starter != null
                ? starter.getName()
                : Bukkit.getOfflinePlayer(initiator).getName();

        if (starterName == null) {
            starterName = "未知玩家";
        }

        Component header = Component.text(
                "玩家 ",
                NamedTextColor.YELLOW
        ).append(Component.text(
                starterName,
                NamedTextColor.GOLD
        )).append(Component.text(
                " 發起了跳過夜晚投票",
                NamedTextColor.YELLOW
        ));

        // 這一行只在投票開始時廣播
        Bukkit.broadcast(header);

        // 初次顯示票數和按鈕
        broadcastVoteStatus();
    }

    private void broadcastVoteStatus() {
        if (!active) {
            return;
        }

        int yes = yesCount();
        int total = eligiblePlayers.size();

        NamedTextColor countColor = yes >= requiredVotes()
                ? NamedTextColor.GREEN
                : NamedTextColor.RED;

        Component count = Component.text(
                "同意人數：" + yes + "/" + total
                        + "",
                countColor
        );

        Component agree = Component.text(
                "[同意]",
                NamedTextColor.GREEN
        ).clickEvent(ClickEvent.runCommand("/snvote yes"))
         .hoverEvent(HoverEvent.showText(
                Component.text("點擊投下同意票")
         ));

        Component disagree = Component.text(
                "[不同意]",
                NamedTextColor.RED
        ).clickEvent(ClickEvent.runCommand("/snvote no"))
         .hoverEvent(HoverEvent.showText(
                Component.text("點擊投下反對票")
         ));

        Component buttons = agree
                .append(Component.text("                  "))
                .append(disagree);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(count);
            online.sendMessage(buttons);
        }
    }

    private void finishVote() {
        if (!active) {
            return;
        }

        int yes = yesCount();
        int no = noCount();
        int total = eligiblePlayers.size();
        int required = requiredVotes();

        active = false;

        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }

        if (yes >= required) {
            for (World world : Bukkit.getWorlds()) {
                world.setTime(0L);
                world.setStorm(false);
                world.setThundering(false);
            }

            Bukkit.broadcast(Component.text(
                    "跳夜投票通過！已將所有世界時間設為白天。",
                    NamedTextColor.GREEN
            ));
        } else {
            Bukkit.broadcast(Component.text(
                    "跳夜投票未通過。贊成：" + yes
                            + "，反對：" + no
                            + "，真人玩家：" + total
                            + "，需要贊成：" + required + " 票。",
                    NamedTextColor.RED
            ));
        }

        eligiblePlayers.clear();
        votes.clear();
        initiator = null;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令只能由玩家執行。");
            return true;
        }

        if (args.length == 0) {
            startVote(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "yes" -> castVote(player, true);

            case "no" -> castVote(player, false);

            case "status" -> {
                if (!active) {
                    player.sendMessage(Component.text(
                            "目前沒有進行中的投票。",
                            NamedTextColor.RED
                    ));
                } else {
                    player.sendMessage(Component.text(
                            "贊成：" + yesCount()
                                    + "/" + eligiblePlayers.size()
                                    + "，反對：" + noCount()
                                    + "，通過需要："
                                    + requiredVotes() + " 票。",
                            NamedTextColor.YELLOW
                    ));
                }
            }

            default -> player.sendMessage(Component.text(
                    "用法：/snvote [yes|no|status]",
                    NamedTextColor.YELLOW
            ));
        }

        return true;
    }
}
