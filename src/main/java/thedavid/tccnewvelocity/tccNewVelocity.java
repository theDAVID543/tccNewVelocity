package thedavid.tccnewvelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "tccnewvelocity",
        name = "tccNewVelocity",
        version = "1.0-SNAPSHOT"
)
public class tccNewVelocity {
    public static Logger logger = null;
    public static ProxyServer server = null;
    public static PluginContainer plugin = null;
    public static final ChannelIdentifier tccChannel =
            MinecraftChannelIdentifier.from("tcc:channel");
    public static final ChannelIdentifier tccDiscord =
            MinecraftChannelIdentifier.from("tcc:discord");
    public static final ChannelIdentifier tccCooldown =
            MinecraftChannelIdentifier.from("tcc:cooldown");
    @Inject
    public tccNewVelocity(ProxyServer server, Logger logger) {
        tccNewVelocity.server = server;
        tccNewVelocity.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new listener());
        server.getChannelRegistrar().register(tccChannel);
        server.getChannelRegistrar().register(tccDiscord);
        server.getChannelRegistrar().register(tccCooldown);
        plugin = server.getPluginManager().getPlugin("tccnewvelocity").get();
        tccNewVelocity.server.getScheduler()
                .buildTask(this, () -> {
                    Set<UUID> keySet = listener.playerTradeCooldownQuery.keySet();
                    for(UUID uuid : keySet){
                        listener.playerTradeCooldownQuery.put(uuid, listener.playerTradeCooldownQuery.get(uuid) - 1);
                        if(listener.playerTradeCooldownQuery.get(uuid) <= 0){
                            listener.playerTradeCooldownQuery.remove(uuid);
                        }
                    }

                    keySet = listener.playerAdCooldownQuery.keySet();
                    for(UUID uuid : keySet){
                        listener.playerAdCooldownQuery.put(uuid, listener.playerAdCooldownQuery.get(uuid) - 1);
                        if(listener.playerAdCooldownQuery.get(uuid) <= 0){
                            listener.playerAdCooldownQuery.remove(uuid);
                        }
                    }
                    Set<RegisteredServer> servers = listener.serverQuery;
                    if(!servers.isEmpty()){
                        for(RegisteredServer r : servers){
                            if(r.getPlayersConnected().size() > 0){
                                keySet = listener.playerTradeCooldownQuery.keySet();
                                for(UUID uuid : keySet){
                                    listener.sendCooldown(r, String.valueOf(uuid), "trade", listener.playerTradeCooldownQuery.get(uuid).toString());
                                }

                                keySet = listener.playerAdCooldownQuery.keySet();
                                for(UUID uuid : keySet){
                                    listener.sendCooldown(r, String.valueOf(uuid), "ad", listener.playerAdCooldownQuery.get(uuid).toString());
                                }
                                listener.serverQuery.remove(r);
                            }
                        }
                    }
                }).repeat(1,TimeUnit.SECONDS).schedule();
    }

}
