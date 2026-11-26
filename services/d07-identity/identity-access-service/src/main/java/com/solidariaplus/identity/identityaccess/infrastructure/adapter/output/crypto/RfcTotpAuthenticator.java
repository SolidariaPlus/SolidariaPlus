package com.solidariaplus.identity.identityaccess.infrastructure.adapter.output.crypto;

import com.solidariaplus.identity.identityaccess.application.port.output.TotpAuthenticator;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.stream.LongStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
class RfcTotpAuthenticator implements TotpAuthenticator {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20;
    private static final long STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int MODULUS = 1_000_000;
    private static final long ALLOWED_DRIFT_STEPS = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String newSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        RANDOM.nextBytes(secret);
        return encode(secret);
    }

    @Override
    public boolean verify(String secret, String code, Instant now) {
        byte[] key = decode(secret);
        long step = now.getEpochSecond() / STEP_SECONDS;
        byte[] presented = code.getBytes(StandardCharsets.US_ASCII);
        return LongStream.rangeClosed(step - ALLOWED_DRIFT_STEPS, step + ALLOWED_DRIFT_STEPS)
                .anyMatch(counter -> MessageDigest.isEqual(codeAt(key, counter), presented));
    }

    private static byte[] codeAt(byte[] key, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash =
                    mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = (ByteBuffer.wrap(hash, offset, Integer.BYTES).getInt() & 0x7FFFFFFF) % MODULUS;
            return String.format("%0" + DIGITS + "d", binary).getBytes(StandardCharsets.US_ASCII);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(byte[] data) {
        StringBuilder encoded = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xFF);
            bits += 8;
            while (bits >= 5) {
                encoded.append(BASE32.charAt((buffer >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            encoded.append(BASE32.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return encoded.toString();
    }

    private static byte[] decode(String encoded) {
        ByteBuffer decoded = ByteBuffer.allocate(encoded.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char symbol : encoded.toCharArray()) {
            buffer = (buffer << 5) | BASE32.indexOf(symbol);
            bits += 5;
            if (bits >= 8) {
                decoded.put((byte) (buffer >> (bits - 8)));
                bits -= 8;
            }
        }
        return decoded.array();
    }
}
