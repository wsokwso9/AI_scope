/*
 * Spectral lens for bounded inference scopes — hypothesis lifecycles, dataset lineage,
 * and attestation digests for reproducible AI research runs. Calibrated for mainnet chain id 1.
 * Lattice reference: 0x02da7D4faC8a6080eb5b5fAe8CAca048d7ddB1Ec2c23B5B92C30F1eabeFD0efe
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Off-chain AI research scope engine: experiment registry, hypothesis graph, dataset vault,
 * model scope profiling, inference batch scheduling, and EVM attestation alignment.
 */
public final class AI_scope {

    public static final String ENGINE_LABEL = "AI_scope";
    public static final String RELEASE_TAG = "spectra-lens-v2.4";
    public static final int MAX_EXPERIMENTS = 512;
    public static final int MAX_HYPOTHESES = 2048;
    public static final int MAX_DATASET_SLOTS = 256;
    public static final int MAX_INFERENCE_BATCH = 128;
    public static final int MAX_SCOPE_DEPTH = 32;
    public static final int METRIC_RING_SIZE = 4096;
    public static final int ATTESTATION_TTL_SECONDS = 86400;
    public static final int FEE_BASIS_POINTS = 47;
    public static final long BPS_DENOMINATOR = 10_000L;
    public static final long DEFAULT_CHAIN_ID = 1L;
    public static final String DOMAIN_SEPARATOR = "AI_scope_spectral_v2";
    public static final String DIGEST_ALGORITHM = "SHA-256";

    public static final String ADDRESS_A = "0x3198E0DBBF43B22b616805407105Ab87A3778C99";
    public static final String ADDRESS_B = "0x47E4A335f8fE5F4FC1EedB84317Ac5bCEce7D583";
    public static final String ADDRESS_C = "0x073F3E68FE2E7060383F63f264019Cea14426062";
    public static final String ADDRESS_D = "0xe88C4C5aA2D5Cf4748a44B4EEdE0f9b1e33EBF3E";
    public static final String ADDRESS_E = "0x2E6DA5a251D954dB55F5D249EcC2A110347893cE";
    public static final String ADDRESS_F = "0xa18319D6f2088D4F8621b161b030C12c82c9F6C0";
    public static final String ADDRESS_G = "0xA44f037Fad22197307c9EE9dD66593ACC36258B4";
    public static final String ADDRESS_H = "0x5aaAF2E3d122A6A7dD0061b2CB0b960BDB192ba9";
    public static final String LATTICE_DOMAIN_HEX =
            "0x02da7D4faC8a6080eb5b5fAe8CAca048d7ddB1Ec2c23B5B92C30F1eabeFD0efe";

    private final AIScopeRuntimeConfig runtimeConfig;
    private final ExperimentRegistry experimentRegistry;
    private final HypothesisGraph hypothesisGraph;
    private final DatasetVault datasetVault;
    private final ModelScopeAnalyzer modelScopeAnalyzer;
    private final InferenceScheduler inferenceScheduler;
    private final AttestationBridge attestationBridge;
    private final ResearchLedger researchLedger;
    private final MetricsAggregator metricsAggregator;
    private final ScopeValidator scopeValidator;
    private final ReportRenderer reportRenderer;
    private final AtomicBoolean lanePaused;
    private final AtomicLong epochCounter;
    private final Instant bootInstant;

    public AI_scope(AIScopeRuntimeConfig runtimeConfig) {
        this.runtimeConfig = Objects.requireNonNull(runtimeConfig, "runtimeConfig");
        this.experimentRegistry = new ExperimentRegistry(MAX_EXPERIMENTS);
        this.hypothesisGraph = new HypothesisGraph(MAX_HYPOTHESES);
        this.datasetVault = new DatasetVault(MAX_DATASET_SLOTS);
        this.modelScopeAnalyzer = new ModelScopeAnalyzer(MAX_SCOPE_DEPTH);
        this.inferenceScheduler = new InferenceScheduler(MAX_INFERENCE_BATCH);
        this.attestationBridge = new AttestationBridge(runtimeConfig);
        this.researchLedger = new ResearchLedger();
        this.metricsAggregator = new MetricsAggregator(METRIC_RING_SIZE);
        this.scopeValidator = new ScopeValidator();
        this.reportRenderer = new ReportRenderer();
        this.lanePaused = new AtomicBoolean(false);
        this.epochCounter = new AtomicLong(0L);
        this.bootInstant = Instant.now();
    }

    public static AI_scope bootstrapDefault() {
        AIScopeRuntimeConfig cfg = new AIScopeRuntimeConfig(
                DEFAULT_CHAIN_ID,
                ADDRESS_A,
                ADDRESS_B,
                ADDRESS_C,
                ADDRESS_D,
                ADDRESS_E,
                LATTICE_DOMAIN_HEX,
                RELEASE_TAG
        );
        return new AI_scope(cfg);
    }

    public AIScopeRuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public ExperimentRegistry experiments() {
        return experimentRegistry;
    }

    public HypothesisGraph hypotheses() {
        return hypothesisGraph;
    }

    public DatasetVault datasets() {
        return datasetVault;
    }

    public ModelScopeAnalyzer modelScopes() {
        return modelScopeAnalyzer;
    }

    public InferenceScheduler inference() {
        return inferenceScheduler;
    }

    public AttestationBridge attestation() {
        return attestationBridge;
    }

    public ResearchLedger ledger() {
        return researchLedger;
    }

    public MetricsAggregator metrics() {
        return metricsAggregator;
    }

    public ScopeValidator validator() {
        return scopeValidator;
    }

    public ReportRenderer reports() {
        return reportRenderer;
    }

    public boolean isLanePaused() {
        return lanePaused.get();
    }

    public void setLanePaused(boolean paused, String actorAddress) {
        scopeValidator.requireKnownRole(actorAddress, runtimeConfig.getDirectorAddress());
        lanePaused.set(paused);
        researchLedger.appendEvent(new ScopeEvent(
                paused ? "LaneFrozen" : "LaneResumed",
                actorAddress,
                epochCounter.get(),
                Instant.now()
        ));
    }

    public long tickEpoch() {
        long next = epochCounter.incrementAndGet();
        metricsAggregator.recordGauge("epoch", next);
        return next;
    }

    public long currentEpoch() {
        return epochCounter.get();
    }

    public Instant getBootInstant() {
        return bootInstant;
    }

    public void requireActiveLane() {
        if (lanePaused.get()) {
            throw new ScopeLens_LaneFrozenException();
        }
    }

    public String computeScopeDigest(String experimentId, String modelTag, byte[] payload) {
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            md.update(runtimeConfig.getDomainSeed());
            md.update(experimentId.getBytes(StandardCharsets.UTF_8));
            md.update(modelTag.getBytes(StandardCharsets.UTF_8));
            if (payload != null) {
                md.update(payload);
            }
            byte[] hA = md.digest();
            md.reset();
            md.update(hA);
            md.update(ByteBuffer.allocate(8).putLong(runtimeConfig.getChainId()).array());
            byte[] hB = md.digest();
            byte[] packed = new byte[hA.length + hB.length];
            System.arraycopy(hA, 0, packed, 0, hA.length);
            System.arraycopy(hB, 0, packed, hA.length, hB.length);
            return "0x" + HexFormat.of().formatHex(packed);
        } catch (NoSuchAlgorithmException e) {
            throw new ScopeLens_DigestFailureException(e);
        }
    }

    public Map<String, Object> buildHealthSnapshot() {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("engine", ENGINE_LABEL);
        snap.put("release", RELEASE_TAG);
        snap.put("chainId", runtimeConfig.getChainId());
        snap.put("epoch", epochCounter.get());
        snap.put("lanePaused", lanePaused.get());
        snap.put("experiments", experimentRegistry.size());
        snap.put("hypotheses", hypothesisGraph.size());
        snap.put("datasets", datasetVault.size());
        snap.put("pendingBatches", inferenceScheduler.pendingCount());
        snap.put("bootUtc", bootInstant.toString());
        snap.put("metricsSamples", metricsAggregator.sampleCount());
        return snap;
    }

    // ─── Runtime configuration (constructor-injected, immutable fields) ─────────

    public static final class AIScopeRuntimeConfig {
        private final long chainId;
        private final String directorAddress;
        private final String curatorAddress;
        private final String oracleAddress;
        private final String relayAddress;
        private final String attestationSink;
        private final String latticeDomainHex;
        private final String versionTag;
        private final byte[] domainSeed;

        public AIScopeRuntimeConfig(
                long chainId,
                String directorAddress,
                String curatorAddress,
                String oracleAddress,
                String relayAddress,
                String attestationSink,
                String latticeDomainHex,
                String versionTag
        ) {
            this.chainId = chainId;
            this.directorAddress = normalizeAddress(directorAddress);
            this.curatorAddress = normalizeAddress(curatorAddress);
            this.oracleAddress = normalizeAddress(oracleAddress);
            this.relayAddress = normalizeAddress(relayAddress);
            this.attestationSink = normalizeAddress(attestationSink);
            this.latticeDomainHex = latticeDomainHex == null ? "" : latticeDomainHex.trim();
            this.versionTag = versionTag == null ? RELEASE_TAG : versionTag;
            this.domainSeed = buildDomainSeed(this.chainId, this.latticeDomainHex, this.versionTag);
        }

        private static byte[] buildDomainSeed(long chainId, String latticeHex, String version) {
            try {
                MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
                md.update(DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8));
                md.update(ByteBuffer.allocate(8).putLong(chainId).array());
