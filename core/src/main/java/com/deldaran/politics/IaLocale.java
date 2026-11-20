package com.deldaran.politics;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class IaLocale {
    private static final int PORT = 8089;
    private final File dossier;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ExecutorService fil = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ia-locale");
        t.setDaemon(true);
        return t;
    });
    private Process processus;
    private volatile boolean pret, enCours;
    private volatile String etat = "arretee";
    public volatile int reponses, echecs;
    public volatile float derniereDuree;

    public IaLocale(File dossier) {
        this.dossier = dossier;
    }

    public boolean pret() {
        return pret;
    }

    public String etat() {
        return etat;
    }

    public boolean disponible() {
        return new File(dossier, "llama/llama-server.exe").exists() && new File(dossier, "qwen3-4b.gguf").exists();
    }

    public void demarrer() {
        if (enCours || pret) return;
        if (!disponible()) {
            etat = "absente (" + dossier + ")";
            return;
        }
        enCours = true;
        etat = "demarrage...";
        fil.submit(() -> {
            try {
                if (!sante()) {
                    ProcessBuilder pb = new ProcessBuilder(new File(dossier, "llama/llama-server.exe").getAbsolutePath(), "-m", new File(dossier, "qwen3-4b.gguf").getAbsolutePath(),
                            "-ngl", "99", "-c", "2048", "--port", String.valueOf(PORT), "--no-webui");
                    pb.redirectErrorStream(true);
                    pb.redirectOutput(new File(dossier, "serveur.log"));
                    processus = pb.start();
                    Runtime.getRuntime().addShutdownHook(new Thread(this::arreterProcessus));
                    long fin = System.currentTimeMillis() + 90000;
                    while (!sante()) {
                        if (!processus.isAlive() || System.currentTimeMillis() > fin) {
                            etat = "echec du demarrage (voir serveur.log)";
                            arreterProcessus();
                            return;
                        }
                        Thread.sleep(500);
                    }
                }
                pret = true;
                etat = "prete";
            } catch (Exception e) {
                etat = "erreur : " + e.getMessage();
            } finally {
                enCours = false;
            }
        });
    }

    private boolean sante() {
        try {
            HttpRequest r = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + "/health")).timeout(Duration.ofSeconds(2)).GET().build();
            return client.send(r, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void demander(String systeme, String consigne, String situation, Consumer<String> retour) {
        if (!pret) return;
        fil.submit(() -> {
            long debut = System.nanoTime();
            try {
                String corps = "{\"messages\":[{\"role\":\"system\",\"content\":" + json(systeme) + "},{\"role\":\"user\",\"content\":" + json(consigne)
                        + "}],\"temperature\":0.6,\"top_p\":0.9,\"max_tokens\":60,\"stream\":false}";
                HttpRequest r = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + "/v1/chat/completions")).timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(corps, StandardCharsets.UTF_8)).build();
                HttpResponse<String> rep = client.send(r, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (rep.statusCode() != 200) throw new IllegalStateException("code " + rep.statusCode());
                JsonValue v = new JsonReader().parse(rep.body());
                String texte = nettoyer(v.get("choices").get(0).get("message").getString("content"));
                derniereDuree = (System.nanoTime() - debut) / 1e9f;
                if (texte.isEmpty()) throw new IllegalStateException("reponse vide");
                reponses++;
                noter(situation, consigne, texte);
                retour.accept(texte);
            } catch (Exception e) {
                echecs++;
                if (!sante()) {
                    pret = false;
                    etat = "perdue : " + e.getMessage();
                }
            }
        });
    }

    private static String nettoyer(String t) {
        t = t.replaceAll("(?s)<think>.*?</think>", "").trim();
        int saut = t.indexOf('\n');
        if (saut > 0) t = t.substring(0, saut).trim();
        t = t.replaceAll("^[\"«“\\s]+|[\"»”\\s]+$", "");
        for (int k = 0; k < t.length(); k++) {
            char c = t.charAt(k);
            if (c > 0x24F && "’‘«»“”…—–".indexOf(c) < 0) return "";
        }
        if (t.split("\\s+").length < 2) return "";
        if (t.length() > 160) {
            int point = Math.max(t.lastIndexOf('.', 160), Math.max(t.lastIndexOf('!', 160), t.lastIndexOf('?', 160)));
            t = point > 40 ? t.substring(0, point + 1) : t.substring(0, 160) + "...";
        }
        return t;
    }

    public static String affichable(String t) {
        t = t.replace('—', '-').replace('–', '-').replace('’', '\'').replace('‘', '\'').replace('«', '"').replace('»', '"').replace('“', '"').replace('”', '"').replace('…', '.').replace("œ", "oe").replace("Œ", "Oe");
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.replaceAll("[^\\x20-\\x7E]", "");
    }

    private void noter(String situation, String consigne, String texte) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dossier, "repliques.txt"), true), StandardCharsets.UTF_8)) {
            w.write("### " + LocalDateTime.now().withNano(0) + " " + situation + "\n" + consigne + "\n>>> " + texte + "\n\n");
        } catch (Exception e) {
            System.out.println("[ia] repliques.txt : " + e.getMessage());
        }
    }

    private static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    public void arreter() {
        pret = false;
        etat = "arretee";
        fil.submit(this::arreterProcessus);
    }

    private synchronized void arreterProcessus() {
        if (processus != null && processus.isAlive()) processus.destroy();
        processus = null;
    }

    public void dispose() {
        pret = false;
        arreterProcessus();
        fil.shutdownNow();
    }
}
