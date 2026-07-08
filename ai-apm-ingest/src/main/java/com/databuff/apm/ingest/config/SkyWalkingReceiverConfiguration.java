package com.databuff.apm.ingest.config;

import com.databuff.apm.ingest.gateway.PipelineGateway;
import com.databuff.apm.ingest.log.OtlpLogDirectWriter;
import com.databuff.apm.ingest.metric.OtlpMetricDirectWriter;
import com.databuff.apm.ingest.skywalking.SkyWalkingConverter;
import com.databuff.apm.ingest.skywalking.SkyWalkingIngestService;
import com.databuff.apm.ingest.skywalking.SkyWalkingJvmConverter;
import com.databuff.apm.ingest.skywalking.SkyWalkingLogConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SkyWalkingReceiverConfiguration {

    @Bean
    SkyWalkingConverter skyWalkingConverter() {
        return new SkyWalkingConverter();
    }

    @Bean
    SkyWalkingJvmConverter skyWalkingJvmConverter() {
        return new SkyWalkingJvmConverter();
    }

    @Bean
    SkyWalkingLogConverter skyWalkingLogConverter() {
        return new SkyWalkingLogConverter();
    }

    @Bean
    SkyWalkingIngestService skyWalkingIngestService(
            SkyWalkingConverter traceConverter,
            SkyWalkingJvmConverter jvmConverter,
            SkyWalkingLogConverter logConverter,
            PipelineGateway gateway,
            OtlpMetricDirectWriter metricDirectWriter,
            OtlpLogDirectWriter logDirectWriter) {
        return new SkyWalkingIngestService(
                traceConverter,
                jvmConverter,
                logConverter,
                gateway,
                metricDirectWriter,
                logDirectWriter);
    }
}
