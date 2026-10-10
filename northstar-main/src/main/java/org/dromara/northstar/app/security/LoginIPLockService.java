package org.dromara.northstar.app.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

/**
 * IP 登录失败限流服务
 * <p>
 * 同一 IP 密码错误达到 5 次，冻结该 IP 所有操作 1 小时；
 * 在冻结期内再次触发失败，则升级为封禁该 IP 1 天。
 * </p>
 *
 * @author KevinHuangwl
 */
@Component
public class LoginIPLockService {

    /**
     * 触发冻结所需的连续失败次数
     */
    public static final int MAX_FAILURES = 5;

    /**
     * 冻结持续时间：1 小时
     */
    public static final long FREEZE_DURATION_MS = TimeUnit.HOURS.toMillis(1);

    /**
     * 封禁持续时间：1 天
     */
    public static final long BAN_DURATION_MS = TimeUnit.DAYS.toMillis(1);

    /**
     * IP 状态
     */
    public enum LockStatus {
        NORMAL,
        FROZEN,
        BANNED
    }

    private final ConcurrentMap<String, IPLoginStatus> statusMap = new ConcurrentHashMap<>();

    private final Clock clock;

    public LoginIPLockService() {
        this(Clock.systemUTC());
    }

    LoginIPLockService(Clock clock) {
        this.clock = clock;
    }

    /**
     * 记录一次登录失败，并返回当前 IP 的最新状态
     *
     * @param ip 客户端 IP
     * @return NORMAL / FROZEN / BANNED
     */
    public LockStatus recordFailure(String ip) {
        if (ip == null || ip.isBlank()) {
            return LockStatus.NORMAL;
        }
        IPLoginStatus status = statusMap.computeIfAbsent(ip, _ -> new IPLoginStatus());
        synchronized (status) {
            long now = clock.millis();

            // 若已封禁，检查是否过期
            if (status.bannedUntil > 0) {
                if (now < status.bannedUntil) {
                    return LockStatus.BANNED;
                }
                status.clear();
            }

            // 若已冻结，检查是否过期
            if (status.frozenUntil > 0) {
                if (now < status.frozenUntil) {
                    // 冻结期内再次失败，升级为封禁 1 天
                    status.bannedUntil = now + BAN_DURATION_MS;
                    status.frozenUntil = 0;
                    status.failures = 0;
                    return LockStatus.BANNED;
                }
                status.clear();
            }

            status.failures++;
            if (status.failures >= MAX_FAILURES) {
                status.frozenUntil = now + FREEZE_DURATION_MS;
                status.failures = 0;
                return LockStatus.FROZEN;
            }
            return LockStatus.NORMAL;
        }
    }

    /**
     * 检查 IP 当前状态（会惰性清理过期记录）
     *
     * @param ip 客户端 IP
     * @return NORMAL / FROZEN / BANNED
     */
    public LockStatus checkStatus(String ip) {
        if (ip == null || ip.isBlank()) {
            return LockStatus.NORMAL;
        }
        IPLoginStatus status = statusMap.get(ip);
        if (status == null) {
            return LockStatus.NORMAL;
        }
        synchronized (status) {
            long now = clock.millis();

            if (status.bannedUntil > 0) {
                if (now < status.bannedUntil) {
                    return LockStatus.BANNED;
                }
                status.clear();
                return LockStatus.NORMAL;
            }

            if (status.frozenUntil > 0) {
                if (now < status.frozenUntil) {
                    return LockStatus.FROZEN;
                }
                status.clear();
                return LockStatus.NORMAL;
            }

            return LockStatus.NORMAL;
        }
    }

    /**
     * 获取剩余限制时间（秒）
     *
     * @param ip 客户端 IP
     * @return 剩余秒数，未受限返回 0
     */
    public long getRemainingSeconds(String ip) {
        if (ip == null || ip.isBlank()) {
            return 0;
        }
        IPLoginStatus status = statusMap.get(ip);
        if (status == null) {
            return 0;
        }
        synchronized (status) {
            long now = clock.millis();
            if (status.bannedUntil > 0 && now < status.bannedUntil) {
                return (status.bannedUntil - now) / 1000;
            }
            if (status.frozenUntil > 0 && now < status.frozenUntil) {
                return (status.frozenUntil - now) / 1000;
            }
            return 0;
        }
    }

    /**
     * 清除 IP 的登录失败记录（登录成功时调用）
     *
     * @param ip 客户端 IP
     */
    public void clear(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        statusMap.remove(ip);
    }

    private static class IPLoginStatus {
        int failures;
        long frozenUntil;
        long bannedUntil;

        void clear() {
            failures = 0;
            frozenUntil = 0;
            bannedUntil = 0;
        }
    }
}
