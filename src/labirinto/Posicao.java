package labirinto;

/** Coordenadas da matriz, com índices iniciados em zero. */
public record Posicao(int linha, int coluna) {
    @Override public String toString() { return "(" + linha + ", " + coluna + ")"; }
}
