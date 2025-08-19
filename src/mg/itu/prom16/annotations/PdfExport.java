package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method for PDF export functionality.
 * When applied to a controller method, the response will be returned as a PDF file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PdfExport {
    /**
     * The filename for the PDF export (without .pdf extension)
     * If empty, a default filename will be generated
     */
    String filename() default "";
    
    /**
     * The title to display in the PDF document
     * If empty, no title will be added
     */
    String title() default "";
    
    /**
     * Whether to force download or display inline in browser
     */
    boolean forceDownload() default true;
}