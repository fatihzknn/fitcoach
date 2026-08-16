package com.fitcoach.auth;

/**
 * Application role. USER is a regular client who does their own onboarding and
 * training. TRAINER is panel-only by default and manages a roster of clients —
 * see com.fitcoach.roster — but may also optionally complete onboarding and
 * track their own training exactly like a USER; only the panel/roster
 * relationship (a trainer can't have a trainer of their own) is role-gated.
 */
public enum Role {
    USER,
    TRAINER
}
