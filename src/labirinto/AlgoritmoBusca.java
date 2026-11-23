package labirinto;

import java.util.List;

public interface AlgoritmoBusca {
    /** eventos=false permite medir sem construir o histórico de animação. */
    ResultadoBusca buscar(Labirinto mapa, List<Posicao> objetivos, boolean eventos);

    default ResultadoBusca buscar(Labirinto mapa) {
        return buscar(mapa, mapa.saidas(), true);
    }
}
