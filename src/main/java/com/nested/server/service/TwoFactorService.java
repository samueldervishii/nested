package com.nested.server.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
public class TwoFactorService {

    private static final String ISSUER = "Nested";
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    public TwoFactorService() {
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    /**
     * Generate a new TOTP secret
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Generate QR code data URL for authenticator app setup
     */
    public String generateQrCodeDataUrl(String secret, String username) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] imageData = qrGenerator.generate(data);
            String mimeType = qrGenerator.getImageMimeType();
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Verify a TOTP code against a secret
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != 6) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code);
    }

    /**
     * Generate manual entry key (formatted secret for display)
     */
    public String getManualEntryKey(String secret) {
        // Format the secret in groups of 4 for easier manual entry
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i += 4) {
            if (i > 0) formatted.append(" ");
            formatted.append(secret, i, Math.min(i + 4, secret.length()));
        }
        return formatted.toString();
    }
}
