package org.dimdev.dimdoors.client.wthit;

import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;

import org.dimdev.dimdoors.block.entity.DetachedRiftBlockEntity;
import org.dimdev.dimdoors.block.entity.EntranceRiftBlockEntity;

// FIXME: is not actually client sided
public class WthitPlugin implements IWailaPlugin {
	@Override
	public void register(IRegistrar registrar) {
		registrar.addComponent(EntranceRiftProvider.INSTANCE, TooltipPosition.BODY, EntranceRiftBlockEntity.class);
		registrar.addComponent(DetachedRiftProvider.INSTANCE, TooltipPosition.BODY, DetachedRiftBlockEntity.class);

	}
}
