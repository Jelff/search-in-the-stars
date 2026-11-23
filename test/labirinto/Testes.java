package labirinto;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Testes sem dependências. BFS independente serve de referência, apenas aqui. */
public final class Testes {
    private static int verificacoes;
    private Testes() {}
    public static void main(String[] args) throws Exception {
        Labirinto original = LeitorLabirinto.ler(Path.of("dados/labirinto.dat"));
        conferir(original.linhas() == 12 && original.colunas() == 10, "Dimensões originais");
        conferir(original.inicio().equals(new Posicao(2, 1)), "Início original");
        conferir(original.saidas().size() == 3, "Três saídas");
        verificarBusca(original, original.saidas(), 13);
        verificarBusca(original, List.of(new Posicao(5, 9)), 13);
        verificarBusca(original, List.of(new Posicao(10, 7)), 22);
        verificarBusca(original, List.of(new Posicao(8, 0)), -1);
        conferir(new AEstrela().heuristica(original.inicio(), original.saidas()) == 7, "Manhattan inicial");
        Labirinto curto = new Labirinto(List.of("23"));
        verificarBusca(curto, curto.saidas(), 1);
        Labirinto bloqueado = new Labirinto(List.of("213"));
        verificarBusca(bloqueado, bloqueado.saidas(), -1);
        Labirinto empate = new Labirinto(List.of("030", "020", "030"));
        verificarBusca(empate, empate.saidas(), 1);
        testarArquivos();
        testarAleatorios();
        testarPintura(original);
        System.out.println("OK: " + verificacoes + " verificações. Arquivo original, validação, 400 mapas aleatórios e desenho dos painéis.");
    }
    private static void verificarBusca(Labirinto mapa, List<Posicao> metas, int esperado) {
        ResultadoBusca d = new Dijkstra().buscar(mapa, metas, true);
        ResultadoBusca a = new AEstrela().buscar(mapa, metas, true);
        ResultadoBusca zero = new AEstrela(true).buscar(mapa, metas, true);
        for (ResultadoBusca r : List.of(d, a, zero)) {
            conferir(r.custo() == esperado, r.algoritmo() + " custo esperado " + esperado + ", obtido " + r.custo());
            conferir(r.encontrou() == (esperado >= 0), "Estado de sucesso");
            conferir(r.processadas() <= r.descobertas(), "Processadas <= descobertas");
            long descobertas = r.eventos().stream().filter(e -> e.tipo() == EventoBusca.Tipo.DESCOBERTA).count();
            long processadas = r.eventos().stream().filter(e -> e.tipo() == EventoBusca.Tipo.PROCESSADA).count();
            conferir(descobertas == r.descobertas() && processadas == r.processadas(), "Métricas dos eventos");
            long distintas = r.eventos().stream().filter(e -> e.tipo() == EventoBusca.Tipo.PROCESSADA)
                    .map(EventoBusca::posicao).distinct().count();
            conferir(distintas == r.processadas(), "Cada célula é processada uma vez");
            if (r.encontrou()) {
                conferir(r.caminho().get(0).equals(mapa.inicio()), "Caminho começa no início");
                conferir(metas.contains(r.destino()), "Caminho termina no objetivo selecionado");
                conferir(r.caminho().size() - 1 == r.custo(), "Custo = movimentos");
                for (int i = 0; i < r.caminho().size(); i++) {
                    conferir(mapa.transitavel(r.caminho().get(i)), "Caminho respeita paredes e limites");
                    if (i > 0) conferir(mapa.vizinhos(r.caminho().get(i - 1)).contains(r.caminho().get(i)), "Passos adjacentes");
                }
            } else conferir(r.caminho().isEmpty(), "Sem caminho retorna lista vazia");
        }
        conferir(d.caminho().equals(zero.caminho()), "A* h=0: mesmo caminho do Dijkstra");
        conferir(d.processadas() == zero.processadas() && d.descobertas() == zero.descobertas(), "A* h=0: mesmas métricas");
        List<Posicao> ordemD = d.eventos().stream().filter(e -> e.tipo() == EventoBusca.Tipo.PROCESSADA).map(EventoBusca::posicao).toList();
        List<Posicao> ordemZero = zero.eventos().stream().filter(e -> e.tipo() == EventoBusca.Tipo.PROCESSADA).map(EventoBusca::posicao).toList();
        conferir(ordemD.equals(ordemZero), "A* h=0: mesma ordem de processamento");
        ResultadoBusca semEventos = new AEstrela().buscar(mapa, metas, false);
        conferir(semEventos.eventos().isEmpty() && semEventos.custo() == a.custo(), "Modo sem eventos");
        // Consistência h(u) <= 1 + h(v) para toda aresta deste mapa.
        AEstrela algoritmo = new AEstrela();
        for (int l = 0; l < mapa.linhas(); l++) for (int c = 0; c < mapa.colunas(); c++) {
            Posicao u = new Posicao(l, c);
            if (mapa.transitavel(u)) for (Posicao v : mapa.vizinhos(u))
                conferir(algoritmo.heuristica(u, metas) <= 1 + algoritmo.heuristica(v, metas), "Consistência da heurística");
        }
    }
    private static void testarArquivos() throws IOException {
        List<List<String>> invalidos = List.of(List.of(), List.of(""), List.of("23", "1"),
                List.of("2x3"), List.of("003"), List.of("223"), List.of("200"), List.of("23", ""));
        for (List<String> linhas : invalidos) {
            boolean falhou = false;
            try { new Labirinto(linhas); } catch (IllegalArgumentException esperado) { falhou = true; }
            conferir(falhou, "Arquivo inválido rejeitado: " + linhas);
        }
        Path temporario = Files.createTempFile("labirinto-teste", ".dat");
        try {
            Files.writeString(temporario, "\uFEFF111\r\n123\r\n111", StandardCharsets.UTF_8);
            Labirinto lido = LeitorLabirinto.ler(temporario);
            conferir(lido.linhas() == 3 && lido.saidas().size() == 1, "CRLF, BOM e ausência de quebra final");
        } finally { Files.deleteIfExists(temporario); }
    }
    private static void testarAleatorios() {
        Random random = new Random(20261118L);
        for (int t = 0; t < 400; t++) {
            int altura = 3 + random.nextInt(10), largura = 3 + random.nextInt(10);
            char[][] grade = new char[altura][largura];
            for (char[] linha : grade) for (int c = 0; c < largura; c++) linha[c] = random.nextDouble() < .35 ? '1' : '0';
            grade[0][0] = '2';
            grade[altura - 1][largura - 1] = '3';
            grade[0][largura - 1] = '3';
            List<String> linhas = new ArrayList<>();
            for (char[] linha : grade) linhas.add(new String(linha));
            Labirinto mapa = new Labirinto(linhas);
            verificarBusca(mapa, mapa.saidas(), referenciaBfs(mapa, mapa.saidas()));
            if (t % 10 == 0) {
                List<Posicao> unica = List.of(mapa.saidas().get(1));
                verificarBusca(mapa, unica, referenciaBfs(mapa, unica));
            }
        }
    }
    /** Referência independente: não usa fila de prioridade nem heurística. */
    private static int referenciaBfs(Labirinto mapa, List<Posicao> metas) {
        int[][] dist = new int[mapa.linhas()][mapa.colunas()];
        for (int[] linha : dist) Arrays.fill(linha, -1);
        ArrayDeque<Posicao> fila = new ArrayDeque<>();
        fila.add(mapa.inicio()); dist[mapa.inicio().linha()][mapa.inicio().coluna()] = 0;
        while (!fila.isEmpty()) {
            Posicao p = fila.remove();
            if (metas.contains(p)) return dist[p.linha()][p.coluna()];
            for (Posicao v : mapa.vizinhos(p)) if (dist[v.linha()][v.coluna()] < 0) {
                dist[v.linha()][v.coluna()] = dist[p.linha()][p.coluna()] + 1;
                fila.add(v);
            }
        }
        return -1;
    }
    private static void testarPintura(Labirinto mapa) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (AlgoritmoBusca busca : List.of(new Dijkstra(), new AEstrela())) {
                PainelLabirinto painel = new PainelLabirinto(mapa);
                painel.setSize(470, 550);
                for (EventoBusca evento : busca.buscar(mapa).eventos()) painel.aplicar(evento);
                BufferedImage imagem = new BufferedImage(470, 550, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = imagem.createGraphics(); painel.paint(g); g.dispose();
                conferir(imagem.getRGB(0, 0) != 0, "Pintura em modo headless");
                // Apenas inspeção local opcional, fora do pacote de entrega.
                if (Boolean.getBoolean("labirinto.imagens")) try {
                    ImageIO.write(imagem, "png", Path.of("build", busca instanceof Dijkstra ? "dijkstra.png" : "aestrela.png").toFile());
                } catch (IOException e) { throw new IllegalStateException(e); }
            }
        });
    }
    private static void conferir(boolean condicao, String descricao) {
        verificacoes++;
        if (!condicao) throw new AssertionError(descricao);
    }
}
