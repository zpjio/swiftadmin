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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook 渠道发送器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookChannelSender extends AbstractChannelSender {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WEBHOOK;
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        try {
            JsonNode config = objectMapper.readTree(context.getChannel().getConfig());
            String webhookUrl = config.get("webhookUrl").asText();

            // 构建请求体
            String requestBody = buildRequestBody(context);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 添加自定义请求头
            if (config.has("headers")) {
                JsonNode headersNode = config.get("headers");
                headersNode.fields().forEachRemaining(entry -> {
                    headers.set(entry.getKey(), entry.getValue().asText());
                });
            }

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String messageId = UUID.randomUUID().toString().replace("-", "");
                return SendResult.success(messageId, context.getTo(), null);
            } else {
                return SendResult.fail(context.getTo(), "Webhook调用失败: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Webhook发送失败: to={}", context.getTo(), e);
            return SendResult.fail(context.getTo(), e.getMessage());
        }
    }

    @Override
    protected boolean doHealthCheck(String config) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            String webhookUrl = configNode.get("webhookUrl").asText();
            return webhookUrl != null && (webhookUrl.startsWith("http://") || webhookUrl.startsWith("https://"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SendResult testSend(String config, String to) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            String webhookUrl = configNode.get("webhookUrl").asText();

            Map<String, Object> testBody = new HashMap<>();
            testBody.put("test", true);
            testBody.put("message", "SwiftAdmin Webhook 测试");
            testBody.put("timestamp", System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(testBody, headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);

            return SendResult.success(null, to, null);
        } catch (Exception e) {
            return SendResult.fail(to, e.getMessage());
        }
    }

    private String buildRequestBody(MessageContext context) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("messageId", context.getMessageId());
        body.put("to", context.getTo());
        body.put("subject", context.getSubject());
        body.put("content", context.getContent());
        body.put("bizType", context.getBizType());
        body.put("bizId", context.getBizId());
        body.put("timestamp", System.currentTimeMillis());

        return objectMapper.writeValueAsString(body);
    }
}