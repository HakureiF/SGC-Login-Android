package com.example.sgcdemo2.util;

import org.apache.commons.codec.binary.Base64;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesUtil {
    //AES 16位
    //AES：加密方式   CBC：工作模式   PKCS5Padding：填充模式
    private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";
    private static final String AES_KEY = "test";
    /**
     * The constant CODE_TYPE.
     */
    public static final String CODE_TYPE = "UTF-8";  // 编码方式


    public static String encrypt(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        try {
            /*
             * 新建一个密码编译器的实例，由三部分构成，用"/"分隔，分别代表如下
             * 1. 加密的类型(如AES，DES，RC2等)
             * 2. 模式(AES中包含ECB，CBC，CFB，CTR，CTS等)
             * 3. 补码方式(包含nopadding/PKCS5Padding等等)
             * 依据这三个参数可以创建很多种加密方式
             */
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            //偏移量
            IvParameterSpec zeroIv = new IvParameterSpec(AES_KEY.getBytes(CODE_TYPE));
            byte[] byteContent = content.getBytes(CODE_TYPE);
            //使用加密秘钥
            SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY.getBytes(CODE_TYPE), AES);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, zeroIv); // 初始化为加密模式的密码器
            byte[] result = cipher.doFinal(byteContent); // 加密
            return Base64.encodeBase64String(result); //通过Base64转码返回
        } catch (Exception ex) {
            Logger.getLogger(AesUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
