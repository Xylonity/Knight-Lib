package dev.xylonity.knightlib.api.network;

import dev.xylonity.knightlib.KnightLib;

import java.util.Map;
import java.util.Objects;
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

    public static final String PROTOCOL = "5";

    private static final Map<String, RegisteredEndpoint> ENDPOINTS = new ConcurrentHashMap<>();

    static {
        ENDPOINTS.put(KnightLib.MOD_ID, new RegisteredEndpoint(PROTOCOL, new NetworkEndpoint(KnightLib.NETWORK)));
    }

    /**
     * Returns an endpoint using KnightLib's current default protocol
     */
    public static NetworkEndpoint endpoint(String modId) {
        return endpoint(modId, PROTOCOL);
    }

    /**
     * Returns an endpoint with a protocol version owned by an external mod
     */
    public static NetworkEndpoint endpoint(String modId, String protocol) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(protocol, "protocol");
        if (modId.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] modId cannot be blank");
        }
        if (protocol.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] protocol cannot be blank");
        }

        final RegisteredEndpoint registration = ENDPOINTS.compute(modId, (id, existing) -> {
            if (existing == null) {
                return new RegisteredEndpoint(protocol, new NetworkEndpoint(KnightLib.NETWORK.createEndpoint(id, protocol)));
            }
            if (!existing.protocol().equals(protocol)) {
                throw new IllegalStateException("[KnightLib] Network endpoint " + modId
                        + " is already registered with protocol " + existing.protocol()
                        + " and cannot also use " + protocol);
            }

            return existing;
        });

        return registration.endpoint();
    }

    private record RegisteredEndpoint(
            String protocol,
            NetworkEndpoint endpoint
    ) {
        ;;
    }

}