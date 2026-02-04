package com.petclinic.backend.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to validate export service dependencies are properly configured
 * Validates: Requirements 1.4, 1.5
 */
@SpringBootTest
class DependencyValidationTest {
    
    @Test
    void testITextPdfLibraryAvailable() {
        try {
            // Test iText PDF library classes
            Class.forName("com.itextpdf.kernel.pdf.PdfDocument");
            Class.forName("com.itextpdf.kernel.pdf.PdfWriter");
            Class.forName("com.itextpdf.layout.Document");
            Class.forName("com.itextpdf.layout.element.Paragraph");
            Class.forName("com.itextpdf.layout.element.Table");
            
            // If we reach here, all classes are available
            assertTrue(true, "iText PDF library is properly configured");
            
        } catch (ClassNotFoundException e) {
            fail("iText PDF library not found: " + e.getMessage());
        }
    }
    
    @Test
    void testOpenCsvLibraryAvailable() {
        try {
            // Test OpenCSV library classes
            Class.forName("com.opencsv.CSVWriter");
            Class.forName("com.opencsv.CSVReader");
            
            // If we reach here, all classes are available
            assertTrue(true, "OpenCSV library is properly configured");
            
        } catch (ClassNotFoundException e) {
            fail("OpenCSV library not found: " + e.getMessage());
        }
    }
    
    @Test
    void testPdfDocumentCreation() {
        try {
            // Test basic PDF document creation
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
            
            document.add(new com.itextpdf.layout.element.Paragraph("Test PDF"));
            document.close();
            
            assertTrue(baos.size() > 0, "PDF document was created successfully");
            
        } catch (Exception e) {
            fail("Failed to create PDF document: " + e.getMessage());
        }
    }
    
    @Test
    void testCsvWriterCreation() {
        try {
            // Test basic CSV writer creation
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            com.opencsv.CSVWriter csvWriter = new com.opencsv.CSVWriter(stringWriter);
            
            String[] header = {"Column1", "Column2", "Column3"};
            String[] data = {"Value1", "Value2", "Value3"};
            
            csvWriter.writeNext(header);
            csvWriter.writeNext(data);
            csvWriter.close();
            
            String csvContent = stringWriter.toString();
            assertTrue(csvContent.contains("Column1"), "CSV content was created successfully");
            assertTrue(csvContent.contains("Value1"), "CSV data was written successfully");
            
        } catch (Exception e) {
            fail("Failed to create CSV content: " + e.getMessage());
        }
    }
}