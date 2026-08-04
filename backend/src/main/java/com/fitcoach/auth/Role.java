package com.fitcoach.auth;

/**
 * Application role. USER is a regular client who does their own onboarding and
 * training. TRAINER is a panel-only account that manages a roster of clients —
 * see com.fitcoach.roster — and never does personal onboarding/plan/workout flows.
 */
public enum Role {
    USER,
    TRAINER
}
