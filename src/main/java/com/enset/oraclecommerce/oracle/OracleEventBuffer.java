package com.enset.oraclecommerce.oracle;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory ring buffer of the latest user requests (lost on restart, which is fine for a live view). */

public class OracleEventBuffer {

    private static final int MAX = 100;

    private final ConcurrentLinkedDeque<OracleEvent> events = new ConcurrentLinkedDeque<>();
    private final AtomicLong seq = new AtomicLong();

    public OracleEvent publish(String method, String path, int status, long durationMs,
                               List<OracleEvent.Stmt> statements) {
        boolean write = statements.stream().anyMatch(s ->
                s.type().equals("INSERT") || s.type().equals("UPDATE") || s.type().equals("DELETE"));
        String operation = operationOf(method, path);
        String entity = entityOf(path);

        OracleEvent ev = new OracleEvent(seq.incrementAndGet(), System.currentTimeMillis(), method, path,
                operation, entity, operation + " " + entity, write, status, durationMs, statements,
                "app", write, null);
        return add(ev);
    }

    /**
     * A statement typed in the SQL editor.
     * operation: Ajout / Modification / Suppression / Consultation / COMMIT / ROLLBACK
     */
    public OracleEvent publishSql(String source, String type, String table, List<OracleEvent.Stmt> statements,
                                  boolean dml, long durationMs, OracleEvent.Tx tx) {
        String operation = switch (type) {
            case "INSERT" -> "Ajout";
            case "UPDATE", "MERGE" -> "Modification";
            case "DELETE" -> "Suppression";
            case "COMMIT", "ROLLBACK" -> type;
            default -> "Consultation";
        };
        String label = "SQL · " + type + (table != null ? " " + table : "")
                + ("sql-other".equals(source) ? " (autre session)" : "");
        OracleEvent ev = new OracleEvent(seq.incrementAndGet(), System.currentTimeMillis(), "SQL", "/sql",
                operation, table != null ? table.toLowerCase() : "", label, dml, 200, durationMs, statements,
                source, false, tx);
        return add(ev);
    }

    private OracleEvent add(OracleEvent ev) {
        events.addLast(ev);
        while (events.size() > MAX) events.pollFirst();
        return ev;
    }

    /** since < 0 : the last few events as history (not to be animated); otherwise only newer ones. */
    public OracleEvent.Page since(long since) {
        List<OracleEvent> all = new ArrayList<>(events);
        long last = all.isEmpty() ? seq.get() : all.get(all.size() - 1).id();
        if (since < 0) {
            return new OracleEvent.Page(last, all.subList(Math.max(0, all.size() - 15), all.size()));
        }
        return new OracleEvent.Page(last, all.stream().filter(e -> e.id() > since).toList());
    }

    private static String operationOf(String method, String path) {
        if (!"POST".equals(method)) return "Consultation";
        if (path.endsWith("/delete")) return "Suppression";
        if (path.endsWith("/annuler")) return "Annulation";
        if (path.matches(".*/(edit|statut|activer|desactiver)$") || path.matches("^/[^/]+/\\d+$")) return "Modification";
        return "Ajout";
    }

    private static String entityOf(String path) {
        String first = path.replaceFirst("^/", "").split("/")[0];
        return switch (first) {
            case "produits" -> "produit";
            case "categories" -> "catégorie";
            case "clients" -> "client";
            case "commandes" -> "commande";
            case "paiements" -> "paiement";
            case "mouvements-stock" -> "mouvement de stock";
            case "roles" -> "rôle";
            case "utilisateurs" -> "utilisateur";
            default -> first.isEmpty() ? "accueil" : first;
        };
    }
}
