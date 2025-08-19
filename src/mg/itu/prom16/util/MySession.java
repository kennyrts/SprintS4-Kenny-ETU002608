package mg.itu.prom16.util;

import jakarta.servlet.http.HttpSession;

public class MySession {
    HttpSession session;

    // Constructeur qui initialise la session
    public MySession(HttpSession session) {
        this.session = session;
    }

    // Fonction pour récupérer un objet à partir d'une clé
    public Object get(String key) {
        return session.getAttribute(key);
    }

    // Fonction pour ajouter un objet avec une clé dans la session
    public void add(String key, Object objet) {
        session.setAttribute(key, objet);
    }

    // Fonction pour supprimer un objet de la session à partir d'une clé
    public void delete(String key) {
        session.removeAttribute(key);
    }
}
