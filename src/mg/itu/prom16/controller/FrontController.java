package mg.itu.prom16.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import javax.servlet.*;
import javax.servlet.http.*;
import mg.itu.prom16.util.Mapping;
import mg.itu.prom16.util.ModelView;
import mg.itu.prom16.util.MySession;
import mg.itu.prom16.annotations.FormField;
import mg.itu.prom16.annotations.FormObject;
import mg.itu.prom16.annotations.Get;
import mg.itu.prom16.annotations.Post;
import mg.itu.prom16.annotations.Param;
import mg.itu.prom16.annotations.Restapi;
import mg.itu.prom16.annotations.Url;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FrontController extends HttpServlet {
    HashMap<String,Mapping> urlMapping = new HashMap<>();

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
        // Obtenir l'URI complète
        String uri = request.getRequestURI();
    
        // Obtenir le contexte de l'application
        String contextPath = request.getContextPath();
        
        // Extraire la partie de l'URI après le contexte
        String relativeUri = uri.substring(contextPath.length() + 1);

        if (!urlMapping.containsKey(relativeUri)) {
            handleError(response, "Aucun mapping trouvé pour l'URL : " + relativeUri);
            return;
        }


        String message = "URL : "+relativeUri+ "<br/>";
        message += "Mapping: <br>";
        if (urlMapping.containsKey(relativeUri)) {
            Mapping mapping = urlMapping.get(relativeUri);
            
            message += "Classe:"+mapping.getClassName()+ "<br/>";
            message += "Methode:"+mapping.getMethodName()+ "<br/>";
            // Charger la classe
            Class<?> clazz = Class.forName(mapping.getClassName());
            
            // Créer une instance de la classe
            Object instance = clazz.getDeclaredConstructor().newInstance();
            
            // Obtenir la méthode
            // Method method = clazz.getDeclaredMethod(mapping.getMethodName());
            Method method = null;
            for (Method fonction : clazz.getDeclaredMethods()) {
                if (fonction.getName().equals(mapping.getMethodName())) {
                    method = fonction;
                }
            }

            // Vérifier le type de requête HTTP et l'annotation de la méthode
            String httpMethod = request.getMethod();
            boolean isGet = method.isAnnotationPresent(Get.class);
            boolean isPost = method.isAnnotationPresent(Post.class);
            
            if (!isGet && !isPost) {
                isGet = true; // Par défaut, considérer comme GET
            }

            if (httpMethod.equals("GET") && !isGet) {
                handleError(response, "Méthode non autorisée : L'URL " + relativeUri + " n'accepte que POST, mais GET a été utilisé.");
                return;
            }

            if (httpMethod.equals("POST") && !isPost) {
                handleError(response, "Méthode non autorisée : L'URL " + relativeUri + " n'accepte que GET, mais POST a été utilisé.");
                return;
            }

            Parameter[] parameters = method.getParameters();
            Object[] parameterValues = new Object[parameters.length];
            // System.out.println("Parameters.length:"+parameters.length);
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (parameter.getType().equals(MySession.class)) {                    
                    parameterValues[i] = new MySession(request.getSession());
                } else if (parameter.isAnnotationPresent(Param.class)) {
                    Param paramAnnotation = parameter.getAnnotation(Param.class);
                    String paramName = paramAnnotation.name();
                    String paramValue = request.getParameter(paramName);
                    parameterValues[i] = paramValue;
                }else if (parameter.isAnnotationPresent(FormObject.class)) {
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
                    parameterValues[i] = formObject;
                } 
                else{
                    // System.out.println("parametre:"+parameter.getName());
                    String paramName = parameter.getName();
                    String paramValue = request.getParameter(paramName);
                    parameterValues[i] = paramValue;
                }
            }
            // Exécuter la méthode et obtenir le résultat
            Object result = method.invoke(instance,parameterValues);

            if (method.isAnnotationPresent(Restapi.class)) {
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
                message += "Resultat de la methode: " + result + "<br/>";
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
                return;
            } else {
                handleError(response, "Type de retour non reconnu : " + result.getClass().getName());
            }
        }
        else{
            message += "Aucun Mapping associe";
        }

        // Utilisation de la variable pour afficher les classes annotées
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet FrontController</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<p>" + message + "</p>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    private Object convertToFieldType(Field field, String paramValue) {
        Class<?> fieldType = field.getType();
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
        }
        return paramValue;
    }

    private void handleError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Erreur</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p>" + message + "</p>");
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
}
