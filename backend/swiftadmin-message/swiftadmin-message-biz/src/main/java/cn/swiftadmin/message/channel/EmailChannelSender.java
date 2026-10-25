package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.enums.ChannelType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件渠道发送器
 * <p>使用本地缓存避免每次发送都创建新的 JavaMailSender</p>
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelSender extends AbstractChannelSender {

    private final ObjectMapper objectMapper;

    /** JavaMailSender 缓存：渠道ID -> JavaMailSender */
    private final Map<Long, JavaMailSender> senderCache = new ConcurrentHashMap<>();

    /** 渠道配置缓存：渠道ID -> config JSON string，用于检测配置变更 */
    private final Map<Long, String> configCache = new ConcurrentHashMap<>();

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        try {
            Long channelId = context.getChannel().getId();
            String config = context.getChannel().getConfig();

            JavaMailSender mailSender = getOrCreateMailSender(channelId, config);

            JsonNode configNode = objectMapper.readTree(config);
            String from = configNode.has("from") ? configNode.get("from").asText() : "";
            String subject = context.getSubject();
            String content = context.getContent();
            String to = context.getTo();

            if (!StringUtils.hasText(subject)) {
                subject = "通知";
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true = HTML format

            mailSender.send(mimeMessage);

            String messageId = UUID.randomUUID().toString().replace("-", "");
            return SendResult.success(messageId, to, null);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}", context.getTo(), e);
            return SendResult.fail(context.getTo(), e.getMessage());
        }
    }

    @Override
    protected boolean doHealthCheck(String config) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) createMailSender(configNode);
            mailSender.testConnection();
            return true;
        } catch (Exception e) {
            log.error("邮件服务器连接测试失败", e);
            return false;
        }
    }

    @Override
    public SendResult testSend(String config, String to) {
        try {
            JsonNode configNode = objectMapper.readTree(config);
            JavaMailSender mailSender = createMailSender(configNode);

            String from = configNode.has("from") ? configNode.get("from").asText() : "";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("SwiftAdmin 邮件渠道测试");
            message.setText("这是一封测试邮件，来自 SwiftAdmin 消息推送模块。");

            mailSender.send(message);
            return SendResult.success(null, to, null);
        } catch (Exception e) {
            return SendResult.fail(to, e.getMessage());
        }
    }

    /**
     * 获取或创建 JavaMailSender（带缓存）
     */
    private JavaMailSender getOrCreateMailSender(Long channelId, String config) {
        // 检查配置是否变更
        String cachedConfig = configCache.get(channelId);
        if (cachedConfig != null && cachedConfig.equals(config)) {
            JavaMailSender cached = senderCache.get(channelId);
            if (cached != null) {
                return cached;
            }
        }

        // 配置变更或首次创建
        try {
            JsonNode configNode = objectMapper.readTree(config);
            JavaMailSender mailSender = createMailSender(configNode);
            senderCache.put(channelId, mailSender);
            configCache.put(channelId, config);
            log.debug("创建/更新 JavaMailSender 缓存: channelId={}", channelId);
            return mailSender;
        } catch (Exception e) {
            log.error("解析邮件配置失败: channelId={}", channelId, e);
            throw new RuntimeException("邮件配置解析失败", e);
        }
    }

    /**
     * 创建 JavaMailSender
     */
    private JavaMailSender createMailSender(JsonNode config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.get("host").asText());
        mailSender.setPort(config.get("port").asInt());
        mailSender.setUsername(config.get("username").asText());
        mailSender.setPassword(config.get("password").asText());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        boolean sslEnabled = config.has("sslEnabled") && config.get("sslEnabled").asBoolean();
        if (sslEnabled) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        return mailSender;
    }

    /**
     * 清除指定渠道的缓存
     */
    public void clearCache(Long channelId) {
        senderCache.remove(channelId);
        configCache.remove(channelId);
        log.info("清除邮件发送器缓存: channelId={}", channelId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        senderCache.clear();
        configCache.clear();
        log.info("清除所有邮件发送器缓存");
    }
}