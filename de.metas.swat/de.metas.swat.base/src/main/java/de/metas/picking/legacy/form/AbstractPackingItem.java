/**
 *
 */
package de.metas.picking.legacy.form;

/*
 * #%L
 * de.metas.swat.base
 * %%
 * Copyright (C) 2015 metas GmbH
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.uom.api.IUOMConversionBL;
import org.adempiere.uom.api.UOMConversionContext;
import org.compiere.model.I_C_UOM;
import org.compiere.util.Util;

import de.metas.adempiere.model.I_M_Product;
import de.metas.inoutcandidate.api.IShipmentScheduleBL;
import de.metas.inoutcandidate.model.I_M_ShipmentSchedule;
import de.metas.product.IProductDAO;
import de.metas.product.ProductId;
import de.metas.quantity.Quantity;
import de.metas.util.Check;
import de.metas.util.Services;
import lombok.NonNull;

/**
 * Item to be packed.
 *
 * Inside contains a mapping of {@link I_M_ShipmentSchedule} to qtys that need to be packed.
 */
public abstract class AbstractPackingItem implements IPackingItem
{
	private static final int GROUPINGKEY_ToBeGenerated = Integer.MIN_VALUE;

	private final ArrayList<I_M_ShipmentSchedule> schedules;
	private final ShipmentScheduleQtyPickedMap sched2qty;

	private final int groupingKey;
	private I_M_Product product; // lazy
	private final I_C_UOM uom;
	private BigDecimal weightSingle;
	private boolean closed = false;

	/**
	 * See {@link #AbstractPackingItem(Map, int)}.
	 *
	 * @param scheds2Qtys
	 */
	protected AbstractPackingItem(final ShipmentScheduleQtyPickedMap scheds2Qtys)
	{
		this(scheds2Qtys, GROUPINGKEY_ToBeGenerated);
	}

	/**
	 *
	 * @param scheds2Qtys this instance's {@link #getShipmentSchedules()} will return the schedules in the order they were retunred by the given map's {@link Map#entrySet()} implementation.
	 *            So, if you care for that order, then I suggest to call this constructor with an {@link LinkedHashMap} or similar.
	 * @param groupingKey
	 */
	public AbstractPackingItem(@NonNull final ShipmentScheduleQtyPickedMap sched2qtyParam, final int groupingKey)
	{
		Check.assume(!sched2qtyParam.isEmpty(), "scheds2Qtys not empty");
		this.sched2qty = sched2qtyParam.copy();

		schedules = new ArrayList<>(sched2qty.getShipmentSchedules());

		final I_M_ShipmentSchedule firstSchedule = schedules.get(0);
		if (groupingKey == GROUPINGKEY_ToBeGenerated)
		{
			this.groupingKey = computeGroupingKey(firstSchedule);
		}
		else
		{
			this.groupingKey = groupingKey;
		}

		uom = Services.get(IShipmentScheduleBL.class).getUomOfProduct(firstSchedule);
		Check.assumeNotNull(uom, "uom not null");

		assertValid();
	}

	/** Copy constructor */
	protected AbstractPackingItem(final IPackingItem copyFrom)
	{
		if (copyFrom instanceof AbstractPackingItem)
		{
			final AbstractPackingItem copyFromItem = (AbstractPackingItem)copyFrom;
			sched2qty = copyFromItem.sched2qty.copy();
			schedules = new ArrayList<>(copyFromItem.schedules);
			groupingKey = copyFromItem.groupingKey;
			product = copyFromItem.product;
			uom = copyFromItem.uom;
			weightSingle = copyFromItem.weightSingle;
			closed = copyFromItem.closed;
		}
		else
		{
			throw new IllegalArgumentException("Packing item " + copyFrom + " does not extend " + AbstractPackingItem.class);
		}
	}

	protected void updateFrom(final IPackingItem item)
	{
		if (!(item instanceof AbstractPackingItem))
		{
			throw new IllegalArgumentException("Packing item " + item + " does not extend " + AbstractPackingItem.class);
		}

		final AbstractPackingItem itemCasted = (AbstractPackingItem)item;
		sched2qty.setFrom(itemCasted.sched2qty);

		schedules.clear();
		schedules.addAll(itemCasted.schedules);

		// this.groupingKey = itemCasted.groupingKey;
		product = itemCasted.product;
		// this.uom = itemCasted.uom;
		weightSingle = itemCasted.weightSingle;
		closed = itemCasted.closed;
	}

	@Override
	public final boolean isClosed()
	{
		return closed;
	}

	@Override
	public final void setClosed(final boolean closed)
	{
		this.closed = closed;
	}

	/**
	 * Assets that this packing item is correct.
	 *
	 * More precisely, checks if schedules have same UOM, same grouping key.
	 */
	private final void assertValid()
	{
		if (sched2qty.isEmpty())
		{
			return;
		}

		final IShipmentScheduleBL shipmentScheduleBL = Services.get(IShipmentScheduleBL.class);

		I_C_UOM uom = null;
		int groupingKey = GROUPINGKEY_ToBeGenerated;
		boolean firstSched = true;
		for (final I_M_ShipmentSchedule sched : sched2qty.getShipmentSchedules())
		{
			final I_C_UOM currentUOM = shipmentScheduleBL.getUomOfProduct(sched);
			final int currentKey = computeGroupingKey(sched);
			if (firstSched)
			{
				groupingKey = currentKey;
				uom = currentUOM;
				firstSched = false;
			}
			else
			{
				if (uom.getC_UOM_ID() != currentUOM.getC_UOM_ID())
				{
					throw new AdempiereException("schedules does not have same UOM");
				}
				if (groupingKey != currentKey)
				{
					throw new AdempiereException("schedules does not have same grouping key");
				}
			}
		}
	}

	/**
	 * Gets GroupingKey for given shipment schedule
	 *
	 * NOTE: this method is called from constructor too.
	 *
	 * @param sched
	 * @return
	 */
	protected int computeGroupingKey(final I_M_ShipmentSchedule sched)
	{
		final IShipmentScheduleBL shipmentScheduleBL = Services.get(IShipmentScheduleBL.class);

		// #100 FRESH-435: in FreshPackingItem we rely on all scheds having the same effective C_BPartner_Location_ID, so we need to include that in the key
		final boolean includeBPartner = true;
		return shipmentScheduleBL.mkKeyForGrouping(sched, includeBPartner).hashCode();
	}

	@Override
	public final List<I_M_ShipmentSchedule> getShipmentSchedules()
	{
		return new ArrayList<>(schedules);
	}

	@Override
	public final Quantity getQtySum()
	{
		return sched2qty.getQtySum()
				.orElseGet(() -> Quantity.zero(getC_UOM()));
	}

	@Override
	public final void setQtyForSched(
			@NonNull final I_M_ShipmentSchedule sched,
			@NonNull final Quantity qty)
	{
		sched2qty.setQtyForSched(sched, qty);
	}

	@Override
	public final BigDecimal retrieveWeightSingle(final String trxName)
	{
		if (weightSingle == null)
		{
			weightSingle = getM_Product().getWeight();
		}
		return weightSingle;
	}

	@Override
	public final BigDecimal computeWeightInProductUOM()
	{
		BigDecimal computedWeight = BigDecimal.ZERO;

		final BigDecimal weightPerUnit = retrieveWeightSingle(ITrx.TRXNAME_None);

		final IUOMConversionBL uomConversionBL = Services.get(IUOMConversionBL.class);
		for (final I_M_ShipmentSchedule sched : getShipmentSchedules())
		{
			final Quantity qtyForSched = getQtyForSched(sched);
			final BigDecimal qtyInProductUOM = uomConversionBL
					.convertQtyToProductUOM(UOMConversionContext.of(getProductId()), qtyForSched.getAsBigDecimal(), qtyForSched.getUOM());
			computedWeight = computedWeight.add(weightPerUnit.multiply(qtyInProductUOM));
		}
		return computedWeight;
	}

	@Override
	public final Quantity retrieveVolumeSingle(final String trxName)
	{
		return Quantity.of(getM_Product().getVolume(), getM_Product().getC_UOM());
	}

	@Override
	public final I_M_Product getM_Product()
	{
		// FIXME: refactor this shit
		if (product == null)
		{
			final ProductId productId = getProductId();
			product = Services.get(IProductDAO.class).getById(productId, I_M_Product.class);
		}
		return product;
	}

	@Override
	public final ProductId getProductId()
	{
		final List<I_M_ShipmentSchedule> shipmentSchedules = getShipmentSchedules();
		if (shipmentSchedules.isEmpty())
		{
			return null;
		}

		// all scheds must have the same product
		final I_M_ShipmentSchedule firstShipmentSchedule = shipmentSchedules.get(0);
		return ProductId.ofRepoId(firstShipmentSchedule.getM_Product_ID());
	}

	@Override
	public final void addSingleSched(final I_M_ShipmentSchedule sched)
	{
		addSchedules(ShipmentScheduleQtyPickedMap.singleton(sched, Quantity.zero(getC_UOM())));
	}

	@Override
	public final Quantity getQtyForSched(final I_M_ShipmentSchedule sched)
	{
		return sched2qty.getQty(sched);
	}

	@Override
	public final ShipmentScheduleQtyPickedMap getQtys()
	{
		return sched2qty.subset(schedules);
	}

	@Override
	public final ShipmentScheduleQtyPickedMap subtract(final Quantity subtrahent)
	{
		final Predicate<I_M_ShipmentSchedule> acceptShipmentSchedulePredicate = null; // no filter, i.e. accept all
		return subtract(subtrahent, acceptShipmentSchedulePredicate);
	}

	@Override
	public final ShipmentScheduleQtyPickedMap subtract(
			@NonNull final Quantity subtrahent,
			@Nullable final Predicate<I_M_ShipmentSchedule> acceptShipmentSchedulePredicate)
	{
		final ShipmentScheduleQtyPickedMap result = ShipmentScheduleQtyPickedMap.newInstance();

		//
		// Qty that needs to be subtracted
		Quantity qtyToSubtract = subtrahent;
		boolean allowRemainingQtyToSubtract = false;

		//
		// Create a copy of sched2qty and work on it
		// Later, after everything is validated we will copy it back.
		// We are doing this because we want to avoid inconsistencies in case an exception popups
		final ShipmentScheduleQtyPickedMap sched2qtyCopy = sched2qty.copy();

		//
		// Iterate all schedule/qty entries and subtract requested qty
		// for (final Iterator<Entry<I_M_ShipmentSchedule, Quantity>> it = sched2qtyCopy.entrySet().iterator(); it.hasNext();)
		for (final I_M_ShipmentSchedule sched : sched2qtyCopy.getShipmentSchedules())
		{
			//
			// If there is no qty to subtract, stop here
			if (qtyToSubtract.signum() <= 0)
			{
				break;
			}

			//
			// Make sure current shipment schedule is accepted by our predicate (if any)
			if (acceptShipmentSchedulePredicate != null && !acceptShipmentSchedulePredicate.test(sched))
			{
				// NOTE: we are not removing from map because remaining items will be copied back at the end
				// it.remove();

				// In case we excluded a shipment schedule, we cannot enforce to always have QtyToSubtract=0 at the end.
				// NOTE: in future we could add a parameter or something to enforce this or not.
				// Then pls check which is calling this method, because there is BL which relly on this logic
				// (e.g. Kommissioner Terminal, when we pack the qty which was not found in HUs, but we are doing this only for those shipment schedules which have Force delivery rule)
				allowRemainingQtyToSubtract = true;

				// Skip this record
				continue;
			}

			final Quantity schedQty = sched2qtyCopy.getQty(sched);
			final Quantity schedQtySubtracted;

			//
			// Current qtyToSubtract is bigger then current schedule's available Qty
			// => subtract only schedule's available Qty
			if (qtyToSubtract.compareTo(schedQty) > 0)
			{
				schedQtySubtracted = schedQty;
				sched2qtyCopy.remove(sched);
			}
			// Current QtyToSubtract is lower or equal with current schedule's available Qty
			// => subtract the whole qtyToSubtract
			else
			{
				schedQtySubtracted = qtyToSubtract;
				final Quantity schedQtyRemaining = schedQty.subtract(schedQtySubtracted);
				sched2qtyCopy.setQty(sched, schedQtyRemaining);
			}

			//
			// Update qtyToSubtract
			qtyToSubtract = qtyToSubtract.subtract(schedQtySubtracted);

			//
			// Add our subtracted schedule/qty pair to result to be returned
			result.setQty(sched, schedQtySubtracted);
		}

		//
		// If we could not subtract the whole qty that was asked, throw an exception
		if (!allowRemainingQtyToSubtract && qtyToSubtract.signum() != 0)
		{
			throw new PackingItemSubtractException(this, subtrahent, qtyToSubtract);
		}

		//
		// If there were changes (i.e. result is not empty) then we need to copy back our modified sched2qty map
		if (!result.isEmpty())
		{
			sched2qty.setFrom(sched2qtyCopy);

			// make sure that the ordering of the remaining schedules is not changed.
			final Iterator<I_M_ShipmentSchedule> iterator = schedules.iterator();
			while (iterator.hasNext())
			{
				final I_M_ShipmentSchedule currentSched = iterator.next();
				if (!sched2qty.contains(currentSched))
				{
					iterator.remove();
				}
			}
		}

		//
		// Return the result
		return result;
	}

	@Override
	public final void addSchedules(final ShipmentScheduleQtyPickedMap toAdd)
	{
		final boolean removeExistingOnes = false;
		addSchedules(toAdd, removeExistingOnes);
	}

	private final void addSchedules(
			@NonNull final ShipmentScheduleQtyPickedMap toAdd,
			final boolean removeExistingOnes)
	{
		//
		// Make sure we are allowed to add those shipment schedules
		for (final I_M_ShipmentSchedule schedToAdd : toAdd.getShipmentSchedules())
		{
			if (!canAddSchedule(schedToAdd))
			{
				throw new IllegalArgumentException(schedToAdd + " can't be added to " + this);
			}
		}

		//
		// Remove existing schedules (if asked)
		// NOTE: we remove existing ones AFTER we validate because "canAddSchedule" always returns true in case there are no schedules
		if (removeExistingOnes)
		{
			sched2qty.clear();
			schedules.clear();
		}

		//
		// Add shipment schedules
		for (final I_M_ShipmentSchedule schedToAdd : toAdd.getShipmentSchedules())
		{
			final Quantity qtyToAdd = toAdd.getQty(schedToAdd);
			final Quantity qty = sched2qty.getQty(schedToAdd);
			if (qty == null)
			{
				// don't invoke addSched because we might have been called by addSched ourselves
				sched2qty.setQty(schedToAdd, qtyToAdd);
				schedules.add(schedToAdd);
			}
			else
			{
				final Quantity qtyNew = qty.add(qtyToAdd);
				sched2qty.setQty(schedToAdd, qtyNew);
			}
		}
	}

	@Override
	public final void addSchedules(final IPackingItem packingItem)
	{
		addSchedules(packingItem.getQtys());
	}

	@Override
	public final void setSchedules(final IPackingItem packingItem)
	{
		final ShipmentScheduleQtyPickedMap toAdd = packingItem.getQtys();
		final boolean removeExistingOnes = true;
		addSchedules(toAdd, removeExistingOnes);
	}

	@Override
	public final boolean canAddSchedule(final I_M_ShipmentSchedule schedToAdd)
	{
		if (sched2qty.isEmpty())
		{
			return true;
		}

		return groupingKey == computeGroupingKey(schedToAdd);
	}

	@Override
	public final void setWeightSingle(final BigDecimal piWeightSingle)
	{
		weightSingle = piWeightSingle;
	}

	@Override
	public final int getGroupingKey()
	{
		return groupingKey;
	}

	@Override
	public final I_C_UOM getC_UOM()
	{
		return uom;
	}

	@Override
	public abstract IPackingItem copy();

	@Override
	public boolean isSameAs(final IPackingItem item)
	{
		return Util.same(this, item);
	}
}
