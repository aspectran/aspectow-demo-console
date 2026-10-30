/*
 * Copyright (c) 2018-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.demo.console;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * HTTP stress tester for simulating massive concurrent session creation,
 * promotion to normal sessions, and concurrent expiration/invalidation.
 *
 * <p>Created: 2026. 10. 30.</p>
 */
public class SessionStressTester {

    private static final String DEFAULT_TARGET_URL = "http://localhost:8080/";
    private static final int DEFAULT_TOTAL_SESSIONS = 1500;
    private static final int DEFAULT_CONCURRENCY = 40;
    private static final double DEFAULT_PROMOTE_RATIO = 0.3;
    private static final int DEFAULT_MONITOR_SECONDS = 75;

    public static void main(String[] args) throws Exception {
        String targetUrl = (args.length > 0 ? args[0] : DEFAULT_TARGET_URL);
        int totalSessions = (args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_TOTAL_SESSIONS);
        int concurrency = (args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_CONCURRENCY);
        double promoteRatio = (args.length > 3 ? Double.parseDouble(args[3]) : DEFAULT_PROMOTE_RATIO);
        int monitorSeconds = (args.length > 4 ? Integer.parseInt(args[4]) : DEFAULT_MONITOR_SECONDS);

        System.out.printf("=== Aspectow Session Stress Test ===%n");
        System.out.printf("Target URL      : %s%n", targetUrl);
        System.out.printf("Total Sessions  : %d (Promote Ratio: %.0f%%)%n", totalSessions, promoteRatio * 100);
        System.out.printf("Concurrency     : %d%n", concurrency);
        System.out.printf("Monitor Window  : %ds%n%n", monitorSeconds);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        Semaphore semaphore = new Semaphore(concurrency);
        AtomicInteger createdCount = new AtomicInteger();
        AtomicInteger promotedCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        LongAdder totalLatencyMs = new LongAdder();
        List<String> promotedCookies = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(totalSessions);

        // Phase 1: Massive concurrent session creation (some promoted via a 2nd request)
        System.out.println("[Phase 1] Creating sessions...");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalSessions; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        semaphore.acquire();
                        long t0 = System.currentTimeMillis();
                        String clientIp = "10.20." + ((idx / 250) % 250) + "." + (idx % 250);
                        HttpRequest req = HttpRequest.newBuilder(URI.create(targetUrl))
                                .timeout(Duration.ofSeconds(15))
                                .header("User-Agent", "AspectowStressTester/1.0")
                                .header("X-Forwarded-For", clientIp)
                                .GET()
                                .build();

                        HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
                        totalLatencyMs.add(System.currentTimeMillis() - t0);

                        if (res.statusCode() >= 200 && res.statusCode() < 400) {
                            int current = createdCount.incrementAndGet();
                            String cookie = extractSessionCookie(res);
                            if (cookie != null && (idx % 100) < (promoteRatio * 100)) {
                                // Send a 2nd request with the session cookie to promote tempResident -> normal session
                                HttpRequest req2 = HttpRequest.newBuilder(URI.create(targetUrl))
                                        .timeout(Duration.ofSeconds(15))
                                        .header("User-Agent", "AspectowStressTester/1.0")
                                        .header("Cookie", cookie)
                                        .header("X-Forwarded-For", clientIp)
                                        .GET()
                                        .build();
                                HttpResponse<Void> res2 = client.send(req2, HttpResponse.BodyHandlers.discarding());
                                if (res2.statusCode() >= 200 && res2.statusCode() < 400) {
                                    promotedCount.incrementAndGet();
                                    promotedCookies.add(cookie);
                                }
                            }
                            if (current % 250 == 0) {
                                System.out.printf("  ... %d / %d sessions created%n", current, totalSessions);
                            }
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        semaphore.release();
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("[Phase 1 Done] Elapsed: %dms | Created: %d | Promoted: %d | Failed: %d | Avg Latency: %dms%n%n",
                elapsed, createdCount.get(), promotedCount.get(), failCount.get(),
                createdCount.get() > 0 ? totalLatencyMs.sum() / createdCount.get() : 0);

        // Phase 2: Probe server responsiveness while mass session expiration/eviction occurs
        System.out.printf("[Phase 2] Probing server responsiveness for %ds while sessions expire...%n", monitorSeconds);
        long endMonitor = System.currentTimeMillis() + (monitorSeconds * 1000L);
        int probeSeq = 0;
        while (System.currentTimeMillis() < endMonitor) {
            probeSeq++;
            long pStart = System.currentTimeMillis();
            int batchOk = 0;
            int batchFail = 0;
            long maxProbeLatency = 0;

            for (int b = 0; b < 5; b++) {
                long r0 = System.currentTimeMillis();
                try {
                    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(targetUrl))
                            .timeout(Duration.ofSeconds(10))
                            .header("User-Agent", "AspectowStressTester/1.0")
                            .GET();
                    if (b % 2 == 0 && !promotedCookies.isEmpty()) {
                        builder.header("Cookie", promotedCookies.get((probeSeq + b) % promotedCookies.size()));
                    }
                    HttpResponse<Void> r = client.send(builder.build(), HttpResponse.BodyHandlers.discarding());
                    long lat = System.currentTimeMillis() - r0;
                    maxProbeLatency = Math.max(maxProbeLatency, lat);
                    if (r.statusCode() >= 200 && r.statusCode() < 400) {
                        batchOk++;
                    } else {
                        batchFail++;
                    }
                } catch (Exception e) {
                    long lat = System.currentTimeMillis() - r0;
                    maxProbeLatency = Math.max(maxProbeLatency, lat);
                    batchFail++;
                }
            }

            System.out.printf("  [+%02ds] Probe OK: %d/5 | Fail: %d | Max Latency: %4dms %s%n",
                    probeSeq, batchOk, batchFail, maxProbeLatency,
                    maxProbeLatency > 2000 ? "<-- WARNING: High Latency / Potential Lock!" : "");

            long sleepMs = 1000L - (System.currentTimeMillis() - pStart);
            if (sleepMs > 0) {
                TimeUnit.MILLISECONDS.sleep(sleepMs);
            }
        }

        System.out.println("\n=== Stress Test Completed ===");
    }

    private static String extractSessionCookie(HttpResponse<?> response) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        for (String c : cookies) {
            int semi = c.indexOf(';');
            String pair = (semi > 0 ? c.substring(0, semi) : c).trim();
            if (pair.startsWith("JSESSIONID=") || pair.contains("SESSION")) {
                return pair;
            }
        }
        return cookies.isEmpty() ? null : cookies.get(0).split(";")[0].trim();
    }

}
