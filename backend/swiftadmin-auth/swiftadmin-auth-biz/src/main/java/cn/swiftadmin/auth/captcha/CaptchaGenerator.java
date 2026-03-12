package cn.swiftadmin.auth.captcha;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

/**
 * 图形验证码生成器
 * <p>生成随机字母数字验证码图片</p>
 *
 * @author zhangpengju
 * @since 2026-03-10
 */
public class CaptchaGenerator {

    /** 验证码字符集 */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    /** 图片宽度 */
    private static final int WIDTH = 120;

    /** 图片高度 */
    private static final int HEIGHT = 40;

    /** 验证码长度 */
    private static final int CODE_LENGTH = 4;

    /** 干扰线数量 */
    private static final int LINE_COUNT = 5;

    private static final Random RANDOM = new Random();

    /**
     * 生成验证码
     *
     * @return 验证码文本
     */
    public static String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    /**
     * 生成验证码图片
     *
     * @param code 验证码文本
     * @return Base64 编码的图片
     */
    public static String generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制干扰线
        for (int i = 0; i < LINE_COUNT; i++) {
            g.setColor(getRandomColor(100, 200));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                    RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        // 绘制验证码字符
        Font font = new Font("Arial", Font.BOLD, 24);
        g.setFont(font);
        for (int i = 0; i < code.length(); i++) {
            g.setColor(getRandomColor(20, 130));
            g.drawString(String.valueOf(code.charAt(i)), 20 + i * 25, 28);
        }

        // 添加噪点
        for (int i = 0; i < 50; i++) {
            g.setColor(getRandomColor(100, 255));
            g.fillOval(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
        }

        g.dispose();

        // 转换为 Base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }

    /**
     * 获取随机颜色
     */
    private static Color getRandomColor(int min, int max) {
        int r = min + RANDOM.nextInt(max - min);
        int g = min + RANDOM.nextInt(max - min);
        int b = min + RANDOM.nextInt(max - min);
        return new Color(r, g, b);
    }
}