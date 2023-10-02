package de.marcely.worldstates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Collection;
import java.util.function.Consumer;

public class ProxyCommunicator implements PluginMessageListener {

    private static final String CHANNEL = "WorldStatesExpansion";
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private final WorldStatesExpansion expansion;

    private String serverName;

    public ProxyCommunicator(WorldStatesExpansion expansion) {
        this.expansion = expansion;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord"))
            return;

        final DataInputStream stream = new DataInputStream(new ByteArrayInputStream(message));

        try {
            switch (stream.readUTF()) {
                case CHANNEL: {
                    final ServerState state = GSON.fromJson(stream.readUTF(), ServerState.class);

                    this.expansion.addServer(state);
                }
                break;

                case "GetServer": {
                    this.serverName = stream.readUTF();
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastInfo() {
        if (this.serverName == null) {
            requestServerChannel();
            return;
        }

        sendMessage(stream -> {
            try {
                final String encoded = GSON.toJson(ServerState.from(Bukkit.getServer(), this.serverName));

                stream.writeUTF("Forward");
                stream.writeUTF("ALL");
                stream.writeUTF(CHANNEL);
                stream.writeUTF(encoded);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void requestServerChannel() {
        sendMessage(stream -> {
            try {
                stream.writeUTF("GetServer");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMessage(Consumer<DataOutputStream> consumer) {
        final Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        if (players.isEmpty())
            return;

        final ByteArrayOutputStream binaryStream = new ByteArrayOutputStream();
        final Player player = players.stream()
                .skip((int) (players.size() * Math.random()))
                .findFirst().get(); // obtain a random player

        consumer.accept(new DataOutputStream(binaryStream));

        player.sendPluginMessage(
                this.expansion.getPlaceholderAPI(),
                "BungeeCord",
                binaryStream.toByteArray()
        );
    }
}
