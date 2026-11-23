package labirinto;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/** A* com Manhattan mínima para os objetivos selecionados, consistente nesta grade. */
public final class AEstrela implements AlgoritmoBusca {
    private final boolean heuristicaZero;
    public AEstrela() { this(false); }
    /** Modo didático/teste: h=0 faz A* utilizar a prioridade do Dijkstra. */
    public AEstrela(boolean heuristicaZero) { this.heuristicaZero = heuristicaZero; }

    public int heuristica(Posicao p, List<Posicao> metas) {
        if (metas.isEmpty()) throw new IllegalArgumentException("Não há objetivos para a heurística.");
        if (heuristicaZero) return 0;
        int menor = Integer.MAX_VALUE;
        for (Posicao destino : metas) {
            int manhattan = Math.abs(p.linha() - destino.linha())
                    + Math.abs(p.coluna() - destino.coluna());
            menor = Math.min(menor, manhattan);
        }
        return menor;
    }

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
        int hInicial = heuristica(inicio, metas);
        fila.add(new EntradaFila(inicio, 0, hInicial, ordem++));
        ApoioBusca.evento(eventos, registrar, EventoBusca.Tipo.DESCOBERTA, inicio, 0, hInicial);

        while (!fila.isEmpty()) {
            EntradaFila entrada = fila.poll();
            Posicao atual = entrada.posicao();
            int l = atual.linha(), c = atual.coluna();
            if (entrada.g() != g[l][c] || fechado[l][c]) continue;
            // Fechamento definitivo é válido porque Manhattan é consistente neste modelo.
            fechado[l][c] = true;
            processadas++;
            ApoioBusca.evento(eventos, registrar, EventoBusca.Tipo.PROCESSADA, atual, g[l][c], heuristica(atual, metas));
            if (metas.contains(atual)) {
                List<Posicao> caminho = ApoioBusca.reconstruir(atual, anterior);
                long nanos = System.nanoTime() - inicioTempo;
                for (int i = 0; i < caminho.size(); i++)
                    ApoioBusca.evento(eventos, registrar, EventoBusca.Tipo.CAMINHO,
                            caminho.get(i), i, heuristica(caminho.get(i), metas));
                return new ResultadoBusca("A*", caminho, g[l][c], processadas, descobertas, nanos, eventos);
            }
            for (Posicao vizinho : mapa.vizinhos(atual)) {
                int vl = vizinho.linha(), vc = vizinho.coluna();
                if (fechado[vl][vc]) continue;
                int novoG = g[l][c] + 1;
                if (novoG < g[vl][vc]) {
                    boolean primeira = g[vl][vc] == Integer.MAX_VALUE;
                    g[vl][vc] = novoG;
                    anterior[vl][vc] = atual;
                    int h = heuristica(vizinho, metas);
                    fila.add(new EntradaFila(vizinho, novoG, novoG + h, ordem++));
                    if (primeira) descobertas++;
                    ApoioBusca.evento(eventos, registrar, primeira ? EventoBusca.Tipo.DESCOBERTA
                            : EventoBusca.Tipo.MELHORIA, vizinho, novoG, h);
                }
            }
        }
        return new ResultadoBusca("A*", List.of(), -1, processadas, descobertas,
                System.nanoTime() - inicioTempo, eventos);
    }
}
