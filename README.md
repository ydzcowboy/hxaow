# hxaow
韩信工程在线升级工具
* 自动执行数据库脚本
* 自动进行前后台服务包替换，支持多服务同时发布
* 自动清除废弃文件
* 执行记录版本信息，自动进行版本依赖校验、数据库文件校验。
* 支持升级失败后，再次升级。

* 可利用hudson版本自动构建，只需要将工程xml文件放到工程根目录进行发布即可。

#使用方式：
1. 进入安装目录
2. 将升级版本工程放到update下(CZYTH2016.12.30.00 为测试版本)
3. 修改conf/hxaow.conf 配置文件（具体参见配置文件说明,https://github.com/ydzcowboy/hxaow/wiki
4. 使用CMD 进入安装目录下。 执行如下命令： hxaow migrate 。  依照提示进行升级。   
   linux 系统， 使用： ./hxaow migrate 命令。（如果权限不足，进行权限授权：chmod u+x hxaow）
5. 执行升级后，会在安装目录，logs文件夹下，按日期生成升级文件日志。

具体参见：https://github.com/ydzcowboy/hxaow
