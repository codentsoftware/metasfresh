package de.metas.ui.web.pporder;

import java.util.stream.Stream;

import org.adempiere.util.GuavaCollectors;

import com.google.common.collect.ImmutableMap;

import de.metas.ui.web.handlingunits.HUDocumentViewType;
import de.metas.ui.web.view.IDocumentViewType;

/*
 * #%L
 * metasfresh-webui-api
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

public enum PPOrderLineType implements IDocumentViewType
{
	MainProduct("MP", true) //
	, BOMLine_Component("CO", false) //
	, BOMLine_ByCoProduct("BY", true) //
	//
	, HU_LU(HUDocumentViewType.LU) //
	, HU_TU(HUDocumentViewType.TU) //
	, HU_VHU(HUDocumentViewType.VHU) //
	, HU_Storage(HUDocumentViewType.HUStorage) //
	;

	private final String name;
	private final String iconName;
	private final HUDocumentViewType huDocumentViewType;
	
	private final boolean canReceive;
	private final boolean canIssue;

	private PPOrderLineType(final String name, final boolean canReceive)
	{
		this.name = name;
		this.iconName = canReceive ? "PP_Order_Receive" : "PP_Order_Issue"; // see https://github.com/metasfresh/metasfresh-webui-frontend/issues/675#issuecomment-297016790
		this.huDocumentViewType = null;
		
		this.canReceive = canReceive;
		this.canIssue = !canReceive;
	}

	private PPOrderLineType(HUDocumentViewType huType)
	{
		this.name = huType.getName();
		this.iconName = huType.getIconName();
		this.huDocumentViewType = huType;
		
		canReceive = false;
		canIssue = false;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getIconName()
	{
		return iconName;
	}

	public static final PPOrderLineType cast(final IDocumentViewType type)
	{
		return (PPOrderLineType)type;
	}

	public boolean canReceive()
	{
		return canReceive;
	}

	public boolean canIssue()
	{
		return canIssue;
	}

	public static final PPOrderLineType ofHUDocumentViewType(final HUDocumentViewType huType)
	{
		PPOrderLineType type = huType2type.get(huType);
		if(type == null)
		{
			throw new IllegalArgumentException("No type found for " + huType);
		}
		return type;
	}
	
	private static final ImmutableMap<HUDocumentViewType, PPOrderLineType> huType2type = Stream.of(values())
			.filter(type -> type.huDocumentViewType != null)
			.collect(GuavaCollectors.toImmutableMapByKey(type -> type.huDocumentViewType));
}
