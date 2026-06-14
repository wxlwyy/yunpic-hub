package com.wyy.yunpicturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.UserRoleEnum;
import com.wyy.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VIP 过期降级定时任务
 * 每天凌晨 3 点执行，将过期的 VIP 用户降级为普通用户
 */
@Component
@Slf4j
public class VipExpireJob {

    @Resource
    private UserService userService;

    /**
     * 每天凌晨 3 点执行
     * cron 表达式：秒 分 时 日 月 周，日和月用*表示每天和每月，日和周必须有一个是？表示不用这个字段
     * 如果表达每周三就WED，此时日就是？
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void downgradeExpiredVip() {
        log.info("开始执行 VIP 过期降级任务");

        int batchSize = 500;  // 每批 500 人，防止长事务
        long totalCount = 0;  // 总共处理的人数，改成 long，防止溢出
        LocalDateTime now = LocalDateTime.now();

        int maxBatches = 100;  // 最多处理 100 批，防止死循环
        int batchCount = 0;  // 处理了多少批

        while (batchCount < maxBatches) {
            batchCount++;

            // 分批查询过期用户（按 ID 排序，保证顺序）
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userRole", UserRoleEnum.VIP.getValue())
                       .lt("vipExpireTime", now)  // lt是小于号
                       .orderByAsc("id")  // 按 ID 排序，每次从最小的开始取，避免遗漏
                       .last("LIMIT " + batchSize);  // 每次只处理五百个

            List<User> expiredUsers = userService.list(queryWrapper);

            if (expiredUsers == null || expiredUsers.isEmpty()) {
                break;
            }

            // 提取用户 ID
            List<Long> userIds = expiredUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            // 批量降级（带角色条件，防止重复更新）
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.in("id", userIds)
                        .eq("userRole", UserRoleEnum.VIP.getValue())  // 确保是 VIP 才降级
                        .set("userRole", UserRoleEnum.USER.getValue());
                        // 保留 vipExpireTime，方便追溯
                        // TODO: 如果需要审计，可以增加 vipDowngradeTime 字段记录降级时间

            // 获取实际更新的行数，一条sql就能批量执行，是原子性
            int updated = userService.getBaseMapper().update(null, updateWrapper);  // 传null表示用updateWrapper负责
            totalCount += updated;

            log.info("第 {} 批：查询 {} 人，实际更新 {} 人", batchCount, expiredUsers.size(), updated);

            // 如果本批不足 batchSize，说明已经是最后一批
            if (expiredUsers.size() < batchSize) {
                break;
            }

            // 批次间休眠，减轻数据库压力
            try {
                Thread.sleep(100);  // 休眠 100ms
            } catch (InterruptedException e) {
                log.error("任务休眠被中断", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 检查是否达到最大批次限制
        if (batchCount >= maxBatches) {
            log.warn("达到最大批次限制 {}，可能存在异常数据或死循环，请检查", maxBatches);
        }

        log.info("VIP 过期降级任务执行完成，共处理 {} 人", totalCount);
    }
}
