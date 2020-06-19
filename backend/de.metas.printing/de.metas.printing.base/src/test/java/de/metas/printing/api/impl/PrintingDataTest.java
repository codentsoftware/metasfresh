/*
 * #%L
 * de.metas.printing.base
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

package de.metas.printing.api.impl;

import de.metas.organization.OrgId;
import de.metas.printing.HardwarePrinterId;
import de.metas.printing.HardwareTrayId;
import de.metas.printing.PrinterRoutingId;
import de.metas.printing.PrintingQueueItemId;
import de.metas.printing.api.util.PdfCollator;
import de.metas.printing.model.I_AD_PrinterRouting;
import de.metas.printing.model.I_C_Print_Job;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.rules.TestName;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.*;

class PrintingDataTest
{
	private Helper helper;

	@BeforeEach
	void beforeEach(TestInfo testInfo)
	{
		helper = new Helper(testInfo);
		helper.setup();
	}

	@Test
	void two_segments_last_takes_precedence()
	{
		// given
		// PDF to print
		final byte[] binaryPdfData = new PdfCollator()
				.addPages(helper.getPdf("01"), 1, 1) // First 1 pages
				.toByteArray();
		// expected result
		final byte[] dataExpected = new PdfCollator()
				.addPages(helper.getPdf("01"), 1, 1) // Last 2 pages => we got only one
				.toByteArray();

		final HardwarePrinterId printerId = HardwarePrinterId.ofRepoId(30);

		// when
		final PrintingData printingData = PrintingData.builder()
				.documentName("test")
				.orgId(OrgId.ofRepoId(10))
				.printingQueueItemId(PrintingQueueItemId.ofRepoId(20))
				.data(binaryPdfData)
				.segment(PrintingSegment.builder()
						.printerRoutingId(PrinterRoutingId.ofRepoId(401))
						.routingType(I_AD_PrinterRouting.ROUTINGTYPE_PageRange)
						.initialPageFrom(1)
						.initialPageTo(100)
						.printerId(printerId).build())
				.segment(PrintingSegment.builder()
						.printerRoutingId(PrinterRoutingId.ofRepoId(402))
						.routingType(I_AD_PrinterRouting.ROUTINGTYPE_LastPages)
						.lastPages(1)
						.printerId(printerId).build())
				.build();

		// then
		assertThat(printingData.getNumberOfPages()).isEqualTo(1);
		assertThat(printingData.getSegments()).extracting("printerRoutingId.repoId", "pageFrom", "pageTo")
				// the segment we first added has to be discarded, because the "LastPages" one takes precedence and there are no paged left to cover for the first segment
				.containsExactly(tuple(402, 1, 1));
	}

	@Test
	void two_segments_last_overlaps_first()
	{
		// create the routings, which also will create the logical printer and trays
		// setting pageFrom=1, pageTo=3 to support documents with e.g. 3, 4, 5 pages. In all those cases, the last page shall be printed on tray02

		// PDF to print
		final byte[] binaryPdfData = new PdfCollator()
				.addPages(helper.getPdf("01"), 1, 3) // First 3 pages
				.toByteArray();
		// expected result
		final byte[] dataExpected = new PdfCollator()
				.addPages(helper.getPdf("01"), 1, 1) // First 1 pages (because 2nd and 3rd page overlaps with last 2)
				.addPages(helper.getPdf("01"), 2, 3) // Last 2 pages
				.toByteArray();

		final HardwarePrinterId printerId = HardwarePrinterId.ofRepoId(30);
		HardwareTrayId tray1Id = HardwareTrayId.ofRepoId(printerId, 301);
		HardwareTrayId tray2Id = HardwareTrayId.ofRepoId(printerId, 302);

		// when
		final PrintingData printingData = PrintingData.builder()
				.documentName("test")
				.orgId(OrgId.ofRepoId(10))
				.printingQueueItemId(PrintingQueueItemId.ofRepoId(20))
				.data(binaryPdfData)
				.segment(PrintingSegment.builder()
						.printerRoutingId(PrinterRoutingId.ofRepoId(401))
						.routingType(I_AD_PrinterRouting.ROUTINGTYPE_PageRange)
						.initialPageFrom(1)
						.initialPageTo(3)
						.printerId(printerId)
						.trayId(tray1Id).build())
				.segment(PrintingSegment.builder()
						.printerRoutingId(PrinterRoutingId.ofRepoId(402))
						.routingType(I_AD_PrinterRouting.ROUTINGTYPE_LastPages)
						.lastPages(2)
						.printerId(printerId)
						.trayId(tray2Id).build())
				.build();

		// then
		assertThat(printingData.getNumberOfPages()).isEqualTo(3);
		assertThat(printingData.getSegments()).extracting("printerRoutingId.repoId", "pageFrom", "pageTo")
				// the segment we first added has to be discarded, because the "LastPages" one takes precedence and there are no paged left to cover for the first segment
				.containsExactly(
						tuple(401, 1, 1),
						tuple(402, 2, 3));
	}
}