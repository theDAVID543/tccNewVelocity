package thedavid.tccnewvelocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class listener {
    public static Map<UUID, Integer> playerTradeCooldownQuery = new HashMap<>();
    public static Map<UUID, Integer> playerAdCooldownQuery = new HashMap<>();
    public static Set<RegisteredServer> serverQuery = new HashSet<>();
    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        // Received plugin message, check channel identifier matches
        if (e.getSource() instanceof Player) {
            return;
        }
        if (e.getIdentifier().equals(tccNewVelocity.tccChannel)) {
            e.setResult(PluginMessageEvent.ForwardResult.handled());
//          Read the data written to the message
            ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
            String inPlayer = in.readUTF();
            String prefix = in.readUTF();
            String channel = in.readUTF();
            String message = in.readUTF();
            Component messageComponent = GsonComponentSerializer.gson().deserialize(message);
            String messagePlainString = PlainTextComponentSerializer.plainText().serialize(messageComponent);
            Component prefixComponent = GsonComponentSerializer.gson().deserialize(prefix);
            tccNewVelocity.logger.info(channel + ": " + inPlayer + "> " + messagePlainString);
            for (RegisteredServer server : tccNewVelocity.server.getAllServers()) {
                sendChannelMessage(server, inPlayer, prefix, channel, message);
                if(server.getPlayersConnected().size() > 0){
                    sendCooldown(server, tccNewVelocity.server.getPlayer(inPlayer).get().getUniqueId().toString(), channel, "300");
                }else{
                    serverQuery.add(server);
                    if(channel.equals("trade")){
                        playerTradeCooldownQuery.put(tccNewVelocity.server.getPlayer(inPlayer).get().getUniqueId(), 300);
                    }else if(channel.equals("ad")){
                        playerAdCooldownQuery.put(tccNewVelocity.server.getPlayer(inPlayer).get().getUniqueId(), 300);
                    }
                }
            }
        }else if(e.getIdentifier().equals(tccNewVelocity.tccDiscord)){
            ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
            String author = in.readUTF();
            String channel = in.readUTF();
            String message = in.readUTF();
            tccNewVelocity.logger.info("DC : " + channel + ": " + author + "> " + message);
            ServerConnection registeredServer = (ServerConnection) e.getSource();
            switch (channel) {
                case "general":
                    registeredServer.getServer().sendMessage(Component.text()
                            .append(Component.text("[DC]").color(NamedTextColor.BLUE))
                            .append(Component.text(author + "> ").color(NamedTextColor.WHITE))
                            .append(Component.text(message))
                    );
                    break;
                case "trade":
                    registeredServer.getServer().sendMessage(Component.text()
                            .append(Component.text("$ ").color(NamedTextColor.GOLD))
                            .append(Component.text("[DC]").color(NamedTextColor.BLUE))
                            .append(Component.text(author + "> ").color(NamedTextColor.WHITE))
                            .append(Component.text(message))
                        );
                    break;
                case "ad":
                    registeredServer.getServer().sendMessage(Component.text()
                            .append(Component.text("! ").color(NamedTextColor.AQUA))
                            .append(Component.text("[DC]").color(NamedTextColor.BLUE))
                            .append(Component.text(author + "> ").color(NamedTextColor.WHITE))
                            .append(Component.text(message))
                    );
                    break;
            }
        }
    }
    @Subscribe
    public void onLoginServer(PlayerChooseInitialServerEvent e){
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF("login");
        buf.writeUTF(e.getPlayer().getUsername());
        tccNewVelocity.server.getScheduler()
                .buildTask(tccNewVelocity.plugin, () -> {
                    if(e.getInitialServer().isPresent()){
                        e.getInitialServer().get().sendPluginMessage(tccNewVelocity.tccDiscord, buf.toByteArray());
                        tccNewVelocity.logger.info("sent login plugin message");
                    }
                })
                .delay(3L, TimeUnit.SECONDS)
                .schedule();
    }
//    @Subscribe
//    public void onDisconnect(DisconnectEvent e){
//        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
//        buf.writeUTF("disconnect");
//        buf.writeUTF(e.getPlayer().getUsername());
//        ServerConnection server = e.getPlayer().getCurrentServer().get();
//        server.sendPluginMessage(tccNewVelocity.tccDiscord, buf.toByteArray());
//    }
//    @Subscribe
//    public void onChangeServer(ServerPostConnectEvent e){
//        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
//        buf.writeUTF("change");
//        buf.writeUTF(e.getPlayer().getUsername());
//        e.getPlayer().getCurrentServer()
//        e.getPreviousServer().sendPluginMessage(tccNewVelocity.tccDiscord, buf.toByteArray());
//    }
    @Subscribe
    public void onChangeServer(ServerPreConnectEvent e){
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF("change");
        buf.writeUTF(e.getPlayer().getUsername());
        if(e.getPreviousServer() != null){
            e.getPreviousServer().sendPluginMessage(tccNewVelocity.tccDiscord, buf.toByteArray());
            tccNewVelocity.logger.info("sent change server plugin message");
        }
    }
    public void sendChannelMessage(RegisteredServer server, String player, String prefix, String channel, String message){
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF(player);
        buf.writeUTF(prefix);
        buf.writeUTF(channel);
        buf.writeUTF(message);
        // Send it
        server.sendPluginMessage(tccNewVelocity.tccChannel, buf.toByteArray());
    }
    public static void sendCooldown(RegisteredServer server, String playerUUID, String channel, String time){
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF(playerUUID);
        buf.writeUTF(channel);
        buf.writeUTF(time);
        // Send it
        server.sendPluginMessage(tccNewVelocity.tccCooldown, buf.toByteArray());
    }
}