package com.databuff.apm.common.trace;

import com.databuff.apm.common.model.DcSpan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceParentUtilTest {

    @Test
    void treatsBlankOrZeroParentIdAsRoot() {
        assertThat(TraceParentUtil.isRootParentId(null)).isTrue();
        assertThat(TraceParentUtil.isRootParentId("")).isTrue();
        assertThat(TraceParentUtil.isRootParentId("0")).isTrue();
        assertThat(TraceParentUtil.isRootParentId("0000000000000000")).isTrue();
    }

    @Test
    void treatsNonRootParentIdAsChild() {
        assertThat(TraceParentUtil.isRootParentId("abc123")).isFalse();
        assertThat(TraceParentUtil.isParentFromParentId("parent-span")).isZero();
    }

    @Test
    void applyIsParentSyncsFlagFromParentId() {
        DcSpan root = new DcSpan();
        root.parent_id = "0";
        root.is_parent = 0;
        TraceParentUtil.applyIsParent(root);
        assertThat(root.is_parent).isEqualTo(1);

        DcSpan child = new DcSpan();
        child.parent_id = "parent-span";
        child.is_parent = 1;
        TraceParentUtil.applyIsParent(child);
        assertThat(child.is_parent).isZero();
    }
}
