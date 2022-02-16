package org.dimdev.dimdoors.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public interface UseItemOnBlockCallback {
	Event<UseItemOnBlockCallback> EVENT = EventFactory.createLoop().createConsumerLoop(UseItemOnBlockCallback.class,
			listeners -> (player, world, hand, hitresult) -> {
				for (UseItemOnBlockCallback event : listeners) {
					ActionResult result = event.useItemOnBlock(player, world, hand, hitresult);

					if (result != ActionResult.PASS) {
						return result;
					}
				}

				return ActionResult.PASS;
			}
	);

	ActionResult useItemOnBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult);
}
