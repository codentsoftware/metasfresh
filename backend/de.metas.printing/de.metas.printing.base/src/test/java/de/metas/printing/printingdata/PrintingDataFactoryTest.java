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

package de.metas.printing.printingdata;

import de.metas.document.archive.api.DocOutboundService;
import de.metas.document.archive.model.I_C_Doc_Outbound_Log;
import de.metas.organization.OrgId;
import de.metas.printing.HardwarePrinterRepository;
import de.metas.printing.OutputType;
import de.metas.printing.PrintingQueueItemId;
import de.metas.printing.api.impl.Helper;
import de.metas.printing.api.util.PdfCollator;
import de.metas.printing.model.I_AD_PrinterHW;
import de.metas.printing.model.I_AD_PrinterRouting;
import de.metas.printing.model.I_C_Printing_Queue;
import de.metas.util.Services;
import org.adempiere.ad.table.api.IADTableDAO;
import org.adempiere.archive.api.IArchiveStorageFactory;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.test.AdempiereTestHelper;
import org.compiere.model.I_AD_Archive;
import org.compiere.model.I_C_Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PrintingDataFactoryTest
{

	private Helper helper;
	private PrintingDataFactory printingDataFactory;
	private IArchiveStorageFactory archiveStorageFactory;

	@BeforeEach
	void setup(TestInfo testInfo)
	{
		AdempiereTestHelper.get().init();

		helper = new Helper(testInfo);
		helper.setup();
		printingDataFactory = new PrintingDataFactory(new HardwarePrinterRepository(), new DocOutboundService());
		archiveStorageFactory = Services.get(IArchiveStorageFactory.class);
	}

	@Test
	void createPrintingDataForQueueItem()
	{
		// given
		final byte[] binaryPdfData = new PdfCollator()
				.addPages(helper.getPdf("01"), 1, 3) // First 3 pages
				.toByteArray();

		final I_AD_PrinterHW hwPrinterRecord = helper.getCreatePrinterHW("hwPrinter", OutputType.Store);
		final I_AD_PrinterRouting printerRouting = helper.createPrinterRouting("logicalPrinter", null, 10,-1, 1, 100);

		final I_C_Order referencedDocument = newInstance(I_C_Order.class);
		saveRecord(referencedDocument);

		final I_AD_Archive archiveRecord = newInstance(I_AD_Archive.class);
		archiveRecord.setName("archiveName");
		archiveRecord.setAD_Table_ID(InterfaceWrapperHelper.getTableId(I_C_Order.class));
		archiveRecord.setRecord_ID(referencedDocument.getC_Order_ID());
		archiveStorageFactory.getArchiveStorage(archiveRecord).setBinaryData(archiveRecord, binaryPdfData);
		saveRecord(archiveRecord);

		final I_C_Doc_Outbound_Log docOutboundLogRecord = newInstance(I_C_Doc_Outbound_Log.class);
		docOutboundLogRecord.setAD_Table_ID(InterfaceWrapperHelper.getTableId(I_C_Order.class));
		docOutboundLogRecord.setRecord_ID(referencedDocument.getC_Order_ID());
		saveRecord(docOutboundLogRecord);

		helper.createPrinterConfigAndMatching(null, "hwPrinter", null,10, "logicalPrinter", null);

		final I_C_Printing_Queue printingQueueRecord = newInstance(I_C_Printing_Queue.class);
		printingQueueRecord.setAD_Archive_ID(archiveRecord.getAD_Archive_ID());
		printingQueueRecord.setAD_Org_ID(23);
		saveRecord(printingQueueRecord);

		// when
		final PrintingData printingData = printingDataFactory.createPrintingDataForQueueItem(printingQueueRecord);

		// then
		assertThat(printingData.hasData()).isTrue();
		assertThat(printingData.getPrintingQueueItemId()).isEqualTo(PrintingQueueItemId.ofRepoId(printingQueueRecord.getC_Printing_Queue_ID()));
		assertThat(printingData.getDocumentFileName()).isEqualTo("C_Order-100007.pdf"); // the file name is not so nice, because there is not documentName, docType etc set up
		assertThat(printingData.getNumberOfPages()).isEqualTo(3);
		assertThat(printingData.getOrgId()).isEqualTo(OrgId.ofRepoId(23));
		assertThat(printingData.getSegments()).isNotEmpty()
				.extracting("pageFrom", "pageTo", "printerRoutingId.repoId")
				.containsExactly(tuple(1, 3, printerRouting.getAD_PrinterRouting_ID()));
	}
}