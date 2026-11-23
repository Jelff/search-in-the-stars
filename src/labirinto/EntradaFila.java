package labirinto;

import java.util.Comparator;

/** Snapshot imutável. Melhorias inserem outra entrada, sem modificar a anterior. */
public record EntradaFila(Posicao posicao, int g, int prioridade, long ordem) {
    public static final Comparator<EntradaFila> ORDEM = Comparator
            .comparingInt(EntradaFila::prioridade).thenComparingLong(EntradaFila::ordem);
}
