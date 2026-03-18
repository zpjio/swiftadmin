package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.enums.ChannelType;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信渠道发送器
 * <p>支持阿里云、腾讯云短信服务</p>
 *
 * <h3>阿里云配置格式（JSON）：</h3>
 * <pre>
 * {
 *   "provider": "aliyun",
 *   "accessKeyId": "xxx",
 *   "accessKeySecret": "xxx",
 *   "signName": "签名名称",
 *   "templateCode": "SMS_XXX"
 * }
 * </pre>
 *
 * <h3>腾讯云配置格式（JSON）：</h3>
 * <pre>
 * {
 *   "provider": "tencent",
 *   "secretId": "xxx",
 *   "secretKey": "xxx",
 *   "appId": "1400xxxxx",
 *   "signName": "签名名称",
 *   "templateCode": "123456",
 *   "region": "ap-guangzhou"  // 可选，默认 ap-guangzhou
 * }
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-16
 */
@Slf4j
@Component
public class SmsChannelSender extends AbstractChannelSender {

    private final ObjectMapper objectMapper;

    /** SDK 客户端缓存（按配置缓存） */
    private final ConcurrentHashMap<String, Client> aliyunClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SmsClient> tencentClients = new ConcurrentHashMap<>();

    /** 短信服务商类型 */
    private static final String PROVIDER_ALIYUN = "aliyun";
    private static final String PROVIDER_TENCENT = "tencent";

    public SmsChannelSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SMS;
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        try {
            JsonNode config = objectMapper.readTree(context.getChannel().getConfig());
            String provider = config.has("provider") ? config.get("provider").asText() : PROVIDER_ALIYUN;
            String to = context.getTo();
            String templateCode = getTemplateCode(config, context);
            Map<String, Object> variables = context.getVariables();

            // 根据服务商调用不同的 SDK
            SendResult result;
            if (PROVIDER_ALIYUN.equalsIgnoreCase(provider)) {
                result = sendByAliyun(config, to, templateCode, variables);
            } else if (PROVIDER_TENCENT.equalsIgnoreCase(provider)) {
                result = sendByTencent(config, to, templateCode, variables);
            } else {
                log.error("不支持的短信服务商: {}", provider);
                return SendResult.fail(to, "不支持的短信服务商: " + provider);
            }

            return result;
        } catch (Exception e) {
            log.error("短信发送失败: to={}", context.getTo(), e);
            return SendResult.fail(context.getTo(), e.getMessage());
        }
    }

    /**
     * 获取模板代码
     * <p>优先使用模板中的云服务商模板码，其次使用渠道默认模板</p>
     */
    private String getTemplateCode(JsonNode config, MessageContext context) {
        // 优先使用模板中的云服务商模板码（providerTemplateCode）
        if (context.getTemplate() != null && StringUtils.hasText(context.getTemplate().getProviderTemplateCode())) {
            return context.getTemplate().getProviderTemplateCode();
        }
        // 其次使用渠道配置中的默认模板
        if (config.has("templateCode") && StringUtils.hasText(config.get("templateCode").asText())) {
            return config.get("templateCode").asText();
        }
        return null;
    }

    /**
     * 阿里云短信发送
     */
    private SendResult sendByAliyun(JsonNode config, String phone, String templateCode, Map<String, Object> variables) {
        // 参数校验
        if (!config.has("accessKeyId") || !config.has("accessKeySecret") || !config.has("signName")) {
            return SendResult.fail(phone, "阿里云短信配置缺失：accessKeyId、accessKeySecret 或 signName");
        }
        String accessKeyId = config.get("accessKeyId").asText();
        String accessKeySecret = config.get("accessKeySecret").asText();
        String signName = config.get("signName").asText();

        // 检查必要参数是否为空
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret) || !StringUtils.hasText(signName)) {
            return SendResult.fail(phone, "阿里云短信配置无效：accessKeyId、accessKeySecret 或 signName 为空");
        }
        if (!StringUtils.hasText(templateCode)) {
            return SendResult.fail(phone, "短信模板编码缺失");
        }

        try {
            // 获取或创建客户端
            String cacheKey = accessKeyId + ":" + accessKeySecret;
            Client client = aliyunClients.computeIfAbsent(cacheKey, k -> {
                try {
                    Config clientConfig = new Config()
                            .setAccessKeyId(accessKeyId)
                            .setAccessKeySecret(accessKeySecret)
                            .setEndpoint("dysmsapi.aliyuncs.com");
                    return new Client(clientConfig);
                } catch (Exception e) {
                    log.error("创建阿里云短信客户端失败", e);
                    throw new RuntimeException("创建阿里云短信客户端失败: " + e.getMessage(), e);
                }
            });

            // 构建请求
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(objectMapper.writeValueAsString(variables));

            // 发送短信
            SendSmsResponse response = client.sendSms(request);

            // 解析结果
            if (response.getBody() != null && "OK".equals(response.getBody().getCode())) {
                String bizId = response.getBody().getBizId();
                log.info("阿里云短信发送成功: phone={}, bizId={}", phone, bizId);
                return SendResult.success(bizId != null ? bizId : generateMessageId(), phone);
            } else {
                String errorCode = response.getBody() != null ? response.getBody().getCode() : "UNKNOWN";
                String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "未知错误";
                log.warn("阿里云短信发送失败: phone={}, code={}, message={}", phone, errorCode, errorMsg);
                return SendResult.fail(phone, errorMsg);
            }
        } catch (Exception e) {
            log.error("阿里云短信发送异常: phone={}", phone, e);
            return SendResult.fail(phone, "短信发送异常: " + e.getMessage());
        }
    }

    /**
     * 腾讯云短信发送
     */
    private SendResult sendByTencent(JsonNode config, String phone, String templateCode, Map<String, Object> variables) {
        // 参数校验
        if (!config.has("secretId") || !config.has("secretKey") || !config.has("signName")) {
            return SendResult.fail(phone, "腾讯云短信配置缺失：secretId、secretKey 或 signName");
        }
        String secretId = config.get("secretId").asText();
        String secretKey = config.get("secretKey").asText();
        String signName = config.get("signName").asText();
        String appId = config.has("appId") ? config.get("appId").asText() : "";
        String region = config.has("region") ? config.get("region").asText() : "ap-guangzhou";

        // 检查必要参数是否为空
        if (!StringUtils.hasText(secretId) || !StringUtils.hasText(secretKey) || !StringUtils.hasText(signName)) {
            return SendResult.fail(phone, "腾讯云短信配置无效：secretId、secretKey 或 signName 为空");
        }
        if (!StringUtils.hasText(appId)) {
            return SendResult.fail(phone, "腾讯云短信配置缺失：appId");
        }
        if (!StringUtils.hasText(templateCode)) {
            return SendResult.fail(phone, "短信模板编码缺失");
        }

        try {
            // 获取或创建客户端
            String cacheKey = secretId + ":" + secretKey + ":" + region;
            SmsClient client = tencentClients.computeIfAbsent(cacheKey, k -> {
                try {
                    Credential cred = new Credential(secretId, secretKey);
                    HttpProfile httpProfile = new HttpProfile();
                    httpProfile.setEndpoint("sms.tencentcloudapi.com");
                    ClientProfile clientProfile = new ClientProfile();
                    clientProfile.setHttpProfile(httpProfile);
                    return new SmsClient(cred, region, clientProfile);
                } catch (Exception e) {
                    log.error("创建腾讯云短信客户端失败", e);
                    throw new RuntimeException("创建腾讯云短信客户端失败: " + e.getMessage(), e);
                }
            });

            // 构建请求
            com.tencentcloudapi.sms.v20210111.models.SendSmsRequest req = new com.tencentcloudapi.sms.v20210111.models.SendSmsRequest();
            req.setSmsSdkAppId(appId);
            req.setSignName(signName);
            req.setTemplateId(templateCode);
            req.setPhoneNumberSet(new String[]{formatPhoneForTencent(phone)});
            req.setTemplateParamSet(convertVariablesToArray(variables));

            // 发送短信
            com.tencentcloudapi.sms.v20210111.models.SendSmsResponse response = client.SendSms(req);

            // 解析结果
            if (response.getSendStatusSet() != null && response.getSendStatusSet().length > 0) {
                var status = response.getSendStatusSet()[0];
                if ("Ok".equalsIgnoreCase(status.getCode())) {
                    log.info("腾讯云短信发送成功: phone={}, serialNo={}", phone, status.getSerialNo());
                    return SendResult.success(status.getSerialNo(), phone);
                } else {
                    log.warn("腾讯云短信发送失败: phone={}, code={}, message={}", phone, status.getCode(), status.getMessage());
                    return SendResult.fail(phone, status.getMessage());
                }
            } else {
                log.warn("腾讯云短信响应异常: phone={}", phone);
                return SendResult.fail(phone, "短信发送响应异常");
            }
        } catch (TencentCloudSDKException e) {
            log.error("腾讯云短信发送异常: phone={}", phone, e);
            return SendResult.fail(phone, "短信发送异常: " + e.getMessage());
        }
    }

    /**
     * 转换变量为腾讯云格式（数组）
     */
    private String[] convertVariablesToArray(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return new String[0];
        }
        return variables.values().stream()
                .map(v -> v != null ? v.toString() : "")
                .toArray(String[]::new);
    }

    /**
     * 格式化手机号为腾讯云格式（需要 +86 前缀）
     */
    private String formatPhoneForTencent(String phone) {
        if (phone == null) {
            return null;
        }
        // 如果已经有国际区号前缀，直接返回
        if (phone.startsWith("+")) {
            return phone;
        }
        // 添加中国大陆区号前缀
        return "+86" + phone;
    }

    @Override
    protected boolean doHealthCheck(String config) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            String provider = configNode.has("provider") ? configNode.get("provider").asText() : PROVIDER_ALIYUN;

            // 检查必要配置项是否存在且非空
            if (PROVIDER_ALIYUN.equalsIgnoreCase(provider)) {
                return hasNonEmptyField(configNode, "accessKeyId")
                        && hasNonEmptyField(configNode, "accessKeySecret")
                        && hasNonEmptyField(configNode, "signName");
            } else if (PROVIDER_TENCENT.equalsIgnoreCase(provider)) {
                return hasNonEmptyField(configNode, "secretId")
                        && hasNonEmptyField(configNode, "secretKey")
                        && hasNonEmptyField(configNode, "signName")
                        && hasNonEmptyField(configNode, "appId");
            }
            return false;
        } catch (Exception e) {
            log.error("短信渠道配置解析失败", e);
            return false;
        }
    }

    /**
     * 检查字段是否存在且值非空
     */
    private boolean hasNonEmptyField(JsonNode node, String fieldName) {
        return node.has(fieldName) && StringUtils.hasText(node.get(fieldName).asText());
    }

    @Override
    public SendResult testSend(String config, String to) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            Map<String, Object> variables = Map.of("code", "123456");

            // 获取模板编码
            String templateCode = configNode.has("templateCode") ? configNode.get("templateCode").asText() : "SMS_000000000";

            String provider = configNode.has("provider") ? configNode.get("provider").asText() : PROVIDER_ALIYUN;
            if (PROVIDER_ALIYUN.equalsIgnoreCase(provider)) {
                return sendByAliyun(configNode, to, templateCode, variables);
            } else {
                return sendByTencent(configNode, to, templateCode, variables);
            }
        } catch (Exception e) {
            return SendResult.fail(to, e.getMessage());
        }
    }

    private String generateMessageId() {
        return "SMS" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 清理缓存的客户端（配置变更时调用）
     */
    public void clearClientCache() {
        aliyunClients.clear();
        tencentClients.clear();
        log.info("短信客户端缓存已清理");
    }
}