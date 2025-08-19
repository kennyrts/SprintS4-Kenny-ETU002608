package mg.itu.prom16.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV generator utility for Sprint framework
 * Converts ModelView data to standard CSV format
 */
public class CsvGenerator {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Generate CSV from ModelView data in standard format
     * @param modelView The ModelView containing data to export
     * @param includeHeaders Whether to include column headers
     * @param delimiter The delimiter to use (default: comma)
     * @return byte array containing the CSV content
     */
    public static byte[] generateCsv(ModelView modelView, boolean includeHeaders, String delimiter) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(baos, "UTF-8");
        
        HashMap<String, Object> data = modelView.getData();
        
        // Find the main data - prioritize lists, then complex objects, then scalars
        List<?> mainList = null;
        Object mainObject = null;
        
        // First pass: look for lists (collections of data)
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip common web attributes
            if (isWebAttribute(key)) {
                continue;
            }
            
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty() && (mainList == null || list.size() > mainList.size())) {
                    mainList = list;
                }
            }
        }
        
        // Second pass: if no list found, look for complex objects
        if (mainList == null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (isWebAttribute(key)) {
                    continue;
                }
                
                if (value != null && !isPrimitiveOrString(value) && !(value instanceof List)) {
                    mainObject = value;
                    break; // Take the first complex object found
                }
            }
        }
        
        // Generate CSV based on what we found
        if (mainList != null && !mainList.isEmpty()) {
            // Write CSV for the main list
            writeListAsCsv(writer, mainList, includeHeaders, delimiter);
        } else if (mainObject != null) {
            // Write CSV for a single complex object
            writeObjectAsCsv(writer, mainObject, includeHeaders, delimiter);
        } else {
            // Write CSV for scalar values only
            writeScalarsAsCsv(writer, data, includeHeaders, delimiter);
        }
        
        writer.flush();
        writer.close();
        
        return baos.toByteArray();
    }
    
    /**
     * Write a list of objects as standard CSV
     */
    private static void writeListAsCsv(Writer writer, List<?> list, 
                                      boolean includeHeaders, String delimiter) throws IOException {
        
        if (list.isEmpty()) return;
        
        Object firstItem = list.get(0);
        
        if (isPrimitiveOrString(firstItem)) {
            // Simple list of primitives
            if (includeHeaders) {
                writer.write("Value\n");
            }
            for (Object item : list) {
                writer.write(escapeCsvValue(formatValue(item)) + "\n");
            }
        } else {
            // List of complex objects
            Field[] fields = firstItem.getClass().getDeclaredFields();
            
            // Write headers
            if (includeHeaders) {
                boolean first = true;
                for (Field field : fields) {
                    if (!first) writer.write(delimiter);
                    writer.write(escapeCsvValue(formatKey(field.getName())));
                    first = false;
                }
                writer.write("\n");
            }
            
            // Write data rows
            for (Object item : list) {
                boolean first = true;
                for (Field field : fields) {
                    if (!first) writer.write(delimiter);
                    
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(item);
                        writer.write(escapeCsvValue(formatValue(fieldValue)));
                    } catch (IllegalAccessException e) {
                        writer.write("");
                    }
                    first = false;
                }
                writer.write("\n");
            }
        }
    }
    
    /**
     * Write a single complex object as standard CSV (one header line, one data line)
     */
    private static void writeObjectAsCsv(Writer writer, Object obj, 
                                        boolean includeHeaders, String delimiter) throws IOException {
        
        if (obj == null) return;
        
        Field[] fields = obj.getClass().getDeclaredFields();
        
        // Write headers
        if (includeHeaders) {
            boolean first = true;
            for (Field field : fields) {
                if (!first) writer.write(delimiter);
                writer.write(escapeCsvValue(formatKey(field.getName())));
                first = false;
            }
            writer.write("\n");
        }
        
        // Write data row
        boolean first = true;
        for (Field field : fields) {
            if (!first) writer.write(delimiter);
            
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(obj);
                writer.write(escapeCsvValue(formatValue(fieldValue)));
            } catch (IllegalAccessException e) {
                writer.write("");
            }
            first = false;
        }
        writer.write("\n");
    }
    
    /**
     * Write scalar values as standard CSV (one header line, one data line)
     */
    private static void writeScalarsAsCsv(Writer writer, HashMap<String, Object> data, 
                                         boolean includeHeaders, String delimiter) throws IOException {
        
        // Collect all scalar values (primitive types and strings only)
        HashMap<String, Object> scalars = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip web attributes, lists, and complex objects
            if (isWebAttribute(key) || value instanceof List || 
                (value != null && !isPrimitiveOrString(value))) {
                continue;
            }
            
            if (value != null) {
                scalars.put(key, value);
            }
        }
        
        if (scalars.isEmpty()) return;
        
        // Write headers
        if (includeHeaders) {
            boolean first = true;
            for (String key : scalars.keySet()) {
                if (!first) writer.write(delimiter);
                writer.write(escapeCsvValue(formatKey(key)));
                first = false;
            }
            writer.write("\n");
        }
        
        // Write values
        boolean first = true;
        for (Object value : scalars.values()) {
            if (!first) writer.write(delimiter);
            writer.write(escapeCsvValue(formatValue(value)));
            first = false;
        }
        writer.write("\n");
    }
    
    /**
     * Check if a key is a web attribute that should be skipped
     */
    private static boolean isWebAttribute(String key) {
        return key.equals("error") || key.equals("success") || key.equals("message");
    }
    
    /**
     * Format a value for CSV output
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof Date) {
            return DATE_FORMAT.format((Date) value);
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "Yes" : "No";
        } else {
            return value.toString();
        }
    }
    
    /**
     * Escape CSV value (handle commas, quotes, newlines)
     */
    private static String escapeCsvValue(String value) {
        if (value == null) return "";
        
        // If value contains delimiter, quotes, or newlines, wrap in quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            // Escape existing quotes by doubling them
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        
        return value;
    }
    
    /**
     * Format a key name for display (convert camelCase to Title Case)
     */
    private static String formatKey(String key) {
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
     * Check if an object is a primitive type or String
     */
    private static boolean isPrimitiveOrString(Object obj) {
        if (obj == null) return true;
        
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() || 
               clazz == String.class || 
               clazz == Integer.class || 
               clazz == Long.class || 
               clazz == Double.class || 
               clazz == Float.class || 
               clazz == Boolean.class ||
               clazz == Date.class ||
               Number.class.isAssignableFrom(clazz);
    }
}