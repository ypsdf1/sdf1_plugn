package com.sdf1;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private String cfgCdkObj = "";
    private String cfgCyName = "";
    private int cfgVer = 0;
    private boolean configLoaded = false;
    private boolean cdksWarned = false;
    private String cfgLinkMode = "";
    private String cfgUpdateChannel = "";
    private final Map<Integer, ScoreAction> scoreMap = new HashMap<Integer, ScoreAction>();
    private Plugin cyPlugin = null;
    private java.lang.reflect.Method cyGiveSlots = null;
    private Economy economy = null;
    private static final long LISTEN_TIMEOUT_MS = 15000L;
    private final Map<UUID, Long> listening = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> chatCd = new ConcurrentHashMap<UUID, Long>();
    private int failCount = 0;
    private final Random rng = new Random();
    private boolean circuitBroken = false;
    private String remoteVer = "";
    private static final String API_GH = "https://api.github.com/repos/ypsdf1/sdf1_plugn/releases/latest";
    private static final String API_GE = "https://gitee.com/api/v5/repos/nihaoshidifu/sdf1_plugn/releases/latest";
    private static final String DL_GH = "https://github.com/ypsdf1/sdf1_plugn/releases";
    private static final String DL_GE = "https://gitee.com/nihaoshidifu/sdf1_plugn/releases";

    private static final Map<String, String> ALIASES = new HashMap<String, String>();
    static {
        String[][] pairs = {
                {"计分板","计分板"},{"口令板","计分板"},{"口令库","计分板"},{"查重板","计分板"},{"scoreboard","计分板"},{"cdk板","计分板"},{"cdk","计分板"},
                {"联控插件","联控插件"},{"联控","联控插件"},{"cy插件","联控插件"},{"cy","联控插件"},{"cy_beibao","联控插件"},
                {"分值","分值"},{"分数","分值"},{"score","分值"},{"points","分值"},
                {"动作","动作"},{"action","动作"},{"操作","动作"},
                {"类型","类型"},{"type","类型"},
                {"格子数","格子数"},{"格子","格子数"},{"slots","格子数"},{"空间","格子数"},{"格数","格子数"},
                {"天数","天数"},{"天","天数"},{"days","天数"},
                {"金额","金额"},{"金","金额"},{"钱","金额"},{"money","金额"},
                {"版本号","版本号"},{"版本","版本号"},{"version","版本号"},{"ver","版本号"},
                {"更新通道","更新通道"},{"联控模式","联控模式"}
        };
        for (String[] p : pairs) ALIASES.put(p[0].toLowerCase(), p[1]);
    }

    private static final Map<String, Integer> CN_NUMS = new HashMap<String, Integer>();
    static {
        CN_NUMS.put("零", 0); CN_NUMS.put("一", 1); CN_NUMS.put("二", 2); CN_NUMS.put("三", 3);
        CN_NUMS.put("四", 4); CN_NUMS.put("五", 5); CN_NUMS.put("六", 6); CN_NUMS.put("七", 7);
        CN_NUMS.put("八", 8); CN_NUMS.put("九", 9); CN_NUMS.put("十", 10); CN_NUMS.put("百", 100);
        CN_NUMS.put("千", 1000); CN_NUMS.put("万", 10000);
        CN_NUMS.put("壹", 1); CN_NUMS.put("贰", 2); CN_NUMS.put("叁", 3); CN_NUMS.put("肆", 4);
        CN_NUMS.put("伍", 5); CN_NUMS.put("陆", 6); CN_NUMS.put("柒", 7); CN_NUMS.put("捌", 8);
        CN_NUMS.put("玖", 9); CN_NUMS.put("拾", 10); CN_NUMS.put("佰", 100); CN_NUMS.put("仟", 1000);
    }

    private static class ActionEntry {
        String type;
        double money, moneyMin, moneyMax;
        int slots, days, slotsMin, slotsMax, daysMin, daysMax;
        int decimalPlaces = -1;
        String rawText = "";

        ActionEntry(String t) { type = t; }
    }
    private static class ScoreAction {
        List<ActionEntry> actions = new ArrayList<ActionEntry>();
        boolean oneTime = false;
    }

    @Override
    public void onEnable() {
        log("[SDF1] ===== 启动 =====");
        extractDefaultConfig();
        loadConfig(true);
        setupEconomy();
        setupCyReflection();
        getCommand("sdf1").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        checkUpdate(null);
        log("[SDF1] ===== 启动完成 =====");
    }
    @Override
    public void onDisable() { log("[SDF1] 卸载"); listening.clear(); }
    private void log(String msg) { getLogger().info(msg); }

    // PLACEHOLDER_PART2
    // ===== 共享算法：主控/被控互相发现 =====
    private static final String[] DISCOVER_NAMES = {"Sdf1", "sdf1", "SDF1", "SDF1Plugin"};
    private static final String[] CY_DISCOVER_NAMES = {"CY_beibao", "CY", "cy_beibao", "Cy", "cy"};
    private Plugin discoveredCy = null;
    private java.lang.reflect.Method discoveredCyActivate = null;
    private java.lang.reflect.Method discoveredCyPing = null;
    private long lastCyDiscover = 0;
    private static final long DISCOVER_INTERVAL = 30000L;

    private void discoverCyPlugin() {
        long now = System.currentTimeMillis();
        if (now - lastCyDiscover < DISCOVER_INTERVAL) return;
        lastCyDiscover = now;
        if (discoveredCy != null && discoveredCy.isEnabled()) return;
        discoveredCy = null; discoveredCyActivate = null; discoveredCyPing = null;
        for (String name : CY_DISCOVER_NAMES) {
            Plugin p = Bukkit.getPluginManager().getPlugin(name);
            if (p != null && p.isEnabled()) {
                discoveredCy = p;
                try { discoveredCyActivate = p.getClass().getMethod("onSdf1Activation", String.class, int.class, int.class); } catch (Exception ignored) {}
                try { discoveredCyPing = p.getClass().getMethod("onSdf1Ping"); } catch (Exception ignored) {}
                log("[共享] 发现被控: " + p.getName());
                return;
            }
        }
        log("[共享] 未发现被控插件");
    }
    private boolean isCyConnected() {
        return discoveredCy != null && discoveredCy.isEnabled() && discoveredCyActivate != null;
    }
    private String cyPing() {
        if (!isCyConnected()) { discoverCyPlugin(); if (!isCyConnected()) return "未连接"; }
        if (discoveredCyPing != null) {
            try { Object result = discoveredCyPing.invoke(discoveredCy); return result != null ? result.toString() : "OK"; }
            catch (Exception e) { return "ping失败"; }
        }
        return "OK(无ping方法)";
    }

    // ===== 配置文件 =====
    private void extractDefaultConfig() {
        File folder = getDataFolder();
        File f = new File(folder, "设置.txt");
        log("[SDF1] 目标: " + f.getAbsolutePath() + " exists=" + f.exists());
        if (f.exists() && f.length() > 0) { log("[SDF1] 已存在，跳过"); return; }
        try {
            InputStream in = getResource("设置.txt");
            if (in != null) {
                if (!folder.exists()) folder.mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                byte[] buf = new byte[1024]; int len;
                while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
                fos.close(); in.close();
                log("[SDF1] jar提取完成 " + f.length() + "字节"); return;
            }
        } catch (Exception e) { log("[SDF1] jar提取异常: " + e.getMessage()); }
        writeFallbackConfig(f);
    }
    private void writeFallbackConfig(File f) {
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
            pw.println("# SDF1 主控配置"); pw.println("版本号: 1"); pw.println("更新通道: GH");
            pw.println("计分板: "); pw.println("联控插件: "); pw.println("联控模式: 开");
            pw.println(); pw.println("1:"); pw.println("  - 给那个玩家100块");
            pw.println(); pw.println("2:"); pw.println("  - 给那个玩家抽个盲盒"); pw.println("  - 然后口令删了");
            pw.flush(); pw.close();
            log("[SDF1] 生成默认配置 " + f.length() + "字节");
        } catch (Exception e) { log("[SDF1] 写入失败: " + e.getMessage()); }
    }
    private void loadConfig(boolean verbose) {
        scoreMap.clear(); configLoaded = false; cdksWarned = false;
        cfgCdkObj = ""; cfgCyName = ""; cfgVer = 0; cfgLinkMode = ""; cfgUpdateChannel = "";
        File f = new File(getDataFolder(), "设置.txt");
        if (!f.exists() || f.length() == 0) extractDefaultConfig();
        try {
            List<String> rawLines = new ArrayList<String>();
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
            String line; while ((line = r.readLine()) != null) rawLines.add(line); r.close();
            if (verbose) { log("===== 配置原文 ====="); for (int i = 0; i < rawLines.size(); i++) log(String.format("L%02d: %s", i + 1, rawLines.get(i))); }
            List<String> diag = new ArrayList<String>();
            List<String> cleanLines = stripAllComments(rawLines, diag);
            if (verbose) { log("===== 逐行解析 ====="); for (String d : diag) log(d); }
            deepParse(cleanLines, diag);
            if (verbose) {
                log("===== 最终配置 =====");
                log("版本号: " + cfgVer); log("计分板: " + (cfgCdkObj.isEmpty() ? "(未设置)" : cfgCdkObj));
                log("联控插件: " + (cfgCyName.isEmpty() ? "(未设置)" : cfgCyName));
                log("联控模式: " + (cfgLinkMode.isEmpty() ? "(未设置)" : cfgLinkMode));
                log("规则数: " + scoreMap.size());
                for (Map.Entry<Integer, ScoreAction> entry : scoreMap.entrySet()) {
                    ScoreAction sa = entry.getValue();
                    log("  分值" + entry.getKey() + " -> " + sa.actions.size() + "个动作" + (sa.oneTime ? " (一次性)" : ""));
                    for (int i = 0; i < sa.actions.size(); i++) {
                        ActionEntry ae = sa.actions.get(i);
                        log("    动作" + (i + 1) + ": [" + ae.type + "] " + ae.rawText);
                    }
                }
            }
        } catch (Exception e) { log("[SDF1] 读取失败: " + e.getMessage()); }
    }

    // PLACEHOLDER_PART3
    private void deepParse(List<String> cleanLines, List<String> diag) {
        Map<String, String> globals = new HashMap<String, String>();
        List<Map<String, String>> blocks = new ArrayList<Map<String, String>>();
        Map<String, String> cur = null;
        for (int i = 0; i < cleanLines.size(); i++) {
            String line = cleanLines.get(i);
            if (line.isEmpty()) continue;
            String bare = line.replaceAll("[\\[\\]{}()（）【】]", "").trim();
            if (bare.matches("^\\d+:\\s*$")) {
                String blockId = bare.replaceAll(":", "").trim();
                log("[解析] 新建块: 分值=" + blockId);
                diag.add("  -> [规则块] 分值 = " + blockId);
                cur = new HashMap<String, String>(); cur.put("分值", blockId); blocks.add(cur);
                continue;
            }
            if (bare.startsWith("-")) {
                String actionText = bare.substring(1).trim();
                actionText = actionText.replaceAll("^\"(.*)\"$", "$1").replaceAll("^'(.*)'$", "$1");
                diag.add("  -> [动作] \"" + actionText + "\"");
                if (cur != null) {
                    parseNaturalAction(cur, actionText, diag);
                    log("[解析] cur=" + cur.get("分值"));
                } else {
                    diag.add("  -> [警告] 动作不在规则块内");
                }

                continue;
            }
            String[] kv = extractKV(line);
            if (kv == null) { diag.add("  -> [警告] 无法识别: \"" + line + "\""); continue; }
            String key = cleanKey(kv[0]); String value = kv[1].trim();
            if (key.isEmpty()) continue;
            value = value.replaceAll("^\"(.*)\"$", "$1").replaceAll("^'(.*)'$", "$1");
            String resolved = resolveAlias(key);
            if (!resolved.equals(key)) diag.add("  -> 同义词: \"" + key + "\" -> \"" + resolved + "\"");
            key = resolved;
            if (isKnownGlobal(key)) { globals.put(key, value); diag.add("  -> [全局] " + key + " = \"" + value + "\""); }
            else if (key.equals("分值")) { cur = new HashMap<String, String>(); cur.put(key, value); blocks.add(cur); diag.add("  -> [新规则] 分值 = " + value); }
            else if (isKnownBlock(key)) { if (cur == null) { cur = new HashMap<String, String>(); blocks.add(cur); } cur.put(key, value); diag.add("  -> [块内] " + key + " = \"" + value + "\""); }
            else { diag.add("  -> [跳过] 未知: \"" + key + "\""); }
        }
        String verStr = safeStr(globals.get("版本号"));
        if (!verStr.isEmpty()) { try { cfgVer = (int) Double.parseDouble(verStr); } catch (Exception e) { cfgVer = 0; } }
        cfgCdkObj = safeStr(globals.get("计分板")); cfgCyName = safeStr(globals.get("联控插件"));
        cfgLinkMode = safeStr(globals.get("联控模式")); cfgUpdateChannel = safeStr(globals.get("更新通道"));
        for (Map<String, String> block : blocks) {
            log("[解析] 块内容: " + block);

            int scoreKey = safeInt(block.get("分值"));
            if (scoreKey <= 0) continue;
            ScoreAction sa = new ScoreAction();
            sa.oneTime = "true".equals(block.get("_一次性"));
            if ("true".equals(block.get("_经济盲盒"))) {
                ActionEntry e = new ActionEntry("经济盲盒");
                e.moneyMin = safeDouble(block.get("_经济盲盒_min"));
                e.moneyMax = safeDouble(block.get("_经济盲盒_max"));
                if (e.moneyMin <= 0) e.moneyMin = 100; if (e.moneyMax <= 0) e.moneyMax = e.moneyMin;
                e.decimalPlaces = safeInt(block.getOrDefault("_小数位", "-1"));
                e.rawText = "经济盲盒 " + fmtMoney(e.moneyMin) + "~" + fmtMoney(e.moneyMax) + (e.decimalPlaces >= 0 ? " (" + e.decimalPlaces + "位小数)" : "");
                sa.actions.add(e);
            } else if ("true".equals(block.get("_联控盲盒"))) {
                ActionEntry e = new ActionEntry("联控盲盒");
                e.daysMin = safeInt(block.getOrDefault("_天数min", "7")); e.daysMax = safeInt(block.getOrDefault("_天数max", "90"));
                e.slotsMin = safeInt(block.getOrDefault("_格子min", "27")); e.slotsMax = safeInt(block.getOrDefault("_格子max", "136"));
                if (e.daysMin <= 0) e.daysMin = 7; if (e.daysMax <= 0) e.daysMax = 90;
                if (e.slotsMin <= 0) e.slotsMin = 27; if (e.slotsMax <= 0) e.slotsMax = 136;
                e.rawText = "联控盲盒 " + e.daysMin + "~" + e.daysMax + "天 " + e.slotsMin + "~" + e.slotsMax + "格";
                sa.actions.add(e);
            } else {
                if (block.containsKey("_发钱")) { ActionEntry e = new ActionEntry("发钱"); e.money = safeDouble(block.get("_发钱")); e.rawText = "发钱 $" + fmtMoney(e.money); sa.actions.add(e); }
                if (block.containsKey("_扣钱")) { ActionEntry e = new ActionEntry("扣钱"); e.money = safeDouble(block.get("_扣钱")); e.rawText = "扣钱 $" + fmtMoney(e.money); sa.actions.add(e); }
                if (block.containsKey("_联控")) { ActionEntry e = new ActionEntry("联控"); e.slots = safeInt(block.get("_格子数")); e.days = safeInt(block.get("_天数")); e.rawText = "联控 +" + e.slots + "格 " + e.days + "天"; sa.actions.add(e); }
            }
            if (block.containsKey("_小数位")) {
                int dp = safeInt(block.get("_小数位"));
                for (ActionEntry ae : sa.actions) ae.decimalPlaces = dp;
            }
            if ("true".equals(block.get("_删除口令"))) {
                ActionEntry de = new ActionEntry("删除口令");
                de.rawText = "删除口令";
                sa.actions.add(de);
                log("[解析] 添加删除口令动作");
            }


            if (sa.actions.isEmpty()) { diag.add("[警告] 分值" + scoreKey + " 无有效动作"); continue; }
            scoreMap.put(scoreKey, sa);
        }
        configLoaded = !cfgCdkObj.isEmpty();
    }
    private void parseNaturalAction(Map<String, String> block, String text, List<String> diag) {
        text = text.replaceAll("^['\"](.*)['\"]$", "$1").trim();
        if (text.contains("删") || text.contains("删除")) {
            block.put("_一次性", "true");
            block.put("_删除口令", "true");
            diag.add("    -> 识别: 删除口令");
        }

        Matcher decMatch = Pattern.compile("(\\d+)\\s*位\\s*小数").matcher(text);
        if (decMatch.find()) { block.put("_小数位", decMatch.group(1)); diag.add("    -> 识别: 小数位=" + decMatch.group(1)); }
        if (text.contains("盲盒") || text.contains("抽")) {
            double[] mr = extractMoneyRange(text);
            if (mr != null) { block.put("_经济盲盒", "true"); block.put("_经济盲盒_min", String.valueOf(mr[0])); block.put("_经济盲盒_max", String.valueOf(mr[1])); diag.add("    -> 识别: 经济盲盒 " + mr[0] + "~" + mr[1]); return; }
            int[] sr = extractSlotDayRange(text);
            if (sr != null) { block.put("_联控盲盒", "true"); block.put("_天数min", String.valueOf(sr[0])); block.put("_格子min", String.valueOf(sr[1])); block.put("_天数max", String.valueOf(sr[2])); block.put("_格子max", String.valueOf(sr[3])); diag.add("    -> 识别: 联控盲盒"); return; }
            block.put("_联控盲盒", "true"); diag.add("    -> 识别: 联控盲盒(默认)"); return;
        }
        boolean hasSlotKeyword = text.contains("背包") || text.contains("空间") || text.contains("格") || text.contains("激活");
        if (hasSlotKeyword) {
            int[] sd = extractSlotDay(text);
            if (sd != null) { block.put("_联控", "true"); block.put("_格子数", String.valueOf(sd[1])); block.put("_天数", String.valueOf(sd[0])); diag.add("    -> 识别: 联控 " + sd[0] + "天" + sd[1] + "格"); }
        }
        boolean isTake = text.contains("扣") || text.contains("减") || text.contains("扣除");
        if (isTake) { double amt = extractMoneyExcludingSlots(text); if (amt > 0) { block.put("_扣钱", String.valueOf(amt)); diag.add("    -> 识别: 扣钱 " + amt); } }
        boolean isGive = !isTake && (text.contains("给") || text.contains("奖") || text.contains("发") || text.contains("加"));
        if (isGive) { double amt = extractMoneyExcludingSlots(text); if (amt > 0) { block.put("_发钱", String.valueOf(amt)); diag.add("    -> 识别: 发钱 " + amt); } }
    }

    // PLACEHOLDER_PART4
    // ===== 工具方法 =====
    private String safeStr(String s) { return s == null ? "" : s.trim(); }
    private int safeInt(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        s = s.trim();
        try { return Integer.parseInt(s); } catch (Exception e) {}
        int result = 0; int current = 0;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            Integer val = CN_NUMS.get(ch);
            if (val == null) continue;
            if (val >= 10) { if (current == 0) current = 1; result += current * val; current = 0; }
            else { current = val; }
        }
        return result + current;
    }
    private double safeDouble(String s) { try { return Double.parseDouble(s == null ? "0" : s.trim()); } catch (Exception e) { return safeInt(s); } }
    private String fmtMoney(double v) { return v == (long) v ? String.valueOf((long) v) : String.format("%.2f", v); }
    private int findUnquoted(String s, char target) {
        boolean inSq = false; boolean inDq = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDq) inSq = !inSq;
            else if (c == '"' && !inSq) inDq = !inDq;
            else if (c == target && !inSq && !inDq) return i;
        }
        return -1;
    }
    private String[] extractKV(String line) {
        line = line.replaceAll("^[\\u2022\\u00b7\\-*\\u25ba\\u25b6>]+\\s*", "");
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ':' || c == '=' || c == '\uff1a') {
                String k = line.substring(0, i).trim().replaceAll("[\\[\\]{}()（）【】]", "").trim();
                String v = line.substring(i + 1).trim();
                if (!k.isEmpty()) return new String[]{k, v};
                break;
            }
        }
        return null;
    }
    private String cleanKey(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.trim().replaceAll("^[\\d]+[.]+", "").trim().replaceAll("[\\[\\]{}()（）【】]", "").trim();
    }
    private int getIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) { if (c == ' ') count++; else if (c == '\t') count += 4; else break; }
        return count;
    }
    private String resolveAlias(String key) {
        String lower = key.toLowerCase().trim();
        String canonical = ALIASES.get(lower);
        if (canonical != null) return canonical;
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) { if (lower.contains(entry.getKey())) return entry.getValue(); }
        return key;
    }
    private boolean isKnownGlobal(String key) {
        return key.equals("计分板") || key.equals("联控插件") || key.equals("版本号") || key.equals("更新通道") || key.equals("联控模式");
    }
    private boolean isKnownBlock(String key) {
        return key.equals("分值") || key.equals("动作") || key.equals("类型") || key.equals("格子数") || key.equals("天数") || key.equals("金额");
    }
    private double parseMoneyWithUnit(String numStr, String unitStr) {
        double num = Double.parseDouble(numStr);
        if (unitStr == null || unitStr.isEmpty() || unitStr.equals("元") || unitStr.equals("块")) return num;
        if (unitStr.equals("千")) return num * 1000;
        if (unitStr.equals("万")) return num * 10000;
        if (unitStr.equals("十万")) return num * 100000;
        if (unitStr.equals("百万")) return num * 1000000;
        return num;
    }
    private double extractMoneyAmount(String text) {
        double[] range = extractMoneyRange(text);
        if (range != null) return (range[0] + range[1]) / 2;
        Matcher m = Pattern.compile("([\\d.]+)\\s*(元|块|千|万|十万|百万)?").matcher(text);
        double max = 0;
        while (m.find()) { double a = parseMoneyWithUnit(m.group(1), m.group(2)); if (a > max) max = a; }
        return max;
    }
    private double[] extractMoneyRange(String text) {
        Matcher m = Pattern.compile("([\\d.]+)\\s*(元|块|千|万|十万|百万)?\\s*[~\\-到至]+\\s*([\\d.]+)\\s*(元|块|千|万|十万|百万)?").matcher(text);
        if (m.find()) {
            double min = parseMoneyWithUnit(m.group(1), m.group(2));
            double max = parseMoneyWithUnit(m.group(3), m.group(4));
            if (min > 0 && max > 0) { if (min > max) { double t = min; min = max; max = t; } return new double[]{min, max}; }
        }
        return null;
    }
    private double extractMoneyExcludingSlots(String text) {
        String[] parts = text.split("[，,、；;。]");
        double max = 0;
        for (String part : parts) {
            part = part.trim();
            if (part.contains("格") || part.contains("天") || part.contains("空间") || part.contains("背包") || part.contains("激活")) continue;
            double[] range = extractMoneyRange(part);
            if (range != null) { double avg = (range[0] + range[1]) / 2; if (avg > max) max = avg; continue; }
            Matcher m = Pattern.compile("([\\d.]+)\\s*(元|块|千|万|十万|百万)?").matcher(part);
            while (m.find()) { double a = parseMoneyWithUnit(m.group(1), m.group(2)); if (a > max) max = a; }
        }
        return max;
    }

    private int[] extractSlotDay(String text) {
        Matcher m1 = Pattern.compile("(\\d+)\\s*格.*?(\\d+)\\s*天").matcher(text);
        if (m1.find()) return new int[]{Integer.parseInt(m1.group(2)), Integer.parseInt(m1.group(1))};
        Matcher m2 = Pattern.compile("(\\d+)\\s*天.*?(\\d+)\\s*格").matcher(text);
        if (m2.find()) return new int[]{Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2))};
        Matcher m3 = Pattern.compile("(\\d+)\\s*格").matcher(text);
        if (m3.find()) return new int[]{0, Integer.parseInt(m3.group(1))};
        Matcher m4 = Pattern.compile("(\\d+)\\s*天").matcher(text);
        if (m4.find()) return new int[]{Integer.parseInt(m4.group(1)), 0};
        return null;
    }
    private int[] extractSlotDayRange(String text) {
        Matcher m = Pattern.compile("(\\d+)\\s*天\\s*(\\d+)\\s*格\\s*[~\\-到至]+\\s*(\\d+)\\s*天\\s*(\\d+)\\s*格").matcher(text);
        if (m.find()) return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))};
        return null;
    }
    private String deepMine(List<String> lines, int fromLine) {
        int baseIndent = getIndent(lines.get(fromLine));
        for (int i = fromLine + 1; i < lines.size(); i++) {
            String next = lines.get(i); String trimmed = next.trim();
            if (trimmed.isEmpty()) continue; if (getIndent(next) <= baseIndent) break;
            String[] kv = extractKV(trimmed);
            if (kv != null) { String val = kv[1].trim().replaceAll("^\"(.*)\"$", "$1").replaceAll("^'(.*)'$", "$1"); if (!val.isEmpty()) return val; }
            else { String raw = trimmed.replaceAll("[\\[\\]{}()（）【】]", "").trim(); if (!raw.isEmpty() && !raw.matches("^[\\d.:]+$")) return raw; }
        }
        return "";
    }
    private List<String> stripAllComments(List<String> rawLines, List<String> diag) {
        List<String> result = new ArrayList<String>();
        boolean inBlock = false; boolean inHtml = false;
        int totalComments = 0; int totalInline = 0;
        diag.add("===== 逐行诊断 ====="); diag.add("总行数: " + rawLines.size());
        for (int i = 0; i < rawLines.size(); i++) {
            String raw = rawLines.get(i); String trimmed = raw.trim(); String ln = String.format("L%02d", i + 1);
            if (trimmed.isEmpty()) { diag.add(ln + ": (空行)"); result.add(""); continue; }
            if (inBlock) { int endIdx = trimmed.indexOf("*/"); if (endIdx >= 0) { inBlock = false; String after = trimmed.substring(endIdx + 2).trim(); result.add(after.isEmpty() ? "" : after); } else result.add(""); diag.add(ln + ": " + raw); diag.add("     -> [踢除块注释中]"); totalComments++; continue; }
            if (inHtml) { int endIdx = trimmed.indexOf("-->"); if (endIdx >= 0) { inHtml = false; String after = trimmed.substring(endIdx + 3).trim(); result.add(after.isEmpty() ? "" : after); } else result.add(""); diag.add(ln + ": " + raw); diag.add("     -> [踢除HTML注释中]"); totalComments++; continue; }
            if (trimmed.contains("/*")) { int bs = trimmed.indexOf("/*"); int be = trimmed.indexOf("*/", bs + 2); if (be >= 0) { String b = trimmed.substring(0, bs).trim(); String a = trimmed.substring(be + 2).trim(); String c = (b + " " + a).trim(); result.add(c); } else { inBlock = true; String b = trimmed.substring(0, bs).trim(); result.add(b.isEmpty() ? "" : b); } diag.add(ln + ": " + raw); diag.add("     -> [踢除块注释]"); totalComments++; continue; }
            if (trimmed.contains("<!--")) { int hs = trimmed.indexOf("<!--"); int he = trimmed.indexOf("-->", hs + 4); if (he >= 0) { String b = trimmed.substring(0, hs).trim(); String a = trimmed.substring(he + 3).trim(); String c = (b + " " + a).trim(); result.add(c); } else { inHtml = true; String b = trimmed.substring(0, hs).trim(); result.add(b.isEmpty() ? "" : b); } diag.add(ln + ": " + raw); diag.add("     -> [踢除HTML注释]"); totalComments++; continue; }
            if (trimmed.startsWith("#") || trimmed.startsWith("//")) { diag.add(ln + ": " + raw); diag.add("     -> [踢除整行注释]"); result.add(""); totalComments++; continue; }
            int hashIdx = findUnquoted(trimmed, '#'); if (hashIdx >= 0) { trimmed = trimmed.substring(0, hashIdx).trim(); totalInline++; }
            int slashIdx = findUnquoted(trimmed, '/'); if (slashIdx >= 0 && slashIdx + 1 < trimmed.length() && trimmed.charAt(slashIdx + 1) == '/') { trimmed = trimmed.substring(0, slashIdx).trim(); totalInline++; }
            diag.add(ln + ": " + raw); diag.add("     -> [保留] \"" + trimmed + "\"");
            result.add(trimmed);
        }
        diag.add("总行: " + rawLines.size() + " | 注释: " + totalComments + " | 有效行: " + result.size());
        return result;
    }
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { log("[Vault] 未找到"); return; }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) { economy = rsp.getProvider(); log("[Vault] 已连接: " + economy.getName()); }
        else log("[Vault] 未找到经济提供者");
    }
    private void setupCyReflection() {
        cyPlugin = null; cyGiveSlots = null;
        if (!isLinkEnabled()) { log("[联控] 模式=" + cfgLinkMode + "，跳过"); return; }
        if (cfgCyName.isEmpty()) { log("[联控] 未配置，跳过"); return; }
        cyPlugin = Bukkit.getPluginManager().getPlugin(cfgCyName);
        if (cyPlugin == null) { for (String n : CY_DISCOVER_NAMES) { cyPlugin = Bukkit.getPluginManager().getPlugin(n); if (cyPlugin != null) break; } }
        if (cyPlugin == null) { log("[联控] 未找到: " + cfgCyName); return; }
        try { cyGiveSlots = cyPlugin.getClass().getMethod("onSdf1Activation", String.class, int.class, int.class); log("[联控] 已连接: " + cyPlugin.getName()); } catch (NoSuchMethodException e) { log("[联控] 缺少方法"); cyPlugin = null; }
    }
    private boolean isLinkEnabled() {
        if (cfgLinkMode.isEmpty()) return false;
        String m = cfgLinkMode.toLowerCase().trim();
        return !m.equals("关") && !m.equals("false") && !m.equals("停止") && !m.equals("off") && !m.equals("0") && !m.equals("禁用") && !m.equals("关闭");
    }
    private int lookupScore(String code) {
        if (!configLoaded || cfgCdkObj.isEmpty()) return -1;
        try {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective obj = board.getObjective(cfgCdkObj);
            if (obj == null) { if (!cdksWarned) { log("[计分板] 找不到: " + cfgCdkObj); cdksWarned = true; } return -1; }
            cdksWarned = false; Score s = obj.getScore(code);
            if (!s.isScoreSet()) return -1; return s.getScore();
        } catch (Exception e) { return -1; }
    }
    private void consumeCode(String code, boolean oneTime) {
        if (!oneTime) return;
        log("[删除] 口令: \"" + code + "\" 目标: " + cfgCdkObj);
        try {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective obj = board.getObjective(cfgCdkObj);
            if (obj != null) {
                Score sc = obj.getScore(code);
                if (sc.isScoreSet()) {
                    // 反射尝试 resetScore
                    boolean removed = false;
                    try {
                        java.lang.reflect.Method m = obj.getClass().getMethod("resetScore", String.class);
                        m.invoke(obj, code); removed = true;
                    } catch (Throwable ignored) {}
                    if (!removed) {
                        try {
                            java.lang.reflect.Method m = board.getClass().getMethod("resetScore", String.class);
                            m.invoke(board, code); removed = true;
                        } catch (Throwable ignored) {}
                    }
                    if (!removed) { sc.setScore(Integer.MIN_VALUE); removed = true; }
                    log("[删除] API结果: " + removed);
                }
            }
        } catch (Exception e) { log("[删除] API异常: " + e.getMessage()); }
        fallbackDelete(code);
    }
    private void fallbackDelete(final String code) {
        Bukkit.getScheduler().runTask(this, new Runnable() { public void run() {
            String safeCode = code.replace("\"", "\\\"");
            String cmd = "scoreboard players reset " + safeCode + " " + cfgCdkObj;
            log("[删除] 兜底执行: " + cmd);
            try {
                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                log("[删除] 兜底结果: " + ok);
            } catch (Exception e) { log("[删除] 兜底失败: " + e.getMessage()); }
        } });
    }




    private void startListening(Player p) {
        listening.put(p.getUniqueId(), System.currentTimeMillis());
        p.sendMessage("§a[SDF1] §f已开启口令监听 (15秒)");
    }
    private void stopListening(UUID uuid, boolean silent) {
        listening.remove(uuid);
        if (!silent) { Player pl = Bukkit.getPlayer(uuid); if (pl != null && pl.isOnline()) pl.sendMessage("§e[SDF1] 监听已关闭"); }
    }

    // ===== 聊天监听 =====
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer(); UUID u = p.getUniqueId(); String msg = event.getMessage().trim();
        if (msg.isEmpty() || !listening.containsKey(u)) return;
        event.setCancelled(true); stopListening(u, true);
        p.sendMessage("§a[SDF1] §f已拦截，比对中...");
        Long last = chatCd.get(u); if (last != null && System.currentTimeMillis() - last < 500) return;
        chatCd.put(u, System.currentTimeMillis());
        int scoreVal = lookupScore(msg);
        if (scoreVal < 0) { p.sendMessage("§c[SDF1] 口令无效"); log("[拦截] " + p.getName() + " 无效: \"" + msg + "\""); return; }
        ScoreAction sa = scoreMap.get(scoreVal);
        if (sa == null) { p.sendMessage("§c[SDF1] 规则未配置"); log("[拦截] " + p.getName() + " 分值=" + scoreVal + " 无规则"); return; }
        log("[拦截] " + p.getName() + " 分值=" + scoreVal + " 动作数=" + sa.actions.size() + (sa.oneTime ? " 一次性" : ""));
        boolean oneTime = sa.oneTime;
        p.sendMessage("§a[SDF1] 执行: " + sa.actions.size() + "个动作");
        for (ActionEntry ae : sa.actions) {
            log("[执行] [" + ae.type + "] " + ae.rawText);
            if ("发钱".equals(ae.type)) execGiveMoney(p, ae, oneTime, msg);
            else if ("扣钱".equals(ae.type)) execTakeMoney(p, ae, oneTime, msg);
            else if ("经济盲盒".equals(ae.type)) execMoneyBox(p, ae, oneTime, msg);
            else if ("联控盲盒".equals(ae.type)) execSlotBox(p, ae, oneTime, msg);
            else if ("联控".equals(ae.type)) execCy(p, ae, oneTime, msg);
            else if ("删除口令".equals(ae.type)) execDelete(p, ae, oneTime, msg);


        }
    }

    // ===== 执行动作 =====
    private String fmtAmount(double v, int dp) {
        if (dp >= 0) return String.format("%." + dp + "f", v);
        return v == (long) v ? String.valueOf((long) v) : String.format("%.2f", v);
    }
    private void execGiveMoney(Player p, ActionEntry a, boolean oneTime, String code) {
        if (economy == null) { p.sendMessage("§c[SDF1] 经济不可用"); return; }
        double before = economy.getBalance(p); economy.depositPlayer(p, a.money); double after = economy.getBalance(p);
        consumeCode(code, oneTime); p.sendMessage("§a[SDF1] 获得 §e$" + fmtAmount(a.money, a.decimalPlaces));
        p.sendMessage("§7余额: §a$" + fmtMoney(before) + " §7-> §a$" + fmtMoney(after));
        log("[发钱] " + p.getName() + " +$" + fmtAmount(a.money, a.decimalPlaces));
    }
  private void execTakeMoney(Player p, ActionEntry a, boolean oneTime, String code) {
        if (economy == null) { p.sendMessage("§c[SDF1] 经济不可用"); return; }
        double amount = Math.abs(a.money);
        if (!economy.has(p, amount)) { p.sendMessage("§c[SDF1] 余额不足! 需要: $" + fmtAmount(amount, a.decimalPlaces)); return; }
        double before = economy.getBalance(p); economy.withdrawPlayer(p, amount); double after = economy.getBalance(p);
        consumeCode(code, oneTime); p.sendMessage("§c[SDF1] 扣除 §e$" + fmtAmount(amount, a.decimalPlaces));
        p.sendMessage("§7余额: §a$" + fmtMoney(before) + " §7-> §a$" + fmtMoney(after));
        log("[扣款] " + p.getName() + " -$" + fmtAmount(amount, a.decimalPlaces));
    }

    private void execMoneyBox(final Player p, final ActionEntry a, final boolean oneTime, final String code) {
        if (economy == null) { p.sendMessage("§c[SDF1] 经济不可用"); return; }
        p.sendMessage("§6§l[SDF1] §e§l盲盒开启中...");
        Bukkit.getScheduler().runTaskLater(this, new Runnable() { public void run() {
            p.sendMessage("§6§l[SDF1] §e§l正在摇晃盲盒...");
            Bukkit.getScheduler().runTaskLater(Main.this, new Runnable() { public void run() {
                p.sendMessage("§6§l[SDF1] §e§l即将揭晓...");
                Bukkit.getScheduler().runTaskLater(Main.this, new Runnable() { public void run() {
                    double amount = a.moneyMin + rng.nextDouble() * (a.moneyMax - a.moneyMin);
                    amount = Math.round(amount * 100.0) / 100.0;
                    double before = economy.getBalance(p); economy.depositPlayer(p, amount); double after = economy.getBalance(p);
                    consumeCode(code, oneTime);
                    p.sendMessage("§6§l[SDF1] §e§l恭喜！获得 $§c§l" + fmtMoney(amount) + " §e§l！");
                    p.sendMessage("§7余额: §a$" + fmtMoney(before) + " §7-> §a$" + fmtMoney(after));
                    log("[经济盲盒] " + p.getName() + " +$" + fmtMoney(amount));
                } }, 20L);
            } }, 20L);
        } }, 20L);
    }
    private void execSlotBox(final Player p, final ActionEntry a, final boolean oneTime, final String code) {
        p.sendMessage("§6§l[SDF1] §e§l盲盒开启中...");
        Bukkit.getScheduler().runTaskLater(this, new Runnable() { public void run() {
            p.sendMessage("§6§l[SDF1] §e§l正在摇晃盲盒...");
            Bukkit.getScheduler().runTaskLater(Main.this, new Runnable() { public void run() {
                p.sendMessage("§6§l[SDF1] §e§l即将揭晓...");
                Bukkit.getScheduler().runTaskLater(Main.this, new Runnable() { public void run() {
                    int rd = a.daysMin + rng.nextInt(Math.max(1, a.daysMax - a.daysMin + 1));
                    int rs = a.slotsMin + rng.nextInt(Math.max(1, a.slotsMax - a.slotsMin + 1));
                    if (tryCyActivate(p, rs, rd)) {
                        consumeCode(code, oneTime);
                        p.sendMessage("§6§l[SDF1] §e§l恭喜！获得 §c§l" + rd + "天会员 + " + rs + "格空间 §e§l！");
                    } else { p.sendMessage("§c[SDF1] 联控连接失败，盲盒作废"); }
                } }, 20L);
            } }, 20L);
        } }, 20L);
    }
    private void execCy(Player p, ActionEntry a, boolean oneTime, String code) {
        if (tryCyActivate(p, a.slots, a.days)) { consumeCode(code, oneTime); p.sendMessage("§a[SDF1] 激活: " + a.slots + "格 " + (a.days > 0 ? a.days + "天" : "永久")); }
    }
    private boolean tryCyActivate(Player p, int slots, int days) {
        if (!isCyConnected()) { discoverCyPlugin(); if (!isCyConnected()) { p.sendMessage("§c[SDF1] 联控未连接，跳过"); return false; } }
        try { discoveredCyActivate.invoke(discoveredCy, p.getName(), slots, days); return true; }
        catch (Exception e) { p.sendMessage("§c[SDF1] 联控失败，跳过"); log("[联控] 失败: " + e.getMessage()); return false; }
    }
    private void execDelete(Player p, ActionEntry a, boolean oneTime, String code) {
        log("[删除] " + p.getName() + " 口令: \"" + code + "\"");
        consumeCode(code, true);
    }



    // ===== 插件事件 =====
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        String name = event.getPlugin().getName();
        for (String n : CY_DISCOVER_NAMES) {
            if (name.equalsIgnoreCase(n)) { log("[联控] 检测到 " + name); discoverCyPlugin(); setupCyReflection(); return; }
        }
    }
    @EventHandler
    public void onPluginDisableEvent(PluginDisableEvent event) {
        String name = event.getPlugin().getName();
        if (cyPlugin != null && name.equals(cyPlugin.getName())) { log("[联控] " + name + " 卸载"); cyPlugin = null; cyGiveSlots = null; }
        if (discoveredCy != null && name.equals(discoveredCy.getName())) { discoveredCy = null; discoveredCyActivate = null; discoveredCyPing = null; }
    }

    // ===== 指令 =====
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0) {
            if (s instanceof Player) { Player p = (Player) s; if (!configLoaded) { p.sendMessage("§c未加载"); return true; } if (cfgCdkObj.isEmpty()) { p.sendMessage("§c未配置"); return true; } startListening(p); }
            else { s.sendMessage("[SDF1] v" + cfgVer + " rules=" + scoreMap.size() + " 联控:" + (isCyConnected() ? "是" : "否") + " Vault:" + (economy != null ? "是" : "否")); }
            return true;
        }
        String cmd = a[0].toLowerCase();
        if (cmd.equals("reload")) {
            if (!s.isOp()) { s.sendMessage("§c仅OP"); return true; }
            scoreMap.clear(); configLoaded = false; cdksWarned = false; listening.clear(); failCount = 0; circuitBroken = false;
            loadConfig(true); setupCyReflection(); discoverCyPlugin(); setupEconomy();
            s.sendMessage("[SDF1] 重载 v" + cfgVer + " 规则:" + scoreMap.size());
        } else if (cmd.equals("update")) { if (!s.isOp()) { s.sendMessage("§c仅OP"); return true; } checkUpdate(s); }
        else if (cmd.equals("listen")) { if (!(s instanceof Player)) { s.sendMessage("§c仅玩家"); return true; } startListening((Player) s); }
        else if (cmd.equals("status")) { s.sendMessage("[SDF1] v" + cfgVer + " 计分板:" + cfgCdkObj + " 规则:" + scoreMap.size() + " 联控:" + cyPing() + " Vault:" + (economy != null ? economy.getName() : "无")); }
        else s.sendMessage("[SDF1] /sdf1 [listen|reload|update|status]");
        return true;
    }

    // ===== 更新检查 =====
    private void checkUpdate(final CommandSender manual) {
        new Thread(new Runnable() { public void run() {
            try { boolean preferGH = "GH".equalsIgnoreCase(cfgUpdateChannel) || cfgUpdateChannel.isEmpty();
                String pApi = preferGH ? API_GH : API_GE; String pDl = preferGH ? DL_GH : DL_GE; String pName = preferGH ? "GitHub" : "Gitee";
                String bApi = preferGH ? API_GE : API_GH; String bDl = preferGH ? DL_GE : DL_GH; String bName = preferGH ? "Gitee" : "GitHub";
                String[] result = fetchRelease(pApi, pName); if (result != null) { applyUpdate(result[0], result[1], pDl, manual, pName); return; }
                log("[更新] " + pName + " 失败，切换 " + bName);
                result = fetchRelease(bApi, bName); if (result != null) { applyUpdate(result[0], result[1], bDl, manual, bName); return; }
                log("[更新] 双路均失败"); if (manual != null) manual.sendMessage("[更新] 检查失败");
            } catch (Exception e) { log("[更新] 异常: " + e.getMessage()); }
        } }).start();
    }
    private String[] fetchRelease(String apiUrl, String ch) {
        try {
            TrustManager[] trustAll = new TrustManager[]{ new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                public boolean verify(String h, javax.net.ssl.SSLSession s) { return true; }
            });
            HttpURLConnection c = (HttpURLConnection) new URL(apiUrl).openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("User-Agent", "SDF1-Plugin/1.0");
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setInstanceFollowRedirects(true);
            if (c.getResponseCode() != 200) {
                log("[更新] " + ch + " HTTP " + c.getResponseCode());
                return null;
            }
            String json = new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String rv = jParse(json, "tag_name");
            String rn = jParse(json, "body");
            if (rv == null || rv.isEmpty()) return null;
            return new String[]{rv, rn != null ? rn : ""};
        } catch (Exception e) {
            log("[更新] " + ch + " " + e.getMessage());
            return null;
        }
    }

    private void applyUpdate(String rv, String notes, String dlLink, CommandSender manual, String ch) {
        remoteVer = rv; if (!rv.equals(String.valueOf(cfgVer))) {
            String msg = "[SDF1] 新版本! v" + cfgVer + " -> v" + rv; log(msg); log("下载: " + dlLink);
            if (manual != null) { manual.sendMessage(msg); manual.sendMessage("下载: " + dlLink); }
            for (Player op : Bukkit.getOnlinePlayers()) { if (op.isOp()) { op.sendMessage(msg); op.sendMessage("下载: " + dlLink); } }
        } else { log("[更新] 已是最新 v" + cfgVer); if (manual != null) manual.sendMessage("[SDF1] 已是最新 v" + cfgVer); }
    }
    private static String jParse(String j, String k) {
        int i = j.indexOf("\"" + k + "\""); if (i < 0) return ""; int colon = j.indexOf(":", i);
        int start = j.indexOf("\"", colon + 1); if (start < 0) return ""; int end = j.indexOf("\"", start + 1);
        if (end < 0) return ""; return j.substring(start + 1, end);
    }
}
