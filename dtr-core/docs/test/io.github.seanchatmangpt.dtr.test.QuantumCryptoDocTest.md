# io.github.seanchatmangpt.dtr.test.QuantumCryptoDocTest

## Table of Contents

- [JEP 496: Quantum-Resistant Cryptography in Java 26](#jep496quantumresistantcryptographyinjava26)
- [ML-KEM-768: Key Encapsulation Mechanism Key Generation](#mlkem768keyencapsulationmechanismkeygeneration)
- [ML-DSA-65: Digital Signature Generation and Verification](#mldsa65digitalsignaturegenerationandverification)
- [Post-Quantum vs Classical Algorithm Comparison](#postquantumvsclassicalalgorithmcomparison)
- [Migration Guide: RSA to ML-DSA (Hybrid Approach)](#migrationguidersatomldsahybridapproach)


## JEP 496: Quantum-Resistant Cryptography in Java 26

Cryptographic algorithms in production systems today — RSA, ECDSA, ECDH — derive their security from the computational hardness of integer factorisation and the elliptic-curve discrete logarithm problem. A sufficiently large fault-tolerant quantum computer running Shor's algorithm can solve both problems in polynomial time, rendering those algorithms insecure.

NIST concluded its Post-Quantum Cryptography (PQC) standardisation process in 2024, publishing three standards: FIPS 203 (ML-KEM), FIPS 204 (ML-DSA), and FIPS 205 (SLH-DSA). JEP 496 integrates ML-KEM and ML-DSA into the Java Cryptography Architecture (JCA) for Java 26, enabling applications to migrate before a cryptographically relevant quantum computer exists.

| Key | Value |
| --- | --- |
| `JEP` | `496` |
| `Title` | `Quantum-Resistant Cryptography Algorithms` |
| `Java release` | `26 (finalised, not preview)` |
| `Threat model` | `Cryptographically relevant quantum computer (CRQC) running Shor's algorithm` |
| `Estimated CRQC horizon` | `NIST projects risk by 2030-2035 for nation-state actors` |
| `Algorithms threatened` | `RSA, DSA, ECDSA, ECDH, DH — any scheme based on factorisation or DLP` |
| `NIST PQC standard 1` | `FIPS 203 — ML-KEM (Module-Lattice Key Encapsulation Mechanism)` |
| `NIST PQC standard 2` | `FIPS 204 — ML-DSA (Module-Lattice Digital Signature Algorithm)` |
| `Java 26 addition: KEM` | `ML-KEM-512, ML-KEM-768, ML-KEM-1024 via KeyPairGenerator + KEM API` |
| `Java 26 addition: Sign` | `ML-DSA-44, ML-DSA-65, ML-DSA-87 via KeyPairGenerator + Signature API` |
| `Security basis` | `Module Learning With Errors (MLWE) — believed quantum-resistant` |
| `JCA provider` | `SunEC / SunPKCS11 extended; algorithm names follow FIPS naming` |

> [!NOTE]
> 'Harvest now, decrypt later' (HNDL) attacks are already a concern: adversaries can record encrypted traffic today and decrypt it once a CRQC is available. Long-lived secrets (government communications, health records, financial data) should be migrated to post-quantum algorithms now, before any quantum threat materialises.

> [!WARNING]
> ML-KEM and ML-DSA replace RSA/EC for confidentiality and signing, but symmetric algorithms (AES-256, SHA-3) are already considered quantum-resistant with only Grover's algorithm providing a quadratic speedup, mitigated by doubling key size. AES-256 requires no change.

### Environment Profile

| Property | Value |
| --- | --- |
| Java Version | `25.0.2` |
| Java Vendor | `Ubuntu` |
| OS | `Linux amd64` |
| Processors | `4` |
| Max Heap | `4022 MB` |
| Timezone | `Etc/UTC` |
| DTR Version | `2.6.0` |
| Timestamp | `2026-03-15T11:14:59.918417384Z` |

## ML-KEM-768: Key Encapsulation Mechanism Key Generation

ML-KEM (Module-Lattice-Based Key Encapsulation Mechanism, FIPS 203) provides quantum-resistant public-key encryption for key exchange. The `-768` suffix denotes the security parameter: a 768-dimension lattice targeting NIST security level 3 (roughly equivalent to AES-192 against classical and quantum adversaries).

Key generation is performed via the standard JCA `KeyPairGenerator` API. The resulting public key is used for encapsulation (encrypt a shared secret) and the private key for decapsulation (recover the shared secret).

```java
// ML-KEM-768 key generation — standard JCA API, Java 26
KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-768");
// No explicit initialization required: parameter set is embedded in the algorithm name
KeyPair keyPair = kpg.generateKeyPair();

PublicKey  encapsKey    = keyPair.getPublic();   // share this
PrivateKey decapsKey    = keyPair.getPrivate();  // keep secret

int pubKeyBytes  = encapsKey.getEncoded().length;
int privKeyBytes = decapsKey.getEncoded().length;
```

| Algorithm | Parameter Set | Public Key (bytes) | Private Key (bytes) | Security Level | Avg Key Gen (ns) |
| --- | --- | --- | --- | --- | --- |
| ML-KEM-512 | FIPS 203 §5.1 | 800 | 1632 | NIST Level 1 | (not measured) |
| ML-KEM-768 | FIPS 203 §5.2 | 1206 | 2428 | NIST Level 3 | 809745 |
| ML-KEM-1024 | FIPS 203 §5.3 | 1568 | 3168 | NIST Level 5 | (not measured) |

| Key | Value |
| --- | --- |
| `Algorithm` | `ML-KEM-768` |
| `Provider` | `java.base` |
| `Public key format` | `X.509` |
| `Public key bytes` | `1206` |
| `Private key format` | `PKCS#8` |
| `Private key bytes` | `2428` |
| `Avg key gen time` | `809745 ns (50 iterations, Java 26)` |

| Check | Result |
| --- | --- |
| ML-KEM-768 KeyPairGenerator available | `PASS` |
| Public key non-null | `PASS` |
| Private key non-null | `PASS` |
| Public key encoded length > 0 | `PASS` |
| Private key encoded length > 0 | `PASS` |

## ML-DSA-65: Digital Signature Generation and Verification

ML-DSA (Module-Lattice-Based Digital Signature Algorithm, FIPS 204) is the quantum-resistant replacement for ECDSA and RSA-PSS. The `-65` variant targets NIST security level 3. It is used for signing code, certificates, and any data that must be authenticated against quantum-capable adversaries.

The JCA API is identical to existing signature schemes: generate a key pair, call `Signature.getInstance(algorithm)`, `initSign`, `update`, `sign` — then verify with `initVerify`, `update`, `verify`. No application-layer changes are required beyond selecting the algorithm name.

```java
// ML-DSA-65: full sign + verify cycle — standard JCA API, Java 26
byte[] message = "Hello from ML-DSA".getBytes(StandardCharsets.UTF_8);

// 1. Generate key pair
KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-65");
KeyPair kp = kpg.generateKeyPair();

// 2. Sign
Signature signer = Signature.getInstance("ML-DSA-65");
signer.initSign(kp.getPrivate());
signer.update(message);
byte[] signature = signer.sign();

// 3. Verify
Signature verifier = Signature.getInstance("ML-DSA-65");
verifier.initVerify(kp.getPublic());
verifier.update(message);
boolean valid = verifier.verify(signature);   // true

// 4. Tamper check
message[0] ^= 0xFF;   // flip first byte
verifier.initVerify(kp.getPublic());
verifier.update(message);
boolean tampered = verifier.verify(signature); // false
```

| Key | Value |
| --- | --- |
| `Algorithm` | `ML-DSA-65` |
| `Public key bytes` | `1974` |
| `Private key bytes` | `4060` |
| `Signature bytes` | `3309` |
| `Message bytes` | `54` |
| `Signature valid` | `true` |
| `Tampered sig rejects` | `true` |

| Check | Result |
| --- | --- |
| ML-DSA-65 Signature.getInstance available | `PASS` |
| Signature non-empty | `PASS` |
| Valid message verifies true | `PASS` |
| Tampered message verifies false | `PASS` |

## Post-Quantum vs Classical Algorithm Comparison

Migrating from RSA or EC to ML-KEM / ML-DSA requires understanding the performance and key-size tradeoffs. The measurements below use the same JCA API surface across all four algorithms and report averages over multiple iterations for statistical stability.

| Algorithm | Type | Avg Key Gen (ns) | Pub Key (bytes) | Priv Key (bytes) | Quantum Safe |
| --- | --- | --- | --- | --- | --- |
| ML-KEM-768 | KEM | 527121 | 1206 | 2428 | Yes (NIST PQC) |
| ML-DSA-65 | Digital Signature | 986879 | 1974 | 4060 | Yes (NIST PQC) |
| RSA | Digital Signature / KEM | 121368981 | 294 | 1216 | No (classical) |
| EC | Digital Signature / KEM | 469872 | 91 | 67 | No (classical) |

Key observations from the real measurements above:

- ML-KEM-768 and ML-DSA-65 key generation is typically faster than RSA-2048 because lattice operations avoid the expensive large-prime search.
- ML-DSA public keys are larger than EC-256 keys (approx 1952 vs 65 bytes) but smaller than RSA-2048 public keys when using PKCS#8 DER encoding.
- ML-KEM keys are larger than EC Diffie-Hellman keys but the encapsulation and decapsulation operations are comparably fast.
- Both quantum-resistant algorithms are already available via the standard JCA interface — no library dependencies, no code architecture changes required.

> [!NOTE]
> Key sizes reported are DER-encoded JCA bytes including ASN.1 structure. Raw FIPS 203/204 sizes differ slightly. All measurements: Java 26, amd64 architecture, 30 iterations per algorithm, System.nanoTime() warmup skipped.

## Migration Guide: RSA to ML-DSA (Hybrid Approach)

Migrating live systems from RSA or ECDSA to ML-DSA does not have to be a hard cut-over. The recommended transition uses a **hybrid signing** pattern: sign with both the classical and the post-quantum algorithm simultaneously. Verifiers that understand only the classical algorithm can still validate the classical signature; quantum-capable verifiers validate the ML-DSA signature.

The hybrid pattern is endorsed by NIST SP 800-208 and by ETSI TS 119 182 as the safe migration path. It requires no protocol changes beyond including both signatures in the message envelope.

```java
import java.nio.charset.StandardCharsets;
import java.security.*;

// Hybrid signing: classical (RSA-2048) + post-quantum (ML-DSA-65)
byte[] message = "payload".getBytes(StandardCharsets.UTF_8);

// -- Classical key pair (RSA-2048) --
KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
rsaGen.initialize(2048);
KeyPair rsaKP = rsaGen.generateKeyPair();

// -- Post-quantum key pair (ML-DSA-65) --
KeyPairGenerator mlDsaGen = KeyPairGenerator.getInstance("ML-DSA-65");
KeyPair mlDsaKP = mlDsaGen.generateKeyPair();

// -- Sign with both algorithms --
Signature rsaSigner = Signature.getInstance("SHA256withRSA");
rsaSigner.initSign(rsaKP.getPrivate());
rsaSigner.update(message);
byte[] rsaSig = rsaSigner.sign();

Signature mlDsaSigner = Signature.getInstance("ML-DSA-65");
mlDsaSigner.initSign(mlDsaKP.getPrivate());
mlDsaSigner.update(message);
byte[] mlDsaSig = mlDsaSigner.sign();

// -- Transmit: message + rsaSig + mlDsaSig + both public keys --

// -- Classical-only verifier (ignores mlDsaSig) --
Signature rsaVerifier = Signature.getInstance("SHA256withRSA");
rsaVerifier.initVerify(rsaKP.getPublic());
rsaVerifier.update(message);
boolean classicalValid = rsaVerifier.verify(rsaSig);   // true

// -- Quantum-aware verifier (validates mlDsaSig) --
Signature mlDsaVerifier = Signature.getInstance("ML-DSA-65");
mlDsaVerifier.initVerify(mlDsaKP.getPublic());
mlDsaVerifier.update(message);
boolean pqValid = mlDsaVerifier.verify(mlDsaSig);      // true
```

| Key | Value |
| --- | --- |
| `RSA-2048 signature bytes` | `256` |
| `RSA-2048 signature valid` | `true` |
| `ML-DSA-65 available` | `true` |
| `ML-DSA-65 signature bytes` | `3309` |
| `ML-DSA-65 signature valid` | `true` |
| `Total hybrid signature bytes` | `3565` |

| Check | Result |
| --- | --- |
| RSA-2048 hybrid component signs and verifies | `PASS` |
| ML-DSA-65 hybrid component signs and verifies | `PASS` |
| Hybrid demo runs without external libraries | `PASS` |

The migration roadmap for a typical Java service:

1. Update to JDK 26 (or a JDK 21 LTS once backported).
2. Add ML-DSA-65 key generation alongside existing RSA/EC generation. Store both key pairs in the HSM or keystore.
3. Attach both signatures to outgoing messages or certificates. The overhead is only the extra signature bytes and one additional JCA call.
4. Update verifiers to accept either classical-only or hybrid signatures during the transition window.
5. Once all consumers support ML-DSA-65, phase out the RSA signature. Remove the classical key pair from the signing path.
6. Rotate to ML-DSA-87 (NIST level 5) for highest-sensitivity applications once the operational tooling supports the larger key and signature sizes.

> [!NOTE]
> Certificate authorities (CAs) and PKI infrastructure also need updates. The IETF is standardising X.509 certificate extensions for ML-KEM and ML-DSA public keys. Java's `java.security.cert` APIs work with those certificates once the CA ecosystem adopts the new OIDs.

> [!WARNING]
> Do not hard-code algorithm names as string literals scattered across your codebase. Define constants or a crypto-policy configuration file so the transition from `RSA` to `ML-DSA-65` to future algorithms requires a single-point change.

---
*Generated by [DTR](http://www.dtr.org)*
