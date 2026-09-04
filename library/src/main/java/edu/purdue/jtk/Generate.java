package edu.purdue.jtk;

/**
 * The Generate class enumerates the types of "synthetic" brain waves that the MuseGenerator class can produce for
 * testing and demonstration.
 *
 * @author Tim Korb
 * @since 1.0.0
 */
public enum Generate {
    Unfocused,
    Focused,
    Calm,
    FrontBrain,
    RearBrain,
    LeftBrain,
    RightBrain,
    MaxRight,
    Zero,
    /** Eyes-open mental effort: high beta/gamma, low alpha. */
    Thinking,
    /** Eyes-closed rest: high alpha, low beta/gamma. */
    Relaxing,
    Winner,
    Loser
}
