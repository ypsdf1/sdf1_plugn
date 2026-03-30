# sdf1 插件
## 目录：
- 主指令
- 参数

## 食用方法：
下载最新版本安装包[点击跳转](https://github.com/ypsdf1/sdf1_plugn/releases)，丢进服务器plugn目录就行了

## 指令介绍：

- 主指令：/sdf1，用途监控玩家输入的CDK，去匹配数据库
- 参数1：admin， 用途临时添加一个人为插件管理员，无需给予op
- 参数2：update，用途手动检查更新

## 数据库介绍
- 本插件轻量化，使用的是原版积分板作为数据库。未配置任何远程数据库或sqllite数据库。使用两个记分板。作为数据库，一个用来存储口令，一个用来存储玩家有没有兑换过这条口令。
- 创建记分板指令为scoreboard object add name dummy
- 创建口令指令为Scoreboard player set name1 name2 1 参数解析: Name一表示口令名字，Name二表示数据库名字

## 注意事项
- 本插件由gemini全程编写。如有问题请提交工单！
