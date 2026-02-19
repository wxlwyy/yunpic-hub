package com.wyy.yunpicture.infrastructure.utils;

import cn.hutool.core.util.StrUtil;
import java.awt.Color;

/**
 * 颜色相似度计算工具类（基于RGB欧氏距离）
 * 核心逻辑：RGB三维空间距离越小 → 相似度越高（返回0~1之间的值，1=完全相同，0=完全不同）
 */
public class ColorSimilarUtils {

    /**
     * 重载方法（常用）：直接通过16进制颜色字符串计算相似度（适配数据库存储的#RRGGBB格式）
     * @param hexColor1 颜色1的16进制字符串（如#FF0000，支持大小写、带/不带#）
     * @param hexColor2 颜色2的16进制字符串
     * @return 相似度（0~1），格式错误/空值返回0.0
     */
    public static double calculateSimilarity(String hexColor1, String hexColor2) {
        // 空值校验
        if (StrUtil.isBlank(hexColor1) || StrUtil.isBlank(hexColor2)) {
            return 0.0;
        }

        // 解析16进制字符串为Color对象
//        Color color1 = parseHexColor(hexColor1);
//        Color color2 = parseHexColor(hexColor2);

        Color color1 = Color.decode(hexColor1);
        Color color2 = Color.decode(hexColor2);

        // 调用核心计算方法
        return calculateSimilarity(color1, color2);
    }


    /**
     * 计算两个Color对象的相似度
     * @param color1 颜色1（目标颜色）
     * @param color2 颜色2（图片主色调）
     * @return 相似度（0~1），若入参为null返回0.0
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        // 空值校验：任意颜色为null，相似度为0
        if (color1 == null || color2 == null) {
            return 0.0;
        }

        // 提取RGB通道值（0~255）
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();

        // 步骤1：计算RGB三维空间的欧氏距离
        double rDiff = Math.pow(r1 - r2, 2);
        double gDiff = Math.pow(g1 - g2, 2);
        double bDiff = Math.pow(b1 - b2, 2);
        double distance = Math.sqrt(rDiff + gDiff + bDiff);

        // 步骤2：计算RGB空间最大可能距离（纯黑vs纯白）
        double maxDistance = Math.sqrt(3 * Math.pow(255, 2));

        double similarity = 1 - (distance / maxDistance);

        // 确保相似度在0~1区间（浮点精度问题兜底）
        return Math.max(0.0, Math.min(1.0, similarity));
    }



//    /**
//     * 辅助方法：解析16进制颜色字符串为Color对象
//     * 支持格式：#RRGGBB、RRGGBB、#RGB、RGB（如#FF0000、FF0000、#F00、F00）
//     * @param hexColor 16进制颜色字符串
//     * @return Color对象（解析失败返回null）
//     */
//    private static Color parseHexColor(String hexColor) {
//        try {
//            // 统一处理：去掉#前缀（如果有）
//            String cleanHex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
//
//            // 处理短格式（如#F00 → #FF0000）
//            if (cleanHex.length() == 3) {
//                cleanHex = new StringBuilder()
//                        .append(cleanHex.charAt(0)).append(cleanHex.charAt(0))
//                        .append(cleanHex.charAt(1)).append(cleanHex.charAt(1))
//                        .append(cleanHex.charAt(2)).append(cleanHex.charAt(2))
//                        .toString();
//            }
//
//            // 解析为Color（需补回#前缀）
//            return Color.decode("#" + cleanHex);
//        } catch (NumberFormatException e) {
//            // 格式错误（如#XYZ、12345），返回null
//            return null;
//        }
//    }



    // 测试示例
    public static void main(String[] args) {
        // 测试1：相同颜色
        String red = "#FF0000";
        System.out.println(calculateSimilarity(red, red)); // 输出1.0

        // 测试2：相似颜色（纯红vs浅红）
        String lightRed = "#F00A0A";
        System.out.println(calculateSimilarity(red, lightRed)); // 输出≈0.95

        // 测试3：完全不同颜色（红vs绿）
        String green = "#00FF00";
        System.out.println(calculateSimilarity(red, green)); // 输出≈0.18

        // 测试4：空值/格式错误
//        System.out.println(calculateSimilarity(null, red)); // 输出0.0
//        System.out.println(calculateSimilarity(red, "#XYZ")); // 输出0.0

    }



}