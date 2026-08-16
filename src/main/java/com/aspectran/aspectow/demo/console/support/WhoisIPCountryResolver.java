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
package com.aspectran.aspectow.demo.console.support;

import com.aspectran.aspectow.appmon.AboutMe;
import com.aspectran.aspectow.appmon.common.support.IPCountryResolver;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.SystemUtils;
import com.aspectran.utils.apon.JsonToParameters;
import com.aspectran.utils.apon.Parameters;
import com.aspectran.utils.cache.Cache;
import com.aspectran.utils.cache.ConcurrentLruCache;
import com.aspectran.utils.net.IpAddressUtils;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * WHOIS OpenAPI based {@link IPCountryResolver} implementation.
 *
 * <p>Sample WHOIS response:
 * {"whois":{"query":"185.80.140.175","queryType":"IPv4","registry":"RIPENCC","countryCode":"YE"}}</p>
 *
 * <p>Created: 2020/06/29</p>
 */
public class WhoisIPCountryResolver implements IPCountryResolver, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(WhoisIPCountryResolver.class);

    private static final int TIMEOUT = 3000;

    private static final String NONE = "(none)";

    private static final String FAILED = "(failed)";

    private static final int DEFAULT_MAX_CACHE_SIZE = 2048;

    private static final List<String> iso2CountryCodes;

    private String apiUrl;

    private int maxCacheSize;

    private CloseableHttpClient httpClient;

    private Cache<String, String> cache;

    static {
        iso2CountryCodes = List.of(Locale.getISOCountries());
    }

    public WhoisIPCountryResolver() {
        this(SystemUtils.getProperty("ipascc.api.url"), DEFAULT_MAX_CACHE_SIZE);
    }

    public WhoisIPCountryResolver(String apiUrl) {
        this(apiUrl, DEFAULT_MAX_CACHE_SIZE);
    }

    public WhoisIPCountryResolver(String apiUrl, int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        setApiUrl(apiUrl);
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        if (StringUtils.hasText(apiUrl)) {
            if (httpClient == null) {
                httpClient = createHttpClient();
            }
            if (cache != null) {
                cache.clear();
            }
            cache = new ConcurrentLruCache<>(maxCacheSize, this::getCountryCode);
        } else {
            if (cache != null) {
                cache.clear();
                cache = null;
            }
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception ignored) {
                }
                httpClient = null;
            }
        }
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMaxCacheSize(int maxCacheSize) {
        Assert.isTrue(maxCacheSize > 0, "maxCacheSize must be positive");
        this.maxCacheSize = maxCacheSize;
        if (StringUtils.hasText(apiUrl)) {
            if (cache != null) {
                cache.clear();
            }
            cache = new ConcurrentLruCache<>(maxCacheSize, this::getCountryCode);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }

    @Override
    @Nullable
    public String resolveCountryCode(String ipAddress, @Nullable Locale locale) {
        Assert.notNull(ipAddress, "ipAddress must not be null");

        String ip6 = IpAddressUtils.normalizeIPv6(ipAddress);
        if (ip6 != null) {
            ipAddress = ip6;
        }

        if (cache == null || !StringUtils.hasText(apiUrl) || isPrivateOrLocalIp(ipAddress)) {
            return getCountryCode(locale);
        }

        String countryCode = cache.get(ipAddress);
        if (countryCode == null || NONE.equals(countryCode) || FAILED.equals(countryCode)) {
            countryCode = getCountryCode(locale);
        }
        return countryCode;
    }

    private String getCountryCode(String ipAddress) {
        if (httpClient == null) {
            return FAILED;
        }
        try {
            ClassicRequestBuilder requestBuilder = ClassicRequestBuilder
                    .get()
                    .setCharset(StandardCharsets.UTF_8)
                    .setUri(apiUrl + ipAddress);

            ClassicHttpRequest request = requestBuilder.build();

            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("WHOIS API returned HTTP status {} for IP {}", statusCode, ipAddress);
                    }
                    return FAILED;
                }
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    Parameters parameters = JsonToParameters.from(result);
                    Parameters whois = parameters.getParameters("whois");
                    if (whois != null) {
                        String countryCode = whois.getString("countryCode");
                        if (countryCode != null && iso2CountryCodes.contains(countryCode)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Country code of IP address {} is {}", ipAddress, countryCode);
                            }
                            return countryCode;
                        }
                    }
                    return NONE;
                }
                return FAILED;
            });
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("WHOIS IP lookup failed for {}: {}", ipAddress, e.getMessage(), e);
            } else {
                logger.warn("WHOIS IP lookup failed for {}: {}", ipAddress, e.getMessage());
            }
            return FAILED;
        }
    }

    private static CloseableHttpClient createHttpClient() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(TIMEOUT))
                .setSocketTimeout(Timeout.ofMilliseconds(TIMEOUT))
                .setTimeToLive(TimeValue.ofMinutes(5))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(TIMEOUT))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(5);
        connectionManager.setMaxTotal(5);
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .disableCookieManagement()
                .disableAuthCaching()
                .setUserAgent("Aspectran-AppMon/" + AboutMe.VERSION)
                .build();
    }

    private static boolean isPrivateOrLocalIp(@NonNull String ip) {
        return ip.equals("127.0.0.1") ||
                ip.startsWith("127.") ||
                ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("169.254.") ||
                ip.equals("localhost") ||
                ip.equals("0000:0000:0000:0000:0000:0000:0000:0001") ||
                ip.equals("0000:0000:0000:0000:0000:0000:0000:0000") ||
                ip.startsWith("fe80:") ||
                (ip.startsWith("172.") && is172Private(ip));
    }

    private static boolean is172Private(@NonNull String ip) {
        int secondDot = ip.indexOf('.', 4);
        if (secondDot > 4) {
            try {
                int secondOctet = Integer.parseInt(ip.substring(4, secondDot));
                return secondOctet >= 16 && secondOctet <= 31;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    @Nullable
    private String getCountryCode(Locale locale) {
        return (locale != null ? locale.getCountry() : null);
    }

    public static void main(String[] args) throws Exception {
        WhoisIPCountryResolver ipCountryResolver = new WhoisIPCountryResolver();
        System.out.println(ipCountryResolver.resolveCountryCode("103.99.216.86", Locale.KOREA));
        System.out.println(ipCountryResolver.resolveCountryCode("103.99.216.999", Locale.KOREA));
        System.out.println(ipCountryResolver.resolveCountryCode("0:0:0:0:0:0:0:1", Locale.KOREA));
        System.out.println(ipCountryResolver.resolveCountryCode("2a01:6502:a56:4735::1", Locale.KOREA));
        ipCountryResolver.destroy();
    }

}
