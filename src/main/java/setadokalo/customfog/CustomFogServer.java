package setadokalo.customfog;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import setadokalo.customfog.config.CustomFogConfig;
import setadokalo.customfog.config.DimensionConfig;
import setadokalo.customfog.config.ServerConfig;

import java.util.Objects;

public class CustomFogServer implements DedicatedServerModInitializer {
	public static ServerConfig config;

	@Override
	public void onInitializeServer() {
		config = ServerConfig.getConfig();
		CustomFog.log(Level.INFO, "Initializing packet sender");
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, CustomFog.SERVER_CONFIG_PACKET_ID))
				ServerPlayNetworking.send(
					handler.player,
					CustomFog.SERVER_CONFIG_PACKET_ID,
					PacketByteBufs.create().writeString(config.serialize())
				);
			CustomFog.log(Level.INFO, "Sending packet");
		});
		ServerPlayNetworking.registerGlobalReceiver(CustomFog.OP_UPDATE_CONFIG_PACKET_ID,
			(server, player, handler, buf, responseSender) -> {
				if (player.hasPermissionLevel(3)) {
					Identifier dimId = buf.readIdentifier();
					DimensionConfig dimConf = new DimensionConfig(
						buf.readBoolean(),
						buf.readEnumConstant(CustomFogConfig.FogType.class),
						buf.readFloat(),
						buf.readFloat(),
						buf.readFloat(),
						buf.readFloat()
					);
					if (Objects.equals(dimId.toString(), Utils.WATER_CONFIG)) {
						config.waterOverride = dimConf;
					} else if (Objects.equals(dimId.toString(), Utils.POWDER_SNOW_CONFIG)) {
						config.snowOverride = dimConf;
					} else if (Objects.equals(dimId.toString(), "_customfog_internal:__/default/__")) {
						config.defaultOverride = dimConf;
					} else if (Objects.equals(dimId.toString(), "_customfog_internal:__/universal/__")) {
						config.universalOverride = dimConf;
					} else {
						config.overrides.put(dimId, dimConf);
					}
					config.saveConfig();
					sendUpdatedConfig(server);
				}
			}
		);
		CommandRegistrationCallback.EVENT.register(
			(dis, ded) -> dis.register(CommandManager.literal("customfog")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.literal("reload").executes(CustomFogServer::customFogReload))
			)
		);
	}
	private static int customFogReload(CommandContext<ServerCommandSource> ctx) {
		config = ServerConfig.getConfig();
		sendUpdatedConfig(ctx.getSource().getServer());
		ctx.getSource().sendFeedback(
			new TranslatableText("modid.customfog").formatted(Formatting.GOLD)
				.append(new TranslatableText("chat.customfog.reloaded").formatted(Formatting.YELLOW)),
			true);
		return 0;
	}

	private static void sendUpdatedConfig(MinecraftServer server) {
		String serialized =	config.serialize();
		for (ServerPlayerEntity entity : server.getPlayerManager().getPlayerList()) {
			if (ServerPlayNetworking.canSend(entity, CustomFog.SERVER_CONFIG_PACKET_ID))
				ServerPlayNetworking.send(entity, CustomFog.SERVER_CONFIG_PACKET_ID, PacketByteBufs.create().writeString(serialized));
		}
	}
}