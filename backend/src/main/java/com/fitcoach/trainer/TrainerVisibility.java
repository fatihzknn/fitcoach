package com.fitcoach.trainer;

/**
 * Plain static utility (not a Spring bean) so it stays trivially testable and
 * mockable — TrainerPhilosophy.getTargetSex() is a simple stubbed getter in tests,
 * consistent with the rest of this codebase's Mockito-based entity testing style.
 */
public final class TrainerVisibility {

    private TrainerVisibility() {}

    /**
     * True if a trainer gated to {@code targetSex} should be visible to / selectable
     * by a user with the given profile sex. A null {@code targetSex} means the
     * trainer is sex-neutral and visible to everyone.
     */
    public static boolean isVisible(String targetSex, String userSex) {
        return targetSex == null || targetSex.equals(userSex);
    }
}
