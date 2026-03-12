package cn.swiftadmin.common.core.controller;

import cn.swiftadmin.common.core.result.ErrorCode;
import cn.swiftadmin.common.core.result.R;
import cn.swiftadmin.common.core.vo.ErrorCodeDocVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 错误码文档控制器
 * <p>提供错误码字典查询接口，便于前端国际化提示</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
@Slf4j
@Tag(name = "错误码文档", description = "错误码字典查询接口")
@RestController
@RequestMapping("/api/common/error-codes")
public class ErrorCodeDocController {

    /**
     * 获取全部错误码字典
     *
     * @return 错误码列表
     */
    @GetMapping
    @Operation(summary = "获取全部错误码", description = "返回系统所有错误码的字典，便于前端国际化提示")
    public R<List<ErrorCodeDocVO>> getAllErrorCodes() {
        List<ErrorCodeDocVO> errorCodes = Arrays.stream(ErrorCode.values())
                .map(errorCode -> {
                    ErrorCodeDocVO vo = ErrorCodeDocVO.builder()
                            .code(errorCode.getCode())
                            .message(errorCode.getMessage())
                            .build();
                    vo.categorize();
                    return vo;
                })
                .collect(Collectors.toList());

        log.debug("返回错误码字典，共 {} 条", errorCodes.size());
        return R.ok(errorCodes);
    }

    /**
     * 按分类获取错误码
     *
     * @param category 分类（USER/SOCIAL_AUTH/ACCOUNT/SECURITY/COMMON/SYSTEM）
     * @return 错误码列表
     */
    @GetMapping("/category")
    @Operation(summary = "按分类获取错误码", description = "根据错误分类返回对应的错误码")
    public R<List<ErrorCodeDocVO>> getErrorCodesByCategory(String category) {
        List<ErrorCodeDocVO> errorCodes = Arrays.stream(ErrorCode.values())
                .map(errorCode -> {
                    ErrorCodeDocVO vo = ErrorCodeDocVO.builder()
                            .code(errorCode.getCode())
                            .message(errorCode.getMessage())
                            .build();
                    vo.categorize();
                    return vo;
                })
                .filter(vo -> category.equalsIgnoreCase(vo.getCategory()))
                .collect(Collectors.toList());

        log.debug("返回分类 [{}] 错误码，共 {} 条", category, errorCodes.size());
        return R.ok(errorCodes);
    }

    /**
     * 获取错误码详情
     *
     * @param code 错误码
     * @return 错误码详情
     */
    @GetMapping("/detail")
    @Operation(summary = "获取错误码详情", description = "根据错误码查询详细信息")
    public R<ErrorCodeDocVO> getErrorCodeDetail(Integer code) {
        if (code == null) {
            return R.fail(400, "错误码不能为空");
        }

        ErrorCode errorCode = Arrays.stream(ErrorCode.values())
                .filter(e -> e.getCode() == code)
                .findFirst()
                .orElse(null);

        if (errorCode == null) {
            return R.fail(404, "错误码不存在");
        }

        ErrorCodeDocVO vo = ErrorCodeDocVO.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        vo.categorize();

        log.debug("查询错误码详情：code={}, category={}", code, vo.getCategory());
        return R.ok(vo);
    }
}
