package com.aiden.plugin.viewpdf.stockwatcher;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

public class SinaQuoteFetcherTest {
    private static final Pattern REFRESH_TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

    @Test
    public void fetchQuotes_parsesSinaPayloadAndFillsRefreshTime() {
        String payload = buildPayload();
        String body = "var hq_str_sh600000=\"" + payload + "\";\n";
        CapturingStubHttpClient client = new CapturingStubHttpClient(200, body.getBytes(StandardCharsets.UTF_8), Map.of("Content-Type", List.of("text/plain; charset=UTF-8")));
        SinaQuoteFetcher fetcher = new SinaQuoteFetcher(client);

        SinaQuoteFetcher.FetchResult result = fetcher.fetchQuotes(List.of("sh600000", "sz000001"));

        Assert.assertTrue(result.isSuccess());
        Assert.assertNotNull(result.getRefreshTime());
        Assert.assertTrue(REFRESH_TIME_PATTERN.matcher(result.getRefreshTime()).matches());
        Assert.assertEquals(URI.create("https://hq.sinajs.cn/list=sh600000,sz000001"), client.lastUri);

        Quote shQuote = result.getQuotes().get("sh600000");
        Assert.assertNotNull(shQuote);
        Assert.assertEquals("sh600000", shQuote.getCode());
        Assert.assertEquals("浦发银行", shQuote.getName());
        Assert.assertEquals(11.0d, shQuote.getOpen(), 0.000001d);
        Assert.assertEquals(10.0d, shQuote.getPrevClose(), 0.000001d);
        Assert.assertEquals(12.5d, shQuote.getPrice(), 0.000001d);
        Assert.assertEquals(13.0d, shQuote.getHigh(), 0.000001d);
        Assert.assertEquals(10.5d, shQuote.getLow(), 0.000001d);
        Assert.assertEquals(12.4d, shQuote.getBid(), 0.000001d);
        Assert.assertEquals(12.6d, shQuote.getAsk(), 0.000001d);
        Assert.assertEquals(Long.valueOf(1234567L), shQuote.getVolume());
        Assert.assertEquals(34567890.0d, shQuote.getAmount(), 0.000001d);
        Assert.assertEquals(Long.valueOf(100L), shQuote.getBid1Volume());
        Assert.assertEquals(12.4d, shQuote.getBid1Price(), 0.000001d);
        Assert.assertEquals(Long.valueOf(200L), shQuote.getAsk1Volume());
        Assert.assertEquals(12.6d, shQuote.getAsk1Price(), 0.000001d);
        Assert.assertEquals(2.5d, shQuote.getChange(), 0.000001d);
        Assert.assertEquals(25.0d, shQuote.getChangePct(), 0.000001d);
        Assert.assertEquals("2026-05-18", shQuote.getQuoteDate());
        Assert.assertEquals("15:16:17", shQuote.getQuoteTime());
        Assert.assertEquals("2026-05-18 15:16:17", shQuote.getQuoteDateTime());
        Assert.assertEquals(result.getRefreshTime(), shQuote.getLastRefreshTime());

        Quote szQuote = result.getQuotes().get("sz000001");
        Assert.assertNotNull(szQuote);
        Assert.assertEquals("sz000001", szQuote.getCode());
        Assert.assertNull(szQuote.getName());
        Assert.assertEquals(result.getRefreshTime(), szQuote.getLastRefreshTime());
    }

    private static String buildPayload() {
        String[] fields = new String[32];
        fields[0] = "浦发银行";
        fields[1] = "11.0";
        fields[2] = "10.0";
        fields[3] = "12.5";
        fields[4] = "13.0";
        fields[5] = "10.5";
        fields[6] = "12.4";
        fields[7] = "12.6";
        fields[8] = "1234567";
        fields[9] = "34567890";
        fields[10] = "100";
        fields[11] = "12.4";
        fields[20] = "200";
        fields[21] = "12.6";
        fields[30] = "2026-05-18";
        fields[31] = "15:16:17";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            if (fields[i] != null) {
                sb.append(fields[i]);
            }
        }
        return sb.toString();
    }

    private static final class CapturingStubHttpClient extends HttpClient {
        private final int statusCode;
        private final byte[] body;
        private final HttpHeaders headers;
        private URI lastUri;

        private CapturingStubHttpClient(int statusCode, byte[] body, Map<String, List<String>> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = HttpHeaders.of(headers, (k, v) -> true);
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
            this.lastUri = request.uri();
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new StubHttpResponse(statusCode, request, headers, body);
            return response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            try {
                return CompletableFuture.completedFuture(send(request, responseBodyHandler));
            } catch (IOException | InterruptedException e) {
                CompletableFuture<HttpResponse<T>> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseBodyHandler);
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, new SecureRandom());
                return ctx;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private static final class StubHttpResponse implements HttpResponse<byte[]> {
        private final int statusCode;
        private final HttpRequest request;
        private final HttpHeaders headers;
        private final byte[] body;

        private StubHttpResponse(int statusCode, HttpRequest request, HttpHeaders headers, byte[] body) {
            this.statusCode = statusCode;
            this.request = request;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<byte[]>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public byte[] body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
