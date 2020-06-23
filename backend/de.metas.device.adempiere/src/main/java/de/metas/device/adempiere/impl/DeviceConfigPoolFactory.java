package de.metas.device.adempiere.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.adempiere.service.ClientId;
import org.adempiere.util.net.IHostIdentifier;

import de.metas.device.adempiere.IDeviceConfigPool;
import de.metas.device.adempiere.IDeviceConfigPoolFactory;
import de.metas.organization.OrgId;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * de.metas.device.adempiere
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class DeviceConfigPoolFactory implements IDeviceConfigPoolFactory
{
	private final ConcurrentHashMap<DeviceConfigPoolKey, IDeviceConfigPool> deviceConfigPools = new ConcurrentHashMap<>();

	@Override
	public IDeviceConfigPool getDeviceConfigPool(
			@NonNull final IHostIdentifier clientHost,
			@NonNull final ClientId adClientId,
			@NonNull final OrgId adOrgId)
	{
		return deviceConfigPools.computeIfAbsent(
				DeviceConfigPoolKey.of(clientHost, adClientId, adOrgId),
				this::createDeviceConfigPool);
	}

	private IDeviceConfigPool createDeviceConfigPool(final DeviceConfigPoolKey key)
	{
		return new SysConfigDeviceConfigPool(
				key.getClientHost(),
				key.getAdClientId(),
				key.getAdOrgId());
	}

	@Value(staticConstructor = "of")
	private static class DeviceConfigPoolKey
	{
		@NonNull
		IHostIdentifier clientHost;
		@NonNull
		ClientId adClientId;
		@NonNull
		OrgId adOrgId;
	}
}
