package labirinto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Apenas utilidades; o laço de cada algoritmo permanece em sua própria classe. */
final class ApoioBusca {
    private ApoioBusca() {}
    static int[][] distancias(Labirinto mapa) {
        int[][] g = new int[mapa.linhas()][mapa.colunas()];
        for (int[] linha : g) Arrays.fill(linha, Integer.MAX_VALUE);
        return g;
    }
    static List<Posicao> objetivos(Labirinto mapa, List<Posicao> objetivos) {
        if (objetivos == null || objetivos.isEmpty()) throw new IllegalArgumentException("Selecione ao menos uma saída.");
        List<Posicao> copia = List.copyOf(objetivos);
        for (Posicao p : copia) if (!mapa.saidas().contains(p))
            throw new IllegalArgumentException("O objetivo " + p + " não é uma saída do mapa.");
        return copia;
    }
    static void evento(List<EventoBusca> eventos, boolean registrar, EventoBusca.Tipo tipo,
            Posicao p, int g, int h) {
        if (registrar) eventos.add(new EventoBusca(tipo, p, g, h));
    }
    static List<Posicao> reconstruir(Posicao fim, Posicao[][] anterior) {
        List<Posicao> caminho = new ArrayList<>();
        for (Posicao p = fim; p != null; p = anterior[p.linha()][p.coluna()]) caminho.add(p);
        Collections.reverse(caminho);
        return caminho;
    }
    static void caminho(List<EventoBusca> eventos, boolean registrar, List<Posicao> caminho) {
        for (int i = 0; i < caminho.size(); i++)
            evento(eventos, registrar, EventoBusca.Tipo.CAMINHO, caminho.get(i), i, 0);
    }
}
