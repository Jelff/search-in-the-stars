package labirinto;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        try { executar(args); }
        catch (Exception erro) {
            System.err.println("Erro: " + erro.getMessage());
            System.err.println("Use --ajuda para consultar os comandos.");
            System.exit(1);
        }
    }
    private static void executar(String[] args) throws IOException {
        Path arquivo = Path.of("dados", "labirinto.dat"), csv = null;
        boolean console = false, benchmark = false, web = false;
        int porta = 8081;
        Posicao objetivo = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--ajuda", "-h" -> {
                    System.out.println("""
                        Uso: java -jar labirinto.jar [opções]
                          --arquivo caminho.dat  Arquivo a carregar (padrão: dados/labirinto.dat)
                          --web                  Abrir servidor da interface espacial no localhost
                          --porta número         Porta da interface web (padrão: 8081)
                          --console              Executar os dois algoritmos no terminal
                          --objetivo linha,coluna Buscar somente essa saída (requer --console)
                          --csv resultado.csv    Exportar métricas (ativa --console)
                          --benchmark            Mediana de 101 buscas após 50 aquecimentos (ativa --console)
                          --ajuda                Mostrar esta ajuda
                        Sem opções: abre a interface Swing. Java 17 ou superior.
                        """);
                    return;
                }
                case "--arquivo" -> arquivo = Path.of(valor(args, ++i));
                case "--web" -> web = true;
                case "--porta" -> porta = Integer.parseInt(valor(args, ++i));
                case "--console" -> console = true;
                case "--benchmark" -> { benchmark = true; console = true; }
                case "--csv" -> { csv = Path.of(valor(args, ++i)); console = true; }
                case "--objetivo" -> {
                    String[] partes = valor(args, ++i).split(",", -1);
                    if (partes.length != 2) throw new IllegalArgumentException("Objetivo deve ser linha,coluna.");
                    objetivo = new Posicao(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]));
                }
                default -> throw new IllegalArgumentException("Opção desconhecida: " + args[i]);
            }
        }
        if (web) {
            if (console || objetivo != null) throw new IllegalArgumentException("Use --web separadamente do modo console.");
            ServidorWeb.iniciar(arquivo, porta);
            return;
        }
        if (objetivo != null && !console) throw new IllegalArgumentException("--objetivo requer --console.");
        Labirinto mapa = LeitorLabirinto.ler(arquivo);
        if (!console) {
            if (GraphicsEnvironment.isHeadless()) throw new IllegalArgumentException("Sem ambiente gráfico; utilize --console.");
            Path caminho = arquivo;
            SwingUtilities.invokeLater(() -> new JanelaLabirinto(mapa, caminho).setVisible(true));
            return;
        }
        List<Posicao> metas = objetivo == null ? mapa.saidas() : List.of(objetivo);
        System.out.println("Grade: " + mapa.linhas() + " x " + mapa.colunas() + " | Início: " + mapa.inicio() + " | Objetivos: " + metas);
        List<String> linhasCsv = new ArrayList<>();
        linhasCsv.add("algoritmo;encontrou;linha_destino;coluna_destino;custo;celulas_caminho;processadas;descobertas;tempo_ns;mediana_sem_eventos_ns");
        for (AlgoritmoBusca algoritmo : List.of(new Dijkstra(), new AEstrela())) {
            ResultadoBusca r = algoritmo.buscar(mapa, metas, true);
            System.out.println("\n" + r.algoritmo() + "\n" + JanelaLabirinto.resumo(r));
            System.out.println("Caminho: " + r.caminho());
            imprimirMapa(mapa, r.caminho());
            long mediana = -1;
            if (benchmark) {
                mediana = mediana(algoritmo, mapa, metas);
                System.out.printf(Locale.ROOT, "Mediana sem eventos: %.3f ms%n", mediana / 1_000_000.0);
            }
            Posicao fim = r.destino();
            linhasCsv.add(r.algoritmo() + ";" + r.encontrou() + ";" + (fim == null ? "" : fim.linha())
                    + ";" + (fim == null ? "" : fim.coluna()) + ";" + r.custo() + ";" + r.caminho().size()
                    + ";" + r.processadas() + ";" + r.descobertas() + ";" + r.nanos() + ";" + mediana);
        }
        if (csv != null) {
            Files.write(csv, linhasCsv, StandardCharsets.UTF_8);
            System.out.println("\nCSV salvo em: " + csv.toAbsolutePath());
        }
        System.out.println("\nTempos dependem da JVM e da máquina. A animação e a leitura estão excluídas.");
    }
    private static String valor(String[] args, int indice) {
        if (indice >= args.length) throw new IllegalArgumentException("Falta valor para a opção.");
        return args[indice];
    }
    private static long mediana(AlgoritmoBusca algoritmo, Labirinto mapa, List<Posicao> metas) {
        for (int i = 0; i < 50; i++) algoritmo.buscar(mapa, metas, false);
        long[] tempos = new long[101];
        for (int i = 0; i < tempos.length; i++) tempos[i] = algoritmo.buscar(mapa, metas, false).nanos();
        Arrays.sort(tempos);
        return tempos[tempos.length / 2];
    }
    private static void imprimirMapa(Labirinto mapa, List<Posicao> caminho) {
        for (int l = 0; l < mapa.linhas(); l++) {
            StringBuilder texto = new StringBuilder();
            for (int c = 0; c < mapa.colunas(); c++) {
                char valor = mapa.valor(l, c);
                texto.append(valor == '0' && caminho.contains(new Posicao(l, c)) ? '*' : valor);
            }
            System.out.println(texto);
        }
    }
}
