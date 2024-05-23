Dans le but de recuperer la classe et la methode associee a une url, voici ce qu'on va faire:
    - lancer le script.bat
    - deplacer le .jar obtenu vers lib du projet web
    - quand une classe est de type controller, mettre import mg.itu.prom16.annotations.* puis annoter comme ceci la classe @Controller
    - si on veut recuperer une methode dans une classe annotee a l'aide de l'url, annoter la methode get comme ceci @Get(url = "Controller/methode")
    - mettre tous les controlleurs dans un seul package.
    - ensuite dans web.xml declarer cette variable comme ceci
        <context-param>
            <param-name>package</param-name>
            <param-value>mg.itu.prom16.controllers</param-value>
        </context-param>
        entre les balises <param-value>, mettre le nom du package qui contient tous les controlleurs
    - declarer le FrontController comme ceci dans web.xml
        <servlet>
            <servlet-name>FrontController</servlet-name>
            <servlet-class>mg.itu.prom16.controller.FrontController</servlet-class>
        </servlet>
        <servlet-mapping>
            <servlet-name>FrontController</servlet-name>
            <url-pattern>/*</url-pattern>
        </servlet-mapping>