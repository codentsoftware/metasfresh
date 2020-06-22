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

import de.metas.organization.OrgId;
import de.metas.printing.HardwarePrinter;
import de.metas.printing.HardwarePrinterId;
import de.metas.printing.HardwarePrinterRepository;
import de.metas.printing.HardwareTrayId;
import de.metas.printing.OutputType;
import de.metas.printing.PrinterRoutingId;
import de.metas.printing.PrintingQueueItemId;
import de.metas.printing.api.impl.Helper;
import de.metas.printing.api.util.PdfCollator;
import de.metas.printing.model.I_AD_PrinterHW;
import de.metas.printing.model.I_AD_PrinterHW_MediaTray;
import de.metas.printing.model.I_AD_PrinterRouting;
import de.metas.printing.printingdata.PrintingData;
import de.metas.printing.printingdata.PrintingDataToPDFFileStorer;
import de.metas.printing.printingdata.PrintingSegment;
import de.metas.util.time.SystemTime;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.I_AD_SysConfig;
import org.compiere.model.X_AD_SysConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class PrintingDataToPDFFileStorerTest
{
	private Helper helper;
	private PrintingDataToPDFFileStorer printingDataToPDFFileStorer;
	private HardwarePrinterRepository hardwarePrinterRepository;

	@BeforeEach
	void beforeEach(TestInfo testInfo)
	{
		helper = new Helper(testInfo);
		helper.setup();
		printingDataToPDFFileStorer = new PrintingDataToPDFFileStorer();
		hardwarePrinterRepository = new HardwarePrinterRepository();
	}

	/**
	 * Call {@link PrintingDataToPDFFileStorer#storeInFileSystem(PrintingData)} twice with two different timstamps and verify the result.
	 */
	@Test
	void storeInFileSystem() throws IOException
	{
		// given
		// PDF to print
		final byte[] binaryPdfData = new PdfCollator()
				.addPages(helper.getPdf("01"), 1, 3) // First 3 pages
				.toByteArray();

		final I_AD_SysConfig sysConfigRecord = InterfaceWrapperHelper.newInstance(I_AD_SysConfig.class);
		sysConfigRecord.setName(PrintingDataToPDFFileStorer.SYSCONFIG_STORE_PDF_BASE_DIRECTORY);
		sysConfigRecord.setValue("/tmp/");
		sysConfigRecord.setConfigurationLevel(X_AD_SysConfig.CONFIGURATIONLEVEL_Organization);
		InterfaceWrapperHelper.saveRecord(sysConfigRecord);

		final I_AD_PrinterHW hwPrinterRecord = helper.getCreatePrinterHW("hwPrinter", OutputType.Store);
		final HardwarePrinterId printerId = HardwarePrinterId.ofRepoId(hwPrinterRecord.getAD_PrinterHW_ID());
		final I_AD_PrinterHW_MediaTray hwTray1Record = helper.getCreatePrinterTrayHW("hwPrinter", "hwTray1");
		final I_AD_PrinterHW_MediaTray hwTray2Record = helper.getCreatePrinterTrayHW("hwPrinter", "hwTray2Record");
		final HardwareTrayId tray1Id = HardwareTrayId.ofRepoId(printerId, hwTray1Record.getAD_PrinterHW_MediaTray_ID());
		final HardwareTrayId tray2Id = HardwareTrayId.ofRepoId(printerId, hwTray2Record.getAD_PrinterHW_MediaTray_ID());

		final HardwarePrinter printer = hardwarePrinterRepository.getById(printerId);

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
						.printer(printer)
						.trayId(tray1Id).build())
				.segment(PrintingSegment.builder()
						.printerRoutingId(PrinterRoutingId.ofRepoId(402))
						.routingType(I_AD_PrinterRouting.ROUTINGTYPE_LastPages)
						.lastPages(2)
						.printer(printer)
						.trayId(tray2Id).build())
				.build();

		// when
		SystemTime.setTimeSource(() -> 100);
		printingDataToPDFFileStorer.storeInFileSystem(printingData);

		SystemTime.setTimeSource(() -> 200);
		new PrintingDataToPDFFileStorer().storeInFileSystem(printingData);

		// then

		// expected files
		final File tray1File1 = new File("/tmp/hwPrinter/hwTray1/100_test.pdf");
		final File tray2File1 = new File("/tmp/hwPrinter/hwTray2Record/100_test.pdf");

		final File tray1File2 = new File("/tmp/hwPrinter/hwTray1/200_test.pdf");
		final File tray2File2 = new File("/tmp/hwPrinter/hwTray2Record/200_test.pdf");

		// expected binary content result
		final byte[] dataExpectedTray1 = new PdfCollator()
				.addPages(helper.getPdf("01"), 1, 1) // First 1 pages (because 2nd and 3rd page overlaps with last 2)
				.toByteArray();
		final byte[] dataExpectedTray2 = new PdfCollator()
				.addPages(helper.getPdf("01"), 2, 3) // Last 2 pages
				.toByteArray();

		assertThat(tray1File1).exists();
		final byte[] tray1File1Content = Files.readAllBytes(tray1File1.toPath());
		helper.assertEqualsPDF(dataExpectedTray1, tray1File1Content);

		assertThat(tray2File1).exists();
		final byte[] tray2File1Content = Files.readAllBytes(tray2File1.toPath());
		helper.assertEqualsPDF(dataExpectedTray2, tray2File1Content);

		assertThat(tray1File2).exists();
		final byte[] tray1File2Content = Files.readAllBytes(tray1File2.toPath());
		helper.assertEqualsPDF(dataExpectedTray1, tray1File2Content);

		assertThat(tray2File2).exists();
		final byte[] tray2File2Content = Files.readAllBytes(tray2File2.toPath());
		helper.assertEqualsPDF(dataExpectedTray2, tray2File2Content);
	}
}