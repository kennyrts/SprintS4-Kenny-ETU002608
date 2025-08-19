package mg.itu.prom16.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple PDF generator utility for Sprint framework
 * This is a basic implementation that creates simple text-based PDFs
 */
public class PdfGenerator {
    
    /**
     * Generate a simple PDF from ModelView data
     * @param modelView The ModelView containing data to export
     * @param title The title for the PDF document
     * @return byte array containing the PDF content
     */
    public static byte[] generatePdf(ModelView modelView, String title) throws IOException {
        StringBuilder content = new StringBuilder();
        
        // Add title if provided
        if (title != null && !title.isEmpty()) {
            content.append(title).append("\n");
            content.append("=".repeat(title.length())).append("\n\n");
        }
        
        // Process ModelView data
        HashMap<String, Object> data = modelView.getData();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip common web attributes
            if (key.equals("error") || key.equals("success") || key.equals("message")) {
                continue;
            }
            
            content.append(formatDataEntry(key, value));
        }
        
        return generateSimplePdf(content.toString());
    }
    
    /**
     * Format a data entry for PDF output
     */
    private static String formatDataEntry(String key, Object value) {
        StringBuilder result = new StringBuilder();
        
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            result.append(formatKey(key)).append(":\n");
            
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                result.append("  ").append(i + 1).append(". ");
                
                if (hasToStringMethod(item)) {
                    result.append(item.toString());
                } else {
                    result.append(formatObject(item));
                }
                result.append("\n");
            }
            result.append("\n");
        } else if (value != null) {
            result.append(formatKey(key)).append(": ");
            if (hasToStringMethod(value)) {
                result.append(value.toString());
            } else {
                result.append(formatObject(value));
            }
            result.append("\n\n");
        }
        
        return result.toString();
    }
    
    /**
     * Format an object by extracting its fields
     */
    private static String formatObject(Object obj) {
        if (obj == null) return "null";
        
        StringBuilder result = new StringBuilder();
        result.append(obj.getClass().getSimpleName()).append(" {\n");
        
        try {
            java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(obj);
                result.append("    ").append(formatKey(field.getName())).append(": ");
                result.append(fieldValue != null ? fieldValue.toString() : "null");
                result.append("\n");
            }
        } catch (Exception e) {
            result.append("    [Error accessing fields: ").append(e.getMessage()).append("]");
        }
        
        result.append("  }");
        return result.toString();
    }
    
    /**
     * Format a key name for display
     */
    private static String formatKey(String key) {
        // Convert camelCase to Title Case
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : key.toCharArray()) {
            if (c == '_') {
                result.append(' ');
                capitalizeNext = true;
            } else if (Character.isUpperCase(c) && result.length() > 0) {
                result.append(' ').append(c);
                capitalizeNext = false;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Check if an object has a meaningful toString method
     */
    private static boolean hasToStringMethod(Object obj) {
        if (obj == null) return false;
        
        try {
            String toString = obj.toString();
            String className = obj.getClass().getName();
            // If toString returns the default Object.toString format, it's not meaningful
            return !toString.matches(className + "@[0-9a-f]+");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate a simple text-based PDF
     * This is a basic implementation - in a real scenario, you'd use a proper PDF library
     */
    private static byte[] generateSimplePdf(String content) throws IOException {
        // This is a simplified PDF generation
        // In a real implementation, you would use iText, PDFBox, or similar library
        
        StringBuilder pdf = new StringBuilder();
        
        // Basic PDF header (simplified)
        pdf.append("%PDF-1.4\n");
        pdf.append("1 0 obj\n");
        pdf.append("<<\n");
        pdf.append("/Type /Catalog\n");
        pdf.append("/Pages 2 0 R\n");
        pdf.append(">>\n");
        pdf.append("endobj\n\n");
        
        // Pages object
        pdf.append("2 0 obj\n");
        pdf.append("<<\n");
        pdf.append("/Type /Pages\n");
        pdf.append("/Kids [3 0 R]\n");
        pdf.append("/Count 1\n");
        pdf.append(">>\n");
        pdf.append("endobj\n\n");
        
        // Page object
        pdf.append("3 0 obj\n");
        pdf.append("<<\n");
        pdf.append("/Type /Page\n");
        pdf.append("/Parent 2 0 R\n");
        pdf.append("/MediaBox [0 0 612 792]\n");
        pdf.append("/Contents 4 0 R\n");
        pdf.append("/Resources <<\n");
        pdf.append("/Font <<\n");
        pdf.append("/F1 <<\n");
        pdf.append("/Type /Font\n");
        pdf.append("/Subtype /Type1\n");
        pdf.append("/BaseFont /Helvetica\n");
        pdf.append(">>\n");
        pdf.append(">>\n");
        pdf.append(">>\n");
        pdf.append(">>\n");
        pdf.append("endobj\n\n");
        
        // Content stream
        String contentStream = generateContentStream(content);
        pdf.append("4 0 obj\n");
        pdf.append("<<\n");
        pdf.append("/Length ").append(contentStream.length()).append("\n");
        pdf.append(">>\n");
        pdf.append("stream\n");
        pdf.append(contentStream);
        pdf.append("\nendstream\n");
        pdf.append("endobj\n\n");
        
        // Cross-reference table
        pdf.append("xref\n");
        pdf.append("0 5\n");
        pdf.append("0000000000 65535 f \n");
        pdf.append("0000000009 65535 n \n");
        pdf.append("0000000074 65535 n \n");
        pdf.append("0000000120 65535 n \n");
        pdf.append("0000000274 65535 n \n");
        
        // Trailer
        pdf.append("trailer\n");
        pdf.append("<<\n");
        pdf.append("/Size 5\n");
        pdf.append("/Root 1 0 R\n");
        pdf.append(">>\n");
        pdf.append("startxref\n");
        pdf.append("274\n");
        pdf.append("%%EOF\n");
        
        return pdf.toString().getBytes("ISO-8859-1");
    }
    
    /**
     * Generate PDF content stream
     */
    private static String generateContentStream(String content) {
        StringBuilder stream = new StringBuilder();
        
        stream.append("BT\n");
        stream.append("/F1 12 Tf\n");
        stream.append("50 750 Td\n");
        
        // Split content into lines and add to PDF
        String[] lines = content.split("\n");
        for (String line : lines) {
            // Escape special characters
            String escapedLine = line.replace("(", "\\(").replace(")", "\\)").replace("\\", "\\\\");
            stream.append("(").append(escapedLine).append(") Tj\n");
            stream.append("0 -15 Td\n"); // Move to next line
        }
        
        stream.append("ET\n");
        
        return stream.toString();
    }
}