package com.mds.lsp.tcl;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class SourceFileIndex {
    final Set<String> namespaces = new HashSet<>();

    /** Simple names of classes declared in this file */
    final Set<String> topLevelClasses = new HashSet<>();

    /** Simple name of declarations in this file, including classes, methods, and fields */
    final Set<String> declarations = new HashSet<>();

    /**
     * Simple names of reference appearing in this file.
     *
     * <p>This is fast to compute and provides a useful starting point for find-references
     * operations.
     */
    final Set<String> references = new HashSet<>();

    final Instant updated = Instant.now();
}