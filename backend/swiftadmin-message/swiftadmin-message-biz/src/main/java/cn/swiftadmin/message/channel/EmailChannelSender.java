package cn.swiftadmin.message.channel;

import cn.swiftadmin.message.api.channel.MessageContext;
import cn.swiftadmin.message.api.channel.SendResult;
import cn.swiftadmin.message.api.enums.ChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;

/**
 * 邮件渠道发送器
 *
 * @author zhangpengju
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelSender extends AbstractChannelSender {

    private final ObjectMapper objectMapper;

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        try {
            JsonNode config = objectMapper.readTree(context.getChannel().getConfig());
            JavaMailSender mailSender = createMailSender(config);

            String from = config.has("from") ? config.get("from").asText() : "";
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
}