package mg.itu.prom16.util;

import java.util.HashMap;

public class ModelView {
    private String url;
    private HashMap<String, Object> data;

    // Constructeur par défaut
    public ModelView() {
        this.data = new HashMap<>();
    }

    // Constructeur avec l'URL
    public ModelView(String url) {
        this.url = url;
        this.data = new HashMap<>();
    }

    // Getter pour l'URL
    public String getUrl() {
        return url;
    }

    // Setter pour l'URL
    public void setUrl(String url) {
        this.url = url;
    }

    // Getter pour les données
    public HashMap<String, Object> getData() {
        return data;
    }

    // Setter pour les données
    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    // Méthode pour ajouter une donnée
    public void addObject(String key, Object value) {
        this.data.put(key, value);
    }

    // Méthode pour obtenir une donnée
    public Object getObject(String key) {
        return this.data.get(key);
    }

    // Méthode pour supprimer une donnée
    public void removeObject(String key) {
        this.data.remove(key);
    }

    public static void main(String[] args) {
        // Exemple d'utilisation
        ModelView mv = new ModelView("http://example.com");
        mv.addObject("name", "John Doe");
        mv.addObject("age", 30);

        System.out.println("URL: " + mv.getUrl());
        System.out.println("Données: " + mv.getData());
    }
}
