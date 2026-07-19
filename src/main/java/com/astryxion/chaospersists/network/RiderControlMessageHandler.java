package com.astryxion.chaospersists.network;

import com.astryxion.chaospersists.core.ChaosPersists;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RiderControlMessageHandler {
    private static final Logger L = LogManager.getLogger();

    public static void handle(RiderControlMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ChaosPersists.flyup_keystate = message.keystate;
            }
        });
        ctx.setPacketHandled(true);
    }
}
