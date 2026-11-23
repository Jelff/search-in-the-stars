package labirinto;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/** Desenha o mapa a partir dos eventos; não executa a busca. */
@SuppressWarnings("serial") // Componentes são desenhados, não serializados.
public final class PainelLabirinto extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final Color PAREDE = new Color(45, 55, 70);
    public static final Color ABERTA = new Color(255, 223, 109);
    public static final Color FECHADA = new Color(146, 204, 244);
    public static final Color CAMINHO = new Color(166, 112, 218);
    private Labirinto mapa;
    private int[][] estados;
    private Posicao atual;

    public PainelLabirinto(Labirinto mapa) {
        setBackground(new Color(245, 247, 251));
        setPreferredSize(new Dimension(470, 510));
        reiniciar(mapa);
    }
    public void reiniciar(Labirinto novo) {
        mapa = novo;
        estados = new int[mapa.linhas()][mapa.colunas()];
        atual = null;
        repaint();
    }
    public void aplicar(EventoBusca evento) {
        Posicao p = evento.posicao();
        estados[p.linha()][p.coluna()] = switch (evento.tipo()) {
            case DESCOBERTA, MELHORIA -> 1;
            case PROCESSADA -> 2;
            case CAMINHO -> 3;
        };
        atual = p;
        repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        int tamanho = Math.max(1, Math.min((getWidth() - 44) / mapa.colunas(),
                (getHeight() - 44) / mapa.linhas()));
        int x0 = (getWidth() - tamanho * mapa.colunas()) / 2;
        int y0 = (getHeight() - tamanho * mapa.linhas()) / 2;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, tamanho / 3)));
        for (int l = 0; l < mapa.linhas(); l++) {
            for (int c = 0; c < mapa.colunas(); c++) {
                char valor = mapa.valor(l, c);
                Color cor = valor == '1' ? PAREDE : switch (estados[l][c]) {
                    case 1 -> ABERTA;
                    case 2 -> FECHADA;
                    case 3 -> CAMINHO;
                    default -> Color.WHITE;
                };
                int x = x0 + c * tamanho, y = y0 + l * tamanho;
                g.setColor(cor);
                g.fillRect(x, y, tamanho, tamanho);
                g.setColor(new Color(215, 221, 229));
                g.drawRect(x, y, tamanho, tamanho);
                // Início e saída mantêm marcadores sobre a cor da exploração/caminho.
                if (valor == '2' || valor == '3') {
                    g.setColor(valor == '2' ? new Color(18, 120, 65) : new Color(191, 40, 55));
                    g.fillOval(x + tamanho / 5, y + tamanho / 5, tamanho * 3 / 5, tamanho * 3 / 5);
                    g.setColor(Color.WHITE);
                    String texto = valor == '2' ? "I" : "S";
                    int largura = g.getFontMetrics().stringWidth(texto);
                    g.drawString(texto, x + (tamanho - largura) / 2,
                            y + (tamanho + g.getFontMetrics().getAscent()) / 2 - 2);
                }
            }
        }
        g.setColor(new Color(65, 76, 92));
        for (int c = 0; c < mapa.colunas(); c++)
            g.drawString(String.valueOf(c), x0 + c * tamanho + tamanho / 3, y0 - 5);
        for (int l = 0; l < mapa.linhas(); l++)
            g.drawString(String.valueOf(l), x0 - 21, y0 + l * tamanho + tamanho * 2 / 3);
        if (atual != null) {
            g.setColor(new Color(235, 113, 20));
            g.setStroke(new BasicStroke(3));
            g.drawRect(x0 + atual.coluna() * tamanho + 2, y0 + atual.linha() * tamanho + 2,
                    Math.max(1, tamanho - 4), Math.max(1, tamanho - 4));
        }
        g.dispose();
    }
}
