package mg.itu.prom16.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;
import mg.itu.prom16.util.Mapping;
import mg.itu.prom16.annotations.Get;

public class FrontController extends HttpServlet {
    HashMap<String,Mapping> urlMapping = new HashMap<>();

    public void init() throws ServletException {
        ServletContext context = getServletContext();
        String dossier = context.getInitParameter("package");
        String chemin = "WEB-INF/classes/"+dossier.replace('.', '/');
        String absoluteDossierPath = context.getRealPath(chemin);
        File folder = new File(absoluteDossierPath);
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
                                    // Vérifier si la méthode est annotée avec @Get
                                    if (method.isAnnotationPresent(Get.class)) {
                                        // Obtenir l'annotation
                                        Get getAnnotation = method.getAnnotation(Get.class);
                                        urlMapping.put(getAnnotation.url(), new Mapping(className,method.getName()));
                                    }
                                }
                            }
                        }    
                    } catch (Exception e) {
                        
                    }                    
                }
            }
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
            Method method = clazz.getDeclaredMethod(mapping.getMethodName());
            
            // Exécuter la méthode et obtenir le résultat
            String result = (String) method.invoke(instance);
            
            // Ajouter le résultat de la méthode au message
            message += "Resultat de la methode: " + result + "<br/>";
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

    private String getClassName(String filePath, int basePathLength) {
        String className = filePath.substring(basePathLength)
                .replace(File.separatorChar, '.')
                .replace(".class", "");
        return className;
    }

    private boolean isAnnotatedWithController(String className) {
        try {
            Class<?> cls = Class.forName(className);
            for (Annotation annotation : cls.getAnnotations()) {
                if (annotation.annotationType().getSimpleName().equals("Controller")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
