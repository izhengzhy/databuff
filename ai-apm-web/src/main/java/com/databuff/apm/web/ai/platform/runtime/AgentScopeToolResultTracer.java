package com.databuff.apm.web.ai.platform.runtime;

import com.databuff.apm.web.ai.platform.task.ExpertSessionResolver;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tracing.Tracer;
import io.agentscope.core.tracing.TracerRegistry;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Component
public class AgentScopeToolResultTracer implements Tracer {

    private final AgentScopeSessionHook sessionHook;

    public AgentScopeToolResultTracer(AgentScopeSessionHook sessionHook) {
        this.sessionHook = sessionHook;
    }

    @PostConstruct
    void registerTracer() {
        TracerRegistry.register(this);
    }

    @Override
    public Mono<ToolResultBlock> callTool(
            Toolkit toolkit,
            ToolCallParam param,
            Supplier<Mono<ToolResultBlock>> next) {
        long startedAtMs = System.currentTimeMillis();
        return next.get().doOnNext(result -> capture(param, result, startedAtMs));
    }

    private void capture(ToolCallParam param, ToolResultBlock result, long startedAtMs) {
        if (param == null || result == null) {
            return;
        }
        long durationMs = Math.max(0L, System.currentTimeMillis() - startedAtMs);
        String runtimeSessionId = param.getRuntimeContext() == null
                ? null
                : param.getRuntimeContext().getSessionId();
        if (runtimeSessionId == null || runtimeSessionId.isBlank()) {
            runtimeSessionId = ExpertSessionResolver.sessionIdFromRuntimeContext(param.getRuntimeContext())
                    .orElse("");
        }
        sessionHook.captureToolResult(runtimeSessionId, param.getToolUseBlock(), result, durationMs);
    }
}
