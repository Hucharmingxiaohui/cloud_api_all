package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.util.Base64;

/** EUA transport only: the encrypted response contains the entire original business result. */
@Component
public class CqDockTransportCodec {
    private static final Provider PROVIDER = new BouncyCastleProvider();
    private final CqDockProperties properties;

    public CqDockTransportCodec(CqDockProperties properties) { this.properties = properties; }

    public String encrypt(String plain) { return crypt(plain, Cipher.ENCRYPT_MODE); }
    public String decrypt(String cipher) { return crypt(cipher, Cipher.DECRYPT_MODE); }

    private String crypt(String input, int mode) {
        String key = properties.getSm4Key();
        byte[] keyBytes = key == null ? null : key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes == null || keyBytes.length != 16) {
            throw new IllegalStateException("cq-dock.sm4-key must contain exactly 16 UTF-8 bytes");
        }
        try {
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", PROVIDER);
            cipher.init(mode, new SecretKeySpec(keyBytes, "SM4"));
            if (mode == Cipher.ENCRYPT_MODE) {
                return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));
            }
            byte[] decoded = cipher.doFinal(Base64.getDecoder().decode(input));
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(decoded)).toString();
        } catch (Exception ignored) {
            throw new IllegalArgumentException("EUA数据加解密失败");
        }
    }

    public JSONObject request(Object body) {
        JSONObject envelope = new JSONObject(); envelope.put("encrypt", true);
        envelope.put("data", encrypt(body == null ? "{}" : JSON.toJSONString(body)));
        return envelope;
    }

    public JSONObject response(Object body) {
        JSONObject envelope = new JSONObject(); envelope.put("encrypt", true); envelope.put("code", 200);
        envelope.put("data", encrypt(JSON.toJSONString(body, SerializerFeature.WriteMapNullValue)));
        return envelope;
    }

    public JSONObject decodeRequest(String raw) {
        JSONObject envelope = JSON.parseObject(raw);
        requireEnvelope(envelope, false);
        JSONObject original = JSON.parseObject(decrypt(envelope.getString("data")));
        if (original == null) throw new IllegalArgumentException("请求数据解密失败");
        return original;
    }

    public JSONObject decodeResponse(String raw) {
        JSONObject envelope = JSON.parseObject(raw);
        if (envelope == null) throw new IllegalArgumentException("EUA响应为空");
        if (!Boolean.TRUE.equals(envelope.get("encrypt"))) {
            // Authentication can fail before the remote transport handler runs. Never accept plaintext success.
            Integer code = envelope.getInteger("code");
            if (code != null && code != 200 && !Boolean.TRUE.equals(envelope.get("success"))) {
                envelope.put("success", false); return envelope;
            }
            throw new IllegalArgumentException("EUA响应未加密");
        }
        requireEnvelope(envelope, true);
        JSONObject original = JSON.parseObject(decrypt(envelope.getString("data")));
        if (original == null || !(original.get("code") instanceof Number)
                || !(original.get("success") instanceof Boolean)) {
            throw new IllegalArgumentException("EUA业务响应格式错误");
        }
        return original;
    }

    private void requireEnvelope(JSONObject envelope, boolean response) {
        if (envelope == null || !Boolean.TRUE.equals(envelope.get("encrypt"))
                || !(envelope.get("data") instanceof String) || envelope.getString("data").isEmpty()
                || (response && !Integer.valueOf(200).equals(envelope.getInteger("code")))) {
            throw new IllegalArgumentException("EUA加密报文格式错误");
        }
    }
}
