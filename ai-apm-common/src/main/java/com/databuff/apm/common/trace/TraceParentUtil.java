package com.databuff.apm.common.trace;

import com.databuff.apm.common.model.DcSpan;

/**
 * Legacy portal {@code parentId=0} semantics: {@code is_parent=1} iff the span has no parent
 * span id ({@code parent_id} blank, {@code "0"}, or all-zero OTel parent span id).
 */
public final class TraceParentUtil {

    private TraceParentUtil() {
    }

    public static boolean isRootParentId(String parentId) {
        if (parentId == null || parentId.isBlank() || "0".equals(parentId)) {
            return true;
        }
        for (int i = 0; i < parentId.length(); i++) {
            if (parentId.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    public static int isParentFromParentId(String parentId) {
        return isRootParentId(parentId) ? 1 : 0;
    }

    public static void applyIsParent(DcSpan span) {
        if (span == null) {
            return;
        }
        span.is_parent = isParentFromParentId(span.parent_id);
    }

    public static String parentKey(String parentId) {
        return isRootParentId(parentId) ? "0" : parentId;
    }
}
