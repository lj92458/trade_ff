package com.liujun.trade_ff.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;

/**
 * 验证码 帮助类
 * Created by WuShaotong on 2016/8/8.
 */
public class VerifyCodeUtil {
    private static Logger log = LoggerFactory.getLogger(VerifyCodeUtil.class);
    private static int width = 80;//定义图片的width
    private static int height = 30;//定义图片的height
    private static int codeCount = 4;//定义图片上显示验证码的个数
    private static int fontHeight = 25;     //字符高度
    private static int paddingLeft = 5;    //左侧空余
    private static int codeX = 18;     //字符间距
    private static int codeY = 22;  //字符垂直位置
    private static char[] codeSequence = { '0' , '1', '2', '3', '4', '5', '6', '7', '8', '9' };

    private static Font DEFAULT_FONT = null;

    static {
        String fontPath = VerifyCodeUtil.class.getResource("/").getFile().replaceAll("%20", " ") + "../../fonts/" + "SFSlapstickComicShaded.ttf";
        File fontFile = new File(fontPath);
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(fontFile);
            BufferedInputStream fb = new BufferedInputStream(fi);
            DEFAULT_FONT = Font.createFont(Font.TRUETYPE_FONT, fb);
            DEFAULT_FONT = DEFAULT_FONT.deriveFont(Font.BOLD, fontHeight);
        } catch (Exception e) {
            DEFAULT_FONT = new Font("Fixedsys", Font.BOLD, fontHeight);
        }finally {
            try {
                fi.close();
            } catch (Exception e) {

            }
        }
    }

    public static String generateVerifyCode(OutputStream os){
        // 定义图像buffer
        BufferedImage buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics gd = buffImg.getGraphics();
        // 创建一个随机数生成器类
        Random random = new Random();
        // 将图像填充为白色
        gd.setColor(Color.WHITE);
        gd.fillRect(0, 0, width, height);

        // 创建字体，字体的大小应该根据图片的高度来定。
        //Font font = new Font("Fixedsys", Font.BOLD, fontHeight);
        // 设置字体。
        gd.setFont(DEFAULT_FONT);

        // 画边框。
        gd.setColor(Color.BLACK);
        gd.drawRect(0, 0, width - 1, height - 1);

        // 随机产生40条干扰线，使图象中的认证码不易被其它程序探测到。
        gd.setColor(Color.BLACK);
        int red = 0, green = 0, blue = 0;
        for (int i = 0; i < 40; i++) {
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);
            gd.setColor(new Color(red, green, blue));

            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int xl = random.nextInt(12);
            int yl = random.nextInt(12);
            gd.drawLine(x, y, x + xl, y + yl);
        }

        // randomCode用于保存随机产生的验证码，以便用户登录后进行验证。
        StringBuffer randomCode = new StringBuffer();

        // 随机产生codeCount数字的验证码。
        for (int i = 0; i < codeCount; i++) {
            // 得到随机产生的验证码数字。
            String code = String.valueOf(codeSequence[random.nextInt(codeSequence.length)]);
            // 产生随机的颜色分量来构造颜色值，这样输出的每位数字的颜色值都将不同。
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);

            // 用随机产生的颜色将验证码绘制到图像中。
            gd.setColor(new Color(red, green, blue));
            gd.drawString(code, i * codeX + paddingLeft, codeY);

            // 将产生的四个随机数组合在一起。
            randomCode.append(code);
        }
        try {
            ImageIO.write(buffImg, "jpeg", os);
        } catch (IOException e) {
            log.error("输出图片验证码，异常：", e);
            return null;
        }
        return randomCode.toString();
    }
}
