package mg.itu.prom16.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import mg.itu.prom16.util.HttpStatusException;
import mg.itu.prom16.util.FormException;
import mg.itu.prom16.util.Mapping;
import mg.itu.prom16.util.ModelView;
import mg.itu.prom16.util.MySession;
import mg.itu.prom16.util.UploadedFile;
import mg.itu.prom16.annotations.FileUpload;
import mg.itu.prom16.annotations.FormField;
import mg.itu.prom16.annotations.FormObject;
import mg.itu.prom16.annotations.Get;
import mg.itu.prom16.annotations.Post;
import mg.itu.prom16.annotations.Param;
import mg.itu.prom16.annotations.Restapi;
import mg.itu.prom16.annotations.Url;
import mg.itu.prom16.annotations.validation.Email;
import mg.itu.prom16.annotations.validation.Max;
import mg.itu.prom16.annotations.validation.Min;
import mg.itu.prom16.annotations.validation.Required;
import mg.itu.prom16.annotations.Auth;
import mg.itu.prom16.annotations.PdfExport;
import mg.itu.prom16.annotations.CsvExport;
import mg.itu.prom16.util.PdfGenerator;
import mg.itu.prom16.util.CsvGenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FrontController extends HttpServlet {
    HashMap<String,Mapping> urlMapping = new HashMap<>();
    private String lastUrl;

    public void init() throws ServletException {
        ServletContext context = getServletContext();
        String dossier = context.getInitParameter("package");
        if (dossier == null || dossier.isEmpty()) {
            throw new ServletException("Le paramètre de package est vide ou non spécifié.");
        }
        String chemin = "WEB-INF/classes/"+dossier.replace('.', '/');
        String absoluteDossierPath = context.getRealPath(chemin);
        File folder = new File(absoluteDossierPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new ServletException("Le package spécifié n'existe pas : " + dossier);
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".class")) {
                    String className = dossier+"."+getClassName(file.getPath(), folder.getPath().length() + 1);
                    try {
                        Class<?> cls = Class.forName(className);
                        for (Annotation annotation : cls.getAnnotations()) {
                            if (annotation.annotationType().getSimpleName().equals("Controller")) {
                                    for (Method method : cls.getDeclaredMethods()) {
                                    // Vérifier si la méthode est annotée avec @Url
                                    if (method.isAnnotationPresent(Url.class)) {
                                        // Obtenir l'annotation                                        
                                        Url urlAnnotation = method.getAnnotation(Url.class);
                                        String url = urlAnnotation.value();
                                        if (urlMapping.containsKey(url)) {
                                            throw new ServletException("L'URL '" + url + "' est attachée à plusieurs fonctions.");
                                        }
                                        urlMapping.put(url, new Mapping(className, method.getName()));
                                    }
                                }
                            }
                        }    
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new ServletException("Erreur lors du traitement de la classe : " + className, e);
                    }                    
                }
            }
        }
        else {
            throw new ServletException("Le package spécifié est vide : " + dossier);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            processRequest(request, response);    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            processRequest(request, response);    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            // Obtenir l'URI complète
            String uri = request.getRequestURI();            
        
            // Obtenir le contexte de l'application
            String contextPath = request.getContextPath();
            
            // Extraire la partie de l'URI après le contexte
            String relativeUri = uri.substring(contextPath.length() + 1);

            if (!urlMapping.containsKey(relativeUri)) {            
                throw new HttpStatusException(HttpServletResponse.SC_NOT_FOUND, "Aucun mapping trouvé pour l'URL : " + relativeUri);
            } else {
                Mapping mapping = urlMapping.get(relativeUri);
                // Charger la classe
                Class<?> clazz = Class.forName(mapping.getClassName());
                
                // Check for @Auth annotation at the class level
                String requiredRole = null;
                if (clazz.isAnnotationPresent(Auth.class)) {
                    Auth classAuthAnnotation = clazz.getAnnotation(Auth.class);
                    requiredRole = classAuthAnnotation.value();
                    
                    // Get the role from the session using the context parameter
                    String sessionRoleParam = getServletContext().getInitParameter("session-role");
                    HttpSession session = request.getSession();
                    String userRole = (String) session.getAttribute(sessionRoleParam); // Use the parameter from web.xml

                    // Check if the user has the required role
                    if (requiredRole.isEmpty()) {
                        // If the required role is empty, allow access if userRole is not null or empty
                        if (userRole != null && !userRole.isEmpty()) {
                            // Access granted
                        } else {
                            throw new HttpStatusException(HttpServletResponse.SC_FORBIDDEN, "Access denied: insufficient permissions.");
                        }
                    } else {
                        // If a specific role is required, check against it
                        if (userRole == null || !userRole.equals(requiredRole)) {
                            throw new HttpStatusException(HttpServletResponse.SC_FORBIDDEN, "Access denied: insufficient permissions.");
                        }
                    }
                }

                // Créer une instance de la classe
                Object instance = clazz.getDeclaredConstructor().newInstance();
                
                // Obtenir la méthode                
                Method method = null;
                for (Method fonction : clazz.getDeclaredMethods()) {
                    if (fonction.getName().equals(mapping.getMethodName())) {
                        method = fonction;
                    }
                }

                if (method == null) {
                    throw new HttpStatusException(HttpServletResponse.SC_NOT_FOUND, "Méthode non trouvée : " + mapping.getMethodName());
                }

                // Vérifier le type de requête HTTP et l'annotation de la méthode
                String httpMethod = request.getMethod();
                boolean isGet = method.isAnnotationPresent(Get.class);
                // System.out.println("isGet : "+isGet);
                boolean isPost = method.isAnnotationPresent(Post.class);
                // System.out.println("isPost : "+isPost);
                // System.out.println("httpMethod : "+httpMethod);
                if (!isGet && !isPost) {
                    isGet = true; // Par défaut, considérer comme GET
                }

                // if (httpMethod.equals("GET") && !isGet) {
                //     throw new HttpStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "L'URL " + relativeUri + " n'accepte que POST, mais GET a ete utilise.");
                // }

                // if (httpMethod.equals("POST") && !isPost) {                    
                //     throw new HttpStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "L'URL " + relativeUri + " n'accepte que GET, mais POST a ete utilise.");
                // }

                // Check for @Auth annotation
                if (method.isAnnotationPresent(Auth.class)) {
                    Auth authAnnotation = method.getAnnotation(Auth.class);
                    requiredRole = authAnnotation.value();
                    
                    // Get the role from the session using the context parameter
                    String sessionRoleParam = getServletContext().getInitParameter("session-role");
                    HttpSession session = request.getSession();
                    String userRole = (String) session.getAttribute(sessionRoleParam); // Use the parameter from web.xml

                    // Check if the user has the required role
                    if (requiredRole.isEmpty()) {
                        // If the required role is empty, allow access if userRole is not null or empty
                        if (userRole != null && !userRole.isEmpty()) {
                            // Access granted
                        } else {
                            throw new HttpStatusException(HttpServletResponse.SC_FORBIDDEN, "Access denied: insufficient permissions.");
                        }
                    } else {
                        // If a specific role is required, check against it
                        if (userRole == null || !userRole.equals(requiredRole)) {
                            throw new HttpStatusException(HttpServletResponse.SC_FORBIDDEN, "Access denied: insufficient permissions.");
                        }
                    }
                }

                Parameter[] parameters = method.getParameters();
                Object[] parameterValues = new Object[parameters.length];

                boolean isMultipart = request.getContentType() != null && request.getContentType().startsWith("multipart/form-data");

                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.getType().equals(MySession.class)) {                    
                        parameterValues[i] = new MySession(request.getSession());
                    } else if (parameter.isAnnotationPresent(Param.class)) {
                        Param paramAnnotation = parameter.getAnnotation(Param.class);
                        String paramName = paramAnnotation.name();
                        String paramValue = request.getParameter(paramName);
                        parameterValues[i] = paramValue;
                    } else if (parameter.isAnnotationPresent(FormObject.class)) {
                            Object formObject = parameter.getType().getDeclaredConstructor().newInstance();
                            for (Field field : formObject.getClass().getDeclaredFields()) {
                            String fieldName = field.getName();
                            if (field.isAnnotationPresent(FormField.class)) {
                                FormField formField = field.getAnnotation(FormField.class);
                                if (!formField.name().isEmpty()) {
                                    fieldName = formField.name();
                                }
                            }
                            String paramValue = request.getParameter(fieldName);
                            field.setAccessible(true);
                            field.set(formObject, convertToFieldType(field, paramValue));
                        }
                        try {
                            validateFormObject(formObject);                           
                        }
                        catch (FormException e) {
                            // Handle FormException                            
                            System.out.println("Form URL: " + e.getFormUrl());
                            System.out.println("Submitted Values: " + e.getSubmittedValues());
                            System.out.println("Validation Errors:");
                            for (Map.Entry<String, List<String>> entry : e.getErrors().entrySet()) {
                                System.out.println("Field: " + entry.getKey() + " - Errors: " + String.join(", ", entry.getValue()));
                            }
                            // Add errors and submitted values to request attributes
                            request.setAttribute("errors", e.getErrors());
                            request.setAttribute("submittedValues", e.getSubmittedValues());
                            request.setAttribute("error", "Please correct the form errors");                                                        
                            
                            // Forward back to the form
                            RequestDispatcher dispatcher = request.getRequestDispatcher(e.getFormUrl());
                            dispatcher.forward(request, response);
                            return;
                        }                        
                        parameterValues[i] = formObject;
                    } else if (parameter.getType().equals(UploadedFile.class)) {
                        if (isMultipart) {
                            // System.out.println("isMultipart");
                            if (parameter.isAnnotationPresent(FileUpload.class)) {
                                String paramName = parameter.getAnnotation(FileUpload.class).name();
                                // String paramName = "file";
                                // System.out.println("paramName : "+paramName);
                                Part filePart = request.getPart(paramName); // Récupération de la partie fichier                                                        
                    
                                if (filePart != null) {
                                    // System.out.println("filePart not null");
                                    String fileName = extractFileName(filePart);
                                    String contentType = filePart.getContentType();
                                    long size = filePart.getSize();
                                    InputStream inputStream = filePart.getInputStream();   
                                                                
                    
                                    // Création de l'objet UploadedFile
                                    parameterValues[i] = new UploadedFile(fileName, contentType, size, inputStream);
                                } else {
                                    // System.out.println("filePart null");
                                    parameterValues[i] = null; // Pas de fichier associé
                                }   
                            }                            
                        } else {
                            parameterValues[i] = null; // Pas une requête multipart
                        }
                    } else{                        
                        String paramName = parameter.getName();
                        String paramValue = request.getParameter(paramName);
                        parameterValues[i] = paramValue;
                    }
                }
                lastUrl = relativeUri;
                // Exécuter la méthode et obtenir le résultat
                Object result = method.invoke(instance,parameterValues);

                if (method.isAnnotationPresent(PdfExport.class)) {
                    // Handle PDF export
                    PdfExport pdfAnnotation = method.getAnnotation(PdfExport.class);
                    
                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        
                        // Generate PDF
                        String title = pdfAnnotation.title().isEmpty() ? "Export PDF" : pdfAnnotation.title();
                        byte[] pdfBytes = PdfGenerator.generatePdf(mv, title);
                        
                        // Set response headers
                        response.setContentType("application/pdf");
                        String filename = pdfAnnotation.filename().isEmpty() ? "export.pdf" : pdfAnnotation.filename() + ".pdf";
                        
                        if (pdfAnnotation.forceDownload()) {
                            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                        } else {
                            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
                        }
                        
                        response.setContentLength(pdfBytes.length);
                        response.getOutputStream().write(pdfBytes);
                        response.getOutputStream().flush();
                    } else {
                        throw new HttpStatusException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "PDF export requires ModelView return type");
                    }
                } else if (method.isAnnotationPresent(CsvExport.class)) {
                    // Handle CSV export
                    CsvExport csvAnnotation = method.getAnnotation(CsvExport.class);
                    
                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        
                        // Generate CSV
                        boolean includeHeaders = csvAnnotation.includeHeaders();
                        String delimiter = csvAnnotation.delimiter();
                        byte[] csvBytes = CsvGenerator.generateCsv(mv, includeHeaders, delimiter);
                        
                        // Set response headers
                        response.setContentType("text/csv;charset=UTF-8");
                        String filename = csvAnnotation.filename().isEmpty() ? "export.csv" : csvAnnotation.filename() + ".csv";
                        
                        if (csvAnnotation.forceDownload()) {
                            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                        } else {
                            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
                        }
                        
                        response.setContentLength(csvBytes.length);
                        response.getOutputStream().write(csvBytes);
                        response.getOutputStream().flush();
                    } else {
                        throw new HttpStatusException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CSV export requires ModelView return type");
                    }
                } else if (method.isAnnotationPresent(Restapi.class)) {
                    response.setContentType("application/json;charset=UTF-8");
                    if (result instanceof ModelView) {
                        // Si result est un ModelView, extraire les données et les convertir en JSON
                        ModelView mv = (ModelView) result;
                        HashMap<String, Object> data = mv.getData();
                        writeJsonResponse(response, data);
                    } else {
                        // Sinon, transformer directement result en JSON
                        writeJsonResponse(response, result);
                    }
                } else if (result instanceof String) {
                    // Si le résultat est une String, l'ajouter directement au message
                    response.getWriter().write((String) result);
                } else if (result instanceof ModelView) {
                    ModelView mv = (ModelView) result;
                    // Récupérer l'URL et dispatcher les données vers cet URL
                    String destinationUrl = mv.getUrl();
                    HashMap<String, Object> data = mv.getData();

                    for (String key : data.keySet()) {
                        request.setAttribute(key, data.get(key));
                    }

                    RequestDispatcher dispatcher = request.getRequestDispatcher(destinationUrl);
                    dispatcher.forward(request, response);                    
                } else {
                    throw new HttpStatusException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Type de retour non reconnu : " + result.getClass().getName());
                }
            }            
        } catch (HttpStatusException e) {
            handleError(response, e.getStatusCode(), e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            e.printStackTrace();
        }   
    }

    private Object convertToFieldType(Field field, String paramValue) {
        Class<?> fieldType = field.getType();
        try {            
            if (paramValue == null || paramValue.trim().isEmpty()) {
                if (fieldType == int.class) return 0;
                if (fieldType == long.class) return 0L;
                if (fieldType == float.class) return 0.0f;
                if (fieldType == double.class) return 0.0d;
                if (fieldType == boolean.class) return false;
                return null;
            }
            if (fieldType == int.class || fieldType == Integer.class) {
                return Integer.parseInt(paramValue);
            } else if (fieldType == long.class || fieldType == Long.class) {
                return Long.parseLong(paramValue);
            } else if (fieldType == float.class || fieldType == Float.class) {
                return Float.parseFloat(paramValue);
            } else if (fieldType == double.class || fieldType == Double.class) {
                return Double.parseDouble(paramValue);
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                return Boolean.parseBoolean(paramValue);
            } else if (fieldType == java.sql.Timestamp.class) {
                if (paramValue.contains("T")) {
                    // Handle HTML datetime-local format (yyyy-MM-ddThh:mm)
                    return java.sql.Timestamp.valueOf(paramValue.replace("T", " ") + ":00");
                }
                return java.sql.Timestamp.valueOf(paramValue);
            }   
        } catch (Exception e) {
            System.out.println(field.getName() + " : "+paramValue);
            // Return default values for primitive types on error
            if (fieldType == int.class) return 0;
            if (fieldType == long.class) return 0L;
            if (fieldType == float.class) return 0.0f;
            if (fieldType == double.class) return 0.0d;
            if (fieldType == boolean.class) return false;
        }
        return paramValue;
    }

    private void handleError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Erreur</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p>Erreur " + statusCode + " : " + message + "</p>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    private String getClassName(String filePath, int basePathLength) {
        String className = filePath.substring(basePathLength)
                .replace(File.separatorChar, '.')
                .replace(".class", "");
        return className;
    }

    private void writeJsonResponse(HttpServletResponse response, Object data) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (PrintWriter writer = response.getWriter()) {
            writer.write(gson.toJson(data));
        }
    }
    
    private String extractFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String content : contentDisposition.split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf("=") + 2, content.length() - 1);
            }
        }
        return null;
    }

    private void validateFormObject(Object formObject) throws FormException, HttpStatusException {
        HashMap<String, List<String>> errors = new HashMap<>();
        HashMap<String, Object> submittedValues = new HashMap<>();

        for (Field field : formObject.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(formObject);
                submittedValues.put(field.getName(), value); // Store the submitted value
                List<String> fieldErrors = new ArrayList<>();

                // Check for @Required
                if (field.isAnnotationPresent(Required.class)) {
                    Required required = field.getAnnotation(Required.class);
                    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                        fieldErrors.add(required.message());
                    }
                }

                // Check for @Email
                if (field.isAnnotationPresent(Email.class)) {
                    Email email = field.getAnnotation(Email.class);
                    if (value instanceof String) {
                        String emailStr = (String) value;
                        if (!emailStr.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                            fieldErrors.add(email.message());
                        }
                    }
                }

                // Check for @Min
                if (field.isAnnotationPresent(Min.class)) {
                    Min min = field.getAnnotation(Min.class);
                    if (value instanceof Integer && (Integer) value < min.value()) {                        
                        fieldErrors.add(min.message());
                    }                    
                }

                // Check for @Max
                if (field.isAnnotationPresent(Max.class)) {
                    Max max = field.getAnnotation(Max.class);
                    if (value instanceof Integer && (Integer) value > max.value()) {
                        fieldErrors.add(max.message());
                    }
                }

                // If there are errors for this field, add them to the map
                if (!fieldErrors.isEmpty()) {
                    errors.put(field.getName(), fieldErrors);
                }

            } catch (IllegalAccessException e) {
                throw new HttpStatusException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validation error: " + e.getMessage());
            }
        }

        // If there are validation errors, throw FormException
        if (!errors.isEmpty()) {
            String actionType = "GET"; // Determine the action type (you can modify this logic as needed)
            throw new FormException(lastUrl, actionType, errors, submittedValues);
        }
    }
}
