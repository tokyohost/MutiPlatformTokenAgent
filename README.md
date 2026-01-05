# 这是一个使用agent 方式hook 某翼平台和ruoyi-vue-plus 登录接口获取token 并存储到redis 中的工具
### 解决的问题
- 适用于简单的多平台API调用获取Token集成的场景，不用那么重的引入OAuth2/SSO 客户端依赖
- 适用于没有统一认证网关的场景
- 适用于无法修改平台源码的场景
- 适用于无法使用浏览器登录获取token 的场景
- 无需逆向JS 登录验证的场景
### 支持的平台
- 某翼平台 6.9.x 及以上版本
- Ruoyi-vue-plus 5.X 版本
- 后续可方便的集成其它基于Springboot 的平台

### 原理说明
- 通过Java Agent 方式hook 掉某翼平台和ruoyi-vue-plus 的登录接口，模拟颁发token 的过程
- 通过配置的账号信息调用登录接口获取token
- 通过配置的redis 连接信息将token 写入redis 中，供其他API调用（gateway）使用

### 编译打包

```bash
mvn clean package -DskipTests
```


## agent 使用方法

### Agent启动命令

参数列表
- **passwordConfig**：指定密码配置文件名称，必须在agent包内
- **passwordOutSideConfig**：指定密码配置文件名称，必须在agent 同级目录下
- **username**：指定登录账号名
- **password**：指定登录密码
- **tokenRedisKey**：指定token 写入redis 的key 名称
- **redisHost**：指定redis 地址，默认localhost
- **redisPort**：指定redis 端口，默认6379
- **redisPassword**：指定redis 密码，默认无密码
- **redisDatabase**：指定redis 库，默认0
- **clientId**：RuoyiVuePlus 平台clientId
- **grantType**：RuoyiVuePlus 授权类型，默认password
- **tenantId**：RuoyiVuePlus 租户ID，默认000000
- **timeout**：RuoyiVuePlus 过期时间，单位秒，默认7天
- **refushTokenTime**：定时刷新Token 时间，单位秒，默认300秒

指定agent 内部配置文件方式：
```bash
#指定agent 内部配置文件方式：
java -javaagent:E:\WorkSpace\platformAuthAgent\platformAuthAgent\target\MutiPlatformTokenAgent-1.0.jar=passwordConfig=ctsi-pwd.yml
 -jar .\cloud_sentinel_api-6.9.3-SNAPSHOT.jar
```
启动时指定账号名方式：必须指定tokenRedisKey，否则无法将获取的token 写入redis中
```bash
#启动时指定账号名方式：必须指定tokenRedisKey，否则无法将获取的token 写入redis中
java -javaagent:E:\WorkSpace\platformAuthAgent\platformAuthAgent\target\MutiPlatformTokenAgent-1.0.jar=username=admin,password=123456,tokenRedisKey="ctsi:token" -jar .\cloud_sentinel_api-6.9.3-SNAPSHOT.jar
```
启动时指定外部配置文件方式
```bash
#启动时指定账号名方式：必须指定tokenRedisKey，否则无法将获取的token 写入redis中
java -javaagent:E:\WorkSpace\platformAuthAgent\platformAuthAgent\target\MutiPlatformTokenAgent-1.0.jar=passwordOutSideConfig=outsideConfig.yml -jar .\cloud_sentinel_api-6.9.3-SNAPSHOT.jar
```

ruoyivuePlus 启动方式
cmd 
```bash
java -javaagent:E:\WorkSpace\platformAuthAgent\platformAuthAgent\target\MutiPlatformTokenAgent-1.0.jar=passwordOutSideConfig=outsideConfigRuoyi.yml -Dspring.profiles.active=devtest -jar  .\ruoyi-admin.jar
 ```
powershell 启动方式
```powershell
java --% -javaagent:E:\WorkSpace\platformAuthAgent\platformAuthAgent\target\MutiPlatformTokenAgent-1.0.jar=passwordOutSideConfig=outsideConfigRuoyi.yml -Dspring.profiles.active=devtest -jar  .\ruoyi-admin.jar
 ```

passwordOutSideConfig 配置文件格式

```yaml
username: admin #账户名
password:  #密码 比翼平台可不用填密码
tokenRedisKey: "ctsi:token" #存储token 的redis key 名称
redisHost: localhost #redis 地址
redisPort: 6379 #redis 端口
redisPassword: "" #redis 密码
redisDatabase: 0 #redis 库
clientId: e5cd7e4891bf95d1d19206ce24a7b32e #RuoyiVuePlus 平台clientId
grantType: password #授权类型，默认password
tenantId: 000000 #租户ID，默认000000
timeout: 604800 #过期时间，单位秒，默认7天
refushTokenTime: 300 #定时刷新Token 时间，单位秒，默认300秒

```