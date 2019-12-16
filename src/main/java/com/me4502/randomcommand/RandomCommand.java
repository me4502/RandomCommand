package com.me4502.randomcommand;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Plugin(
        id = "randomcommand",
        name = "RandomCommand",
        description = "Randomly performs a command from a list.",
        authors = {
                "Me4502"
        }
)
public class RandomCommand {

    @Inject private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    private List<String> commands = new ArrayList<>();

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        loadConfig();

        CommandSpec randomCommand = CommandSpec.builder()
                .arguments(GenericArguments.optional(GenericArguments.player(Text.of("player"))))
                .executor((src, args) -> {
                    String command = commands.get(ThreadLocalRandom.current().nextInt(commands.size()));
                    if (args.hasAny("player")) {
                        command = command.replace("@p", args.<Player>getOne("player").get().getName());
                    }
                    Sponge.getCommandManager().process(Sponge.getServer().getConsole(), command);
                    src.sendMessage(Text.of("Ran command."));
                    return CommandResult.success();
                })
                .permission("randomcommand")
                .build();

        Sponge.getCommandManager().register(this, randomCommand, "legendrandom", "lrandom");
    }

    @Listener
    public void onServerReload(GameReloadEvent event) {
        loadConfig();
    }

    public void loadConfig() {
        commands.clear();

        try {
            if (!Files.exists(defaultConfig, LinkOption.NOFOLLOW_LINKS)) {
                URL jarConfigFile = this.getClass().getResource("default.conf");
                ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setURL(jarConfigFile).build();
                configManager.save(loader.load());
            }

            ConfigurationNode node = configManager.load();

            commands.addAll(node.getNode("commands").getList(TypeToken.of(String.class), Lists.newArrayList("say ayy")));
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }
}
