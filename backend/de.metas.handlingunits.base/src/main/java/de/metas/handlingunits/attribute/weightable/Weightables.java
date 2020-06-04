package de.metas.handlingunits.attribute.weightable;

import javax.annotation.Nullable;

import org.adempiere.mm.attributes.AttributeCode;

import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import lombok.experimental.UtilityClass;

/*
 * #%L
 * de.metas.handlingunits.base
 * %%
 * Copyright (C) 2020 metas GmbH
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

@UtilityClass
public class Weightables
{
	public static final AttributeCode ATTR_WeightGross = AttributeCode.ofString("WeightGross");
	public static final AttributeCode ATTR_WeightNet = AttributeCode.ofString("WeightNet");
	public static final AttributeCode ATTR_WeightTare = AttributeCode.ofString("WeightTare");
	public static final AttributeCode ATTR_WeightTareAdjust = AttributeCode.ofString("WeightTareAdjust");

	/**
	 * Boolean property which if set, it will allow user to change weights but ONLY on VHU level
	 *
	 * @task http://dewiki908/mediawiki/index.php/08270_Wareneingang_POS_multiple_lines_in_1_TU_%28107035315495%29
	 */
	public static final String PROPERTY_WeightableOnlyIfVHU = IWeightable.class.getName() + ".WeightableOnlyIfVHU";

	public IWeightable wrap(@Nullable final IAttributeStorage attributeStorage)
	{
		return new AttributeStorageWeightable(attributeStorage);
	}

}
