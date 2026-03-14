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
 * 企业微信渠道发送器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomChannelSender extends AbstractChannelSender {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WECOM;
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        try {
            JsonNode config = objectMapper.readTree(context.getChannel().getConfig());
            String webhookUrl = config.get("webhookUrl").asText();

            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "text");

            Map<String, String> text = new HashMap<>();
            text.put("content", context.getContent());
            message.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String messageId = UUID.randomUUID().toString().replace("-", "");
                return SendResult.success(messageId, context.getTo(), null);
            } else {
                return SendResult.fail(context.getTo(), "企业微信发送失败: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("企业微信发送失败: to={}", context.getTo(), e);
            return SendResult.fail(context.getTo(), e.getMessage());
        }
    }

    @Override
    protected boolean doHealthCheck(String config) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            String webhookUrl = configNode.get("webhookUrl").asText();
            // 简单检查URL格式
            return webhookUrl != null && webhookUrl.startsWith("https://qyapi.weixin.qq.com");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SendResult testSend(String config, String to) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            String webhookUrl = configNode.get("webhookUrl").asText();

            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "text");

            Map<String, String> text = new HashMap<>();
            text.put("content", "【SwiftAdmin】这是一条测试消息，来自消息推送模块。");
            message.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);

            return SendResult.success(null, to, null);
        } catch (Exception e) {
            return SendResult.fail(to, e.getMessage());
        }
    }
}