package labirinto;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Interface local: os algoritmos continuam sendo executados em Java. */
public final class ServidorWeb {
    private final Labirinto original;
    private ServidorWeb(Labirinto mapa) { original = mapa; }

    public static void iniciar(Path arquivo, int porta) throws IOException {
        ServidorWeb app = new ServidorWeb(LeitorLabirinto.ler(arquivo));
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        servidor.createContext("/", app::atender);
        servidor.start();
        System.out.println("Mapa espacial: http://localhost:" + servidor.getAddress().getPort());
        System.out.println("Pressione Ctrl+C para encerrar.");
    }

    private void atender(HttpExchange req) throws IOException {
        try {
            String rota = req.getRequestURI().getPath();
            if (rota.equals("/api/mapa") && req.getRequestMethod().equals("GET")) {
                responder(req, 200, "application/json", mapaJson(original));
            } else if (rota.equals("/api/buscar") && req.getRequestMethod().equals("POST")) {
                byte[] bytes = req.getRequestBody().readNBytes(65537);
                if (bytes.length > 65536) { responder(req, 413, "text/plain", "Mapa muito grande (máximo 64 KB)."); return; }
                String texto = new String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "").strip();
                Labirinto mapa = new Labirinto(Arrays.asList(texto.split("\\R", -1)));
                List<Posicao> metas = mapa.saidas();
                String query = req.getRequestURI().getRawQuery();
                if (query != null && query.startsWith("objetivo=")) {
                    String[] p = URLDecoder.decode(query.substring(9), StandardCharsets.UTF_8).split(",");
                    if (p.length != 2) throw new IllegalArgumentException("Destino inválido.");
                    Posicao meta = new Posicao(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                    if (!mapa.saidas().contains(meta)) throw new IllegalArgumentException("Escolha uma saída do mapa.");
                    metas = List.of(meta);
                }
                responder(req, 200, "application/json", "{\"mapa\":" + mapaJson(mapa)
                        + ",\"resultados\":[" + resultadoJson(new Dijkstra().buscar(mapa, metas, true))
                        + "," + resultadoJson(new AEstrela().buscar(mapa, metas, true)) + "]}");
            } else if (req.getRequestMethod().equals("GET") && List.of("/", "/index.html", "/style.css", "/app.js", "/favicon.svg").contains(rota)) {
                String nome = rota.equals("/") ? "index.html" : rota.substring(1);
                try (var recurso = ServidorWeb.class.getResourceAsStream("/web/" + nome)) {
                    if (recurso == null) { responder(req, 404, "text/plain", "Recurso não encontrado."); return; }
                    String tipo = nome.endsWith("css") ? "text/css" : nome.endsWith("js") ? "text/javascript"
                            : nome.endsWith("svg") ? "image/svg+xml" : "text/html";
                    responder(req, 200, tipo, new String(recurso.readAllBytes(), StandardCharsets.UTF_8));
                }
            } else responder(req, 404, "text/plain", "Rota não encontrada.");
        } catch (IllegalArgumentException erro) {
            responder(req, 400, "text/plain", erro.getMessage());
        } finally { req.close(); }
    }

    private static void responder(HttpExchange req, int status, String tipo, String texto) throws IOException {
        byte[] dados = texto.getBytes(StandardCharsets.UTF_8);
        req.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        req.getResponseHeaders().set("Cache-Control", "no-store");
        req.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        req.sendResponseHeaders(status, dados.length);
        req.getResponseBody().write(dados);
    }
    private static String posicao(Posicao p) { return "[" + p.linha() + "," + p.coluna() + "]"; }
    private static String mapaJson(Labirinto m) {
        StringBuilder s = new StringBuilder("{\"linhas\":[");
        for (int l = 0; l < m.linhas(); l++) {
            if (l > 0) s.append(',');
            s.append('"');
            for (int c = 0; c < m.colunas(); c++) s.append(m.valor(l, c));
            s.append('"');
        }
        s.append("],\"inicio\":").append(posicao(m.inicio())).append(",\"saidas\":[");
        for (int i = 0; i < m.saidas().size(); i++) { if (i > 0) s.append(','); s.append(posicao(m.saidas().get(i))); }
        return s.append("]}").toString();
    }
    private static String resultadoJson(ResultadoBusca r) {
        StringBuilder s = new StringBuilder("{\"algoritmo\":\"").append(r.algoritmo()).append("\",\"encontrou\":")
                .append(r.encontrou()).append(",\"custo\":").append(r.custo()).append(",\"processadas\":").append(r.processadas())
                .append(",\"descobertas\":").append(r.descobertas()).append(",\"nanos\":").append(r.nanos()).append(",\"caminho\":[");
        for (int i = 0; i < r.caminho().size(); i++) { if (i > 0) s.append(','); s.append(posicao(r.caminho().get(i))); }
        s.append("],\"eventos\":[");
        for (int i = 0; i < r.eventos().size(); i++) {
            EventoBusca e = r.eventos().get(i);
            if (i > 0) s.append(',');
            s.append("{\"tipo\":\"").append(e.tipo()).append("\",\"posicao\":").append(posicao(e.posicao()))
                    .append(",\"g\":").append(e.g()).append(",\"h\":").append(e.h()).append('}');
        }
        return s.append("]}").toString();
    }
}
