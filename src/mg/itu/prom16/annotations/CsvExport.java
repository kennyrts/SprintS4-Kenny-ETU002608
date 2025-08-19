package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method for CSV export functionality.
 * When applied to a controller method, the response will be returned as a CSV file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CsvExport {
    /**
     * The filename for the CSV export (without .csv extension)
     * If empty, a default filename will be generated
     */
    String filename() default "";
    
    /**
     * Whether to include headers in the CSV file
     */
    boolean includeHeaders() default true;
    
    /**
     * The delimiter to use in the CSV file
     */
    String delimiter() default ",";
    
    /**
     * Whether to force download or display inline in browser
     */
    boolean forceDownload() default true;
}