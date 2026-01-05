/**
 * {@code GamePhase} opisuje aktualną fazę rozgrywki.
 *
 * <p>Fazy są częścią wymagań zadania 8:
 * {@code PLAYING} (normalna gra), {@code SCORING_REVIEW} (AGREE/RESUME), {@code FINISHED}.
 */

package pl.edu.go.game;

public enum GamePhase {
    PLAYING,
    SCORING_REVIEW,
    FINISHED
}
