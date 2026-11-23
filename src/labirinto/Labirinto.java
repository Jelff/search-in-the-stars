package labirinto;

import java.util.ArrayList;
import java.util.List;

/** Modelo imutável: 1 = parede, 0 = passagem, 2 = início, 3 = saída. */
public final class Labirinto {
    private final char[][] celulas;
    private final Posicao inicio;
    private final List<Posicao> saidas;

    public Labirinto(List<String> linhas) {
        if (linhas.isEmpty() || linhas.get(0).isEmpty())
            throw new IllegalArgumentException("O arquivo está vazio.");
        celulas = new char[linhas.size()][linhas.get(0).length()];
        Posicao encontrada = null;
        List<Posicao> destinos = new ArrayList<>();
        for (int l = 0; l < linhas.size(); l++) {
            String texto = linhas.get(l);
            if (texto.length() != colunas())
                throw new IllegalArgumentException("Linha " + (l + 1) + " tem tamanho diferente das demais.");
            for (int c = 0; c < colunas(); c++) {
                char valor = texto.charAt(c);
                if (valor < '0' || valor > '3')
                    throw new IllegalArgumentException("Caractere inválido na linha " + (l + 1) + ", coluna " + (c + 1) + ".");
                celulas[l][c] = valor;
                if (valor == '2') {
                    if (encontrada != null) throw new IllegalArgumentException("Existe mais de um início (2).");
                    encontrada = new Posicao(l, c);
                } else if (valor == '3') destinos.add(new Posicao(l, c));
            }
        }
        if (encontrada == null) throw new IllegalArgumentException("Não existe início (2).");
        if (destinos.isEmpty()) throw new IllegalArgumentException("Não existe saída (3).");
        inicio = encontrada;
        saidas = List.copyOf(destinos);
    }

    public int linhas() { return celulas.length; }
    public int colunas() { return celulas[0].length; }
    public Posicao inicio() { return inicio; }
    public List<Posicao> saidas() { return saidas; }
    public char valor(int l, int c) { return celulas[l][c]; }
    public boolean transitavel(Posicao p) {
        return p.linha() >= 0 && p.linha() < linhas() && p.coluna() >= 0
                && p.coluna() < colunas() && valor(p.linha(), p.coluna()) != '1';
    }

    /** Ordem fixa: cima, direita, baixo, esquerda. Todas as arestas custam 1. */
    public List<Posicao> vizinhos(Posicao p) {
        List<Posicao> resultado = new ArrayList<>(4);
        int[][] movimentos = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
        for (int[] m : movimentos) {
            Posicao v = new Posicao(p.linha() + m[0], p.coluna() + m[1]);
            if (transitavel(v)) resultado.add(v);
        }
        return resultado;
    }
}
