package com.databuff.apm.web.ai;

import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;

import java.net.URI;
import java.util.regex.Pattern;

public final class LlmChatModelFactory {

    private static final Pattern VERSION_IN_PATH = Pattern.compile(".*/v\\d+$");

    private LlmChatModelFactory() {
    }

    public static Model build(
            OpenAiCompatibleChatClient.ResolvedLlmProvider provider,
            String modelName,
            boolean stream) {
        String resolvedModel = modelName == null || modelName.isBlank()
                ? provider.defaultModel()
                : modelName;
        String apiKey = provider.apiKey() == null ? "" : provider.apiKey();
        String baseUrl = normalizeBaseUrl(provider.baseUrl());
        if (LlmApiTypes.isAnthropic(provider.apiType())) {
            // Anthropic Java SDK always appends /v1/messages to baseUrl.
            return AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(resolvedModel)
                    .baseUrl(normalizeAnthropicSdkBaseUrl(baseUrl))
                    .stream(stream)
                    .build();
        }
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(resolvedModel)
                .baseUrl(baseUrl)
                .stream(stream)
                .build();
    }

    public static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.trim().replaceAll("/$", "");
    }

    public static String buildAnthropicMessagesUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith("/messages")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/messages";
        }
        return normalized + "/v1/messages";
    }

    /**
     * Base URL for AgentScope's Anthropic SDK client. The SDK always adds {@code /v1/messages}
     * to the configured base, so a provider base ending in {@code /v1} (e.g. Kimi coding API)
     * must be stripped to avoid {@code /v1/v1/messages}.
     */
    public static String normalizeAnthropicSdkBaseUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith("/v1/messages")) {
            normalized = normalized.substring(0, normalized.length() - "/messages".length());
        }
        if (normalized.endsWith("/v1")) {
            return normalized.substring(0, normalized.length() - 3);
        }
        return normalized;
    }

    public static String buildOpenAiChatCompletionsUrl(String baseUrl) {
        return buildVersionedEndpoint(baseUrl, "/v1/chat/completions", "/chat/completions");
    }

    public static String buildOpenAiModelsUrl(String baseUrl) {
        return buildVersionedEndpoint(baseUrl, "/v1/models", "/models");
    }

    private static String buildVersionedEndpoint(String baseUrl, String defaultEndpoint, String completeSuffix) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith(completeSuffix)) {
            return normalized;
        }
        String endpoint = defaultEndpoint;
        try {
            URI uri = URI.create(normalized);
            String path = uri.getPath();
            if (path != null && !path.isBlank() && !"/".equals(path)) {
                String trimmedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
                if (VERSION_IN_PATH.matcher(trimmedPath).matches()) {
                    if (endpoint.startsWith("/v1/")) {
                        endpoint = endpoint.substring(3);
                    } else if ("/v1".equals(endpoint)) {
                        endpoint = "";
                    }
                }
                String joinedPath = joinPaths(trimmedPath, endpoint);
                URI rebuilt = new URI(uri.getScheme(), uri.getAuthority(), joinedPath, uri.getQuery(), uri.getFragment());
                return rebuilt.toString();
            }
        } catch (Exception ignored) {
            // fall through to simple concatenation
        }
        return normalized + endpoint;
    }

    private static String joinPaths(String basePath, String endpoint) {
        if (basePath == null || basePath.isBlank() || "/".equals(basePath)) {
            return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        }
        if (endpoint == null || endpoint.isBlank()) {
            return basePath;
        }
        String left = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String right = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return left + right;
    }
}
