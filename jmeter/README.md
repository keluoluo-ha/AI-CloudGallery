# 图片缓存压测对照 (JMeter)

对照压测三组接口，验证缓存层级对热点查询响应时间的提升。

## 一、对照分组

| 组 | Thread Group 名 | 接口 | 方法 | 缓存层 |
|---|---|---|---|---|
| A | `A-直查DB(POST /picture/list/page/vo)` | `/api/picture/list/page/vo` | POST + JSON | 无（直查 MySQL） |
| B | `B-Redis缓存(GET /picture/list/page/vo/cache)` | `/api/picture/list/page/vo/cache` | GET | Redis |
| C | `C-多级缓存(GET /picture/list/page/vo/caffeinecache)` | `/api/picture/picture/list/page/vo/caffeinecache` | GET | Caffeine + Redis |

> C 组路径里 `picture/picture` 是源码里方法注解写成 `/picture/list/page/vo/caffeinecache`、类上又是 `@RequestMapping("/picture")` 导致的重复。**先按实际路径压，能跑通就行；压完再决定要不要把方法注解改成 `/list/page/vo/caffeinecache`。**

三组默认参数：`current=1, pageSize=10`（走公共图库，仅看过审图片，无需登录态）。

## 二、压测参数（已写在 jmx 里）

- 线程数（并发用户）：100
- Ramp-Up：10 秒
- 持续时间：60 秒
- 调度器：开启（按时间跑，不按循环次数）

可在 JMeter 顶部「User Defined Variables」改 `THREADS` / `DURATION` / `HOST` / `PORT`。

## 三、运行步骤

### 1. 准备
1. 启动 MySQL、Redis、Spring Boot 服务（端口 8123）。
2. 确认 DB 里有足够过审图片（建议 ≥ 1000 条），否则缓存命中率无意义。
3. 安装 JMeter 5.x（https://jmeter.apache.org/download_jmeter.cgi）。

### 2. 打开测试计划
GUI 模式：
```
jmeter -t jmeter/PictureCacheBenchmark.jmx
```
或双击 jmx 文件用 JMeter GUI 打开。

### 3. 分组跑（关键：不要同时跑！）

三个 Thread Group 默认只有 **A 启用**，B、C 禁用。原因是同时跑会互相抢 CPU/连接池，污染数据。

跑法：
1. 右键 Thread Group A → Enable，B、C 保持 Disable。
2. 点绿色播放按钮跑 60 秒。
3. 跑完记录 A 的 Aggregate Report 数据。
4. **清空 Redis**：`redis-cli FLUSHDB`（让 B 从冷启动开始，公平）。
5. 禁用 A，启用 B，跑 60 秒，记录 B 数据。
6. 再次 `redis-cli FLUSHDB`。
7. 禁用 B，启用 C，跑 60 秒，记录 C 数据。

> 也可以用命令行非 GUI 模式跑（推荐，结果更准、可保存到文件）：
> ```
> jmeter -n -t jmeter/PictureCacheBenchmark.jmx -l jmeter/result-A.jtl -JthreadGroup=A-直查DB
> ```
> 但需要先把 B、C 在 GUI 里禁用、保存，再跑命令行。或者拆成三个独立 jmx 文件。

## 四、看结果

每个 Thread Group 下挂了两个 Listener：
- **Summary Report**：吞吐量、平均响应时间、错误率
- **Aggregate Report**：`90% Line` / `95% Line` / `99% Line`（P99 看这里）

重点看 Aggregate Report 的这几列：

| 列 | 含义 | 关注点 |
|---|---|---|
| Average | 平均响应时间 (ms) | 总体趋势 |
| 90% Line | P90 | 90% 请求快于这个值 |
| 95% Line | P95 | |
| **99% Line** | **P99** | **"20ms 以内"就看这列** |
| Min / Max | 最小/最大 | 排除抖动 |
| Error% | 错误率 | 应为 0%，否则有 bug |
| Throughput | 吞吐量 (req/s) | 越高越好 |

## 五、预期结果（参考）

| 组 | P99 预期 | 说明 |
|---|---|---|
| A 直查 DB | ~80~150ms | 走 MyBatis-Plus 分页 + JSON 序列化 + DB IO |
| B Redis 缓存 | ~20~40ms | 命中 Redis，省 DB IO，但有网络往返 |
| C 多级缓存 | ~5~15ms | 命中 Caffeine（进程内），无网络往返 |

如果 C 的 P99 没降到 20ms 以内，常见原因：
1. **没预热**：第一次请求未命中 Caffeine 也未命中 Redis，走了 DB。解决：正式压测前手动请求 1 次 C 接口预热 Caffeine。
2. **DB 数据量太少**：DB 查询本来就快，缓存优势体现不出来。
3. **JMeter 和服务同机**：网络开销为 0，A 组也会偏快。建议分机压测。
4. **Caffeine 容量不够**：默认 10000，热点 key 不会超。

## 六、写报告

跑完三组，把 Aggregate Report 截图，填进下表（示例）：

| 组 | Avg (ms) | P90 | P95 | P99 | Throughput (req/s) | Error% |
|---|---|---|---|---|---|---|
| A 直查 DB | 92 | 110 | 135 | 165 | 1080 | 0% |
| B Redis | 28 | 35 | 42 | 55 | 3520 | 0% |
| C 多级缓存 | 8 | 12 | 15 | 19 | 12400 | 0% |

结论话术：
> 在 100 并发、60 秒持续压测下，引入 Caffeine + Redis 多级缓存后，图片列表查询 P99 响应时间从 165ms 降至 19ms，下降 88%；吞吐量从 1080 req/s 提升至 12400 req/s，提升约 11 倍。

## 七、备选：wrk（如果没装 JMeter）

更轻量，一行命令：
```bash
# A 组（POST）
wrk -t4 -c100 -d60s -s post.lua http://localhost:8123/api/picture/list/page/vo

# B/C 组（GET）
wrk -t4 -c100 -d60s "http://localhost:8123/api/picture/list/page/vo/cache?current=1&pageSize=10"
wrk -t4 -c100 -d60s "http://localhost:8123/api/picture/picture/list/page/vo/caffeinecache?current=1&pageSize=10"
```
`post.lua` 内容：
```lua
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"current":1,"pageSize":10}'
```
wrk 输出 `Latency Distribution` 里的 `99%` 就是 P99。
