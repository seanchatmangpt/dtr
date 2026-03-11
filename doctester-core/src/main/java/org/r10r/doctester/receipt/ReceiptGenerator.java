/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.r10r.doctester.receipt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.r10r.doctester.metadata.DocMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate blockchain-style receipts for document provenance.
 *
 * Computes SHA3-256 hashes of document content and creates receipt records
 * linking to previous test execution for chronological chain.
 */
public class ReceiptGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptGenerator.class);

    /**
     * Generate a receipt for a document with optional chain linking to previous receipt.
     */
    public LockchainReceipt generateReceipt(
            String testClassName,
            String testMethodName,
            String documentContent,
            DocMetadata metadata,
            Optional<LockchainReceipt> previousReceipt,
            long testDurationMs,
            boolean allTestsPassed) {

        String contentHash = computeSha3(documentContent);
        String previousHash = previousReceipt
            .map(LockchainReceipt::contentHash)
            .orElse("0000000000000000000000000000000000000000"); // Genesis

        return new LockchainReceipt(
            testClassName,
            testMethodName,
            contentHash,
            previousHash,
            Instant.now(),
            metadata,
            Map.of("document", contentHash),
            testDurationMs,
            allTestsPassed
        );
    }

    /**
     * Generate a receipt from an existing document file.
     */
    public LockchainReceipt generateReceiptFromFile(
            String testClassName,
            String testMethodName,
            Path documentPath,
            DocMetadata metadata,
            Optional<LockchainReceipt> previousReceipt,
            long testDurationMs,
            boolean allTestsPassed) throws IOException {

        String documentContent = Files.readString(documentPath, StandardCharsets.UTF_8);
        return generateReceipt(
            testClassName,
            testMethodName,
            documentContent,
            metadata,
            previousReceipt,
            testDurationMs,
            allTestsPassed
        );
    }

    /**
     * Compute SHA3-256 hash of a string.
     *
     * Uses BouncyCastle provider for SHA3-256 support (standard Java MessageDigest
     * in Java 9+ should support SHA3-256 via Security.addProvider).
     */
    public String computeSha3(String content) {
        if (content == null || content.isEmpty()) {
            return "0000000000000000000000000000000000000000";
        }

        try {
            // Try to use standard Java provider first
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("SHA3-256 not available in standard provider, using fallback");
            // Fallback to SHA-256 if SHA3-256 not available
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (NoSuchAlgorithmException e2) {
                logger.error("No hashing algorithm available", e2);
                return "unknown";
            }
        }
    }

    /**
     * Convert byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Compute hashes for multiple file outputs (Markdown, LaTeX, PDF).
     */
    public Map<String, String> computeFileHashes(Map<String, Path> filePaths) {
        var hashes = new HashMap<String, String>();

        for (var entry : filePaths.entrySet()) {
            try {
                String content = Files.readString(entry.getValue(), StandardCharsets.UTF_8);
                hashes.put(entry.getKey(), computeSha3(content));
            } catch (IOException e) {
                logger.warn("Could not read file for hashing: {}", entry.getValue(), e);
                hashes.put(entry.getKey(), "unknown");
            }
        }

        return hashes;
    }
}
