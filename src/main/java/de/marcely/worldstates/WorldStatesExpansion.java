package de.marcely.worldstates;

import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldStatesExpansion extends PlaceholderExpansion implements Configurable, Taskable {

    private static final String CONFIG_INTERVAL = "refresh_interval";
    private static final String CONFIG_RETURN_NON_ERROR = "return_0_instead_of_error";

    private final Map<String, ServerState> servers = new ConcurrentHashMap<>();
    private final ProxyCommunicator proxyCommunicator = new ProxyCommunicator(this);

    private BukkitTask refreshScheduler;

    @Override
    public @NotNull String getIdentifier() {
        return "world-states";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MrEAlderson";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public List<String> getPlaceholders() {
        final List<String> placeholders = new ArrayList<>();

        for (Map.Entry<String, ServerState> server : this.servers.entrySet()) {
            for (Map.Entry<String, WorldState> world : server.getValue().getWorlds().entrySet()) {
                for (String prop : world.getValue().getProperties().keySet()) {
                    placeholders.add(server.getKey() + "_" + world.getKey() + "_" + prop);
                }
            }
        }

        return placeholders;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        boolean success = false;

        identifier = identifier.toLowerCase();

        try {
            // quick search
            {
                final String[] parts = identifier.split("_");

                if (parts.length < 3)
                    return "Invalid syntax. Expected: %world-states_<server>_<world>_<property>%";
                else if (parts.length == 3) {
                    final ServerState server = this.servers.get(parts[0]);

                    if (server == null)
                        return "Unknown server \"" + parts[0] + "\" " +
                                "(Maybe wait a little, check whether you joined with the proxy and whether the server is actually online)";

                    final WorldState world = server.getWorld(parts[1]);

                    if (world == null)
                        return "Unknown world \"" + parts[1] + "\"";

                    final String value = world.getProperties().get(parts[2]);

                    if (value == null)
                        return "Unknown property \"" + parts[2] + "\"";

                    success = true;

                    // it has been a while since we received info... server likely empty now
                    final boolean invalid = System.currentTimeMillis() - server.getLastUpdate() >=
                            (Math.max(getLong(CONFIG_INTERVAL, 8), 5) + 5) * 1000L;

                    if (invalid && parts[2].equals("players"))
                        return "0";

                    return value;
                }
            }

            // we do it like that to accommodate world / servers that have _ in their name
            for (Map.Entry<String, ServerState> server : this.servers.entrySet()) {
                for (Map.Entry<String, WorldState> world : server.getValue().getWorlds().entrySet()) {
                    for (Map.Entry<String, String> prop : world.getValue().getProperties().entrySet()) {
                        final String matching = server.getKey() + "_" + world.getKey() + "_" + prop.getKey();

                        if (!identifier.equals(matching))
                            continue;

                        success = true;

                        return prop.getValue();
                    }
                }
            }

            return "Failed to find a match. Is the syntax correct? Expected: %world-states_<server>_<world>_<property>%";
        } finally {
            if (!success && getBoolean(CONFIG_RETURN_NON_ERROR, false))
                return "0";
        }
    }

    @Override
    public Map<String, Object> getDefaults() {
        return new HashMap<String, Object>() {{
            put(CONFIG_INTERVAL, 8);
            put(CONFIG_RETURN_NON_ERROR, false);
        }};
    }

    @Nullable
    public ServerState getServer(String server) {
        return this.servers.get(server.toLowerCase());
    }

    public void addServer(ServerState state) {
        this.servers.put(state.getName().toLowerCase(), state);
    }

    @Override
    public synchronized void start() {
        if (this.refreshScheduler == null) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(getPlaceholderAPI(), "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(getPlaceholderAPI(), "BungeeCord", this.proxyCommunicator);
        } else
            this.refreshScheduler.cancel();

        this.refreshScheduler = Bukkit.getScheduler().runTaskTimer(getPlaceholderAPI(), () -> {
            this.proxyCommunicator.broadcastInfo();
        }, 40, 20L * getLong(CONFIG_INTERVAL, 8));
    }

    @Override
    public synchronized void stop() {
        if (this.refreshScheduler == null)
            return;

        this.refreshScheduler.cancel();

        Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), "BungeeCord", this.proxyCommunicator);
    }
}
