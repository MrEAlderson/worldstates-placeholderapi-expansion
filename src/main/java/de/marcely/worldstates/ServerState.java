package de.marcely.worldstates;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import org.bukkit.Server;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ServerState {

    @Getter
    @Expose
    private final Map<String, WorldState> worlds = new HashMap<>();
    @Getter
    @Expose
    private String name;

    @Getter
    private final long lastUpdate = System.currentTimeMillis();

    @Nullable
    public WorldState getWorld(String name) {
        return this.worlds.get(name.toLowerCase());
    }

    private void addWorld(String name, WorldState state) {
        this.worlds.put(name.toLowerCase(), state);
    }



    public static ServerState from(Server server, String name) {
        final ServerState state = new ServerState();

        for (World world : server.getWorlds())
            state.worlds.put(world.getName(), WorldState.from(world));

        state.name = name;

        return state;
    }
}