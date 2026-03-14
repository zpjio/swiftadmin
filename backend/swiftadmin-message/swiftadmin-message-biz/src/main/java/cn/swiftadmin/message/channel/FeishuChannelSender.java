package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.enums.ChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 飞书渠道发送器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuChannelSender extends AbstractChannelSender {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ChannelType getChannelType() {
        return ChannelType.FEISHU;
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        try {
            JsonNode config = objectMapper.readTree(context.getChannel().getConfig());
            String webhookUrl = config.get("webhookUrl").asText();
            String secret = config.has("secret") ? config.get("secret").asText() : null;

            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "text");

            Map<String, String> content = new HashMap<>();
            content.put("text", context.getContent());
            message.put("content", content);

            // 如果配置了签名，需要添加签名
            if (StringUtils.hasText(secret)) {
                long timestamp = System.currentTimeMillis() / 1000;
                String sign = genSign(secret, timestamp);
                message.put("timestamp", timestamp);
                message.put("sign", sign);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String messageId = UUID.randomUUID().toString().replace("-", "");
                return SendResult.success(messageId, context.getTo(), null);
            } else {
                return SendResult.fail(context.getTo(), "飞书发送失败: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("飞书发送失败: to={}", context.getTo(), e);
            return SendResult.fail(context.getTo(), e.getMessage());
        }
    }

    @Override
    protected boolean doHealthCheck(String config) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            String webhookUrl = configNode.get("webhookUrl").asText();
            return webhookUrl != null && webhookUrl.startsWith("https://open.feishu.cn");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SendResult testSend(String config, String to) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            String webhookUrl = configNode.get("webhookUrl").asText();
            String secret = configNode.has("secret") ? configNode.get("secret").asText() : null;

            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "text");

            Map<String, String> content = new HashMap<>();
            content.put("text", "【SwiftAdmin】这是一条测试消息，来自消息推送模块。");
            message.put("content", content);

            if (StringUtils.hasText(secret)) {
                long timestamp = System.currentTimeMillis() / 1000;
                String sign = genSign(secret, timestamp);
                message.put("timestamp", timestamp);
                message.put("sign", sign);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);

            return SendResult.success(null, to, null);
        } catch (Exception e) {
            return SendResult.fail(to, e.getMessage());
        }
    }

    private String genSign(String secret, long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
    }
}