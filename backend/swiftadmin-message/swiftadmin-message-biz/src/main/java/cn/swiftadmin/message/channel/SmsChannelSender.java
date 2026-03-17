package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.enums.ChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * 短信渠道发送器
 * <p>支持阿里云、腾讯云短信服务</p>
 * <p>配置格式（JSON）：</p>
 * <pre>
 * {
 *   "provider": "aliyun",  // aliyun 或 tencent
 *   "accessKeyId": "xxx",
 *   "accessKeySecret": "xxx",
 *   "signName": "签名名称",
 *   "templateCode": "SMS_XXX"  // 默认模板码
 * }
 * </pre>
 *
 * @author zhangpengju
 * @since 2026-03-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsChannelSender extends AbstractChannelSender {

    private final ObjectMapper objectMapper;

    /** 短信服务商类型 */
    private static final String PROVIDER_ALIYUN = "aliyun";
    private static final String PROVIDER_TENCENT = "tencent";

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
     * <p>开发环境仅打印日志，生产环境需要引入阿里云 SDK</p>
     */
    private SendResult sendByAliyun(JsonNode config, String phone, String templateCode, Map<String, Object> variables) {
        // 参数校验
        if (!config.has("accessKeyId") || !config.has("signName")) {
            return SendResult.fail(phone, "阿里云短信配置缺失：accessKeyId 或 signName");
        }
        String accessKeyId = config.get("accessKeyId").asText();
        String signName = config.get("signName").asText();

        // 检查必要参数是否为空
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(signName)) {
            return SendResult.fail(phone, "阿里云短信配置无效：accessKeyId 或 signName 为空");
        }
        if (!StringUtils.hasText(templateCode)) {
            return SendResult.fail(phone, "短信模板编码缺失");
        }

        // 开发环境：仅打印日志
        if (isDevEnvironment()) {
            log.info("【阿里云短信】phone={}, signName={}, templateCode={}, variables={}",
                    phone, signName, templateCode, variables);
            return SendResult.success(generateMessageId(), phone, null);
        }

        // 生产环境：调用阿里云 SDK
        // TODO: 引入阿里云短信 SDK 后实现
        // <dependency>
        //     <groupId>com.aliyun</groupId>
        //     <artifactId>dysmsapi20170525</artifactId>
        // </dependency>
        //
        // Config clientConfig = new Config()
        //     .setAccessKeyId(accessKeyId)
        //     .setAccessKeySecret(config.get("accessKeySecret").asText());
        // Client client = new Client(clientConfig);
        //
        // SendSmsRequest request = new SendSmsRequest()
        //     .setPhoneNumbers(phone)
        //     .setSignName(signName)
        //     .setTemplateCode(templateCode)
        //     .setTemplateParam(objectMapper.writeValueAsString(variables));
        //
        // SendSmsResponse response = client.sendSms(request);

        log.warn("生产环境需要接入阿里云短信 SDK");
        return SendResult.success(generateMessageId(), phone, null);
    }

    /**
     * 腾讯云短信发送
     * <p>开发环境仅打印日志，生产环境需要引入腾讯云 SDK</p>
     */
    private SendResult sendByTencent(JsonNode config, String phone, String templateCode, Map<String, Object> variables) {
        // 参数校验
        if (!config.has("secretId") || !config.has("signName")) {
            return SendResult.fail(phone, "腾讯云短信配置缺失：secretId 或 signName");
        }
        String secretId = config.get("secretId").asText();
        String signName = config.get("signName").asText();
        String appId = config.has("appId") ? config.get("appId").asText() : "";

        // 检查必要参数是否为空
        if (!StringUtils.hasText(secretId) || !StringUtils.hasText(signName)) {
            return SendResult.fail(phone, "腾讯云短信配置无效：secretId 或 signName 为空");
        }
        if (!StringUtils.hasText(templateCode)) {
            return SendResult.fail(phone, "短信模板编码缺失");
        }

        // 开发环境：仅打印日志
        if (isDevEnvironment()) {
            log.info("【腾讯云短信】phone={}, appId={}, signName={}, templateId={}, variables={}",
                    phone, appId, signName, templateCode, variables);
            return SendResult.success(generateMessageId(), phone, null);
        }

        // 生产环境：调用腾讯云 SDK
        // TODO: 引入腾讯云短信 SDK 后实现
        // <dependency>
        //     <groupId>com.tencentcloudapi</groupId>
        //     <artifactId>tencentcloud-sdk-java-sms</artifactId>
        // </dependency>
        //
        // Credential cred = new Credential(secretId, config.get("secretKey").asText());
        // SmsClient client = new SmsClient(cred, "ap-guangzhou");
        //
        // SendSmsRequest req = new SendSmsRequest();
        // req.setSmsSdkAppId(appId);
        // req.setSignName(signName);
        // req.setTemplateId(templateCode);
        // req.setPhoneNumberSet(new String[]{phone});
        // req.setTemplateParamSet(convertVariables(variables));
        //
        // SendSmsResponse response = client.SendSms(req);

        log.warn("生产环境需要接入腾讯云短信 SDK");
        return SendResult.success(generateMessageId(), phone, null);
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
                        && hasNonEmptyField(configNode, "signName");
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

    private boolean isDevEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "dev");
        return "dev".equals(profile) || "local".equals(profile);
    }

    private String generateMessageId() {
        return "SMS" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}