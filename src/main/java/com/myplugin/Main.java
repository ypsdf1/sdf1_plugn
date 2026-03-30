package com.myplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Set<UUID> waitingPlayers = new HashSet<>();
    private final String repoURL = "https://api.github.com/repos/ypsdf1/sdf1_plugn/releases/latest";
    private final AtomicInteger failedAttempts = new AtomicInteger(0);
    private boolean circuitBroken = false;

    @Override
    public void onEnable() {
        // 强制初始化插件文件夹为 sdf1，防止产生 myplugn 文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("sdf1") != null) {
            getCommand("sdf1").setExecutor(this);
            getCommand("sdf1").setTabCompleter(this);
        }

        // 忽略 SSL 证书校验（核心修复：解决 PKIX 报错）
        trustAllHttpsCertificates();

        Bukkit.getScheduler().runTaskLater(this, () -> triggerAutoUpdate(Bukkit.getConsoleSender()), 40L);
        getLogger().info("sdf1 插件已启动。SSL 证书兼容补丁已加载。");
    }

    // --- SSL 证书忽略逻辑 ---
    private void trustAllHttpsCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception ignored) {}
    }

    private void triggerAutoUpdate(CommandSender sender) {
        String ver = getConfig().getString("version", "1.0");
        if (!getConfig().getBoolean("check-update", true) || ver.equalsIgnoreCase("999") || ver.equalsIgnoreCase("false")) {
            return;
        }
        if (circuitBroken) return;
        checkUpdate(sender, false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        triggerAutoUpdate(Bukkit.getConsoleSender());
    }

    private String parse(String path, Map<String, String> vars) {
        String raw = getConfig().getString("messages." + path, "Missing: " + path);
        String msg = raw.replace("&", "§");
        msg = msg.replace("{board}", getConfig().getString("scoreboard-names.main", "abc"));
        msg = msg.replace("{version}", getConfig().getString("version", "1.0"));
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "NULL" : entry.getValue());
            }
        }
        return msg;
    }

    private String normalize(String s) {
        return (s == null) ? "" : s.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    private boolean isAdmin(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender || sender.isOp()) return true;
        String raw = getConfig().getString("admin-whitelist", "");
        return Arrays.asList(raw.split(",")).stream().anyMatch(n -> n.trim().equalsIgnoreCase(sender.getName()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("sdf1")) return false;

        if (args.length > 0) {
            if (!isAdmin(sender)) {
                sender.sendMessage(parse("no-permission", null));
                return true;
            }
            String sub = args[0].toLowerCase();
            if (sub.equals("reload")) {
                reloadConfig();
                sender.sendMessage("§a[sdf1] 配置重载成功！");
                return true;
            } else if (sub.equals("update")) {
                circuitBroken = false;
                failedAttempts.set(0);
                sender.sendMessage("§b[sdf1] 正在手动连接 GitHub 仓库...");
                checkUpdate(sender, true);
                return true;
            } else if (sub.equals("admin")) {
                if (args.length >= 3 && (sender instanceof ConsoleCommandSender || sender.isOp())) {
                    updateAdmin(args[1], args[2]);
                    sender.sendMessage("§a[sdf1] 管理员名单已更新。");
                }
                return true;
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(parse("only-player", null));
            return true;
        }

        Player p = (Player) sender;
        waitingPlayers.add(p.getUniqueId());
        p.sendMessage(parse("input-hint", null));
        return true;
    }

    private void updateAdmin(String action, String target) {
        String raw = getConfig().getString("admin-whitelist", "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) list.addAll(Arrays.asList(raw.split(",")));
        if (action.equalsIgnoreCase("add")) { if (!list.contains(target)) list.add(target); }
        else if (action.equalsIgnoreCase("remove")) { list.remove(target); }
        getConfig().set("admin-whitelist", String.join(",", list));
        saveConfig();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!waitingPlayers.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        String userInput = event.getMessage().trim();
        waitingPlayers.remove(player.getUniqueId());

        if (userInput.equals("0")) {
            player.sendMessage(parse("cancel-msg", null));
            return;
        }

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objM = sb.getObjective(getConfig().getString("scoreboard-names.main", "abc"));
        if (objM == null) { player.sendMessage(parse("system-issue", null)); return; }

        String finalKey = null;
        for (String entry : sb.getEntries()) {
            if (entry.equals(userInput) || normalize(entry).equals(normalize(userInput))) {
                finalKey = entry;
                break;
            }
        }

        Map<String, String> v = new HashMap<>();
        v.put("name", userInput);
        v.put("entry", finalKey);

        if (finalKey == null) {
            player.sendMessage(parse("not-found", v));
            return;
        }

        int score = objM.getScore(finalKey).getScore();
        v.put("score", String.valueOf(score));

        if (score == -1) {
            player.sendMessage(parse("used-up", v));
        } else if (score == 0) {
            Objective objR = sb.getObjective(getConfig().getString("scoreboard-names.record", "def"));
            if (objR == null) { player.sendMessage(parse("system-issue", v)); return; }
            String recName = finalKey + "_" + player.getName();
            if (objR.getScore(recName).getScore() != 0) {
                player.sendMessage(parse("already-claimed", v));
            } else {
                double amt = getConfig().getDouble("reward-rules.0", 10.0);
                v.put("amount", String.valueOf(amt));
                pay(player, amt, v);
                objR.getScore(recName).setScore(1);
            }
        } else {
            String sk = String.valueOf(score);
            if (getConfig().contains("reward-rules." + sk)) {
                double amt = getConfig().getDouble("reward-rules." + sk);
                v.put("amount", String.valueOf(amt));
                pay(player, amt, v);
                objM.getScore(finalKey).setScore(-1);
            } else {
                player.sendMessage(parse("system-issue", v));
            }
        }
    }

    private void pay(Player p, double amt, Map<String, String> v) {
        p.sendMessage(parse("success-pay", v));
        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + p.getName() + " " + amt));
    }

    private void checkUpdate(CommandSender sender, boolean manual) {
        String currentVer = getConfig().getString("version", "1.0");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(repoURL).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) sdf1-plugin");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) response.append(inputLine);
                in.close();

                String content = response.toString();
                if (!content.contains("\"tag_name\":\"")) throw new Exception("No tag found");
                String latest = content.split("\"tag_name\":\"")[1].split("\"")[0].replace("v", "");

                failedAttempts.set(0);
                Bukkit.getScheduler().runTask(this, () -> {
                    if (!latest.equals(currentVer)) {
                        Map<String, String> v = new HashMap<>();
                        v.put("latest", latest);
                        sender.sendMessage(parse("update-found", v));
                    } else if (manual) {
                        sender.sendMessage(parse("update-latest", null));
                    }
                });
            } catch (Exception e) {
                int count = failedAttempts.incrementAndGet();
                if (count >= 3 && !circuitBroken) {
                    circuitBroken = true;
                    Bukkit.getScheduler().runTask(this, () -> getLogger().warning(parse("update-circuit-break", null)));
                }
                if (manual) {
                    Bukkit.getScheduler().runTask(this, () -> sender.sendMessage("§c[sdf1] 检查更新时发生异常 (PKIX/网络): " + e.getMessage()));
                }
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1 && isAdmin(s)) return Arrays.asList("reload", "admin", "update");
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) return Arrays.asList("add", "remove");
        return null;
    }
}