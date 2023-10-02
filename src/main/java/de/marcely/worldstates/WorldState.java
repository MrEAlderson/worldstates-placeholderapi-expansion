package de.marcely.worldstates;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class WorldState {

    @Expose
    private final int playersCount;
    @Expose
    private final int loadedChunksCount;
    @Expose
    private final int loadedEntitiesCount;
    @Expose
    private final long time;
    @Expose
    private final boolean hasStorm;
    @Expose
    private final boolean isThundering;

    @Getter(lazy = true)
    private final Map<String, String> properties = constructProperties();

    public WorldState() { // Used by Gson
        this(0, 0, 0, 0, false, false);
    }

    @Nullable
    private Map<String, String> constructProperties() {
        return new HashMap<String, String>(){{
            put("players", Integer.toString(playersCount));
            put("loaded-chunks", Integer.toString(loadedChunksCount));
            put("loaded-entities", Integer.toString(loadedEntitiesCount));
            put("time", Long.toString(time));
            put("storm", Boolean.toString(hasStorm));
            put("thunder", Boolean.toString(isThundering));
        }};
    }


    public static WorldState from(World world) {
        return new WorldState(
                world.getPlayers().size(),
                world.getLoadedChunks().length,
                world.getEntities().size(),
                world.getTime(),
                world.hasStorm(),
                world.isThundering()
        );
    }
}
