package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Generates the Mermaid diagram of the {@link DocumentStatus} state machine and checks it against
 * {@code specs/architecture/document-status-workflow.md}. This class is the only generator of that
 * diagram.
 */
public class DocumentStatusDiagramTest {

    private static final String INDENT = "    ";
    private static final String ARROW = " --> ";
    private static final String LABEL_SEPARATOR = " : ";
    private static final String END = "[*]";

    @Test
    public void generatedDiagramFollowsTheRules() {
        Map<String, Set<String>> edges = new HashMap<>();
        for (String line : generate().split("\n")) {
            if (!line.contains(ARROW)) {
                continue;
            }
            String[] edgeAndLabel = line.trim().split(LABEL_SEPARATOR, 2);
            String[] ends = edgeAndLabel[0].split(ARROW);
            assertNotEquals("self-loop: " + line, ends[0], ends[1]);
            Set<String> types = edgeAndLabel.length == 2
                    ? new HashSet<>(Arrays.asList(edgeAndLabel[1].split(", ")))
                    : new HashSet<>();
            assertNull("duplicate edge: " + line, edges.put(edgeAndLabel[0], types));
        }

        assertTrue("missing entry edge", edges.containsKey(END + ARROW + DocumentStatus.CREATED.name()));

        int changingPairs = 0;
        for (DocumentStatus status : DocumentStatus.values()) {
            boolean terminal = true;
            for (TransitionType type : TransitionType.values()) {
                DocumentStatus target = status.transition(type);
                if (target != status) {
                    terminal = false;
                    changingPairs++;
                    Set<String> label = edges.get(status.name() + ARROW + target.name());
                    assertNotNull("missing edge " + status + ARROW + target, label);
                    assertTrue(status + ARROW + target + " lacks " + type, label.contains(type.name()));
                }
            }
            assertEquals("terminal edge of " + status, terminal, edges.containsKey(status.name() + ARROW + END));
        }

        int labelledTypes = edges.values().stream().mapToInt(Set::size).sum();
        assertEquals("labelled types", changingPairs, labelledTypes);
    }

    static String generate() {
        Set<DocumentStatus> targets = EnumSet.noneOf(DocumentStatus.class);
        for (DocumentStatus status : DocumentStatus.values()) {
            for (TransitionType type : TransitionType.values()) {
                DocumentStatus target = status.transition(type);
                if (target != status) {
                    targets.add(target);
                }
            }
        }

        StringBuilder out = new StringBuilder("stateDiagram-v2\n");
        for (DocumentStatus status : DocumentStatus.values()) {
            if (!targets.contains(status)) {
                out.append(INDENT).append(END).append(ARROW).append(status.name()).append('\n');
            }
        }
        for (DocumentStatus status : DocumentStatus.values()) {
            Map<DocumentStatus, List<String>> typesByTarget = new LinkedHashMap<>();
            for (TransitionType type : TransitionType.values()) {
                DocumentStatus target = status.transition(type);
                if (target != status) {
                    typesByTarget.computeIfAbsent(target, t -> new ArrayList<>()).add(type.name());
                }
            }
            typesByTarget.forEach((target, types) -> out.append(INDENT).append(status.name()).append(ARROW)
                    .append(target.name()).append(LABEL_SEPARATOR).append(String.join(", ", types)).append('\n'));
            if (typesByTarget.isEmpty()) {
                out.append(INDENT).append(status.name()).append(ARROW).append(END).append('\n');
            }
        }
        return out.toString();
    }
}
