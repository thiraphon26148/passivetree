package com.example.passivetree.client;

/** Latest live stats received from the server (client side only). */
public final class ClientStats {
    public static double[] values = new double[0];
    public static float health = 0f;
    public static int level = 0;
    public static float xpProgress = 0f;
    public static boolean received = false;

    public static double value(int index, double fallback) {
        return received && index < values.length ? values[index] : fallback;
    }

    private ClientStats() {}
}
