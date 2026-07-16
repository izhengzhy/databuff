package com.databuff.apm.common.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DorisStreamLoadProfileTest {

    @Test
    void metaServiceProfileMapsReservedColumns() {
        DorisStreamLoadProfile profile = DorisStreamLoadProfile.metaService();
        assertThat(profile.headers().get("strict_mode")).isEqualTo("false");
        assertThat(profile.headers().get("max_filter_ratio")).isEqualTo("1");
        assertThat(profile.headers().get("columns")).contains("`service`");
        assertThat(profile.headers().get("columns")).contains("`type`");
        assertThat(profile.headers().get("columns")).contains("`describe`");
        assertThat(profile.headers().get("jsonpaths")).contains("$.id");
    }

    @Test
    void metricJvmProfileMapsDottedColumns() {
        DorisStreamLoadProfile profile = DorisStreamLoadProfile.metricJvm();
        assertThat(profile.headers().get("strict_mode")).isEqualTo("false");
        assertThat(profile.headers().get("max_filter_ratio")).isEqualTo("1");
        assertThat(profile.headers().get("columns")).startsWith("metric_time,ts,");
        assertThat(profile.headers().get("jsonpaths")).contains("$.metric_time");
        assertThat(profile.headers().get("columns")).contains("thread_count");
        assertThat(profile.headers().get("columns")).contains("cpu_load_process");
        assertThat(profile.headers().get("columns")).contains("gc_major_collection_count");
        assertThat(profile.headers().get("columns")).doesNotContain("`=cpu_load_process");
        assertThat(profile.headers().get("jsonpaths")).contains("$.thread_count");
        assertThat(profile.headers().get("jsonpaths")).contains("$.cpu_load_process");
    }

    @Test
    void defaultTableProfileAllowsFilteringBadRows() {
        DorisStreamLoadProfile profile = DorisStreamLoadProfile.forTable("log_dc_record");
        assertThat(profile.headers().get("strict_mode")).isEqualTo("false");
        assertThat(profile.headers().get("max_filter_ratio")).isEqualTo("1");
    }
}
