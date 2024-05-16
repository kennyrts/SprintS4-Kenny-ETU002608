Dans le but de lister tous les controlleurs, voici ce qu'on va faire:
    - lancer le script.bat
    - deplacer le .jar obtenu vers lib du projet web
    - quand une classe est de type controller, mettre import mg.itu.prom16.annotations.* puis annoter comme ceci la classe @AnnotationController
    - mettre tous les controlleurs dans un seul package.
    - ensuite dans web.xml declarer cette variable comme ceci
        <context-param>
            <param-name>package</param-name>
            <param-value>mg.itu.prom16.controllers</param-value>
        </context-param>
    entre les balises <param-value>, mettre le nom du package qui contient tous les controlleurs
    - faire le mapping du Servlet FrontController comme ceci par exemple (servlet-class n'est pas modifiable, le reste si)
        <servlet>
            <servlet-name>FrontController</servlet-name>
            <servlet-class>mg.itu.prom16.controller.FrontController</servlet-class>
        </servlet>
        <servlet-mapping>
            <servlet-name>FrontController</servlet-name>
            <url-pattern>/front</url-pattern>
        </servlet-mapping>