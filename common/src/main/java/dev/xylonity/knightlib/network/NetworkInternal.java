package dev.xylonity.knightlib.network;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.NetworkEndpoint;

public class NetworkInternal {

    private NetworkInternal() {
        ;;
    }

    public static NetworkEndpoint knightlib() {
        return new NetworkEndpoint(KnightLib.NETWORK);
    }

}
