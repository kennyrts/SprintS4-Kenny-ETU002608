package mg.itu.prom16.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;

public class FrontController extends HttpServlet {

    List<String> annotatedClasses = new ArrayList<>();
    boolean checked = false;


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
        // Récupération du paramètre de contexte
        ServletContext context = getServletContext();
        String dossier = context.getInitParameter("package");
        String chemin = "WEB-INF/classes/"+dossier.replace('.', '/');
        String absoluteDossierPath = context.getRealPath(chemin);

        if (!checked) {
            checked = true;
            // Récupération des fichiers .class dans le dossier spécifié
            File folder = new File(absoluteDossierPath);
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".class")) {
                        String className = dossier+"."+getClassName(file.getPath(), folder.getPath().length() + 1);
                        if (isAnnotatedWithAnnotationController(className)) {
                            annotatedClasses.add(className);
                        }
                    }
                }
            }
        }

        // Utilisation de la variable pour afficher les classes annotées
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet FrontController</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Annotated Classes:</h1>");
            for (String className : annotatedClasses) {
                out.println("<p>" + className + "</p>");
            }
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

    private boolean isAnnotatedWithAnnotationController(String className) {
        try {
            Class<?> cls = Class.forName(className);
            for (Annotation annotation : cls.getAnnotations()) {
                if (annotation.annotationType().getSimpleName().equals("AnnotationController")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
