Dans le but d'envoyer des données d'un formulaire vers un contrôleur, voici ce qu'on va faire:
    - lancer le script.bat
    - deplacer le .jar obtenu vers lib du projet web
    - quand une classe est de type controller, mettre import mg.itu.prom16.annotations.* puis annoter comme ceci la classe @Controller
    - si on veut recuperer une methode dans une classe annotee a l'aide de l'url, annoter la methode get comme ceci @Get(url = "Controller/methode"), cette methode doit avoir obligatoirement String ou ModelView comme type de retour
        * Si cette methode a besoin d'arguments alors annotez le parametre avec @Param(name = "valeur") si le parametre n'a pas d'annotation @Param alors le nom du parametre sera pris en compte. Ca correspond au nom de la variable dans la requete http. Lors de la compilation des controlleurs dans le projet, ajouter -parameters a javac
        * On peut egalement mettre un objet annote FormObject en argument, lorsque l'on cree cette classe alors les attributs auront une annotation FormField(name="variable"). C'est a partir de cela qu'on l'associe a une variable dans la requete http ou alors si il n'est pas annotee, ce sera a partir de son nom
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