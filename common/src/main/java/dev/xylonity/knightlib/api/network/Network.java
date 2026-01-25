package dev.xylonity.knightlib.api.network;

import dev.xylonity.knightlib.KnightLib;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main networking abstraction for creating new endpoints to the C2S/S2C internal functionality.
 * <br><br>
 * In order to make a new networking endpoint, you must create the following attribute in any common class:
 * <br><br>
 * <code>
 *     public static final NetworkEndpoint NETWORK = Network.endpoint(MOD_ID);
 * </code>
 * <br><br>
 * This will grant access to the common networking API. Every single networking abstraction made by a mod must be
 * called via its own endpoint, and there should only be one per mod, to avoid duplicates and unexpected crashes.
 * @see NetworkEndpoint the API of the networking service
 */
public class Network {

    public static final String PROTOCOL = "1";

    private static final Map<String, NetworkEndpoint> ENDPOINTS = new ConcurrentHashMap<>();

    static {
        ENDPOINTS.put(KnightLib.MOD_ID, new NetworkEndpoint(KnightLib.NETWORK));
    }

    public static NetworkEndpoint endpoint(String modId) {
        return ENDPOINTS.computeIfAbsent(modId, id ->
                new NetworkEndpoint(KnightLib.NETWORK.createEndpoint(id, PROTOCOL))
        );

    }

}
