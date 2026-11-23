package labirinto;

import java.util.List;

public record ResultadoBusca(String algoritmo, List<Posicao> caminho, int custo,
        int processadas, int descobertas, long nanos, List<EventoBusca> eventos) {
    public ResultadoBusca {
        caminho = List.copyOf(caminho);
        eventos = List.copyOf(eventos);
    }
    public boolean encontrou() { return !caminho.isEmpty(); }
    public Posicao destino() { return encontrou() ? caminho.get(caminho.size() - 1) : null; }
}
