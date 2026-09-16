#!/usr/bin/env bash
# Closes the issues triaged as won't-fix in specs/product/PRD.md.
#
# ============================================================================
# NOT RUN AUTOMATICALLY. NEVER RUN THIS FROM CI OR AN AGENT.
# Closing issues is the repo owner's decision, not Claude's or any script's.
# Review the reasoning in specs/product/PRD.md §5 first, then run manually:
#     ./scripts/close-wontfix-issues.sh
# ============================================================================
set -euo pipefail

close() { # number reason
  echo "Closing #$1"
  gh issue close "$1" --reason "not planned" --comment "$2"
}

close 65 "Won't fix. This story specifies RabbitMQ enqueue and Solr storage, both replaced by the migration proposed in ADR-001. Superseded by PRD Wave 3 (Cloud Pub/Sub) and Wave 4 (Vertex AI Search). See specs/product/PRD.md."

close 47 "Won't fix. This targets MongoDocumentDao, which ADR-001 Phase 1 wraps and Phase 2 replaces with a Firestore-backed implementation. Ordering behaviour will be specified against the new repository in PRD Wave 2. See specs/product/PRD.md."
