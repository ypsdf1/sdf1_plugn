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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.sql.*;


public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private String cfgCdkObj = "";
    private String cfgCyName = "";
    private String cfgVer = "";
    private boolean configLoaded = false;
    private boolean cdksWarned = false;
    private String cfgLinkMode = "";
    private String cfgUpdateChannel = "";
    private String cfgNameBoard = "";
    private String cfgAdminTeam = "";
    private String cfgAdminTag = "";
    private final Map<Integer, ScoreAction> scoreMap = new HashMap<Integer, ScoreAction>();
    private Economy economy = null;
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
    private static final String[] CY_DISCOVER_NAMES = {"CY_beibao", "CY", "cy_beibao", "Cy", "cy"};
    private Plugin discoveredCy = null;
    private java.lang.reflect.Method discoveredCyActivate = null;
    private java.lang.reflect.Method discoveredCyPing = null;
    private long lastCyDiscover = 0;
    private static final long DISCOVER_INTERVAL = 30000L;
    private String cfgCouponBoard = "";
    private final Map<Integer, Integer> couponRules
            = new HashMap<>();


    private static final Map<String, String> ALIASES = new HashMap<String, String>();
    static {
        String[][] pairs = {
                {"计分板","计分板"},{"口令板","计分板"},{"口令库","计分板"},{"scoreboard","计分板"},
                {"码库","计分板"},{"码板","计分板"},{"兑换码","计分板"},{"激活码","计分板"},
                {"cdks","计分板"},{"cd-key","计分板"},{"cdkey","计分板"},{"cdk","计分板"},
                {"码表","计分板"},{"口令表","计分板"},{"兑换板","计分板"},{"兑换表","计分板"},
                {"记名板","记名板"},{"记名","记名板"},{"nameboard","记名板"},{"查重板","记名板"},
                {"查重","记名板"},{"dedup","记名板"},{"记录板","记名板"},{"已领","记名板"},
                {"已兑换","记名板"},{"领取记录","记名板"},{"防重复","记名板"},
                {"领过","记名板"},{"用过","记名板"},{"去重","记名板"},{"去重板","记名板"},
                {"联控插件","联控插件"},{"联控","联控插件"},{"cy插件","联控插件"},
                {"cy","联控插件"},{"cy_beibao","联控插件"},{"被控","联控插件"},
                {"联动","联控插件"},{"联动插件","联控插件"},{"cyplugin","联控插件"},
                {"副插件","联控插件"},{"附属","联控插件"},
                {"分值","分值"},{"分数","分值"},{"score","分值"},{"points","分值"},
                {"point","分值"},{"积分","分值"},{"权重","分值"},
                {"动作","动作"},{"action","动作"},{"操作","动作"},{"行为","动作"},
                {"执行","动作"},{"效果","动作"},{"处理","动作"},
                {"类型","类型"},{"type","类型"},{"种类","类型"},{"类别","类型"},
                {"格子数","格子数"},{"格子","格子数"},{"slots","格子数"},{"空间","格子数"},
                {"格数","格子数"},{"背包","格子数"},{"容量","格子数"},{"slot","格子数"},
                {"格口","格子数"},{"格位","格子数"},{"仓位","格子数"},{"位置数","格子数"},
                {"天数","天数"},{"天","天数"},{"days","天数"},{"时长","天数"},{"期限","天数"},
                {"day","天数"},{"有效期","天数"},{"持续","天数"},
                {"金额","金额"},{"金","金额"},{"钱","金额"},{"money","金额"},
                {"价格","金额"},{"price","金额"},{"元","金额"},{"块","金额"},
                {"费用","金额"},{"cost","金额"},{"代价","金额"},
                {"版本号","版本号"},{"版本","版本号"},{"version","版本号"},{"ver","版本号"},{"版","版本号"},
                {"更新通道","更新通道"},{"通道","更新通道"},{"channel","更新通道"},{"更新源","更新通道"},
                {"联控模式","联控模式"},{"联动模式","联控模式"},{"linkmode","联控模式"},{"联控开关","联控模式"},
                {"管理团队","管理团队"},{"admin团队","管理团队"},{"team","管理团队"},
                {"管理组","管理团队"},{"admin-team","管理团队"},{"admin_team","管理团队"},
                {"权限组","管理团队"},{"管理队伍","管理团队"},
                {"管理标签","管理标签"},{"admin标签","管理标签"},{"tag","管理标签"},
                {"管理标记","管理标签"},{"admin-tag","管理标签"},{"admin标记","管理标签"},
                {"admin_tag","管理标签"},{"权限标签","管理标签"}
        };
        for (String[] p : pairs) ALIASES.put(p[0].toLowerCase(), p[1]);
    }

    private static final Map<String, Integer> CN_NUMS = new HashMap<String, Integer>();
    static {
        CN_NUMS.put("零",0); CN_NUMS.put("一",1); CN_NUMS.put("二",2);
        CN_NUMS.put("三",3); CN_NUMS.put("四",4); CN_NUMS.put("五",5);
        CN_NUMS.put("六",6); CN_NUMS.put("七",7); CN_NUMS.put("八",8);
        CN_NUMS.put("九",9); CN_NUMS.put("十",10); CN_NUMS.put("百",100);
        CN_NUMS.put("千",1000); CN_NUMS.put("万",10000);
        CN_NUMS.put("亿",100000000);
        CN_NUMS.put("壹",1); CN_NUMS.put("贰",2); CN_NUMS.put("叁",3);
        CN_NUMS.put("肆",4); CN_NUMS.put("伍",5); CN_NUMS.put("陆",6);
        CN_NUMS.put("柒",7); CN_NUMS.put("捌",8); CN_NUMS.put("玖",9);
        CN_NUMS.put("拾",10); CN_NUMS.put("佰",100); CN_NUMS.put("仟",1000);
        CN_NUMS.put("两",2);
    }

    private static class ActionEntry {
        String type;
        double money, moneyMin, moneyMax;
        int slots, days, slotsMin, slotsMax,
                daysMin, daysMax;
        int bondMin, bondMax;
        int decimalPlaces = -1;
        String rawText = "";
        ActionEntry(String t) { type = t; }
    }



    private static class ScoreAction {
        List<ActionEntry> actions = new ArrayList<ActionEntry>();
        boolean deleteCode = false;
        boolean recordName = false;
    }
    // ===== 债券DB（跨插件访问Sdf1_login的bond.db）=====
    private Connection bondDb;

    private Connection getBondDb() {
        if (bondDb != null) {
            try {
                if (!bondDb.isClosed()) return bondDb;
            } catch (Exception ignored) {}
            bondDb = null;
        }
        try {
            Plugin sdf1Login = Bukkit.getPluginManager()
                    .getPlugin("Sdf1_login");
            if (sdf1Login == null) return null;
            File dbFile = new File(
                    sdf1Login.getDataFolder(),
                    "bond.db");
            if (!dbFile.exists()) return null;
            Class.forName("org.sqlite.JDBC");
            bondDb = DriverManager.getConnection(
                    "jdbc:sqlite:"
                            + dbFile.getAbsolutePath());
            return bondDb;
        } catch (Exception e) {
            log("[Bond] DB连接失败: "
                    + e.getMessage());
            return null;
        }
    }



    // ===== 撤销记录 =====
    private static class UndoRecord {
        String code;
        int scoreValue;
        UndoRecord(String c, int s) { code = c; scoreValue = s; }
    }

    private final List<UndoRecord> undoBuf = new ArrayList<UndoRecord>();
    private boolean canUndo = false;

    private String colorize(String s) {
        if (s == null) return "";
        return s.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    @Override
    public void onEnable() {
        log("[SDF1] ===== 启动 =====");
        extractDefaultConfig();
        loadConfig(true);
        setupEconomy();
        discoverCyPlugin();
        getCommand("sdf1").setExecutor(this);
        getCommand("import").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        checkUpdate(null);
        cfgCouponBoard = "";
        couponRules.clear();

        log("===== 启动完成 =====");
    }

    @Override
    public void onDisable() { log("卸载"); listening.clear(); }

    private void log(String msg) { getLogger().info(msg); }

    private void discoverCyPlugin() {
        long now = System.currentTimeMillis();
        if (now - lastCyDiscover < DISCOVER_INTERVAL) return;
        lastCyDiscover = now;
        if (!isLinkEnabled()) { log("[联控] 模式=" + cfgLinkMode + "，跳过"); return; }
        if (discoveredCy != null && discoveredCy.isEnabled()) return;
        discoveredCy = null;
        discoveredCyActivate = null;
        discoveredCyPing = null;
        for (String name : CY_DISCOVER_NAMES) {
            Plugin p = Bukkit.getPluginManager().getPlugin(name);
            if (p != null && p.isEnabled()) {
                discoveredCy = p;
                try {
                    discoveredCyActivate = p.getClass().getMethod(
                            "onSdf1Activation", String.class, int.class, int.class);
                } catch (Exception ignored) {}
                try {
                    discoveredCyPing = p.getClass().getMethod("onSdf1Ping");
                } catch (Exception ignored) {}
                if (discoveredCyActivate != null) {
                    log("[共享] 发现被控: " + p.getName());
                } else {
                    discoveredCy = null;
                    log("[共享] " + p.getName() + " 缺少共享方法，跳过");
                }
                return;
            }
        }
        log("[共享] 未发现被控");
    }

    private boolean isCyConnected() {
        return discoveredCy != null && discoveredCy.isEnabled()
                && discoveredCyActivate != null;
    }

    private String cyPing() {
        if (!isCyConnected()) {
            discoverCyPlugin();
            if (!isCyConnected()) return "未连接";
        }
        if (discoveredCyPing != null) {
            try {
                Object r = discoveredCyPing.invoke(discoveredCy);
                return r != null ? r.toString() : "OK";
            } catch (Exception e) { return "ping失败"; }
        }
        return "OK(无ping)";
    }

    private boolean isLinkEnabled() {
        if (cfgLinkMode.isEmpty()) return false;
        String m = cfgLinkMode.toLowerCase().trim();
        return !m.equals("关") && !m.equals("false") && !m.equals("停止")
                && !m.equals("off") && !m.equals("0") && !m.equals("禁用")
                && !m.equals("关闭");
    }

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
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
                fos.close();
                in.close();
                log("[SDF1] jar提取完成 " + f.length() + "字节");
                return;
            }
        } catch (Exception e) { log("[SDF1] jar提取异常: " + e.getMessage()); }
        writeFallbackConfig(f);
    }

    private void writeFallbackConfig(File f) {
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            pw.println("# SDF1 主控配置");
            pw.println("版本号: 1.0");
            pw.println("更新通道: GH");
            pw.println("管理团队: admin");
            pw.println("管理标签: admin");
            pw.println();
            pw.println("计分板: ");
            pw.println("联控插件: ");
            pw.println("联控模式: 开");
            pw.println("记名板: ");
            pw.println();
            pw.println("1:");
            pw.println("  - 给那个玩家100块");
            pw.println();
            pw.println("2:");
            pw.println("  - 给那个玩家抽个盲盒");
            pw.println("  - 然后口令删了");
            pw.flush();
            pw.close();
        } catch (Exception e) { log("[SDF1] 写入失败: " + e.getMessage()); }
    }

    private void loadConfig(boolean verbose) {
        scoreMap.clear();
        configLoaded = false;
        cdksWarned = false;
        cfgCdkObj = "";
        cfgCyName = "";
        cfgVer = "";
        cfgLinkMode = "";
        cfgUpdateChannel = "";
        cfgNameBoard = "";
        File f = new File(getDataFolder(), "设置.txt");
        if (!f.exists() || f.length() == 0) extractDefaultConfig();
        try {
            List<String> rawLines = new ArrayList<String>();
            Charset encoding = detectEncoding(f);
            log("[SDF1] 配置编码: " + encoding.name());
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), encoding));
            String line;
            while ((line = r.readLine()) != null) rawLines.add(line);
            r.close();
            // [SDF1-诊断] 逐行打印已注释，仅保留最后配置结果
            // log("[SDF1-诊断] 文件编码检测: " + encoding.name());
            // log("[SDF1-诊断] 文件大小: " + f.length() + " 字节");
            // log("[SDF1-诊断] ===== 原始行内容(前20行) =====");
            // for (int i = 0; i < Math.min(20, rawLines.size()); i++) {
            //     String raw = rawLines.get(i);
            //     log("[SDF1-诊断] L" + (i + 1) + ": [" + raw + "]");
            //     StringBuilder hex = new StringBuilder();
            //     for (int j = 0; j < Math.min(60, raw.length()); j++) {
            //         char c = raw.charAt(j);
            //         hex.append(String.format("U+%04X ", (int) c));
            //     }
            //     log("[SDF1-诊断] L" + (i + 1) + "码点: " + hex.toString().trim());
            // }
            // log("[SDF1-诊断] ===== 原始行抄送完毕 =====");
            // log("[SDF1-诊断] ===== 标准化后内容(前20行) =====");
            // for (int i = 0; i < Math.min(20, rawLines.size()); i++) {
            //     String raw = rawLines.get(i).trim();
            //     String norm = normalizeText(raw);
            //     if (!raw.equals(norm)) {
            //         log("[SDF1-诊断] L" + (i + 1) + " 有变化!");
            //         log("[SDF1-诊断]   原始: [" + raw + "]");
            //         log("[SDF1-诊断]   标准: [" + norm + "]");
            //         StringBuilder hexOrig = new StringBuilder();
            //         StringBuilder hexNorm = new StringBuilder();
            //         for (int j = 0; j < Math.min(40, raw.length()); j++) {
            //             hexOrig.append(String.format("U+%04X ", (int) raw.charAt(j)));
            //         }
            //         for (int j = 0; j < Math.min(40, norm.length()); j++) {
            //             hexNorm.append(String.format("U+%04X ", (int) norm.charAt(j)));
            //         }
            //         log("[SDF1-诊断]   原始码点: " + hexOrig.toString().trim());
            //         log("[SDF1-诊断]   标准码点: " + hexNorm.toString().trim());
            //     }
            // }
            // log("[SDF1-诊断] ===== 标准化抄送完毕 =====");
            if (verbose) {
                log("===== 配置原文 =====");
                for (int i = 0; i < rawLines.size(); i++)
                    log(String.format("L%02d: %s", i + 1, rawLines.get(i)));
            }
            List<String> diag = new ArrayList<String>();
            List<String> cleanLines = stripAllComments(rawLines, diag);
            if (verbose) { log("===== 逐行解析 ====="); for (String d : diag) log(d); }
            deepParse(cleanLines, diag);
            if (verbose) {
                log("===== 最终配置 =====");
                log("版本号: " + cfgVer);
                log("计分板: " + (cfgCdkObj.isEmpty() ? "(未设置)" : cfgCdkObj));
                log("联控插件: " + (cfgCyName.isEmpty() ? "(未设置)" : cfgCyName));
                log("联控模式: " + (cfgLinkMode.isEmpty() ? "(未设置)" : cfgLinkMode));
                log("记名板: " + (cfgNameBoard.isEmpty() ? "(未设置)" : cfgNameBoard));
                log("规则数: " + scoreMap.size());
                for (Map.Entry<Integer, ScoreAction> entry : scoreMap.entrySet()) {
                    ScoreAction sa = entry.getValue();
                    log("  分值" + entry.getKey() + " -> " + sa.actions.size()
                            + "个动作" + (sa.recordName ? " (记名)" : "")
                            + (sa.deleteCode ? " (删口令)" : ""));
                    for (int i = 0; i < sa.actions.size(); i++) {
                        ActionEntry ae = sa.actions.get(i);
                        log("    动作" + (i + 1) + ": [" + ae.type + "] " + ae.rawText);
                    }
                }
                log("优惠券规则数: " + couponRules.size());
                for (Map.Entry<Integer, Integer> ce
                        : couponRules.entrySet()) {
                    log("  " + ce.getKey() + "分 → "
                            + ce.getValue() + "%");
                }

            }
        } catch (Exception e) { log("[SDF1] 读取失败: " + e.getMessage()); }
    }
    /**
     * 供 Sdf1_shop 反射调用
     * 在计分板中查找优惠券码并核销
     * @return 折扣百分比(90=9折)，无效返回-1
     */
    public int redeemCoupon(String code) {
        if (cfgCdkObj.isEmpty()
                || couponRules.isEmpty()) return -1;

        try {
            Scoreboard board =
                    Bukkit.getScoreboardManager()
                            .getMainScoreboard();
            Objective obj =
                    board.getObjective(cfgCdkObj);
            if (obj == null) return -1;

            String canonical = null;
            int scoreVal = -1;

            // 精确匹配
            Score s = obj.getScore(code);
            if (s.isScoreSet()) {
                canonical = code;
                scoreVal = s.getScore();
            }

            // 模糊匹配
            if (canonical == null) {
                String normInput =
                        normalizeForCompare(code);
                for (String entry : board.getEntries()) {
                    Score sc = obj.getScore(entry);
                    if (sc.isScoreSet()
                            && normalizeForCompare(entry)
                            .equals(normInput)) {
                        canonical = entry;
                        scoreVal = sc.getScore();
                        break;
                    }
                }
            }

            if (canonical == null) return -1;

            // 查规则：这个分值有对应的优惠券规则吗
            Integer discount =
                    couponRules.get(scoreVal);
            if (discount == null) return -1;

            // 核销：删除该口令
            consumeCode(canonical, true);
            log("[优惠券] 核销: \"" + canonical
                    + "\" 分值=" + scoreVal
                    + " → " + discount + "%");
            return discount;

        } catch (Exception e) {
            log("[优惠券] 异常: " + e.getMessage());
            return -1;
        }
    }

    public int validateCoupon(String code) {
        log("[优惠券] 输入: \"" + code + "\"");
        log("[优惠券] 计分板名: \"" + cfgCdkObj + "\"");
        log("[优惠券] 规则数: " + couponRules.size());

        // ★ 规则丢失时自动重新加载
        if (cfgCdkObj.isEmpty() || couponRules.isEmpty()) {
            log("[优惠券] 规则为空，重新加载配置");
            loadConfig(false);
            log("[优惠券] 重载后规则数: "
                    + couponRules.size());
        }

        if (cfgCdkObj.isEmpty()) {
            log("[优惠券] 终止: 计分板名为空");
            return -1;
        }
        if (couponRules.isEmpty()) {
            log("[优惠券] 终止: 无优惠券规则");
            return -1;
        }
        if (cfgCdkObj.isEmpty() || couponRules.isEmpty()) {
            log("[优惠券] 终止: 重载后仍无规则");
            return -1;
        }
        try {
            Scoreboard board =
                    Bukkit.getScoreboardManager()
                            .getMainScoreboard();
            Objective obj =
                    board.getObjective(cfgCdkObj);
            if (obj == null) {
                log("[优惠券] 终止: 计分板目标不存在: "
                        + cfgCdkObj);
                return -1;
            }

            String canonical = null;
            int scoreVal = -1;
// 在精确匹配之前加：
            String normalized = normalizeForCompare(code);
            if (normalized.isEmpty()) {
                log("[优惠券] 终止: 标准化后为空串");
                return -1;
            }

            // 精确匹配
            Score s = obj.getScore(code);
            if (s.isScoreSet()) {
                canonical = code;
                scoreVal = s.getScore();
            }

            // 模糊匹配
            if (canonical == null) {
                String norm =
                        normalizeForCompare(code);
                for (String entry : board.getEntries()) {
                    Score sc = obj.getScore(entry);
                    if (sc.isScoreSet()
                            && normalizeForCompare(entry)
                            .equals(norm)) {
                        canonical = entry;
                        scoreVal = sc.getScore();
                        break;
                    }
                }
            }

            if (canonical == null) {
                log("[优惠券] 终止: 计分板中找不到: "
                        + code);
                return -1;
            }

            Integer discount =
                    couponRules.get(scoreVal);
            if (discount == null) {
                log("[优惠券] 终止: 分值" + scoreVal
                        + " 无对应规则");
                return -1;
            }

            log("[优惠券] 验证通过: " + canonical
                    + " " + discount + "%");
            return discount;
        } catch (Exception e) {
            log("[优惠券] 异常: " + e.getMessage());
            return -1;
        }
    }


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
                diag.add("  -> [规则块] 分值 = " + blockId);
                cur = new HashMap<String, String>();
                cur.put("分值", blockId);
                blocks.add(cur);
                continue;
            }
            if (bare.startsWith("-")) {
                String actionText = bare.substring(1).trim();
                actionText = actionText.replaceAll("^\"(.*)\"$", "$1")
                        .replaceAll("^'(.*)'$", "$1");
                diag.add("  -> [动作] \"" + actionText + "\"");
                if (cur != null) {
                    parseNaturalAction(cur, actionText, diag);
                } else {
                    diag.add("  -> [警告] 动作不在规则块内");
                }
                continue;
            }
            String[] kv = extractKV(line);
            if (kv == null) {
                diag.add("  -> [警告] 无法识别: \"" + line + "\"");
                continue;
            }
            String key = cleanKey(kv[0]);
            String value = kv[1].trim();
            if (key.isEmpty()) continue;
            value = value.replaceAll("^\"(.*)\"$", "$1").replaceAll("^'(.*)'$", "$1");
            String resolved = resolveAlias(key);
            if (!resolved.equals(key))
                diag.add("  -> 同义词: \"" + key + "\" -> \"" + resolved + "\"");
            key = resolved;
            if (isKnownGlobal(key)) {
                globals.put(key, value);
                diag.add("  -> [全局] " + key + " = \"" + value + "\"");
            } else if (key.equals("分值")) {
                cur = new HashMap<String, String>();
                cur.put(key, value);
                blocks.add(cur);
                diag.add("  -> [新规则] 分值 = " + value);
            } else if (isKnownBlock(key)) {
                if (cur == null) {
                    cur = new HashMap<String, String>();
                    blocks.add(cur);
                }
                cur.put(key, value);
                diag.add("  -> [块内] " + key + " = \"" + value + "\"");
            } else {
                diag.add("  -> [跳过] 未知: \"" + key + "\"");
            }
        }
        cfgVer = safeStr(globals.get("版本号"));
        cfgCdkObj = safeStr(globals.get("计分板"));
        cfgCyName = safeStr(globals.get("联控插件"));
        cfgLinkMode = safeStr(globals.get("联控模式"));
        cfgUpdateChannel = safeStr(globals.get("更新通道"));
        cfgNameBoard = safeStr(globals.get("记名板"));
        cfgAdminTeam = safeStr(globals.get("管理团队"));
        cfgAdminTag = safeStr(globals.get("管理标签"));
        cfgCouponBoard = cfgCdkObj;
        couponRules.clear();
        parseCouponRules(cleanLines);



        for (Map<String, String> block : blocks) {
            int scoreKey = safeInt(block.get("分值"));
            if (scoreKey <= 0) continue;
            ScoreAction sa = new ScoreAction();
            sa.deleteCode = "true".equals(block.get("_删除口令"));
            sa.recordName = "true".equals(block.get("_记名"));

            if ("true".equals(block.get("_经济盲盒"))) {
                ActionEntry e = new ActionEntry("经济盲盒");
                e.moneyMin = safeDouble(block.get("_经济盲盒_min"));
                e.moneyMax = safeDouble(block.get("_经济盲盒_max"));
                if (e.moneyMin <= 0) e.moneyMin = 100;
                if (e.moneyMax <= 0) e.moneyMax = e.moneyMin;
                e.decimalPlaces = safeInt(block.getOrDefault("_小数位", "-1"));
                e.rawText = "经济盲盒 " + fmtMoney(e.moneyMin) + "~" + fmtMoney(e.moneyMax);
                sa.actions.add(e);

            } else if ("true".equals(block.get("_联控盲盒"))) {
                ActionEntry e = new ActionEntry("联控盲盒");
                e.daysMin = safeInt(block.getOrDefault("_天数min", "7"));
                e.daysMax = safeInt(block.getOrDefault("_天数max", "90"));
                e.slotsMin = safeInt(block.getOrDefault("_格子min", "27"));
                e.slotsMax = safeInt(block.getOrDefault("_格子max", "136"));
                if (e.daysMin <= 0) e.daysMin = 7;
                if (e.slotsMin <= 0) e.slotsMin = 27;
                if (e.daysMax <= 0) e.daysMax = e.daysMin;
                if (e.slotsMax <= 0) e.slotsMax = e.slotsMin;
                if (e.daysMax < e.daysMin) e.daysMax = e.daysMin;
                if (e.slotsMax < e.slotsMin) e.slotsMax = e.slotsMin;
                e.rawText = "联控盲盒 " + e.daysMin + "~" + e.daysMax + "天 " + e.slotsMin + "~" + e.slotsMax + "格";
                sa.actions.add(e);

            } else if ("true".equals(block.get("_债券盲盒"))) {
                ActionEntry e = new ActionEntry("债券盲盒");
                e.bondMin = safeInt(block.getOrDefault("_债券盲盒_min", "1"));
                e.bondMax = safeInt(block.getOrDefault("_债券盲盒_max", "10"));
                if (e.bondMin <= 0) e.bondMin = 1;
                if (e.bondMax <= 0) e.bondMax = e.bondMin;
                if (e.bondMax < e.bondMin) e.bondMax = e.bondMin;
                e.rawText = "债券盲盒 " + e.bondMin + "~" + e.bondMax + "个";
                sa.actions.add(e);

            } else {
                if (block.containsKey("_发钱")) {
                    ActionEntry e = new ActionEntry("发钱");
                    e.money = safeDouble(block.get("_发钱"));
                    e.rawText = "发钱 $" + fmtMoney(e.money);
                    sa.actions.add(e);
                }
                if (block.containsKey("_发债券")) {
                    ActionEntry e = new ActionEntry("发债券");
                    e.money = safeDouble(block.get("_发债券"));
                    e.rawText = "发债券 " + (int) e.money;
                    sa.actions.add(e);
                }
                if (block.containsKey("_扣钱")) {
                    ActionEntry e = new ActionEntry("扣钱");
                    e.money = safeDouble(block.get("_扣钱"));
                    e.rawText = "扣钱 $" + fmtMoney(e.money);
                    sa.actions.add(e);
                }
                if (block.containsKey("_联控")) {
                    ActionEntry e = new ActionEntry("联控");
                    e.slots = safeInt(block.get("_格子数"));
                    e.days = safeInt(block.get("_天数"));
                    e.rawText = "联控 +" + e.slots + "格 " + e.days + "天";
                    sa.actions.add(e);
                }
            }

            if (block.containsKey("_小数位")) {
                int dp = safeInt(block.get("_小数位"));
                for (ActionEntry ae : sa.actions) ae.decimalPlaces = dp;
            }
            if (sa.deleteCode) {
                ActionEntry de = new ActionEntry("删除口令");
                de.rawText = "删除口令";
                sa.actions.add(de);
            }
            if (sa.actions.isEmpty() && !sa.recordName && !sa.deleteCode) continue;
            scoreMap.put(scoreKey, sa);
        }
        configLoaded = !cfgCdkObj.isEmpty();
    }

    private void parseCouponRules(List<String> lines) {
        couponRules.clear();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            // 去掉行内注释
            int hash = t.indexOf('#');
            int slash = t.indexOf("//");
            int cut = -1;
            if (hash >= 0 && slash >= 0)
                cut = Math.min(hash, slash);
            else if (hash >= 0) cut = hash;
            else if (slash >= 0) cut = slash;
            if (cut >= 0) t = t.substring(0, cut).trim();
            if (t.isEmpty()) continue;
            java.util.regex.Matcher m =
                    java.util.regex.Pattern
                            .compile("(\\d+)\\s*[=＝]\\s*(\\d+)")
                            .matcher(t);
            while (m.find()) {
                int s = Integer.parseInt(m.group(1));
                int d = Integer.parseInt(m.group(2));
                if (s > 0 && d > 0) {
                    couponRules.put(s, d);
                }
            }
        }
    }


    private void parseNaturalAction(Map<String, String> block, String text, List<String> diag) {
        text = text.replaceAll("^['\"](.*)['\"]$", "$1").trim();
        text = normalizeText(text);
        log("[SDF1-诊断-动作] 输入: [" + text + "]");
        text = text.replace('－', '-').replace('–', '-').replace('—', '-').replace('～', '~');

        String norm = normalizeAllDigits(text);
        norm = normalizeEnglishNumbers(norm);
        norm = normalizeRomanNumerals(norm);
        norm = normalizeMixedNumbers(norm);
        norm = normalizeChineseNumbers(norm);
        log("[SDF1-诊断-动作] 归一化: [" + norm + "]");

        // ★ 直接中文数字范围检测（归一化之前的安全网）
        // 处理"壹佰到贰仟个债券盲盒"等纯中文数字范围
        if (norm.contains("债券")) {
            String cnBondRange = "([零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟两]+)"
                    + "\\s*[~\\-到至]+\\s*"
                    + "([零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟两]+)"
                    + "\\s*个?\\s*债券盲盒";
            Matcher cnRangeM = Pattern.compile(cnBondRange).matcher(norm);
            if (cnRangeM.find()) {
                int bmin = parseChineseNum(cnRangeM.group(1));
                int bmax = parseChineseNum(cnRangeM.group(2));
                if (bmin > 0 && bmax > 0) {
                    if (bmin > bmax) { int t2 = bmin; bmin = bmax; bmax = t2; }
                    block.put("_债券盲盒", "true");
                    block.put("_债券盲盒_min", String.valueOf(bmin));
                    block.put("_债券盲盒_max", String.valueOf(bmax));
                    diag.add("    -> 识别(中文范围): 债券盲盒 " + bmin + "~" + bmax);
                }
            }
            // 也检测"债券盲盒 壹佰到贰仟个"（关键词在前的格式）
            if (!"true".equals(block.get("_债券盲盒"))) {
                String cnBondRange2 = "债券盲盒\\s*"
                        + "([零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟两]+)"
                        + "\\s*[~\\-到至]+\\s*"
                        + "([零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟两]+)"
                        + "\\s*个?";
                Matcher cnRangeM2 = Pattern.compile(cnBondRange2).matcher(norm);
                if (cnRangeM2.find()) {
                    int bmin = parseChineseNum(cnRangeM2.group(1));
                    int bmax = parseChineseNum(cnRangeM2.group(2));
                    if (bmin > 0 && bmax > 0) {
                        if (bmin > bmax) { int t2 = bmin; bmin = bmax; bmax = t2; }
                        block.put("_债券盲盒", "true");
                        block.put("_债券盲盒_min", String.valueOf(bmin));
                        block.put("_债券盲盒_max", String.valueOf(bmax));
                        diag.add("    -> 识别(中文范围): 债券盲盒 " + bmin + "~" + bmax);
                    }
                }
            }
        }

        // ★ 债券检测（含债券盲盒）
        if (!"true".equals(block.get("_债券盲盒")) && norm.contains("债券")) {
            // 先检测范围：X~Y个债券盲盒
            Matcher bondBlindM = Pattern.compile(
                    "(\\d+)\\s*[~\\-到至]+\\s*(\\d+)\\s*个?\\s*债券盲盒"
            ).matcher(norm);
            if (bondBlindM.find()) {
                int bmin = Integer.parseInt(bondBlindM.group(1));
                int bmax = Integer.parseInt(bondBlindM.group(2));
                if (bmin > bmax) {
                    int t2 = bmin;
                    bmin = bmax;
                    bmax = t2;
                }
                block.put("_债券盲盒", "true");
                block.put("_债券盲盒_min", String.valueOf(bmin));
                block.put("_债券盲盒_max", String.valueOf(bmax));
                diag.add("    -> 识别: 债券盲盒 " + bmin + "~" + bmax);
            } else if (norm.contains("盲盒") || norm.contains("抽")) {
                // 盲盒关键词 + X~Y个债券
                Matcher bondR = Pattern.compile(
                        "(\\d+)\\s*[~\\-到至]+\\s*(\\d+)\\s*个?\\s*债券"
                ).matcher(norm);
                if (bondR.find()) {
                    int bmin = Integer.parseInt(bondR.group(1));
                    int bmax = Integer.parseInt(bondR.group(2));
                    if (bmin > bmax) {
                        int t2 = bmin;
                        bmin = bmax;
                        bmax = t2;
                    }
                    block.put("_债券盲盒", "true");
                    block.put("_债券盲盒_min", String.valueOf(bmin));
                    block.put("_债券盲盒_max", String.valueOf(bmax));
                    diag.add("    -> 识别: 债券盲盒 " + bmin + "~" + bmax);
                }
            }
            // 固定发债券（非盲盒）
            if (!"true".equals(block.get("_债券盲盒"))) {
                Matcher bondM = Pattern.compile("(\\d+)\\s*[个块枚元]?" + "\\s*债券").matcher(norm);
                if (bondM.find()) {
                    int bondAmt = Integer.parseInt(bondM.group(1));
                    block.put("_发债券", String.valueOf(bondAmt));
                    diag.add("    -> 识别: 发债券 " + bondAmt);
                }
            }
        }

        // ★ 删除口令检测
        if (norm.contains("删") || norm.contains("删除") || norm.contains("销毁")
                || norm.contains("作废") || norm.contains("清除") || norm.contains("清空")
                || norm.contains("抹除") || norm.contains("删掉") || norm.contains("移除")
                || norm.contains("废弃") || norm.contains("失效") || norm.contains("吊销")
                || norm.contains("注销") || norm.contains("回收") || norm.contains("用掉")
                || norm.contains("消耗") || norm.contains("核销") || norm.contains("废除")) {
            block.put("_删除口令", "true");
            diag.add("    -> 识别: 删除口令");
        }

        // ★ 记名检测
        if (norm.contains("记名") || norm.contains("永久一次性") || norm.contains("记录")
                || norm.contains("标记已用") || norm.contains("防重复") || norm.contains("查重")
                || norm.contains("已领") || norm.contains("已兑换") || norm.contains("去重")) {
            block.put("_记名", "true");
            diag.add("    -> 识别: 记名");
        }

        // ★ 小数位检测
        Matcher decMatch = Pattern.compile("(\\d+)\\s*位\\s*小数").matcher(norm);
        if (decMatch.find()) {
            block.put("_小数位", decMatch.group(1));
            diag.add("    -> 识别: 小数位=" + decMatch.group(1));
        }

        // ★ 盲盒检测
        if (norm.contains("盲盒") || norm.contains("抽")) {

            // ★★ 新增：优先检测债券盲盒
            if (norm.contains("债券")) {
                java.util.List<Integer> bnums =
                        new java.util.ArrayList<>();
                Matcher bnm = Pattern.compile(
                        "(\\d+)").matcher(norm);
                while (bnm.find()) {
                    bnums.add(Integer.parseInt(
                            bnm.group(1)));
                }
                if (!bnums.isEmpty()) {
                    int bmin = java.util.Collections
                            .min(bnums);
                    int bmax = java.util.Collections
                            .max(bnums);
                    block.put("_债券盲盒", "true");
                    block.put("_债券盲盒_min",
                            String.valueOf(bmin));
                    block.put("_债券盲盒_max",
                            String.valueOf(bmax));
                    diag.add("    -> 识别: 债券盲盒 "
                            + bmin + "~" + bmax);
                    return;
                }
            }

            // 原有经济盲盒
            double[] mr = extractMoneyRange(norm);
            if (mr != null) {
                block.put("_经济盲盒", "true");
                block.put("_经济盲盒_min",
                        String.valueOf(mr[0]));
                block.put("_经济盲盒_max",
                        String.valueOf(mr[1]));
                diag.add("    -> 识别: 经济盲盒 "
                        + mr[0] + "~" + mr[1]);
                return;
            }
            int[] sr = extractSlotDayRange(norm);
            if (sr != null) {
                block.put("_联控盲盒", "true");
                block.put("_天数min",
                        String.valueOf(sr[0]));
                block.put("_格子min",
                        String.valueOf(sr[1]));
                block.put("_天数max",
                        String.valueOf(sr[2]));
                block.put("_格子max",
                        String.valueOf(sr[3]));
                diag.add("    -> 识别: 联控盲盒 天数="
                        + sr[0] + "~" + sr[2]
                        + " 格子=" + sr[1]
                        + "~" + sr[3]);
                return;
            }
            int[] dayR = extractDayRange(norm);
            int[] slotR = extractSlotRange(norm);
            if (dayR != null || slotR != null) {
                block.put("_联控盲盒", "true");
                if (dayR != null) {
                    block.put("_天数min",
                            String.valueOf(dayR[0]));
                    block.put("_天数max",
                            String.valueOf(dayR[1]));
                }
                if (slotR != null) {
                    block.put("_格子min",
                            String.valueOf(slotR[0]));
                    block.put("_格子max",
                            String.valueOf(slotR[1]));
                }
                return;
            }
            boolean hasCyKw = norm.contains("格")
                    || norm.contains("天")
                    || norm.contains("背包")
                    || norm.contains("空间")
                    || norm.contains("会员")
                    || norm.contains("激活")
                    || norm.contains("存储");
            boolean hasMoneyKw = norm.contains("给")
                    || norm.contains("钱")
                    || norm.contains("块")
                    || norm.contains("元")
                    || norm.contains("奖")
                    || norm.contains("金额")
                    || norm.contains("金");
            if (hasCyKw && !hasMoneyKw) {
                block.put("_联控盲盒", "true");
                diag.add("    -> 识别: 联控盲盒(关键词)");
            } else {
                block.put("_经济盲盒", "true");
                diag.add("    -> 识别: 经济盲盒(默认)");
            }
            return;
        }


        // ★ 非盲盒：联控检测
        boolean hasSlotKw = norm.contains("背包") || norm.contains("空间")
                || norm.contains("格") || norm.contains("激活") || norm.contains("会员")
                || norm.contains("存储");
        if (hasSlotKw) {
            int[] sd = extractSlotDay(norm);
            if (sd != null) {
                block.put("_联控", "true");
                block.put("_格子数", String.valueOf(sd[1]));
                block.put("_天数", String.valueOf(sd[0]));
                diag.add("    -> 识别: 联控 " + sd[0] + "天" + sd[1] + "格");
            }
        }

        // ★ 扣钱检测
        boolean isTake = norm.contains("扣") || norm.contains("减") || norm.contains("扣除")
                || norm.contains("罚") || norm.contains("没收");
        if (isTake) {
            double amt = extractMoneyExcludingSlots(norm);
            if (amt > 0) {
                block.put("_扣钱", String.valueOf(amt));
                diag.add("    -> 识别: 扣钱 " + amt);
            }
        }

        // ★ 发钱检测（排除债券、排除扣钱）
        boolean isGive = !isTake && !norm.contains("债券")
                && (norm.contains("给") || norm.contains("奖")
                || norm.contains("发") || norm.contains("加")
                || norm.contains("赏") || norm.contains("赠送")
                || norm.contains("返"));
        if (isGive) {
            double amt = extractMoneyExcludingSlots(norm);
            if (amt > 0) {
                block.put("_发钱", String.valueOf(amt));
                diag.add("    -> 识别: 发钱 " + amt);
            }
        }
    }
    private String safeStr(String s) { return s == null ? "" : s.trim(); }

    private int safeInt(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        s = s.trim();
        try { return Integer.parseInt(s); } catch (Exception e) {}
        // ★ 中文数字统一用 parseChineseNum 解析（支持万/亿分节）
        return parseChineseNum(s);
    }

    private String normalizeAllDigits(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '０' && c <= '９') {
                sb.append((char)(c - 0xFF10 + '0'));
            } else if (c == '\uFF0E') {
                sb.append('.');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String normalizeMixedNumbers(String text) {
        // ★ 匹配数字+中文单位（包括繁体：佰仟）
        Matcher m1 = Pattern.compile("(\\d+)([万亿])(\\d+)?([千万百十佰仟])?").matcher(text);
        StringBuffer sb1 = new StringBuffer();
        while (m1.find()) {
            int num = Integer.parseInt(m1.group(1));
            Integer unit1 = CN_NUMS.get(m1.group(2));
            long result = (long) num * (unit1 != null ? unit1 : 1);
            if (m1.group(3) != null) {
                int num2 = Integer.parseInt(m1.group(3));
                Integer unit2 = CN_NUMS.get(m1.group(4));
                result += (long) num2 * (unit2 != null ? unit2 : 1);
            }
            m1.appendReplacement(sb1, String.valueOf(result));
        }
        m1.appendTail(sb1);
        // ★ 匹配数字+中文单位（包括繁体：佰仟）
        Matcher m2 = Pattern.compile("(\\d+)([千万百十佰仟])").matcher(sb1.toString());
        StringBuffer sb2 = new StringBuffer();
        while (m2.find()) {
            int num = Integer.parseInt(m2.group(1));
            Integer unitVal = CN_NUMS.get(m2.group(2));
            if (unitVal != null && unitVal > 0) {
                m2.appendReplacement(sb2, String.valueOf(num * unitVal));
            }
        }
        m2.appendTail(sb2);
        return sb2.toString();
    }

    private String normalizeChineseNumbers(String text) {
        // ★ 预处理：移除中文数字之间的不可见字符（零宽空格等）
        // 防止"壹佰"被拆分为"壹"和"佰"导致解析为1100
        text = text.replaceAll(
                "(?<=[零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟两])"
                + "[\\s\\u00A0\\u200B\\u200C\\u200D\\u2060\\uFE0F\\u180E\\u2028\\u2029\\u3000\\u00AD]*"
                + "(?=[零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟两])",
                ""
        );
        Matcher m = Pattern.compile(
                "[零一二三四五六七八九十百千万亿壹贰叁肆伍陆柒捌玖拾佰仟两]+"
        ).matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int val = parseChineseNum(m.group());
            if (val > 0) {
                m.appendReplacement(sb, String.valueOf(val));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String normalizeEnglishNumbers(String text) {
        Matcher m = Pattern.compile(
                "(?i)\\b(zero|one|two|three|four|five|six|seven|eight|nine|ten|"
                        + "eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|"
                        + "eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|"
                        + "eighty|ninety|hundred|thousand|million|billion)"
                        + "(?:[\\s-]+(?:zero|one|two|three|four|five|six|seven|eight|nine|ten|"
                        + "eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|"
                        + "eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|"
                        + "eighty|ninety|hundred|thousand|million|billion))*\\b"
        ).matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int val = parseEnglishNum(m.group());
            if (val > 0) {
                m.appendReplacement(sb, String.valueOf(val));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String normalizeRomanNumerals(String text) {
        Matcher m = Pattern.compile("(?i)\\b([IVXLCDM]+)\\b").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String matched = m.group(1);
            if (matched.isEmpty()) continue;
            if (matched.matches("(?i)M{0,3}(?:CM|CD|D?C{0,3})(?:XC|XL|L?X{0,3})(?:IX|IV|V?I{0,3})")) {
                int val = parseRomanNum(matched);
                if (val > 0) {
                    m.appendReplacement(sb, String.valueOf(val));
                }
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Charset detectEncoding(File f) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            if (isValidUtf8(bytes)) return StandardCharsets.UTF_8;
            log("[SDF1] 文件非UTF-8，使用GBK编码");
        } catch (Exception e) {}
        try { return Charset.forName("GBK"); } catch (Exception e) {}
        return StandardCharsets.UTF_8;
    }

    private boolean isValidUtf8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int b = bytes[i] & 0xFF;
            if (b <= 0x7F) { i++; continue; }
            int len;
            if ((b & 0xE0) == 0xC0) len = 2;
            else if ((b & 0xF0) == 0xE0) len = 3;
            else if ((b & 0xF8) == 0xF0) len = 4;
            else return false;
            for (int j = 1; j < len; j++) {
                if (i + j >= bytes.length) return false;
                if ((bytes[i + j] & 0xC0) != 0x80) return false;
            }
            i += len;
        }
        return true;
    }

    private String normalizeText(String s) {
        if (s == null) return "";
        return s.replace('－', '-').replace('–', '-').replace('—', '-')
                .replace('−', '-').replace('～', '~')
                .replace("​", "").replace("﻿", "")
                // ★ 清理更多不可见 Unicode 字符
                .replaceAll("[\\u200B\\u200C\\u200D\\u2060\\uFE0F\\u180E"
                        + "\\u2028\\u2029\\u00AD\\u00A0]", "");
    }

    private int parseChineseNum(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.trim();
        try { return Integer.parseInt(s); } catch (Exception e) {}
        int result = 0, section = 0, current = 0;
        for (int i = 0; i < s.length(); i++) {
            Integer val = CN_NUMS.get(s.substring(i, i + 1));
            if (val == null) continue;
            if (val >= 100000000) {
                result = (result + section + current) * val;
                section = 0; current = 0;
            } else if (val >= 10000) {
                section = (section + current) * val;
                current = 0;
            } else if (val >= 10) {
                if (current == 0) current = 1;
                current *= val;
            } else {
                if (current > 0 && current >= 10) {
                    section += current;
                    current = val;
                } else {
                    current = val;
                }
            }
        }
        return result + section + current;
    }

    private int parseRomanNum(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.toUpperCase().trim();
        Map<Character, Integer> v = new HashMap<Character, Integer>();
        v.put('I', 1); v.put('V', 5); v.put('X', 10); v.put('L', 50);
        v.put('C', 100); v.put('D', 500); v.put('M', 1000);
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            Integer cur = v.get(s.charAt(i));
            if (cur == null) return 0;
            Integer nxt = (i + 1 < s.length()) ? v.get(s.charAt(i + 1)) : null;
            if (nxt != null && cur < nxt) result -= cur;
            else result += cur;
        }
        return result;
    }

    private int parseEnglishNum(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.toLowerCase().trim().replaceAll("[\\s-]+", " ");
        Map<String, Integer> w = new HashMap<String, Integer>();
        w.put("zero",0); w.put("one",1); w.put("two",2); w.put("three",3);
        w.put("four",4); w.put("five",5); w.put("six",6); w.put("seven",7);
        w.put("eight",8); w.put("nine",9); w.put("ten",10);
        w.put("eleven",11); w.put("twelve",12); w.put("thirteen",13);
        w.put("fourteen",14); w.put("fifteen",15); w.put("sixteen",16);
        w.put("seventeen",17); w.put("eighteen",18); w.put("nineteen",19);
        w.put("twenty",20); w.put("thirty",30); w.put("forty",40);
        w.put("fifty",50); w.put("sixty",60); w.put("seventy",70);
        w.put("eighty",80); w.put("ninety",90);
        w.put("hundred",100); w.put("thousand",1000);
        w.put("million",1000000); w.put("billion",1000000000);
        int result = 0, current = 0;
        for (String word : s.split(" ")) {
            Integer val = w.get(word);
            if (val == null) continue;
            if (val >= 1000) { result = (result + current) * val; current = 0; }
            else if (val >= 100) { current = (current == 0 ? 1 : current) * val; }
            else { current += val; }
        }
        return result + current;
    }

    private double safeDouble(String s) {
        try { return Double.parseDouble(s == null ? "0" : s.trim()); }
        catch (Exception e) { return safeInt(s); }
    }

    private String fmtMoney(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.format("%.2f", v);
    }

    private String fmtAmount(double v, int dp) {
        if (dp >= 0) return String.format("%." + dp + "f", v);
        return v == (long) v ? String.valueOf((long) v) : String.format("%.2f", v);
    }

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
            if (c == ':' || c == '=' || c == '\uff1a' || c == '\uff1d') {
                String k = line.substring(0, i).trim()
                        .replaceAll("[\\[\\]{}()（）【】]", "").trim();
                String v = line.substring(i + 1).trim();
                if (!k.isEmpty()) return new String[]{k, v};
                break;
            }
        }
        return null;
    }

    private String cleanKey(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.trim().replaceAll("^[\\d]+[.]+", "").trim()
                .replaceAll("[\\[\\]{}()（）【】]", "").trim();
    }

    private int getIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 4;
            else break;
        }
        return count;
    }

    private String resolveAlias(String key) {
        String lower = key.toLowerCase().trim();
        String canonical = ALIASES.get(lower);
        if (canonical != null) return canonical;
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return key;
    }

    private boolean isKnownGlobal(String key) {
        if (key.equals("计分板")) return true;
        if (key.equals("记名板")) return true;
        if (key.equals("联控插件")) return true;
        if (key.equals("版本号")) return true;
        if (key.equals("更新通道")) return true;
        if (key.equals("联控模式")) return true;
       // if (key.equals("管理团队")) return true;
        if (key.equals("管理标签")) return true;
        if (key.equals("优惠券规则")) return true;
        return false;
    }

    private boolean isKnownBlock(String key) {
        return key.equals("分值") || key.equals("动作") || key.equals("类型")
                || key.equals("格子数") || key.equals("天数") || key.equals("金额");
    }

    private double parseMoneyWithUnit(String numStr, String unitStr) {
        double num = Double.parseDouble(numStr);
        if (unitStr == null || unitStr.isEmpty()) return num;
        if (unitStr.equals("千")) return num * 1000;
        if (unitStr.equals("万")) return num * 10000;
        if (unitStr.equals("十万")) return num * 100000;
        if (unitStr.equals("百万")) return num * 1000000;
        return num;
    }

    private double[] extractMoneyRange(String text) {
        text = normalizeText(text);
        text = normalizeAllDigits(text);
        text = normalizeEnglishNumbers(text);
        text = normalizeRomanNumerals(text);
        text = normalizeMixedNumbers(text);
        text = normalizeChineseNumbers(text);
        Matcher m = Pattern.compile(
                "([\\d.]+)\\s*(元|块|千|万|十万|百万)?"
                        + "\\s*[~\\-到至]+\\s*"
                        + "([\\d.]+)\\s*(元|块|千|万|十万|百万)?"
        ).matcher(text);
        if (m.find()) {
            if (m.end() < text.length()) {
                String after = text.substring(m.end()).trim();
                if (after.startsWith("天") || after.startsWith("格"))
                    return null;
            }
            double min = parseMoneyWithUnit(m.group(1), m.group(2));
            double max = parseMoneyWithUnit(m.group(3), m.group(4));
            if (min > 0 && max > 0) {
                if (min > max) { double t = min; min = max; max = t; }
                return new double[]{min, max};
            }
        }
        return null;
    }

    // ★ 修复：中文数字归一化后再匹配金额
    private double extractMoneyExcludingSlots(String text) {
        String[] parts = text.split("[，,、；;。]");
        double max = 0;
        for (String part : parts) {
            part = part.trim();
            if (part.contains("格") || part.contains("天") || part.contains("空间")
                    || part.contains("背包") || part.contains("激活")) continue;
            part = normalizeAllDigits(part);
            part = normalizeEnglishNumbers(part);
            part = normalizeRomanNumerals(part);
            part = normalizeMixedNumbers(part);
            part = normalizeChineseNumbers(part);
            double[] range = extractMoneyRange(part);
            if (range != null) {
                double avg = (range[0] + range[1]) / 2;
                if (avg > max) max = avg;
                continue;
            }
            Matcher m = Pattern.compile("([\\d.]+)\\s*(元|块|千|万|十万|百万)?").matcher(part);
            while (m.find()) {
                double a = parseMoneyWithUnit(m.group(1), m.group(2));
                if (a > max) max = a;
            }
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
        Matcher m = Pattern.compile(
                "(\\d+)\\s*天\\s*(\\d+)\\s*格\\s*[~\\-到至]+\\s*(\\d+)\\s*天\\s*(\\d+)\\s*格"
        ).matcher(text);
        if (m.find()) return new int[]{
                Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))};
        return null;
    }

    private int[] extractDayRange(String text) {
        Matcher m = Pattern.compile("(\\d+)\\s*天\\s*[~\\-到至]+\\s*(\\d+)\\s*天").matcher(text);
        if (m.find()) {
            int a = Integer.parseInt(m.group(1)); int b = Integer.parseInt(m.group(2));
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        Matcher m2 = Pattern.compile("(\\d+)\\s*[~\\-到至]+\\s*(\\d+)\\s*天").matcher(text);
        if (m2.find()) {
            int a = Integer.parseInt(m2.group(1)); int b = Integer.parseInt(m2.group(2));
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        return null;
    }

    private int[] extractSlotRange(String text) {
        Matcher m = Pattern.compile("(\\d+)\\s*格\\s*[~\\-到至]+\\s*(\\d+)\\s*格").matcher(text);
        if (m.find()) {
            int a = Integer.parseInt(m.group(1)); int b = Integer.parseInt(m.group(2));
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        Matcher m2 = Pattern.compile("(\\d+)\\s*[~\\-到至]+\\s*(\\d+)\\s*格").matcher(text);
        if (m2.find()) {
            int a = Integer.parseInt(m2.group(1)); int b = Integer.parseInt(m2.group(2));
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        return null;
    }

    private List<String> stripAllComments(List<String> rawLines, List<String> diag) {
        List<String> result = new ArrayList<String>();
        boolean inBlock = false; boolean inHtml = false;
        int totalComments = 0; int totalInline = 0;
        diag.add("===== 逐行诊断 =====");
        diag.add("总行数: " + rawLines.size());
        for (int i = 0; i < rawLines.size(); i++) {
            String raw = rawLines.get(i);
            String trimmed = raw.trim();
            String ln = String.format("L%02d", i + 1);
            if (trimmed.isEmpty()) { diag.add(ln + ": (空行)"); result.add(""); continue; }
        /*    if (inBlock) {
                int endIdx = trimmed.indexOf("*///");
            /*    if (endIdx >= 0) { inBlock = false; String after = trimmed.substring(endIdx + 2).trim(); result.add(after.isEmpty() ? "" : after); }
                else { result.add(""); }
                diag.add(ln + ": [踢除块注释中]"); totalComments++; continue;
            }
            if (inHtml) {
                int endIdx = trimmed.indexOf("-->");
                if (endIdx >= 0) { inHtml = false; String after = trimmed.substring(endIdx + 3).trim(); result.add(after.isEmpty() ? "" : after); }
                else { result.add(""); }
                diag.add(ln + ": [踢除HTML注释中]"); totalComments++; continue;
            }*/
       /*     if (trimmed.contains("/*")) {
           /*     int bs = trimmed.indexOf("/*"); int be = trimmed.indexOf("*///", bs + 2);
             /*   if (be >= 0) { String b = trimmed.substring(0, bs).trim(); String a = trimmed.substring(be + 2).trim(); result.add((b + " " + a).trim()); }
                else { inBlock = true; String b = trimmed.substring(0, bs).trim(); result.add(b.isEmpty() ? "" : b); }
                diag.add(ln + ": [踢除块注释]"); totalComments++; continue;
            }*/
           /* if (trimmed.contains("<!--")) {
                int hs = trimmed.indexOf("<!--"); int he = trimmed.indexOf("-->", hs + 4);
                if (he >= 0) { inHtml = false; String b = trimmed.substring(0, hs).trim(); String a = trimmed.substring(he + 3).trim(); result.add((b + " " + a).trim()); }
                else { inHtml = true; String b = trimmed.substring(0, hs).trim(); result.add(b.isEmpty() ? "" : b); }
                diag.add(ln + ": [踢除HTML注释]"); totalComments++; continue;
            }
            if (trimmed.startsWith("#") || trimmed.startsWith("//")) {
                diag.add(ln + ": [踢除整行注释]"); result.add(""); totalComments++; continue;
            }*/
            int hashIdx = findUnquoted(trimmed, '#');
            if (hashIdx >= 0) { trimmed = trimmed.substring(0, hashIdx).trim(); totalInline++; }
            int slashIdx = findUnquoted(trimmed, '/');
            if (slashIdx >= 0 && slashIdx + 1 < trimmed.length() && trimmed.charAt(slashIdx + 1) == '/') {
                trimmed = trimmed.substring(0, slashIdx).trim(); totalInline++;
            }
      //      diag.add(ln + ": [保留] \"" + trimmed + "\"");
            result.add(trimmed);
        }
        diag.add("总行: " + rawLines.size() + " | 注释: " + totalComments + " | 有效行: " + result.size());
        return result;
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { log("[Vault] 未找到"); return; }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) { economy = rsp.getProvider(); log("[Vault] 已连接: " + economy.getName()); }
        else { log("[Vault] 未找到经济提供者"); }
    }

    private int lookupScore(String code) {
        if (!configLoaded || cfgCdkObj.isEmpty()) return -1;
        try {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective obj = board.getObjective(cfgCdkObj);
            if (obj == null) {
                if (!cdksWarned) { log("[计分板] 找不到: " + cfgCdkObj); cdksWarned = true; }
                return -1;
            }
            cdksWarned = false;

            // 第一轮：精确匹配
            Score s = obj.getScore(code);
            if (s.isScoreSet()) return s.getScore();

            // 第二轮：模糊匹配 —— 去分隔符 + 统一大小写
            String normInput = normalizeForCompare(code);
            for (String entry : board.getEntries()) {
                Score sc = obj.getScore(entry);
                if (sc.isScoreSet()) {
                    if (normalizeForCompare(entry).equals(normInput)) {
                        log("[模糊匹配] \"" + code + "\" -> 标准码 \""
                                + entry + "\" 分值=" + sc.getScore());
                        return sc.getScore();
                    }
                }
            }
            return -1;
        } catch (Exception e) { return -1; }
    }

    /**
     * 查找口令，返回 [分值, 标准码]
     * 玩家输入 aaabbbbcccc → 标准码 AAAA-BBBB-CCCC
     * 如果精确匹配就用原码，模糊匹配就返回计分板上的原始条目
     */
    private Object[] lookupScoreAndCanonical(String code) {
        if (!configLoaded || cfgCdkObj.isEmpty())
            return new Object[]{ -1, code };
        try {
            Scoreboard board =
                    Bukkit.getScoreboardManager().getMainScoreboard();
            Objective obj = board.getObjective(cfgCdkObj);
            if (obj == null) {
                if (!cdksWarned) {
                    log("[计分板] 找不到: " + cfgCdkObj);
                    cdksWarned = true;
                }
                return new Object[]{ -1, code };
            }
            cdksWarned = false;

            // 第一轮：精确匹配
            Score s = obj.getScore(code);
            if (s.isScoreSet()) {
                return new Object[]{ s.getScore(), code };
            }

            // 第二轮：模糊匹配
            String normInput = normalizeForCompare(code);
            for (String entry : board.getEntries()) {
                Score sc = obj.getScore(entry);
                if (sc.isScoreSet()) {
                    if (normalizeForCompare(entry).equals(normInput)) {
                        log("[模糊匹配] \"" + code
                                + "\" -> 标准码 \"" + entry
                                + "\" 分值=" + sc.getScore());
                        return new Object[]{ sc.getScore(), entry };
                    }
                }
            }

            return new Object[]{ -1, code };
        } catch (Exception e) {
            return new Object[]{ -1, code };
        }
    }
    /**
     * 去除所有分隔符，统一转小写
     * "AAAA-BBBB-CCCC" → "aaaabbbbcccc"
     * "aaabbbbcccc"    → "aaaabbbbcccc"
     * "aAa-BbBb-CcCc"  → "aaaabbbbcccc"
     * "AAAA BB BB CC CC" → "aaaabbbbcccc"
     */
    private String normalizeForCompare(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // 全角→半角（数字）
            if (c >= '\uFF10' && c <= '\uFF19') {
                sb.append((char)(c - 0xFF10 + '0'));
            }
            // 全角→半角（大写字母）
            else if (c >= '\uFF21' && c <= '\uFF3A') {
                sb.append((char)(c - 0xFF21 + 'A'));
            }
            // 全角→半角（小写字母）
            else if (c >= '\uFF41' && c <= '\uFF5A') {
                sb.append((char)(c - 0xFF41 + 'a'));
            }
            // ★ 保留中文
            else if ((c >= '\u4E00' && c <= '\u9FFF')
                    || (c >= '\u3400' && c <= '\u4DBF')) {
                sb.append(c);
            }
            // 保留半角字母和数字
            else if ((c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
            // 分隔符丢弃
        }
        return sb.toString().toLowerCase();
    }



    private void consumeCode(String code, boolean deleteCode) {
        if (!deleteCode) return;
        log("[删除] 口令: \"" + code + "\" 目标: " + cfgCdkObj);
        try {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective obj = board.getObjective(cfgCdkObj);
            if (obj != null) {
                Score sc = obj.getScore(code);
                if (sc.isScoreSet()) {
                    boolean removed = false;
                    try { java.lang.reflect.Method m = obj.getClass().getMethod("resetScore", String.class); m.invoke(obj, code); removed = true; } catch (Throwable ignored) {}
                    if (!removed) try { java.lang.reflect.Method m = board.getClass().getMethod("resetScore", String.class); m.invoke(board, code); removed = true; } catch (Throwable ignored) {}
                    if (!removed) { sc.setScore(Integer.MIN_VALUE); removed = true; }
                    log("[删除] API结果: " + removed);
                }
            }
        } catch (Exception e) { log("[删除] API异常: " + e.getMessage()); }
        fallbackDelete(code);
    }

    private void fallbackDelete(final String code) {
        Bukkit.getScheduler().runTask(this, new Runnable() {
            public void run() {
                String safeCode = code.replace("\"", "\\\"");
                String cmd = "scoreboard players reset " + safeCode + " " + cfgCdkObj;
                log("[删除] 兜底: " + cmd);
                try { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd); }
                catch (Exception e) { log("[删除] 兜底失败: " + e.getMessage()); }
            }
        });
    }

    private boolean checkAdminSilent(CommandSender s) {
        if (s.isOp()) return true;
        if (s.hasPermission("sdf1.admin")) return true;
        if (!(s instanceof Player)) return false;
        Player p = (Player) s;
        if (!cfgAdminTag.isEmpty() && p.getScoreboardTags().contains(cfgAdminTag)) return true;
        if (!cfgAdminTeam.isEmpty()) {
            try { org.bukkit.scoreboard.Team team = p.getScoreboard().getTeam(cfgAdminTeam); if (team != null && team.hasEntry(p.getName())) return true; } catch (Exception ignored) {}
        }
        return false;
    }
    private void startListening(Player p) {
        listening.put(p.getUniqueId(), System.currentTimeMillis());
        p.sendMessage(colorize("&a[SDF1] &f已开启口令监听 (15秒)"));
        final UUID uuid = p.getUniqueId();
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            public void run() {
                if (listening.containsKey(uuid)) {
                    stopListening(uuid, false);
                }
            }
        }, 300L);
    }


    private void stopListening(UUID uuid, boolean silent) {
        listening.remove(uuid);
        if (!silent) { Player pl = Bukkit.getPlayer(uuid); if (pl != null && pl.isOnline()) pl.sendMessage(colorize("&e[SDF1] 监听已关闭")); }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        UUID u = p.getUniqueId();
        String msg = event.getMessage().trim();
        if (msg.isEmpty() || !listening.containsKey(u)) return;
        event.setCancelled(true);
        stopListening(u, true);
        p.sendMessage(colorize("&a[SDF1] &f已拦截，比对中..."));
        Long last = chatCd.get(u);
        if (last != null && System.currentTimeMillis() - last < 500) return;
        chatCd.put(u, System.currentTimeMillis());

        // ★ 优先检查债券CDK
        if (tryBondRedeem(p, msg)) {
            return;
        }

        int scoreVal = lookupScore(msg);
        if (scoreVal < 0) {
            p.sendMessage(colorize("&c[SDF1] 口令无效"));
            log("[拦截] " + p.getName() + " 无效: \"" + msg + "\"");
            return;
        }
        ScoreAction sa = scoreMap.get(scoreVal);
        if (sa == null) {
            p.sendMessage(colorize("&c[SDF1] 规则未配置"));
            log("[拦截] " + p.getName() + " 分值=" + scoreVal + " 无规则");
            return;
        }
        log("[拦截] " + p.getName() + " 分值=" + scoreVal + " 动作数=" + sa.actions.size()
                + (sa.recordName ? " 记名" : "") + (sa.deleteCode ? " 删口令" : ""));

        // 记名检查
        if (sa.recordName && cfgNameBoard != null && !cfgNameBoard.isEmpty()) {
            String nameKey = msg + "_" + p.getName();
            try {
                Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective nameObj = board.getObjective(cfgNameBoard);
                if (nameObj != null) {
                    Score ns = nameObj.getScore(nameKey);
                    if (ns.isScoreSet() && ns.getScore() > 0) {
                        p.sendMessage(colorize("&c[SDF1] 你已领取过此口令"));
                        log("[记名] " + p.getName() + " 重复: " + nameKey);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        p.sendMessage(colorize("&a[SDF1] 执行: " + sa.actions.size() + "个动作"));

        // ===== 快照：执行前的经济/债券 =====
        double econBefore = 0;
        if (economy != null) econBefore = economy.getBalance(p);
        int bondBefore = 0;
        Connection bdb = getBondDb();
        if (bdb != null) {
            try {
                PreparedStatement q = bdb.prepareStatement(
                        "SELECT amount FROM bonds WHERE player_name=?");
                q.setString(1, p.getName());
                ResultSet rs = q.executeQuery();
                if (rs.next()) bondBefore = rs.getInt("amount");
                rs.close();
                q.close();
            } catch (Exception ignored) {}
        }

        // ===== 执行动作（静默） =====
        boolean hasEcon = false;
        boolean hasBond = false;
        boolean hasSlots = false;
        String econMsg = "";
        String bondMsg = "";
        String slotsMsg = "";

        for (ActionEntry ae : sa.actions) {
            log("[执行] [" + ae.type + "] " + ae.rawText);

            if ("发钱".equals(ae.type)) {
                if (economy != null) {
                    economy.depositPlayer(p, ae.money);
                    hasEcon = true;
                    econMsg = "+$" + fmtAmount(ae.money, ae.decimalPlaces);
                }
            } else if ("扣钱".equals(ae.type)) {
                if (economy != null) {
                    double amt = Math.abs(ae.money);
                    economy.withdrawPlayer(p, amt);
                    hasEcon = true;
                    econMsg = "-$" + fmtAmount(amt, ae.decimalPlaces);
                }
            } else if ("经济盲盒".equals(ae.type)) {
                if (economy != null) {
                    double amount = ae.moneyMin
                            + rng.nextDouble()
                            * (ae.moneyMax - ae.moneyMin);
                    amount = Math.round(amount * 100.0)
                            / 100.0;
                    economy.depositPlayer(p, amount);
                    hasEcon = true;
                    econMsg = "+$" + fmtAmount(amount,
                            ae.decimalPlaces);
                }
            } else if ("债券盲盒".equals(ae.type)) {
                int range = Math.max(0, ae.bondMax - ae.bondMin);
                int bondAmt = ae.bondMin
                        + (range > 0 ? rng.nextInt(range + 1) : 0);
                log("[债券盲盒] 金额=" + bondAmt + " bdb="
                        + (bdb != null ? "有" : "无"));
                if (bondAmt > 0 && bdb != null) {
                    int bef = 0;
                    try {
                        PreparedStatement q = bdb.prepareStatement(
                                "SELECT amount FROM bonds"
                                        + " WHERE player_name=?");
                        q.setString(1, p.getName());
                        ResultSet r = q.executeQuery();
                        if (r.next()) bef = r.getInt("amount");
                        r.close(); q.close();
                    } catch (Exception ex) {
                        log("[债券盲盒] 查询余额失败: " + ex);
                    }
                    try {
                        PreparedStatement ps = bdb.prepareStatement(
                                "INSERT INTO bonds"
                                        + "(player_name,amount)"
                                        + " VALUES(?,?) "
                                        + "ON CONFLICT(player_name)"
                                        + " DO UPDATE SET "
                                        + "amount=amount+?");
                        ps.setString(1, p.getName());
                        ps.setInt(2, bondAmt);
                        ps.setInt(3, bondAmt);
                        ps.executeUpdate();
                        ps.close();
                    } catch (SQLException ex) {
                        log("[债券盲盒] 写入失败: " + ex);
                    }
                    log("[债券盲盒] 写入后余额=" + (bef + bondAmt)
                            + "，准备写流水");
                    logBondTx(p.getName(), "redeem", bondAmt,
                            "", "口令系统", "口令奖励-债券盲盒",
                            bef, bef + bondAmt);
                    hasBond = true;
                    bondMsg = "+" + bondAmt + "枚(盲盒)";
                }

            } else if ("联控盲盒".equals(ae.type)) {
                int dayRange = Math.max(0, ae.daysMax - ae.daysMin);
                int slotRange = Math.max(0, ae.slotsMax - ae.slotsMin);
                int rd = ae.daysMin + (dayRange > 0 ? rng.nextInt(dayRange + 1) : 0);
                int rs = ae.slotsMin + (slotRange > 0 ? rng.nextInt(slotRange + 1) : 0);
                if (tryCyActivate(p, rs, rd)) {
                    hasSlots = true;
                    slotsMsg = "+" + rd + "天 +" + rs + "格(盲盒)";
                } else {
                    p.sendMessage(colorize("&c[SDF1] 联控连接失败，盲盒作废"));
                }
            } else if ("联控".equals(ae.type)) {
                if (tryCyActivate(p, ae.slots, ae.days)) {
                    hasSlots = true;
                    slotsMsg = "+" + ae.slots + "格 " + ae.days + "天";
                }
            } else if ("发债券".equals(ae.type)) {
                int bondAmt = (int) ae.money;
                if (bondAmt > 0 && bdb != null) {
                    int bef = 0;
                    try {
                        PreparedStatement q = bdb.prepareStatement(
                                "SELECT amount FROM bonds WHERE player_name=?");
                        q.setString(1, p.getName());
                        ResultSet r = q.executeQuery();
                        if (r.next()) bef = r.getInt("amount");
                        r.close(); q.close();
                    } catch (Exception ignored) {}
                    try {
                        PreparedStatement ps = bdb.prepareStatement(
                                "INSERT INTO bonds(player_name, amount) "
                                        + "VALUES(?,?) "
                                        + "ON CONFLICT(player_name) "
                                        + "DO UPDATE SET amount=amount+?");
                        ps.setString(1, p.getName());
                        ps.setInt(2, bondAmt);
                        ps.setInt(3, bondAmt);
                        ps.executeUpdate();
                        ps.close();
                    } catch (SQLException ignored) {}
                    logBondTx(p.getName(), "admin_give", bondAmt,
                            "", "口令系统", "口令奖励-发债券",
                            bef, bef + bondAmt);
                    hasBond = true;
                    bondMsg = "+" + bondAmt + "枚";
                }


            } else if ("删除口令".equals(ae.type)) {
                consumeCode(msg, true);
            }
        }

        // ===== 快照：执行后的经济/债券 =====
        double econAfter = 0;
        if (economy != null) econAfter = economy.getBalance(p);
        int bondAfter = 0;
        if (bdb != null) {
            try {
                PreparedStatement q2 = bdb.prepareStatement(
                        "SELECT amount FROM bonds WHERE player_name=?");
                q2.setString(1, p.getName());
                ResultSet rs2 = q2.executeQuery();
                if (rs2.next()) bondAfter = rs2.getInt("amount");
                rs2.close();
                q2.close();
            } catch (Exception ignored) {}
        }

        // ===== 统一输出 =====
        p.sendMessage(colorize("&7============== &e" + sa.actions.size() + "个动作 &7=============="));
        p.sendMessage(colorize("&7玩家: &e" + p.getName()));
        p.sendMessage(colorize("&7口令: &e" + msg));

        if (hasEcon) {
            p.sendMessage(colorize("&7------------- &a经济 &7-------------"));
            p.sendMessage(colorize("&7变动: &e" + econMsg));
            p.sendMessage(colorize("&7余额: &e$" + String.format("%.2f", econBefore)
                    + " &7-> &a$" + String.format("%.2f", econAfter)));
        }
        if (hasBond) {
            p.sendMessage(colorize("&7------------- &6债券 &7-------------"));
            p.sendMessage(colorize("&7变动: &e" + bondMsg));
            p.sendMessage(colorize("&7余额: &e" + bondBefore + " &7-> &a" + bondAfter));
        }
        if (hasSlots) {
            p.sendMessage(colorize("&7------------- &b联控 &7-------------"));
            p.sendMessage(colorize("&7获得: &e" + slotsMsg));
        }
        if (!hasEcon && !hasBond && !hasSlots) {
            p.sendMessage(colorize("&7无经济/债券/背包变动"));
        }
        p.sendMessage(colorize("&7============== &e执行完毕 &7=============="));

        // 消耗口令（非删除口令类型统一处理）
        if (sa.deleteCode) {
            consumeCode(msg, true);
        }

        // 记名
        if (sa.recordName && cfgNameBoard != null && !cfgNameBoard.isEmpty()) {
            final String nameKey = msg + "_" + p.getName();
            final String boardName = cfgNameBoard;
            final Player fp = p;
            Bukkit.getScheduler().runTask(this, new Runnable() {
                public void run() {
                    try {
                        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
                        Objective obj = board.getObjective(boardName);
                        if (obj == null) {
                            log("[记名] 记名板不存在: " + boardName);
                            fp.sendMessage(colorize("&c[SDF1] 记名板不存在，记名失败"));
                            return;
                        }
                        obj.getScore(nameKey).setScore(1);
                        log("[记名] 写入: " + nameKey);
                        fp.sendMessage(colorize("&a[SDF1] 记名成功"));
                    } catch (Exception e) {
                        log("[记名] 写入失败: " + e.getMessage());
                    }
                }
            });
        }
    }


    /** 向 bond_transaction 表写入一条流水 */
    private void logBondTx(String player, String type,
                           int amount, String target,
                           String operator, String reason,
                           int before, int after) {
        Connection bdb = getBondDb();
        if (bdb == null) {
            log("[Bond] logBondTx: bdb=null, 跳过");
            return;
        }
        try {
            PreparedStatement ps = bdb.prepareStatement(
                    "INSERT INTO bond_transaction"
                            + "(player_name,type,amount,"
                            + "target_player,operator,reason,"
                            + "balance_before,balance_after,time)"
                            + " VALUES(?,?,?,?,?,?,?,?,?)");
            ps.setString(1, player);
            ps.setString(2, type);
            ps.setInt(3, amount);
            ps.setString(4, target != null ? target : "");
            ps.setString(5, operator != null ? operator : "");
            ps.setString(6, reason != null ? reason : "");
            ps.setInt(7, before);
            ps.setInt(8, after);
            ps.setLong(9, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
            log("[Bond] logBondTx成功: " + player
                    + " " + type + " " + amount);
        } catch (Exception e) {
            log("[Bond] logBondTx失败: " + e.getMessage());
            e.printStackTrace();
        }
    }





    private void execCy(Player p, ActionEntry a, boolean deleteCode, String code) {
        if (tryCyActivate(p, a.slots, a.days)) {
            consumeCode(code, deleteCode);
            p.sendMessage(colorize("&a[SDF1] 激活: " + a.slots + "格 " + (a.days > 0 ? a.days + "天" : "永久")));
        }
    }

    private boolean tryCyActivate(Player p, int slots, int days) {
        if (!isCyConnected()) { discoverCyPlugin(); if (!isCyConnected()) { p.sendMessage(colorize("&c[SDF1] 联控未连接，跳过")); return false; } }
        try { discoveredCyActivate.invoke(discoveredCy, p.getName(), slots, days); return true; }
        catch (Exception e) { p.sendMessage(colorize("&c[SDF1] 联控失败")); log("[联控] 失败: " + e.getMessage()); return false; }
    }


    private void execDelete(Player p, ActionEntry a, String code) {
        log("[删除] " + p.getName() + " 口令: \"" + code + "\"");
        consumeCode(code, true);
    }

    private boolean tryBondRedeem(Player p,
                                  String code) {
        try {
            Plugin sdf1 = Bukkit.getPluginManager()
                    .getPlugin("Sdf1_login");
            if (sdf1 == null || !sdf1.isEnabled())
                return false;

            java.lang.reflect.Method m =
                    sdf1.getClass().getMethod(
                            "redeemBondForExternal",
                            String.class,
                            String.class);
            Object result = m.invoke(sdf1,
                    p.getName(), code);
            String res = result != null
                    ? result.toString()
                    : "fail:null";

            if (res.startsWith("success:")) {
                String[] parts = res.split(":");
                int amount =
                        Integer.parseInt(parts[1]);
                int bef =
                        Integer.parseInt(parts[2]);
                int aft =
                        Integer.parseInt(parts[3]);

                p.sendMessage(colorize(
                        "&6&l[债券] &e&l恭喜！获得 &c&l"
                                + amount + " &e&l债券！"));
                p.sendMessage(colorize(
                        "&7余额: &e" + bef
                                + " &7-> &a" + aft));
                log("[债券] " + p.getName()
                        + " 兑换 " + amount + " 债券");
                return true;
            }

            if ("fail:not_found".equals(res))
                return false;
            if ("fail:already_used".equals(res)) {
                p.sendMessage(colorize(
                        "&c此兑换码已被使用"));
                return true;
            }
            if ("fail:frozen".equals(res)) {
                p.sendMessage(colorize(
                        "&c账户已被冻结"));
                return true;
            }
            return false;
        } catch (Exception e) {
            log("[债券] 反射调用失败: "
                    + e.getMessage());
            return false;
        }
    }

    private void execGiveBond(Player p, ActionEntry a, boolean deleteCode, String code) {
        int amount = (int) a.money;
        if (amount <= 0) { p.sendMessage(colorize("&c[SDF1] 债券金额无效")); return; }
        double oldEcon = 0;
        if (economy != null) oldEcon = economy.getBalance(p);
        int oldBonds = 0;
        Connection bdb = getBondDb();
        if (bdb != null) {
            try {
                PreparedStatement q = bdb.prepareStatement("SELECT amount FROM bonds WHERE player_name=?");
                q.setString(1, p.getName());
                ResultSet rs = q.executeQuery();
                if (rs.next()) oldBonds = rs.getInt("amount");
                rs.close();
                q.close();
            } catch (Exception ignored) {}
        }

        if (bdb != null) {
            try {
                PreparedStatement ps = bdb.prepareStatement(
                        "INSERT INTO bonds(player_name, amount) VALUES(?,?) "
                                + "ON CONFLICT(player_name) DO UPDATE SET amount=amount+?");
                ps.setString(1, p.getName());
                ps.setInt(2, amount);
                ps.setInt(3, amount);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ignored) {}
        }
        consumeCode(code, deleteCode);

        double newEcon = 0;
        if (economy != null) newEcon = economy.getBalance(p);
        int newBonds = 0;
        if (bdb != null) {
            try {
                PreparedStatement q2 = bdb.prepareStatement("SELECT amount FROM bonds WHERE player_name=?");
                q2.setString(1, p.getName());
                ResultSet rs2 = q2.executeQuery();
                if (rs2.next()) newBonds = rs2.getInt("amount");
                rs2.close();
                q2.close();
            } catch (Exception ignored) {}
        }

        p.sendMessage(colorize("&7============== &6获得债券 &7=============="));
        p.sendMessage(colorize("&7玩家: &e" + p.getName()));
        p.sendMessage(colorize("&7口令: &e" + code));
        p.sendMessage(colorize("&6&l获得 &c&l" + amount + " &6&l枚债券"));
        p.sendMessage(colorize("&7-------------"));
        p.sendMessage(colorize("&7经济余额: &e$" + String.format("%.2f", oldEcon) + " &7-> &a$" + String.format("%.2f", newEcon)));
        p.sendMessage(colorize("&7债券余额: &e" + oldBonds + " &7-> &a" + newBonds));
        p.sendMessage(colorize("&7============== &6获得债券 &7=============="));
    }

    // ★ 修改：权限不足直接返回帮助，不报"权限不足"
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (c.getName().equalsIgnoreCase("import")) {
            if (!checkAdminSilent(s)) { showHelp(s); return true; }
            if (a.length < 1) { s.sendMessage(colorize("&c用法: /import <文件名.txt>")); return true; }
            execImport(s, a[0]);
            return true;
        }
        if (a.length == 0) {
            if (s instanceof Player) {
                Player p = (Player) s;
                if (!configLoaded || cfgCdkObj.isEmpty()) { showHelp(s); return true; }
                startListening(p);
            } else {
                showHelp(s);
            }
            return true;
        }
        String sub = a[0].toLowerCase();
        if (sub.equals("listen")) {
            if (!checkAdminSilent(s)) { showHelp(s); return true; }
            if (!(s instanceof Player)) { showHelp(s); return true; }
            startListening((Player) s);
            return true;
        }
        if (!checkAdminSilent(s)) { showHelp(s); return true; }
        if (sub.equals("status")) {
            s.sendMessage("[SDF1] v" + cfgVer
                    + " 计分板:" + cfgCdkObj
                    + " 记名板:" + (cfgNameBoard.isEmpty()
                    ? "(未设置)" : cfgNameBoard)
                    + " 规则:" + scoreMap.size()
                    + " 优惠券规则:" + couponRules.size()
                    + " 联控:" + cyPing()
                    + " Vault:" + (economy != null
                    ? economy.getName() : "无"));
            return true;
        }

        if (sub.equals("reload")) {
            scoreMap.clear(); configLoaded = false; cdksWarned = false; listening.clear(); failCount = 0; circuitBroken = false;
            loadConfig(true); discoverCyPlugin(); setupEconomy();
            s.sendMessage("[SDF1] 重载 v" + cfgVer + " 规则:" + scoreMap.size());
            return true;
        }
        if (sub.equals("update")) { checkUpdate(s); return true; }
        if (sub.equals("get")) { execGet(s); return true; }
        if (sub.equals("import")) {
            if (a.length < 2) { s.sendMessage(colorize("&c用法: /sdf1 import <文件名.txt>")); return true; }
            execImport(s, a[1]);
            return true;
        }
        if (sub.equals("undo")) { doUndo(s); return true; }
        showHelp(s);
        return true;
    }

    // ★ 修改：管理员显示完整帮助，非管理员只显示监听
    private void showHelp(CommandSender s) {
        if (checkAdminSilent(s)) {
            s.sendMessage(colorize("&e/sdf1 - 打开口令监听"));
            s.sendMessage(colorize("&e/sdf1 status - 查看状态"));
            s.sendMessage(colorize("&e/sdf1 listen - 开启监听"));
            s.sendMessage(colorize("&e/sdf1 reload - 重载配置"));
            s.sendMessage(colorize("&e/sdf1 get - 查看口令库存"));
            s.sendMessage(colorize("&e/sdf1 import <文件> - 导入口令"));
            s.sendMessage(colorize("&e/sdf1 undo - 撤销上次导入"));
            s.sendMessage(colorize("&e/import <文件> - 导入口令(独立命令)"));
            s.sendMessage(colorize("&e/sdf1 update - 检查更新"));
        } else {
            s.sendMessage(colorize("&e/sdf1 - 打开口令监听"));
        }
    }

    private void execGet(CommandSender s) {
        if (cfgCdkObj.isEmpty()) { s.sendMessage(colorize("&c计分板未配置")); return; }
        try {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective obj = board.getObjective(cfgCdkObj);
            if (obj == null) { s.sendMessage(colorize("&c计分板目标不存在: " + cfgCdkObj)); return; }
            Map<Integer, List<String>> grouped = new TreeMap<Integer, List<String>>();
            int total = 0;
            for (String entry : board.getEntries()) {
                try { Score sc = obj.getScore(entry); if (sc.isScoreSet()) { int val = sc.getScore(); if (!grouped.containsKey(val)) grouped.put(val, new ArrayList<String>()); grouped.get(val).add(entry); total++; } } catch (Exception ignored) {}
            }
            s.sendMessage(colorize("&a[SDF1] ===== 口令库存全量抄送 ====="));
            s.sendMessage(colorize("&a[SDF1] 口令库: " + cfgCdkObj));
            s.sendMessage(colorize("&a[SDF1] 总存货: " + total + " 条"));
            s.sendMessage(colorize("&a[SDF1] 规则数: " + scoreMap.size()));
            if (!cfgNameBoard.isEmpty()) s.sendMessage(colorize("&a[SDF1] 记名板: " + cfgNameBoard));
            s.sendMessage(colorize("&7----------------"));
            for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
                int scoreVal = entry.getKey(); List<String> codes = entry.getValue();
                ScoreAction sa = scoreMap.get(scoreVal);
                String ruleDesc = sa != null ? (sa.actions.size() + "个动作" + (sa.recordName ? " (记名)" : "") + (sa.deleteCode ? " (删口令)" : "")) : "未配置规则";
                s.sendMessage(colorize("&e" + scoreVal + "分 &7[" + ruleDesc + "] &f共" + codes.size() + "条，口令为："));
                int claimedCount = 0;
                if (sa != null && sa.recordName && !cfgNameBoard.isEmpty()) {
                    try {
                        Objective nameObj = board.getObjective(cfgNameBoard);
                        if (nameObj != null) {
                            Set<String> codeSet = new HashSet<String>(codes);
                            for (String ne : board.getEntries()) {
                                try { Score ns = nameObj.getScore(ne); if (ns.isScoreSet() && ns.getScore() > 0) { for (String code : codeSet) { if (ne.startsWith(code + "_")) { claimedCount++; break; } } } } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }
                for (String code : codes) { s.sendMessage(colorize("  &f" + code)); }
                if (sa != null && sa.recordName && !cfgNameBoard.isEmpty()) {
                    s.sendMessage(colorize("  &7已领取: &c" + claimedCount + "&7/" + codes.size() + " (剩余&a" + (codes.size() - claimedCount) + "&7)"));
                }
                s.sendMessage(colorize("&7----------------"));
            }
            s.sendMessage(colorize("&a[SDF1] 抄送完毕"));
        } catch (Exception e) { s.sendMessage(colorize("&c查询失败: " + e.getMessage())); }
    }

    private String cleanImportCode(String s) {
        s = s.replaceAll("^[(（【{\\[「『\"]+", "").trim();
        s = s.replaceAll("[)）】}\\]」』\"]+$", "").trim();
        s = s.replaceAll("^[-*•·>►▶]+\\s*", "").trim();
        return s;
    }

    private int tryImportCode(Objective obj, Scoreboard board, String code, int scoreVal) {
        try { if (obj.getScore(code).isScoreSet() && obj.getScore(code).getScore() > 0) return 2; } catch (Exception e) { return 2; }
        ScoreAction targetSA = scoreMap.get(scoreVal);
        if (targetSA != null && targetSA.recordName && cfgNameBoard != null && !cfgNameBoard.isEmpty()) {
            try {
                Objective nameObj = board.getObjective(cfgNameBoard);
                if (nameObj != null) {
                    for (String ne : board.getEntries()) {
                        if (ne.startsWith(code + "_")) { Score ns = nameObj.getScore(ne); if (ns.isScoreSet() && ns.getScore() > 0) { log("[导入-查重] \"" + code + "\" 已被领取，跳过"); return 3; } }
                    }
                }
            } catch (Exception ignored) {}
        }
        try { obj.getScore(code).setScore(scoreVal); return 1; } catch (Exception e) { return 2; }
    }

    // ★ 修改：导入时追踪口令用于撤销
    private void execImport(CommandSender s, String fileName) {
        if (cfgCdkObj.isEmpty()) { s.sendMessage(colorize("&c计分板未配置")); return; }
        File f = new File(getDataFolder(), fileName);
        if (!f.exists()) { s.sendMessage(colorize("&c文件不存在: " + fileName)); return; }
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
            List<String> rawLines = new ArrayList<String>(); String line;
            while ((line = r.readLine()) != null) rawLines.add(line); r.close();
            List<String> diag = new ArrayList<String>();
            List<String> cleanLines = stripAllComments(rawLines, diag);

            undoBuf.clear(); canUndo = false;

            int count = 0, skip = 0, dedup = 0; int currentScore = 1;
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective obj = board.getObjective(cfgCdkObj);
            if (obj == null) { s.sendMessage(colorize("&c计分板目标不存在: " + cfgCdkObj)); return; }

            for (String cleanLine : cleanLines) {
                String trimmed = cleanLine.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.equals("--") || trimmed.equals("---")) { currentScore = 1; continue; }
                Matcher blockHead = Pattern.compile("^(\\d+)\\s*分\\s*[：:]?\\s*$").matcher(trimmed);
                if (blockHead.matches()) { currentScore = Math.max(1, Integer.parseInt(blockHead.group(1))); continue; }

                List<String[]> inlinePairs = new ArrayList<String[]>();
                Matcher pairMatcher = Pattern.compile("(\\S+?)\\s+(\\d+)\\s*分").matcher(trimmed);
                StringBuffer matchedSb = new StringBuffer();
                while (pairMatcher.find()) { inlinePairs.add(new String[]{pairMatcher.group(1), pairMatcher.group(2)}); pairMatcher.appendReplacement(matchedSb, ""); }
                pairMatcher.appendTail(matchedSb);
                String remainingLine = matchedSb.toString().trim();

                if (!inlinePairs.isEmpty()) {
                    for (String[] pair : inlinePairs) {
                        String codeBlock = pair[0].trim(); int pairScore = Math.max(1, Integer.parseInt(pair[1]));
                        String[] subCodes = codeBlock.split("[|,，、;；]+");
                        for (String sub : subCodes) {
                            sub = cleanImportCode(sub); if (sub.isEmpty()) continue;
                            int result = tryImportCode(obj, board, sub, pairScore);
                            if (result == 1) { count++; undoBuf.add(new UndoRecord(sub, pairScore)); }
                            else if (result == 2) skip++; else if (result == 3) dedup++;
                        }
                    }
                    if (!remainingLine.isEmpty()) {
                        String[] remainParts = remainingLine.split("[|,，、;；]+");
                        for (String part : remainParts) {
                            part = cleanImportCode(part); if (part.isEmpty()) continue;
                            int result = tryImportCode(obj, board, part, currentScore);
                            if (result == 1) { count++; undoBuf.add(new UndoRecord(part, currentScore)); }
                            else if (result == 2) skip++; else if (result == 3) dedup++;
                        }
                    }
                } else {
                    String[] parts = trimmed.split("[|,，、;；]+");
                    boolean isBondLine = trimmed.contains("债券");
                    for (String part : parts) {
                        if (isBondLine) {
                            String[] bondFields = part.split("\\s+");
                            if (bondFields.length >= 2) {
                                String bondCode = bondFields[0];
                                try {
                                    int bondAmt = Integer.parseInt(bondFields[1]);
                                    Connection bdb = getBondDb();
                                    if (bdb != null) {
                                        PreparedStatement bps = bdb.prepareStatement(
                                                "INSERT OR IGNORE INTO cdk(code,amount,type,created_time) VALUES(?,?,?,?)");
                                        bps.setString(1, bondCode);
                                        bps.setInt(2, bondAmt);
                                        bps.setString(3, "bond");
                                        bps.setLong(4, System.currentTimeMillis());
                                        int rows = bps.executeUpdate();
                                        bps.close();
                                        if (rows > 0) {
                                            count++;
                                            undoBuf.add(new UndoRecord(bondCode, currentScore));
                                        } else {
                                            skip++;
                                        }
                                    }
                                } catch (Exception ignored) {}
                                continue;
                            }
                        }

                        part = cleanImportCode(part); if (part.isEmpty()) continue;
                        int result = tryImportCode(obj, board, part, currentScore);
                        if (result == 1) { count++; undoBuf.add(new UndoRecord(part, currentScore)); }
                        else if (result == 2) skip++; else if (result == 3) dedup++;
                    }
                }
            }
            String resultMsg = "&a[SDF1] 导入完成: 新增 " + count + " 条，跳过 " + skip + " 条";
            if (dedup > 0) resultMsg += "，记名查重拦截 " + dedup + " 条";
            s.sendMessage(colorize(resultMsg));
            log("[导入] " + s.getName() + " " + fileName + " 新增=" + count + " 跳过=" + skip + " 查重=" + dedup);
            if (!undoBuf.isEmpty()) { canUndo = true; s.sendMessage(colorize("&e/sdf1 undo &7可撤销本次导入")); }
        } catch (Exception e) { s.sendMessage(colorize("&c读取失败: " + e.getMessage())); }
    }

    // ★ 新增：撤销上次导入
    private void doUndo(CommandSender s) {
        if (!canUndo || undoBuf.isEmpty()) { s.sendMessage(colorize("&c无可撤销的导入操作")); return; }
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective obj = board.getObjective(cfgCdkObj);
        if (obj == null) { s.sendMessage(colorize("&c计分板目标不存在")); undoBuf.clear(); canUndo = false; return; }
        int removed = 0;
        for (UndoRecord rec : undoBuf) {
            try {
                Score sc = obj.getScore(rec.code);
                if (sc.isScoreSet()) {
                    boolean ok = false;
                    try { obj.getClass().getMethod("resetScore", String.class).invoke(obj, rec.code); ok = true; } catch (Throwable ignored) {}
                    if (!ok) try { board.getClass().getMethod("resetScore", String.class).invoke(board, rec.code); ok = true; } catch (Throwable ignored) {}
                    if (!ok) sc.setScore(Integer.MIN_VALUE);
                    removed++;
                }
            } catch (Exception ignored) {}
        }
        final List<String> codes = new ArrayList<String>();
        for (UndoRecord rec : undoBuf) codes.add(rec.code);
        Bukkit.getScheduler().runTask(this, new Runnable() {
            public void run() {
                for (String code : codes) {
                    try { String safeCode = code.replace("\"", "\\\""); Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players reset " + safeCode + " " + cfgCdkObj); } catch (Exception ignored) {}
                }
            }
        });
        s.sendMessage(colorize("&a[SDF1] 撤销完成: 移除 " + removed + "/" + undoBuf.size() + " 条口令"));
        log("[撤销] " + s.getName() + " 移除=" + removed + "/" + undoBuf.size());
        undoBuf.clear(); canUndo = false;
    }

    private void checkUpdate(final CommandSender manual) {
        String checkingMsg = "正在检查更新..."; log(checkingMsg);
        if (manual != null) manual.sendMessage(checkingMsg);
        new Thread(new Runnable() { public void run() {
            try {
                boolean preferGH = "GH".equalsIgnoreCase(cfgUpdateChannel) || cfgUpdateChannel.isEmpty();
                String pApi = preferGH ? API_GH : API_GE; String pDl = preferGH ? DL_GH : DL_GE; String pName = preferGH ? "GitHub" : "Gitee";
                String bApi = preferGH ? API_GE : API_GH; String bDl = preferGH ? DL_GE : DL_GH; String bName = preferGH ? "Gitee" : "GitHub";
                String[] result = fetchRelease(pApi, pName);
                if (result != null) { applyUpdate(result[0], result[1], pDl, manual, pName); return; }
                log("[更新] " + pName + " 失败，切换 " + bName);
                result = fetchRelease(bApi, bName);
                if (result != null) { applyUpdate(result[0], result[1], bDl, manual, bName); return; }
                log("双路均失败");
                if (manual != null) manual.sendMessage("[更新] 检查失败");
            } catch (Exception e) { log("异常: " + e.getMessage()); }
        }}).start();
    }

    private String[] fetchRelease(String apiUrl, String ch) {
        HttpURLConnection c = null;
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager(){public X509Certificate[] getAcceptedIssuers(){return null;}public void checkClientTrusted(X509Certificate[] c,String a){}public void checkServerTrusted(X509Certificate[] c,String a){}}};
            SSLContext sc = SSLContext.getInstance("TLS"); sc.init(null, trustAll, new java.security.SecureRandom());
            URL url = new URL(apiUrl); c = (HttpURLConnection) url.openConnection();
            if (c instanceof HttpsURLConnection) { HttpsURLConnection hc = (HttpsURLConnection) c; hc.setSSLSocketFactory(sc.getSocketFactory()); hc.setHostnameVerifier(new javax.net.ssl.HostnameVerifier(){public boolean verify(String h,javax.net.ssl.SSLSession sess){return true;}}); }
            c.setRequestMethod("GET"); c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            c.setRequestProperty("Accept", "*/*");
            c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            c.setConnectTimeout(15000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(true);
            int code = c.getResponseCode();
            if (code != 200) { log(ch + " HTTP " + code); return null; }
            String json = readStream(c.getInputStream());
            if (json == null || json.isEmpty()) { log(ch + " 响应为空"); return null; }
            String rv = jParse(json, "tag_name"); String rn = jParse(json, "body");
            if (rv == null || rv.isEmpty()) return null;
            return new String[]{rv, rn != null ? rn : ""};
        } catch (Exception e) {
            log(ch + " " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private String readStream(InputStream is) {
        try { BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)); StringBuilder sb = new StringBuilder(); String line; while ((line = reader.readLine()) != null) sb.append(line); reader.close(); return sb.toString(); }
        catch (Exception e) { log("[更新] 读取流失败: " + e.getMessage()); return null; }
    }

    private void applyUpdate(String rv, String notes, String dlLink, CommandSender manual, String ch) {
        remoteVer = rv;
        // 版本比较：远程≠本地即视为有更新（用户即将推新版本）
        if (!rv.trim().equals(cfgVer.trim())) {
            // 使用Bukkit.getConsoleSender().sendMessage()输出彩色日志
            Bukkit.getConsoleSender().sendMessage("§a[Sdf1] §e新版本! v" + cfgVer + " -> v" + rv);
            Bukkit.getConsoleSender().sendMessage("§a[Sdf1] §b§l下载: " + dlLink);
            if (manual != null) { manual.sendMessage("§a[Sdf1] §e新版本! v" + cfgVer + " -> v" + rv); manual.sendMessage("§a[Sdf1] §b§l下载: " + dlLink); }
            for (Player op : Bukkit.getOnlinePlayers()) { if (op.isOp()) { op.sendMessage("§a[Sdf1] §e新版本! v" + cfgVer + " -> v" + rv); op.sendMessage("§a[Sdf1] §b§l下载: " + dlLink); } }
        } else {
            Bukkit.getConsoleSender().sendMessage("§a[Sdf1] §7已是最新 v" + cfgVer);
            if (manual != null) manual.sendMessage("§a[Sdf1] §7已是最新 v" + cfgVer);
        }
    }

    private int parseVersionNum(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String jParse(String j, String k) {
        int i = j.indexOf("\"" + k + "\""); if (i < 0) return "";
        int colon = j.indexOf(":", i); int start = j.indexOf("\"", colon + 1);
        if (start < 0) return ""; int end = j.indexOf("\"", start + 1); if (end < 0) return "";
        return j.substring(start + 1, end);
    }
}
