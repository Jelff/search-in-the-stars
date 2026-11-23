package labirinto;

/** Eventos são reproduzidos depois; nenhuma pausa entra no tempo da busca. */
public record EventoBusca(Tipo tipo, Posicao posicao, int g, int h) {
    public enum Tipo { DESCOBERTA, MELHORIA, PROCESSADA, CAMINHO }
    public int f() { return g + h; }
}
