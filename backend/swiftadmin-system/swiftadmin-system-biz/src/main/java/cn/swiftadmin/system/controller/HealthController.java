package cn.swiftadmin.system.controller;

import cn.swiftadmin.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * <p>提供系统健康状态检查接口</p>
 * <p>用于负载均衡器的健康检测和系统监控</p>
 *
 * @author zhangpengju
 * @since 2026-03-09
 */
@Tag(name = "健康检查", description = "系统健康状态检查接口")
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 健康检查接口
     * <p>返回系统当前健康状态</p>
     *
     * @return 健康状态信息
     */
    @Operation(summary = "健康检查", description = "返回系统当前健康状态")
    @GetMapping
    public R<String> health() {
        return R.ok("ok");
    }

}
