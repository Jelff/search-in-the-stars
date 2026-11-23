package labirinto;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/** Dijkstra: sempre retira a posição com menor custo acumulado g. */
public final class Dijkstra implements AlgoritmoBusca {
    @Override public ResultadoBusca buscar(Labirinto mapa, List<Posicao> objetivos, boolean registrar) {
        List<Posicao> metas = ApoioBusca.objetivos(mapa, objetivos);
        long inicioTempo = System.nanoTime();
        int[][] g = ApoioBusca.distancias(mapa);
        Posicao[][] anterior = new Posicao[mapa.linhas()][mapa.colunas()];
        boolean[][] fechado = new boolean[mapa.linhas()][mapa.colunas()];
        PriorityQueue<EntradaFila> fila = new PriorityQueue<>(EntradaFila.ORDEM);
        List<EventoBusca> eventos = new ArrayList<>();
        long ordem = 0;
        int processadas = 0, descobertas = 1;
        Posicao inicio = mapa.inicio();
        g[inicio.linha()][inicio.coluna()] = 0;
        fila.add(new EntradaFila(inicio, 0, 0, ordem++));
        ApoioBusca.evento(eventos, registrar, EventoBusca.Tipo.DESCOBERTA, inicio, 0, 0);

        while (!fila.isEmpty()) {
            EntradaFila entrada = fila.poll();
            Posicao atual = entrada.posicao();
            int l = atual.linha(), c = atual.coluna();
            // Entradas antigas não são processadas nem contadas novamente.
            if (entrada.g() != g[l][c] || fechado[l][c]) continue;
            fechado[l][c] = true;
            processadas++;
            ApoioBusca.evento(eventos, registrar, EventoBusca.Tipo.PROCESSADA, atual, g[l][c], 0);

            // Encerrar apenas na retirada válida, nunca na descoberta do objetivo.
            if (metas.contains(atual)) {
                List<Posicao> caminho = ApoioBusca.reconstruir(atual, anterior);
                long nanos = System.nanoTime() - inicioTempo;
                ApoioBusca.caminho(eventos, registrar, caminho);
                return new ResultadoBusca("Dijkstra", caminho, g[l][c], processadas, descobertas, nanos, eventos);
            }
            for (Posicao vizinho : mapa.vizinhos(atual)) {
                int vl = vizinho.linha(), vc = vizinho.coluna();
                if (fechado[vl][vc]) continue;
                int novoG = g[l][c] + 1;
                if (novoG < g[vl][vc]) { // Relaxamento da aresta de custo 1.
                    boolean primeira = g[vl][vc] == Integer.MAX_VALUE;
                    g[vl][vc] = novoG;
                    anterior[vl][vc] = atual;
                    fila.add(new EntradaFila(vizinho, novoG, novoG, ordem++));
                    if (primeira) descobertas++;
                    ApoioBusca.evento(eventos, registrar, primeira ? EventoBusca.Tipo.DESCOBERTA
                            : EventoBusca.Tipo.MELHORIA, vizinho, novoG, 0);
                }
            }
        }
        return new ResultadoBusca("Dijkstra", List.of(), -1, processadas, descobertas,
                System.nanoTime() - inicioTempo, eventos);
    }
}
