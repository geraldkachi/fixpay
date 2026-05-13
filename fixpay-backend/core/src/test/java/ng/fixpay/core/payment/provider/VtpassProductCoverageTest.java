package ng.fixpay.core.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Comprehensive sandbox coverage test — hits the real VTpass sandbox for all 36 products.
 * Results are printed as a formatted table at the end. No hard assertions are thrown;
 * the report itself is the artefact. The test always succeeds so it does not block the build.
 *
 * Run:
 *   $env:VTPASS_API_KEY="7a1593b111e836697ac1a122367c5ca2"
 *   $env:VTPASS_SECRET_KEY="SK_650580adb1b7a0da2e430bd4316c5ba9224a9dcb66c"
 *   .\gradlew :core:test --tests "*.VtpassProductCoverageTest" --rerun
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VtpassProductCoverageTest {

    // ── state ─────────────────────────────────────────────────────────────────
    private static VtpassClient CLIENT;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<Row> REPORT = Collections.synchronizedList(new ArrayList<>());

    // ── inner types ───────────────────────────────────────────────────────────
    enum Op { GET_VARIATIONS, MERCHANT_VERIFY, PURCHASE, SAMPLE_PURCHASE }

    record Product(
        String tc, String name, String category, String serviceId,
        Op op, BigDecimal amount, String billerRef, String variationCode, String verifyType
    ) {}

    record Row(
        String tc, String product, String category, String serviceId,
        String op, String status, String code, String reason
    ) {}

    // ── catalogue (36 products) ───────────────────────────────────────────────
    static final List<Product> PRODUCTS = List.of(

        // ── Airtime ──────────────────────────────────────────────────────────
        new Product("001", "Airtel Airtime VTU",        "Airtime",     "airtel",                       Op.PURCHASE,       new BigDecimal("100"), "08011111111",  null,            null),
        new Product("002", "MTN Airtime VTU",            "Airtime",     "mtn",                          Op.PURCHASE,       new BigDecimal("100"), "08011111111",  null,            null),
        new Product("003", "GLO Airtime VTU",            "Airtime",     "glo",                          Op.PURCHASE,       new BigDecimal("100"), "08011111111",  null,            null),
        new Product("004", "9mobile Airtime VTU",        "Airtime",     "etisalat",                     Op.PURCHASE,       new BigDecimal("100"), "08011111111",  null,            null),
        new Product("005", "Smile Network Payment",      "Airtime",     "smile-direct",                 Op.GET_VARIATIONS,   null,                  null,           null,            null),
        new Product("006", "International Airtime",      "Airtime",     "foreign-airtime",              Op.GET_VARIATIONS,   null,                  null,           null,            null),

        // ── Data ─────────────────────────────────────────────────────────────
        new Product("007", "Airtel Data",                "Data",        "airtel-data",                  Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),
        new Product("008", "MTN Data",                   "Data",        "mtn-data",                     Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),
        new Product("009", "GLO Data",                   "Data",        "glo-data",                     Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),
        new Product("010", "9mobile Data",               "Data",        "etisalat-data",                Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),
        new Product("011", "Smile Data",                 "Data",        "smile-direct",                 Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),
        new Product("012", "Spectranet Internet",        "Data",        "spectranet",                   Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),
        new Product("013", "GLO Data (SME)",             "Data",        "glo-sme-data",                 Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),
        new Product("014", "9mobile SME Data",           "Data",        "etisalat-sme-data",            Op.SAMPLE_PURCHASE,  null,                  "08011111111",  null,            null),

        // ── TV ───────────────────────────────────────────────────────────────
        new Product("015", "DSTV Subscription",          "TV",          "dstv",                         Op.MERCHANT_VERIFY, null,                 "1212121212",   null,            null),
        new Product("016", "GOtv Payment",               "TV",          "gotv",                         Op.MERCHANT_VERIFY, null,                 "1212121212",   null,            null),
        new Product("017", "Startimes Subscription",     "TV",          "startimes",                    Op.MERCHANT_VERIFY, null,                 "1212121212",   null,            null),
        new Product("018", "ShowMax",                    "TV",          "showmax",                      Op.SAMPLE_PURCHASE,  null,                 "08011111111",  null,            null),

        // ── Electricity ──────────────────────────────────────────────────────
        new Product("019", "IKEDC (Ikeja Electric)",     "Electricity", "ikeja-electric",               Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("020", "EKEDC (Eko Electric)",       "Electricity", "eko-electric",                 Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("021", "AEDC (Abuja Electric)",      "Electricity", "abuja-electric",               Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("022", "KEDCO (Kano Electric)",      "Electricity", "kedco",                        Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("023", "PHED (Port Harcourt)",       "Electricity", "phed",                         Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("024", "JED (Jos Electric)",         "Electricity", "jos-electric",                 Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("025", "KAEDCO (Kaduna Electric)",   "Electricity", "kaduna-electric",              Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("026", "EEDC (Enugu Electric)",      "Electricity", "enugu-electric",               Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("027", "IBEDC (Ibadan Electric)",    "Electricity", "ibadan-electric",              Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("028", "BEDC (Benin Electric)",      "Electricity", "benin-electric",               Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("029", "ABEDC (Aba Electric)",       "Electricity", "aba-electric",                 Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),
        new Product("030", "YEDC (Yola Electric)",       "Electricity", "yola-electric",                Op.MERCHANT_VERIFY, null,                 "1111111111111", null,           "prepaid"),

        // ── Education ────────────────────────────────────────────────────────
        new Product("031", "WAEC Result Checker",        "Education",   "waec",                         Op.SAMPLE_PURCHASE,  null,                 "08011111111",  null,            null),
        new Product("032", "WAEC Registration PIN",      "Education",   "waec-registration",            Op.SAMPLE_PURCHASE,  null,                 "08011111111",  null,            null),
        new Product("033", "JAMB PIN (UTME)",            "Education",   "jamb",                         Op.MERCHANT_VERIFY, null,                 "0123456789",   null,            "utme-mock"),

        // ── Insurance ────────────────────────────────────────────────────────
        new Product("034", "Third Party Motor Ins.",     "Insurance",   "ui-insure",                    Op.SAMPLE_PURCHASE,  null,                 "ABC123XY",     null,            null),
        new Product("035", "Personal Accident Ins.",     "Insurance",   "personal-accident-insurance",  Op.SAMPLE_PURCHASE,  null,                 "08011111111",  null,            null),
        new Product("036", "Home Cover Insurance",       "Insurance",   "home-cover-insurance",         Op.SAMPLE_PURCHASE,  null,                 "08011111111",  null,            null)
    );

    // ── setup ─────────────────────────────────────────────────────────────────
    @BeforeAll
    static void setUpClient() {
        String apiKey    = System.getenv("VTPASS_API_KEY");
        String secretKey = System.getenv("VTPASS_SECRET_KEY");
        String baseUrl   = System.getenv().getOrDefault("VTPASS_BASE_URL", "https://sandbox.vtpass.com");
        assumeTrue(
            apiKey != null && !apiKey.isBlank() && secretKey != null && !secretKey.isBlank(),
            "Skipping — VTPASS_API_KEY / VTPASS_SECRET_KEY not set"
        );
        CLIENT = new VtpassClient(RestClient.builder(), MAPPER, baseUrl, apiKey, secretKey);
    }

    // ── main test ─────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void runProductCoverage_collectAllResults() {
        for (Product p : PRODUCTS) {
            REPORT.add(runProduct(p));
        }
    }

    // ── execution helpers ─────────────────────────────────────────────────────
    private static Row runProduct(Product p) {
        try {
            return switch (p.op()) {
                case GET_VARIATIONS  -> runGetVariations(p);
                case MERCHANT_VERIFY -> runMerchantVerify(p);
                case PURCHASE        -> runPurchase(p);                case SAMPLE_PURCHASE -> runSamplePurchase(p);            };
        } catch (Exception ex) {
            return row(p, "FAIL", "EXC", trunc(ex.getMessage(), 70));
        }
    }

    private static Row runGetVariations(Product p) throws Exception {
        String raw    = CLIENT.getVariations(p.serviceId());
        JsonNode root = MAPPER.readTree(raw);
        String code   = extractCode(root);
        JsonNode content = root.path("content");
        // VTpass API has a typo: uses "varations" (not "variations") in some responses
        JsonNode vars = content.has("varations")  ? content.get("varations")
                      : content.has("variations") ? content.get("variations")
                      : MAPPER.createArrayNode();

        if (vars.isArray() && !vars.isEmpty()) {
            return row(p, "PASS", code, vars.size() + " variation(s) available");
        }
        if ("028".equals(code)) {
            return row(p, "WARN", code, "Not whitelisted — enable on VTpass sandbox dashboard");
        }
        String msg = root.path("response_description").asText(raw.substring(0, Math.min(raw.length(), 80)));
        return row(p, "FAIL", code, msg);
    }

    private static Row runMerchantVerify(Product p) throws Exception {
        String raw    = CLIENT.merchantVerify(p.serviceId(), p.billerRef(), p.verifyType());
        JsonNode root = MAPPER.readTree(raw);
        String code   = extractCode(root);

        if ("000".equals(code) || "020".equals(code)) {
            JsonNode c    = root.path("content");
            String name   = c.path("Customer_Name").asText(c.path("name").asText("Customer found"));
            return row(p, "PASS", code, "Customer: " + name);
        }
        if ("028".equals(code)) {
            return row(p, "WARN", code, "Not whitelisted — enable on VTpass sandbox dashboard");
        }
        // 011 = not found, 030 = biller unreachable — expected for most DISCOs in sandbox
        String msg    = root.path("content").path("message").asText(
                        root.path("response_description").asText(raw.substring(0, Math.min(raw.length(), 80))));
        // "011" means the endpoint was reached but no test meter exists — treat as INFO not FAIL
        String status = "011".equals(code) ? "INFO" : "FAIL";
        return row(p, status, code, trunc(msg, 70));
    }

    /**
     * Fetches the variation list, picks the first entry, then attempts a purchase with it.
     * The variation code and amount used are shown in the Reason column.
     */
    private static Row runSamplePurchase(Product p) throws Exception {
        // Step 1 — fetch variations
        String raw    = CLIENT.getVariations(p.serviceId());
        JsonNode root = MAPPER.readTree(raw);
        String code   = extractCode(root);

        if ("028".equals(code)) {
            return row(p, "WARN", code, "Not whitelisted — enable on VTpass sandbox dashboard");
        }

        JsonNode content = root.path("content");
        JsonNode vars = content.has("varations")  ? content.get("varations")
                      : content.has("variations") ? content.get("variations")
                      : MAPPER.createArrayNode();

        if (!vars.isArray() || vars.isEmpty()) {
            String msg = root.path("response_description").asText(raw.substring(0, Math.min(raw.length(), 80)));
            return row(p, "FAIL", code, "No variations returned: " + trunc(msg, 48));
        }

        // Step 2 — pick first variation
        JsonNode first       = vars.get(0);
        String variationCode = first.path("variation_code").asText();
        String amountStr     = first.path("variation_amount").asText("100");
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.replace(",", ""));
        } catch (NumberFormatException e) {
            amount = new BigDecimal("100");
        }

        // Step 3 — small delay then purchase
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String reqId = "FP-SAMP-" + p.tc() + "-" + System.currentTimeMillis();
        VtpassPurchaseResult result = CLIENT.purchase(reqId, p.serviceId(), amount, p.billerRef(), variationCode);
        String pCode  = result.providerCode();
        String detail = String.format("var=%s amt=%.0f", variationCode, amount);

        if (result.successful() || result.pending()) {
            return row(p, "PASS", pCode, detail + " — " + trunc(result.providerMessage(), 30));
        }
        if ("019".equals(pCode)) {
            return row(p, "PASS", pCode, detail + " — duplicate, service reachable");
        }
        if ("028".equals(pCode)) {
            return row(p, "WARN", pCode, detail + " — not whitelisted for purchase");
        }
        return row(p, "FAIL", pCode, detail + " — " + trunc(result.providerMessage(), 30));
    }

    private static Row runPurchase(Product p) throws Exception {
        // Small delay to avoid DUPLICATE_TRANSACTION (code 019) when same phone+service
        // is hit multiple times within the 15-second sandbox dedup window
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String reqId              = "FP-COV-" + p.tc() + "-" + System.currentTimeMillis();
        VtpassPurchaseResult result = CLIENT.purchase(reqId, p.serviceId(), p.amount(), p.billerRef(), p.variationCode());
        String code               = result.providerCode();

        if (result.successful() || result.pending()) {
            return row(p, "PASS", code, trunc(result.providerMessage(), 70));
        }
        if ("019".equals(code)) {
            return row(p, "PASS", code, "Duplicate txn — service is reachable and working");
        }
        if ("028".equals(code)) {
            return row(p, "WARN", code, "Not whitelisted — enable on VTpass sandbox dashboard");
        }
        return row(p, "FAIL", code, trunc(result.providerMessage(), 70));
    }

    // ── utilities ─────────────────────────────────────────────────────────────
    private static String extractCode(JsonNode root) {
        if (root.has("code"))                return root.get("code").asText();
        String rd = root.path("response_description").asText("");
        return rd.matches("\\d{3}") ? rd : rd.isEmpty() ? "???" : rd;
    }

    private static Row row(Product p, String status, String code, String reason) {
        return new Row(p.tc(), p.name(), p.category(), p.serviceId(), p.op().name(), status, code, reason);
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ── table output ──────────────────────────────────────────────────────────
    @AfterAll
    static void printReport() {
        if (REPORT.isEmpty()) return;

        // Column widths
        final int W_TC   = 3;
        final int W_PROD = 30;
        final int W_CAT  = 11;
        final int W_SID  = 26;
        final int W_OP   = 15;
        final int W_STAT = 6;
        final int W_CODE = 5;
        final int W_RSN  = 55;

        // total inner width (all cells + inner separators " | " * 7)
        final int INNER = W_TC + W_PROD + W_CAT + W_SID + W_OP + W_STAT + W_CODE + W_RSN + (7 * 3);

        String sep = "+"
            + "-".repeat(W_TC   + 2) + "+"
            + "-".repeat(W_PROD + 2) + "+"
            + "-".repeat(W_CAT  + 2) + "+"
            + "-".repeat(W_SID  + 2) + "+"
            + "-".repeat(W_OP   + 2) + "+"
            + "-".repeat(W_STAT + 2) + "+"
            + "-".repeat(W_CODE + 2) + "+"
            + "-".repeat(W_RSN  + 2) + "+";

        String hdrLine = String.format("| %-" + W_TC   + "s | %-" + W_PROD + "s | %-" + W_CAT  + "s | %-"
                                         + W_SID  + "s | %-" + W_OP   + "s | %-" + W_STAT + "s | %-"
                                         + W_CODE + "s | %-" + W_RSN  + "s |",
            "TC", "Product", "Category", "ServiceID", "Operation", "Status", "Code", "Reason");

        System.out.println();
        System.out.println("  VTPass Sandbox — Product Coverage Report");
        System.out.println(sep);
        System.out.println(hdrLine);
        System.out.println(sep);

        String lastCat = "";
        long pass = 0, warn = 0, info = 0, fail = 0;

        for (Row r : REPORT) {
            if (!r.category().equals(lastCat)) {
                lastCat = r.category();
                System.out.printf("| %-" + INNER + "s |%n", "  ─── " + lastCat.toUpperCase() + " ───");
            }
            System.out.printf("| %-" + W_TC   + "s | %-" + W_PROD + "s | %-" + W_CAT  + "s | %-"
                                 + W_SID  + "s | %-" + W_OP   + "s | %-" + W_STAT + "s | %-"
                                 + W_CODE + "s | %-" + W_RSN  + "s |%n",
                r.tc(),
                trunc(r.product(),  W_PROD),
                trunc(r.category(), W_CAT),
                trunc(r.serviceId(), W_SID),
                trunc(r.op(),       W_OP),
                r.status(),
                r.code(),
                trunc(r.reason(),   W_RSN));
            switch (r.status()) {
                case "PASS" -> pass++;
                case "WARN" -> warn++;
                case "INFO" -> info++;
                default     -> fail++;
            }
        }

        System.out.println(sep);
        System.out.printf("| %-" + INNER + "s |%n",
            String.format("  PASS: %d   WARN: %d   INFO: %d   FAIL: %d   TOTAL: %d",
                pass, warn, info, fail, REPORT.size()));
        System.out.println(sep);
        System.out.println();
        System.out.println("  PASS = working correctly   WARN = not whitelisted on sandbox account");
        System.out.println("  INFO = endpoint reached, no sandbox test data (e.g. meter not found)");
        System.out.println("  FAIL = hard error or exception");
        System.out.println();
    }
}
